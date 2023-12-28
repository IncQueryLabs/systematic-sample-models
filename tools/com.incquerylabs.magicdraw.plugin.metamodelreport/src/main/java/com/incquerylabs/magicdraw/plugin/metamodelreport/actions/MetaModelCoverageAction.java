package com.incquerylabs.magicdraw.plugin.metamodelreport.actions;

import com.incquerylabs.magicdraw.plugin.metamodelreport.dtos.MetaModelCoverageDTO;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.Finder;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLPackage;
import org.eclipse.emf.ecore.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

		Set<List<ENamedElement>> potentialCRKs = getPotentialCRKs(metamodel, structuralFeatures);


		Map<EClassifier, Collection<Element>> eCoreCoverageMap = getClassCoverageMap(eClasses, project);
		Set<List<ENamedElement>> actualCRKs = getActualCRKs(eCoreCoverageMap);


		outputDir = setupDestinationDirectory(project);
		outputDir.mkdirs();

		MetaModelCoverageDTO modelCoverage = MetaModelCoverageDTO.getInitializedInstance(
												eClasses.size(), 
												structuralFeatures.size(), 
												filteredStructuralFeatures.size(), 
												pureUmlFeatures.size(),
												potentialCRKs.size());

		calculateEClassCoverage(eCoreCoverageMap, modelCoverage);
		calculateEDataTypeCoverage(structuralFeatures, eCoreCoverageMap, modelCoverage);
		modelCoverage.numOfCoveredFeatures = 
				calculateStructuralFeatureCoverage(structuralFeatures, eCoreCoverageMap, modelCoverage.coveredFeatures, modelCoverage.uncoveredFeatures, modelCoverage.features);
		modelCoverage.coveredFilteredFeatures = 
				calculateStructuralFeatureCoverage(filteredStructuralFeatures, eCoreCoverageMap, null, modelCoverage.uncoveredFilteredFeatures, null);

		modelCoverage.coveredPureUMLFeatures =
				calculateStructuralFeatureCoverage(pureUmlFeatures, eCoreCoverageMap, null, modelCoverage.uncoveredPureUMLFeatures, null);

		modelCoverage.numOfCoveredCrks = actualCRKs.size();
		actualCRKs.stream().map(MetaModelCoverageAction::crkToString).forEach(modelCoverage.coveredCrks::add);
		potentialCRKs.stream().filter(crk -> ! actualCRKs.contains(crk)).map(MetaModelCoverageAction::crkToString).forEach(modelCoverage.uncoveredCrks::add);

		serializeToJson(modelCoverage, outputDir);
		serializeToCsv(modelCoverage, outputDir);

		return new Result(outputDir);
	}

	private static String crkToString(List<ENamedElement> crk) {
		return crk.stream().map(ENamedElement::getName).collect(Collectors.joining(" "));
	}

	private Set<List<ENamedElement>> getPotentialCRKs(UMLPackage metamodel, Set<EStructuralFeature> structuralFeatures) {
		Set<List<ENamedElement>> results = new HashSet<>();
		structuralFeatures.stream()
			.filter(this::isCrkFeature)
			.flatMap( feature ->
				getConcreteClassifiers(metamodel, feature.getEContainingClass()).flatMap( srcConcrete ->
					getConcreteClassifiers(metamodel, feature.getEType()).map ( trgConcrete ->
						Arrays.asList(srcConcrete, feature, trgConcrete)
					)
				)
			).forEach(results::add);
		return results;
	}

	private Stream<EClassifier> getConcreteClassifiers(UMLPackage metamodel, EClassifier type) {
		if (type instanceof EDataType) {
			return Stream.of(type);
		} else {
			EClass superClass = (EClass) type;

			return metamodel.getEClassifiers().stream()
					.filter(EClass.class::isInstance).map(EClass.class::cast)
					.filter(eClass -> !eClass.isAbstract())
					.filter(superClass::isSuperTypeOf)
					.map(EClassifier.class::cast);
		}

	}

	private Set<List<ENamedElement>> getActualCRKs(Map<EClassifier, Collection<Element>> eCoreCoverageMap) {
		Set<List<ENamedElement>> results = new HashSet<>();
		eCoreCoverageMap.entrySet().stream()
			.flatMap( entry -> entry.getValue().stream())
			.flatMap( element ->
				element.eClass().getEAllStructuralFeatures().stream()
					.filter(this::isCrkFeature)
					.flatMap( feature ->
						eGetValues(element, feature).stream()
							.filter(val -> isNonDefault(val, feature))
							.map(val -> feature instanceof EReference ? ((EObject) val).eClass() : feature.getEType() )
							.map(concreteValClass -> Arrays.asList(element.eClass(), feature, concreteValClass) )
					)
			).forEach(results::add);
		return results;
	}

	public boolean isCrkFeature(EStructuralFeature feature) {
		return isFiltered(feature);
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
					if (eGetValues(instance, feature).stream().anyMatch( val -> isNonDefault(val, feature) ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Collection<Object> eGetValues(Element instance, EStructuralFeature feature) {
		Object valueOrCollection = instance.eGet(feature);
		if (feature.isMany()) {
			return (Collection<Object>) valueOrCollection;
		} else { // single-valued
			return valueOrCollection == null ? Collections.emptySet() : Collections.singleton(valueOrCollection);
		}
	}

	private static boolean isNonDefault(Object val, EStructuralFeature feature) {
		if (feature instanceof EAttribute) {
			EDataType dataType = ((EAttribute) feature).getEAttributeType();
			return dataType.getDefaultValue() != val;
		} else {
			return true;
		}
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
