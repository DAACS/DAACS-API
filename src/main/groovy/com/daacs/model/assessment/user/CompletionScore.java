package com.daacs.model.assessment.user;
/**
 * Created by chostetter on 7/5/16.
 */
public enum CompletionScore {
    LOW(1), MEDIUM(2), HIGH(3);

    private final int rawVal;
    CompletionScore(int rawVal){
        this.rawVal = rawVal;
    }

    public int getRawVal(){
        return rawVal;
    }

    public static CompletionScore getEnum(String strVal) throws Exception{

        Integer value = Integer.parseInt(strVal);
        for (CompletionScore score : CompletionScore.values()){

            if(score.getRawVal() == value){
                return score;
            }
        }
        throw new IllegalArgumentException("Invalid enum value [" + strVal + "]");
    }
}
