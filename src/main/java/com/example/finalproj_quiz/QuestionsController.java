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
import java.util.stream.Collectors;


@Controller
public class QuestionsController {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    GameRepository gameRepository;

    //------------------------------------------------------------------------------------------------------------------


    // front page
    @GetMapping("/")
    public String frontPage(){
        return "front_page";
    }


    //----------------------------------------------- admin only -------------------------------------------------------


    // admin only : initializes and clears anything needed for the quiz
    @GetMapping("/register-quiz")
    public String initializeQuiz(Model model, HttpSession session) {

        model.addAttribute("categoriesList", categoriesList());
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
        Game game = new Game();
        game.isFuzz = isFuzz;

        gameRepository.addGame(game);

        Player player;

        if (isRemote == true){
            game.isRemote = true;
            player = createNewAdmin(name);
            game.listOfPlayers.add(player);
            game.scoreboard.put(player.getName(), 0);
            game.answerCounter = 0;
        }
        else {
             player = createNewAdmin(name);
        }
        session.setAttribute("player", player);

        session.setAttribute("isFuzz", game.isFuzz);
        session.setAttribute("isRemote", game.isRemote);

        if(category != null){
            game.questions = getQuizWithCategory(category, inputNumberOfQuestions);
        }else{
            game.questions = getQuiz(inputNumberOfQuestions);
        }



        return "redirect:/play/" + game.quizCode;
    }




    //----------------------------------------------- player only ------------------------------------------------------


    // player only : form to register players
    @GetMapping("/register-players")
    public String registerPlayers(){
        return "register_players";
    }

    // player only : registered players through form and sets role "player" to the player object in session
    @PostMapping("/register-players")
    public String registeredPlayers(@RequestParam Integer userQuizCode, @RequestParam String name, HttpSession session){
        Game game = gameRepository.findByQuizCode(userQuizCode);

        Player player = createNewPlayer(name);
        game.listOfPlayers.add(player);
        session.setAttribute("player", player);
        game.scoreboard.put(player.getName(), 0);
        return "redirect:/play/" + userQuizCode;
    }


    //------------------------------------------------------------------------------------------------------------------


    // Start quiz from the generated quiz
    @GetMapping("/play/{quizCode}")
    public String startQuiz(@PathVariable Integer quizCode, Model model, HttpSession session){
        Game game = gameRepository.findByQuizCode(quizCode);

        Player currentPlayer = (Player) session.getAttribute("player");
        model.addAttribute("listOfPlayers", game.listOfPlayers);
        model.addAttribute("quizCode", quizCode);
        model.addAttribute("player", currentPlayer);
        model.addAttribute("quizCode", quizCode);
        model.addAttribute("isRemote", game.isRemote);


        if (!game.forwardPlayers) {
            return "start_quiz";
        }

        game.addPlayerForwardAndCheckPlayerCounter(currentPlayer);
        return "redirect:/play/" + quizCode + '/' + game.questionNumber;
    }


    @ResponseBody
    @GetMapping("/api/play/{quizCode}")
    public List<Player> waitStart(@PathVariable Integer quizCode) {
        Game game = gameRepository.findByQuizCode(quizCode);
        return game.listOfPlayers;
    }




    // post method when admin has clicked on start quiz
    @PostMapping("/play/{quizCode}")
    public String postStartQuiz(@PathVariable Integer quizCode, HttpSession session){
        Game game = gameRepository.findByQuizCode(quizCode);

        game.forwardPlayers = true;

        if(game.isRemote && game.listOfPlayers.size() == 1){
            System.out.println("setter forwardPlayers til false");
            game.forwardPlayers = false;
        }

        game.playerForwarded.clear();
        session.removeAttribute("correctAnswerText");

        if (game.isFuzz) {
            game.fuzzModeScoreList.clear();
            for (int i = game.listOfPlayers.size(); i >= 0; i--) {
                game.fuzzModeScoreList.add(i);
            }
        }

        return "redirect:/play/" + quizCode + '/' + game.questionNumber;
    }


    // display page for each quiz question
    @GetMapping("/play/{quizCode}/{questionNumber}")
    public String questionPage(@PathVariable Integer quizCode, @PathVariable int questionNumber, Model model, HttpSession session) throws JsonProcessingException {
        Game game = gameRepository.findByQuizCode(quizCode);

        game.showNextQuestion = false;

        if (questionNumber == game.questions.length-1) {
            game.isFinalQuestion = true;
        }

        List<String> alternatives = Arrays.asList("A", "B", "C", "D");
        Collections.shuffle(alternatives);

        model.addAttribute("player", session.getAttribute("player"));
        model.addAttribute("isRemote", game.isRemote);
        model.addAttribute("isAnsweredCorrectly", game.isAnsweredCorrectly);
        model.addAttribute("question", mapper.writeValueAsString(game.questions[questionNumber].getQuestion()).replaceAll("^\"|\"$", "").replaceAll("\\\\", ""));
        model.addAttribute("correctAnswer", alternatives.get(0));

        //To account for a discovered mistake in the API:
        String[] answerTextArray = {mapper.writeValueAsString(game.questions[questionNumber].getCorrectAnswer()).replaceAll("^\"|\"$", ""),
                                    mapper.writeValueAsString(game.questions[questionNumber].getIncorrectAnswers()[0]).replaceAll("^\"|\"$", ""),
                                    mapper.writeValueAsString(game.questions[questionNumber].getIncorrectAnswers()[1]).replaceAll("^\"|\"$", ""),
                                    mapper.writeValueAsString(game.questions[questionNumber].getIncorrectAnswers()[2]).replaceAll("^\"|\"$", "")
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
    public synchronized String postScore(@PathVariable Integer quizCode,
                            @PathVariable int questionNumber,
                            HttpSession session, Model model,
                            @RequestParam(required = false) String answer) throws JsonProcessingException {

        Game game = gameRepository.findByQuizCode(quizCode);

        String correctAnswer = mapper.writeValueAsString(game.questions[questionNumber].getCorrectAnswer()).replaceAll("^\"|\"$", "");
        String question = mapper.writeValueAsString(game.questions[questionNumber].getQuestion()).replaceAll("^\"|\"$", "").replaceAll("\\\\", "");
        Player currentPlayer = (Player) session.getAttribute("player");

        model.addAttribute("player", currentPlayer);
        model.addAttribute("question", question);

        if(game.isRemote){
            game.answerCounter++;
        }

        // if player role is player and they answer the question correctly, increase points
        if ((currentPlayer.getRole().equals("player") || game.isRemote) && correctAnswer.equals(answer)) {
                int tempScore = game.scoreboard.get(currentPlayer.getName());
                if (game.isFuzz) {
                    game.scoreboard.put(currentPlayer.getName(), tempScore + game.fuzzModeScoreList.get(0));
                    game.lastScoresMap.put(currentPlayer.getName(), game.fuzzModeScoreList.get(0));
                    game.fuzzModeScoreList.remove(0);
                } else {
                    game.lastScoresMap.put(currentPlayer.getName(), 1);
                    game.scoreboard.put(currentPlayer.getName(), tempScore + 1);
                }
        } else {
            game.lastScoresMap.put(currentPlayer.getName(), 0);
        }

        // returns result page if this is currently the last question
        if (game.isFinalQuestion){
            return "redirect:/play/" + quizCode + "/calculatingresults";
        }

        // if player role is admin, generate next question
        if(currentPlayer.getRole().equals("admin")) {
            game.questionNumber++;
        }

        return "redirect:/play/" + quizCode + "/wait";
    }



    //---------------------------------------------- waiting page ------------------------------------------------------

    @GetMapping("/play/{quizCode}/wait")
    public String waitingPage(@PathVariable Integer quizCode, Model model, HttpSession session){
        Game game = gameRepository.findByQuizCode(quizCode);



        Player currentPlayer = (Player) session.getAttribute("player");
        model.addAttribute("showNextQuestion", game.showNextQuestion);
        model.addAttribute("isRemote", game.isRemote);
        model.addAttribute("scoreboard", game.scoreboard);
        model.addAttribute("player", session.getAttribute("player"));
        model.addAttribute("lastScores", game.lastScoresMap);

        model.addAttribute("quizCode", quizCode);

        if (!game.forwardPlayers) {
            return "waiting_page";
        }

        game.lastScoresMap.clear();
        game.addPlayerForwardAndCheckPlayerCounter(currentPlayer);
        return "redirect:/play/" + quizCode + '/' + game.questionNumber;
    }

/*-------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
    @ResponseBody
    @GetMapping("/api/play/wait/{quizCode}")
    public Object[] scorebooard(@PathVariable Integer quizCode) {
        Game game = gameRepository.findByQuizCode(quizCode);
        return new Object []{game.scoreboard, game.lastScoresMap};
    }



    @GetMapping("/play/{quizCode}/calculatingresults")
    public String calculatingResults(@PathVariable Integer quizCode, Model model, HttpSession session){
        Game game = gameRepository.findByQuizCode(quizCode);

        model.addAttribute("player", session.getAttribute("player"));
        model.addAttribute("quizCode", quizCode);
        model.addAttribute("isRemote", game.isRemote);

        return "calculating_results";
    }

    @GetMapping("/play/{quizCode}/results")
    public String results(@PathVariable Integer quizCode, Model model, HttpSession session){
        Game game = gameRepository.findByQuizCode(quizCode);


        Player player = (Player) session.getAttribute("player");
        model.addAttribute("player", player);

        game.questionNumber = 0;

        model.addAttribute("scoreboardList", orderListByName(reversedScoreboardList(game)));
        sortScoreboard(reversedScoreboardList(game), game);

        model.addAttribute("scoreboard", game.scoreboard);
        model.addAttribute("placementScoreboard", placementScoreboard(game));
        model.addAttribute("numberOfQuestions", game.questions.length);
        model.addAttribute("numberOfPlayers", game.listOfPlayers.size());
        model.addAttribute("isFuzz", game.isFuzz);
        model.addAttribute("isRemote", game.isRemote);



        return "result_page";
    }





    // ------------------------------------------ TEST RESPONSE BODY ---------------------------------------------
    @ResponseBody
    @GetMapping("/api/play/{quizCode}/wait")
    public Object[] wait(@PathVariable Integer quizCode, HttpSession session) {
        Game game = gameRepository.findByQuizCode(quizCode);
        Player player = (Player) session.getAttribute("player");

        if (game.answerCounter >= game.listOfPlayers.size()){
            game.answerCounter = 0;
            game.showNextQuestion = true;
        }
        boolean isAdmin = player.getRole().equals("admin");
        boolean adminAndNotRemote = isAdmin && !game.isRemote;
        boolean adminAndRemoteAndShowNextQuestion = isAdmin && game.isRemote && game.showNextQuestion;

        boolean showNextButton = adminAndNotRemote || adminAndRemoteAndShowNextQuestion;

        if (!game.forwardPlayers) {
            return new Object[]{-1, showNextButton};
        }
        else {
            game.lastScoresMap.clear();
            Player currentPlayer = (Player) session.getAttribute("player");
            game.addPlayerForwardAndCheckPlayerCounter(currentPlayer);
            return new Object[]{game.questionNumber, showNextButton};
        }
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
        System.out.println(Arrays.deepToString(returnQuiz));

        return returnQuiz;
    }





    // new player
    private Player createNewPlayer(String name) {
        Player player = new Player(name);
        player.setRole("player");

        return player;
    }

    private Player createNewAdmin(String name) {
        Player player = new Player(name);
        player.setRole("admin");
        return player;
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



    public List<Map.Entry<String, Integer>> reversedScoreboardList(Game game) {
        List<HashMap.Entry<String, Integer>> list = new ArrayList<>(game.scoreboard.entrySet());

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


    public void sortScoreboard(List<Map.Entry<String, Integer>> list, Game game){
        HashMap<String, Integer> sorted = new LinkedHashMap<>();

        for (HashMap.Entry<String, Integer> entry : list) {
            sorted.put(entry.getKey(), entry.getValue());
        }

        game.scoreboard = sorted;
    }


    public Map<String, Integer> placementScoreboard(Game game){
        List<HashMap.Entry<String, Integer>> tempList = new ArrayList<>(game.scoreboard.entrySet());
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





