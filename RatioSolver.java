import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.WuPalmer;


class Entity {
	String name;
	String value;
}
class Ratio {
	String value1;
	String value2;
}

public class RatioSolver {

	static Ratio theRatio = new Ratio();
	private static ILexicalDatabase db = new NictWordNet();      
	static StanfordCoreNLP pipeline;
	static ArrayList<Entity> allEntities  = new ArrayList<Entity>();
	static ArrayList<Entity> ratioEntities  = new ArrayList<Entity>();
	private static void getFinalEntities(ArrayList<Entity> candidateEntities) {
		int len = candidateEntities.size();
		double max = -2;
		Entity[] finalEntities = new Entity[2];
		finalEntities[0] = new Entity();
		finalEntities[1] = new Entity();
		finalEntities[0].name = "entity1";
		finalEntities[1].name = "entity2";
		for (int i = 0 ; i < len; i ++){
			  for(int j = i+1 ; j < len; j ++){
				  String possibility1 = candidateEntities.get(i).name;
				  String possibility2 = candidateEntities.get(j).name;
				  if(!possibility1.equals(possibility2)) {
					  double score = (new WuPalmer(db)).calcRelatednessOfWords(possibility1, possibility2);
					  System.out.println(possibility1 + "," + possibility2 + "," + score);
					  if (score > max) {
						  finalEntities[0] = candidateEntities.get(i);
						  finalEntities[1] = candidateEntities.get(j);
						  max = score;
					  }
					  
				  }
			  }
		}
		System.out.println("final" + finalEntities[0].name + "," + finalEntities[1].name);
		allEntities.add(finalEntities[0]);
		allEntities.add(finalEntities[1]);
		ratioEntities.add(finalEntities[0]);
		ratioEntities.add(finalEntities[1]);
	}
	private static void developEquations() {
		String ratioEquation;
		int unknowns = 0;
		if (theRatio.value1 == null) { 
			ratioEquation = "ratio = ";
			unknowns++;
		}
		else
			ratioEquation = theRatio.value1 + "/" + theRatio.value2 + " = ";
		if (ratioEntities.get(0).value == null) {
				ratioEquation = ratioEquation + ratioEntities.get(0).name + "/";
				unknowns++;
		}
		else
			ratioEquation = ratioEquation + ratioEntities.get(0).value + "/";
		if (ratioEntities.get(1).value == null) {
			ratioEquation = ratioEquation + ratioEntities.get(1).name;
			unknowns++;
		}
		else
			ratioEquation = ratioEquation + ratioEntities.get(1).value;
		System.out.println(ratioEquation);
		String secondaryEquation = "";
		if (unknowns >= 2) {
			for (Entity entity : allEntities) {
				if (!ratioEntities.contains(entity))
					secondaryEquation = entity.value + " = "; 
			}
			if (ratioEntities.get(0).value == null) 
				secondaryEquation = secondaryEquation + ratioEntities.get(0).name + " + ";
			else
				secondaryEquation = secondaryEquation + ratioEntities.get(0).value + " + ";
			if (ratioEntities.get(1).value == null) 
				secondaryEquation = secondaryEquation + ratioEntities.get(1).name;
			else
				secondaryEquation = secondaryEquation + ratioEntities.get(1).value;
			System.out.println(secondaryEquation);
		}
		
	}
	private static void process (String input, StanfordCoreNLP pipeline) {
		Annotation document = new Annotation(input);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	System.out.println(dependencies);
	    	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	    	for (SemanticGraphEdge edge : edges) {
	    		if (edge.getRelation().toString().equals("num") || edge.getRelation().toString().equals("advcl")) {
	    			Entity newEntity = new Entity();
	    			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
	    				continue;
	    			newEntity.name = edge.getSource().lemma();
	    			if (newEntity.name.equals("ratio") || newEntity.name.equals("scale")) {
	    				theRatio.value2 = edge.getTarget().originalText();
	    				IndexedWord intermediateNode = edge.getTarget();
	    				boolean foundFlag = false;
	    				for (SemanticGraphEdge innerEdge : edges) {
	    		    		if (innerEdge.getSource().equals(intermediateNode) && innerEdge.getRelation().toString().equals("number")) {
	    		    			theRatio.value1 = innerEdge.getTarget().originalText();
	    		    			foundFlag = true;
	    		    			break;
	    		    		}
	    				}
	    		    	if (!foundFlag) {
	    		    		theRatio.value1 = "1";
	    		    	}
	    			}
	    			else {
	    				newEntity.value = edge.getTarget().originalText();
	    				System.out.println(newEntity.name+"|"+newEntity.value);
	    				allEntities.add(newEntity);
	    			}
	    		}
	    	}
	    	String simpleSentence = sentence.toString();
	    	if (simpleSentence.contains("ratio") && theRatio.value1 == null) {
	    		String[] words = simpleSentence.split("[\\s\\.]");
	    		for (String word : words) {
	    			if (word.matches("\\d+") && theRatio.value1 == null)
	    				theRatio.value1 = word;
	    			else if (word.matches("\\d+"))
	    				theRatio.value2 = word;
	    		}
	    	}
	    	if (simpleSentence.contains("ratio")) {
	    		int count = 0;
	    		for (Entity entity : allEntities) {
	    			if (simpleSentence.contains(entity.name))
	    				count++;
	    		}
	    		if (count!=2) {
	    			ArrayList<Entity> candidateEntities = new ArrayList<Entity>();
	    			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		    	String word = token.get(TextAnnotation.class);
	    		    	String lemma = token.get(LemmaAnnotation.class);
	    		    	String pos = token.get(PartOfSpeechAnnotation.class);
	    		    	if (pos.contains("NN") && !word.equals("ratio")) {
	    		    		System.out.println(word+"|"+pos+"|"+lemma);
	    		    		Entity newEntity = new Entity();
	    		    		newEntity.name = lemma;
	    		    		if (word.equals("aerobics"))
	    		    			newEntity.name = word;
	    		    		candidateEntities.add(newEntity);
	    		    	}
	    			}
	    			getFinalEntities(candidateEntities);
	    		}
	    	}
	    }
	}
	
	public static String solve (String input, StanfordCoreNLP pipeline) {
		process(input,pipeline);
		developEquations();
		return "";
	}
	
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    pipeline = new StanfordCoreNLP(props);
	    String input = "Two numbers are in the ratio 8 to 3. If the sum of numbers is 143 , find the numbers.";
	    solve(input,pipeline);
	}
}
