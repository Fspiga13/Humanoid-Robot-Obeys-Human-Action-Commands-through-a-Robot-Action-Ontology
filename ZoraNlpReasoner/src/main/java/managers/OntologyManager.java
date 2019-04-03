/**
 * This class handles queries to ontologies.
 */

package main.java.managers;

import main.java.data.Pair;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class OntologyManager {

    private static final String ROBOT_ONTO_PATH = "Ontologies/ZoraActionsOnto.owl";
    private static final String COLORS_ONTO_PATH = "Ontologies/ColorsOnto.owl";

    /*
    private static final String ROBOT_ONTO_PATH = "../../../Ontologies/ZoraActionsOnto.owl";
    private static final String COLORS_ONTO_PATH = "../../../Ontologies/ColorsOnto.owl";
    */

    private static final String PREFIX = "" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
            "PREFIX Colors: <http://www.semanticweb.org/federico-spiga/ontologies/Colors#> " +
            "PREFIX ZoraActions: <http://www.semanticweb.org/federico-spiga/ontologies/ZoraActions#> ";

    // Delexical verbs
    private static final List<String> DELEXICAL =
            new ArrayList<>(Arrays.asList("do", "go", "make", "perform", "take", "turn"));

    private static final OntModel colorsOnto = OntologyManager.readOntology(COLORS_ONTO_PATH);
    private static final OntModel zoraActionOnto = OntologyManager.readOntology(ROBOT_ONTO_PATH);

    /**
     * Executes a query to read the robot operating mode from the ontology.
     * @return initial operating mode
     */
    public static String OperatingModeQuery(){

        OntModel model = zoraActionOnto;

        String req =
            PREFIX +
            " SELECT ?modeName " +
            " WHERE { ?mode rdf:type ZoraActions:RobotOperatingMode . " +
            " ?mode ZoraActions:hasOperatingMode ?modeName } " ;

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

       String mode = "";

        while (res.hasNext()) {
            QuerySolution qs = res.next();
            // Compone il codice dell'azione, costituito da:
            // 2 caratteri per la parte del robot
            // 1 o 2 caratteri per l'azione da far eseguire alla parte del corpo
            mode = qs.getLiteral("modeName").getString();
        }

        return mode;
    }

    /**
     * Executes a query to get the action codes that can be performed with the input data parameters.
     * @param verb verb keyword of action
     * @param dobj robot body part, "" if use the whole robot or is not specified
     * @param side body side "left" or "right", "" if not specified
     * @return list of found action codes
     */
    public static List<String> actionQuery(String verb, String dobj, String side) {

        OntModel model = zoraActionOnto;

        String req =
            PREFIX +
            " SELECT ?individual ?partCode ?actionCode " +
            " WHERE{ " + "?word ZoraActions:keyword \"" + verb + "\"^^xsd:string . " +
            " ?action ZoraActions:uses ?word . " +
            " ?action ZoraActions:code ?actionCode ." +
            " ?action ZoraActions:involves ?individual . " +
            " ?individual ZoraActions:code ?partCode . " +
            " OPTIONAL{ ?individual ZoraActions:uses ?s . " +
            " ?s ZoraActions:keyword ?bp } " +
            " OPTIONAL{ ?individual ZoraActions:bodySide ?sd } " +
            " FILTER( ((?bp) =\"" + dobj + "\"^^xsd:string && \"" + dobj +
            "\"^^xsd:string != \"\"^^xsd:string ) ||  \"" + dobj + "\"^^xsd:string = \"\"^^xsd:string ) " +
            " FILTER( ((?sd) =\"" + side + "\"^^xsd:string && \"" + side +
            "\"^^xsd:string != \"\"^^xsd:string ) || \"" + side + "\"^^xsd:string = \"\"^^xsd:string ) } ";

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

        List<String> codes = new ArrayList<>();

        while (res.hasNext()) {
            QuerySolution qs = res.next();
            // Compone il codice dell'azione, costituito da:
            // 2 caratteri per la parte del robot
            // 1 o 2 caratteri per l'azione da far eseguire alla parte del corpo
            codes.add(qs.getLiteral("partCode").getString() + "-" + qs.getLiteral("actionCode").getString());
        }


        return codes;
    }

    /**
     * Executes a query to get the compund action codes that can be performed with the input data parameters.
     * @param verb verb keyword of action
     * @param dobj robot body part, "" if use the whole robot or is not specified
     * @return list of found action codes
     */
    public static List<String> compoundActionQuery(String verb, String dobj) {

        OntModel model = zoraActionOnto;

        String req =
            PREFIX +
            " SELECT ?actionCode ?partCode " +
            " WHERE { ?word ZoraActions:keyword ?actionWord . " +
            " ?action ZoraActions:uses ?word . " +
            " ?action ZoraActions:code ?actionCode . " +
            " ?action ZoraActions:involvesBoth ?part . " +
            " ?part ZoraActions:code ?partCode . " +
            " ?part ZoraActions:usesBoth ?word2 . " +
            " ?word2 ZoraActions:keyword \"" + dobj + "\"^^xsd:string . " +
            " FILTER( ((?actionWord) =\"" + verb + "\"^^xsd:string && \"" + verb +
            "\"^^xsd:string != \"\"^^xsd:string ) ||  \"" + verb + "\"^^xsd:string = \"\"^^xsd:string ) } ";

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

        List<String> codes = new ArrayList<>();

        Map<String, List<String>> temp = new HashMap<>();

        while (res.hasNext()) {
            QuerySolution qs = res.next();
            // Compone il codice dell'azione, costituito da:
            // 2 caratteri per la parte del robot
            // 1 o 2 caratteri per l'azione da far eseguire alla parte del corpo

            String ac = qs.getLiteral("actionCode").getString();

            temp.putIfAbsent(ac, new ArrayList<>());
            temp.get(ac).add(qs.getLiteral("partCode").getString());
        }

        for(String action : temp.keySet()) {
            codes.add((temp.get(action).get(0).charAt(0)) + "L" +
                      (temp.get(action).get(1).charAt(0)) + "R" +
                        "-" + action);
        }

        return codes;
    }

    /**
     * Performs a query to get the keywords and synonyms associated with the input type.
     * @param type "actions" to return actions, "bodyParts" to return body parts
     * @return hashMap with keywords as keys and the list of synonyms as values
     */
    public static Map<String, List<String>> keywordsQuery(String type){

        OntModel model = zoraActionOnto;

        String var = "";

        switch(type){
            case "actions":
                var = "ActionWord";
                break;
            case "bodyParts":
                var = "BodyPartWord";
                break;
            default:
                break;
        }

        String req =
            PREFIX +
            " SELECT ?keyword ?synonym " +
            " WHERE {?word rdf:type ZoraActions:"+ var +
            " . ?word ZoraActions:keyword ?keyword . " +
            " ?word ZoraActions:synonym ?synonym } ";

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

        Map<String, List<String>> keywords = new LinkedHashMap<>();

        while (res.hasNext()) {
            QuerySolution qs = res.next();

            String kw = qs.getLiteral("keyword").getString();
            String sn = qs.getLiteral("synonym").getString();

            // Se la keyword non è presente, la aggiungo e le associo la lista
            keywords.putIfAbsent(kw, new ArrayList<>());
            // Aggiungo il sinonimo
            keywords.get(kw).add(sn);
        }

        if(type.equalsIgnoreCase("actions"))
            keywords.put("delexical", DELEXICAL);

        /*
        for(String k : keywords.keySet()) {
            System.out.println(k);
            for (String s : keywords.get(k)) {
                System.out.println("\t" + s);
            }
        }
        */

        return keywords;
    }

    /**
     * Performs a query to get the actions incompatible
     * with the new action to perform.
     * @param newActionCode new action code
     * @return pairs list containing incompatible previous actions
     */
    public static List<Pair<String, String>> compatibilityQuery(String newActionCode){

        OntModel model = zoraActionOnto;

        String part = newActionCode.substring(0,newActionCode.indexOf("-"));
        String action = newActionCode.substring(newActionCode.indexOf("-") +1);

        if(action.startsWith("C"))
            action = "C";

        String req;

        if(part.length() == 4){
            String part2 = part.substring(2);
            part = part.substring(0,2);

            req = PREFIX +
                "SELECT  distinct ?incompatibleActionName ?incompatibleActionPart " +
                " WHERE { ?part ZoraActions:code \"" + part + "\"^^xsd:string . " +
                " ?part2 ZoraActions:code \"" + part2 + "\"^^xsd:string . " +
                " ?action ZoraActions:involvesBoth ?part . " +
                " ?action ZoraActions:involvesBoth ?part2 . " +
                " ?action ZoraActions:code \""+ action + "\"^^xsd:string . " +
                " ?action ZoraActions:incompatibleWithPrevious ?incompatibleAction ." +
                " ?incompatibleAction ZoraActions:uses ?actionWord ." +
                " ?actionWord ZoraActions:keyword ?incompatibleActionName ." +
                " OPTIONAL { ?incompatibleAction ZoraActions:involves ?incPart ." +
                " ?incPart ZoraActions:uses ?partWord . " +
                " ?partWord ZoraActions:keyword ?incompatibleActionPart } }";
        } else {
            req = PREFIX +
                " SELECT distinct ?incompatibleActionName ?incompatibleActionPart" +
                " WHERE { ?part ZoraActions:code \"" + part + "\"^^xsd:string . " +
                " ?action ZoraActions:involves ?part . " +
                " ?action ZoraActions:code \"" + action + "\"^^xsd:string . " +
                " ?action ZoraActions:incompatibleWithPrevious ?incompatibleAction . " +
                " ?incompatibleAction ZoraActions:uses ?actionWord . " +
                " ?actionWord ZoraActions:keyword ?incompatibleActionName . " +
                " OPTIONAL { ?incompatibleAction ZoraActions:involves ?incPart . " +
                " ?incPart ZoraActions:uses ?partWord . " +
                " ?partWord ZoraActions:keyword ?incompatibleActionPart } }";
        }

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

        List<Pair<String, String>> incompatibleActions = new ArrayList<>();

        while (res.hasNext()) {
            QuerySolution qs = res.next();

            String incAction = qs.getLiteral("incompatibleActionName").getString();
            String incPart = null;

            if(qs.contains("incompatibleActionPart"))
                incPart = qs.getLiteral("incompatibleActionPart").getString();

            incompatibleActions.add(new Pair<>(incAction, incPart));
        }

        /*
        for(Pair<String,String> p : incompatible){
            System.out.println(p.getLeft() + "  " + p.getRight());
        }
        */

        return incompatibleActions;
    }


    /**
     * Executes a query to get the components of the
     * action that make up the input action code.
     * (e.g. AR-0: up arm right)
     * @param newActionCode action code
     * @return three component list, if one is not used returns ""
     */
    public static List<String> takeComponentsQuery(String newActionCode){

        OntModel model = zoraActionOnto;

        String part = newActionCode.substring(0,newActionCode.indexOf("-"));
        String action = newActionCode.substring(newActionCode.indexOf("-") +1);

        String req =
                PREFIX +
                " SELECT ?wordKey ?bpKey ?sd " +
                " WHERE { " +
		        " ?bp ZoraActions:code \"" + part + "\"^^xsd:string . " +
                " OPTIONAL{ ?bp ZoraActions:uses ?bpname . " +
                " ?bpname ZoraActions:keyword ?bpKey . }" +
                " ?action ZoraActions:involves ?bp . " +
                " ?action ZoraActions:code \"" + action + "\"^^xsd:string . " +
                " ?action ZoraActions:uses ?word . " +
                " ?word ZoraActions:keyword ?wordKey . " +
                " OPTIONAL{	?bp ZoraActions:bodySide ?sd .} }";

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

        List<String> components = new ArrayList<>();

        while (res.hasNext()) {
            QuerySolution qs = res.next();
            // Compone la lista di componenti, costituito da:
            // verbo, nome e parte del corpo
            components.add(qs.getLiteral("wordKey").getString());
            components.add(qs.getLiteral("bpKey").getString());
            components.add(qs.getLiteral("sd").getString());
        }

        return components;
    }


    /**
     * Runs the query to get the colors with relative rgb.
     * @return hashMap with color names as keys and
     * rgb data as values
     */
    public static Map<String, String> colorsQuery(){

        OntModel model = colorsOnto;

        String req =
            PREFIX +
            " SELECT ?name ?red ?green ?blue " +
            " WHERE { ?color Colors:name ?name . " +
            " ?color Colors:rgbCoordinateRed ?red . " +
            " ?color Colors:rgbCoordinateGreen ?green . " +
            " ?color Colors:rgbCoordinateBlue ?blue }" ;

        //System.out.println("\n\nQuery: "+ req + "\n");

        Query query = QueryFactory.create(req);

        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet res = qe.execSelect();

        //ResultSetFormatter.out(System.out, res, query);

        Map<String, String> colors = new HashMap<>();

        while (res.hasNext()) {
            QuerySolution qs = res.next();

            colors.put(qs.getLiteral("name").getString(),
                (qs.getLiteral("red").getInt()) + "," +
                (qs.getLiteral("green").getInt()) + "," +
                (qs.getLiteral("blue").getInt()));
        }

        //System.out.println(rgb);

        return colors;
    }

    /**
     * Method used to read the ontology from file
     * @param path ontology path
     * @return ontology model
     */
    private static OntModel readOntology(String path){

        //Model model = ModelFactory.createDefaultModel();
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        try {
            File file = new File(path);
            FileReader reader = new FileReader(file);
            model.read(reader,null);
            //model.write(System.out);

        } catch (Exception e) { e.printStackTrace(); }

        return model;
    }
}
