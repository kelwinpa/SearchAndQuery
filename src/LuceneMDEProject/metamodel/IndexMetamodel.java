package LuceneMDEProject.metamodel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

import LuceneMDEProject.LuceneServiceImp;

public class IndexMetamodel {

	public IndexMetamodel() {
		super();
	}

	public Document parseArtifactForIndex(Path file) {

		Document doc = new Document();

		URI fileURI = URI.createFileURI(file.toString());

		Field pathField = new TextField(LuceneServiceImp.PATH_TAG, file.toString(), Field.Store.YES);
		doc.add(pathField);

		Field artifactType = new TextField(LuceneServiceImp.TYPE_TAG, LuceneServiceImp.METAMODEL_TYPE, Field.Store.YES);
		doc.add(artifactType);

		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore",
				new EcoreResourceFactoryImpl());

		Resource resource = resourceSet.getResource(fileURI, true);

		Collection<String> listnsUri = new ArrayList<String>();

		if (resource.isLoaded() && resource.getErrors() != null) {
			EObject root = resource.getContents().get(0);
			Diagnostic diagnostic = Diagnostician.INSTANCE.validate(root);
			System.out.println("MetaModel " + diagnostic);

			EList<EObject> contents = resource.getContents();

			for (Iterator<EObject> iter = contents.iterator(); iter.hasNext();) {

				EObject next = iter.next();

				if (next instanceof EPackage) {
					EPackage ePackage = (EPackage) next;
					doc = ePackageIndex(ePackage, doc);
					if (ePackage.getNsURI() != null && !ePackage.getNsURI().isEmpty()) {
						listnsUri.add(ePackage.getNsURI());
					}
				} else if (next instanceof EClass) {
					EClass eClass = (EClass) next;
					doc = eClassIndex(eClass, doc);
				} else if (next instanceof EEnum) {
					EEnum eEnum = (EEnum) next;
					doc = eEnumIndex(eEnum, doc);
				} else if (next instanceof EDataType) {
					EDataType eDataType = (EDataType) next;
					doc = eDataTypeIndex(eDataType, doc);
				} else if (next instanceof EAnnotation) {
					// GET all the EAnnotations
					EList<EAnnotation> annotations = ((EModelElement) next).getEAnnotations();
					doc = indexAnnotations(annotations, doc);
				}
			}
		}

		if (listnsUri != null && listnsUri.size() == 1) {
			// TODO How to extract metamodel nsFrom file

			for (String nsUri : listnsUri) {
				Field nsUris = new TextField(LuceneServiceImp.NsURI_INDEX_CODE, nsUri, Field.Store.YES);
				doc.add(nsUris);
			}
		}

		return doc;
	}

	private Document ePackageIndex(EPackage ePackage, Document doc) {
		Field ePackageField = new TextField(LuceneServiceImp.NAME_TAG, ePackage.getName(), Field.Store.YES);
		doc.add(ePackageField);
		// GET NsURI
		if (ePackage.getNsURI() != null && !ePackage.getNsURI().isEmpty()) {
			Field EPackageNsURIField = new TextField(LuceneServiceImp.NsURI_INDEX_CODE, ePackage.getNsURI(),
					Field.Store.YES);
			doc.add(EPackageNsURIField);
		}

		// GET EAnnotation
		EList<EClassifier> eClassifiers = ePackage.getEClassifiers();
		if (eClassifiers != null && !eClassifiers.isEmpty()) {

			for (EClassifier eClassifier : eClassifiers) {
				
				if (eClassifier instanceof EClass) {
					EClass eClass = (EClass) eClassifier;
					doc = eClassIndex(eClass, doc);
				} else if(eClassifier instanceof EDataType) { 
					//PrimitiveTypes
					EDataType eDataType = (EDataType) eClassifier;
					doc = eDataTypeIndex(eDataType, doc);
				}
				
				
				
			}

		}

		// GET EAnnotation
		EList<EAnnotation> annotations = ePackage.getEAnnotations();
		if (annotations != null && !annotations.isEmpty()) {
			doc = indexAnnotations(annotations, doc);
		}
		return doc;
	}

	private Document eClassIndex(EClass eClass, Document doc) {
		try {

			Field eClassField = new TextField(LuceneServiceImp.ECLASS_INDEX_CODE, eClass.getName(), Field.Store.YES);
			doc.add(eClassField);

			// GET EAnnotation
			EList<EAnnotation> annotations = eClass.getEAnnotations();
			if (annotations != null && !annotations.isEmpty()) {
				doc = indexAnnotations(annotations, doc);
			}

			// Index EClass Attributes
			for (EAttribute attribute : eClass.getEAttributes()) {
				Field eClassAttributeField = new TextField(LuceneServiceImp.EATTRIBUTE_INDEX_CODE, attribute.getName(),
						Field.Store.YES);
				doc.add(eClassAttributeField);

				if (attribute.getEType() != null) {

					if (attribute.getEType() instanceof EDataType) {
						EDataType eDataType = (EDataType) attribute.getEType();
						doc = eDataTypeIndex(eDataType, doc);
					}
				}
			}

			// Index EClass References
			for (EReference reference : eClass.getEReferences()) {
				Field eClassReferenceField = new TextField(LuceneServiceImp.EREFERENCE_INDEX_CODE, reference.getName(),
						Field.Store.YES);
				doc.add(eClassReferenceField);
			}

		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
		}

		return doc;
	}

	private static Document eDataTypeIndex(EDataType eDataType, Document doc) {
		Field eDataTypeField = new TextField(LuceneServiceImp.EDATATYPE_INDEX_CODE, eDataType.getName(),
				Field.Store.YES);
		doc.add(eDataTypeField);
		return doc;
	}

	private Document indexAnnotations(List<EAnnotation> annotations, Document doc) {
		if (annotations != null && !annotations.isEmpty()) {
			for (EAnnotation eAnnotation : annotations) {
				if (getAnnotationKey(eAnnotation) != null && getAnnotationKey(eAnnotation).equals("weight")) {
					if (getAnnotationValue(eAnnotation) != null) {
						Field EPackageEAnnotationField = new TextField(LuceneServiceImp.EANNOTATION_INDEX_CODE,
								getAnnotationValue(eAnnotation), Field.Store.YES);
						doc.add(EPackageEAnnotationField);
					}
				}
			}
		}
		return doc;
	}

	private String getAnnotationKey(EAnnotation eAnnotation) {
		String result = null;
		if (eAnnotation != null) {
			EMap<String, String> annotationDetails = eAnnotation.getDetails();
			for (Entry<String, String> entry : annotationDetails) {
				if (entry.getKey() != null && entry.getValue() != null) {
					result = entry.getKey();
				}
			}
		}
		return result;
	}

	private String getAnnotationValue(EAnnotation eAnnotation) {
		String result = null;
		if (eAnnotation != null) {
			EMap<String, String> annotationDetails = eAnnotation.getDetails();
			for (Entry<String, String> entry : annotationDetails) {
				if (entry.getKey() != null && entry.getValue() != null) {
					result = entry.getValue();
				}
			}
		}
		return result;
	}

	private Document eEnumIndex(EEnum eEnum, Document doc) {
		Field eEnumField = new TextField(LuceneServiceImp.EENUM_INDEX_CODE, eEnum.getName(), Field.Store.YES);
		doc.add(eEnumField);
		return doc;
	}

}
