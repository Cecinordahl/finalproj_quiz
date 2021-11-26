package com.example.finalproj_quiz;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AddPlayerForwardTest {

    public static void main(String[] args) throws InterruptedException {
        Game game = setupGame();

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        for (Player player : game.listOfPlayers){
            executorService.submit(() -> game.addPlayerForwardAndCheckPlayerCounter(player));
        }

        executorService.shutdown();
        if (executorService.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
            printGameState(game);
        }
    }

    @Test
    public void shouldSetForwardAfterAllPlayersAdded() {
        // Arrange
        Game game = setupGame();

        // Act
        for (Player player : game.listOfPlayers) {
            game.addPlayerForwardAndCheckPlayerCounter(player);
            printGameState(game);
        }

        // Assert
        Assertions.assertThat(game.forwardPlayers).isFalse();
        Assertions.assertThat(game.playerForwarded).isEmpty();
    }

    private static Game setupGame() {
        Game game = new Game();
        game.isRemote = false;
        game.isFuzz = false;
        game.forwardPlayers = true; // Admin has set forward to true
        game.listOfPlayers = generatePlayers(5000);
        return game;
    }

    private static void printGameState(Game game) {
        System.out.println("Forward players: " + game.forwardPlayers);
        System.out.println("Player forwarded" + game.playerForwarded);
    }

    private static List<Player> generatePlayers(int numberOfPlayers) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < numberOfPlayers; i++) {
            Player e = new Player();
            e.setName("Player " + i);
            players.add(e);
        }
        return players;
    }

}
