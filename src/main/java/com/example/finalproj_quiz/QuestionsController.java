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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


@Controller
public class QuestionsController {


    @Autowired
    ObjectMapper mapper;

    private boolean isReady = false;
    private Questions[] questions;
    private int questionNumber = 0;
    private Player player;
    private boolean isFinalQuestion = false;
    private int numberOfQuestions;

    private List<Player> listOfPlayers = new ArrayList<>();
    private HashMap<String, Integer> scoreboard = new HashMap<>();

    private int quizCode = generateRandomQuizCode();


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
    public String registerQuiz(@RequestParam Integer inputNumberOfQuestions, HttpSession session){
        numberOfQuestions = inputNumberOfQuestions;

        questions = getQuiz(inputNumberOfQuestions);
        player = new Player();
        player.setRole("admin");
        session.setAttribute("player", player);
        return "redirect:/play/" + quizCode;
    }

    // Register players get & post
    @GetMapping("/register-players")
    public String registerPlayers(){
        return "register_players";
    }

    @PostMapping("/register-players")
    public String registeredPlayers(@RequestParam String userQuizCode, @RequestParam String name, HttpSession session){
        player = new Player(name);
        player.setRole("player");
        listOfPlayers.add(player);
        session.setAttribute("player", player);
        return "redirect:/play/" + userQuizCode;
    }


    // Start quiz
    @GetMapping("/play/{quizCode}")
    public String startQuiz(@PathVariable String quizCode, Model model, HttpSession session){
        model.addAttribute("listOfPlayers", listOfPlayers);
        model.addAttribute("quizCode", quizCode);
        model.addAttribute("questionNumber", questionNumber);
        model.addAttribute("isReady", isReady);
        model.addAttribute("player", session.getAttribute("player"));


        if (isReady){
            isReady = false;
            return "redirect:/play/" + quizCode + '/' + questionNumber;
        }
        return "start_quiz";
    }

    @PostMapping("/play/{quizCode}")
    public String postStartQuiz(@PathVariable String quizCode){
        isReady = true;
        return "redirect:/play/" + quizCode + '/' + questionNumber;
    }



    @GetMapping("/play/{quizCode}/{questionNumber}")
    public String questionPage(@PathVariable int quizCode, @PathVariable int questionNumber, Model model, HttpSession session) throws JsonProcessingException {
        if (questionNumber == numberOfQuestions-1) {
            isFinalQuestion = true;
        }

        model.addAttribute("question", mapper.writeValueAsString(questions[questionNumber].getQuestion()));
        model.addAttribute("player", session.getAttribute("player"));

        List<String> alternatives = Arrays.asList("A", "B", "C", "D");
        Collections.shuffle(alternatives);

        model.addAttribute(alternatives.get(0), mapper.writeValueAsString(questions[questionNumber].getCorrectAnswer()).replaceAll("^\"|\"$", ""));
        model.addAttribute("correctAnswer", alternatives.get(0));

        model.addAttribute(alternatives.get(1), mapper.writeValueAsString(questions[questionNumber].getIncorrectAnswers()[0]).replaceAll("^\"|\"$", ""));
        model.addAttribute(alternatives.get(2), mapper.writeValueAsString(questions[questionNumber].getIncorrectAnswers()[1]).replaceAll("^\"|\"$", ""));
        model.addAttribute(alternatives.get(3), mapper.writeValueAsString(questions[questionNumber].getIncorrectAnswers()[2]).replaceAll("^\"|\"$", ""));

        nextQuestion();
        model.addAttribute("selectedAnswer", null);

        return "question_page";
    }

    @PostMapping("/play/{quizCode}/{questionNumber}")
    public String postScore(@PathVariable int quizCode, @PathVariable int questionNumber, HttpSession session, Model model, @RequestParam String answer){
        model.addAttribute("player", session.getAttribute("player"));

        if (isFinalQuestion){
            isFinalQuestion = false;
            resetQuestionNumber();
            return "result_page";
        }

        return "waiting_page";
    }

    @GetMapping("/play/{quizCode}/wait")
    public String waitingPage(@PathVariable int quizCode, Model model, HttpSession session){
        model.addAttribute("scoreboard", scoreboard);
        model.addAttribute("player", session.getAttribute("player"));
        return "waiting_page";
    }







    // function to increase question number
    public void nextQuestion(){
        questionNumber++;
    }

    // function to reset question number
    public void resetQuestionNumber(){
        questionNumber = 0;
    }

    // function to generate a random number between 1 and 1000 that represents the quiz code
    public int generateRandomQuizCode(){
        return ThreadLocalRandom.current().nextInt(1, 1000);
    }


}





