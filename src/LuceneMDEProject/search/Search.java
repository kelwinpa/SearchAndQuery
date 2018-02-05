package LuceneMDEProject.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import LuceneMDEProject.LuceneServiceImp;

public class Search {

	public Search() {
		super();
	}

	public void searchIndex(List<String> indexPathList) throws IOException, ParseException {

		BufferedReader readerLine = new BufferedReader(new InputStreamReader(System.in));
		while (true) {

			String queryString = null;
			String indexPath = null;
			int hitsPerPage = 10;
			boolean raw = false;
			System.out.println("Query formart [forgeType]:[value] AND [Root].[Node]:[value]");
			System.out.println("Enter query or (q) to exit: ");
			queryString = readerLine.readLine();

			if (queryString == null || queryString.length() == -1) {
				System.out.println("\nValue not readible");
				continue;
			}

			queryString = queryString.trim();
			if (queryString.length() == 0) {
				System.out.println("\nSpace is not acceptable");
				continue;
			}

			if (!queryString.equals("q")) {

				String[] subQueries = null;
				subQueries = queryString.split("AND");
				if (subQueries.length == 1) {
					subQueries = queryString.split("and");
				}
				// Only Queries with lenght 2
				if (subQueries.length == 2) {

					String[] firstSubQuery = subQueries[0].split(":");

					if (firstSubQuery[0].trim().equals(LuceneServiceImp.TYPE_TAG)) {
						if (firstSubQuery[1].trim().equals(LuceneServiceImp.METAMODEL_TYPE)) {
							indexPath = indexPathList.get(0);
						} else if (firstSubQuery[1].trim().equals(LuceneServiceImp.MODEL_TYPE)) {
							indexPath = indexPathList.get(1);
						} else if (firstSubQuery[1].trim().equals(LuceneServiceImp.TRANSFORMATIO_TYPE)) {
							indexPath = indexPathList.get(2);
						}
					}

					subQueries[1] = subQueries[1].trim();
					String[] secondSubQuery = subQueries[1].split(":");
					String value = secondSubQuery[1].trim();
					String[] partSecondSubQuery = secondSubQuery[0].split("[.]");
					String root = partSecondSubQuery[0].trim();
					String node = partSecondSubQuery[1].trim();

					String queryContent = node + "=\"" + value + "\"";

					IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
					IndexSearcher searcher = new IndexSearcher(reader);
					Analyzer analyzer = new StandardAnalyzer();

					// MultiFieldQueryParser queryParser = new
					// MultiFieldQueryParser(node, analyzer);
					QueryParser parser = new QueryParser(node, analyzer);
					Query query = parser.parse(value);
					System.out.println("Searching for: [" + root + "] with [" + queryContent + "]");
					// System.out.println("Searching for: " +
					// query.toString("contents"));

					searcher.search(query, 100);

					doPagingSearch(readerLine, searcher, query, hitsPerPage, raw,
							indexPath == null && queryString == null);

				}

			} else {
				break;
			}
		}
		readerLine.close();
	}

	public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, int hitsPerPage,
			boolean raw, boolean interactive) throws IOException {

		// Collect enough docs to show 5 pages
		TopDocs results = searcher.search(query, Integer.MAX_VALUE);
		ScoreDoc[] hits = results.scoreDocs;

		int numTotalHits = (int) results.totalHits;
		System.out.println(numTotalHits + " total matching documents");

		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);

		while (true) {
			if (end > hits.length) {
				System.out.println("Only results 1 - " + hits.length + " of " + numTotalHits
						+ " total matching documents collected.");
				System.out.println("Collect more (y/n) ?");
				String line = in.readLine();
				if (line.length() == 0 || line.charAt(0) == 'n') {
					break;
				}

				hits = searcher.search(query, numTotalHits).scoreDocs;
			}

			end = Math.min(hits.length, start + hitsPerPage);

			for (int i = start; i < end; i++) {
				if (raw) { // output raw format
					System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
					continue;
				}

				Document doc = searcher.doc(hits[i].doc);
				String path = doc.get("path");
				if (path != null) {
					System.out.println((i + 1) + ". " + path);
					String title = doc.get("title");
					if (title != null) {
						System.out.println("   Title: " + doc.get("title"));
					}
				} else {
					System.out.println((i + 1) + ". " + "No path for this document");
				}

			}

			if (!interactive || end == 0) {
				break;
			}

			if (numTotalHits >= end) {
				boolean quit = false;
				while (true) {
					System.out.print("Press ");
					if (start - hitsPerPage >= 0) {
						System.out.print("(p)revious page, ");
					}
					if (start + hitsPerPage < numTotalHits) {
						System.out.print("(n)ext page, ");
					}
					System.out.println("(q)uit or enter number to jump to a page.");

					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'q') {
						quit = true;
						break;
					}
					if (line.charAt(0) == 'p') {
						start = Math.max(0, start - hitsPerPage);
						break;
					} else if (line.charAt(0) == 'n') {
						if (start + hitsPerPage < numTotalHits) {
							start += hitsPerPage;
						}
						break;
					} else {
						int page = Integer.parseInt(line);
						if ((page - 1) * hitsPerPage < numTotalHits) {
							start = (page - 1) * hitsPerPage;
							break;
						} else {
							System.out.println("No such page");
						}
					}
				}
				if (quit)
					break;
				end = Math.min(numTotalHits, start + hitsPerPage);
			}
		}
	}

}
