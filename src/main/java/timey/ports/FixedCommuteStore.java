package timey.ports;

import java.util.List;
import java.util.Optional;

import timey.domain.transit.FixedCommute;

/** Stores user-recorded commute durations for exact origin and destination pairs. */
public interface FixedCommuteStore {
    void save(FixedCommute commute);

    Optional<FixedCommute> find(String origin, String destination);

    /** Returns saved timings in stable presentation order. */
    List<FixedCommute> findAll();

    /** Removes the exact origin/destination timing, returning whether it existed. */
    boolean remove(String origin, String destination);
}
