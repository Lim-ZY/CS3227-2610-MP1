package Timey.ports;

import java.util.List;

import Timey.domain.alert.SavedPlan;

/** Stores selected commute plans for later display. */
public interface PlanStore {
    /** Replaces the locally saved plans with the supplied stable-order list. */
    void saveAll(List<SavedPlan> plans);
}
