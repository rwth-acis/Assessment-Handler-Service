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

// TODO Describe your own service
/**
 * Assessment-Handler-Service
 * 
 * This service was developed as the main part of the "Chat Assessments with Social Bots" bachelor thesis
 * 
 * The main functions of the service are used to do moodle quiz and a so called nlu assessment
 * 
 * 
 */
// TODO Adjust the following configuration
@Api
@SwaggerDefinition(
		info = @Info(
				title = "Assessment-Handler-Service",
				version = "1.0.0",
				description = "A las2peer Template Service for demonstration purposes.",
				termsOfService = "none",
				contact = @Contact(
						name = "Aaron D. Conrardy",
						url = "https://github.com/Aran30",
						email = "aaron30@live.be"),
				license = @License(
						name = "",
						url = "")))
@ServicePath("/AssessmentHandler")
// TODO Your own service class
public class AssessmentHandlerService extends RESTService {
    // Used for keeping context between assessment and non-assessment states
    // Key is the channelId
    private static HashMap<String, String> assessmentStarted = new HashMap<String, String>();
    // Used to keep track at which question one currently is of the given assessment
    // Key is the channelId
    private static HashMap<String, Integer> currQuestion = new HashMap<String, Integer>();
    // Used to keep the assessment that is currently being done
    // Key is the channelId 
    private static HashMap<String, String[][]> currAssessment = new HashMap<String, String[][]>();
    private static HashMap<String, JSONObject> currentAssessment = new HashMap<String, JSONObject>();
    
    // Used to keep track of the # of correct answers
    private static HashMap<String, Integer> currPoints = new HashMap<String, Integer>();
    // Used to keep track on which Questions were answered wrongly
    private static HashMap<String, String> currCorrectQuestions = new HashMap<String, String>();
    // Used to keep track on how many Questions were answered correctly
    private static HashMap<String, Integer> score = new HashMap<String, Integer>();
    // Used for asking the questions in the right order
    private static HashMap<String, Integer> questionsAsked = new HashMap<String, Integer>();   
    
    private static HashMap<String, Boolean> topicProcessed = new HashMap<String, Boolean>();  
    
    private static boolean attemptStartedOnMoodle = false;
    
    private static HashMap<String, Boolean> topicsProposed = new HashMap<String, Boolean>();
    
	@POST
	@Path("/assessment")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response assessment(String body) {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject bodyJson = (JSONObject) p.parse(body);		
			System.out.println(bodyJson);
			JSONObject response = new JSONObject();
			String channel = bodyJson.getAsString("channel");
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
					for(String content : assessment) {
						System.out.println(content);
						 contentJson = (JSONObject) p.parse(content);
						if(contentJson.getAsString("topic").toLowerCase().equals(bodyJson.getAsString("topic").toLowerCase())){
							setUpNluAssessment(contentJson, channel, bodyJson.getAsString("quitIntent"), bodyJson.getAsString("helpIntent"));
							response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +((JSONArray) this.currentAssessment.get(channel).get("Questions")).get(0));
							
							response.put("closeContext", "false");
							return Response.ok().entity(response).build(); 
						}
					}
					for(String content : assessment) {
						System.out.println(content);
						 contentJson = (JSONObject) p.parse(content);
						if(contentJson.getAsString("topic").toLowerCase().contains(bodyJson.getAsString("topic").toLowerCase())){
						//	setUpAssessment(contentJson, channel);
							setUpNluAssessment(contentJson, channel, bodyJson.getAsString("quitIntent"), bodyJson.getAsString("helpIntent"));
						//	response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +this.currAssessment.get(channel)[0][1]);
							response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +((JSONArray) this.currentAssessment.get(channel).get("Questions")).get(0));
							
							response.put("closeContext", "false");
							return Response.ok().entity(response).build(); 
						}
					}
					
					
					JSONObject error = new JSONObject();
					error.put("text", "Topic with name " + bodyJson.getAsString("topic")+ " not found");
					error.put("closeContext", "true");
					return Response.ok().entity(error).build();
					
				}
				

			} else {
				System.out.println(bodyJson.getAsString("intent"));
				return Response.ok().entity(continueJSONAssessment(channel, bodyJson.getAsString("intent"), bodyJson, "NLUAssessment")).build();
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
        for(int i = length-1; i >= 0 ; i--){
            if(assessmentContent[i][0].equals("")){
            	assessmentContent[i][0] = Integer.toString(max + noNum);
            	noNum--;
            }
        }      
        Arrays.sort(assessmentContent, (a, b) -> Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])));
        Sequence = new JSONArray();
        Questions = new JSONArray();
        Intents = new JSONArray();
        Hints = new JSONArray();
        for(int i = 0; i < length ; i++){
        	Sequence.add(assessmentContent[i][0]);
        	Questions.add(assessmentContent[i][1]);
        	Intents.add(assessmentContent[i][2]);
        	Hints.add(assessmentContent[i][3]);
            System.out.println(assessmentContent[i][0] + " " + assessmentContent[i][1] + " " + assessmentContent[i][2] + " " + assessmentContent[i][3]);
        } 
        JSONObject currAssessmentContent = new JSONObject();
        currAssessmentContent.put("Sequence", Sequence);
        currAssessmentContent.put("Questions", Questions);
        currAssessmentContent.put("Intents", Intents);
        currAssessmentContent.put("Hints", Hints);
        currAssessmentContent.put("currentQuestion", 0);
        currAssessmentContent.put("currentWrongQuestions", "");
        currAssessmentContent.put("currentMark", 0);
        currAssessmentContent.put("quitIntent", quitIntent);
        currAssessmentContent.put("helpIntent", helpIntent);
        this.currentAssessment.put(channel, currAssessmentContent);
        this.assessmentStarted.put(channel, "true");	
  		
	}
	
	
	
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
							response.put("text", "Select a quiz by responding with the corresponding number: \n" + topicNames);
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
								response.put("text", "We will now start the nlu assessment on "+ contentJson.getAsString("topic") + " :)!\n" +((JSONArray) this.currentAssessment.get(channel).get("Questions")).get(0));							
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
				return Response.ok().entity(continueJSONAssessment(channel, bodyJson.getAsString("intent"), bodyJson, "NLUAssessment")).build();
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
	public Response nluAssessmentDE(String body) {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONObject bodyJson = (JSONObject) p.parse(body);		
			System.out.println(bodyJson);
			JSONObject response = new JSONObject();
			String channel = bodyJson.getAsString("channel");
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
							response.put("text", "Wähle ein Quiz indem du mit der entsprechenden Nummer oder dem entsprechenden Name antwortest:\\n" + topicNames);
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
								response.put("text", "Wir starten jetzt das Nlu Assessment über "+ contentJson.getAsString("topic") + " :)!\n" +((JSONArray) this.currentAssessment.get(channel).get("Questions")).get(0));							
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
				return Response.ok().entity(continueJSONAssessment(channel, bodyJson.getAsString("intent"), bodyJson, "NLUAssessmentDe")).build();
			}		
			
		} catch (ParseException e) {
			e.printStackTrace();
		}	
		JSONObject error = new JSONObject();
		error.put("text", "Something went wrong");
		error.put("closeContext", "true");
		return Response.ok().entity(error).build();

	}
	
    private JSONObject continueJSONAssessment(String channel, String intent, JSONObject triggeredBody, String assessmentType){
    	JSONObject response = new JSONObject();
    	String answer = "";
    	int currentQuestionNumber = this.getCurrentQuestionNumber(channel);
    	response.put("closeContext", "false");
        if(assessmentType.equals("NLUAssessment")) {
	        if(intent.equals(this.getQuitIntent(channel))){
	        	// Additionall check to see if the quit intent was recongnized by accident (Writing "e a c" was recognized as quit once...)
	        	// here should not be the entire size but the current number of questions .. 
	        	answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getCurrentQuestionNumber(channel) + " Questions right! \n"; 
	            if(this.getMarks(channel) == this.getCurrentQuestionNumber(channel)) {
	            	answer += "You got no questions wrong!";
	            } else answer +=  "You got following Questions wrong: \n" + this.getWrongQuestions(channel);
	        	this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else if(intent.equals(this.getHelpIntent(channel))){
	        	answer+= ((JSONArray)this.currentAssessment.get(channel).get("Hints")).get(this.getCurrentQuestionNumber(channel)).toString() + "\n";
	        	response.put("closeContext", "false");
	        } else { 
		        if(intent.equals(((JSONArray)this.currentAssessment.get(channel).get("Intents")).get(currentQuestionNumber))){
		            answer += "Correct Answer! \n";
		            this.incrementMark(channel, 1);
		        } else {
		        	answer += "Wrong answer :/ \n";
		        	this.addWrongQuestion(channel);
		        }
		        this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel) == this.getAssessmentSize(channel)) {
		        		answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got no Questions wrong! \n " + this.getWrongQuestions(channel);
		        	} else answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += ((JSONArray)this.currentAssessment.get(channel).get("Questions")).get(this.getCurrentQuestionNumber(channel));        
		        }
	        }
	        
        } else if(assessmentType.equals("NLUAssessmentDe")) {
	        if(intent.equals(this.getQuitIntent(channel))){
	        	// here should not be the entire size but the current number of questions .. 
	        	answer += "Das Assessment ist fertig \n" + "Du hast " + this.getMarks(channel) + "/" + this.getCurrentQuestionNumber(channel) + " Fragen richtig beantwortet! \n"; 
	            if(this.getMarks(channel) == this.getCurrentQuestionNumber(channel)) {
	            	answer += "Du hast keine falsche Antworten!";
	            } else answer +=  "Du hast folgende Fragen falsch beantwortet: \n" + this.getWrongQuestions(channel);
	        	this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else if(intent.equals(this.getHelpIntent(channel))){
	        	answer+= ((JSONArray)this.currentAssessment.get(channel).get("Hints")).get(this.getCurrentQuestionNumber(channel)).toString() + "\n";
	        	response.put("closeContext", "false");
	        } else { 
		        if(intent.equals(((JSONArray)this.currentAssessment.get(channel).get("Intents")).get(currentQuestionNumber))){
		            answer += "Richtige Antwort! \n";
		            this.incrementMark(channel, 1);
		        } else {
		        	answer += "Falsche Antwort :/ \n";
		        	this.addWrongQuestion(channel);
		        }
		        this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel) == this.getAssessmentSize(channel)) {
		        		answer += "Das Assessment ist fertig \n" + "Du hast " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Fragen richtig beantwortet! \n Du hast keine falsche Antworten! \n " + this.getWrongQuestions(channel);
		        	} else answer += "Das Assessment ist fertig \n" + "Du hast " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Fragen richtig beantwortet!  \n Du hast keine falsche Antworten: \n " + this.getWrongQuestions(channel);
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += ((JSONArray)this.currentAssessment.get(channel).get("Questions")).get(this.getCurrentQuestionNumber(channel));        
		        }
	        }
	        
        } else if(assessmentType.equals("moodleAssessment")) {
        	String msg = triggeredBody.getAsString("msg");
	        if(intent.equals(this.getQuitIntent(channel)) && !checkIfAnswerToQuestion(msg, channel)) {
	        	
	        		answer += "Assessment is over \n" + "Your final mark is *" + this.getMarks(channel) + "/" + (this.getTotalMarksUntilCurrentQuestion(channel) - this.getMarkForCurrentQuestion(channel)) + "* \n";  	
		            if(this.getMarks(channel) == ((this.getTotalMarksUntilCurrentQuestion(channel) - this.getMarkForCurrentQuestion(channel)))) {
		            	answer += "You got no questions wrong!";
		            } else answer += "You got following Questions wrong: \n " + this.getWrongQuestions(channel);
		        	
		            this.assessmentStarted.put(channel, null);
		        	
		            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3,this.createXAPIForMoodle(channel, false).toString() + "*" + triggeredBody.getAsString("email"));
		            response.put("closeContext", "true");	
	        	
	        	
	        } else { 
	        	
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	if(this.getQuestionType(channel).equals("numerical") || this.getQuestionType(channel).equals("shortanswer") ) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().equals(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	        			this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        		 } else {
	        			 answer += "Wrong answer :/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("truefalse")) {
	        		if(!("true".contains(msg.toLowerCase()) || "false".contains(msg.toLowerCase())) ) {
	        			answer += "Please answer with \"True\" or \"False\"\n";
        				JSONObject userMistake = new JSONObject();
        				userMistake.put("text", answer);
        				userMistake.put("closeContext", "false");
        				return userMistake;
        				}
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	 		            this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        		 } else {
	        			answer += "Wrong answer :/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("multichoice")) {
	        		
	        		if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().split(";").length <= 2) {
	        			System.out.println("A");
	        			if(msg.length() > 1) {
	        				answer += "Please only enter the letter/number corresponding to the given answers!\n";
	        				JSONObject userMistake = new JSONObject();
	        				userMistake.put("text", answer);
	        				userMistake.put("closeContext", "false");
	        				return userMistake;
	        			} else {
	        				System.out.println(this.getAnswerPossibilitiesForMCQ(channel));
	        				if(!this.getAnswerPossibilitiesForMCQ(channel).toLowerCase().contains(msg.toLowerCase())) {
	        					answer += "Please only enter the letter/number corresponding to the given answers!\n";
		        				JSONObject userMistake = new JSONObject();
		        				userMistake.put("text", answer);
		        				userMistake.put("closeContext", "false");
		        				return userMistake;
	        				}
		        			if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
			        			answer += "Correct Answer! \n";
			        			this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
			        		 } else {
			        			answer += "Wrong answer :/ \n";
			 		        	this.addWrongQuestion(channel);
			        		 }
	        			}
	        		} else {
	        			String[] multipleAnswers = ((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().split(";");
	        			String[] userAnswers = msg.split("\\s+");
	        			double splitMark = this.getMarkForCurrentQuestion(channel)/(multipleAnswers.length-1);
	        			int numberOfCorrectAnswers = 0;
        				for(int j = 0 ; j < userAnswers.length; j++ ){	
        					if(userAnswers[j].length() > 1 || !this.getAnswerPossibilitiesForMCQ(channel).toLowerCase().contains(userAnswers[j])) {
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
	        					this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        				} else if(userAnswers.length > numberOfCorrectAnswers) {
	        					// what if 0 points  ?  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Your answers were all wrong\n";
	        					} else answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        					this.incrementMark(channel, splitMark*numberOfCorrectAnswers);
	        					this.addWrongQuestion(channel);
	        				} else {
	        					answer += "You somehow managed to get more points than intended\n";
	        					this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        				}
	        			} else if((multipleAnswers.length-1) > userAnswers.length) {
	        				 this.addWrongQuestion(channel);
	        				 if(userAnswers.length > numberOfCorrectAnswers) {  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Your answers were all wrong\n";
	        					} else answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        					this.incrementMark(channel, splitMark*numberOfCorrectAnswers);
	        				} else if(userAnswers.length == numberOfCorrectAnswers) {
	        					answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s)\n";
	        					this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				} else {
	        					answer += "You somehow managed to get more points than intended\n";
	        					this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				}
	        			} else if((multipleAnswers.length-1) < userAnswers.length) {
	        				this.addWrongQuestion(channel);
	        				// careful here, - points if someone has too many answers
	        				 answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        				 int points = numberOfCorrectAnswers - userAnswers.length; 
	        				 if(points >= 0) {
	        					 this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				 }
	        			}	
	        		}
	        	}
	        	if(!((JSONArray)this.currentAssessment.get(channel).get("Feedback")).get(this.getCurrentQuestionNumber(channel)).toString().equals("")) {
	        		answer += ((JSONArray)this.currentAssessment.get(channel).get("Feedback")).get(this.getCurrentQuestionNumber(channel)).toString() + "\n";
	        	}
	        	this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel) == this.getAssessmentSize(channel)) {
		        		answer += "Assessment is over \n" + "Your final mark is *" + this.getMarks(channel) + "/" + this.getMaxMarks(channel) + "* \n You got no Questions wrong! \n " + this.getWrongQuestions(channel);
		        	} else answer += "Assessment is over \n" + "Your final mark is *" + this.getMarks(channel) + "/" + this.getMaxMarks(channel) + "*  \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
		            this.assessmentStarted.put(channel, null);
		            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3,this.createXAPIForMoodle(channel, true).toString() + "*" + triggeredBody.getAsString("email"));
		            
		            response.put("closeContext", "true");
		        } else {
		            answer += ((JSONArray)this.currentAssessment.get(channel).get("Questions")).get(this.getCurrentQuestionNumber(channel)).toString() + ((JSONArray)this.currentAssessment.get(channel).get("Possibilities")).get(this.getCurrentQuestionNumber(channel)).toString() ;        
		        }
	        }
        } else if(assessmentType == "moodleAssessmentDe") {
        	String msg = triggeredBody.getAsString("msg");
	        if(intent.equals(this.getQuitIntent(channel)) && !checkIfAnswerToQuestion(msg, channel)) {
	        	
	        		// add check if lrs is actually available...
		        	answer += "Assessment ist fertig \n" + "Dein Endresultat ist *" + this.getMarks(channel) + "/" + (this.getTotalMarksUntilCurrentQuestion(channel) - this.getMarkForCurrentQuestion(channel)) + "* \n";  	
		            if(this.getMarks(channel) == (this.getTotalMarksUntilCurrentQuestion(channel) - this.getMarkForCurrentQuestion(channel))) {
		            	answer += "Du hast keine falsche Antworten!";
		            } else answer += "Du hast folgende Fragen falsch beantwortet: \n " + this.getWrongQuestions(channel);
		        	this.assessmentStarted.put(channel, null);
		        	
		        	Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3,this.createXAPIForMoodle(channel, false).toString() + "*" + triggeredBody.getAsString("email"));
		            
		            response.put("closeContext", "true");
	        	
	        	
	        } else { 
	        	
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	if(this.getQuestionType(channel).equals("numerical") || this.getQuestionType(channel).equals("shortanswer") ) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().equals(msg.toLowerCase())) {
	        			answer += "Richtige Antwort! \n";
	        			this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        		 } else {
	        			 answer += "Falsche Antwort :/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("truefalse")) {
	        		if(!("wahr".contains(msg.toLowerCase()) || "falsch".contains(msg.toLowerCase())) ) {
	        			answer += "Bitte antworte nur mit \"Wahr\" oder \"Falsch\"\n";
        				JSONObject userMistake = new JSONObject();
        				userMistake.put("text", answer);
        				userMistake.put("closeContext", "false");
        				return userMistake;
	        		}
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Richtige Antwort! \n";
	 		            this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        		 } else {
	        			answer += "Falsche Antwort :/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("multichoice")) {
	        		
	        		if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().split(";").length <= 2) {
	        			System.out.println("A");
	        			if(msg.length() > 1) {
	        				answer += "Bitte antworte nur mit den vorgegebenen Buchstaben/Zahlen!\n";
	        				JSONObject userMistake = new JSONObject();
	        				userMistake.put("text", answer);
	        				userMistake.put("closeContext", "false");
	        				return userMistake;
	        			} else {
	        				System.out.println(this.getAnswerPossibilitiesForMCQ(channel));
	        				if(!this.getAnswerPossibilitiesForMCQ(channel).toLowerCase().contains(msg.toLowerCase())) {
	        					answer += "Bitte antworte nur mit den vorgegebenen Buchstaben/Zahlen!\n";
		        				JSONObject userMistake = new JSONObject();
		        				userMistake.put("text", answer);
		        				userMistake.put("closeContext", "false");
		        				return userMistake;
	        				}
		        			if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
			        			answer += "Richtige Antwort! \n";
			        			this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
			        		 } else {
			        			answer += "Falsche Antwort :/ \n";
			 		        	this.addWrongQuestion(channel);
			        		 }
	        			}
	        		} else {
	        			String[] multipleAnswers = ((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().split(";");
	        			String[] userAnswers = msg.split("\\s+");
	        			double splitMark = this.getMarkForCurrentQuestion(channel)/(multipleAnswers.length-1);
	        			int numberOfCorrectAnswers = 0;
        				for(int j = 0 ; j < userAnswers.length; j++ ){	
        					if(userAnswers[j].length() > 1 || !this.getAnswerPossibilitiesForMCQ(channel).toLowerCase().contains(userAnswers[j])) {
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
	        					this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        				} else if(userAnswers.length > numberOfCorrectAnswers) {
	        					// what if 0 points  ?  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Deine Antworten waren alle falsch\n";
	        					} else answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en) und " + (userAnswers.length-numberOfCorrectAnswers) + " falsche\n";
	        					this.incrementMark(channel, splitMark*numberOfCorrectAnswers);
	        					this.addWrongQuestion(channel);
	        				} else {
	        					answer += "Du hast mehr Punkte bekommen als vorgegeben?!\n";
	        					this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        				}
	        			} else if((multipleAnswers.length-1) > userAnswers.length) {
	        				 this.addWrongQuestion(channel);
	        				 if(userAnswers.length > numberOfCorrectAnswers) {  
	        					if(numberOfCorrectAnswers == 0) {
	        						answer += "Deine Antworten waren alle falsch\n";
	        					} else answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en) und " + (userAnswers.length-numberOfCorrectAnswers) + " falsche\n";
	        					this.incrementMark(channel, splitMark*numberOfCorrectAnswers);
	        				} else if(userAnswers.length == numberOfCorrectAnswers) {
	        					answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en)\n";
	        					this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				} else {
	        					answer += "Du hast mehr Punkte bekommen als vorgegeben?!\n";
	        					this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				}
	        			} else if((multipleAnswers.length-1) < userAnswers.length) {
	        				this.addWrongQuestion(channel);
	        				// careful here, - points if someone has too many answers
	        				 answer += "Deine Antwort war teilweise richtig, du hast " + numberOfCorrectAnswers + " richtige Antwort(en) und " + (userAnswers.length-numberOfCorrectAnswers) + " falsche\n";
	        				 int points = numberOfCorrectAnswers - userAnswers.length; 
	        				 if(points >= 0) {
	        					 this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				 }
	        			}	
	        		}
	        	}
	        	if(!((JSONArray)this.currentAssessment.get(channel).get("Feedback")).get(this.getCurrentQuestionNumber(channel)).toString().equals("")) {
	        		answer += ((JSONArray)this.currentAssessment.get(channel).get("Feedback")).get(this.getCurrentQuestionNumber(channel)).toString() + "\n";
	        	}
	        	this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel) == this.getAssessmentSize(channel)) {
		        		answer += "Assessment ist fertig \n" + "Dein Endresultat ist *" + this.getMarks(channel) + "/" + this.getMaxMarks(channel) + "* \n Du hast keine falsche Fragen \n " + this.getWrongQuestions(channel);
		        	} else answer += "Assessment ist fertig \n" + "Dein Endresultat ist *" + this.getMarks(channel) + "/" + this.getMaxMarks(channel) + "*  \n Du hast folgende Fragen falsch beantwortet: \n " + this.getWrongQuestions(channel);
		            this.assessmentStarted.put(channel, null);
		            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3,this.createXAPIForMoodle(channel, true).toString() + "*" + triggeredBody.getAsString("email"));
		            response.put("closeContext", "true");
		        } else {
		            answer += ((JSONArray)this.currentAssessment.get(channel).get("Questions")).get(this.getCurrentQuestionNumber(channel)).toString() + ((JSONArray)this.currentAssessment.get(channel).get("Possibilities")).get(this.getCurrentQuestionNumber(channel)).toString() ;        
		        }
	        }
        } else {
        	System.out.println("Assessment type: "+ assessmentType + " not known");
        }
        response.put("text", answer);
        return response;
    }
    
    private double getMarks(String channel) {
    	return Double.parseDouble(this.currentAssessment.get(channel).getAsString("currentMark"));
    }
    
    private double getMarkForCurrentQuestion(String channel) {
    	return Double.parseDouble(((JSONArray) this.currentAssessment.get(channel).get("QuestionPoints")).get(this.getCurrentQuestionNumber(channel)).toString());
    }
    
    private double getMaxMarks(String channel) {
    	return Double.parseDouble(this.currentAssessment.get(channel).getAsString("maxMark"));
    }
    
    private double getTotalMarksUntilCurrentQuestion(String channel) {
    	double result = 0;
    	for(int i = 0; i <= this.getCurrentQuestionNumber(channel) ; i++) {
    		result += Double.parseDouble(((JSONArray) this.currentAssessment.get(channel).get("QuestionPoints")).get(i).toString());
    	}
    	return result;
    }	
    
    
    private int getAssessmentSize(String channel) {
    	return ((JSONArray)this.currentAssessment.get(channel).get("Questions")).size();
    
    }
    private String getWrongQuestions(String channel) {
    	return this.currentAssessment.get(channel).getAsString("currentWrongQuestions");
    }    
    private int getCurrentQuestionNumber(String channel) {
    	return Integer.parseInt(this.currentAssessment.get(channel).getAsString("currentQuestion"));
    }    
        
    private void incrementMark(String channel, double value) {
    	this.currentAssessment.get(channel).put("currentMark", (this.getMarks(channel)) + (Math.round(value*100.0)/100.0));
    } 
    
    private void addWrongQuestion(String channel) {
    	this.currentAssessment.get(channel).put("currentWrongQuestions", this.getWrongQuestions(channel) + ((JSONArray)this.currentAssessment.get(channel).get("Questions")).get(this.getCurrentQuestionNumber(channel)) + "\n"  );
    } 
    
    private void incrementCounter(String channel) {
    	this.currentAssessment.get(channel).put("currentQuestion", this.getCurrentQuestionNumber(channel) + 1 );
    }
    
    private String getQuestionType(String channel) {
    	return ((JSONArray)this.currentAssessment.get(channel).get("QuestionType")).get(this.getCurrentQuestionNumber(channel)).toString();
    }
    
    private String getQuitIntent(String channel){
    	return this.currentAssessment.get(channel).getAsString("quitIntent");
    }
    
    private String getHelpIntent(String channel){
    	return this.currentAssessment.get(channel).getAsString("helpIntent");
    }
    // returns the letters/numbers used to enumerate the possible answers for the MCQ
    private String getAnswerPossibilitiesForMCQ(String channel) {
    	 String answers = ((JSONArray) this.currentAssessment.get(channel).get("Possibilities")).get(this.getCurrentQuestionNumber(channel)).toString();
    	 String[] splitLineBreak = answers.split("\\n");
    	 String concat = "";
    	 // i = 1 bcs "select one or more" is part of the string. 
    	 for(int i = 1 ; i < splitLineBreak.length ; i++) {
    		 concat += splitLineBreak[i].split("\\.")[0];
    	 }
    	 return concat;
    }
    // Check if user msg was misread as quitIntent
    private boolean checkIfAnswerToQuestion (String msg, String channel) {
    	msg = msg.toLowerCase();
    	// check if perhaps answer is similar to true/false, shortanswer or numerical question
    	if("wahr".contains(msg) ||"falsch".contains(msg) || "true".contains(msg) || "false".contains(msg) || ((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().equals(msg)) {
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
    
    private JSONObject createXAPIForMoodle(String channel, boolean completed) {
    	JSONObject result = new JSONObject();
        result.put("completion", completed);
        JSONObject score = new JSONObject();
        score.put("raw", this.getMarks(channel));
        score.put("min",  0.0);
        if(!score.getAsString("max").equals("0.0")) {
        	if(completed) {
        		score.put("max", this.getMaxMarks(channel));
        		score.put("scaled", this.getMarks(channel)/this.getMaxMarks(channel));
        	} else
        	{
        		score.put("max", (this.getTotalMarksUntilCurrentQuestion(channel) - this.getMarkForCurrentQuestion(channel)));
        		score.put("scaled", this.getMarks(channel)/(this.getTotalMarksUntilCurrentQuestion(channel) - this.getMarkForCurrentQuestion(channel)));
        	}
        	
        }
        result.put("score", score);
        JSONObject xAPI = new JSONObject();
        xAPI.put("result", result);
 //       xAPI.put("timestamp",java.time.LocalDateTime.now() );
        xAPI.put("actor", this.currentAssessment.get(channel).get("actor"));
        xAPI.put("object", this.currentAssessment.get(channel).get("object"));
        xAPI.put("verb", this.currentAssessment.get(channel).get("verb"));
        return xAPI;
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
    	System.out.println(body);
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject triggeredBody = (JSONObject) p.parse(body);
		String channel = triggeredBody.getAsString("channel");
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
					answer.put("text","Select a quiz by responding with the corresponding number: \n"
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
			        				this.attemptStartedOnMoodle = true;	
			        				// add error here if quiz is already started
			        				
			        				result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + quizid + "&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        try {	
			        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
			        		        } catch( NullPointerException e ) {
			        		        	this.topicProcessed.put("topicName", false);
			        		        	this.attemptStartedOnMoodle = false;
			        		        	error.put("text", "Your teacher seems to be currently working on this quiz, maybe try again later");
			        		        	error.put("closeContext", true);
			        		        	return Response.ok().entity(error).build();
			        		        	
			        		        }
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        this.topicProcessed.put(topicName, false);
			        		        this.attemptStartedOnMoodle = false;
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
			        		        				questions +=  "*"+doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text() + "*\n";
			        		        			}
			        		        			if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("") && doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 1) {
			        		        				questions +=  "*"+doc.getElementsByClass("qtext").text() + "*\n";
			        		        			}
			        		        		}
		        		        		}
		        		        		assessment[k][0] = questions ;
		        		        		assessment[k][2] = ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("type");
		        		        		System.out.println(doc.getElementsByClass("qtext").text());
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
		        		        					System.out.println(assessment[k][1] + "is at " + item.text().split("\\.")[0] );
		        		        					if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        						if(assessment[k][1].contains(item.text().split("\\.")[1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					} else {
		        		        						if(item.text().split("\\.")[1].contains(assessment[k][1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					}
		        		        				}
		        		        			}
		        		        		}
			        		        		
			        		        }
			        		        JSONArray Questions = new JSONArray();
			        		        JSONArray Answers = new JSONArray();
			        		        JSONArray Possibilities = new JSONArray();
			        		        JSONArray QuestionType = new JSONArray();
			        		        JSONArray QuestionPoints = new JSONArray();
			        		        JSONArray Feedback = new JSONArray();
			        		        int maxMark = 0;
			        		        for(int k = 0 ; k < assessment.length ; k++) {
			        		        	if(assessment[k][2].equals("missing")) {
			        		        		continue;
			        		        	}
			        		        	Questions.add(assessment[k][0]);
			        		        	if(assessment[k][2].equals("multichoice")) {
			        		        		Answers.add(assessment[k][4]);
			        		        	} else Answers.add(assessment[k][1]);
			        		        	QuestionPoints.add(assessment[k][5]);
			        		        	Possibilities.add(assessment[k][3]);
			        		        	QuestionType.add(assessment[k][2]);
			        		        	maxMark += Double.parseDouble(assessment[k][5]); 
			        		        	Feedback.add(assessment[k][6]);
			        		        }
			        		        JSONObject currAssessmentContent = new JSONObject();
			        		        currAssessmentContent.put("QuestionPoints", QuestionPoints);
			        		        currAssessmentContent.put("Questions", Questions);
			        		        currAssessmentContent.put("Answers", Answers);
			        		        currAssessmentContent.put("Possibilities", Possibilities);
			        		        currAssessmentContent.put("QuestionType", QuestionType);
			        		        currAssessmentContent.put("currentQuestion" , 0);
			        		        currAssessmentContent.put("currentWrongQuestions" ,"");
			        		        currAssessmentContent.put("currentMark", 0);
			        		        currAssessmentContent.put("maxMark", maxMark);
			        		        currAssessmentContent.put("Feedback", Feedback);
			        		        currAssessmentContent.put("quitIntent", triggeredBody.getAsString("quitIntent"));
			        		        
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
			        		    	currAssessmentContent.put("actor", actor);
			        		    	currAssessmentContent.put("object", object);
			        		    	currAssessmentContent.put("verb", verb);
			        		        
			        		        
			        		        this.currentAssessment.put(channel, currAssessmentContent);
			        		        JSONObject response = new JSONObject();
			        		        response.put("text", "We will now start the moodle quiz :) \n " + assessment[0][0] + assessment[0][3]);
			        		        response.put("closeContext" , "false");
			        		        this.score.put(channel, 0);
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
			return Response.ok().entity(continueJSONAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessment")).build();
		}  
	}
    
    // Again will need to make code less redundant, but a bit short on time...
    @POST
	@Path("/moodleQuizWithAnswersDE")
    @Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response moodleQuizwithAnswersDe(String body) throws ParseException {
    	System.out.println(body);
    	JSONObject error = new JSONObject();
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject triggeredBody = (JSONObject) p.parse(body);
		String channel = triggeredBody.getAsString("channel");
		String wstoken = triggeredBody.getAsString("wstoken");
		if(!(triggeredBody.get("courseId") instanceof JSONArray)) {
			System.out.println("course id is :");
			JSONArray courseId = new JSONArray();
			courseId.add(triggeredBody.get("courseId"));
			triggeredBody.put("courseId", courseId);
		}
		JSONArray courseIds = (JSONArray) triggeredBody.get("courseId");
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
			        				this.attemptStartedOnMoodle = true;	
			        				result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + quizid + "&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        try {	
			        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
			        		        } catch( NullPointerException e ) {
			        		        	this.topicProcessed.put("topicName", false);
			        		        	this.attemptStartedOnMoodle = false;
			        		        	error.put("text", "Dein Lehrer scheint gerade an diesem Quiz zu arbeiten. Versuchen es später vielleicht noch einmal");
			        		        	error.put("closeContext", true);
			        		        	return Response.ok().entity(error).build();
			        		        }
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        this.topicProcessed.put(topicName, false);
			        		        this.attemptStartedOnMoodle = false;
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
			        		        				questions +=  "*"+doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text() + "*\n";
			        		        			}
			        		        			if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("") && doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 1) {
			        		        				questions +=  "*"+doc.getElementsByClass("qtext").text() + "*\n";
			        		        			}
			        		        		}
		        		        		}
		        		        		assessment[k][0] = questions ;
		        		        		
		        		        		System.out.println(doc.getElementsByClass("qtext").text());
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
		        		        		System.out.println("Correct answer is: " + assessment[k][1]);
		        		        		if(assessment[k][2].equals("multichoice") || assessment[k][2].equals("truefalse")) {
		        		        			// check if answers or answer here ? 
		        		        			Elements multiChoiceAnswers = doc.getElementsByClass("ml-1");
		        		        			for(Element item : multiChoiceAnswers) {
		        		        				if(assessment[k][2].equals("truefalse")) {
		        		        					if(item.text().toLowerCase().contains("true")) {
		        		        						assessment[k][3] +=" • "+ "Wahr" + " \n";
		        		        					} else assessment[k][3] +=" • "+ "Falsch" + " \n";
		        		        				} else assessment[k][3] +=" • "+ item.text() + " \n";
		        		        				System.out.println(item.text() + "\n");
		        		        				if(assessment[k][2].equals("multichoice")) {
		        		        					System.out.println(assessment[k][1] + "is at " + item.text().split("\\.")[0] );
		        		        					if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        						if(assessment[k][1].contains(item.text().split("\\.")[1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					} else {
		        		        						if(item.text().split("\\.")[1].contains(assessment[k][1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					}
		        		        				}
		        		        			}
		        		        		}
			        		        		
			        		        }
			        		        JSONArray Questions = new JSONArray();
			        		        JSONArray Answers = new JSONArray();
			        		        JSONArray Possibilities = new JSONArray();
			        		        JSONArray QuestionType = new JSONArray();
			        		        JSONArray QuestionPoints = new JSONArray();
			        		        JSONArray Feedback = new JSONArray();
			        		        int maxMark = 0;
			        		        for(int k = 0 ; k < assessment.length ; k++) {
			        		        	if(assessment[k][2].equals("missing")) {
			        		        		continue;
			        		        	}
			        		        	Questions.add(assessment[k][0]);
			        		        	if(assessment[k][2].equals("multichoice")) {
			        		        		Answers.add(assessment[k][4]);
			        		        	} else Answers.add(assessment[k][1]);
			        		        	QuestionPoints.add(assessment[k][5]);
			        		        	Possibilities.add(assessment[k][3]);
			        		        	QuestionType.add(assessment[k][2]);
			        		        	maxMark += Double.parseDouble(assessment[k][5]); 
			        		        	Feedback.add(assessment[k][6]);
			        		        }
			        		        JSONObject currAssessmentContent = new JSONObject();
			        		        currAssessmentContent.put("QuestionPoints", QuestionPoints);
			        		        currAssessmentContent.put("Questions", Questions);
			        		        currAssessmentContent.put("Answers", Answers);
			        		        currAssessmentContent.put("Possibilities", Possibilities);
			        		        currAssessmentContent.put("QuestionType", QuestionType);
			        		        currAssessmentContent.put("currentQuestion" , 0);
			        		        currAssessmentContent.put("currentWrongQuestions" ,"");
			        		        currAssessmentContent.put("currentMark", 0);
			        		        currAssessmentContent.put("maxMark", maxMark);
			        		        currAssessmentContent.put("Feedback", Feedback);
			        		        currAssessmentContent.put("quitIntent", triggeredBody.getAsString("quitIntent"));
			        		        
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
			        		    	currAssessmentContent.put("actor", actor);
			        		    	currAssessmentContent.put("object", object);
			        		    	currAssessmentContent.put("verb", verb);
			        		    	//statement.put("timestamp","2020-07-07T22:08:45Z" );
			        		        
			        		    	this.currentAssessment.put(channel, currAssessmentContent);
			        		        JSONObject response = new JSONObject();
			        		        response.put("text", "Wir starten jetzt das Moodle Quiz :) \n " + assessment[0][0] + assessment[0][3]);
			        		        response.put("closeContext" , "false");
			        		        this.score.put(channel, 0);
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
			return Response.ok().entity(continueJSONAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessmentDe")).build();
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
			        				this.attemptStartedOnMoodle = true;	
			        				result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + quizid + "&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        res = (JSONObject) p.parse(result.getResponse());
			        		        try {	
			        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
			        		        } catch( NullPointerException e ) {
			        		        	this.topicProcessed.put("topicName", false);
			        		        	this.attemptStartedOnMoodle = false;
			        		        	error.put("text", "Dein Lehrer scheint gerade an diesem Quiz zu arbeiten. Versuchen es später vielleicht noch einmal");
			        		        	error.put("closeContext", true);
			        		        	return Response.ok().entity(error).build();
			        		        }
			        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
			        						"", MediaType.APPLICATION_JSON, headers);
			        		        this.topicProcessed.put(topicName, false);
			        		        this.attemptStartedOnMoodle = false;
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
			        		        				questions +=  "*"+doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text() + "*\n";
			        		        			}
			        		        			if(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l).text().equals("") && doc.getElementsByClass("qtext").get(0).getElementsByTag("p").size() == 1) {
			        		        				questions +=  "*"+doc.getElementsByClass("qtext").text() + "*\n";
			        		        			}
			        		        		}
		        		        		}
		        		        		assessment[k][0] = questions ;
		        		        		
		        		        		System.out.println(doc.getElementsByClass("qtext").text());
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
		        		        						assessment[k][3] +=" • "+ "Wahr" + " \n";
		        		        					} else assessment[k][3] +=" • "+ "Falsch" + " \n";
		        		        				} else assessment[k][3] +=" • "+ item.text() + " \n";
		        		        				System.out.println(item.text() + "\n");
		        		        				if(assessment[k][2].equals("multichoice")) {
		        		        					System.out.println(assessment[k][1] + "is at " + item.text().split("\\.")[0] );
		        		        					if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        						if(assessment[k][1].contains(item.text().split("\\.")[1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					} else {
		        		        						if(item.text().split("\\.")[1].contains(assessment[k][1])) {
				        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
				        		        				}
		        		        					}
		        		        				}
		        		        			}
		        		        		}
			        		        		
			        		        }
			        		        JSONArray Questions = new JSONArray();
			        		        JSONArray Answers = new JSONArray();
			        		        JSONArray Possibilities = new JSONArray();
			        		        JSONArray QuestionType = new JSONArray();
			        		        JSONArray QuestionPoints = new JSONArray();
			        		        JSONArray Feedback = new JSONArray();
			        		        int maxMark = 0;
			        		        for(int k = 0 ; k < assessment.length ; k++) {
			        		        	if(assessment[k][2].equals("missing")) {
			        		        		continue;
			        		        	}
			        		        	Questions.add(assessment[k][0]);
			        		        	if(assessment[k][2].equals("multichoice")) {
			        		        		Answers.add(assessment[k][4]);
			        		        	} else Answers.add(assessment[k][1]);
			        		        	QuestionPoints.add(assessment[k][5]);
			        		        	Possibilities.add(assessment[k][3]);
			        		        	QuestionType.add(assessment[k][2]);
			        		        	maxMark += Double.parseDouble(assessment[k][5]); 
			        		        	Feedback.add(assessment[k][6]);
			        		        }
			        		        JSONObject currAssessmentContent = new JSONObject();
			        		        currAssessmentContent.put("QuestionPoints", QuestionPoints);
			        		        currAssessmentContent.put("Questions", Questions);
			        		        currAssessmentContent.put("Answers", Answers);
			        		        currAssessmentContent.put("Possibilities", Possibilities);
			        		        currAssessmentContent.put("QuestionType", QuestionType);
			        		        currAssessmentContent.put("currentQuestion" , 0);
			        		        currAssessmentContent.put("currentWrongQuestions" ,"");
			        		        currAssessmentContent.put("currentMark", 0);
			        		        currAssessmentContent.put("maxMark", maxMark);
			        		        currAssessmentContent.put("Feedback", Feedback);
			        		        currAssessmentContent.put("quitIntent", triggeredBody.getAsString("quitIntent"));
			        		        
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
			        		    	currAssessmentContent.put("actor", actor);
			        		    	currAssessmentContent.put("object", object);
			        		    	currAssessmentContent.put("verb", verb);
			        		    	//statement.put("timestamp","2020-07-07T22:08:45Z" );
			        		        
			        		    	this.currentAssessment.put(channel, currAssessmentContent);
			        		        JSONObject response = new JSONObject();
			        		        response.put("text", "Wir starten jetzt das Moodle Quiz :) \n " + assessment[0][0] + assessment[0][3]);
			        		        response.put("closeContext" , "false");
			        		        this.score.put(channel, 0);
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
			return Response.ok().entity(continueJSONAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessmentDe")).build();
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
    
    
    @GET
	@Path("/a")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response a() {
    	JSONObject b = new JSONObject();
    	
    	JSONObject a  = new JSONObject();
    	JSONObject c = new JSONObject();
    	c.put("topic","Ass2");
    	a.put("topic", "Ass1");
    	b.put("content",a);
    	// of course add check for errors with try catch..
    	JSONObject actor = new JSONObject();
    	JSONObject verb = new JSONObject();
    	JSONObject object = new JSONObject();
    	JSONObject result = new JSONObject();
    	actor.put("objectType", "Agent");
    	actor.put("mbox", "mailto:alice@example.com");
    	verb.put("id", "https://w3id.org/xapi/dod-isd/verbs/completed");
    	JSONObject display = new JSONObject();
    	display.put("en-US", "completed");
    	verb.put("display", display);
    	
    	result.put("completion", true);
    	result.put("response", "feedback");
    	JSONObject score = new JSONObject();
    	score.put("min",0);
    	score.put("max",10);
    	score.put("raw",5);
    	double scaled = 0.5;
    	score.put("scaled", scaled);
    	result.put("score",score);
    	
    	object.put("id", "https://moodle.tech4comp.dbis.rwth-aachen.de/mod/quiz/view.php?id=44");
    	JSONObject definition = new JSONObject();
    	JSONObject name = new JSONObject();
    	name.put("en-US", "quizName");
    	definition.put("name" , name);
    	object.put("definition", definition);
    	JSONObject statement = new JSONObject();
    	statement.put("actor", actor);
    	statement.put("verb", verb);
    	statement.put("object", object);
    	statement.put("result", result);
    	statement.put("timestamp","2020-07-07T22:08:45Z" );
		String clientKey =  "c414faf567644beb7682da7686f732feb21f233b";
		String clientSecret = "e4068bd9b8df0d1ce8a0719d78e1d2e4fb55c9c2";
		String lrsAuth = Base64.getEncoder().encodeToString((clientKey + ":" + clientSecret).getBytes());
    	System.out.println(lrsAuth);
    	Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_2,statement.toString() + "*BAConrardy");

    	
    	if(b.containsKey("content")) {
    		JSONArray content = new JSONArray();
    		content.add(b.get("content"));
    		content.add(c);
    		b.put("content", c);
    		return Response.ok().entity(b).build();
    	}
		return null;
	}
    
    
    
    


}