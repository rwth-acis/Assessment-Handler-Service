package i5.las2peer.services.AssessmentHandler.AssessmentContent;

import java.util.ArrayList;

public abstract class Assessment {
	private String topicName;
	private String quitIntent;
	private double marks;
	private int currentQuestion;
	private String currentWrongQuestions;
	private ArrayList<String> questions; 
	
	public Assessment(String quitIntent, ArrayList<String> questions) {
		this.quitIntent = quitIntent;
		this.questions = questions;
		this.marks = 0;
		this.currentQuestion = 0;
		this.currentWrongQuestions = "";
	}

	public String getQuitIntent() {
		return this.quitIntent;
	}
	
	public double getMarks() {
		return this.marks; 
	}
	
	public void incrementMark(double value) {
    	this.marks += (Math.round(value*100.0)/100.0);
    } 
	
	public int getCurrentQuestionNumber() {
		return this.currentQuestion;
	}
	
	public void incrementCurrentQuestionNumber() {
		this.currentQuestion++ ;
	}
	
	public int getAssessmentSize() {
		return this.questions.size();
	} 
	
	public String getWrongQuestions() {
		return this.currentWrongQuestions;	
	}
	
	public void addWrongQuestion(){
	    	this.currentWrongQuestions += questions.get(getCurrentQuestionNumber()) + "\n" ;
	} 
	
	public String getCurrentQuestion() {
		return this.questions.get(this.getCurrentQuestionNumber());
	}
	
	
	
}
