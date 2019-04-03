/**
 * This class is used to exchange data with the Zora Robot
*/

package main.java.managers;

import main.java.analyzer.SentenceAnalyzer;

import java.io.*;
import java.net.*;
import java.util.List;


public class HttpManager {

    /**
     * When the method is started, waits for a connection.
     * Once the POST request is received, containing user input,
     * it calls up the methods to extract the action codes
     * and sends them in response to the POST.
     * @param analyzer input's sentence analyzer
     */
    public void postManager(SentenceAnalyzer analyzer) {

        try {

            //String sentence = "";
            System.out.println("\nListening for connection on port 5003 ....");

            int port = 5003;

            ServerSocket ss = new ServerSocket(port);

            // Enters infinite loop waiting for connections and manages them
            for (;;) {

                // Waits for a client to connect
                Socket client = ss.accept();

                InputStream is = client.getInputStream();
                InputStreamReader isReader = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isReader);

                // Reads and prints the request header
                System.out.println("\n");
                String headerLine = null;
                while((headerLine = br.readLine()).length() != 0){
                    System.out.println(headerLine);
                }

                // Reads the POST request payload
                StringBuilder payload = new StringBuilder();
                while(br.ready()){
                    payload.append((char) br.read());
                }
                System.out.println("Payload data is: " + payload.toString());

                // Get outgoing streams to respond the client
                PrintWriter out = new PrintWriter(client.getOutputStream());
                String inputData = payload.toString();
                String outputData;

                outputData = this.dataManager(inputData, analyzer);

                // Outgoing header
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Server: localhost:5003\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("Content-Length:" + outputData.length() + "\r\n");
                out.write("\r\n");

                // Output the action codes
                out.write(outputData);
                out.flush();

                out.close();
                is.close();
                client.close();

            } // New cycle, waits for the next connection

        }catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java HttpMirror <port>");
        }
    }

    /**
     * The method manages the data received as input to the robot.
     * Specifically: it recalls the methods for extracting the action
     * codes from the sentence and solves the particular case of
     * sentences with incomplete actions.
     * @param inputData data received as input by the robot
     * @param analyzer input's sentence analyzer
     * @return action codes list to send to the robot
     */
    public String dataManager(String inputData, SentenceAnalyzer analyzer) {

        String sentence = "";
        String allActions = "";
        List<String> actions;

        // Incoming action code:
        // adds the action if compatible and continues the analysis of the sentence
        if(inputData.startsWith("*")){
            System.out.println(inputData);
            inputData = inputData.substring(1);
            analyzer.verifyAction(inputData);
            actions = analyzer.actionsExtraction(sentence);
        } else {
            // Input sentence to analyze:
            // sends the extracted action codes back

            sentence = inputData;
            analyzer.resetActions();
            actions = analyzer.actionsExtraction(sentence);
        }

        System.out.println("\n\n>>> List of actions in: " + sentence + " <<<\n");
        for (String action : actions)
            System.out.println(action);


        // Compact the action codes into a single string
        for (String a : actions) {
            // Action codes separated by ;
            allActions = allActions + a + ";";
        }
        //System.out.println(allActions);

        return allActions;
    }
}
