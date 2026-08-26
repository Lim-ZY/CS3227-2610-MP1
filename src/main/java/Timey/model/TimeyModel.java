package Timey.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.transit.RouteAlternative;
import Timey.parser.PlanCommand;

/** Stores the mutable state of a Timey command session. */
public final class TimeyModel {
    private PlanCommand pendingPlan;
    private List<RouteAlternative> pendingAlternatives = List.of();
    private List<String> planningMessages = List.of();
    private DepartureRecommendation selectedRecommendation;

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
}
