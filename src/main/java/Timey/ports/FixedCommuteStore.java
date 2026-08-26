package Timey.ports;

import java.util.Optional;

import Timey.domain.transit.FixedCommute;

/** Stores user-recorded commute durations for exact origin and destination pairs. */
public interface FixedCommuteStore {
    void save(FixedCommute commute);

    Optional<FixedCommute> find(String origin, String destination);
}
