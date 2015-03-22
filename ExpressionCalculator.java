import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;


public class ExpressionCalculator {

	private static String appid = "XKXW7Q-RUTH97KAHA";
	
	public static String solve(String input) {
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
				for (WAPod pod : queryResult.getPods()) {
					if (!pod.isError() && pod.getTitle().contains("Result")) {
						for (WASubpod subpod : pod.getSubpods()) {
							for (Object element : subpod.getContents()) {
								if (element instanceof WAPlainText) {
									return ((WAPlainText) element).getText();
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			
		}
		return "";
	}
}