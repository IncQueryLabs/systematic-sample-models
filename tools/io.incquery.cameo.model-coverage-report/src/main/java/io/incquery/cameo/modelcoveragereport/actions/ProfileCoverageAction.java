package io.incquery.cameo.modelcoveragereport.actions;

import io.incquery.cameo.modelcoveragereport.dtos.ProfileCoverageDTO;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.uml.Finder;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.VisibilityKindEnum;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLPackage;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ProfileCoverageAction extends CoverageReportAction {
	private static final long serialVersionUID = 7959507763497894824L;
	private transient Element selectedObjects;

	public ProfileCoverageAction(Profile selectedObjects) {
		super(selectedObjects.getName());
		this.selectedObjects = selectedObjects;
	}

	@Override
	public Result actionPerformed() {
		Project project = Application.getInstance().getProject();
		File outputDir = setupDestinationDirectory(project);
		outputDir.mkdirs();

		Collection<Stereotype> stereotypes = getAllStereotypes(selectedObjects);
		Collection<Stereotype> filteredStereotypes = filterStereotpyes(stereotypes);
		Collection<Property> propertysOfStereotyps = getPropertysOfStereotyps(stereotypes);
		ProfileCoverageDTO profileCoverageBean = new ProfileCoverageDTO(filteredStereotypes.size(), propertysOfStereotyps.size());

		Map<Stereotype, Collection<Element>> stereotypedElementsMap = 
				calculateStereotypedElementsMap(filteredStereotypes, project);
		Map<Property, Collection<Element>> propertyCoverageMap =
				calculatePropertyMap(stereotypedElementsMap, propertysOfStereotyps);

		calculateStereotypesCoverage(stereotypedElementsMap, profileCoverageBean);

		calculatePropertyCoverage(propertyCoverageMap, profileCoverageBean);
		
		serializeToJson(profileCoverageBean, outputDir);

		return new Result(outputDir);
	}

	private Map<Property, Collection<Element>> calculatePropertyMap(
			Map<Stereotype, Collection<Element>> stereotypedElementsMap, Collection<Property> propertysOfStereotyps) {
		Map<Property, Collection<Element>> propertyCoverageMap = new HashMap<>();
		for (Property property : propertysOfStereotyps) {
			propertyCoverageMap.put(property, new HashSet<>());
		}

		for (Entry<Stereotype, Collection<Element>> stereotypedElements : stereotypedElementsMap.entrySet()) {
			Stereotype stereo = stereotypedElements.getKey();
			Collection<Property> currentProps = stereo.getMember().stream()
					.filter(Property.class::isInstance).map(Property.class::cast)
					.filter(prop -> prop.getVisibility() == VisibilityKindEnum.PUBLIC)
					.collect(Collectors.toList());
			for (Element stereotypedElement : stereotypedElements.getValue()) {
				for (Property property : currentProps) {
					// MD190sp4 version
					Slot slot = StereotypesHelper.getSlot(stereotypedElement, stereo, property, false, false);
					if (slot != null) {
						propertyCoverageMap.get(property).add(slot);
					}
					// MD2021xr1 version
//					TaggedValue val = StereotypesHelper.getTaggedValue(stereotypedElement, property);
//					if(val != null && val.hasValue()) {
//						propertyCoverageMap.get(property).add(val);
//					}
				}
			}
		}
		return propertyCoverageMap;
	}

	private Map<Stereotype, Collection<Element>> calculateStereotypedElementsMap(
			Collection<Stereotype> filteredStereotypes, Project project) {
		Map<Stereotype, Collection<Element>> stereotypedElementsMap = new HashMap<>();

		for (Stereotype stereo : filteredStereotypes) {
			stereotypedElementsMap.put(stereo, new HashSet<>());
		}

		project.getPrimaryModel().eAllContents().forEachRemaining(con -> {
			if (con instanceof Element) {
				Element elem = (Element) con;
				if (elem.isEditable() || ProjectUtilities.isRemote( project.getPrimaryProject())) {
					Collection<Stereotype> foundStereotypes = StereotypesHelper
							.getAllAssignedStereotypes(Collections.singletonList(elem));
					for (Stereotype st : foundStereotypes) {
						if (stereotypedElementsMap.containsKey(st)) {
							stereotypedElementsMap.get(st).add(elem);
						}
					}
				}
			}

		});
		return stereotypedElementsMap;
	}

	private Collection<Stereotype> getAllStereotypes(Element profile) {
		return Finder.byTypeRecursively()
				.find(profile, new Class[] { UMLPackage.eINSTANCE.getStereotype().getInstanceClass() }).stream()
				.map(Stereotype.class::cast).collect(Collectors.toList());
	}

	private Collection<Stereotype> filterStereotpyes(Collection<Stereotype> stereotypes) {
		return stereotypes.stream().filter(str -> !str.isAbstract()).collect(Collectors.toList());
	}

	private Collection<Property> getPropertysOfStereotyps(Collection<Stereotype> stereotypes) {
		return stereotypes.stream().flatMap(str -> str.getOwnedAttribute().stream())
				.filter(prop -> prop.getVisibility() == VisibilityKindEnum.PUBLIC).collect(Collectors.toList());
	}

	public void calculateStereotypesCoverage(Map<Stereotype, Collection<Element>> stereotypedElementsMap, ProfileCoverageDTO profileCoverageBean) {
		int coveredStereotypes = 0;
		for (Entry<Stereotype, Collection<Element>> stereotypedElements : stereotypedElementsMap.entrySet()) {
			String stereoName = stereotypedElements.getKey().getQualifiedName();
			if (stereotypedElements.getValue().isEmpty()) {
				profileCoverageBean.uncoveredStereotypes.add(stereoName);
			} else {
				coveredStereotypes++;
				profileCoverageBean.coveredStereotypes.add(stereoName);
			}
		}
		profileCoverageBean.numOfCoveredStereotypes = coveredStereotypes;
	}

	public void calculatePropertyCoverage(Map<Property, Collection<Element>> propertyCoverageMap, ProfileCoverageDTO profileCoverageBean) {
		int coveredPublicProperties = 0;
		for (Entry<Property, Collection<Element>> properties : propertyCoverageMap.entrySet()) {
			String propName = properties.getKey().getQualifiedName();
			if (properties.getValue().isEmpty()) {
				profileCoverageBean.uncoveredPublicProperties.add(propName);
			} else {
				coveredPublicProperties++;
				profileCoverageBean.coveredPublicProperties.add(propName);
			}
		}
		profileCoverageBean.numOfCoveredPublicProperties = coveredPublicProperties;

	}

}
