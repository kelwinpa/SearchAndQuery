package LuceneMDEProject;

public class LuceneServiceImp {

	public static final String TEXT_TAG = "text";
	public static final String TYPE_TAG = "forgeType";
	public static final String NAME_TAG = "name";
	public static final String AUTHOR_TAG = "author";
	public static final String PATH_TAG = "path";
	public static final String LAST_UPDATE_TAG = "lastUpdate";

	/*
	 * ECORE METAMODEL TAGS
	 */

	public static final String EPACKAGE_INDEX_CODE = "ePackage";
	public static final String NsURI_INDEX_CODE = "nsuri";
	public static final String EANNOTATION_INDEX_CODE = "eAnnotation";
	public static final String ECLASS_INDEX_CODE = "eClass";
	public static final String EATTRIBUTE_INDEX_CODE = "eAttribute";
	public static final String EREFERENCE_INDEX_CODE = "eReference";
	public static final String EENUM_INDEX_CODE = "eEnum";
	public static final String ELITERAL_INDEX_CODE = "eLiteral";
	public static final String EDATATYPE_INDEX_CODE = "eDataType";

	public static String[] metamodelLuceneTags = { EPACKAGE_INDEX_CODE, NsURI_INDEX_CODE, EANNOTATION_INDEX_CODE,
			ECLASS_INDEX_CODE, EATTRIBUTE_INDEX_CODE, EREFERENCE_INDEX_CODE, EENUM_INDEX_CODE, ELITERAL_INDEX_CODE,
			EDATATYPE_INDEX_CODE };

	/*
	 * MODEL TAGS
	 */
	public static final String CUSTOM_LUCENE_INDEX_SEPARATOR_CHARACTER = "_";
	public static final String CONFORM_TO_TAG = "conformToMM";
	public static String[] transformationLuceneTags = { CONFORM_TO_TAG };

	/*
	 * ATL Transformation TAGS
	 */
	public static final String HELPER_TAG = "helper";
	public static final String FROM_METAMODEL_TAG = "fromMM";
	public static final String TO_METAMODEL_TAG = "toMM";
	public static final String RULE_NAME_TAG = "rule";
	public static final String FROM_METACLASS = "fromMC";
	public static final String FROM_TOCLASS = "toMC";

	public static String[] modelLuceneTags = { HELPER_TAG, FROM_METAMODEL_TAG, TO_METAMODEL_TAG, RULE_NAME_TAG,
			FROM_METACLASS, FROM_TOCLASS };
}
