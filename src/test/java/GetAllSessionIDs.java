import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileWriter;
import java.io.IOException;

public class GetAllSessionIDs {
    public static void main(String args[]) throws UnirestException {
        GenerateTextLogsToPDF generateTextLogsToPDF = new GenerateTextLogsToPDF();
        String buildID = "e37949fd085624012389502342e57318c3914411";

        try {
            FileWriter myWriter = new FileWriter("src/test/resources/sessions.txt");

            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.get("https://api.browserstack.com/automate/builds/"+buildID+"/sessions.json?limit=100")
                    .header("Authorization", generateTextLogsToPDF.basicAuthHeaderGeneration())
                    .asString();
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(response.getBody());
            System.out.println(jsonArray.size());
            JSONObject automateSessionResponseJSON=null, getAutomateSessions=null, getAutomateSessionsID = null;
            for(int i=0;i<jsonArray.size();i++){
                automateSessionResponseJSON = (JSONObject) jsonArray.get(i);
                getAutomateSessions = (JSONObject) parser.parse(automateSessionResponseJSON.get("automation_session").toString());
                //System.out.println(getAutomateSessions.get("hashed_id"));
                myWriter.write(getAutomateSessions.get("hashed_id").toString()+"\n");
            }

            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}
