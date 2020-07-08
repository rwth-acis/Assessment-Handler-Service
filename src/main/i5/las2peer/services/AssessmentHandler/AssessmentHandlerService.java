package i5.las2peer.services.AssessmentHandler;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
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
 * las2peer-Template-Service
 * 
 * This is a template for a very basic las2peer service that uses the las2peer WebConnector for RESTful access to it.
 * 
 * Note: If you plan on using Swagger you should adapt the information below in the SwaggerDefinition annotation to suit
 * your project. If you do not intend to provide a Swagger documentation of your service API, the entire Api and
 * SwaggerDefinition annotation should be removed.
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
					for(String content : assessment) {
						System.out.println(content);
						 contentJson = (JSONObject) p.parse(content);
						if(contentJson.getAsString("topic").toLowerCase().equals(bodyJson.getAsString("topic").toLowerCase())){
						//	setUpAssessment(contentJson, channel);
							setUpNluAssessment(contentJson, channel, bodyJson.getAsString("quitIntent"), bodyJson.getAsString("helpIntent"));
						//	response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +this.currAssessment.get(channel)[0][1]);
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
	
/*    private JSONObject continueAssessment(String channel, String intent, JSONObject triggeredBody, String assessmentType){
    	JSONObject response = new JSONObject();
    	String answer = "";
    	response.put("closeContext", "false");
        System.out.println(this.currQuestion.get(channel));
        System.out.println(this.currAssessment.get(channel)[this.currQuestion.get(channel)][2]);
        if(assessmentType == "NLUAssessment") {
	        if(intent.equals(quitIntent)) {
	        	answer += "Assessment is over \n" + "You got " + this.score.get(channel) + "/" + this.currAssessment.get(channel).length + "Questions right! \n You got following Questions wrong: \n " + this.currCorrectQuestions.get(channel);
	            this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else { 
		        if(intent.equals(this.currAssessment.get(channel)[this.currQuestion.get(channel)][2])){
		            answer += "Correct Answer! \n";
		            this.score.put(channel, this.score.get(channel) + 1 );
		        } else {
		        	answer += "wrong answer :/ \n";
		        	this.currCorrectQuestions.put(channel, this.currCorrectQuestions.get(channel) + this.currAssessment.get(channel)[this.currQuestion.get(channel)][1] + "\n");
		        }
		        this.currQuestion.put(channel,this.currQuestion.get(channel)+1);
		        if(this.currQuestion.get(channel)==this.currAssessment.get(channel).length){
		            answer += "Assessment is over \n" + "You got " + this.score.get(channel) + "/" + this.currAssessment.get(channel).length + "Questions right! \n You got following Questions wrong: \n " + this.currCorrectQuestions.get(channel);
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += this.currAssessment.get(channel)[this.currQuestion.get(channel)][1];        
		        }
	        }
	        
        } else if(assessmentType == "moodleAssessment") {
	        if(intent.equals(quitIntent)) {
	        	answer += "Assessment is over \n" + "You got " + this.score.get(channel) + "/" + this.currAssessment.get(channel).length + "Questions right! \n You got following Questions wrong: \n " + this.currCorrectQuestions.get(channel);
	            this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else { 
	        	String msg = triggeredBody.getAsString("msg");
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	System.out.println(this.currAssessment.get(channel)[this.currQuestion.get(channel)][1] + "  " + msg);
		        if(this.currAssessment.get(channel)[this.currQuestion.get(channel)][1].toLowerCase().contains(msg.toLowerCase())){
		            answer += "Correct Answer! \n";
		            this.score.put(channel, this.score.get(channel) + 1 );
		        } else {
		        	answer += "wrong answer :/ \n";
		        	this.currCorrectQuestions.put(channel, this.currCorrectQuestions.get(channel) + this.currAssessment.get(channel)[this.currQuestion.get(channel)][0]  + "\n");
		        }
		        this.currQuestion.put(channel,this.currQuestion.get(channel)+1);
		        if(this.currQuestion.get(channel)==this.currAssessment.get(channel).length){
		            answer += "Assessment is over \n" + "You got " + this.score.get(channel) + "/" + this.currAssessment.get(channel).length + "Questions right! \n You got following Questions wrong: \n " + this.currCorrectQuestions.get(channel);
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += this.currAssessment.get(channel)[this.currQuestion.get(channel)][0] + this.currAssessment.get(channel)[this.currQuestion.get(channel)][3];        
		        }
	        }
        } else {
        	System.out.println("Assessment type: "+ assessmentType + " not known");
        }
        response.put("text", answer);
        return response;
    }*/
    
    private JSONObject continueJSONAssessment(String channel, String intent, JSONObject triggeredBody, String assessmentType){
    	JSONObject response = new JSONObject();
    	String answer = "";
    	int currentQuestionNumber = this.getCurrentQuestionNumber(channel);
    	response.put("closeContext", "false");
    	System.out.println(assessmentType + " " + intent + "  " + this.getHelpIntent(channel) +  this.getQuitIntent(channel));
        if(assessmentType.equals("NLUAssessment")) {
	        if(intent.equals(this.getQuitIntent(channel))){
	        	// here should not be the entire size but the current number of questions .. 
	        	answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getCurrentQuestionNumber(channel) + "Questions right! \n"; 
	            if(this.getMarks(channel).equals(this.getCurrentQuestionNumber(channel))) {
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
		        	answer += "wrong answer :/ \n";
		        	this.addWrongQuestion(channel);
		        }
		        this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel).equals(this.getAssessmentSize(channel))) {
		        		answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got no Questions wrong! \n " + this.getWrongQuestions(channel);
		        	} else answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += ((JSONArray)this.currentAssessment.get(channel).get("Questions")).get(this.getCurrentQuestionNumber(channel));        
		        }
	        }
	        
        } else if(assessmentType == "moodleAssessment") {
	        if(intent.equals(this.getQuitIntent(channel))) {
	        	answer += "Assessment is over \n" + "Your final mark is *" + this.getMarks(channel) + "/" + this.getTotalMarksUntilCurrentQuestion(channel) + "* \n";  	
	            if(this.getMarks(channel).equals(this.getTotalMarksUntilCurrentQuestion(channel))) {
	            	answer += "You got no questions wrong!";
	            } else answer += "You got following Questions wrong: \n " + this.getWrongQuestions(channel);
	        	this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else { 
	        	String msg = triggeredBody.getAsString("msg");
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	if(this.getQuestionType(channel).equals("numerical") || this.getQuestionType(channel).equals("shortanswer") ) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().equals(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	        			this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        		 } else {
	        			 answer += "wrong answer :/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("truefalse")) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	 		            this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        		 } else {
	        			answer += "wrong answer :/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("multichoice")) {
	        		if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().split(";").length == 2) {
	        			if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
		        			answer += "Correct Answer! \n";
		        			this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
		        		 } else {
		        			answer += "wrong answer :/ \n";
		 		        	this.addWrongQuestion(channel);
		        		 }
	        		} else {
	        			String[] multipleAnswers = ((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().split(";");
	        			String[] userAnswers = msg.split("\\s+");
	        			double splitMark = this.getMarkForCurrentQuestion(channel)/(multipleAnswers.length-1);
	        			int numberOfCorrectAnswers = 0;
	        			System.out.println(multipleAnswers.length-1);
	        			for(int i = 0 ; i < multipleAnswers.length -1 ; i++) {
	        				System.out.println(multipleAnswers[i]);
	        				
	        				for(int j = 0 ; j < userAnswers.length; j++ ){
	        					System.out.println(userAnswers[j]);	
	        					if(userAnswers[j].length() > 1 ) {
	        						System.out.println("answer was larger than 1");
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
	        				} else {
	        					answer += "You somehow managed to get more points than intended\n";
	        					this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        				}
	        			} else if((multipleAnswers.length-1) > userAnswers.length) {
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
	        				// careful here, - points if someone has too many answers
	        				 answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s) and " + (userAnswers.length-numberOfCorrectAnswers) + " wrong one(s)\n";
	        				 int points = numberOfCorrectAnswers - userAnswers.length; 
	        				 if(points >= 0) {
	        					 this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        				 }
	        			}
	        			
	        			/*if(numberOfCorrectAnswers == (multipleAnswers.length-1) && (multipleAnswers.length-1) == userAnswers.length ) {
	        				answer += "Correct Answer(s)! \n";
		 		            this.incrementMark(channel, this.getMarkForCurrentQuestion(channel));
	        			} else if(numberOfCorrectAnswers < (multipleAnswers.length-1) && numberOfCorrectAnswers > 0 && (multipleAnswers.length-1) >= userAnswers.length) {
	        				answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s)\n";
	        				this.addWrongQuestion(channel);
		 		            this.incrementMark(channel, numberOfCorrectAnswers*splitMark);
	        			} else if(numberOfCorrectAnswers < (multipleAnswers.length-1) && numberOfCorrectAnswers > 0 && (multipleAnswers.length-1) >= userAnswers.length) {
	        				answer += "Your answer(s) were all wrong \n";
	        				this.addWrongQuestion(channel);
	        				this.incrementMark(channel, numberOfCorrectAnswers);
	        			} else if(userAnswers.length >= (multipleAnswers.length-1)) {
	        				if(numberOfCorrectAnswers > 0 ) {
	        					answer += "Your answer was partially correct, you got " + numberOfCorrectAnswers + " correct answer(s)";
	        					numberOfCorrectAnswers = numberOfCorrectAnswers - userAnswers.length + multipleAnswers.length -1;
	        					answer += " and " + numberOfCorrectAnswers + " wrong answer(s)\n";
	        					this.addWrongQuestion(channel);
			 		            this.incrementMark(channel, numberOfCorrectAnswers);
	        				} else {
	        					this.addWrongQuestion(channel);
	        					answer += "Your answers were all wrong \n";
	        				}
	        			} else if(numberOfCorrectAnswers == 0) {
	        				this.addWrongQuestion(channel);
	        				answer += "Your answers were all wrong \n";
	        			}*/	
	        			
	        		}
	        		 
	        	}
	        	if(!((JSONArray)this.currentAssessment.get(channel).get("Feedback")).get(this.getCurrentQuestionNumber(channel)).toString().equals("")) {
	        		answer += ((JSONArray)this.currentAssessment.get(channel).get("Feedback")).get(this.getCurrentQuestionNumber(channel)).toString() + "\n";
	        	}
	        	this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel).equals(String.valueOf(this.getAssessmentSize(channel)))) {
		        		answer += "Assessment is over \n" + "Your final mark is *" + this.getMarks(channel) + "/" + this.getMaxMarks(channel) + "* \n You got no Questions wrong! \n " + this.getWrongQuestions(channel);
		        	} else answer += "Assessment is over \n" + "Your final mark is *" + this.getMarks(channel) + "/" + this.getMaxMarks(channel) + "*  \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
		            this.assessmentStarted.put(channel, null);
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
    
    private String getMarks(String channel) {
    	return this.currentAssessment.get(channel).getAsString("currentMark");
    }
    
    private double getMarkForCurrentQuestion(String channel) {
    	return Double.parseDouble(((JSONArray) this.currentAssessment.get(channel).get("QuestionPoints")).get(this.getCurrentQuestionNumber(channel)).toString());
    }
    
    private String getMaxMarks(String channel) {
    	return this.currentAssessment.get(channel).getAsString("maxMark");
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
    	this.currentAssessment.get(channel).put("currentMark", Double.parseDouble(this.getMarks(channel)) + (Math.round(value*100.0)/100.0));
    	System.out.println("doule is  :" + Double.parseDouble(this.getMarks(channel)) + value + "=" +  this.getMarks(channel));
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
    
    @POST
	@Path("/moodle")
    @Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME",
			notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response moodle(String body) throws ParseException {
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
		
		String quizid="";
		String attemptId = "";
		String topicName = triggeredBody.getAsString("topic");
		if(assessmentStarted.get(channel) == null) {
			if(topicName == null) {
				JSONObject error = new JSONObject();
				error.put("text", "No topic name was recognized.");
		        error.put("closeContext" , "true");
		        return Response.ok().entity(error).build();
			}
			
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
		        			if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name").toLowerCase().equals(topicName.toLowerCase())) {
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
		        		        System.out.println(channel + "\n" + res);
		        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
		        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
		        						"", MediaType.APPLICATION_JSON, headers);
		        		        this.topicProcessed.put(topicName, false);
		        		        this.attemptStartedOnMoodle = false;
		        		        System.out.println(channel + "\n" + result);
		        		        System.out.println(result.getResponse());
		        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_get_attempt_review&attemptid=" + attemptId + "&page=-1&moodlewsrestformat=json" , "",
		        						"", MediaType.APPLICATION_JSON, headers);
		        		        
		        		        res = (JSONObject) p.parse(result.getResponse());
		        		        System.out.println(channel + "\n" + res);
		        		        String html = "";
		        		        Document doc = Jsoup.parse("<html></html>");
		        		        String questions = "";
		        		        String answers = "";
		        		        String[][] assessment = new String[((JSONArray) res.get("questions")).size()][7];
		        		        for(int k = 0 ; k < ((JSONArray) res.get("questions")).size() ; k++) {
		        		        	html =  ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("html");
		        		        	doc = Jsoup.parse(html);
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
		        		        			System.out.println(doc.getElementsByClass("qtext").get(0).getElementsByTag("p").get(l));
		        		        		}
	        		        		}
	        		        		assessment[k][0] = questions ;
	        		        		assessment[k][2] = ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("type");
	        		        		System.out.println(doc.getElementsByClass("qtext").text());
	        		        		// to differentiate between questions with one answer and questions with multiple correct answers
	        		        		if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
	        		        			assessment[k][3] += "Select one or more: (Separate your answers with a whitespace, e.g : a b)\n";
	        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1] +"\n";
	        		        			assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1];
	        		        		} else {
	        		        			if(assessment[k][2].equals("multichoice")) {
	        		        				assessment[k][3] += "Select one:(choose by simply answering with the associated letter/number)\n";
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
		        		        this.currentAssessment.put(channel, currAssessmentContent);
		        		        JSONObject response = new JSONObject();
		        		        response.put("text", "We will now start the moodle quiz :) \n " + assessment[0][0] + assessment[0][3]);
		        		        response.put("closeContext" , "false");
		        		        this.score.put(channel, 0);
		        		        assessmentStarted.put(channel,"true");
		        		        return Response.ok().entity(response).build();
		        			}
		        		}
		        	}
		        }
			}
				
			JSONObject error = new JSONObject();
			error.put("text", "Topic not found in given courses.");
			return Response.ok().entity(error).build();
		} else {
			return Response.ok().entity(continueJSONAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessment")).build();
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