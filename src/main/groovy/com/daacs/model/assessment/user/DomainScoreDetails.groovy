package com.daacs.model.assessment.user

/**
 * Created by chostetter on 7/27/16.
 */
class DomainScoreDetails {
    private int scoreSum = 0;
    private int numQuestions = 0;

    public void incrementNumQuestions(){
        numQuestions++;
    }

    public void incrementNumQuestions(int amount){
        numQuestions += amount;
    }

    public void addScore(int score){
        scoreSum += score;
    }

    public int getScoreSum(){
        return scoreSum;
    }

    public int getNumQuestions(){
        return numQuestions;
    }
}
