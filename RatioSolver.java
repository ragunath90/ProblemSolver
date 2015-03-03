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
	static ArrayList<Equation> allEquations = new ArrayList<Equation>();
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
			ratioEquation = theRatio.value1 + " / " + theRatio.value2 + " = ";
		if (ratioEntities.get(0).value == null) {
				ratioEquation = ratioEquation + ratioEntities.get(0).name + " / ";
				unknowns++;
		}
		else
			ratioEquation = ratioEquation + ratioEntities.get(0).value + " / ";
		if (ratioEntities.get(1).value == null) {
			ratioEquation = ratioEquation + ratioEntities.get(1).name;
			unknowns++;
		}
		else
			ratioEquation = ratioEquation + ratioEntities.get(1).value;
		System.out.println(ratioEquation);
		allEquations.add(EquationSimplifier.simplify(ratioEquation));
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
			allEquations.add(EquationSimplifier.simplify(secondaryEquation));
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
	    			IndexedWord intermediateNode = edge.getTarget();
	    			for (SemanticGraphEdge innerEdge : edges) {
	    				if (innerEdge.getSource().equals(intermediateNode) && innerEdge.getRelation().toString().contains("mod")) {
	    					newEntity.name = newEntity.name+"_"+innerEdge.getTarget().originalText();
	    					break;
	    				}
	    			}
	    			boolean existingFlag = false;
	    			for (Entity entity : allEntities) {
	    				if (entity.name.equals(newEntity.name)) {
	    					existingFlag=true;
	    					entity.value = edge.getTarget().originalText();
	    					System.out.println(entity.name+"|"+entity.value);
	    					allEntities.add(allEntities.indexOf(entity), entity);
	    					break;
	    				}
	    			}
	    			if (!existingFlag) {
	    				newEntity.value = edge.getTarget().originalText();
	    				System.out.println(newEntity.name+"|"+newEntity.value);
	    				allEntities.add(newEntity);
	    			}
	    		}
	    	}
	    	String simpleSentence = sentence.toString();
	    	if (simpleSentence.contains("ratio") && theRatio.value1 == null) {
	    		String[] words = simpleSentence.split("[\\s\\.]");
	    		boolean ratioWordFlag = false;
	    		for (String word : words) {
	    			if (word.equals("ratio")) 
	    				ratioWordFlag = true;
	    			if (ratioWordFlag && word.matches("\\d+") && theRatio.value1 == null)
	    				theRatio.value1 = word;
	    			else if (ratioWordFlag && word.matches("\\d+") && theRatio.value2 == null)
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
		System.out.println("------------------------------------------------------------");
		if (allEquations.size() == 1) {
			System.out.println("Ratio = "+theRatio.value1 + ":" +theRatio.value2);
			for (int i=0; i<ratioEntities.size(); i++) {
				System.out.print(ratioEntities.get(i).name +" = ");
				if (ratioEntities.get(i).value != null)
					System.out.println(ratioEntities.get(i).value);
				else
					System.out.println(EquationSimplifier.answer);	
			}
		}
		else {
			ArrayList<Double> answers = EquationSolver.solve(allEquations);
			System.out.println("Ratio = "+theRatio.value1 + ":" +theRatio.value2);
			for (int i=0; i<ratioEntities.size(); i++) {
				System.out.print(ratioEntities.get(i).name +" = ");
				System.out.println(Math.round(answers.get(i)));
			}
		}
		EquationSimplifier.clearMap();
		return "";
	}
	
	public static void main(String[] args) {
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner,parse,dcoref");
	    pipeline = new StanfordCoreNLP(props);
	    String input =  "The ratio of boys to girls is 9 to 4. If there are 26 students, how many girls are there?";
	    solve(input,pipeline);
	}
}
