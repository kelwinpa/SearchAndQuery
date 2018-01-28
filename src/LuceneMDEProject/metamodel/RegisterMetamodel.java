package LuceneMDEProject.metamodel;

import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.BasicExtendedMetaData;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

public class RegisterMetamodel {

	public RegisterMetamodel() {
		super();
	}

	public Resource registerMetamodel(Path file, String METAMODEL) {

		URI fileURI = URI.createFileURI(file.toAbsolutePath().toString() + METAMODEL);

		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

		ResourceSet rs = new ResourceSetImpl();

		final ExtendedMetaData extendedMetaData = new BasicExtendedMetaData(rs.getPackageRegistry());

		rs.getLoadOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, extendedMetaData);

		Resource r = rs.getResource(fileURI, true);

		for (EObject eObject : r.getContents()) {
			if (eObject instanceof EPackage) {
				EPackage p = (EPackage) eObject;
				registerSubPackage(p);
			}
		}

		return r;
	}

	private void registerSubPackage(EPackage p) {
		EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
		for (EPackage pack : p.getESubpackages()) {
			registerSubPackage(pack);
		}
	}

}
