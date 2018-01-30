package LuceneMDEProject.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;

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
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreEList;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import LuceneMDEProject.LuceneServiceImp;

public class IndexModel {

	public IndexModel() {
		super();
	}

	public Document createModelIndex(Path modelDir, String idxModel, Boolean create, Resource metamodel, String MODEL)
			throws IOException {

		Directory dir = FSDirectory.open(Paths.get(idxModel));
		Analyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		if (create) {
			iwc.setOpenMode(OpenMode.CREATE);
		} else {
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		}

		IndexWriter writer = new IndexWriter(dir, iwc);

		Document document = parseArtifactForIndexModel(modelDir, metamodel, MODEL);

		writer.addDocument(document);
		writer.close();
		
		return document;
	}

	private Document parseArtifactForIndexModel(Path modelDir, Resource metamodel, String MODEL) throws IOException {

		Document doc = new Document();

		URI fileURI = URI.createFileURI(modelDir.toAbsolutePath().toString() + MODEL);

		ResourceSet load_resourceSet = new ResourceSetImpl();
		load_resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());

		Resource load_resource = load_resourceSet.getResource(fileURI, true);

		File model = new File(fileURI.toFileString());
		modelDir = Paths.get(model.getAbsolutePath());
		Field pathField = new TextField(LuceneServiceImp.PATH_TAG, model.toString(), Field.Store.YES);
		doc.add(pathField);

		Field artifactType = new TextField(LuceneServiceImp.TYPE_TAG, LuceneServiceImp.MODEL_TYPE, Field.Store.YES);
		doc.add(artifactType);

		FilenameUtils.getExtension(model.getName());

		InputStream inputStream = new FileInputStream(model.getAbsolutePath());
		String text = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
		Field textField = new TextField(LuceneServiceImp.TEXT_TAG, text, Field.Store.YES);
		doc.add(textField);

		String artifactName = model.getName();
		Field artName = new TextField(LuceneServiceImp.NAME_TAG, artifactName, Field.Store.YES);
		doc.add(artName);

		String author = Files.getOwner(model.toPath()).getName();
		Field authorField = new TextField(LuceneServiceImp.AUTHOR_TAG, author, Field.Store.YES);
		doc.add(authorField);

		Date lastUpdate = new Date(model.lastModified());
		Field lastUpdateField = new TextField(LuceneServiceImp.LAST_UPDATE_TAG, lastUpdate.toString(), Field.Store.YES);
		doc.add(lastUpdateField);

		// Metamodel
		Object emm = metamodel.getContents().get(0);

		if (emm instanceof EPackageImpl) {
			EPackageImpl ecoreMM = (EPackageImpl) emm;

			Field conformToFieldName = new TextField(LuceneServiceImp.CONFORM_TO_TAG, ecoreMM.getName(), Field.Store.YES);
			doc.add(conformToFieldName);

		}

		if (load_resource.isLoaded() && load_resource.getErrors() != null) {

			EObject root = load_resource.getContents().get(0);
			Diagnostic diagnostic = Diagnostician.INSTANCE.validate(root);
			System.out.println("Model: " + diagnostic);

			EList<EObject> contents = load_resource.getContents();

			for (Iterator<EObject> iter = contents.iterator(); iter.hasNext();) {

				EObject next = iter.next();
				EClass eClass = next.eClass();

				// CLASS ATTRIBUTES
				for (EAttribute attribute : eClass.getEAllAttributes()) {
					EStructuralFeature feature = eClass.getEStructuralFeature(attribute.getName());
					Object resultingDataType = (Object) next.eGet(feature);
					if (resultingDataType != null) {
						String attributeValue = resultingDataType.toString();

						Field eClassWithAttributeField = new TextField(eClass.getName(), attributeValue,
								Field.Store.YES);
						doc.add(eClassWithAttributeField);

						Field eClassWithAttributeAndAttributeValueField = new TextField(
								eClass.getName() + LuceneServiceImp.CUSTOM_LUCENE_INDEX_SEPARATOR_CHARACTER + attribute.getName(), attributeValue, Field.Store.YES);
						doc.add(eClassWithAttributeAndAttributeValueField);

					}

				}

				// EClass References
				for (EReference reference : eClass.getEAllReferences()) {
					Object object = next.eGet(reference);
					if (!(object instanceof EcoreEList)) {
						EObject eo = (EObject) object;
						collectReferences(eo, reference, doc);
					} else {
						@SuppressWarnings("unchecked")
						EcoreEList<EObject> ecoreEList = (EcoreEList<EObject>) object;
						for (EObject eo : ecoreEList) {
							collectReferences(eo, reference, doc);
						}
					}
				}
			}
		}

		return doc;

	}

	private void collectReferences(EObject eo, EReference reference, Document doc) {

		if (eo != null && eo.eClass() instanceof EClass) {
			EClass value = (EClass) eo.eClass();
			if (value != null) {
				for (EAttribute eattribute : value.getEAttributes()) {
					if (eo.eGet(eattribute) != null) {
						String key = reference.getName();
						String indexValue = eo.eGet(eattribute).toString();

						Field eClassReferenceField = new TextField(key, indexValue, Field.Store.YES);
						doc.add(eClassReferenceField);

					}
				}
			}
		}

	}

}
