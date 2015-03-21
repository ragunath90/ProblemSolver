import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

class Timestamp {
	String time;
	HashMap<String,State> situation;
}
class State {
	String owner;
	HashMap<String,String> ownedEntities;
}

public class AdditionSolver {
	static StanfordCoreNLP pipeline;
	static HashMap<String,String> variables = new HashMap<String,String>();
	static ArrayList<Timestamp> story = new ArrayList<Timestamp>();
	static int timeStep = 0;
	static int varCount = 1;
	static int unknownCounter = 0;
	static HashMap<String,String> keywordMap = new HashMap<String,String>();
	static HashMap<String,String> procedureMap = new HashMap<String,String>();
	static LinkedHashSet<String> owners = new LinkedHashSet<String>();
	static LinkedHashSet<String> entities = new LinkedHashSet<String>();
	static String questionEntity = "";
	static String questionOwner = "";
	static int questionTime = -1;
	private static void loadKeywordLookup() {
		keywordMap.put("put","changeOut");
	}
	private static void loadProcedureLookup() {
		procedureMap.put("changeOut","[owner1]-[entity]. [owner2]+[entity]");
		procedureMap.put("comparePlus","[owner1]+[owner2]");
	}
	private static void updateTimestamp (String owner, Entity newEntity, String tense) {
		owners.add(owner);
		entities.add(newEntity.name);
		HashMap<String,State> currentSituation = new HashMap<String,State>();
		int changeTime = timeStep;
		if (tense.equals("past"))
			changeTime = 0;
		if (story.size() != timeStep) {
			currentSituation = story.get(changeTime).situation;
		}
		State currentState;
		if (currentSituation.containsKey(owner)) { 
			currentState = currentSituation.get(owner);
		}
		else {
			currentState = new State();
			currentState.owner = owner;
			currentState.ownedEntities = new HashMap<String,String>();
		}
		if (currentState.ownedEntities.containsKey(newEntity.name)) {
			String existingValue = currentState.ownedEntities.get(newEntity.name);
			if (existingValue.contains("x")) {
				variables.put(existingValue, newEntity.value);
				updateValues();
			}
		}
		currentState.ownedEntities.put(newEntity.name, newEntity.value);
		currentSituation.put(owner, currentState);
		Timestamp updatedTimestamp = new Timestamp();
		updatedTimestamp.time = "t"+changeTime;
		updatedTimestamp.situation = currentSituation;
		if (story.size() == changeTime)
			story.add(changeTime,updatedTimestamp);
		else {
			story.remove(changeTime);
			story.add(changeTime,updatedTimestamp);
		}
		displayStory();
	}
	private static void updateValues() {
		ArrayList<Timestamp> newStory = new ArrayList<Timestamp>();
		for (Timestamp t : story) {
			Iterator<Map.Entry<String,State>> it = t.situation.entrySet().iterator();
			while (it.hasNext()) {
			     Map.Entry<String,State> pairs = it.next();
			     State s = pairs.getValue();
			     String entityName= "", newValue = "";
			     Iterator<Map.Entry<String,String>> it1 = s.ownedEntities.entrySet().iterator();
				 while (it1.hasNext()) {
					Map.Entry<String,String> newPairs = it1.next();
					if (newPairs.getValue().contains("x")) {
						Pattern varPattern = Pattern.compile("x\\d+");
						Matcher varMatcher = varPattern.matcher(newPairs.getValue().toString());
						if (varMatcher.find()) {
							entityName = newPairs.getKey();
							newValue = newPairs.getValue().replaceFirst("x\\d+", variables.get(varMatcher.group()));	
						}
					}
				 }
				 if (!newValue.equals(""))
					 s.ownedEntities.put(entityName, newValue);
				 t.situation.put(pairs.getKey(), s);		 
			}
			newStory.add(t);
		}	
		story = newStory;
	}
	private static void displayStory() {
		System.out.println("----------------------------------------------------");
		for (Timestamp t : story) {
			System.out.println(t.time);
			Iterator<Map.Entry<String,State>> it = t.situation.entrySet().iterator();
			while (it.hasNext()) {
			     Map.Entry<String,State> pairs = it.next();
			     System.out.print(pairs.getKey()+" ");
			     State s = pairs.getValue();
			     Iterator<Map.Entry<String,String>> it1 = s.ownedEntities.entrySet().iterator();
				 while (it1.hasNext()) {
					Map.Entry<String,String> newPairs = it1.next();
					System.out.print(newPairs.getKey()+" "+newPairs.getValue());
				 }
				 System.out.println("");
			}	
		}
	}
	
	private static void reflectChanges(String owner1, String owner2, Entity newEntity, String keyword, String procedure, String tense) {
		if (owner1.equals("")) {
			owner1 = "unknown" + unknownCounter + 1;
			unknownCounter++;
		}
		if (owner2.equals("")) {
			owner2 = "unknown" + unknownCounter + 1;
			unknownCounter++;
		}
		if (keyword.equals("") && newEntity.name!=null) {
			if (entities.contains(owner1))
				updateTimestamp(owner2,newEntity,tense);
			else
				updateTimestamp(owner1,newEntity,tense);
			return;
		}
		else {
			String oldValue1="", oldValue2="";
			try {
				oldValue1 = story.get(timeStep).situation.get(owner1).ownedEntities.get(newEntity.name);
			} catch (NullPointerException ex) {
				addOwner(owner1,newEntity.name);
				oldValue1 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
			}
			try {
				oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
			} catch (NullPointerException ex) {
				addOwner(owner2,newEntity.name);
				oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
			}
			if (procedure.contains("change")) {
				timeStep++;
				tense = "";
			}
			String[] steps = procedureMap.get(procedure).split("\\.");
			System.out.println(procedure+"|"+procedureMap.get(procedure)+"|"+steps.length);
			for (int i=0; i<2; i++) {
				Entity modifiedEntity = new Entity();
				modifiedEntity.name = newEntity.name;
				steps[i] = steps[i].replace("[owner1]", oldValue1);
				steps[i] = steps[i].replace("[owner2]", oldValue2);
				steps[i] = steps[i].replace("[entity]", newEntity.value);
				modifiedEntity.value = steps[i];
				if (i == 0)
					updateTimestamp(owner1,modifiedEntity,tense);
				else
					updateTimestamp(owner2,modifiedEntity,tense);
			}
		}
	}
	private static void addOwner(String owner, String name) {
		String varName = "x"+varCount;
		for (int i=0; i<=timeStep; i++) {
			HashMap<String,State> iSituation = story.get(i).situation;
			State newState = new State();
			newState.owner = owner;
			newState.ownedEntities = new HashMap<String,String>();
			newState.ownedEntities.put(name, varName);
			iSituation.put(owner, newState);
			story.get(i).situation = iSituation;
		}
		variables.put(varName, null);
		varCount++;
		
	}
	private static void process (String input, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for (CoreMap sentence: sentences) {
	    	String tense = "";
    	    String keyword = "", procedure = "";
    	    boolean isQuestion = false;
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	//String word = token.get(TextAnnotation.class);
		    	String lemma = token.get(LemmaAnnotation.class);
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains("W")) {
		    		processQuestion (sentence);
		    		isQuestion = true;
		    		break;
		    	}
		    	if (pos.contains("VB") || pos.contains("JJ")) {
		    		if (pos.contains("VBD") || pos.contains("VBN"))
			    		tense = "past";
		    		else
		    			tense = "present";
		    		if (keywordMap.containsKey(lemma)) {
		    			keyword = lemma;
		    			procedure = keywordMap.get(keyword);
		    		}
		    			
		    	}
			}
	    	if (isQuestion)
	    		continue;
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	System.out.println(dependencies);
	    	String owner1 = "", owner2 = "";
	    	Entity newEntity = new Entity();
	    	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	    	for (SemanticGraphEdge edge : edges) {
	    		//3 apples
	    		if (edge.getRelation().toString().equals("num") || edge.getRelation().toString().equals("advcl")) {
	    			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
	    				continue;
	    			newEntity.name = edge.getSource().lemma();
	    			//have to check this compound entity name
	    			/*IndexedWord intermediateNode = edge.getTarget();
	    			for (SemanticGraphEdge innerEdge : edges) {
	    				if (innerEdge.getSource().equals(intermediateNode) && innerEdge.getRelation().toString().contains("mod")) {
	    					newEntity.name = newEntity.name+"_"+innerEdge.getTarget().originalText();
	    					break;
	    				}
	    			}*/
	    			newEntity.value = edge.getTarget().originalText();
	    			System.out.println("entity"+newEntity.name+"|"+newEntity.value);
	    		}
	    		if (edge.getTarget().toString().contains("NN")) {
	    			if (edge.getRelation().toString().equals("nsubj"))
	    				owner1 = edge.getTarget().lemma();
	    			else if (edge.getRelation().toString().contains("prep"))
	    				owner2 = edge.getTarget().lemma();
	    		}
	    	}
	    	System.out.println(owner1+"|"+owner2+"|"+newEntity.name+"|"+newEntity.value+"|"+keyword+"|"+procedure+"|"+tense);
    		reflectChanges(owner1,owner2,newEntity,keyword,procedure,tense);
	    }
	}
	
	private static void processQuestion(CoreMap sentence) {
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    	String word = token.get(TextAnnotation.class);
	    	String lemma = token.get(LemmaAnnotation.class);
	    	String pos = token.get(PartOfSpeechAnnotation.class);
	    	if (pos.contains("VB")) {
	    		if (pos.contains("VBD") || pos.contains("VBN")) {
		    		questionTime = 0;
	    		}
	    		else
	    			questionTime = timeStep;		
	    	}
	    	if (entities.contains(word)) 
	    		questionEntity = word;
	    	if (entities.contains(lemma)) 
	    		questionEntity = lemma;
	    	if (owners.contains(word)) 
	    		questionOwner = word;
	    	if (owners.contains(lemma)) 
	    		questionOwner = lemma;
		}
		
	}
	public static String solve (String input, StanfordCoreNLP pipeline) {
		process(input,pipeline);
		displayStory();
		ansQuestion();
		return "";
	}
	private static void ansQuestion() {
		String ans = story.get(questionTime).situation.get(questionOwner).ownedEntities.get(questionEntity);
		System.out.println(questionOwner + " has " + ExpressionCalculator.solve(ans) + " " + questionEntity );
		
	}
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    loadKeywordLookup();
	    loadProcedureLookup();
	    pipeline = new StanfordCoreNLP(props);
	    String input =  "Ruth had 3 apples. She put 2 apples into a basket. How many apples are there in the basket now, if in the beginning there were 4 apples in the basket?";
	    System.out.println(Parser.parse(input, pipeline));
	    solve(Parser.parse(input, pipeline),pipeline);
	}
}
