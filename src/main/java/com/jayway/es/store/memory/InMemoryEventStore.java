package com.jayway.es.store.memory;

import com.jayway.es.api.Event;
import com.jayway.es.store.EventStore;
import com.jayway.es.store.EventStream;
import com.jayway.es.store.ListEventStream;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventStore implements EventStore<Long> {
    private final Map<UUID, ListEventStream> streams = new ConcurrentHashMap<>();
    private final TreeSet<Transaction> transactions = new TreeSet<>();

    @Override
    public ListEventStream loadEventStream(UUID aggregateId) {
        ListEventStream eventStream = streams.get(aggregateId);
        if (eventStream == null) {
            eventStream = new ListEventStream();
            streams.put(aggregateId, eventStream);
        }
        return eventStream;
    }

    @Override
    public void store(UUID aggregateId, long version, List<Event> events) {
        ListEventStream stream = loadEventStream(aggregateId);
        if (stream.version() != version) {
            throw new ConcurrentModificationException("Stream has already been modified");
        }
        streams.put(aggregateId, stream.append(events));
        synchronized (transactions) {
            transactions.add(new Transaction(events));
        }
    }

    public EventStream<Long> loadEventsAfter(Long timestamp) {
        // include all events after this timestamp, except the events with the current timestamp
        // since new events might be added with the current timestamp
        List<Event> events = new LinkedList<>();
        long now;
        synchronized (transactions) {
            now = System.currentTimeMillis();
            for (Transaction t : transactions.tailSet(new Transaction(timestamp)).headSet(new Transaction(now))) {
                events.addAll(t.events);
            }
        }
        return new ListEventStream(now - 1, events);
    }

    @Override
    public Observable<Event> all() {
        throw new UnsupportedOperationException();
    }

}

class Transaction implements Comparable<Transaction> {
    public final List<? extends Event> events;
    private final long timestamp;

    public Transaction(long timestamp) {
        events = Collections.emptyList();
        this.timestamp = timestamp;

    }

    public Transaction(List<? extends Event> events) {
        this.events = events;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public int compareTo(Transaction other) {
        if (timestamp < other.timestamp) {
            return -1;
        } else if (timestamp > other.timestamp) {
            return 1;
        }
        return 0;
    }
}
