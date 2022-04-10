package com.jayway.es.store;

import com.jayway.es.api.Event;
import rx.Observable;

import java.util.List;
import java.util.UUID;

public interface EventStore<V> {
	EventStream<Long> loadEventStream(UUID aggregateId);
	void store(UUID aggregateId, long version, List<Event> events);
	Observable<Event> all();
}
