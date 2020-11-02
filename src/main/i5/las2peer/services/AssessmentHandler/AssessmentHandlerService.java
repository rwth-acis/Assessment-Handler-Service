package i5.las2peer.services.AssessmentHandler;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import javax.ws.rs.Consumes;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import i5.las2peer.services.AssessmentHandler.AssessmentContent.*;

/**
 * Assessment-Handler-Service
 * 
 * This service was developed as the main part of the "Chat Assessments with Social Bots" bachelor thesis
 * 
 * The main functions of the service are used to do moodle quizzes and so called nlu assessments
 * 
 * 
 */
@Api
@SwaggerDefinition(
		info = @Info(
				title = "Assessment-Handler-Service",
				version = "1.0.0",
				description = "The main service used for the quizbots created with the SBF.",
				termsOfService = "none",
				contact = @Contact(
						name = "Aaron D. Conrardy",
						url = "https://github.com/Aran30",
						email = "aaron30@live.be"),
				license = @License(
						name = "",
						url = "")))
@ServicePath("/AssessmentHandler")
public class AssessmentHandlerService extends RESTService {
    // Used for keeping context between assessment and non-assessment states
    // Key is the channelId
    private static HashMap<String, String> assessmentStarted = new HashMap<String, String>();
   // Used to keep track if the topics were already given for a specific user. The assessment function first gives a list on available topics and then expects an answer. 
    private static HashMap<String, Boolean> topicsProposed = new HashMap<String, Boolean>();  
    // Used to make sure that the same moodle quiz is not being started twice at the same time. You can only start a quiz once on moodle until you submit it. 
    private static HashMap<String, Boolean> topicProcessed = new HashMap<String, Boolean>();
    // Saves the current NLUAssessment object for a specific user
    private static HashMap<String, NLUAssessment> currentNLUAssessment = new HashMap<String, NLUAssessment>();
    // Saves the current Moodle Assessment for a specific user
    private static HashMap<String, MoodleQuiz> currentMoodleAssessment = new HashMap<String, MoodleQuiz>();
    // Keep track of the related channels to a bot. Needed to reset the assessments once a bot gets restarted.
    private static HashMap<String, ArrayList<String>> botChannel = new HashMap<String, ArrayList<String>>();
    
	
	@POST
	@Path("/nluAssessment")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response nluAssessment(String body) {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject bodyJson = (JSONObject) p.parse(body);		
			System.out.println(bodyJson);
			JSONObject response = new JSONObject();
			String channel = bodyJson.getAsString("channel");
			ArrayList<String> channels =  botChannel.get(bodyJson.getAsString("botName"));
			channels.add(channel);
			botChannel.put(bodyJson.getAsString("botName"), channels);
			if(this.assessmentStarted.get(channel) == null){
				// function needs assessmentContent parameter
				if(!(bodyJson.get("assessmentContent") instanceof JSONArray)) {
					JSONArray assessmentContent = new JSONArray();
					assessmentContent.add(bodyJson.get("assessmentContent"));
					bodyJson.put("assessmentContent", assessmentContent);
				}
				JSONArray jsonAssessment = (JSONArray) bodyJson.get("assessmentContent");
				ArrayList<String> assessment = new ArrayList<String>();
				if(jsonAssessment != null) {
					int len = jsonAssessment.size();
					for(int i=0; i<len ; i++){
						assessment.add(jsonAssessment.get(i).toString());
					}
					JSONObject contentJson;
					if(this.topicsProposed.get(channel) == null) {
						String topicNames="";
						int topicNumber = 1;
						for(String content : assessment) {
							 contentJson = (JSONObject) p.parse(content);
							 topicNames += " • " + topicNumber + ". " + contentJson.getAsString("topic") + "\n";
							 topicNumber++;
						}
						if(!topicNames.equals("")) {
							response.put("text", "Select a quiz by responding with the corresponding number/name: \n" + topicNames);
							response.put("closeContext", false);
							this.topicsProposed.put(channel, true);
							return Response.ok().entity(response).build();
						}
						JSONObject error = new JSONObject();
						error.put("text", "There are currently no topics available, try again later!");
						error.put("closeContext", "true");
						return Response.ok().entity(error).build();
					} else {
						
						String chosenTopicNumber = bodyJson.getAsString("msg").split("\\.")[0];
						String similarNames = "";
				        ArrayList<String> similarTopicNames = new ArrayList<String>();
				        String smiliarNames = "";
						int topicCount = 1;
						for(String content : assessment) {
							 contentJson = (JSONObject) p.parse(content);
							if(contentJson.getAsString("topic").toLowerCase().equals(bodyJson.getAsString("msg").toLowerCase()) || chosenTopicNumber.equals(String.valueOf(topicCount))){
								setUpNluAssessment(contentJson, channel, bodyJson.getAsString("quitIntent"), bodyJson.getAsString("helpIntent"));
								this.topicsProposed.remove(channel);
								this.assessmentStarted.put(channel, "true");
								// change to nlu quiz object
								response.put("text", "We will now start the nlu assessment on "+ contentJson.getAsString("topic") + " :)!\n" + this.currentNLUAssessment.get(channel).getCurrentQuestion());							
								response.put("closeContext", "false");
								
								return Response.ok().entity(response).build(); 
							} else if(contentJson.getAsString("topic").toLowerCase().contains(bodyJson.getAsString("msg").toLowerCase())) {
								similarTopicNames.add(contentJson.getAsString("topic"));
								similarNames += " • " + topicCount + ". " + contentJson.getAsString("topic") + "\n";
							}
							topicCount++;
						}
						if(similarTopicNames.size() == 1) {
							bodyJson.put("msg", similarTopicNames.get(0));
							return nluAssessment(bodyJson.toString());
						} else if(similarTopicNames.size() > 1) {
							response.put("text", "Multiple nlu assessments are similar to the name you wrote, which one of these do you want to start?\n" + similarNames);							
							response.put("closeContext", "false");
							return Response.ok().entity(response).build();
						}
						JSONObject error = new JSONObject();
						error.put("text", "Topic with name " + bodyJson.getAsString("topic")+ " not found");
						error.put("closeContext", "true");
						return Response.ok().entity(error).build();
					}
				}
				

			} else {
				System.out.println(bodyJson.getAsString("intent"));
				return Response.ok().entity(continueAssessment(channel, bodyJson.getAsString("intent"), bodyJson, "NLUAssessment")).build();
			}		
			
		} catch (ParseException e) {
			e.printStackTrace();
		}	
		JSONObject error = new JSONObject();
		error.put("text", "Something went wrong");
		error.put("closeContext", "true");
		return Response.ok().entity(error).build();

	}	
	
	
	@POST
	@Path("/nluAssessmentDE")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response nluAssessmentDe(String body) {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject bodyJson = (JSONObject) p.parse(body);		
			System.out.println(bodyJson);
			JSONObject response = new JSONObject();
			String channel = bodyJson.getAsString("channel");
			ArrayList<String> channels =  botChannel.get(bodyJson.getAsString("botName"));
			channels.add(channel);
			botChannel.put(bodyJson.getAsString("botName"), channels);
			if(this.assessmentStarted.get(channel) == null){
				// function needs assessmentContent parameter
				if(!(bodyJson.get("assessmentContent") instanceof JSONArray)) {
					JSONArray assessmentContent = new JSONArray();
					assessmentContent.add(bodyJson.get("assessmentContent"));
					bodyJson.put("assessmentContent", assessmentContent);
				}
				JSONArray jsonAssessment = (JSONArray) bodyJson.get("assessmentContent");
				ArrayList<String> assessment = new ArrayList<String>();
				if(jsonAssessment != null) {
					int len = jsonAssessment.size();
					for(int i=0; i<len ; i++){
						assessment.add(jsonAssessment.get(i).toString());
					}
					JSONObject contentJson;
					if(this.topicsProposed.get(channel) == null) {
						String topicNames="";
						int topicNumber = 1;
						for(String content : assessment) {
							 contentJson = (JSONObject) p.parse(content);
							 topicNames += " • " + topicNumber + ". " + contentJson.getAsString("topic") + "\n";
							 topicNumber++;
						}
						if(!topicNames.equals("")) {
							response.put("text", "Wähle ein Quiz indem du mit der entsprechenden Nummer oder dem entsprechenden Name antwortest:\n" + topicNames);
							response.put("closeContext", false);
							this.topicsProposed.put(channel, true);
							return Response.ok().entity(response).build();
						}
						JSONObject error = new JSONObject();
						error.put("text", "Derzeit sind keine Themen verfügbar, versuche zu einem späteren Zeitpunkt wieder!");
						error.put("closeContext", "true");
						return Response.ok().entity(error).build();
					} else {
						
						String chosenTopicNumber = bodyJson.getAsString("msg").split("\\.")[0];
						String similarNames = "";
				        ArrayList<String> similarTopicNames = new ArrayList<String>();
				        String smiliarNames = "";
						int topicCount = 1;
						for(String content : assessment) {
							 contentJson = (JSONObject) p.parse(content);
							if(contentJson.getAsString("topic").toLowerCase().equals(bodyJson.getAsString("msg").toLowerCase()) || chosenTopicNumber.equals(String.valueOf(topicCount))){
								setUpNluAssessment(contentJson, channel, bodyJson.getAsString("quitIntent"), bodyJson.getAsString("helpIntent"));
								this.topicsProposed.remove(channel);
								this.assessmentStarted.put(channel, "true");
								response.put("text", "Wir starten jetzt das Nlu Assessment über "+ contentJson.getAsString("topic") + " :)!\n" + this.currentNLUAssessment.get(channel).getCurrentQuestion());							
								response.put("closeContext", "false");
								
								return Response.ok().entity(response).build(); 
							} else if(contentJson.getAsString("topic").toLowerCase().contains(bodyJson.getAsString("msg").toLowerCase())) {
								similarTopicNames.add(contentJson.getAsString("topic"));
								similarNames += " • " + topicCount + ". " + contentJson.getAsString("topic") + "\n";
							}
							topicCount++;
						}
						if(similarTopicNames.size() == 1) {
							bodyJson.put("msg", similarTopicNames.get(0));
							return nluAssessmentDe(bodyJson.toString());
						} else if(similarTopicNames.size() > 1) {
							response.put("text", "Mehrere Nlu Assessments entsprechen deiner Antwort, welche von diesen möchtest du denn anfangen?\n" + similarNames);							
							response.put("closeContext", "false");
							return Response.ok().entity(response).build();
						}
						JSONObject error = new JSONObject();
						error.put("text", "Topic with name " + bodyJson.getAsString("topic")+ " not found");
						error.put("closeContext", "true");
						return Response.ok().entity(error).build();
					}
				}
				

			} else {
				System.out.println(bodyJson.getAsString("intent"));
				return Response.ok().entity(continueAssessment(channel, bodyJson.getAsString("intent"), bodyJson, "NLUAssessmentDe")).build();
			}		
			
		} catch (ParseException e) {
			e.printStackTrace();
		}	
		JSONObject error = new JSONObject();
		error.put("text", "Something went wrong");
		error.put("closeContext", "true");
		return Response.ok().entity(error).build();

	}
	
	private void setUpNluAssessment(JSONObject content , String channel, String quitIntent, String helpIntent) {
        int noNum = 0;
        JSONArray Sequence =(JSONArray) content.get("Sequence");
        JSONArray Questions =(JSONArray) content.get("Questions");
        JSONArray Intents =(JSONArray) content.get("Intents");
        JSONArray Hints =(JSONArray) content.get("Hints");
        int length = Questions.size(); 
        int max = 0;
        String[][] assessmentContent = new String[length][4];
        for(int i = 0; i < length ; i++){
            if(Sequence.get(i).equals("")){
                noNum++;   
            } else if(Integer.parseInt(Sequence.get(i).toString()) > max){
                max = Integer.parseInt(Sequence.get(i).toString());    
            } else if(Integer.parseInt(Sequence.get(i).toString()) == max) {
            	Sequence.add(i, String.valueOf(max+1));
            	max++;
            }           
            assessmentContent[i][0] = Sequence.get(i).toString();
            assessmentContent[i][1] = Questions.get(i).toString();
            assessmentContent[i][2] = Intents.get(i).toString();
            assessmentContent[i][3] = Hints.get(i).toString();     
        }
        
        // to fill out the blank sequence slots
        // last blank space will be at last place
        
        
        
        // change to nlu quiz object
        for(int i = length-1; i >= 0 ; i--){
            if(assessmentContent[i][0].equals("")){
            	assessmentContent[i][0] = Integer.toString(max + noNum);
            	noNum--;
            }
        }      
        Arrays.sort(assessmentContent, (a, b) -> Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])));
        ArrayList<String> questions = new ArrayList<String>();
        ArrayList<String> intents = new ArrayList<String>();
        ArrayList<String> hints = new ArrayList<String>();
        for(int i = 0; i < length ; i++){
        	questions.add(assessmentContent[i][1]);
        	intents.add(assessmentContent[i][2]);
        	hints.add(assessmentContent[i][3]);
            System.out.println(assessmentContent[i][0] + " " + assessmentContent[i][1] + " " + assessmentContent[i][2] + " " + assessmentContent[i][3]);
        } 
        NLUAssessment assessment = new NLUAssessment(quitIntent, questions, intents, hints, helpIntent);
        this.currentNLUAssessment.put(channel, assessment);
        this.assessmentStarted.put(channel, "true");	
  		
	}
	
	
    private JSONObject continueAssessment(String channel, String intent, JSONObject triggeredBody, String assessmentType){
    	JSONObject response = new JSONObject();
    	String answer = "";    	
    	response.put("closeContext", "false");
        if(assessmentType.equals("NLUAssessment")) {
        	NLUAssessment assessment = this.currentNLUAssessment.get(channel);
	        if(intent.equals(assessment.getQuitIntent())){
	        	// Additionall check to see if the quit intent was recognized by accident (Writing "e a c" was recognized as quit once...)
	        	answer += "Assessment is over \n" + "You got " + assessment.getMarks() + "/" + assessment.getCurrentQuestionNumber() + " Questions right! \n"; 
	            if(assessment.getMarks() == assessment.getCurrentQuestionNumber()) {
	            	answer += "You got no questions wrong!";
	            } else answer +=  "You got following Questions wrong: \n" + assessment.getWrongQuestions();
	        	this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else if(intent.equals(assessment.getHelpIntent())){
	        	answer+= assessment.getQuestionHint() + "\n";
	        	response.put("closeContext", "false");
	        } else { 
		        if(intent.equals(assessment.getCorrectAnswerIntent())){
		            answer += "Correct Answer! \n";
		            assessment.incrementMark(1);
		        } else {
		        	answer += "Wrong answer :/ \n";
		        	assessment.addWrongQuestion();
		        }
		        assessment.incrementCurrentQuestionNumber();
		        if(assessment.getCurrentQuestionNumber() == assessment.getAssessmentSize()){
		        	if(assessment.getMarks() == assessment.getAssessmentSize()) {
		        		answer += "Assessment is over \n" + "You got " + assessment.getMarks() + "/" + assessment.getAssessmentSize() + "Questions right! \n You got no Questions wrong! \n " + assessment.getWrongQuestions();
		        	} else answer += "Assessment is over \n" + "You got " + assessment.getMarks() + "/" + assessment.getAssessmentSize() + "Questions right! \n You got following Questions wrong: \n " + assessment.getWrongQuestions();
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += assessment.getCurrentQuestion();        
		        }
	        }
	        
        } else if(assessmentType.equals("NLUAssessmentDe")) {
        	NLUAssessment assessment = this.currentNLUAssessment.get(channel);
	        if(intent.equals(assessment.getQuitIntent())){
	        	answer += "Das Assessment ist fertig \n" + "Du hast " + assessment.getMarks() + "/" + assessment.getCurrentQuestionNumber() + " Fragen richtig beantwortet! \n"; 
	            if(assessment.getMarks() == assessment.getCurrentQuestionNumber()) {
	            	answer += "Du hast keine falsche Antworten!";
	            } else answer +=  "Du hast folgende Fragen falsch beantwortet: \n" + assessment.getWrongQuestions();
	        	this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else if(intent.equals(assessment.getHelpIntent())){
	        	answer+= assessment.getQuestionHint() + "\n";
	        	response.put("closeContext", "false");
	        } else { 
		        if(intent.equals(assessment.getCorrectAnswerIntent())){
		            answer += "Richtige Antwort! \n";
		            assessment.incrementMark(1);
		        } else {
		        	answer += "Falsche Antwort :/ \n";
		        	assessment.addWrongQuestion();
		        }
		        assessment.incrementCurrentQuestionNumber();
		        if(assessment.getCurrentQuestionNumber() == assessment.getAssessmentSize()){
		        	if(assessment.getMarks() == assessment.getAssessmentSize()) {
		        		answer += "Das Assessment ist fertig \n" + "Du hast " + assessment.getMarks() + "/" + assessment.getAssessmentSize() + "Fragen richtig beantwortet! \n Du hast keine falsche Antworten! \n " + assessment.getWrongQuestions();
		        	} else answer += "Das Assessment ist fertig \n" + "Du hast " + assessment.getMarks() + "/" + assessment.getAssessmentSize() + "Fragen richtig beantwortet!  \n Du hast keine falsche Antworten: \n " + assessment.getWrongQuestions();
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += assessment.getCurrentQuestion();        
		        }
	        }
	        
        } else if(assessmentType.equals("moodleAssessment")) {
        	MoodleQuiz quiz = this.currentMoodleAssessment.get(channel);
        	String msg = triggeredBody.getAsString("msg");
	        if(intent.equals(quiz.getQuitIntent()) && !quiz.checkIfAnswerToQuestion(msg)) {
	        		answer += "Assessment is over \n" + "Your final mark is *" + quiz.getMarks() + "/" + (quiz.getTotalMarksUntilCurrentQuestion() - quiz.getMarkForCurrentQuestion()) + "* \n";  	
		            if(quiz.getMarks() == ((quiz.getTotalMarksUntilCurrentQuestion() - quiz.getMarkForCurrentQuestion()))) {
		            	answer += "You got no questions wrong!";
		            } else answer += "You got following Questions wrong: \n " + quiz.getWrongQuestions();
		        	
		            this.assessmentStarted.put(channel, null);
		        	
		            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3, quiz.createXAPIForMoodle(false).toString() + "*" + triggeredBody.getAsString("email"));
		            response.put("closeContext", "true");	
	        	
	        	
	        } else { 
	        	
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	if(quiz.getQuestionType().equals("numerical") || quiz.getQuestionType().equals("shortanswer") ) {
	        		 if(quiz.getAnswer().toLowerCase().equals(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	        			quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        		 } else {
	        			 answer += "Wrong answer :/ \n";
	 		        	quiz.addWrongQuestion();
	        		 }
	        	} else if(quiz.getQuestionType().equals("truefalse")) {
	        		if(!("true".contains(msg.toLowerCase()) || "false".contains(msg.toLowerCase())) ) {
	        			answer += "Please answer with \"True\" or \"False\"\n";
        				JSONObject userMistake = new JSONObject();
        				userMistake.put("text", answer);
        				userMistake.put("closeContext", "false");
        				return userMistake;
        				}
	        		 if(quiz.getAnswer().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	 		            quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        		 } else {
	        			answer += "Wrong answer :/ \n";
	 		        	quiz.addWrongQuestion();
	        		 }
	        	} else if(quiz.getQuestionType().equals("multichoice")) {
	        		
	        		if(quiz.getAnswer().split(";").length <= 2) {
	        			System.out.println("A");
	        			if(msg.length() > 1) {
	        				answer += "Please only enter the letter/number corresponding to the given answers!\n";
	        				JSONObject userMistake = new JSONObject();
	        				userMistake.put("text", answer);
	        				userMistake.put("closeContext", "false");
	        				return userMistake;
	        			} else {
	        				if(!quiz.getAnswerPossibilitiesForMCQ().toLowerCase().contains(msg.toLowerCase())) {
	        					answer += "Please only enter the letter/number corresponding to the given answers!\n";
		        				JSONObject userMistake = new JSONObject();
		        				userMistake.put("text", answer);
		        				userMistake.put("closeContext", "false");
		        				return userMistake;
	        				}
		        			if(quiz.getAnswer().toLowerCase().contains(msg.toLowerCase())) {
			        			answer += "Correct Answer! \n";
			        			quiz.incrementMark(quiz.getMarkForCurrentQuestion());
			        		 } else {
			        			answer += "Wrong answer :/ \n";
			 		        	quiz.addWrongQuestion();
			        		 }
	        			}
	        		} else {
	        			String[] multipleAnswers = quiz.getAnswer().split(";");
	        			String[] userAnswers = msg.split("\\s+");
	        			double splitMark = quiz.getMarkForCurrentQuestion()/(multipleAnswers.length-1);
	        			int numberOfCorrectAnswers = 0;
        				for(int j = 0 ; j < userAnswers.length; j++ ){	
        					if(userAnswers[j].length() > 1 || !quiz.getAnswerPossibilitiesForMCQ().toLowerCase().contains(userAnswers[j].toLowerCase())) {
	        					answer += "Please only enter the letters/numbers corresponding to the given answers!\n";
		        				JSONObject userMistake = new JSONObject();
		        				userMistake.put("text", answer);
		        				userMistake.put("closeContext", "false");
		        				return userMistake;
        					}
        				}
	        			for(int i = 0 ; i < multipleAnswers.length -1 ; i++) {
	        				for(int j = 0 ; j < userAnswers.length; j++ ){
	        					if(userAnswers[j].length() > 1 ) {
	        						continue;
	        					} else if(multipleAnswers[i].toLowerCase().contains(userAnswers[j].toLowerCase())) {
	        						numberOfCorrectAnswers++;
	        						break;
	        					}
	        				}
	        			}
	        			if((multipleAnswers.length-1) == userAnswers.length ) {
	        				if(userAnswers.length == numberOfCorrectAnswers) {
	        					answer += "Correct Answer(s)! \n";
	        					quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        				} else if(userAnswers.length > numberOfCorrectAnswers) {
	        					// what if 0 points  ?  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Your answers were all wrong\n";
	        					} else answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        					quiz.incrementMark(splitMark*numberOfCorrectAnswers);
	        					quiz.addWrongQuestion();
	        				} else {
	        					answer += "You somehow managed to get more points than intended\n";
	        					quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        				}
	        			} else if((multipleAnswers.length-1) > userAnswers.length) {
	        				 quiz.addWrongQuestion();
	        				 if(userAnswers.length > numberOfCorrectAnswers) {  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Your answers were all wrong\n";
	        					} else answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        					quiz.incrementMark(splitMark*numberOfCorrectAnswers);
	        				} else if(userAnswers.length == numberOfCorrectAnswers) {
	        					answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s)\n";
	        					quiz.incrementMark(numberOfCorrectAnswers*splitMark);
	        				} else {
	        					answer += "You somehow managed to get more points than intended\n";
	        					quiz.incrementMark(numberOfCorrectAnswers*splitMark);
	        				}
	        			} else if((multipleAnswers.length-1) < userAnswers.length) {
	        				quiz.addWrongQuestion();
	        				// careful here, - points if someone has too many answers
	        				 answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        				 int points = numberOfCorrectAnswers - userAnswers.length; 
	        				 if(points >= 0) {
	        					 quiz.incrementMark(numberOfCorrectAnswers*splitMark);
	        				 }
	        			}	
	        		}
	        	}
	        	if(!quiz.getFeedback().equals("")) {
	        		answer += quiz.getFeedback() + "\n";
	        	}
	        	quiz.incrementCurrentQuestionNumber();
		        if(quiz.getCurrentQuestionNumber() == quiz.getAssessmentSize()){
		        	if(quiz.getMarks() == quiz.getMaxMarks()) {
		        		answer += "Assessment is over \n" + "Your final mark is *" + quiz.getMarks() + "/" + quiz.getMaxMarks() + "* \n You got no Questions wrong! \n ";
		        	} else answer += "Assessment is over \n" + "Your final mark is *" + quiz.getMarks() + "/" + quiz.getMaxMarks() + "*  \n You got following Questions wrong: \n " + quiz.getWrongQuestions();
		            this.assessmentStarted.put(channel, null);
		            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3, quiz.createXAPIForMoodle(true).toString() + "*" + triggeredBody.getAsString("email"));
		            
		            response.put("closeContext", "true");
		        } else {
		            answer += quiz.getCurrentQuestion() + quiz.getPossibilities() ;        
		        }
	        }
        } else if(assessmentType == "moodleAssessmentDe"){
        	MoodleQuiz quiz = this.currentMoodleAssessment.get(channel);
        	String msg = triggeredBody.getAsString("msg");
	        if(intent.equals(quiz.getQuitIntent()) && !quiz.checkIfAnswerToQuestion(msg)) {
	        	
	        		// add check if lrs is actually available...
		        	answer += "Assessment ist fertig \n" + "Dein Endresultat ist *" + quiz.getMarks() + "/" + (quiz.getTotalMarksUntilCurrentQuestion() - quiz.getMarkForCurrentQuestion()) + "* \n";  	
		            if(quiz.getMarks() == (quiz.getTotalMarksUntilCurrentQuestion() - quiz.getMarkForCurrentQuestion())) {
		            	answer += "Du hast keine falsche Antworten!";
		            } else answer += "Du hast folgende Fragen falsch beantwortet: \n " + quiz.getWrongQuestions();
		        	this.assessmentStarted.put(channel, null);
		        	
		        	Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3,quiz.createXAPIForMoodle(false).toString() + "*" + triggeredBody.getAsString("email"));
		            
		            response.put("closeContext", "true");
	        	
	        	
	        } else { 
	        	
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	if(quiz.getQuestionType().equals("numerical") || quiz.getQuestionType().equals("shortanswer") ) {
	        		 if(quiz.getAnswer().toLowerCase().equals(msg.toLowerCase())) {
	        			answer += "Richtige Antwort! \n";
	        			quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        		 } else {
	        			 answer += "Falsche Antwort :/ \n";
	 		        	quiz.addWrongQuestion();
	        		 }
	        	} else if(quiz.getQuestionType().equals("truefalse")) {
	        		if(!("wahr".contains(msg.toLowerCase()) || "falsch".contains(msg.toLowerCase())) ) {
	        			answer += "Bitte antworte nur mit \"Wahr\" oder \"Falsch\"\n";
        				JSONObject userMistake = new JSONObject();
        				userMistake.put("text", answer);
        				userMistake.put("closeContext", "false");
        				return userMistake;
	        		}
	        		 if(quiz.getAnswer().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Richtige Antwort! \n";
	 		            quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        		 } else {
	        			answer += "Falsche Antwort :/ \n";
	 		        	quiz.addWrongQuestion();
	        		 }
	        	} else if(quiz.getQuestionType().equals("multichoice")) {
	        		
	        		if(quiz.getAnswer().split(";").length <= 2) {
	        			if(msg.length() > 1) {
	        				answer += "Bitte antworte nur mit den vorgegebenen Buchstaben/Zahlen!\n";
	        				JSONObject userMistake = new JSONObject();
	        				userMistake.put("text", answer);
	        				userMistake.put("closeContext", "false");
	        				return userMistake;
	        			} else {
	        				System.out.println(quiz.getAnswerPossibilitiesForMCQ());
	        				if(!quiz.getAnswerPossibilitiesForMCQ().toLowerCase().contains(msg.toLowerCase())) {
	        					answer += "Bitte antworte nur mit den vorgegebenen Buchstaben/Zahlen!\n";
		        				JSONObject userMistake = new JSONObject();
		        				userMistake.put("text", answer);
		        				userMistake.put("closeContext", "false");
		        				return userMistake;
	        				}
		        			if(quiz.getAnswer().toLowerCase().contains(msg.toLowerCase())) {
			        			answer += "Richtige Antwort! \n";
			        			quiz.incrementMark(quiz.getMarkForCurrentQuestion());
			        		 } else {
			        			answer += "Falsche Antwort :/ \n";
			 		        	quiz.addWrongQuestion();
			        		 }
	        			}
	        		} else {
	        			String[] multipleAnswers = quiz.getAnswer().split(";");
	        			String[] userAnswers = msg.split("\\s+");
	        			double splitMark = quiz.getMarkForCurrentQuestion()/(multipleAnswers.length-1);
	        			int numberOfCorrectAnswers = 0;
        				for(int j = 0 ; j < userAnswers.length; j++ ){	
        					if(userAnswers[j].length() > 1 || !quiz.getAnswerPossibilitiesForMCQ().toLowerCase().contains(userAnswers[j].toLowerCase())) {
	        					answer += "Bitte antworte nur mit den vorgegebenen Buchstaben/Zahlen!\n";
		        				JSONObject userMistake = new JSONObject();
		        				userMistake.put("text", answer);
		        				userMistake.put("closeContext", "false");
		        				return userMistake;
        					}
        				}
	        			for(int i = 0 ; i < multipleAnswers.length -1 ; i++) {
	        				for(int j = 0 ; j < userAnswers.length; j++ ){
	        					if(userAnswers[j].length() > 1 ) {
	        						continue;
	        					} else if(multipleAnswers[i].toLowerCase().contains(userAnswers[j].toLowerCase())) {
	        						numberOfCorrectAnswers++;
	        						break;
	        					}
	        				}
	        			}
	        			if((multipleAnswers.length-1) == userAnswers.length ) {
	        				if(userAnswers.length == numberOfCorrectAnswers) {
	        					answer += "Richtige Antwort(en)! \n";
	        					quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        				} else if(userAnswers.length > numberOfCorrectAnswers) {
	        					// what if 0 points  ?  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Deine Antworten waren alle falsch\n";
	        					} else answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en) und " + (userAnswers.length-numberOfCorrectAnswers) + " falsche\n";
	        					quiz.incrementMark(splitMark*numberOfCorrectAnswers);
	        					quiz.addWrongQuestion();
	        				} else {
	        					answer += "Du hast mehr Punkte bekommen als vorgegeben?!\n";
	        					quiz.incrementMark(quiz.getMarkForCurrentQuestion());
	        				}
	        			} else if((multipleAnswers.length-1) > userAnswers.length) {
	        				 quiz.addWrongQuestion();
	        				 if(userAnswers.length > numberOfCorrectAnswers) {  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Deine Antworten waren alle falsch\n";
	        					} else answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en) und " + (userAnswers.length-numberOfCorrectAnswers) + " falsche\n";
	        					quiz.incrementMark(splitMark*numberOfCorrectAnswers);
	        				} else if(userAnswers.length == numberOfCorrectAnswers) {
	        					answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en)\n";
	        					quiz.incrementMark(numberOfCorrectAnswers*splitMark);
	        				} else {
	        					answer += "Du hast mehr Punkte bekommen als vorgegeben?!\n";
	        					quiz.incrementMark(numberOfCorrectAnswers*splitMark);
	        				}
	        			} else if((multipleAnswers.length-1) < userAnswers.length) {
	        				quiz.addWrongQuestion();
	        				// careful here, - points if someone has too many answers
	        				 answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en) und " + (userAnswers.length-numberOfCorrectAnswers) + " falsche\n";
	        				 int points = numberOfCorrectAnswers - userAnswers.length; 
	        				 if(points >= 0) {
	        					 quiz.incrementMark(numberOfCorrectAnswers*splitMark);
	        				 }
	        			}	
	        		}
	        	}
	        	if(!(quiz.getFeedback().equals(""))) {
	        		answer += quiz.getFeedback() + "\n";
	        	}
	        	quiz.incrementCurrentQuestionNumber();
		        if(quiz.getCurrentQuestionNumber() == quiz.getAssessmentSize()){
		        	if(quiz.getMarks() == quiz.getAssessmentSize()) {
		        		answer += "Assessment ist fertig \n" + "Dein Endresultat ist *" + quiz.getMarks() + "/" + quiz.getMaxMarks() + "* \n Du hast keine falsche Fragen \n " + quiz.getWrongQuestions();
		        	} else answer += "Assessment ist fertig \n" + "Dein Endresultat ist *" + quiz.getMarks() + "/" + quiz.getMaxMarks() + "*  \n Du hast folgende Fragen falsch beantwortet: \n " + quiz.getWrongQuestions();
		            this.assessmentStarted.put(channel, null);
		            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3, quiz.createXAPIForMoodle(true).toString() + "*" + triggeredBody.getAsString("email"));
		            response.put("closeContext", "true");
		        } else {
		            answer += quiz.getCurrentQuestion() + quiz.getPossibilities() ;        
		        }
	        }
        	
        } else {
        	System.out.println("Assessment type: "+ assessmentType + " not known");
        }
        response.put("text", answer);
        return response;
    }
        
    
    @POST
	@Path("/moodleQuiz")
    @Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response moodleQuiz(String body) throws ParseException {
    	JSONObject error = new JSONObject();
    //	System.out.println(body);
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject triggeredBody = (JSONObject) p.parse(body);
		String channel = triggeredBody.getAsString("channel");
		String wstoken = triggeredBody.getAsString("wstoken");
		ArrayList<String> channels =  botChannel.get(triggeredBody.getAsString("botName"));
		if(channels == null) { // should definitely not be the case
			this.botChannel.put(triggeredBody.getAsString("botName"), new ArrayList<String>());
		} else {
			channels.add(channel);
			botChannel.put(triggeredBody.getAsString("botName"), channels);
		} 
		if(!(triggeredBody.get("courseId") instanceof JSONArray)) {
		//	System.out.println("course id is :");
			JSONArray courseId = new JSONArray();
			courseId.add(triggeredBody.get("courseId"));
			triggeredBody.put("courseId", courseId);
		}
		JSONArray courseIds =(JSONArray) triggeredBody.get("courseId");
	//	System.out.println(triggeredBody.getAsString("msg"));
		String quizid="";
		String attemptId = "";
		if(assessmentStarted.get(channel) == null) {
			if(this.topicsProposed.get(channel) == null) {
				String topicNames="";
				MiniClient client = new MiniClient();
				client.setConnectorEndpoint(triggeredBody.getAsString("LMSURL"));
				System.out.println("Now connecting");
				HashMap<String, String> headers = new HashMap<String, String>();
				String courseid = null;
				int topicNumber = 1;
					for(int courses=0 ; courses < courseIds.size() ; courses++) {
					courseid = courseIds.get(courses).toString();
					ClientResponse result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=core_course_get_contents&courseid=" + courseid + "&moodlewsrestformat=json" , "",
							"", MediaType.APPLICATION_JSON, headers);
					JSONArray resi = (JSONArray) p.parse(result.getResponse());
			        JSONObject res= new JSONObject();
				        for(int i = 0; i < resi.size() ;i++) {
				        	for(int j = 0; j < ((JSONArray)((JSONObject) resi.get(i)).get("modules")).size();j++) {
				        		if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("modname").equals("quiz")){
				        			topicNames+=" • " + topicNumber + ". " +  (((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name")) +"\n";
				        			topicNumber++;
				        		}
				        	}
				        }
					}
					if(topicNames.equals("")) {
						topicNames += "No topic available";
					} else this.topicsProposed.put(channel,true);
					JSONObject answer = new JSONObject();
					answer.put("text","Select a quiz by responding with the corresponding number/name: \n"
							+ topicNames);
					answer.put("closeContext", "false");
					return Response.ok().entity(answer).build();
			} else {
				String chosenTopicNumber = triggeredBody.getAsString("msg").split("\\.")[0];
				String similarNames = "";
				MiniClient client = new MiniClient();
				client.setConnectorEndpoint(triggeredBody.getAsString("LMSURL"));
				System.out.println("Now connecting");
				HashMap<String, String> headers = new HashMap<String, String>();
				String courseid = null;
		        ArrayList<String> similarTopicNames = new ArrayList<String>();
				int topicCount = 1;
				for(int courses=0 ; courses < courseIds.size() ; courses++) {
				courseid = courseIds.get(courses).toString();
				ClientResponse result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=core_course_get_contents&courseid=" + courseid + "&moodlewsrestformat=json" , "",
						"", MediaType.APPLICATION_JSON, headers);
				System.out.println(channel + "\n" + result);
				JSONArray resi = (JSONArray) p.parse(result.getResponse());
		        JSONObject res= new JSONObject();
			        // first for loop for checking if topic exists with corresponding number or exact match with name
			        for(int i = 0; i < resi.size() ;i++) {
			        	for(int j = 0; j < ((JSONArray)((JSONObject) resi.get(i)).get("modules")).size();j++) {
			        		if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("modname").equals("quiz")){
			        			String topicName = ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name");
			        			if(String.valueOf(topicCount).equals(chosenTopicNumber) || topicName.toLowerCase().equals(triggeredBody.getAsString("msg").toLowerCase())) {
			        				this.topicsProposed.remove(channel);
			        				quizid = ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("instance");
			        				if(this.topicProcessed.containsKey(topicName)) {
			        					while(this.topicProcessed.get(topicName)) {
				        					// add catch exception with the parsing and set the bool var to false if error
				        				} 
			        				}
			        				this.topicProcessed.put(topicName, true);
			        				// add error here if quiz is already started
			        				
			        				result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + quizid + "&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        try {	
			        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
			        		        } catch( NullPointerException e ) {
			        		        	this.topicProcessed.put(topicName, false);
			        		        	error.put("text", "Your teacher seems to be currently working on this quiz, maybe try again later");
			        		        	error.put("closeContext", true);
			        		        	return Response.ok().entity(error).build();
			        		        	
			        		        }
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        this.topicProcessed.put(topicName, false);
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_get_attempt_review&attemptid=" + attemptId + "&page=-1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);     
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        String html = "";
			        		        Document doc = Jsoup.parse("<html></html>");
			        		        String questions = "";
			        		        String answers = "";
			        		        String[][] assessment = new String[((JSONArray) res.get("questions")).size()][7];
			        		        for(int k = 0 ; k < ((JSONArray) res.get("questions")).size() ; k++) {
			        		        	html =  ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("html");
			        		        	doc = Jsoup.parse(html);
			        		        	assessment[k][2] = ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("type");
		        		        		if(!assessment[k][2].equals("truefalse") && !assessment[k][2].equals("multichoice") && !assessment[k][2].equals("numerical") && !assessment[k][2].equals("shortanswer")) {
		        		        			assessment[k][2] = "missing"; 
		        		        			System.out.println("A question was skipped due to having an unhandled type");
		        		        			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2,"A question was skipped due to having an unhandled type");
		        		        			continue;
		        		        		}
			        		        	assessment[k][3] = "";
			        		        	assessment[k][4] = "";
			        		        	assessment[k][5] = doc.getElementsByClass("grade").text().split("Marked out of ")[1];
			        		        	if(doc.getElementsByClass("generalfeedback") != null) {
			        		        		assessment[k][6] = doc.getElementsByClass("generalfeedback").text();
			        		        	} else assessment[k][6] = "";
		        		        		questions = "";
		        		        		if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 0) {
		        		        			if(!doc.getElementsByClass("qtext").text().equals("")) {
		        		        				questions +=  "*"+doc.getElementsByClass("qtext").text() + "*\n";
		        		        			}
		        		        		} else {
		        		        			for(int l = 0 ; l < doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() ; l++) {
			        		        			if(!doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("")) {
			        		        				String str = doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).html();
			        		        				// Explanation: Moodle is not really consistent when creating the quizzes. Sometimes, the quiz text was in one single <p> element, other times there were <p> for all sentence, other times <br> came out of nowhere...
			        		        				// Therefore, the solution provided here is not really pretty, but as long as it works....
			        		        				str = str.replace("<br>", "\\n");
			        		        				str = str.replace("&nbsp;", "WhiteSpaceHere");
			        		        				String split  = "";
			        		        				Document replace = Jsoup.parse(str);
			        		        				str = replace.text();
			        		        				for(int f = 0; f < str.split("\\\\n").length ; f++) {
			                                    		System.out.println(f);
			                                    		if(!str.split("\\\\n")[f].equals("")) {
				                                    		if((f+1) == str.split("\\\\n").length) {
				                                    			split += "*" + str.split("\\\\n")[f] + "*";
				                                    		} else split += "*" + str.split("\\\\n")[f] + "* \n ";
				                                    	}
			                                    	}
			        		        				split = split.replace("WhiteSpaceHere", " ");
			        		        				if(!split.equals("")) {
			        		        					questions +=   split + "\n";
			        		        				}
			        		        				}
			        		        			if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("") && doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 1) {
			        		        				if(!doc.getElementsByClass("qtext").text().equals("")) {
			        		        					questions +=  "*"+doc.getElementsByClass("qtext").text() + "*\n";
			        		        				}
			        		        				
			        		        			}			
			        		        		}
		        		        		}
		        		        		assessment[k][0] = questions ;
		        		        		assessment[k][2] = ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("type");
		        		        		// to differentiate between questions with one answer and questions with multiple correct answers
		        		        		if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        			assessment[k][3] += "Select one or more (Separate your answers with a whitespace, e.g : a b): \n";
		        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1] +"\n";
		        		        			assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1];
		        		        		} else {
		        		        			if(assessment[k][2].equals("multichoice")) {
		        		        				assessment[k][3] += "Select one (choose by simply answering with the associated letter/number):\n";
		        		        			} else if(assessment[k][2].equals("truefalse")) {
		        		        				assessment[k][3] += "Select one:\n";
		        		        			}
		        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1] +"\n";
		        		        			if(assessment[k][2].equals("truefalse")) {
		        		        				assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1];
		        		        			} else assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answer is: ")[1];
		        		        			
		        		        		}
		        		        		if(assessment[k][2].equals("multichoice") || assessment[k][2].equals("truefalse")) {
		        		        			// check if answers or answer here ? 
		        		        			Elements multiChoiceAnswers = doc.getElementsByClass("ml-1");
		        		        			for(Element item : multiChoiceAnswers) {
		        		        				assessment[k][3] +=" • "+ item.text() + " \n";
		        		        				System.out.println(item.text() + "\n");
		        		        				if(assessment[k][2].equals("multichoice") ) {
		        		        	//				System.out.println(assessment[k][1] + "is at " + item.text().split("\\.")[0] );
		        		        					if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        					//	System.out.println("correct answer are: " + assessment[k][1]);
		        		        						if(assessment[k][1].contains(item.text().split("\\.",2)[1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				//	System.out.println("asskeement is :" +  assessment[k][4]);
				        		        				}
		        		        					} else {
		        		        						System.out.println("correct answer is: " + assessment[k][1]);
		        		        						if(item.text().split("\\.",2)[1].contains(assessment[k][1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        					//System.out.println("asskeement is :2" +  assessment[k][4]);
				        		        				}
		        		        					}
		        		        				}
		        		        			}
		        		        		}
			        		        		
			        		        }
			        		        ArrayList<String> Questions = new ArrayList<String>();
			        		        ArrayList<String> Answers = new ArrayList<String>();
			        		        ArrayList<String> Possibilities = new ArrayList<String>();
			        		        ArrayList<String> QuestionType = new ArrayList<String>();
			        		        ArrayList<Double> QuestionPoints = new ArrayList<Double>();
			        		        ArrayList<String> Feedback = new ArrayList<String>();
			        		        int maxMark = 0;
			        		        for(int k = 0 ; k < assessment.length ; k++) {
			        		        	if(assessment[k][2].equals("missing")) {
			        		        		continue;
			        		        	}
			        		        	Questions.add(assessment[k][0]);
			        		        	if(assessment[k][2].equals("multichoice")) {
			        		        		Answers.add(assessment[k][4]);
			        		        	} else Answers.add(assessment[k][1]);
			        		        	QuestionPoints.add(Double.parseDouble(assessment[k][5]));
			        		        	Possibilities.add(assessment[k][3]);
			        		        	QuestionType.add(assessment[k][2]);
			        		        	maxMark += Double.parseDouble(assessment[k][5]); 
			        		        	Feedback.add(assessment[k][6]);
			        		        }
			        		        JSONObject actor =  new JSONObject();
			        		        actor.put("mbox", "mailto:" + triggeredBody.getAsString("email"));
			        		        actor.put("objectType", "Agent");
			        		        JSONObject verb = new JSONObject();
			        		        JSONObject display = new JSONObject();
			        		    	display.put("en-US", "completed");
			        		    	verb.put("display", display);
			        		    	verb.put("id", "https://w3id.org/xapi/dod-isd/verbs/completed" );
			        		    	JSONObject object = new JSONObject();
			        		    	JSONObject definition = new JSONObject();
			        		    	JSONObject name = new JSONObject();
			        		    	name.put("en-US", topicName);
			        		    	definition.put("name" , name);
			        		    	object.put("definition", definition);
			        		    	object.put("id", triggeredBody.getAsString("LMSURL") +"/mod/quiz/view.php?id="  + ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("id"));
			        		    	object.put("objectType", "Activity");
			        		    	//statement.put("timestamp","2020-07-07T22:08:45Z" );
			        		        
			        		    	MoodleQuiz moodleQuiz  = new MoodleQuiz(triggeredBody.getAsString("quitIntent"), Questions, QuestionPoints, Answers, Possibilities, QuestionType, maxMark, Feedback, actor, verb, object);
			        		    	
			        		    	this.currentMoodleAssessment.put(channel, moodleQuiz);
			        		        JSONObject response = new JSONObject();
			        		        response.put("text", "We will now start the moodle quiz :) \n " + assessment[0][0] + assessment[0][3]);
			        		        response.put("closeContext" , "false");
			        		        assessmentStarted.put(channel,"true");
			        		        
			        		        return Response.ok().entity(response).build();
			        			} else {
			        				
			        				if(topicName.toLowerCase().contains(triggeredBody.getAsString("msg").toLowerCase())){
			        					similarNames +=" • "+ topicCount + ". " + topicName +"\n";
			        					similarTopicNames.add(topicName);
			        				}
			        				topicCount++;
			        			}
			        		}
			        	}
			        	
			        	// here error if number is not there or the user wants to stop ? 
			        }
				}
				if(!similarNames.equals("")) {
					// not the most efficient way, but at least readable
					System.out.println(similarTopicNames.size());
					if(similarTopicNames.size() == 1) {
						System.out.println("AA");
						triggeredBody.put("msg", similarTopicNames.get(0));
						return moodleQuiz(triggeredBody.toString());
					}
					 error = new JSONObject();
					error.put("text", "Multiple quizzes are similar to the name you wrote, which one of these do you want to start?\n" + similarNames);
					error.put("closeContext" , "false");
					return Response.ok().entity(error).build();
				}	
				error = new JSONObject();
				error.put("text", "Something went wrong when trying to start your quiz. Maybe try again later...");
				this.topicsProposed.remove(channel);
				return Response.ok().entity(error).build();
			}	
		} else {
			System.out.println("Why doesnt this work");
			return Response.ok().entity(continueAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessment")).build();
		}  
	}
       
    @POST
	@Path("/moodleQuizDE")
    @Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response moodleQuizDe(String body) throws ParseException {
    	System.out.println(body);
    	JSONObject error = new JSONObject();
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject triggeredBody = (JSONObject) p.parse(body);
		
		String channel = triggeredBody.getAsString("channel");
		ArrayList<String> channels =  botChannel.get(triggeredBody.getAsString("botName"));
		channels.add(channel);
		botChannel.put(triggeredBody.getAsString("botName"), channels);
		String wstoken = triggeredBody.getAsString("wstoken");
		if(!(triggeredBody.get("courseId") instanceof JSONArray)) {
			System.out.println("course id is :");
			JSONArray courseId = new JSONArray();
			courseId.add(triggeredBody.get("courseId"));
			triggeredBody.put("courseId", courseId);
		}
		JSONArray courseIds =(JSONArray) triggeredBody.get("courseId");
		System.out.println(triggeredBody.getAsString("msg"));
		String quizid="";
		String attemptId = "";
		if(assessmentStarted.get(channel) == null) {
			if(this.topicsProposed.get(channel) == null) {
				String topicNames="";
				MiniClient client = new MiniClient();
				client.setConnectorEndpoint(triggeredBody.getAsString("LMSURL"));
				System.out.println("Now connecting");
				HashMap<String, String> headers = new HashMap<String, String>();
				String courseid = null;
				int topicNumber = 1;
					for(int courses=0 ; courses < courseIds.size() ; courses++) {
					courseid = courseIds.get(courses).toString();
					ClientResponse result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=core_course_get_contents&courseid=" + courseid + "&moodlewsrestformat=json" , "",
							"", MediaType.APPLICATION_JSON, headers);
					JSONArray resi = (JSONArray) p.parse(result.getResponse());
			        JSONObject res= new JSONObject();
				        for(int i = 0; i < resi.size() ;i++) {
				        	for(int j = 0; j < ((JSONArray)((JSONObject) resi.get(i)).get("modules")).size();j++) {
				        		if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("modname").equals("quiz")){
				        			topicNames+=" • " + topicNumber + ". " +  (((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name")) +"\n";
				        			topicNumber++;
				        		}
				        	}
				        }
					}
					if(topicNames.equals("")) {
						topicNames += "No topic available";
					} else this.topicsProposed.put(channel,true);
					JSONObject answer = new JSONObject();
					answer.put("text","Wähle ein Quiz indem du mit der entsprechenden Nummer oder dem entsprechenden Name antwortest:\n"
							+ topicNames);
					answer.put("closeContext", "false");
					return Response.ok().entity(answer).build();
			} else {
				String chosenTopicNumber = triggeredBody.getAsString("msg").split("\\.")[0];
				String similarNames = "";
				MiniClient client = new MiniClient();
				client.setConnectorEndpoint(triggeredBody.getAsString("LMSURL"));
				System.out.println("Now connecting");
				HashMap<String, String> headers = new HashMap<String, String>();
				String courseid = null;
		        ArrayList<String> similarTopicNames = new ArrayList<String>();
				int topicCount = 1;
				for(int courses=0 ; courses < courseIds.size() ; courses++) {
				courseid = courseIds.get(courses).toString();
				ClientResponse result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=core_course_get_contents&courseid=" + courseid + "&moodlewsrestformat=json" , "",
						"", MediaType.APPLICATION_JSON, headers);
				System.out.println(channel + "\n" + result);
				JSONArray resi = (JSONArray) p.parse(result.getResponse());
		        JSONObject res= new JSONObject();
			        // first for loop for checking if topic exists with corresponding number or exact match with name
			        for(int i = 0; i < resi.size() ;i++) {
			        	for(int j = 0; j < ((JSONArray)((JSONObject) resi.get(i)).get("modules")).size();j++) {
			        		if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("modname").equals("quiz")){
			        			String topicName = ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name");
			        			if(String.valueOf(topicCount).equals(chosenTopicNumber) || topicName.toLowerCase().equals(triggeredBody.getAsString("msg").toLowerCase())) {
			        				this.topicsProposed.remove(channel);
			        				quizid = ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("instance");
			        				if(this.topicProcessed.containsKey(topicName)) {
			        					while(this.topicProcessed.get(topicName)) {
				        					// add catch exception with the parsing and set the bool var to false if error
				        				} 
			        				}
			        				this.topicProcessed.put(topicName, true);
			        				result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + quizid + "&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        try {	
			        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
			        		        } catch( NullPointerException e ) {
			        		        	this.topicProcessed.put(topicName, false);
			        		        	error.put("text", "Dein Lehrer scheint gerade an diesem Quiz zu arbeiten. Versuchen es später vielleicht noch einmal");
			        		        	error.put("closeContext", true);
			        		        	return Response.ok().entity(error).build();
			        		        }
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        this.topicProcessed.put(topicName, false);
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_get_attempt_review&attemptid=" + attemptId + "&page=-1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);     
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        String html = "";
			        		        Document doc = Jsoup.parse("<html></html>");
			        		        String questions = "";
			        		        String answers = "";
			        		        String[][] assessment = new String[((JSONArray) res.get("questions")).size()][7];
			        		        for(int k = 0 ; k < ((JSONArray) res.get("questions")).size() ; k++) {
			        		        	html =  ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("html");
			        		        	doc = Jsoup.parse(html);
			        		        	assessment[k][2] = ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("type");
		        		        		if(!assessment[k][2].equals("truefalse") && !assessment[k][2].equals("multichoice") && !assessment[k][2].equals("numerical") && !assessment[k][2].equals("shortanswer")) {
		        		        			assessment[k][2] = "missing"; 
		        		        			System.out.println("A question was skipped due to having an unhandled type");
		        		        			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2,"A question was skipped due to having an unhandled type");
		        		        			continue;
		        		        		}
			        		        	assessment[k][3] = "";
			        		        	assessment[k][4] = "";
			        		        	assessment[k][5] = doc.getElementsByClass("grade").text().split("Marked out of ")[1];
			        		        	if(doc.getElementsByClass("generalfeedback") != null) {
			        		        		assessment[k][6] = doc.getElementsByClass("generalfeedback").text();
			        		        	} else assessment[k][6] = "";
		        		        		questions = "";
		        		        		if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 0) {
		        		        			questions = "*"+doc.getElementsByClass("qtext").text() + "*\n";
		        		        		} else {
		        		        			for(int l = 0 ; l < doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() ; l++) {
			        		        			if(!doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("")) {
			        		        				String str = doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).html();
			        		        				// Explanation: Moodle is not really consistent when creating the quizzes. Sometimes, the quiz text was in one single <p> element, other times there were <p> for all sentence, other times <br> came out of nowhere...
			        		        				// Therefore, the solution provided here is not really pretty, but as long as it works....
			        		        				str = str.replace("<br>", "\\n");
			        		        				str = str.replace("&nbsp;", "WhiteSpaceHere");
			        		        				String split  = "";
			        		        				Document replace = Jsoup.parse(str);
			        		        				str = replace.text();
			        		        				for(int f = 0; f < str.split("\\\\n").length ; f++) {
			                                    		System.out.println(f);
			                                    		if(!str.split("\\\\n")[f].equals("")) {
				                                    		if((f+1) == str.split("\\\\n").length) {
				                                    			split += "*" + str.split("\\\\n")[f] + "*";
				                                    		} else split += "*" + str.split("\\\\n")[f] + "* \n ";
				                                    	}
			                                    	}
			        		        				split = split.replace("WhiteSpaceHere", " ");
			        		        				if(!split.equals("")) {
			        		        					questions +=   split + "\n";
			        		        				}
			        		        				}
			        		        			if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("") && doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 1) {
			        		        				if(!doc.getElementsByClass("qtext").text().equals("")) {
			        		        					questions +=  "*"+doc.getElementsByClass("qtext").text() + "*\n";
			        		        				}
			        		        				
			        		        			}			
			        		        		}
		        		        		}
		        		        		assessment[k][0] = questions ;
		        		    
		        		        		// to differentiate between questions with one answer and questions with multiple correct answers
		        		        		if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        			assessment[k][3] += "Wähle eine oder mehrere Antworten ( Trenne deine Antworten mit einem Leerzeichen, bsp. : a b): \n";
		        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1] +"\n";
		        		        			assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1];
		        		        		} else {
		        		        			if(assessment[k][2].equals("multichoice")) {
		        		        				assessment[k][3] += "Wähle eine Antwort (Antworte mit dem entsprechenden Buchstaben/Nummer): \n";
		        		        			} else if(assessment[k][2].equals("truefalse")) {
		        		        				assessment[k][3] += "Wähle eine Antwort:\n";
		        		        			}
		        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1] +"\n";
		        		        			if(assessment[k][2].equals("truefalse")) {
		        		        				if(doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1].toLowerCase().contains("true")) {
		        		        					assessment[k][1] = "wahr";
		        		        				} else assessment[k][1] = "falsch";
		        		        				
		        		        			} else assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answer is: ")[1];
		        		        			
		        		        		}
		        		        		if(assessment[k][2].equals("multichoice") || assessment[k][2].equals("truefalse")) {
		        		        			// check if answers or answer here ? 
		        		        			Elements multiChoiceAnswers = doc.getElementsByClass("ml-1");
		        		        			for(Element item : multiChoiceAnswers) {
		        		        				if(assessment[k][2].equals("truefalse")) {
		        		        					if(item.text().toLowerCase().contains("true")) {
		        		        						assessment[k][3] += "• "+ "Wahr" + " \n";
		        		        					} else assessment[k][3] +=" • "+ "Falsch" + " \n";
		        		        				} else assessment[k][3] +=" • "+ item.text() + " \n";
		        		        				if(assessment[k][2].equals("multichoice")) {
		        		        					System.out.println(assessment[k][1] + "is at " + item.text().split("\\.")[0] );
		        		        					if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        						if(assessment[k][1].contains(item.text().split("\\.",2)[1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					} else {
		        		        						if(item.text().split("\\.",2)[1].contains(assessment[k][1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					}
		        		        				}
		        		        			}
		        		        		}
			        		        		
			        		        }
			        		        ArrayList<String> Questions = new ArrayList<String>();
			        		        ArrayList<String> Answers = new ArrayList<String>();
			        		        ArrayList<String> Possibilities = new ArrayList<String>();
			        		        ArrayList<String> QuestionType = new ArrayList<String>();
			        		        ArrayList<Double> QuestionPoints = new ArrayList<Double>();
			        		        ArrayList<String> Feedback = new ArrayList<String>();
			        		        int maxMark = 0;
			        		        for(int k = 0 ; k < assessment.length ; k++) {
			        		        	if(assessment[k][2].equals("missing")) {
			        		        		continue;
			        		        	}
			        		        	Questions.add(assessment[k][0]);
			        		        	if(assessment[k][2].equals("multichoice")) {
			        		        		Answers.add(assessment[k][4]);
			        		        	} else Answers.add(assessment[k][1]);
			        		        	QuestionPoints.add(Double.parseDouble(assessment[k][5]));
			        		        	Possibilities.add(assessment[k][3]);
			        		        	QuestionType.add(assessment[k][2]);
			        		        	maxMark += Double.parseDouble(assessment[k][5]); 
			        		        	Feedback.add(assessment[k][6]);
			        		        }
			        		        JSONObject actor =  new JSONObject();
			        		        actor.put("mbox", "mailto:" + triggeredBody.getAsString("email"));
			        		        actor.put("objectType", "Agent");
			        		        JSONObject verb = new JSONObject();
			        		        JSONObject display = new JSONObject();
			        		    	display.put("en-US", "completed");
			        		    	verb.put("display", display);
			        		    	verb.put("id", "https://w3id.org/xapi/dod-isd/verbs/completed" );
			        		    	JSONObject object = new JSONObject();
			        		    	JSONObject definition = new JSONObject();
			        		    	JSONObject name = new JSONObject();
			        		    	name.put("en-US", topicName);
			        		    	definition.put("name" , name);
			        		    	object.put("definition", definition);
			        		    	object.put("id", triggeredBody.getAsString("LMSURL") +"/mod/quiz/view.php?id="  + ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("id"));
			        		    	object.put("objectType", "Activity");
			        		    	//statement.put("timestamp","2020-07-07T22:08:45Z" );
			        		        
			        		    	MoodleQuiz moodleQuiz  = new MoodleQuiz(triggeredBody.getAsString("quitIntent"), Questions, QuestionPoints, Answers, Possibilities, QuestionType, maxMark, Feedback, actor, verb, object);
			        		    	
			        		    	this.currentMoodleAssessment.put(channel, moodleQuiz);
			        		        JSONObject response = new JSONObject();
			        		        response.put("text", "Wir starten jetzt das Moodle Quiz :) \n " + assessment[0][0] + assessment[0][3]);
			        		        response.put("closeContext" , "false");
			        		        assessmentStarted.put(channel,"true");
			        		        return Response.ok().entity(response).build();
			        			} else {
			        				
			        				if(topicName.toLowerCase().contains(triggeredBody.getAsString("msg").toLowerCase())){
			        					similarNames +=" • "+ topicCount + ". " + topicName +"\n";
			        					similarTopicNames.add(topicName);
			        				}
			        				topicCount++;
			        			}
			        		}
			        	}
			        	
			        	// here error if number is not there or the user wants to stop ? 
			        }
				}
				if(!similarNames.equals("")) {
					// not the most efficient way, but at least readable
					System.out.println(similarTopicNames.size());
					if(similarTopicNames.size() == 1) {
						System.out.println("AA");
						triggeredBody.put("msg", similarTopicNames.get(0));
						return moodleQuizDe(triggeredBody.toString());
					}
					 error = new JSONObject();
					error.put("text", "Mehrere Quizze entsprechen deiner Antwort, welche von diesen möchtest du denn anfangen?\n" + similarNames);
					error.put("closeContext" , "false");
					return Response.ok().entity(error).build();
				}	
				 error = new JSONObject();
				error.put("text", "Etwas ist schief gelaufen, versuche zu einem späteren Zeitpunkt erneut...");
				this.topicsProposed.remove(channel);
				return Response.ok().entity(error).build();
			}	
		} else {
			System.out.println("Why doesnt this work");
			return Response.ok().entity(continueAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessmentDe")).build();
		}  
	}   
    
    @POST
	@Path("/getMoodleTopics")
    @Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response getMoodleTopics(String body) throws ParseException {
    	System.out.println(body); 
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject triggeredBody = (JSONObject) p.parse(body);
		// do i really need channel here ? 
		String channel = triggeredBody.getAsString("channel");
		String wstoken = triggeredBody.getAsString("wstoken");
		if(!(triggeredBody.get("courseId") instanceof JSONArray)) {
			System.out.println("course id is :");
			JSONArray courseId = new JSONArray();
			courseId.add(triggeredBody.get("courseId"));
			triggeredBody.put("courseId", courseId);
		}
		JSONArray courseIds =(JSONArray) triggeredBody.get("courseId");
		
		String quizid="";
		//JSONArray topicNames = new JSONArray();
		String topicNames="";
		MiniClient client = new MiniClient();
		client.setConnectorEndpoint(triggeredBody.getAsString("LMSURL"));
		System.out.println("Now connecting");
		HashMap<String, String> headers = new HashMap<String, String>();
		String courseid = null;
			for(int courses=0 ; courses < courseIds.size() ; courses++) {
			courseid = courseIds.get(courses).toString();
			ClientResponse result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=core_course_get_contents&courseid=" + courseid + "&moodlewsrestformat=json" , "",
					"", MediaType.APPLICATION_JSON, headers);
			System.out.println(channel + "\n" + result);
			JSONArray resi = (JSONArray) p.parse(result.getResponse());
	        JSONObject res= new JSONObject();
	        for(int i = 0; i < resi.size() ;i++) {
	        	for(int j = 0; j < ((JSONArray)((JSONObject) resi.get(i)).get("modules")).size();j++) {
	        		if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("modname").equals("quiz")){
	        			topicNames+= (((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name")) +"\n";
	        		}
	        	}
	        }
			}
			if(topicNames.equals("")) {
				topicNames += "No topic available";
			}	
			JSONObject answer = new JSONObject();
			answer.put("text", topicNames);
			return Response.ok().entity(answer).build();
		   
	}
    
    // Used when a bit is initialized to reset the assessments for a specific bot. Simply removes the channels related to the bot from the HashMaps.
    @POST
	@Path("/reset")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response resetChannels(String body) {
    	JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject bodyJson = (JSONObject) p.parse(body);		
			System.out.println(bodyJson);
			String botId = bodyJson.getAsString("botName");
			if(this.botChannel.get(botId) == null) {
				ArrayList<String> channels = new ArrayList<String>();
				this.botChannel.put(botId, channels);
			} else { ArrayList<String> channelsAssociatedWithBot = this.botChannel.get(botId);
				for (int i = 0; i < channelsAssociatedWithBot.size(); i++) {
					String channel = channelsAssociatedWithBot.get(i);
					assessmentStarted.remove(channel);
		    	 	topicsProposed.remove(channel);
		    	 	currentMoodleAssessment.remove(channel);
		    	 	currentNLUAssessment.remove(channel);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
    	    System.out.println("worked");
    	    JSONObject error = new JSONObject();
    	    return Response.ok().entity(error).build();
	}
    
    
    
    


}