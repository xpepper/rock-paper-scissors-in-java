package com.jayway.rps.domain.game;

import com.jayway.es.api.Event;
import com.jayway.rps.domain.Move;
import com.jayway.rps.domain.command.CreateGameCommand;
import com.jayway.rps.domain.command.MakeMoveCommand;
import com.jayway.rps.domain.event.GameCreatedEvent;
import com.jayway.rps.domain.event.GameTiedEvent;
import com.jayway.rps.domain.event.GameWonEvent;
import com.jayway.rps.domain.event.MoveDecidedEvent;

import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

public class Game {
    enum State {
        notInitialized, created, waiting, tied, won
    }

    private State state = State.notInitialized;
    private String player;
    private Move move;

    public List<Event> handle(CreateGameCommand c) {
        if (state != State.notInitialized) throw new IllegalStateException(state.toString());
        return asList(
            new GameCreatedEvent(c.gameId, c.playerEmail));
    }

    public List<Event> handle(MakeMoveCommand c) {
        if (State.created == state) {
            return asList(new MoveDecidedEvent(c.gameId, c.playerEmail, c.move));
        } else if (State.waiting == state) {
            if (player.equals(c.playerEmail)) throw new IllegalArgumentException("Player already in game");
            return asList(
                new MoveDecidedEvent(c.gameId, c.playerEmail, c.move),
                makeEndGameEvent(c.gameId, c.playerEmail, c.move));
        } else {
            throw new IllegalStateException(state.toString());
        }
    }

    private Event makeEndGameEvent(UUID gameId, String opponentEmail, Move opponentMove) {
        if (move.defeats(opponentMove)) {
            return new GameWonEvent(gameId, player, opponentEmail);
        } else if (opponentMove.defeats(move)) {
            return new GameWonEvent(gameId, opponentEmail, player);
        } else {
            return new GameTiedEvent(gameId);
        }
    }

    public void handle(GameCreatedEvent e) {
        state = State.created;
    }

    public void handle(MoveDecidedEvent e) {
        if (state == State.created) {
            move = e.move;
            player = e.playerEmail;
            state = State.waiting;
        }
    }

    public void handle(GameWonEvent e) {
        state = State.won;
    }

    public void handle(GameTiedEvent e) {
        state = State.tied;
    }
}
