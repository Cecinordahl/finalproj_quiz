package com.example.finalproj_quiz;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Game {


    public List<Integer> fuzzModeScoreList = new ArrayList<>();
    public List<Player> listOfPlayers = new ArrayList<>();
    public HashMap<String, Integer> scoreboard = new HashMap<>();
    public HashMap<String, Integer> lastScoresMap = new HashMap<>();
    public Set<String> playerForwarded = new HashSet<>();
    public Questions[] questions;

    public boolean isFuzz;
    public boolean isRemote;
    public boolean forwardPlayers;
    public boolean isFinalQuestion = false;
    public boolean isAnsweredCorrectly = false;
    public boolean showNextQuestion = false;

    public int questionNumber;
    public int quizCode;
    public int answerCounter;

    public Game() {
        this.quizCode = generateRandomQuizCode();
    }

    // function to generate a random number between 1 and 1000 that represents the quiz code
    public int generateRandomQuizCode(){
        return ThreadLocalRandom.current().nextInt(1, 1000);
    }




    // function to check if all players have been forwarded to the next question
    public synchronized void addPlayerForwardAndCheckPlayerCounter(Player currentPlayer) {
        this.playerForwarded.add(currentPlayer.getName());
        int playerCounter = this.playerForwarded.size();

        if (this.isRemote && (playerCounter == this.listOfPlayers.size() - 1)) {
            this.forwardPlayers = false;
            this.playerForwarded.clear();
        } else if (!this.isRemote && (playerCounter == this.listOfPlayers.size())) {
            this.forwardPlayers = false;
            this.playerForwarded.clear();
        }
    }

}
