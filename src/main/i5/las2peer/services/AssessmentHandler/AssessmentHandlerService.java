package i5.las2peer.services.AssessmentHandler;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.UserAgent;
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
				return Response.ok().entity(continueAssessment(channel, bodyJson.getAsString("intent"))).build();
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
        int[] sequence = new int[length];
        this.currAssessment.put(channel, assessmentContent);
        this.currQuestion.put(channel, 0);
        this.currCorrectQuestions.put(channel, ""); 
        this.score.put(channel, 0);
        System.out.println("Curr number is :" + this.currQuestion.get(channel));
        for(int i = 0; i < length ; i++){
            System.out.println(assessmentContent[i][0] + " " + assessmentContent[i][1] + " " + assessmentContent[i][2] + " " + assessmentContent[i][3]);
        } 
        this.assessmentStarted.put(channel, "true");	
        System.out.println(channel);
  		
	}
	
    private JSONObject continueAssessment(String channel, String intent){
    	JSONObject response = new JSONObject();
    	String answer = "";
    	response.put("closeContext", "false");
        System.out.println(this.currQuestion.get(channel));
        System.out.println(this.currAssessment.get(channel)[this.currQuestion.get(channel)][2]);
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
        response.put("text", answer);
       return response;
	
    }


}