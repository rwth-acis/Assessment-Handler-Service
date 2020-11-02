package i5.las2peer.services.AssessmentHandler.AssessmentContent;

import java.util.ArrayList;

public class NLUAssessment extends Assessment {
	
	private String helpIntent;
	private ArrayList<String> intents;
	private ArrayList<String> hints;
	public NLUAssessment(String quitIntent, ArrayList<String> questions, ArrayList<String> intents, ArrayList<String> hints, String helpIntent) {
		super(quitIntent, questions);
		this.helpIntent = helpIntent;
		this.intents = intents;
		this.hints = hints;
	}
	
	public String getCorrectAnswerIntent() {
		return this.intents.get(this.getCurrentQuestionNumber());
	}
	
	public String getQuestionHint() {
		return this.hints.get(this.getCurrentQuestionNumber());
	}
	
	public String getHelpIntent() {
		return this.helpIntent;
	}
	
}
