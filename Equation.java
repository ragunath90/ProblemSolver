import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Equation {
	private int noVars;
	private Double[] coeff;
	public Equation() {
		this.noVars = 0;
		this.coeff = null;
	}
	public Equation(int noVars, Double[] coeff) {
		this.noVars = noVars;
		this.coeff = coeff;
	}
	public int getNoVars() {
		return noVars;
	}
	public Double[] getCoeff() {
		return coeff;
	}
	public static Equation parse(String s) {
		Equation sEqn = null;
		int count = 0;
		String[] eqnComponents = s.split(" ");
		ArrayList<Double> coefficients = new ArrayList<Double>();
		boolean signFlag = false;
		for (String component : eqnComponents) {
			if (component.equals("="))
				break;
			if (component.equals("+") || component.equals("-")) {
				count++;
				if (component.equals("-"))
					signFlag = true;
				continue;
			}
			Pattern pattern = Pattern.compile("^\\d+(?:\\.\\d*)?");
		    Matcher matcher = pattern.matcher(component);
		    if(!matcher.find()) {
		    	if (signFlag)
		    		coefficients.add(new Double(-1));
		    	else
		    		coefficients.add(new Double(1));
		    	signFlag = false;
		    	continue;
		    }
		    Double singleCoeff =  Double.parseDouble(matcher.group());
		    if (signFlag) {
		    	singleCoeff = -singleCoeff;
		    	signFlag = false;
		    }
		    coefficients.add(singleCoeff);
		}
		sEqn = new Equation(count,coefficients.toArray(new Double[coefficients.size()]));
		System.out.println(count+"|"+coefficients);
		return sEqn;
	}
}
