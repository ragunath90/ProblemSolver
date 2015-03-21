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
	HashMap<String, State> situation;
}

class State {
	String owner;
	HashMap<String, String> ownedEntities;
}

public class AdditionSolver {
	
	private static final String CHANGE_OUT = "changeOut";
	private static final String X_VALUE = "x";

	private static final String UNKNOWN = "unknown";
	private static final String CHANGE = "change";
	private static final String OWNER_1 = "[owner1]";
	private static final String OWNER_2 = "[owner2]";
	private static final String ENTITY = "[entity]";
	private static final String TIMESTAMP_PREFIX = "t";

	private static final int NO_OWNERS_SUPPORTED = 2;

	// give this a better meaningful name
	private static final String SPLIT_PATTERN = "x\\d+";
	
	// NLP Parser constants
	private static final String NLP_WORD = "W";
	private static final String NLP_VERB = "VB";
	private static final String NLP_VBD = "VBD";
	private static final String NLP_JJ = "JJ";
	private static final String NLP_NOUN = "VBN";

	private static final String PRESENT = "present";
	private static final String PAST = "past";

	// having everything as static is very very bad. 
	// totally destroys the understandability of code
	// functions should pass and get responses. 
	// this could be HashMap or a class when you want to return multiple values from a function
	static int timeStep = 0;
	static int varCount = 1;
	static int unknownCounter = 0;
	static int questionTime = -1;

	static String questionEntity = "";
	static String questionOwner = "";

	static ArrayList<Timestamp> story = new ArrayList<Timestamp>();
	static HashMap<String,String> variables = new HashMap<String,String>();
	static HashMap<String,String> keywordMap = new HashMap<String,String>();
	static HashMap<String,String> procedureMap = new HashMap<String,String>();
	static LinkedHashSet<String> owners = new LinkedHashSet<String>();
	static LinkedHashSet<String> entities = new LinkedHashSet<String>();
	
	static StanfordCoreNLP pipeline;

	private static void loadKeywordLookup() {
		keywordMap.put("put", CHANGE_OUT);
	}

	private static void loadProcedureLookup() {
		// change every re usable string to constants. 
		procedureMap.put(CHANGE_OUT, "[owner1]-[entity]. [owner2]+[entity]");
		procedureMap.put("comparePlus", "[owner1]+[owner2]");
	}

	private static void updateTimestamp (String owner, Entity newEntity, String tense) {
		owners.add(owner);
		entities.add(newEntity.name);
		HashMap<String,State> currentSituation = new HashMap<String,State>();
		int changeTime = timeStep;
		if (tense.equals(PAST))
			changeTime = 0;
		if (story.size() != timeStep) {
			currentSituation = story.get(changeTime).situation;
		}
		State currentState;
		if (currentSituation.containsKey(owner)) { 
			currentState = currentSituation.get(owner);
		} else {
			currentState = new State();
			currentState.owner = owner;
			currentState.ownedEntities = new HashMap<String,String>();
		}
		if (currentState.ownedEntities.containsKey(newEntity.name)) {
			String existingValue = currentState.ownedEntities.get(newEntity.name);
			if (existingValue.contains(X_VALUE)) {
				variables.put(existingValue, newEntity.value);
				updateValues();
			}
		}
		currentState.ownedEntities.put(newEntity.name, newEntity.value);
		currentSituation.put(owner, currentState);
		Timestamp updatedTimestamp = new Timestamp();
		updatedTimestamp.time = TIMESTAMP_PREFIX + changeTime;
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
					if (newPairs.getValue().contains(X_VALUE)) {
						Pattern varPattern = Pattern.compile(SPLIT_PATTERN);
						Matcher varMatcher = varPattern.matcher(newPairs.getValue().toString());
						if (varMatcher.find()) {
							entityName = newPairs.getKey();
							newValue = newPairs.getValue().replaceFirst(SPLIT_PATTERN, variables.get(varMatcher.group()));	
						}
					}
				 }
				 // use StringUtils.isEmpty. Not sure of the syntax.
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
			Iterator<Map.Entry<String, State>> it = t.situation.entrySet().iterator();
			while (it.hasNext()) {
			     Map.Entry<String, State> pairs = it.next();
			     System.out.print(pairs.getKey() + " ");
			     State s = pairs.getValue();
			     Iterator<Map.Entry<String, String>> it1 = s.ownedEntities.entrySet().iterator();
				 while (it1.hasNext()) {
					Map.Entry<String, String> newPairs = it1.next();
					System.out.print(newPairs.getKey() + " " + newPairs.getValue());
				 }
				 System.out.println("");
			}	
		}
	}

	private static void reflectChanges(String owner1, String owner2, Entity newEntity,
									   String keyword, String procedure, String tense) {
		// Use StringUtils.isEmpty. Do this everywhere you check for null/empty strings
		if (owner1.equals("")) {
			owner1 = UNKNOWN + unknownCounter + 1;
			unknownCounter++;
		}
		if (owner2.equals("")) {
			owner2 = UNKNOWN + unknownCounter + 1;
			unknownCounter++;
		}
		// Write comment here as to why you are exiting with this
		// comments should be written when the code doesn't look obvious
		if (keyword.equals("") && newEntity.name != null) {
			if (entities.contains(owner1))
				updateTimestamp(owner2, newEntity, tense);
			else
				updateTimestamp(owner1, newEntity, tense);
			// when you use return, else is not necessary. 
			// This is called early exit
			return;
		} 
		String oldValue1 = "", oldValue2 = "";
		try {
			oldValue1 = story.get(timeStep).situation.get(owner1).ownedEntities.get(newEntity.name);
		} catch (NullPointerException ex) {
			addOwner(owner1, newEntity.name);
			oldValue1 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
		}
		try {
			oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
		} catch (NullPointerException ex) {
			addOwner(owner2, newEntity.name);
			oldValue2 = story.get(timeStep).situation.get(owner2).ownedEntities.get(newEntity.name);
		}
		if (procedure.contains(CHANGE)) {
			timeStep++;
			tense = "";
		}
		String[] steps = procedureMap.get(procedure).split("\\.");
		System.out.println(procedure + "|" + procedureMap.get(procedure) + "|" + steps.length);
		// can't NO_OWNERS_SUPPORTED by replaced with steps.length? 
		// what if steps only has one value. below code will through IndexOutOfBoundsExcetion.
		// if you expect the length to be 2, you should assert it
		// like assert(steps.length == NO_OWNERS_SUPPORTED)
		for (int i = 0; i < NO_OWNERS_SUPPORTED; i++) {
			Entity modifiedEntity = new Entity();
			modifiedEntity.name = newEntity.name;
			String step = steps[i];
			step = step.replace(OWNER_1, oldValue1);
			step = step.replace(OWNER_2, oldValue2);
			step = step.replace(ENTITY, newEntity.value);
			modifiedEntity.value = step;
			String owner;
			if (i == 0) {
				owner = owner1;
			} else {
				owner = owner2;
			}
			updateTimestamp(owner, modifiedEntity, tense);
		}
	}

	private static void addOwner(String owner, String name) {
		String varName = X_VALUE + varCount;
		for (int i = 0; i <= timeStep; i++) {
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

	private static void process(String input, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for (CoreMap sentence: sentences) {
	    	String tense = "";
    	    String keyword = "", procedure = "";
    	    boolean isQuestion = false;
    	    // too many nexted for loops. 
    	    // break into functions if possible and return necessary values.
    	    // in this case, you could probably send a HashMap back with keys
    	    // 1. isQuesiton, 2. procedure, 3. tense
    	    // code should be as descriptive as possible
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		    	// String word = token.get(TextAnnotation.class);
		    	String lemma = token.get(LemmaAnnotation.class);
		    	String pos = token.get(PartOfSpeechAnnotation.class);
		    	if (pos.contains(NLP_WORD)) {
		    		isQuestion = true;
		    		break;
		    	}
		    	if (pos.contains(NLP_VERB) || pos.contains(NLP_JJ)) {
		    		if (pos.contains(NLP_VBD) || pos.contains(NLP_NOUN))
			    		tense = PAST;
		    		else
		    			tense = PRESENT;
		    		if (keywordMap.containsKey(lemma)) {
		    			keyword = lemma;
		    			procedure = keywordMap.get(keyword);
		    		}
		    			
		    	}
			}
	    	if (isQuestion) {
	    		processQuestion(sentence);
	    		continue;
	    	}
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	System.out.println(dependencies);
	    	String owner1 = "", owner2 = "";
	    	Entity newEntity = new Entity();
	    	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	    	// break the below into a function
	    	for (SemanticGraphEdge edge : edges) {
	    		// change all string literals to constants with meaningful names
	    		// 3 apples
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
	    			System.out.println(ENTITY + newEntity.name + "|" + newEntity.value);
	    		}
	    		if (edge.getTarget().toString().contains("NN")) {
	    			if (edge.getRelation().toString().equals("nsubj"))
	    				owner1 = edge.getTarget().lemma();
	    			else if (edge.getRelation().toString().contains("prep"))
	    				owner2 = edge.getTarget().lemma();
	    		}
	    	}
	    	System.out.println(owner1 + "|" + owner2 + "|" + newEntity.name + 
	    		"|" + newEntity.value + "|" + keyword + "|" + procedure + "|" + tense);
    		reflectChanges(owner1, owner2, newEntity, keyword, procedure, tense);
	    }
	}
	
	private static void processQuestion(CoreMap sentence) {
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    	String word = token.get(TextAnnotation.class);
	    	String lemma = token.get(LemmaAnnotation.class);
	    	String pos = token.get(PartOfSpeechAnnotation.class);
	    	if (pos.contains(NLP_VERB)) {
	    		if (pos.contains(NLP_VBD) || pos.contains(NLP_NOUN)) {
		    		questionTime = 0;
	    		} else {
	    			questionTime = timeStep;		
	    		}
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
	public static String solve(String input, StanfordCoreNLP pipeline) {
		process(input, pipeline);
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
