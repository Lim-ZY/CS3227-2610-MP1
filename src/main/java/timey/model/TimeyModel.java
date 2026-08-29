package timey.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import timey.command.PlanCommand;
import timey.domain.alert.DepartureRecommendation;
import timey.domain.alert.SavedPlan;
import timey.domain.alert.ScheduledDepartureReminder;
import timey.domain.transit.FixedCommute;
import timey.domain.transit.RouteAlternative;
import timey.planner.Planner;
import timey.ports.FixedCommuteStore;
import timey.ports.PlanStore;
import timey.reminder.DepartureReminderService;

/** Stores the mutable state of a Timey command session. */
public final class TimeyModel {
    private final Planner planner;
    private final FixedCommuteStore fixedCommuteStore;
    private final PlanStore planStore;
    private final DepartureReminderService departureReminderService;
    private final Clock clock;
    private PlanCommand pendingPlan;
    private List<RouteAlternative> pendingAlternatives = List.of();
    private List<String> planningMessages = List.of();
    private DepartureRecommendation selectedRecommendation;
    private List<SavedPlan> savedPlans = List.of();

    /** Creates a new TimeyModel. */
    public TimeyModel(Planner planner, FixedCommuteStore fixedCommuteStore,
            PlanStore planStore, DepartureReminderService departureReminderService, Clock clock) {
        this.planner = Objects.requireNonNull(planner);
        this.fixedCommuteStore = Objects.requireNonNull(fixedCommuteStore);
        this.planStore = Objects.requireNonNull(planStore);
        this.departureReminderService = Objects.requireNonNull(departureReminderService);
        this.clock = Objects.requireNonNull(clock);
        this.savedPlans = planStore.loadAll();
        pruneExpiredPlans();
    }

    /** Finds routes, adds a matching fixed timing, and records the resulting plan state. */
    public void plan(PlanCommand plan) {
        Planner.PlanningResult result = planner.findAlternatives(plan);
        replacePlan(plan, result.alternatives(), result.messages());
        findFixedCommute(plan.getOrigin(), plan.getDestination()).ifPresent(commute -> {
            RouteAlternative fixedRoute = new RouteAlternative("Saved timing", java.time.Duration.ZERO,
                    commute.duration(), 0);
            replaceAlternatives(java.util.stream.Stream.concat(java.util.stream.Stream.of(fixedRoute),
                    getPendingAlternatives().stream()).toList());
            addPlanningMessage("Your saved fixed timing is available as route 1.");
        });
    }

    /** Saves a fixed commute duration for later route planning. */
    public FixedCommute saveFixedCommute(String origin, String destination, java.time.Duration duration) {
        FixedCommute commute = new FixedCommute(origin, destination, duration);
        fixedCommuteStore.save(commute);
        return commute;
    }

    /** Finds a saved fixed commute duration for the requested journey. */
    public Optional<FixedCommute> findFixedCommute(String origin, String destination) {
        return fixedCommuteStore.find(origin, destination);
    }

    /** Returns all fixed commute timings in a stable list order. */
    public List<FixedCommute> getFixedCommutes() {
        return fixedCommuteStore.findAll();
    }

    /** Removes a timing that was previously returned by {@link #getFixedCommutes()}. */
    public boolean removeFixedCommute(FixedCommute commute) {
        return fixedCommuteStore.remove(commute.origin(), commute.destination());
    }

    /** Replaces the current plan and clears its previous route selection. */
    public void replacePlan(PlanCommand plan, List<RouteAlternative> alternatives, List<String> messages) {
        this.pendingPlan = Objects.requireNonNull(plan);
        this.pendingAlternatives = List.copyOf(alternatives);
        this.planningMessages = List.copyOf(messages);
        this.selectedRecommendation = null;
    }

    /** Replaces the alternatives associated with the current plan. */
    public void replaceAlternatives(List<RouteAlternative> alternatives) {
        this.pendingAlternatives = List.copyOf(alternatives);
    }

    /** Adds a message associated with the current planning result. */
    public void addPlanningMessage(String message) {
        this.planningMessages = java.util.stream.Stream.concat(planningMessages.stream(),
                java.util.stream.Stream.of(Objects.requireNonNull(message))).toList();
    }

    /** Records the departure recommendation selected for the current plan. */
    private void selectRecommendation(DepartureRecommendation recommendation) {
        this.selectedRecommendation = Objects.requireNonNull(recommendation);
    }

    public Optional<PlanCommand> getPendingPlan() {
        return Optional.ofNullable(pendingPlan);
    }

    public List<RouteAlternative> getPendingAlternatives() {
        return pendingAlternatives;
    }

    public List<String> getPlanningMessages() {
        return planningMessages;
    }

    public Optional<DepartureRecommendation> getSelectedRecommendation() {
        return Optional.ofNullable(selectedRecommendation);
    }

    /** Returns selected plans in the order they were saved. */
    public List<SavedPlan> getSavedPlans() {
        return savedPlans;
    }

    /** Removes plans whose leave-by time is no longer in the future. */
    public void pruneExpiredPlans() {
        List<SavedPlan> remainingPlans = savedPlans.stream().filter(this::isFuture).distinct().toList();
        if (!remainingPlans.equals(savedPlans)) {
            savedPlans = remainingPlans;
            planStore.saveAll(savedPlans);
        }
    }

    /** Returns the currently active departure reminders for this command session. */
    public List<ScheduledDepartureReminder> getScheduledReminders() {
        return departureReminderService.scheduledReminders();
    }

    /** Cancels the requested one-based departure reminder, if it is active. */
    public boolean cancelReminder(int reminderNumber) {
        return departureReminderService.cancel(reminderNumber);
    }

    /** Selects a pending route and schedules a reminder when departure is still in the future. */
    public RouteSelectionResult selectRoute(Integer routeNumber) {
        if (pendingPlan == null) {
            return RouteSelectionResult.noPlan();
        }
        if (routeNumber == null) {
            return RouteSelectionResult.missingNumber(pendingAlternatives.size());
        }
        if (routeNumber < 1 || routeNumber > pendingAlternatives.size()) {
            return RouteSelectionResult.invalidNumber(pendingAlternatives.size());
        }

        RouteAlternative route = pendingAlternatives.get(routeNumber - 1);
        DepartureRecommendation recommendation = planner.recommendDeparture(pendingPlan, route);
        selectRecommendation(recommendation);
        saveSelectedPlan(recommendation);
        if (recommendation.departureTime().isBefore(LocalTime.now(clock))) {
            return RouteSelectionResult.leaveNow(recommendation);
        }
        ScheduledDepartureReminder reminder = departureReminderService.schedule(recommendation);
        return RouteSelectionResult.reminderScheduled(recommendation, reminder);
    }

    /** Returns the clock used to present command-session times. */
    public Clock getClock() {
        return clock;
    }

    /** Prunes plans before the command session is discarded. */
    public void close() {
        pruneExpiredPlans();
    }

    private void saveSelectedPlan(DepartureRecommendation recommendation) {
        LocalDate date = LocalDate.now(clock);
        SavedPlan savedPlan = new SavedPlan(date, pendingPlan.getArrivalTime(), pendingPlan.getOrigin(),
                pendingPlan.getDestination(), recommendation.departureTime());
        if (!isFuture(savedPlan)) {
            return;
        }
        if (!savedPlans.contains(savedPlan)) {
            savedPlans = java.util.stream.Stream.concat(savedPlans.stream(), java.util.stream.Stream.of(savedPlan))
                    .toList();
            planStore.saveAll(savedPlans);
        }
    }

    private boolean isFuture(SavedPlan plan) {
        LocalDate leaveByDate = plan.leaveBy().isAfter(plan.arrivalTime()) ? plan.date().minusDays(1) : plan.date();
        return LocalDateTime.of(leaveByDate, plan.leaveBy()).isAfter(LocalDateTime.now(clock));
    }
}
