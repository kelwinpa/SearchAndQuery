/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package LuceneMDEProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import LuceneMDEProject.search.Search;

/** Simple command-line based search demo. */
public class SearchFiles {
	private SearchFiles() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {

		String usage = "java LuceneMDEProject.IndexFiles.SearchFiles "
				+ "[-indmm INDEX_MM] [-indmodel INDEX_MODEL] [-indatl INDEX_ATL]";

		String idxMm = null;
		String idxModel = null;
		String idxAtl = null;

		for (int i = 0; i < args.length; i++) {
			if ("-indmm".equals(args[i])) {
				idxMm = args[i + 1];
				i++;
			} else if ("-indmodel".equals(args[i])) {
				idxModel = args[i + 1];
				i++;
			} else if ("-indatl".equals(args[i])) {
				idxAtl = args[i + 1];
				i++;
			}
		}

		if (idxMm == null || idxModel == null || idxAtl == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		List<String> indexPathList = new ArrayList<>();
		indexPathList.add(idxMm);
		indexPathList.add(idxModel);
		indexPathList.add(idxAtl);

		Date startSearch = new Date();
		Search search = new Search();
		System.out.println("Search in directory...");
		try {
			search.searchIndex(indexPathList);
		} catch (IOException | ParseException e1) {
			e1.printStackTrace();
		}
		Date endSearch = new Date();
		System.out.println(endSearch.getTime() - startSearch.getTime() + " total milliseconds\n");

	}

}
