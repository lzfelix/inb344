package terrier;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.terrier.structures.Index;

import queryExpansion.StagedQueryExpansion;

/**
 * This class uses Terrier's functionalities to perform IR over a corpus.
 * @author Luiz Felix
 */
public class ModifiedTerrier {
	private static String TERRIER_HOME = "/Users/luiz/Desktop/SET_A/terrier";
	private static String INDEX_PATH = "/Users/luiz/Desktop/SET/terrier-4.0/processing/newIndex";
//	private static String INDEX_PATH = "/Users/luiz/Desktop/SET/terrier-4.0/corpus/clef";
	private static String CHV_PATH = "./CHV/CHV_modified.txt";
	private static String QUERIES_PATH = "tools/expanded_queries.txt";
//	private static String QUERIES_PATH = "tools/clef_queries.txt";
	private static String EXPANDED_QUERIES_PATH = "tools/clef_expanded_queries2.txt";
	private static String STD_INDEX_ALIAS = "data";
	private static String RESULTS_FILE_PATH = "tools/output.txt";
	
	private static boolean IS_CLEF = false;
	
	private Index index;
	private StagedQueryExpansion queryExpansion;
	
	// can be improved
	private LinkedHashMap<String, String> queries;
	
	/**
	 * Installs terrier.home variable on the environment and load index file. 
	 * @param terrierHome The terrier home path
	 * @param indexPath Path to the previously created index
	 * @throws Exception If the indexPath points to an invalid index.
	 */
	public ModifiedTerrier(String terrierHome, String indexPath) throws Exception {
		System.setProperty("terrier.home", terrierHome);
		index = Index.createIndex(indexPath, STD_INDEX_ALIAS);
		
		if (index == null)
			throw new Exception("Index is null, probably the path is invalid.");
		
		queryExpansion = new StagedQueryExpansion(index, CHV_PATH);
	}
	
	/**
	 * Reads all TREC-queries into memory. The queries are lower-cased, stop words are
	 * removed, only stemmed unique terms are kept. The query file must be in the form:
	 * <query_id> <query_text>\n
	 * @param queryPath The path to the query file one by line, with ID
	 * @throws IOException if either the file doesn't exist or there's an IO problem
	 */
	public void readQueries(String queryPath) throws IOException {
		BufferedReader queryBuffer = new BufferedReader(new FileReader(queryPath));
		String newLine;
		
		queries = new LinkedHashMap<String, String>();
		
		while ((newLine = queryBuffer.readLine()) != null) {
			//file ending line problem bypassing
			if (newLine.length() == 0) continue;
			
			int spaceIndex = newLine.indexOf(' ');
			
			//terrier stores qID as a String
			String qID = newLine.substring(0, spaceIndex);
			String query = newLine.substring(spaceIndex + 1);
			
			queries.put(qID, query);
		}
		
		queryBuffer.close();
	}
	
	/**
	 * Runs the read queries on the index using CHV-EMIM query expansion.
	 * @param outputFile Path to the output results file.
	 * @throws Exception If the CHV file isn't found of if there's a fault while reading the index.
	 */
	public void performQueriesWithStagedExpansion(String outputFile) throws Exception {
		DJM.getInstance().performQueries(outputFile, queries, index, this.queryExpansion, IS_CLEF);
	}
	
	/**
	 * Runs the read queries on the index without expanding them.
	 * @param outputFile Path to the output results file.
	 * @throws Exception If there's a fault while reading the index.
	 */
	public void performQueriesWithoutExpansion(String outputFile) throws Exception {
		DJM.getInstance().performQueries(outputFile, queries, index, null, IS_CLEF);
	}
	
	/**
	 * Expands the read queries using the staged expansion and creates a new file to be used with Terrier.
	 * @param outputFile The path to the new file to be created.
	 * @throws Exception If there's an I/O fault either while expanding the query or writing the new file to disk.
	 */
	public void writeExpandedQueries(String outputFile) throws Exception {
		DJM.getInstance().writeExpandedQueries(outputFile, queries, index, this.queryExpansion);
	}
	
	/**
	 * Automatically set DJM's mu parameter using the Tunner class.
	 * @param sampling The percentage of the corpus that is going to be taken in account while tunning.
	 * More is slower, but gives better precision.
	 * @throws IOException If there's an I/O fault while reading the index file.
	 */
	public void tuneMu(double sampling) throws IOException {
		Tunner t = new Tunner(index);
		double mu = t.tuneMu(1.0f, sampling);
		
		DJM.getInstance().setMu(mu);
	}
	
	/**
	 * Returns the current value of mu used by DJM in order to score the documents.
	 * @return Returns the current value of mu used by DJM in order to score the documents.
	 */
	public double getInternalMu() {
		return DJM.getInstance().getMu();
	}
	
	public static void main(String args[]) {
		ModifiedTerrier terrier = null;
		
		try {
			terrier = new ModifiedTerrier(TERRIER_HOME, INDEX_PATH);
		} catch (Exception e) {
			System.out.println("Exception caught: " + e.getMessage());
			e.printStackTrace();
		} 
		
		try {
			
//			Tunner t = new Tunner(terrier.index);
//			t.tuneMu(1, 1);
			
			terrier.readQueries(QUERIES_PATH);
//			terrier.writeExpandedQueries(EXPANDED_QUERIES_PATH);
//			terrier.performQueriesWithStagedExpansion("tools/wololo.txt");
			terrier.performQueriesWithoutExpansion(RESULTS_FILE_PATH);
			
//			terrier.performQueriesWithStagedExpansion(RESULTS_FILE_PATH);
		}
		catch (Exception e) {
			System.out.println("Error while reading the queries: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
