package com.example.finalproj_quiz;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GameRepository {

    private Map<Integer, Game> games = new HashMap<>();

    public Game findByQuizCode(int quizCode) {
        return games.get(quizCode);
    }

    public void addGame(Game g) {
        games.put(g.quizCode, g);
    }
}
