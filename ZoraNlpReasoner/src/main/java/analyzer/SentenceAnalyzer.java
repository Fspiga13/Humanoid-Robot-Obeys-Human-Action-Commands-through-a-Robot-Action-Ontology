/**
 * This class is the heart of semantic analysis.
 * It's here that the sentence given in input to recognize actions to be performed by the robot is analyzed.
 */

package main.java.analyzer;

import main.java.data.Word;
import main.java.managers.*;
import main.java.robot.*;

import java.util.*;


public class SentenceAnalyzer {

    private State robotState;
    private final String[] bodySide = {"left", "right"};

    private Map<String, List<String>> verbs;
    private Map<String, List<String>> nouns;
    private Map<String, List<String>> nlpTags;

    private Map<String, String> colors;

    private Map<String, Word> words;

    // List in which the extracted actions will be saved
    private List<String> actions;

    // Contains the words used as components for the extracted actions
    private List<String> searchedWords;

    /**
     * Analyzer builder.
     * Initialize verbs, nouns, colors and Stanford POStag.
     *
     */
    public SentenceAnalyzer(){

        verbs = OntologyManager.keywordsQuery("actions");
        nouns = OntologyManager.keywordsQuery("bodyParts");

        nlpTags = new HashMap<>();
        nlpTags.put("vb", new ArrayList<>(Arrays.asList("prt", "advmod", "ccomp", "xcomp", "nmod")));
        nlpTags.put("co", new ArrayList<>(Arrays.asList("dobj", "dep")));
        nlpTags.put("adj", new ArrayList<>(Arrays.asList("compound", "amod")));

        colors = OntologyManager.colorsQuery();

        words = new LinkedHashMap<>();
        actions = new ArrayList<>();
        searchedWords = new ArrayList<>();

        robotState = new State();
    }

    public void resetActions(){
        this.words.clear();
        this.actions.clear();
        this.searchedWords.clear();
    }

    /**
     * Method that extracts the robot's actions from the input sentence.
     * @param sentence sentence to analyze
     * @return list of found action codes
     */
    public List<String> actionsExtraction(String sentence) {

        String sentenceCopy = sentence.toLowerCase();

        if(sentenceCopy.contains("mode sequential"))
            return robotState.changeRobotMode(State.Mode.SEQUENTIAL);

        if (sentenceCopy.contains("mode hold"))
            return robotState.changeRobotMode(State.Mode.HOLD);

        if (this.words.isEmpty()) {
            // Extract RDF with Text2RDF tool
            String rdf = RdfManager.textToRdf(sentence, RdfManager.toolMode.OFFLINE);

            // Cleans the output
            words = RdfManager.getWordsNtags(rdf, sentence);
        }
        return this.logicalAnalysis(words);
    }

    /**
     * Performs the logical analysis of the sentence through POS tags and dependencies.
     * It relates verb, direct object and adjective
     * and extracts the action deriving from these three elements.
     * @param words hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     * @return list of found action codes
     */
    private List<String> logicalAnalysis(Map<String, Word> words) {

        String colorCode = "";
        String colorName = "";

        List<String> codes;
        List<String> components = new ArrayList<>();


        for (String word : words.keySet()) {

            components.clear();

            // Avoid reviewing words whose action was extracted and
            // which could lead to duplicate actions or multiple action errors
            if(!this.searchedWords.contains(word)) {

                searchedWords.add(word);

                // Verb recognized by VB tag
                if (words.get(word).getPosTag().startsWith("VB")) {
                    components.addAll(verbAnalysis(words, word));

                } else {
                    // Possible phrasal verb or with a verb interpreted as a name
                    if (words.get(word).getPosTag().startsWith("NN") || words.get(word).getPosTag().startsWith("JJR"))
                       components.addAll(nounAnalysis(words, word));
                }

                // QUERY: vb + dobj + adj
                if(!components.isEmpty()) {

                    /*
                    if (!components.get(0).isEmpty())
                        System.out.print("\nvb keyword: " + components.get(0));
                    if (!components.get(1).isEmpty())
                        System.out.print(" dobj keyword: " + components.get(1));
                    if (!components.get(2).isEmpty())
                        System.out.print(" adj keyword: " + components.get(2));
                    System.out.println();
                    */

                    // Understanding which query to use between single and compound action
                    if ((!components.get(1).isEmpty()) &&
                            (words.get(components.get(1)).getPosTag().equalsIgnoreCase("NNS"))) {

                        // If it has found a plural name it is a compound
                        codes = OntologyManager.compoundActionQuery(components.get(0), components.get(1));

                    } else {

                        // If it has no plural names
                        codes = OntologyManager.actionQuery(components.get(0), components.get(1), components.get(2));
                    }

                    if (codes.size() > 1) {

                        System.out.println("\n*** PROBLEM: " + codes.size() + " actions found. Incomplete action. *** ");
                        String c = "";
                        String lastCode = "";

                        for (String code : codes) {
                            // Remove any duplicates
                            if (!lastCode.equalsIgnoreCase(code))
                                c += code + "/";
                            lastCode = code;
                        }

                        // Error: incomplete action, MA: multiple action
                        c = "ERR-MA:" + c;

                        // Sends back multiple action error code
                        return new ArrayList<>(Arrays.asList(c));

                    } else {
                        if (codes.size() == 1) {
                            System.out.println("\n*** ACTION FOUND: "
                                    + components.get(0) + " " + components.get(2) + " "
                                    + components.get(1) + " CODE: " + codes.get(0));

                            // If the action found uses a color
                            if (this.actions.contains("ELER-C")) {
                                for (String w : words.keySet())
                                    if (colors.containsKey(w)) {
                                        colorCode = colors.get(w);
                                        colorName = w;
                                    }
                                this.actions.set(this.actions.indexOf("ELER-C"), "ELER-C:" + colorCode);
                            }

                            this.checkCompatibility(codes.get(0), components, colorName);
                        }
                    }
                }
            }
        }

        // If no action has been recognized in this sentence
        if (this.actions.isEmpty()) {
            System.out.println("\n*** PROBLEM: no action found (wrong verb, nuon or side)  ***");
            // No action found in sentence, NA: no action
            this.actions.add("ERR-NA");
        }

        robotState.printState();
        return this.actions;
    }


    /**
     * Checks if the found action is compatible with the robot status.
     * If it is compatible it adds to the actions found in the sentence.
     * @param newAction found action code
     * @param components list with verb, direct object and adjective
     * @param colorName color name, "" if not used
     */
    private void checkCompatibility(String newAction, List<String> components, String colorName){

        String compatibility = robotState.verifyActionCompatibility(newAction, actions);

        switch(compatibility){

            case "compatible":
                robotState.updateState(components.get(0), components.get(1), components.get(2), colorName);
                this.actions.add(newAction);
                break;
            default:
                // If the action is incompatible, INC: incompatible
                System.out.println("--- Incompatible action due to: " + compatibility);
                this.actions.add("ERR-INC:" + compatibility);
                break;
        }
        //System.out.println("\nCHECK: " + action);
    }

    /**
     * Checks if the action chosen by the user, to disambiguate a
     * non-specific action, is compatible with the robot status.
     * If it is compatible it adds it to the actions found in the sentence.
     * @param action action code to verify
     */
    public void verifyAction(String action) {

        List<String> components = OntologyManager.takeComponentsQuery(action);
        this.checkCompatibility(action, components, "");
    }

    /**
     * Performs the logical analysis of the sentence through POS tags and dependency.
     * It relates verb, object complement and adjective starting from the verb.
     * @param words hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     * @param word a word of hashMap words
     * @return list of found components
     */
    private List<String> verbAnalysis(Map<String, Word> words, String word) {

        String vb = "";
        String nn = "";
        String side = "";

        List<String> vbsTemp;

        // word may not be the complete verb (e.g. move up)
        vbsTemp = this.searchWithTags(words, word, nlpTags.get("vb"));

        if(!vbsTemp.isEmpty()) {
            int i = 0;
            String vbT = "";
            while(vb.isEmpty() && i < vbsTemp.size()) {

                vbT = word + " " + vbsTemp.get(i);

                //System.out.println("\n**\tPossible verb: " + vbT);

                // Search verb between keywords and synonyms
                vb = this.searchKeyword(verbs, vbT);
                i++;
            }
            if (!vb.isEmpty())
                this.searchedWords.add(vbT);
        }
        if(vb.isEmpty()) {
            // word may be the verb
            //System.out.println("\n**\tPossibile verbo: " + word);
            // Search verb between keywords and synonyms
            vb = this.searchKeyword(verbs, word);
        }

        if (!vb.isEmpty() && !vb.equalsIgnoreCase("delexical")) {

            // Search for direct object through dobj and dep tags
            List<String> compsOgg = this.searchWithTags(words, word, nlpTags.get("co"));

            String compOgg = "";
            if(!compsOgg.isEmpty()) {
                int i = 0;
                while(nn.isEmpty() && i < compsOgg.size()) {

                    compOgg = compsOgg.get(i);

                    //System.out.println("\n**\tPossibile comp ogg: " + compOgg);

                    // Search verb between keywords and synonyms
                    nn = this.searchKeyword(nouns, compOgg);
                    i++;
                }
            }

            if (!nn.isEmpty()) {

                this.searchedWords.add(compOgg);
                // Search body side
                side = this.searchSide(words, compOgg, nlpTags.get("adj"));

                if(!side.isEmpty())
                    this.searchedWords.add(side);

                //System.out.println("\tLato azione: " + side);
            }

        } else {
            // the verb is delexical and dobj indicates the keyword for the action

            // Search for keyword through dobj and dep tags
            vbsTemp = this.searchWithTags(words, word, nlpTags.get("co"));

            if(!vbsTemp.isEmpty()) {
                int i = 0;
                String vbT = "";
                while (vb.isEmpty() && i < vbsTemp.size()) {

                    vbT = vbsTemp.get(i);

                    //System.out.println("\n**\tPossibile keyword: " + vbT);

                    // Search verb between keywords and synonyms
                    vb = this.searchKeyword(verbs, vbT);
                    i++;
                }
                if (!vb.isEmpty())
                    this.searchedWords.add(vbT);
            }
        }

        return new ArrayList<>(Arrays.asList(vb,nn,side));
    }

    /**
     * Performs the logical analysis of the sentence through POS tags and dependency.
     * It relates verb, object complement and adjective starting from
     * a possibile phrasal verb or a verb interpreted as a noun.
     * @param words hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     * @param word a word of hashMap words
     * @return list of found components
     */
    private List<String> nounAnalysis(Map<String, Word> words, String word) {

        String vb = "";
        String nn = "";
        String side = "";

        List<String> vbsTemp;

        // word may not be the complete verb (e.g. move up)
        vbsTemp = this.searchWithTags(words, word, nlpTags.get("vb"));


        if(!vbsTemp.isEmpty()) {
            int i = 0;
            String vbT = "";
            while(vb.isEmpty() && i < vbsTemp.size()) {

                this.searchedWords.add(vbsTemp.get(i));
                vbT = word + " " + vbsTemp.get(i);

                //System.out.println("\n**\tPossibile verbo interpretato come nome: " + vbT);

                // Searches verb between keywords and synonyms
                vb = this.searchKeyword(verbs, vbT);
                i++;
            }
            if (!vb.isEmpty())
                this.searchedWords.add(vbT);
        }
        if(vb.isEmpty()) {
            // word may be the verb
            //System.out.println("\n**\tPossibile verbo: " + word);
            // Searches verb between keywords and synonyms
            vb = this.searchKeyword(verbs, word);
        }

        // Searches for direct object through dobj and dep tags
        List<String> compsOgg = this.searchWithTags(words, word, nlpTags.get("co"));

        String compOgg = "";
        if(!compsOgg.isEmpty()) {
            int i = 0;
            while(nn.isEmpty() && i < compsOgg.size()) {

                compOgg = compsOgg.get(i);

                //System.out.println("\n**\tPossibile comp ogg: " + compOgg);

                // Searches verb between keywords and synonyms
                nn = this.searchKeyword(nouns, compOgg);
                i++;
            }
        }

        if (!nn.isEmpty()) {
            this.searchedWords.add(compOgg);

            // Searches body side
            side = this.searchSide(words, compOgg, nlpTags.get("adj"));

            if (!side.isEmpty())
                this.searchedWords.add(side);

            //System.out.println("\tLato azione: " + side);
        }

        return new ArrayList<>(Arrays.asList(vb,nn,side));
    }

    /**
     * Search for a word in the synonyms list and return the reference keyword.
     * @param words hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     * @param word a word of hashMap words
     * @return matching keyword, "" otherwise
     */
    private String searchKeyword(Map<String, List<String>> words, String word) {

        for(String keyword : words.keySet()) {
            if(keyword.equalsIgnoreCase(word.toLowerCase()) || words.get(keyword).contains(word.toLowerCase())) {
                //System.out.println("\tHo trovato: " + keyword);
                return keyword;
            }
        }
        return "";
    }

    /**
     * Searches dependencies through a list of dependency tags.
     * @param words hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     * @param word word to search in words
     * @param nlpTags dependency tag list used by Stanford
     * @return matching keywords, "" otherwise
     */
    private List<String> searchWithTags(Map<String, Word> words, String word, List<String> nlpTags) {

        List<String> deps = new ArrayList<>();
        for(String tag : nlpTags) {
            if(words.get(word).getDependencies(tag) != null)
                 deps.addAll(words.get(word).getDependencies(tag));

        }
        return deps;
    }


    /**
     * Searches the body side through a list of dependencies tag.
     * @param words hashMap which contains the words of the sentence
     * as a key and the part of speech as a value
     * @param word a word of hashMap words
     * @param nlpTags dependency tag list used by Stanford
     * @return matching body side, "" otherwise
     */
    private String searchSide(Map<String, Word> words, String word, List<String> nlpTags) {

        for(String tag : nlpTags) {
            List<String> matches = words.get(word).getDependencies(tag);
            for(String match : matches) {
                if (match != null && (match.equalsIgnoreCase(bodySide[0]) || match.equalsIgnoreCase(bodySide[1])))
                    return match;
            }
        }
        return "";
    }

}
