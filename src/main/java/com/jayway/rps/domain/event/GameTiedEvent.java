package com.jayway.rps.domain.event;

import com.jayway.es.api.Event;

import java.util.UUID;

public class GameTiedEvent implements Event {
	public final UUID gameId;
	
	GameTiedEvent() {
		gameId = null;
	}

	public GameTiedEvent(UUID gameId) {
		this.gameId = gameId;
	}
	
}
