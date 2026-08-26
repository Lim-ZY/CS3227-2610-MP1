package Timey.infrastructure.transit;

import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import Timey.domain.transit.FixedCommute;
import Timey.ports.FixedCommuteStore;

/** In-memory fixed timing store for isolated command-line and unit-test sessions. */
public final class InMemoryFixedCommuteStore implements FixedCommuteStore {
    private final Map<String, FixedCommute> commutes = new HashMap<>();

    @Override
    public void save(FixedCommute commute) {
        commutes.put(key(commute.origin(), commute.destination()), commute);
    }

    @Override
    public Optional<FixedCommute> find(String origin, String destination) {
        return Optional.ofNullable(commutes.get(key(origin, destination)));
    }

    @Override
    public List<FixedCommute> findAll() {
        return commutes.values().stream()
                .sorted(Comparator.comparing(FixedCommute::origin, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(FixedCommute::destination, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public boolean remove(String origin, String destination) {
        return commutes.remove(key(origin, destination)) != null;
    }

    private String key(String origin, String destination) {
        return origin.trim().toLowerCase() + "\u0000" + destination.trim().toLowerCase();
    }
}
