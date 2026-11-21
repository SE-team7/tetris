package com.team.tetris.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.team.tetris.controller.NetworkController;

/** NetworkEvents·NetworkListener 기본 구현: 필요한 콜백만 구현해도 나머지는 아무 일도 하지 않아야 한다 */
class NetworkEventsTest {

    @Test
    void networkEvents_defaultsDoNothing() {
        NetworkEvents events = new NetworkEvents() {
        };

        assertDoesNotThrow(() -> {
            events.showConnectionStatus("status");
            events.showError("error");
            events.onStateChanged(NetworkController.State.CONNECTED);
            events.onConfigChanged(new MessagePayload.Config("NORMAL", null, 0));
            events.onPeerReadyChanged(true);
            events.onBattleStarting(1000);
            events.onBattleStarted();
            events.onPauseChanged(true);
            events.onBattleFinished(new MessagePayload.Result(null, true, "TIME_UP"));
            events.onRematchRequested();
            events.onRematchAnswered(false);
            events.onLagChanged(300, true);
            events.onConnectionClosed("closed", false);
        });
    }

    @Test
    void networkListener_latencyDefaultDoesNothing() {
        NetworkListener listener = new NetworkListener() {
            @Override
            public void onConnected() {
            }

            @Override
            public void onMessage(NetworkMessage message) {
            }

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onError(String message) {
            }
        };

        assertDoesNotThrow(() -> listener.onLatencyChanged(10, false));
    }
}
