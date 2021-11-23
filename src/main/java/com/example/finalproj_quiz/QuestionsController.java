package com.example.finalproj_quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Controller
public class QuestionsController {

    @Autowired
    ObjectMapper mapper;

    // int variables
    private int playerCounter;
    private int questionNumber;
    private int numberOfQuestions;
    private int quizCode;
    private int answerCounter;

    // boolean variables
    private boolean isReady = false;
    private boolean isFinalQuestion = false;
    private boolean isFuzz = false;
    private boolean isAnsweredCorrectly = false;
    private boolean isRemote = false;
    private boolean showNextQuestion = false;


    // object variables
    private Player player;

    // list, array, hashmap variables
    private Questions[] questions;
    private List<Player> listOfPlayers = new ArrayList<>();
    private HashMap<String, Integer> scoreboard = new HashMap<>();
    private List<Integer> fuzzModeScoreList = new ArrayList<>();
    private HashMap<String, Integer> lastScoresMap = new HashMap<>();



    //------------------------------------------------------------------------------------------------------------------


    // front page
    @GetMapping("/")
    public String frontPage(Model model){
        return "front_page";
    }


    //----------------------------------------------- admin only -------------------------------------------------------


    // admin only : initializes and clears anything needed for the quiz
    @GetMapping("/register-quiz")
    public String initializeQuiz(Model model, HttpSession session) {

        model.addAttribute("categoriesList", categoriesList());
        restartQuiz();
        session.removeAttribute("correctAnswerText");

        return "admin_first_page";
    }

    // admin only : creates quiz and sets role "admin" to the player object in session
    @PostMapping("/register-quiz")
    public String registerQuiz(@RequestParam(required = false, defaultValue = "10") Integer inputNumberOfQuestions,
                               HttpSession session,
                               @RequestParam(required = false) List<String> category,
                               @RequestParam(required = false) boolean isFuzz,
                               @RequestParam(required = false) boolean isRemote,
                               @RequestParam(required = false, defaultValue = "Quiz Master") String name) {
        if (isFuzz == true) {
            this.isFuzz = isFuzz;
        }

        if (isRemote == true){
            this.isRemote = isRemote;
            createNewAdmin(session, name);
            listOfPlayers.add(player);
            scoreboard.put(player.getName(), 0);
            answerCounter = 0;
        }
        else {
            createNewAdmin(session, name);
        }

        session.setAttribute("isFuzz", this.isFuzz);
        session.setAttribute("isRemote", this.isRemote);

        numberOfQuestions = inputNumberOfQuestions;

        if(category != null){
            questions = getQuizWithCategory(category, inputNumberOfQuestions);
        }else{
            questions = getQuiz(inputNumberOfQuestions);
        }

        return "redirect:/play/" + quizCode;
    }




    //----------------------------------------------- player only ------------------------------------------------------


    // player only : form to register players
    @GetMapping("/register-players")
    public String registerPlayers(){
        return "register_players";
    }

    // player only : registered players through form and sets role "player" to the player object in session
    @PostMapping("/register-players")
    public String registeredPlayers(@RequestParam String userQuizCode, @RequestParam String name, HttpSession session){

        createNewPlayer(name, session);

        return "redirect:/play/" + userQuizCode;
    }


    //------------------------------------------------------------------------------------------------------------------


    // Start quiz from the generated quiz
    @GetMapping("/play/{quizCode}")
    public String startQuiz(@PathVariable String quizCode, Model model, HttpSession session){

        model.addAttribute("listOfPlayers", listOfPlayers);
        model.addAttribute("quizCode", quizCode);
        model.addAttribute("questionNumber", questionNumber);
        model.addAttribute("isReady", isReady);
        model.addAttribute("isRemote", isRemote);
        model.addAttribute("player", session.getAttribute("player"));


        if (isReady){
            playerCounter++;
            if((!isRemote && playerCounter == listOfPlayers.size()) || (isRemote && playerCounter == listOfPlayers.size()-1)) {
                isReady = false;
            }
            return "redirect:/play/" + quizCode + '/' + questionNumber;
        }

        return "start_quiz";
    }

    // post method when admin has clicked on start quiz
    @PostMapping("/play/{quizCode}")
    public String postStartQuiz(@PathVariable String quizCode, HttpSession session){

        isReady = true;

        if(isRemote && listOfPlayers.size() == 1){
            isReady = false;
        }

        playerCounter = 0;
        session.removeAttribute("correctAnswerText");

        if (isFuzz) {
            fuzzModeScoreList.clear();
            for (int i = listOfPlayers.size(); i >= 0; i--) {
                fuzzModeScoreList.add(i);
            }
        }

        return "redirect:/play/" + quizCode + '/' + questionNumber;
    }


    // display page for each quiz question
    @GetMapping("/play/{quizCode}/{questionNumber}")
    public String questionPage(@PathVariable String quizCode, @PathVariable int questionNumber, Model model, HttpSession session) throws JsonProcessingException {

        showNextQuestion = false;

        if (questionNumber == numberOfQuestions-1) {
            isFinalQuestion = true;
        }

        List<String> alternatives = Arrays.asList("A", "B", "C", "D");
        Collections.shuffle(alternatives);

        model.addAttribute("player", session.getAttribute("player"));
        model.addAttribute("isRemote", isRemote);
        model.addAttribute("isAnsweredCorrectly", isAnsweredCorrectly);
        model.addAttribute("question", mapper.writeValueAsString(questions[questionNumber].getQuestion()).replaceAll("^\"|\"$", "").replaceAll("\\\\", ""));
        model.addAttribute("correctAnswer", alternatives.get(0));

        //To account for a discovered mistake in the API:
        String[] answerTextArray = {mapper.writeValueAsString(questions[questionNumber].getCorrectAnswer()).replaceAll("^\"|\"$", ""),
                                    mapper.writeValueAsString(questions[questionNumber].getIncorrectAnswers()[0]).replaceAll("^\"|\"$", ""),
                                    mapper.writeValueAsString(questions[questionNumber].getIncorrectAnswers()[1]).replaceAll("^\"|\"$", ""),
                                    mapper.writeValueAsString(questions[questionNumber].getIncorrectAnswers()[2]).replaceAll("^\"|\"$", "")
                                    };

        for (int i = 0; i < answerTextArray.length; i++) {
            if (answerTextArray[i].contains("Bæ Hovedr")) {
                answerTextArray[i] = answerTextArray[i].replace("Bæ Hovedr", "Bullock");
            }
        }

        session.setAttribute("correctAnswerText", answerTextArray[0]);

        model.addAttribute(alternatives.get(0), answerTextArray[0]);
        model.addAttribute(alternatives.get(1), answerTextArray[1]);
        model.addAttribute(alternatives.get(2), answerTextArray[2]);
        model.addAttribute(alternatives.get(3), answerTextArray[3]);

        return "question_page";
    }

    // retrieves answers from players
    @PostMapping("/play/{quizCode}/{questionNumber}")
    public String postScore(@PathVariable String quizCode,
                            @PathVariable int questionNumber,
                            HttpSession session, Model model,
                            @RequestParam(required = false) String answer) throws JsonProcessingException {

        String correctAnswer = mapper.writeValueAsString(questions[questionNumber].getCorrectAnswer()).replaceAll("^\"|\"$", "");
        String question = mapper.writeValueAsString(questions[questionNumber].getQuestion()).replaceAll("^\"|\"$", "").replaceAll("\\\\", "");
        player = (Player) session.getAttribute("player");

        model.addAttribute("player", player);
        model.addAttribute("question", question);

        if(isRemote){
            answerCounter++;
        }

        // if player role is player and they answer the question correctly, increase points
        if ((player.getRole().equals("player") || isRemote) && correctAnswer.equals(answer)) {
                int tempScore = scoreboard.get(player.getName());
                if (isFuzz) {
                    scoreboard.put(player.getName(), tempScore + fuzzModeScoreList.get(0));
                    lastScoresMap.put(player.getName(), fuzzModeScoreList.get(0));
                    fuzzModeScoreList.remove(0);
                } else {
                    lastScoresMap.put(player.getName(), 1);
                    scoreboard.put(player.getName(), tempScore + 1);
                }
        } else {
            lastScoresMap.put(player.getName(), 0);
        }

        // returns result page if this is currently the last question
        if (isFinalQuestion){
            return "redirect:/play/" + quizCode + "/calculatingresults";
        }

        // if player role is admin, generate next question
        if(player.getRole().equals("admin")) {
            nextQuestion();
        }

        return "redirect:/play/" + quizCode + "/wait";
    }



    //---------------------------------------------- waiting page ------------------------------------------------------

    @GetMapping("/play/{quizCode}/wait")
    public String waitingPage(@PathVariable String quizCode, Model model, HttpSession session){

        if (answerCounter >= listOfPlayers.size()){
            answerCounter = 0;
            showNextQuestion = true;
        }

        model.addAttribute("showNextQuestion", showNextQuestion);
        model.addAttribute("isRemote", isRemote);
        model.addAttribute("scoreboard", scoreboard);
        model.addAttribute("player", session.getAttribute("player"));
        model.addAttribute("lastScores", lastScoresMap);


        if (isReady){
            playerCounter++;
            if((!isRemote && playerCounter == listOfPlayers.size()) || (isRemote && playerCounter >= listOfPlayers.size()-1)) {
                isReady = false;
            }
            return "redirect:/play/" + quizCode + '/' + questionNumber;
        }

        return "waiting_page";
    }


    @GetMapping("/play/{quizCode}/calculatingresults")
    public String calculatingResults(@PathVariable String quizCode, Model model, HttpSession session){
        model.addAttribute("player", session.getAttribute("player"));
        model.addAttribute("quizCode", quizCode);
        model.addAttribute("isRemote", isRemote);

        return "calculating_results";
    }

    @GetMapping("/play/{quizCode}/results")
    public String results(@PathVariable String quizCode, Model model, HttpSession session){

        player = (Player) session.getAttribute("player");
        model.addAttribute("player", player);

        resetQuestionNumber();

        model.addAttribute("scoreboardList", orderListByName(reversedScoreboardList()));
        sortScoreboard(reversedScoreboardList());

        model.addAttribute("scoreboard", scoreboard);
        model.addAttribute("placementScoreboard", placementScoreboard());
        model.addAttribute("numberOfQuestions", numberOfQuestions);
        model.addAttribute("numberOfPlayers", listOfPlayers.size());
        model.addAttribute("isFuzz", isFuzz);
        model.addAttribute("isRemote", isRemote);

        return "result_page";
    }











    //----------------------------------------------- functions --------------------------------------------------------


    // retrieves quiz as array from api with 'number' amount of questions and returns value
    public Questions[] getQuiz(int number){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Questions[]> quizzes = restTemplate.getForEntity("https://api.trivia.willfry.co.uk/questions?limit=" + (number+10), Questions[].class);
        Questions[] quizBody = quizzes.getBody();

        Questions[] returnQuiz = new Questions[number];
        int index = 0;

        for (Questions question : quizBody) {
            if (index == number) {
                break;
            }
            if (question.getIncorrectAnswers().length > 2) {
                returnQuiz[index] = question;
                index++;
            }
        }
        return returnQuiz;
    }

    // retrieves quiz as array from api with 'number' amount of questions, and the selected categories, and returns value
    public Questions[] getQuizWithCategory(List<String> categories, int number){
        RestTemplate restTemplate = new RestTemplate();
        List<String> newCategories = new ArrayList<>();

        for (String category : categories) {
            newCategories.add(category.replaceAll(" ", "_"));
        }

        String categoriesAsString = newCategories.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")).toLowerCase();
        ResponseEntity<Questions[]> quizzes = restTemplate.getForEntity("https://api.trivia.willfry.co.uk/questions?categories=" + categoriesAsString + "&limit=" + (number+10), Questions[].class);

        Questions[] quizBody = quizzes.getBody();

        Questions[] returnQuiz = new Questions[number];
        int index = 0;

        for (Questions question : quizBody) {
            if (index == number) {
                break;
            }
            if (question.getIncorrectAnswers().length > 2) {
                returnQuiz[index] = question;
                index++;
            }
        }
        return returnQuiz;
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

    // new player
    private void createNewPlayer(String name, HttpSession session) {
        player = new Player(name);
        player.setRole("player");
        listOfPlayers.add(player);
        session.setAttribute("player", player);
        scoreboard.put(player.getName(), 0);
    }

    private void createNewAdmin(HttpSession session, String name) {
        player = new Player(name);
        player.setRole("admin");
        session.setAttribute("player", player);
    }

    // function to reset the quiz variables
    private void restartQuiz() {
        scoreboard.clear();
        listOfPlayers.clear();
        fuzzModeScoreList.clear();
        isReady = false;
        isFinalQuestion = false;
        isFuzz = false;
        questionNumber = 0;
        isRemote = false;
        quizCode = generateRandomQuizCode();
    }

    // list of quiz categories
    public List<String> categoriesList(){
        List<String> categoriesList = new ArrayList<>();
        categoriesList.add("Food and Drink");
        categoriesList.add("Geography");
        categoriesList.add("General Knowledge");
        categoriesList.add("History");
        categoriesList.add("Art and Literature");
        categoriesList.add("Movies");
        categoriesList.add("Music");
        categoriesList.add("Science");
        categoriesList.add("Society and Culture");
        categoriesList.add("Sport and Leisure");

        return categoriesList;
    }



    public List<Map.Entry<String, Integer>> reversedScoreboardList() {
        List<HashMap.Entry<String, Integer>> list = new ArrayList<>(scoreboard.entrySet());

        list.sort(HashMap.Entry.comparingByValue());
        Collections.reverse(list);

        return list;
    }


    public List<String> orderListByName (List<Map.Entry<String, Integer>> list) {
        List<String> orderedListWithNames = new ArrayList<>();

        for (Map.Entry<String, Integer> item : list) {
            orderedListWithNames.add(item.getKey());
        }

        return orderedListWithNames;
    }


    public void sortScoreboard(List<Map.Entry<String, Integer>> list){
        HashMap<String, Integer> sorted = new LinkedHashMap<>();

        for (HashMap.Entry<String, Integer> entry : list) {
            sorted.put(entry.getKey(), entry.getValue());
        }

        scoreboard = sorted;
    }


    public Map<String, Integer> placementScoreboard(){
        List<HashMap.Entry<String, Integer>> tempList = new ArrayList<>(scoreboard.entrySet());
        Map<String, Integer> placementScoreboard = new HashMap<>();

        int playerPlacement = 1;
        placementScoreboard.put(tempList.get(0).getKey(), 1);

        for (int i = 1; i < tempList.size(); i++) {
            if (tempList.get(i).getValue() == tempList.get(i - 1).getValue()) {
                placementScoreboard.put(tempList.get(i).getKey(), playerPlacement);
            } else {
                playerPlacement = i + 1;
                placementScoreboard.put(tempList.get(i).getKey(), playerPlacement);
            }
        }

        return placementScoreboard;
    }

}





