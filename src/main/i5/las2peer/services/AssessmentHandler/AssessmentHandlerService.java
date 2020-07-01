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
    
    private static String quitIntent = "";
    private static String helpIntent = "";
    
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
				if(bodyJson.get("assessmentContent") instanceof JSONObject) {
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
						if(contentJson.getAsString("topic").equals(bodyJson.getAsString("topic"))){
							setUpAssessment(contentJson, channel);
							setUpJSONAssessment(contentJson, channel);
						//	response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +this.currAssessment.get(channel)[0][1]);
							response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +((JSONArray) this.currentAssessment.get(channel).get("Questions")).get(0));
							response.put("closeContext", "false");
							return Response.ok().entity(response).build(); 
						}
					}
					
				}

			} else {
				return Response.ok().entity(continueAssessment(channel, bodyJson.getAsString("intent"), bodyJson, "NLUAssessment")).build();
			}		
			
		} catch (ParseException e) {
			e.printStackTrace();
		}	

		return Response.ok().entity("Something went wrong").build();

	}	
	
	private void setUpAssessment(JSONObject content , String channel) {
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
        this.currAssessment.put(channel, assessmentContent);
        this.currQuestion.put(channel, 0);
        this.currCorrectQuestions.put(channel, ""); 
        this.score.put(channel, 0);
        for(int i = 0; i < length ; i++){
            System.out.println(assessmentContent[i][0] + " " + assessmentContent[i][1] + " " + assessmentContent[i][2] + " " + assessmentContent[i][3]);
        } 
        this.quitIntent = content.getAsString("QuitIntent");
        this.assessmentStarted.put(channel, "true");	
        System.out.println(channel);
  		
	}
	
	
	private void setUpJSONAssessment(JSONObject content , String channel) {
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
        currAssessmentContent.put("quitIntent", "quitIntent");
        currAssessmentContent.put("currentQuestion", 0);
        currAssessmentContent.put("currentWrongQuestions", "");
        currAssessmentContent.put("currentMark", 0);
        this.quitIntent = content.getAsString("QuitIntent");
        this.currentAssessment.put(channel, currAssessmentContent);
        this.assessmentStarted.put(channel, "true");	
        System.out.println(channel);
  		
	}
	
    private JSONObject continueAssessment(String channel, String intent, JSONObject triggeredBody, String assessmentType){
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
		        	answer += "Wrong Answer:/ \n";
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
		        	answer += "Wrong Answer:/ \n";
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
    }
    
    private JSONObject continueJSONAssessment(String channel, String intent, JSONObject triggeredBody, String assessmentType){
    	JSONObject response = new JSONObject();
    	String answer = "";
    	int currentQuestionNumber = this.getCurrentQuestionNumber(channel);
    	response.put("closeContext", "false");
        if(assessmentType == "NLUAssessment") {
	        if(intent.equals(quitIntent)) {
	        	// here should not be the entire size but the current number of questions .. 
	        	answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
	            this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else { 
		        if(intent.equals(((JSONArray)this.currentAssessment.get(channel).get("Intents")).get(currentQuestionNumber))){
		            answer += "Correct Answer! \n";
		            this.incrementMark(channel, 1);
		        } else {
		        	answer += "Wrong Answer:/ \n";
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
	        if(intent.equals(quitIntent)) {
	        	answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
	            this.assessmentStarted.put(channel, null);
	            response.put("closeContext", "true");
	        } else { 
	        	String msg = triggeredBody.getAsString("msg");
	        	// differ between true false / multiple answers, one answer 
	        	// for multiple choice split with "," to have all the answers
	        	if(this.getQuestionType(channel).equals("numerical") || this.getQuestionType(channel).equals("shortanswer") ) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().equals(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	 		            this.incrementMark(channel, 1);
	        		 } else {
	        			 answer += "Wrong Answer:/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("truefalse")) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	 		            this.incrementMark(channel, 1);
	        		 } else {
	        			answer += "Wrong Answer:/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	} else if(this.getQuestionType(channel).equals("multichoice")) {
	        		 if(((JSONArray) this.currentAssessment.get(channel).get("Answers")).get(this.getCurrentQuestionNumber(channel)).toString().toLowerCase().contains(msg.toLowerCase())) {
	        			answer += "Correct Answer! \n";
	 		            this.incrementMark(channel, 1);
	        		 } else {
	        			answer += "Wrong Answer:/ \n";
	 		        	this.addWrongQuestion(channel);
	        		 }
	        	}
		        this.incrementCounter(channel);
		        if(this.getCurrentQuestionNumber(channel) == getAssessmentSize(channel)){
		        	if(this.getMarks(channel).equals(String.valueOf(this.getAssessmentSize(channel)))) {
		        		answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got no Questions wrong! \n " + this.getWrongQuestions(channel);
		        	} else answer += "Assessment is over \n" + "You got " + this.getMarks(channel) + "/" + this.getAssessmentSize(channel) + "Questions right! \n You got following Questions wrong: \n " + this.getWrongQuestions(channel);
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
    
    private Integer getAssessmentSize(String channel) {
    	return ((JSONArray)this.currentAssessment.get(channel).get("Questions")).size();
    
    }
    private String getWrongQuestions(String channel) {
    	return this.currentAssessment.get(channel).getAsString("currentWrongQuestions");
    }    
    private int getCurrentQuestionNumber(String channel) {
    	return Integer.parseInt(this.currentAssessment.get(channel).getAsString("currentQuestion"));
    }    
    
    private void incrementMark(String channel, int value) {
    	this.currentAssessment.get(channel).put("currentMark", Integer.parseInt(this.getMarks(channel)) + value);
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
    	// set to trtue at the end 
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
				JSONArray resi = (JSONArray) p.parse(result.getResponse());
		        JSONObject res= new JSONObject();
		        for(int i = 0; i < resi.size() ;i++) {
		        	for(int j = 0; j < ((JSONArray)((JSONObject) resi.get(i)).get("modules")).size();j++) {
		        		if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("modname").equals("quiz")){
		        			if(((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("name").equals(topicName)) {
		        				quizid = ((JSONObject)((JSONArray)((JSONObject) resi.get(i)).get("modules")).get(j)).getAsString("instance");
		        				result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + quizid + "&moodlewsrestformat=json" , "",
		        						"", MediaType.APPLICATION_JSON, headers);
		        		         res = (JSONObject) p.parse(result.getResponse());
		        		        attemptId = ((JSONObject) res.get("attempt")).getAsString("id");
		        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_process_attempt&attemptid=" + attemptId + "&finishattempt=1&moodlewsrestformat=json" , "",
		        						"", MediaType.APPLICATION_JSON, headers);
		        		        System.out.println(result.getResponse());
		        		        result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_get_attempt_review&attemptid=" + attemptId + "&page=-1&moodlewsrestformat=json" , "",
		        						"", MediaType.APPLICATION_JSON, headers);
		        		        res = (JSONObject) p.parse(result.getResponse());
		        		        String html = "";
		        		        Document doc = Jsoup.parse("<html></html>");
		        		        String questions = "";
		        		        String answers = "";
		        		        String[][] assessment = new String[((JSONArray) res.get("questions")).size()][5];
		        		        for(int k = 0 ; k < ((JSONArray) res.get("questions")).size() ; k++) {
		        		        	html =  ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("html");
		        		        	doc = Jsoup.parse(html);
		        		        	assessment[k][3] = "";
		        		        	assessment[k][4] = "";
		        		        //	if(((JSONObject)((JSONArray) res.get("questions")).get(i)).getAsString("type") == "truefalse") {
		        		        	// differentiate between true false and others, bcs for tf right answers are written  : the right answer is '' , whereas for other the ight answer is : 
		        		        		questions += doc.getElementsByClass("qtext").text() + "\n";
		        		        		assessment[k][0] = doc.getElementsByClass("qtext").text() +"\n";
		        		        		assessment[k][2] = ((JSONObject)((JSONArray) res.get("questions")).get(k)).getAsString("type");
		        		        		System.out.println(doc.getElementsByClass("qtext").text());
		        		       	//}		// to differentiate between questions with one answer and questions with multiple correct answers
		        		        		if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
		        		        			
		        		        			assessment[k][3] += "Select one or more:\n";
		        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1] +"\n";
		        		        			assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1];
		        		        		} else {
		        		        			if(assessment[k][2].equals("multichoice") || assessment[k][2].equals("truefalse")) {
		        		        				assessment[k][3] += "Select one:\n";
		        		        			}
		        		        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1] +"\n";
		        		        			if(assessment[k][2].equals("truefalse")) {
		        		        				assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1];
		        		        			} else assessment[k][1] = doc.getElementsByClass("rightanswer").text().split("The correct answer is: ")[1];
		        		        			
		        		        		}
		        		        		System.out.println(assessment[k][2]);
		        		        		if(assessment[k][2].equals("multichoice") || assessment[k][2].equals("truefalse")) {
		        		        			System.out.println("K");
		        		        			// check if answers or answer here ? 
		        		        			Elements multiChoiceAnswers = doc.getElementsByClass("ml-1");
		        		        			for(Element item : multiChoiceAnswers) {
		        		        				assessment[k][3] +=" • "+ item.text() + " \n";
		        		        				System.out.println(item.text() + "\n");
		        		        				if(assessment[k][2].equals("multichoice") ) {
			        		        				if(assessment[k][1].contains(item.text().split("\\.")[1])) {
			        		        					assessment[k][4] += item.text().split("\\.")[0] + " ; ";
			        		        				}
		        		        				}
		        		        			}
		        		        		}
		        		        		
		        		        }
		        		        JSONArray Questions = new JSONArray();
		        		        JSONArray Answers = new JSONArray();
		        		        JSONArray Possibilities = new JSONArray();
		        		        JSONArray QuestionType = new JSONArray();
		        		        for(int k = 0 ; k < assessment.length ; k++) {
		        		        	
		        		        }
		        		        for(int k = 0 ; k < assessment.length ; k++) {
		        		        	Questions.add(assessment[k][0]);
		        		        	if(assessment[k][2].equals("multichoice")) {
		        		        		Answers.add(assessment[k][4]);
		        		        	} else Answers.add(assessment[k][1]);
		        		        	
		        		        	Possibilities.add(assessment[k][3]);
		        		        	QuestionType.add(assessment[k][2]);
		        		        	System.out.println(Answers.get(k).toString());
		        		        }
		        		        JSONObject currAssessmentContent = new JSONObject();
		        		        currAssessmentContent.put("Questions", Questions);
		        		        currAssessmentContent.put("Answers", Answers);
		        		        currAssessmentContent.put("Possibilities", Possibilities);
		        		        currAssessmentContent.put("QuestionType", QuestionType);
		        		        currAssessmentContent.put("currentQuestion" , 0);
		        		        currAssessmentContent.put("currentWrongQuestions" ,"");
		        		        currAssessmentContent.put("currentMark", 0);
		        		        currAssessmentContent.put("quitIntent", triggeredBody.getAsString("quitIntent"));
		 /*       		        this.currQuestion.put(triggeredBody.getAsString("channel"), 0);
		        		        this.currAssessment.put(triggeredBody.getAsString("channel"), assessment);
		        		        this.currCorrectQuestions.put(channel, ""); */
		        		        this.currentAssessment.put(channel, currAssessmentContent);
		        		        JSONObject response = new JSONObject();
		        		        response.put("text", "We will now start the moodle quiz :) \n " + assessment[0][0] + assessment[0][3]);
		        		        response.put("closeContext" , "false");
		        		        this.score.put(channel, 0);
		        		        this.quitIntent = triggeredBody.getAsString("QuitIntent");
		        		        assessmentStarted.put(channel,"true");
		        		        return Response.ok().entity(response).build();
		        			}
		        		}
		        	}
		        }
		        res.put("text", "Quiz on topic " + topicName + "does not exist.");
		        res.put("closeContext" , "true");
		        return Response.ok().entity(res).build();
			}
			JSONObject error = new JSONObject();
			error.put("text", "Topic not found in given courses.");
			return Response.ok().entity(error).build();
		} else {
			return Response.ok().entity(continueJSONAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessment")).build();
		}  
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