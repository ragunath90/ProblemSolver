import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;


public class EquationSimplifier {
	private static String appid = "XKXW7Q-RUTH97KAHA";
	private static HashMap<String,String> varStdMap = new HashMap<String,String>();
	public static String answer = "";
	public static void clearMap() {
		varStdMap = new HashMap<String,String>();
	}
	public static HashMap<String,String> getMap() {
		return varStdMap;
	}
	public static Equation simpleSimplifier(String s) {
		Equation sEqn = null;
		String[] eqnComponents = s.split(" ");
		ArrayList<Double> coefficients = new ArrayList<Double>();
		TreeMap <String,Double> varCoeffMap = new TreeMap<String, Double>();
		varCoeffMap.put("const", 0.0);
		boolean signFlag = false, flipFlag = false;
		for (String component : eqnComponents) {
			if (component.equals("=")) {
				flipFlag = true;
				continue;
			}
			if (component.equals("-")) {
					signFlag = true;
					continue;
			}
			if (component.equals("+") || component.equals(""))
				continue;
			Pattern coeffPattern = Pattern.compile("^\\d+(?:\\.\\d+)?");
			Pattern varPattern = Pattern.compile("[a-zA-Z]+");
		    Matcher matcher = coeffPattern.matcher(component);
		    Double singleCoeff = null;
		    if(!matcher.find()) {
		    	System.out.println("var"+component+"|");
		    	if (signFlag && !flipFlag || !signFlag && flipFlag)
		    		singleCoeff = -1.0; 
		    	else
		    		singleCoeff = 1.0;
		    	signFlag = false;
		    }
		    else {
		    	singleCoeff =  Double.parseDouble(matcher.group());
		    	System.out.println("coeff"+singleCoeff);
		    	
		    	if (signFlag && !flipFlag || !signFlag && flipFlag) {
		    		singleCoeff = -singleCoeff;
		    		signFlag = false;
		    	}
		    }
		    matcher = varPattern.matcher(component);
	    	if(!matcher.find()) {
	    		double constant = varCoeffMap.get("const"); 
	    		constant += singleCoeff;
	    		varCoeffMap.put("const", constant);
	    	}
	    	else {
	    		String var = matcher.group();
	    		double curCoeff = 0;
	    		if (varCoeffMap.containsKey(var))
	    			curCoeff += varCoeffMap.get(var);
	    		curCoeff += singleCoeff;
	    		varCoeffMap.put(var, curCoeff);
	    	}
		}
		//http://stackoverflow.com/questions/1066589/java-iterate-through-hashmap
		Iterator<Map.Entry<String,Double>> it = varCoeffMap.entrySet().iterator();
		while (it.hasNext()) {
		     Map.Entry<String,Double> pairs = it.next();
		     if (!pairs.getKey().equals("const"))
		    	 coefficients.add(pairs.getValue());
		}
		coefficients.add(varCoeffMap.get("const"));
		System.out.println("simp" + coefficients);
		sEqn = new Equation(coefficients.size()-1,coefficients.toArray(new Double[coefficients.size()]));
		return sEqn;
	}
	public static String cleanup(String s) {
		String copy = "";
		for (int i=0; i<s.length(); i++) {
			if(s.charAt(i)==' ') {
				char prev = s.charAt(i-1);
				String prevS = prev + "";
				if (prev == '+' || prev == '-' || prev == '=' || prevS.matches("[a-zA-Z]")) {
					copy = copy + " ";
					continue;
				}
				if (prevS.matches("\\d")) {
					if (i == s.length()-1) //unnecessary space at end
						continue;
					String next = s.charAt(i+1) + "";
					if (next.equals("=")) {
						copy = copy + " ";
						continue;
					}
				}
				continue;
			}
			if (s.charAt(i)=='+' || s.charAt(i)=='-' || s.charAt(i)=='=') {
				String next = s.charAt(i+1) + "";
				String prev = s.charAt(i-1) + "";
				if (!prev.equals(""))
					copy = copy + " ";
				copy = copy + s.charAt(i);
				if (!next.equals(""))
					copy = copy + " ";
				continue;
			}
			copy = copy + s.charAt(i);
		}
		return copy;
	}
	public static String standardize(String eqn) {
	
		String[] eqnComponents = eqn.split(" ");
		String copy = "";
		char curVar = 'x';
		for (String component: eqnComponents) {
			Pattern varPattern = Pattern.compile("[a-zA-Z_]+");
		    Matcher matcher = varPattern.matcher(component);
		    if (matcher.find()) {
		    	if (varStdMap.containsKey(matcher.group()))
		    		copy = copy + component.replace(matcher.group(), varStdMap.get(matcher.group()));
		    	else {
		    		String value = curVar + "";
		    		curVar = (char)((int)curVar + 1);
		    		varStdMap.put(matcher.group(), value);
		    		copy = copy + component.replace(matcher.group(), value);
		    	}
		    	copy = copy + " ";
		    	continue;
		    }
		    copy = copy + component + " ";
		}
		System.out.println("in"+eqn+"|"+copy);
		return copy;
	}
	public static Equation simplify(String s) {
		String input = standardize(cleanup(s));
		WAEngine engine = new WAEngine();
		engine.setAppID(appid);
		engine.addFormat("plaintext");
		WAQuery query = engine.createQuery();
		query.setInput(input);
		try {
			WAQueryResult queryResult = engine.performQuery(query);
			if (queryResult.isError()) {
				System.out.println("Query error");
				System.out.println(" error code: " + queryResult.getErrorCode());
				System.out.println(" error message: " + queryResult.getErrorMessage());
			} else if (!queryResult.isSuccess()) {
				System.out.println("Query was not understood; no results available.");
			} else {
				System.out.println("Successful query. Pods follow:\n");
				boolean checkFlag = false;
				for (WAPod pod : queryResult.getPods()) {
					if (!pod.isError() && pod.getTitle().contains("Solution")) {
						for (WASubpod subpod : pod.getSubpods()) {
							for (Object element : subpod.getContents()) {
								if (element instanceof WAPlainText) {
									String ans = ((WAPlainText) element).getText();
									if (ans.contains("/"))
										continue;
									checkFlag = true;
									System.out.println(ans);
									String[] components = ans.split(" ");
									for (String component : components) {
										Pattern numPattern = Pattern.compile("^\\d+(?:\\.\\d+)?");
										Matcher matcher = numPattern.matcher(component);
										if (matcher.find())
											answer = matcher.group();
									}
									return null;
								}
							}	
						}
					}
					else if (!pod.isError() && pod.getTitle().contains("Alternate form")) {
						for (WASubpod subpod : pod.getSubpods()) {
							for (Object element : subpod.getContents()) {
								if (element instanceof WAPlainText) {
									String ans = ((WAPlainText) element).getText();
									if (ans.contains("/"))
										continue;
									checkFlag = true;
									System.out.println(ans);
									System.out.println(cleanup(ans));
									return simpleSimplifier(cleanup(ans)); 
								}
							}	
						}
					}
					
				}
				if (!checkFlag) {
					System.out.println(input);
					System.out.println(cleanup(input));
					return simpleSimplifier(cleanup(input));
				}
			}
		}
		catch (WAException e) {
			e.printStackTrace();
		}
		return null;
	}

}
