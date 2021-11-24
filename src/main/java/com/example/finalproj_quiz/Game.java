package com.example.finalproj_quiz;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Game {


    // list, array, hashmap variables
    List<Integer> fuzzModeScoreList = new ArrayList<>();
    public HashMap<String, Integer> scoreboard = new HashMap<>();
    public boolean isFuzz;
    public boolean isRemote;
    public boolean forwardPlayers;

    public List<Player> listOfPlayers = new ArrayList<>();

    public HashMap<String, Integer> lastScoresMap = new HashMap<>();
    public Questions[] questions;

    public Set<String> playerForwarded = new HashSet<>();

    public boolean isFinalQuestion = false;
    public boolean isAnsweredCorrectly = false;
    public boolean showNextQuestion = false;

    // int variables
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
}
