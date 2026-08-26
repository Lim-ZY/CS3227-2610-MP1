package Timey.model;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.domain.transit.FixedCommute;
import Timey.domain.transit.RouteAlternative;
import Timey.command.PlanCommand;
import Timey.planner.Planner;
import Timey.ports.FixedCommuteStore;
import Timey.reminder.DepartureReminderService;

/** Stores the mutable state of a Timey command session. */
public final class TimeyModel {
    private final Planner planner;
    private final FixedCommuteStore fixedCommuteStore;
    private final DepartureReminderService departureReminderService;
    private final Clock clock;
    private PlanCommand pendingPlan;
    private List<RouteAlternative> pendingAlternatives = List.of();
    private List<String> planningMessages = List.of();
    private DepartureRecommendation selectedRecommendation;

    public TimeyModel(Planner planner, FixedCommuteStore fixedCommuteStore,
            DepartureReminderService departureReminderService, Clock clock) {
        this.planner = Objects.requireNonNull(planner);
        this.fixedCommuteStore = Objects.requireNonNull(fixedCommuteStore);
        this.departureReminderService = Objects.requireNonNull(departureReminderService);
        this.clock = Objects.requireNonNull(clock);
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
    public void selectRecommendation(DepartureRecommendation recommendation) {
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

    /** Returns the currently active departure reminders for this command session. */
    public List<ScheduledDepartureReminder> getScheduledReminders() {
        return departureReminderService.scheduledReminders();
    }

    /** Cancels the requested one-based departure reminder, if it is active. */
    public boolean cancelReminder(int reminderNumber) {
        return departureReminderService.cancel(reminderNumber);
    }

    /** Returns the clock used to present command-session times. */
    public Clock getClock() {
        return clock;
    }
}
