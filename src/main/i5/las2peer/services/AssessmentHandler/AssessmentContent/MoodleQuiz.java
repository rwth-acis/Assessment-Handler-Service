package i5.las2peer.services.AssessmentHandler.AssessmentContent;

import java.util.ArrayList;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class MoodleQuiz extends Assessment {
	
	private ArrayList<Double> markForEachQuestion;
	private ArrayList<String> correctAnswers;
	private ArrayList<String> answerPossibilities;
	private ArrayList<String> typeOfEachQuestion;
	private double maxMark;
	private ArrayList<String> feedbackForQuestions;
	private JSONObject actor;
	private JSONObject verb;
	private JSONObject object;
	
	public MoodleQuiz(String quitIntent, ArrayList<String> questions, ArrayList<Double> questionMarks, ArrayList<String> correctAnswers, ArrayList<String> answerPossibilities, ArrayList<String> typeOfEachQuestion, double maxMark, ArrayList<String> feedbackForQuestions, JSONObject actor, JSONObject verb, JSONObject object) {
		super(quitIntent, questions);
		this.markForEachQuestion = questionMarks;
		this.correctAnswers = correctAnswers;
		this.answerPossibilities = answerPossibilities;
		this.typeOfEachQuestion = typeOfEachQuestion;
		this.maxMark = maxMark;
		this.feedbackForQuestions = feedbackForQuestions;
		this.actor = actor;
		this.verb = verb;
		this.object = object;
	}
	
	public double getMarkForCurrentQuestion(){
		return this.markForEachQuestion.get(this.getCurrentQuestionNumber());
	}
	
	public double getMaxMarks() {
		return this.maxMark;
	}
	
	public double getTotalMarksUntilCurrentQuestion(){
		double result = 0;
    	for(int i = 0; i <= this.getCurrentQuestionNumber() ; i++) {
    		result += this.markForEachQuestion.get(i);
    	}
    	return result;
	}
	
	public String getQuestionType(){
		return this.typeOfEachQuestion.get(this.getCurrentQuestionNumber());
	}
	
	public String getAnswer() {
		return this.correctAnswers.get(this.getCurrentQuestionNumber());
	}
	
	public String getPossibilities() {
		return this.answerPossibilities.get(this.getCurrentQuestionNumber());
	}
	
	public String getFeedback() {
		return this.feedbackForQuestions.get(this.getCurrentQuestionNumber());
	}
	
    // returns the letters/numbers used to enumerate the possible answers for the MCQ
    public String getAnswerPossibilitiesForMCQ(){
    	 String answers = this.answerPossibilities.get(this.getCurrentQuestionNumber());
    	 String[] splitLineBreak = answers.split("\\n");
    	 String concat = "";
    	 // i = 1 bcs "select one or more" is part of the string. 
    	 for(int i = 1 ; i < splitLineBreak.length ; i++) {
    		 concat += splitLineBreak[i].split("\\.")[0];
    	 }
    	 return concat;
    }
    
    // Check if user msg was misread as quitIntent
    public boolean checkIfAnswerToQuestion (String msg) {
    	msg = msg.toLowerCase();
    	// check if perhaps answer is similar to true/false, shortanswer or numerical question
    	if("wahr".contains(msg) ||"falsch".contains(msg) || "true".contains(msg) || "false".contains(msg) || this.correctAnswers.get(this.getCurrentQuestionNumber()).toLowerCase().equals(msg)) {
    		return true; 
    	}
    	//check if answer is perhaps answer to MCQ
		String[] userAnswers = msg.split("\\s+");
		for(int j = 0 ; j < userAnswers.length; j++ ){	
			if(userAnswers[j].length() > 1) {
				return false;
			}
		}
    	return true;
    }
	
    
    public JSONObject createXAPIForMoodle(boolean completed) {
    	JSONObject result = new JSONObject();
        result.put("completion", completed);
        JSONObject score = new JSONObject();
        score.put("raw", this.getMarks());
        score.put("min",  0.0);
        if(completed) {
        	score.put("max", this.getMaxMarks());
        } else {
        	score.put("max", (this.getTotalMarksUntilCurrentQuestion() - this.getMarkForCurrentQuestion()));
        }
        if(!score.getAsString("max").equals("0.0")) {	
        	score.put("scaled", this.getMarks()/this.getMaxMarks());
        } else {
        	score.put("scaled", this.getMarks()/(this.getTotalMarksUntilCurrentQuestion() - this.getMarkForCurrentQuestion()));	
        }
        result.put("score", score);
        JSONObject xAPI = new JSONObject();
        xAPI.put("result", result);
 //       xAPI.put("timestamp",java.time.LocalDateTime.now() );
        xAPI.put("actor", this.actor);
        xAPI.put("object", this.object);
        xAPI.put("verb", this.verb);
        return xAPI;
    } 
}

