package LuceneMDEProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.emf.ecore.resource.Resource;

import LuceneMDEProject.atlTransformation.AtlTransformation;
import LuceneMDEProject.metamodel.IndexMetamodel;
import LuceneMDEProject.metamodel.RegisterMetamodel;
import LuceneMDEProject.model.IndexModel;
import LuceneMDEProject.search.Search;

public class IndexFiles {

	//private static final String METAMODEL = "/Families.ecore";
	// private static final String METAMODEL = "/ATL.ecore";
	//private static final String MODEL = "/Families.xmi";
	private static final String METAMODEL = "/Persons.ecore";
	private static final String MODEL = "/Persons.xmi";
	private static final String ATL = "/Families2Persons.atl";

	// upload both ecore and both model and index them.

	public IndexFiles() {
		super();
	}

	public static void main(String[] args) {

		String usage = "java LuceneMDEProject.IndexFiles"
				+ " [-indmm INDEX_MM] [-mmpath MM_PATH] [-indmodel INDEX_MODEL] [-modelpath MODEL_PATH] [-indatl INDEX_ATL] [-atlpath ATL_PATH] [-update]";

		String idxMm = null;
		String idxModel = null;
		String idxAtl = null;

		String mmPath = null;
		String modelPath = null;
		String atlPath = null;

		boolean create = true;

		for (int i = 0; i < args.length; i++) {
			if ("-indmm".equals(args[i])) {
				idxMm = args[i + 1];
				i++;
			} else if ("-mmpath".equals(args[i])) {
				mmPath = args[i + 1];
				i++;
			} else if ("-indmodel".equals(args[i])) {
				idxModel = args[i + 1];
				i++;
			} else if ("-modelpath".equals(args[i])) {
				modelPath = args[i + 1];
				i++;
			} else if ("-indatl".equals(args[i])) {
				idxAtl = args[i + 1];
				i++;
			} else if ("-atlpath".equals(args[i])) {
				atlPath = args[i + 1];
				i++;
			} else if ("-update".equals(args[i])) {
				create = false;
			}
		}

		if (mmPath == null || modelPath == null || atlPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		final Path mmDir = Paths.get(mmPath);
		final Path modelDir = Paths.get(modelPath);
		final Path atlDir = Paths.get(atlPath);

		if (!Files.isReadable(mmDir) || !Files.isReadable(modelDir) || !Files.isReadable(atlDir)) {
			System.out.println(
					"Document directory '" + mmDir.toAbsolutePath() + "' or '" + modelDir.toAbsolutePath() + "' or '"
							+ atlDir.toAbsolutePath() + "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Document metamodelArtifact = new Document();
		Date start = new Date();
		try {
			System.out.println("Indexing Metamodel to directory '" + idxMm + "'...");

			Directory dir = FSDirectory.open(Paths.get(idxMm));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			if (create) {
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			IndexWriter writer = new IndexWriter(dir, iwc);

			IndexMetamodel indexMetamodel = new IndexMetamodel();
			metamodelArtifact = indexMetamodel.parseArtifactForIndex(mmDir, METAMODEL);

			writer.addDocument(metamodelArtifact);

			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds\n");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		System.out.println("Registering Metamodel...");

		RegisterMetamodel registerMetamodel = new RegisterMetamodel();
		Resource metamodel = registerMetamodel.registerMetamodel(mmDir, METAMODEL);
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds\n");

		// Create index Model

		Date startModel = new Date();
		System.out.println("Indexing Model to directory '" + idxModel + "'...");
		Document modelArtifact = new Document();
		try {
			IndexModel indexModel = new IndexModel();
			modelArtifact = indexModel.createModelIndex(modelDir, idxModel, create, metamodel, MODEL);
			Date endModel = new Date();
			System.out.println(endModel.getTime() - startModel.getTime() + " total milliseconds\n");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		// Create index for Atl Transforation
		Date startATL = new Date();
		Document transArtifact = new Document();
		System.out.println("Indexing ATL Transformation Model to directory '" + idxAtl + "'...");
		try {
			AtlTransformation atlTransformation = new AtlTransformation();
			transArtifact = atlTransformation.createATLIndex(atlDir, idxAtl, create, metamodel, ATL);
			Date endATL = new Date();
			System.out.println(endATL.getTime() - startATL.getTime() + " total milliseconds\n");
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		// Search
		List<Document> artifactList = new ArrayList<>();
		artifactList.add(metamodelArtifact);
		artifactList.add(modelArtifact);
		artifactList.add(transArtifact);

		List<String> indexPathList = new ArrayList<>();
		indexPathList.add(idxMm);
		indexPathList.add(idxModel);
		indexPathList.add(idxAtl);

		Date startSearch = new Date();
		Search search = new Search();
		System.out.println("Search in directory...");
		try {
			search.searchIndex(artifactList, indexPathList);
		} catch (IOException | ParseException e1) {
			e1.printStackTrace();
		}
		Date endSearch = new Date();
		System.out.println(endSearch.getTime() - startSearch.getTime() + " total milliseconds\n");
	}

}
