package com.example.finalproj_quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;



@Controller
public class QuestionsController {


    @Autowired
    ObjectMapper mapper;

    private final Questions[] quiz = getQuiz();


    /* Henter ut quiz fra url, returnerer som array */
    public Questions[] getQuiz(){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Questions[]> quizzes = restTemplate.getForEntity("https://api.trivia.willfry.co.uk/questions?limit=10", Questions[].class);
        return quizzes.getBody();
    }




    // Viser spørsmål og 4 svar alternativer med et riktig alternativ
    @GetMapping("/")
    public String showOneQuestion(Model model) throws JsonProcessingException {
        model.addAttribute("entireQuiz", mapper.writeValueAsString(quiz));
        return "questions_page";
    }

    @PostMapping("/register-quiz")
    public String registerQuiz(@RequestParam Integer numberOfQuestions){
        return "redirect:/register_players";
    }

    @GetMapping("/register-players")
    public String registerPlayers(){
        return "register_players";
    }

    @PostMapping("/register-players")
    public String registeredPlayers(@RequestParam String quizCode, @RequestParam String name){
        return "waiting_page";
    }

    @GetMapping("/waiting-page")
    public String waitingPage(){
        return "waiting_page";
    }












            /* model.addAttribute("question", mapper.writeValueAsString(quiz[0].getQuestion()));
        model.addAttribute("A", mapper.writeValueAsString(quiz[0].getCorrectAnswer()));
        model.addAttribute("B", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[0]));
        model.addAttribute("C", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[1]));
        model.addAttribute("D", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[2]));*/
}





