package com.example.finalproj_quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;



@Controller
public class QuestionsController {

    RestTemplate restTemplate;
    private Questions questions;

    @Autowired
    ObjectMapper mapper;


    /* Henter ut quiz fra url, returnerer som array */
    public void getQuiz(){
        questions = restTemplate.getForObject("https://api.trivia.willfry.co.uk/questions?limit=10", Questions.class);
    }




/* Viser spørsmål og 4 svar alternativer med et riktig alternativ
    @GetMapping("/")
    public String showOneQuestion(Model model) throws JsonProcessingException {
        Questions[] quiz = getQuiz();

        model.addAttribute("entireQuiz", mapper.writeValueAsString(quiz));
        model.addAttribute("question", mapper.writeValueAsString(quiz[0].getQuestion()));

        model.addAttribute("A", mapper.writeValueAsString(quiz[0].getCorrectAnswer()));
        model.addAttribute("B", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[0]));
        model.addAttribute("C", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[1]));
        model.addAttribute("D", mapper.writeValueAsString(quiz[0].getIncorrectAnswers()[2]));

        return "play_all";
    }

 */



}
