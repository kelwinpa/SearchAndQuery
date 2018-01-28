package LuceneMDEProject.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;

public class Search {

	public Search() {
		super();
	}

	public void searchByField(Document artifact, String indexPath) throws IOException, ParseException {

		String queryFiled = null;

		System.out.println("\n----------Fields----------");
		List<IndexableField> distint = artifact.getFields().stream().filter(distinctByKey(IndexableField::name))
				.distinct().collect(Collectors.toList());
		int i = 1;
		List<String> fieldsList = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {

			for (IndexableField field : distint) {
				System.out.println(i + ". " + field.name());
				fieldsList.add(field.name());
				i++;
			}
			System.out.println("\nSelect the field that you want to query: ");
			String[] fields = fieldsList.toArray(new String[0]);

			String input = null;
			try {
				input = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (input == null || input.length() == -1) {
				System.out.println("\nValue not readible");
				continue;
			}

			input = input.trim();
			if (input.length() == 0) {
				System.out.println("\nSpace is not acceptable");
				continue;
			}

			if (isNumeric(input)) {
				int d = Integer.parseInt(input);

				if (d > 0 && d <= fields.length) {
					queryFiled = fields[d - 1];
				} else {
					continue;
				}
			} else {
				continue;
			}

			if (queryFiled != null) {
				break;
			}
		}

		System.out.println("You have choosen the field ["+queryFiled+"] as query");
		String[] result = artifact.getValues(queryFiled);
		System.out.println("-----Results------");
		int j =1;
		for(String k :result){
			System.out.println(j+": "+k);
			j++;
		}
		

	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}

	public static boolean isNumeric(String str) {
		try {
			int d = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}
