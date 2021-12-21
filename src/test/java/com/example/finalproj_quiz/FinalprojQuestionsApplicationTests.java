package com.example.finalproj_quiz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
class FinalprojQuestionsApplicationTests {

    @Autowired
    GameRepository gameRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void checkGetNums(){
        Game game = new Game();
        gameRepository.addGame(game);
        game.questions = getQuiz(5);

        int nums[] = ThreadLocalRandom.current().ints(0, game.questions[0].getIncorrectAnswers().length - 1).distinct().limit(3).toArray();

        System.out.println(nums);
    }

    @Override
    public String toString() {
        return "FinalprojQuestionsApplicationTests{" +
                "gameRepository=" + gameRepository +
                '}';
    }


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



}
