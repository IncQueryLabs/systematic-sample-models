package com.incquerylabs.magicdraw.plugin.metamodelreport.dtos;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.google.gson.annotations.SerializedName;
import org.eclipse.emf.ecore.EStructuralFeature;

public class MetaModelCoverageDTO {

	@SerializedName(value = "numOfAllClasses")
	public int allClasses;

	@SerializedName(value = "numOfCoveredClasses")
	public int numOfCoveredClasses;

	@SerializedName(value = "numOfAllFeatures")
	public int allFeatures;

	@SerializedName(value = "numOfCoveredFeatures")
	public int numOfCoveredFeatures;

	@SerializedName(value = "numOfAllConcreteFeatureKinds")
	public int allPotentialCrks;

	@SerializedName(value = "numOfCoveredConcreteFeatureKinds")
	public int numOfCoveredCrks;

	@SerializedName(value = "numOfAllFilteredFeatures")
	public int allFilteredFeatures;

	@SerializedName(value = "numOfCoveredFilteredFeatures")
	public int coveredFilteredFeatures;

	@SerializedName(value = "numOfAllPureUMLFeatures")
	public int allPureUMLFeatures;

	@SerializedName(value = "numOfCoveredPureUMLFeatures")
	public int coveredPureUMLFeatures;

	@SerializedName(value = "coveredClasses")
	public Collection<String> coveredClasses;

	@SerializedName(value = "uncoveredClasses")
	public Collection<String> uncoveredClasses;

	@SerializedName(value = "coveredFeatures")
	public Collection<String> coveredFeatures;

	@SerializedName(value = "coveredConcreteFeatureKinds")
	public Collection<String> coveredCrks;

	public transient Map<EStructuralFeature, Boolean> features;

	@SerializedName(value = "uncoveredFeatures")
	public Collection<String> uncoveredFeatures;

	@SerializedName(value = "uncoveredFilteredFeatures")
	public Collection<String> uncoveredFilteredFeatures;

	@SerializedName(value = "uncoveredPureUMLFeatures")
	public Collection<String> uncoveredPureUMLFeatures;

	@SerializedName(value = "uncoveredConcreteFeatureKinds")
	public Collection<String> uncoveredCrks;

	public static MetaModelCoverageDTO getInitializedInstance(int allClasses, int allFeatures, int allFilteredFeatures, int allPureUMLFeatures, int allPotentialCrks) {
		MetaModelCoverageDTO newBean = new MetaModelCoverageDTO();
		newBean.allClasses = allClasses;
		newBean.allFeatures = allFeatures;
		newBean.allFilteredFeatures = allFilteredFeatures;
		newBean.allPureUMLFeatures = allPureUMLFeatures;
		newBean.allPotentialCrks = allPotentialCrks;
		newBean.features = new HashMap<>();
		newBean.coveredClasses = new TreeSet<>();
		newBean.uncoveredClasses = new TreeSet<>();
		newBean.coveredFeatures = new TreeSet<>();
		newBean.coveredCrks = new TreeSet<>();
		newBean.uncoveredFeatures = new TreeSet<>();
		newBean.uncoveredFilteredFeatures = new TreeSet<>();
		newBean.uncoveredPureUMLFeatures = new TreeSet<>();
		newBean.uncoveredCrks = new TreeSet<>();
		return newBean;
	}

	@Override
	public String toString() {
		return "ModelCoverageBean [\n\t Number of all eClasses=" + allClasses + ",\n\t Number of covered classes="
				+ numOfCoveredClasses + ",\n\t Number of all eFeatures=" + allFeatures
				+ ",\n\t Number of all Filtered eFeatures=" + allFilteredFeatures
				+ ",\n\t Number of all potential Concrete Feature Kinds=" + allPotentialCrks + ",\n\t Number of covered eFeatures="
				+ numOfCoveredFeatures + ",\n\t Number of covered Concrete Feature Kinds="
				+ numOfCoveredCrks + ",\n\t List of uncovered classes=" + uncoveredClasses + ",\n\t uncoveredFeatures="
				+ uncoveredFeatures + ",\n\t List of uncovered Concrete Feature Kinds="
				+ uncoveredCrks + "]";
	}
}
