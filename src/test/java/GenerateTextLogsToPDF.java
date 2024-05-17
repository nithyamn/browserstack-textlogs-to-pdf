import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;

public class GenerateTextLogsToPDF {
    public String username = System.getenv("BROWSERSTACK_USERNAME");
    public String accesskey = System.getenv("BROWSERSTACK_ACCESS_KEY");
    public void createPDFReport(String sessionID) {
        Document document = null;
        try {
            String textLogs = getTextLogs(sessionID); //pass the session id to the Unirest library to fetch the raw (text) logs
            ArrayList<String> imageLinks = getImageLinks(textLogs); //fetching all the screenshot links from the raw (text) logs
            String dest = "src/test/resources/" + sessionID + ".pdf"; //the pdf file will be saved by the name of the sessionID of the test
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            document = new Document(pdf);
            document.add(new Paragraph(textLogs)); //adding all the raw (text) logs first to the pdf
            for (int i = 0; i < imageLinks.size(); i++) {
                String imFile = imageLinks.get(i);
                ImageData data = ImageDataFactory.create(imFile); //looping through all image URLs retrieved from the array list and converting it to an image
                Image image = new Image(data);
                document.add(image); //adding image to the PDF file towards the end in the same sequence of execution
                System.out.println("Image " + i + " added");
            }
            document.close();
        } catch (Exception e) {
            document.close();
            System.out.println("EXCEPTION ----->");
            e.printStackTrace();
        }
    }
    public String getTextLogs(String sessionId) throws IOException, UnirestException, ParseException {
        //fetches all the session details from the session id
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> getRawLogsFromSessionLogs = Unirest.get("https://api.browserstack.com/automate/sessions/"+sessionId+".json")
                .header("Authorization", basicAuthHeaderGeneration())
                .asString();

        //parses the JSON response received from the session details
        JSONParser parser = new JSONParser();
        JSONObject automateSessionResponseJSON = (JSONObject) parser.parse(getRawLogsFromSessionLogs.getBody());

        //parse the first level of JSON response received from the session details to get the actual URL for raw (text) logs
        JSONObject getRawLogsURL = (JSONObject) parser.parse(automateSessionResponseJSON.get("automation_session").toString());

        //the raw logs retrieves starts with automate.browserstack.com whereas for the REST API we need api.browserstack.com endpoint, hence its structured in the following way:
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.get("https://api.browserstack.com/automate/builds"+getRawLogsURL.get("logs").toString().split("/builds")[1])
                .header("Authorization", basicAuthHeaderGeneration())
                .asString();
        return response.getBody();
    }
    public ArrayList<String> getImageLinks(String textLogString){
        ArrayList<Integer> start = new ArrayList<>();
        ArrayList<Integer> end = new ArrayList<>();
        //fetching the start index and end index of the log lines consisting of "debug" and ".jpeg"/".png" from the entire test log to display the screenshot in the PDF
        String searchStartSting = "https://automate.browserstack.com/s3-debug";
        String searchEndStringJPEG = ".jpeg";
        String searchEndStringPNG = ".png";
        ArrayList<String> imageLinks = new ArrayList<>();

        boolean flag1 = false, flag2 = false;
        //adding all the start indexes of the screenshot logs to an array list
        for (int i = 0; i < textLogString.length() - searchStartSting.length() + 1; i++) {
            if (textLogString.substring(i, i + searchStartSting.length()).equals(searchStartSting)) {
                start.add(i);
                flag1 = true;
            }
        }
        //adding all the end indexes of the screenshot logs to an array list
        for (int i = 0; i < textLogString.length() - searchEndStringJPEG.length() + 1; i++) {
            if (textLogString.substring(i, i + searchEndStringJPEG.length()).equals(searchEndStringJPEG)) {
                end.add(i + searchEndStringJPEG.length());
                flag2 = true;
            }else if(textLogString.substring(i, i + searchEndStringPNG.length()).equals(searchEndStringPNG)){
                end.add(i + searchEndStringPNG.length());
                flag2 = true;
            }
        }

        if (flag1 == false || flag2 == false) {
            System.out.println("NONE");
        }
        //checking if the size of both arrays are same which should ideally be the case
        if(start.size() == end.size()){
            for(int i=0;i<start.size();i++){
                //fetching the exact URL for the screenshots and adding it to an array list
                System.out.println(textLogString.substring(start.get(i), end.get(i)));
                imageLinks.add(textLogString.substring(start.get(i), end.get(i)));
            }
        }
        return imageLinks;
    }


    public String basicAuthHeaderGeneration(){
        //creating Base64 string of the browserstack username and accesskey
        String authCreds = username+":"+accesskey;
        return "Basic " + Base64.getEncoder().encodeToString(authCreds.getBytes());
    }
}
