import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.factory.LinearSolverFactory;


public class EquationSolver {
	public static ArrayList<Double> solve(ArrayList<Equation> systemEquations) {
		ArrayList<Double> answers = new ArrayList<Double>();
		double[][] a,b;
		int row=0;
		Equation pivot = systemEquations.get(0);
		a = new double[pivot.getCoeff().length-1][pivot.getCoeff().length-1];
		b = new double[pivot.getCoeff().length-1][1];
		for (Equation e : systemEquations) {
			Double[] coeff = e.getCoeff();
			int n = coeff.length;
			System.out.print("\n");
			for (int i = 0; i < n-1; i++) {
				a[row][i] = coeff[i];
				System.out.print(coeff[i]+" ");
			}
			b[row][0] = -coeff[n-1];
			row++;
		}
		DenseMatrix64F A = new DenseMatrix64F(a);
		DenseMatrix64F B = new DenseMatrix64F(b);
		DenseMatrix64F X = new DenseMatrix64F(new double[pivot.getCoeff().length - 1][1]);
		
		LinearSolver<DenseMatrix64F> solver;
		solver = LinearSolverFactory.linear(pivot.getCoeff().length-1);
		if( !solver.setA(A) )
            throw new RuntimeException("Solver failed");
		solver.solve(B, X);
		System.out.println(X);
		for (int i=0; i < X.numRows; i++) {
			answers.add(X.get(i, 0));
		}
		System.out.println(A);
		System.out.println(B);
		return answers;
	}
}

