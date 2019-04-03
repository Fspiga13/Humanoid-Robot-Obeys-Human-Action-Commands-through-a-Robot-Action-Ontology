/**
 * This class manages the RDF.
 */

package main.java.managers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import main.java.data.Pair;
import main.java.data.Word;

public class RdfManager {

    private static final String CORENLP = "\"http://it.unica.xproject/corenlp#\"";
    private static final String CORENLP_DEP = "\"http://it.unica.xproject/corenlp#hasDependency:\"";
    private static final String CORENLP_DEP_NMOD = "\"http://it.unica.xproject/corenlp#hasDependency:nmod:\"";
    private static final String CORENLP_DEP_COMPOUND = "\"http://it.unica.xproject/corenlp#hasDependency:compound:\"";
    private static final String CORENLP_DEP_ADVCL = "\"http://it.unica.xproject/corenlp#hasDependency:advcl:\"";

    /***
     * Text2RDF tool operating modes.
     *
     */
    public enum toolMode {
        ONLINE,
        OFFLINE
    }


    /**
     * Gets the RDF of the input sentence, using the Text2RDF tool.
     * It can be run online via a curl call or locally.
     * @param sentence user's input sentence
     * @param mode running online or offline
     * @return string with entire RDF
     */
    public static String textToRdf(String sentence, toolMode mode) {

        String[] command = null;

        switch(mode) {
            case ONLINE:
                // Usa versione online del tool
                String input = "input_text=" + sentence + "&output_format=rdf/xml&mode=1";
                command = new String[]{"curl", "glab.sc.unica.it/text2rdf/", "-X", "POST", "-H", "Content-Type: application/x-www-form-urlencoded", "-d", input};
                break;
            case OFFLINE:
                // Usa versione in locale del tool
                command = new String[]{"python", /*"../../../textToRdf/src/xproject.py"*/"textToRdf/src/xproject.py", "-t", sentence};
                break;
        }

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    /**
     * This method extracts useful parts from the entire RDF.
     * In particular: POS tag and dependency.
     * @param rdf string with entire RDF
     * @param sentence user's input sentence
     * @return hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     */
    public static Map<String, Word> getWordsNtags(String rdf, String sentence) {

        List<Pair<String, String>> stanfordPrefixes = new ArrayList<>();
        Map<String, Word> words = new LinkedHashMap<>();

        int index, nextIndex;

        try {
            rdf = rdf.substring(rdf.indexOf("RDF"));

        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("--- PROBLEMA DI CONNESSIONE CON STANFORD CORENLP ---");
            System.exit(0);
        }

        //System.out.println(rdf);

        // Searchs Stanford prefixes in RDF
        while(rdf.indexOf("xmlns:ns") > 0){

            rdf = rdf.substring(rdf.indexOf("xmlns:ns") + 6);

            String prefix = rdf.substring(rdf.indexOf("=") + 1, rdf.indexOf("\n"));

            //System.out.println("\t" + rdf.substring(0,3) + "  " + prefix);

            // Links the prefix name to the true prefix
            switch(prefix){
                case CORENLP:
                    stanfordPrefixes.add(new Pair<>(rdf.substring(0, 3) + ":postype", "postype"));
                    break;
                case CORENLP_DEP:
                    stanfordPrefixes.add(new Pair<>(rdf.substring(0, 3), "dep"));
                    break;
                case CORENLP_DEP_COMPOUND:
                    stanfordPrefixes.add(new Pair<>(rdf.substring(0, 3), "compund"));
                    break;
                case CORENLP_DEP_NMOD:
                    stanfordPrefixes.add(new Pair<>(rdf.substring(0, 3), "nmod"));
                    break;
                case CORENLP_DEP_ADVCL:
                    stanfordPrefixes.add(new Pair<>(rdf.substring(0, 3), "advcl"));
                    break;
                default:
                    break;
            }

        }

        // Cleans the original sentence from any punctuation
        sentence = sentence.replaceAll("\\p{Punct}","");

        // Splits the sentence word by word
        String[] newSentence = sentence.split(" ");

        // Inserts the words of the sentence into Word objects
        for (String w : newSentence)
            words.put(w, new Word());

        // For each word in the sentence looks for tags in the RDF
        for(String word : words.keySet()){

            //System.out.println("\n" + word + ":");

            if(rdf.indexOf(word + "\">\n") != rdf.lastIndexOf(word + "\">\n")){
                int temp, temp2;

                temp = rdf.indexOf(word + "\">\n");
                temp2 = rdf.lastIndexOf(word + "\">\n");

                if(Character.isDigit(rdf.charAt(temp -2)))
                    index = temp;
                else
                    index = temp2;

            }else {
                index = rdf.indexOf(word + "\">\n");
            }

            nextIndex = rdf.indexOf("</rdf:Description>", index);

            String app = rdf.substring(index, nextIndex);

            //System.out.println("\n" + app + "\n");

            // Until there are other lines ("\n")
            while (app.indexOf("\n") != app.lastIndexOf("\n")) {

                // Changes row and moves to the tag
                app = app.substring(app.indexOf("\n") + 6);

                // For each TAG
                for (Pair stanfordTag : stanfordPrefixes) {

                    String tag = (String) stanfordTag.getLeft();
                    String help = (String) stanfordTag.getRight();

                    // If row begins with a tag
                    if (app.startsWith(tag)) {

                        // If the tag is :postype
                        if (help.equalsIgnoreCase("postype")) {
                            index = app.indexOf(">");
                            nextIndex = app.indexOf("</" + tag);

                            String component = app.substring(index + 1, nextIndex);

                            words.get(word).setPosTag(component);

                            //System.out.println("\tComponent: " + words.get(word).getPosTag());

                        } else {

                            // Other tags

                            index = app.indexOf(":");
                            nextIndex = app.indexOf(" ");

                            String dependency = app.substring(index + 1, nextIndex);

                            if (help.equalsIgnoreCase("nmod"))
                                dependency = "nmod";

                            //System.out.print("\tTag: " + dependency + " hasDependency ");

                            index = app.indexOf("offset_") + 7;
                            app = app.substring(index);
                            nextIndex = app.indexOf("\"/>");

                            index = app.substring(0, nextIndex).lastIndexOf("_") + 1;

                            String reference = app.substring(index, nextIndex);

                            //System.out.print(reference + "\n");

                            words.get(word).addDependency(dependency, reference);

                        }

                        // The tag of this row has already been found
                        break;
                    }
                }
            }
        }

        return words;
    }
}


