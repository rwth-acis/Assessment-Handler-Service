# Assessment-Handler-Service

This service is used to conduct assessments with bots created with the current Social-Bot-Framework. The service will handle organizing the assessment content. The assessment content itself can be given during the bot modeling or can be extracted from the moodle platform.

Build
--------
Execute the following command on your shell:

```shell
ant all 
```
Start
--------

To start the service, use one of the available start scripts:

Windows:

```shell
bin/start_network.bat
```

Unix/Mac:
```shell
bin/start_network.sh
```

Conducting a Moodle Quiz
--------
To start a Moodle Quiz, a first RESTful POST request containing a JSON Body is necessary:
```
POST <service-address>/AssessmentHandler/moodleQuiz
```
The JSON Body will need following attributes to be added by the user during the modeling: 
- LMSURL: The domain of your moodle instance.
- wstoken: The authentication token you receive to able to access moodle's webservices
- courseId: The courseId of the courses you want your quizzes to be extracted from (You can find the courseId by simply going to the specific course and read which id is written in the URL  "/course/view.php?id=courseId"). You can also give multiple courseId's if you want quizzes from multiple courses to be extracted.   
- quitIntent: The name of the intent which lets the service recognize that you want to stop the assessment.  
