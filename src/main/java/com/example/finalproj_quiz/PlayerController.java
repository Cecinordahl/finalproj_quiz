package com.example.finalproj_quiz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

public class PlayerController {

    List<Player> listOfPlayers = new ArrayList<>();

    @GetMapping("/register")
    public String registerPlayers(){
        return "register_players";
    }

    @PostMapping("/registered")
    public String registeredPlayer(@RequestParam String name){
        Player player = new Player(name);
        listOfPlayers.add(player);
        return "play_all";
    }



}
