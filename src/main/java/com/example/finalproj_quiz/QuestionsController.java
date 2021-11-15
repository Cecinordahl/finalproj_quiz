package com.example.finalproj_quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Controller
public class QuestionsController {


    @Autowired
    ObjectMapper mapper;

    private Questions[] questions;



    List<Player> listOfPlayers = new ArrayList<>();


    /* Henter ut quiz fra url, returnerer som array */
    public Questions[] getQuiz(int number){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Questions[]> quizzes = restTemplate.getForEntity("https://api.trivia.willfry.co.uk/questions?limit=" + number, Questions[].class);
        return quizzes.getBody();
    }

    @GetMapping("/")
    public String frontPage(){

        return "front_page";
    }




    @GetMapping("/register-quiz")
    public String initializeQuiz(Model model) {
        return "admin_first_page";
    }

    @PostMapping("/register-quiz")
    public String registerQuiz(@RequestParam Integer numberOfQuestions){
        questions = getQuiz(numberOfQuestions);
        return "redirect:/play/" + generateRandomQuizCode();
    }

    // Register players get & post
    @GetMapping("/register-players")
    public String registerPlayers(){
        return "register_players";
    }

    @PostMapping("/register-players")
    public String registeredPlayers(@RequestParam String quizCode, @RequestParam String name){
        Player player = new Player(name);
        listOfPlayers.add(player);
        return "redirect:/play/" + quizCode;
    }


    // Start quiz
    @GetMapping("/play/{quizCode}")
    public String startQuiz(@PathVariable String quizCode, Model model){
        model.addAttribute("listOfPlayers", listOfPlayers);
        return "start_quiz";
    }

    @GetMapping("/waiting-page")
    public String waitingPage(){
        return "waiting_page";
    }

    @GetMapping("/question-page")
    public String questionPage(){
        return "question_page";
    }

    public int generateRandomQuizCode(){
        return ThreadLocalRandom.current().nextInt(1, 1000);
    }












            /*
            model.addAttribute("entireQuiz", mapper.writeValueAsString(quiz));
            model.addAttribute("question", mapper.writeValueAsString(quiz[0].getQuestion()));
        model.addAttribute("A", mapper.writeValueAsString(quiz[0].getCorrectAnswer()));
        model.addAttribute("B", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[0]));
        model.addAttribute("C", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[1]));
        model.addAttribute("D", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[2]));*/
}





