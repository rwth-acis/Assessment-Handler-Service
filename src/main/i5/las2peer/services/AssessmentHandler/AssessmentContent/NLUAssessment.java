package i5.las2peer.services.AssessmentHandler.AssessmentContent;

import java.util.ArrayList;

public class NLUAssessment extends Assessment {
	
	private String helpIntent;
	private ArrayList<String> intents;
	
	public NLUAssessment(String quitIntent, ArrayList<String> questions, ArrayList<String> intents, String helpIntent) {
		super(quitIntent, questions);
		this.helpIntent = helpIntent;
		this.intents = intents;
	}
	
	public String getCorrectAnswerIntent() {
		return this.intents.get(this.getCurrentQuestionNumber());
	}
	
	public String getHelpIntent() {
		return this.helpIntent;
	}
}
