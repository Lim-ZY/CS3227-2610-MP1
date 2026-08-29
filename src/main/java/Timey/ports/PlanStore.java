package Timey.ports;

import java.util.List;

import Timey.domain.alert.SavedPlan;

/** Stores selected commute plans for later display. */
public interface PlanStore {
    /** Returns locally saved plans in their stored order. */
    default List<SavedPlan> loadAll() {
        return List.of();
    }

    /** Replaces the locally saved plans with the supplied stable-order list. */
    void saveAll(List<SavedPlan> plans);
}
