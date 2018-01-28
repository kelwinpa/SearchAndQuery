package LuceneMDEProject.atlTransformation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.ModelFactory;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.engine.parser.AtlParser;

import LuceneMDEProject.LuceneServiceImp;
import anatlyzer.atl.model.ATLModel;
import anatlyzer.atlext.ATL.Helper;
import anatlyzer.atlext.ATL.InPatternElement;
import anatlyzer.atlext.ATL.MatchedRule;
import anatlyzer.atlext.ATL.ModuleElement;
import anatlyzer.atlext.ATL.OutPatternElement;
import anatlyzer.atlext.ATL.Rule;
import anatlyzer.atlext.ATL.SimpleInPatternElement;
import anatlyzer.atlext.ATL.SimpleOutPatternElement;
import anatlyzer.atlext.OCL.Attribute;
import anatlyzer.atlext.OCL.OclFeatureDefinition;
import anatlyzer.atlext.OCL.Operation;

public class AtlTransformation {

	public AtlTransformation() {
		super();
	}

	public Document createATLIndex(Path atlDir, String idxAtl, Boolean create, Resource metamodel, String ATL)
			throws IOException {

		Directory dir = FSDirectory.open(Paths.get(idxAtl));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		if (create) {
			iwc.setOpenMode(OpenMode.CREATE);
		} else {
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}

		IndexWriter writer = new IndexWriter(dir, iwc);

		Document document = parseArtifactForIndexAtl(atlDir, metamodel, ATL);

		writer.addDocument(document);

		writer.close();
		
		return document;
	}

	private Document parseArtifactForIndexAtl(Path atlDir, Resource metamodel, String ATL) throws IOException {

		Document doc = new Document();
		AtlParser atlParser = new AtlParser();
		ModelFactory modelFactory = new EMFModelFactory();
		IReferenceModel atlMetamodel;

		try {
			atlMetamodel = modelFactory.getBuiltInResource("ATL.ecore");

			URI atlURI = URI.createFileURI(atlDir.toAbsolutePath().toString() + ATL);
			String atlPath = atlURI.toFileString();
			Field pathField = new TextField(LuceneServiceImp.PATH_TAG, atlPath, Field.Store.YES);
			doc.add(pathField);

			EMFModel atlDynModel = (EMFModel) modelFactory.newModel(atlMetamodel);

			atlParser.inject(atlDynModel, atlPath);
			Resource originalTrafo = atlDynModel.getResource();

			ATLModel atlModel = new ATLModel(originalTrafo, originalTrafo.getURI().toFileString(), true);
			EList<ModuleElement> eAllContents = atlModel.getModule().getElements();
			for (ModuleElement moduleElement : eAllContents) {
				if (moduleElement instanceof Helper) {
					Helper h = (Helper) moduleElement;
					if (h.getDefinition() != null) {
						OclFeatureDefinition def = h.getDefinition();
						if (def.getFeature() != null && def.getFeature() instanceof Attribute) {
							Attribute attr = (Attribute) def.getFeature();
							Field attribute = new TextField(LuceneServiceImp.HELPER_TAG, attr.getName(),
									Field.Store.YES);
							Field text = new TextField(LuceneServiceImp.TEXT_TAG, attr.getName(), Field.Store.YES);
							doc.add(text);
							doc.add(attribute);
						}

						if (def.getFeature() != null && def.getFeature() instanceof Operation) {
							Operation attr = (Operation) def.getFeature();
							Field attribute = new TextField(LuceneServiceImp.HELPER_TAG, attr.getName(),
									Field.Store.YES);
							Field text = new TextField(LuceneServiceImp.TEXT_TAG, attr.getName(), Field.Store.YES);
							doc.add(text);
							doc.add(attribute);
						}
					}
				}

				if (moduleElement instanceof Rule) {
					Rule r = (Rule) moduleElement;
					Field ruleName = new TextField(LuceneServiceImp.RULE_NAME_TAG, r.getName(), Field.Store.YES);
					if (r instanceof MatchedRule) {
						MatchedRule mr = (MatchedRule) r;
						EList<InPatternElement> si = mr.getInPattern().getElements();
						for (InPatternElement inPatternElement : si) {
							if (inPatternElement instanceof SimpleInPatternElement) {
								SimpleInPatternElement sipe = (SimpleInPatternElement) inPatternElement;
								Field fromMC = new TextField(LuceneServiceImp.FROM_METACLASS, sipe.getType().getName(), Field.Store.YES);
								doc.add(fromMC);
								Field text = new TextField(LuceneServiceImp.TEXT_TAG, sipe.getType().getName(), Field.Store.YES);
								doc.add(text);
							}
						}

						EList<OutPatternElement> so = mr.getOutPattern().getElements();
						for (OutPatternElement outPatternElement : so) {
							if (outPatternElement instanceof SimpleOutPatternElement) {
								SimpleOutPatternElement sope = (SimpleOutPatternElement) outPatternElement;
								Field toMC = new TextField(LuceneServiceImp.FROM_TOCLASS, sope.getType().getName(), Field.Store.YES);
								doc.add(toMC);
								Field text = new TextField(LuceneServiceImp.TEXT_TAG, sope.getType().getName(), Field.Store.YES);
								doc.add(text);
							}
						}
					}
					doc.add(ruleName);
				}

			}

			File atlTrm = new File(atlPath);

			Field artifactType = new TextField(LuceneServiceImp.TYPE_TAG, atlTrm.getClass().getSimpleName(), Field.Store.YES);
			doc.add(artifactType);

			// get the type of the file --- ask juri
			FilenameUtils.getExtension(atlTrm.getName());

			Field artName = new TextField(LuceneServiceImp.NAME_TAG, atlTrm.getName(), Field.Store.YES);
			doc.add(artName);

			String author = Files.getOwner(atlTrm.toPath()).getName();
			Field authorField = new TextField(LuceneServiceImp.AUTHOR_TAG, author, Field.Store.YES);
			doc.add(authorField);

			String lastUpdate = new Date(atlTrm.lastModified()).toString();
			Field lastUpdateField = new TextField(LuceneServiceImp.LAST_UPDATE_TAG, lastUpdate, Field.Store.YES);
			doc.add(lastUpdateField);

		} catch (ATLCoreException e) {
			System.out.println("ERROR: " + e.getMessage());
			System.exit(1);

		}

		return doc;
	}

}
