package main.java;

import main.java.analyzer.SentenceAnalyzer;
import main.java.managers.HttpManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {

        SentenceAnalyzer analyzer = new SentenceAnalyzer();
        HttpManager httpManager = new HttpManager();


        /** Uncomment this line to wait for an incoming connection from the robot on port 5003 **/
        //httpManager.postManager(analyzer);

        // Calls test method
        Main.test(analyzer);

    }

    /** Use this method to test the application with
     *  some input sentences without using Choregraphe
     **/
    public static void test(SentenceAnalyzer analyzer) throws IOException {

        List<String> actions;

        System.out.println("\n\tINSTRUCTIONS:");
        System.out.println("Enter input sentences separated by \".\" (e.g. Zora stand up. mode HOLD. Zora raise your right arm.)");
        System.out.println("Enter sentences with multiple action linking them with \"and\" (e.g. Zora move up your head and open the hands.)");
        System.out.println("Enter \"mode x\" to change the robot operating mode, where x is HOLD or SEQUENTIAL.");
        System.out.println("Enter \"stop\" to quit.");

        while(true) {
            System.out.print("\nInput:");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = reader.readLine();

            if(input.contains("stop"))
                System.exit(0);

            List<String> sentences = new ArrayList<>(Arrays.asList(input.split("(?<=\\. )")));

            for (String sentence : sentences) {
                analyzer.resetActions();
                actions = analyzer.actionsExtraction(sentence);

                System.out.println("\n\n>>> List of actions in: " + sentence + " <<<\n");
                for (String action : actions) {
                    System.out.println(action);
                }
            }
        }
    }

}
