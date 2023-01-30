package io.incquery.cameo.modelcoveragereport.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EStructuralFeature;

import io.incquery.cameo.modelcoveragereport.dtos.MetaModelCoverageDTO;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.Finder;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLPackage;

public class MetaModelCoverageAction extends CoverageReportAction {

	private static final long serialVersionUID = 2609117531520837581L;
	private static final String MDTRANSIENT = "mdTransient";

	public MetaModelCoverageAction() {
		super("UML");
	}

	@Override
	public Result actionPerformed() {
		Project project = Application.getInstance().getProject();
		if(project == null) {
			return new Result("You must open a project before generating the report", null);
		}
		File outputDir;
		UMLPackage metamodel = UMLPackage.eINSTANCE;

		List<EClassifier> eClasses = getEclasses(metamodel);
		Set<EStructuralFeature> structuralFeatures =
				getAllStructuralFeatures(metamodel);
		Set<EStructuralFeature> filteredStructuralFeatures =
				structuralFeatures.stream().filter(MetaModelCoverageAction::isFiltered).collect(Collectors.toSet());
		Set<EStructuralFeature> pureUmlFeatures =
				structuralFeatures.stream().filter(MetaModelCoverageAction::isUmlStandard).collect(Collectors.toSet());

		Map<EClassifier, Collection<Element>> eCoreCoverageMap = getClassCoverageMap(eClasses, project);

		outputDir = setupDestinationDirectory(project);
		outputDir.mkdirs();

		MetaModelCoverageDTO modelCoverage = MetaModelCoverageDTO.getInitializedInstance(
												eClasses.size(), 
												structuralFeatures.size(), 
												filteredStructuralFeatures.size(), 
												pureUmlFeatures.size());

		calculateEClassCoverage(eCoreCoverageMap, modelCoverage);
		calculateEDataTypeCoverage(structuralFeatures, eCoreCoverageMap, modelCoverage);
		modelCoverage.numOfCoveredFeatures = 
				calculateStructuralFeatureCoverage(structuralFeatures, eCoreCoverageMap, modelCoverage.coveredFeatures, modelCoverage.uncoveredFeatures, modelCoverage.features);
		modelCoverage.coveredFilteredFeatures = 
				calculateStructuralFeatureCoverage(filteredStructuralFeatures, eCoreCoverageMap, null, modelCoverage.uncoveredFilteredFeatures, null);

		modelCoverage.coveredPureUMLFeatures =
				calculateStructuralFeatureCoverage(pureUmlFeatures, eCoreCoverageMap, null, modelCoverage.uncoveredPureUMLFeatures, null);

		serializeToJson(modelCoverage, outputDir);
		serializeToCsv(modelCoverage, outputDir);

		return new Result(outputDir);
	}

	public static boolean isUmlStandard(EStructuralFeature struct) {
		return !struct.getName().startsWith("_");
	}

	public static boolean isFiltered(EStructuralFeature struct) {
		return !struct.isDerived() && struct.isChangeable() && !struct.isTransient()
				&& struct.getEAnnotations().stream().noneMatch(annot -> MDTRANSIENT.equals(annot.getSource()));
	}

	private Map<EClassifier, Collection<Element>> getClassCoverageMap(List<EClassifier> eClasses, Project project) {
		Map<EClassifier, Collection<Element>> eCoreToModelElementsMap = new HashMap<>();
		for (EClassifier eclass : eClasses) {
			Collection<Element> foundElements = Finder.byTypeRecursively()
					.find(project.getPrimaryModel(), new Class[] { eclass.getInstanceClass() }).stream()
					.collect(Collectors.toList());

			if (!ProjectUtilities.isRemote(project.getPrimaryProject())) {
				foundElements = foundElements.stream().filter(BaseElement::isEditable).collect(Collectors.toList());
			}
			eCoreToModelElementsMap.put(eclass, foundElements);
		}
		return eCoreToModelElementsMap;
	}

	private Set<EStructuralFeature> getAllStructuralFeatures(UMLPackage metamodel) {
		return metamodel.getEClassifiers().stream()
				.filter(EClass.class::isInstance).map(EClass.class::cast)
				.flatMap(cls -> cls.getEStructuralFeatures().stream()).collect(Collectors.toSet());
	}

	private List<EClassifier> getEclasses(UMLPackage metamodel) {
		List<EClassifier> eclasses = new ArrayList<>();
		for (EClassifier classifier : metamodel.getEClassifiers()) {
			if((classifier instanceof EClass && !((EClass) classifier).isAbstract() && !((EClass) classifier).isInterface()) ||
				(classifier instanceof EDataType && classifier.getInstanceClass() != null && !classifier.getInstanceClass().isPrimitive() && classifier != metamodel.getString())) {
				eclasses.add(classifier);
			}
		}

		return eclasses;
	}

	public int calculateStructuralFeatureCoverage(Set<EStructuralFeature> structuralFeatures,
												  Map<EClassifier, Collection<Element>> eclassMap, Collection<String> coveredFeatures, Collection<String> uncoveredFeatures, Map<EStructuralFeature, Boolean> features) {
		int numOfCoveredFeatures = 0;
		for (EStructuralFeature feature : structuralFeatures) {
			if (isFeatureCovered(feature, eclassMap)) {
				numOfCoveredFeatures++;
				if(coveredFeatures != null) {
					coveredFeatures.add(getStringRepresentationOfFeature(feature));
				}
				if(features != null) {
					features.put(feature, true);
				}
			} else {
				uncoveredFeatures.add(getStringRepresentationOfFeature(feature));
				if(features != null) {
					features.put(feature, false);
				}
			}
		}
		return numOfCoveredFeatures;
	}

	private String getStringRepresentationOfFeature(EStructuralFeature feature) {
		return feature.getEContainingClass().getName() + "::" + feature.getName();
	}
	
	public boolean isFeatureCovered(EStructuralFeature feature, Map<EClassifier, Collection<Element>> eclassMap) {
		EClass containingEClass = feature.getEContainingClass();
		for (Entry<EClassifier, Collection<Element>> eClass : eclassMap.entrySet()) {
			EClassifier cls = eClass.getKey();
			if (cls instanceof EClass && (cls == containingEClass || ((EClass) cls).getEAllSuperTypes().contains(containingEClass))) {
				for (Element instance : eClass.getValue()) {
					if (instance.eGet(feature) != null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void calculateEClassCoverage(Map<EClassifier, Collection<Element>> eclassMap,
			MetaModelCoverageDTO coverageInfo) {
		for (Entry<EClassifier, Collection<Element>> cls : eclassMap.entrySet()) {
			if(cls.getKey() instanceof EClass) {
				if (cls.getValue().isEmpty()) {
					coverageInfo.uncoveredClasses.add(cls.getKey().getName());
				} else {
					coverageInfo.numOfCoveredClasses++;
					coverageInfo.coveredClasses.add(cls.getKey().getName());
				}
			}
		}
	}

	private void calculateEDataTypeCoverage(Set<EStructuralFeature> structuralFeatures,
			Map<EClassifier, Collection<Element>> eCoreCoverageMap, MetaModelCoverageDTO modelCoverage) {
		eCoreCoverageMap.keySet().stream()
			.filter(EDataType.class::isInstance).map(EDataType.class::cast)
			.forEach(dt -> {
				boolean dtFound = structuralFeatures.stream()
									// sometimes different EDataTypes has the same instance class so practically they 
									// can be covered the same features (e.g. ParameterEffectKind and ParameterParameterEffectKind)
									.filter(feature -> dt == feature.getEType() || dt.getInstanceClass() == feature.getEType().getInstanceClass())
									.anyMatch(feature -> isFeatureCovered(feature, eCoreCoverageMap));
				if(dtFound) {
					modelCoverage.numOfCoveredClasses++;
					modelCoverage.coveredClasses.add(dt.getName());
				} else {
					modelCoverage.uncoveredClasses.add(dt.getName());
				}
			});
	}

	private void serializeToCsv(MetaModelCoverageDTO coverageDTO, File outputDir) {
		File typeOutputFile = new File(outputDir, String.format("%sTypeCoverageInfo.csv", type));
		try (FileWriter writer = new FileWriter(typeOutputFile)) {
			writer.write("Type,Covered"+System.lineSeparator());
			for(String cl : coverageDTO.coveredClasses) {
				writer.write(cl+",true"+System.lineSeparator());
			}
			for(String cl : coverageDTO.uncoveredClasses) {
				writer.write(cl+",false"+System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		File featureOutputFile = new File(outputDir, String.format("%sFeatureCoverageInfo.csv", type));
		try (FileWriter writer = new FileWriter(featureOutputFile)) {
			writer.write("Feature,Covered,Defining Type,UML Standard,Filtered"+System.lineSeparator());
			for(Map.Entry<EStructuralFeature, Boolean> feature : coverageDTO.features.entrySet()) {
				writer.write(String.format("%s,%b,%s,%b,%b%s",
						feature.getKey().getName(),
						feature.getValue(),
						definingTypeString(feature.getKey()),
						isUmlStandard(feature.getKey()),
						isFiltered(feature.getKey()),
						System.lineSeparator()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String definingTypeString(EStructuralFeature feature) {
		EClass type = feature.getEContainingClass();
		if(type.isAbstract() || type.isInterface()) {
			return "[Abstract] "+type.getName();
		} else {
			return type.getName();
		}
	}
}
