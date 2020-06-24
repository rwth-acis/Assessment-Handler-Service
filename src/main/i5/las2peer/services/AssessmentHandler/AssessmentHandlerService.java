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
				System.out.println("I am inside");
				JSONArray jsonAssessment = (JSONArray) bodyJson.get("bodyContent");
				ArrayList<String> assessment = new ArrayList<String>();
				if(jsonAssessment != null) {
					System.out.println("I am inside 2");
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
							response.put("text", "We will now start the assessment on "+ bodyJson.getAsString("topic") + "\n" +this.currAssessment.get(channel)[0][1]);
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

        System.out.println("Connect Success");
		return Response.ok().entity("test").build();

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
            if(Sequence.get(i)==""){
                noNum++;    
            }
            if(Integer.parseInt(Sequence.get(i).toString()) > max){
                max = Integer.parseInt(Sequence.get(i).toString());    
            }            
            assessmentContent[i][0] = Sequence.get(i).toString();
            assessmentContent[i][1] = Questions.get(i).toString();
            assessmentContent[i][2] = Intents.get(i).toString();
            assessmentContent[i][3] = Hints.get(i).toString();     
        }
        
        // to fill out the blank sequence slots
        // last blank space will be at last place
        for(int i = length-1; i >= 0 ; i--){
            if(assessmentContent[i][0] == ""){
            	assessmentContent[i][0] = Integer.toString(max + noNum);
            	noNum--;
            }
        }      
        // to do  :create array and sort it
        Arrays.sort(assessmentContent, (a, b) -> Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])));
        this.currAssessment.put(channel, assessmentContent);
        this.currQuestion.put(channel, 0);
        this.currCorrectQuestions.put(channel, ""); 
        this.score.put(channel, 0);
        for(int i = 0; i < length ; i++){
            System.out.println(assessmentContent[i][0] + " " + assessmentContent[i][1] + " " + assessmentContent[i][2] + " " + assessmentContent[i][3]);
        } 
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
		        	this.currCorrectQuestions.put(channel, this.currCorrectQuestions.get(channel) + this.currAssessment.get(channel)[this.currQuestion.get(channel)][0] + "\n");
		        }
		        this.currQuestion.put(channel,this.currQuestion.get(channel)+1);
		        if(this.currQuestion.get(channel)==this.currAssessment.get(channel).length){
		            answer += "Assessment is over \n" + "You got " + this.score.get(channel) + "/" + this.currAssessment.get(channel).length + "Questions right! \n You got following Questions wrong: \n " + this.currCorrectQuestions.get(channel);
		            this.assessmentStarted.put(channel, null);
		            response.put("closeContext", "true");
		        } else {
		            answer += this.currAssessment.get(channel)[this.currQuestion.get(channel)][0];        
		        }
	        }
        } else {
        	System.out.println("Assessment type: "+ assessmentType + " not known");
        }
        response.put("text", answer);
        return response;
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
		if(assessmentStarted.get(channel) == null) {
			String wstoken = triggeredBody.getAsString("wstoken");
			String id = triggeredBody.getAsString("quizid");  
			String attemptId = "";
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(triggeredBody.getAsString("LMSURL"));
			System.out.println("Now connecting");
			HashMap<String, String> headers = new HashMap<String, String>();
			ClientResponse result = client.sendRequest("GET", "/webservice/rest/server.php?wstoken=" + wstoken + "&wsfunction=mod_quiz_start_attempt&quizid=" + id + "&moodlewsrestformat=json" , "",
					"", MediaType.APPLICATION_JSON, headers);
	        System.out.println("Connect Success");
	        System.out.println(result.getResponse());
	        JSONObject res = (JSONObject) p.parse(result.getResponse());
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
	        String[][] assessment = new String[((JSONArray) res.get("questions")).size()][3];
	        for(int i = 0 ; i < ((JSONArray) res.get("questions")).size() ; i++) {
	        	html =  ((JSONObject)((JSONArray) res.get("questions")).get(i)).getAsString("html");
	        	doc = Jsoup.parse(html);
	        //	if(((JSONObject)((JSONArray) res.get("questions")).get(i)).getAsString("type") == "truefalse") {
	        	// differentiate between true false and others, bcs for tf right answers are written  : the right answer is '' , whereas for other the ight answer is : 
	        		questions += doc.getElementsByClass("qtext").text() + "\n";
	        		assessment[i][0] = doc.getElementsByClass("qtext").text() +"\n";
	        		assessment[i][2] = ((JSONObject)((JSONArray) res.get("questions")).get(i)).getAsString("type");
	        		System.out.println(doc.getElementsByClass("qtext").text());
	       	//}		// to differentiate between questions with one answer and questions with multiple correct answers
	        		if(doc.getElementsByClass("rightanswer").text().contains("answers")) {
	        			assessment[i][0] += "Select one or more:\n";
	        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1] +"\n";
	        			assessment[i][1] = doc.getElementsByClass("rightanswer").text().split("The correct answers are")[1];
	        		} else {
	        			assessment[i][0] += "Select one:\n";
	        			answers += doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1] +"\n";
	        			assessment[i][1] = doc.getElementsByClass("rightanswer").text().split("The correct answer is")[1];
	        		}
	        		System.out.println(assessment[i][2]);
	        		if(assessment[i][2].equals("multichoice") || assessment[i][2].equals("truefalse")) {
	        			System.out.println("K");
	        			// check if answers or answer here ? 
	        			Elements multiChoiceAnswers = doc.getElementsByClass("ml-1");
	        			for(Element item : multiChoiceAnswers) {
	        				assessment[i][0] += item.text() + "\n";
	        				System.out.println(item.text() + "\n");
	        			}
	        			
	        			
	        		}
	        		
	        }
	        this.currQuestion.put(triggeredBody.getAsString("channel"), 0);
	        this.currAssessment.put(triggeredBody.getAsString("channel"), assessment);
	        this.currCorrectQuestions.put(channel, ""); 
	        JSONObject response = new JSONObject();
	        response.put("text", "We will now start the moodle quiz :) \n " + assessment[0][0]);
	        response.put("closeContext" , "false");
	        this.score.put(channel, 0);
	        assessmentStarted.put(channel,"true");
	        return Response.ok().entity(response).build();
		} else {
			return Response.ok().entity(continueAssessment(channel, triggeredBody.getAsString("intent"), triggeredBody, "moodleAssessment")).build();
			
			
		}  
	}   
    
    


}