package io.incquery.cameo.modelcoveragereport.dtos;

import java.util.Collection;
import java.util.TreeSet;

import com.google.gson.annotations.SerializedName;

public class ProfileCoverageDTO {

	@SerializedName(value = "numOfAllStereotypes")
	public int allStereotypes;

	@SerializedName(value = "numOfCoveredStereotypes")
	public int numOfCoveredStereotypes;

	@SerializedName(value = "numOfAllPublicProperties")
	public int allPublicProperties;

	@SerializedName(value = "numOfCoveredPublicProperties")
	public int numOfCoveredPublicProperties;
	
	@SerializedName(value = "coveredStereotypes")
	public Collection<String> coveredStereotypes;

	@SerializedName(value = "uncoveredStereotypes")
	public Collection<String> uncoveredStereotypes;
	
	@SerializedName(value = "coveredPublicProperties")
	public Collection<String> coveredPublicProperties;

	@SerializedName(value = "uncoveredPublicProperties")
	public Collection<String> uncoveredPublicProperties;

	public ProfileCoverageDTO(int allStereotypes, int allPublicProperties) {
		super();
		this.allStereotypes = allStereotypes;
		this.allPublicProperties = allPublicProperties;
		this.coveredStereotypes = new TreeSet<>();
		this.uncoveredStereotypes = new TreeSet<>();
		this.coveredPublicProperties = new TreeSet<>();
		this.uncoveredPublicProperties = new TreeSet<>();
	}

	@Override
	public String toString() {
		return "ModelCoverageBean [\n\t Number of all Stereotypes=" + allStereotypes
				+ ",\n\t Number of covered Stereotypes=" + numOfCoveredStereotypes + ",\n\t Number of all Public Properties="
				+ allPublicProperties + ",\n\t Number of covered Public Properties=" + numOfCoveredPublicProperties
				+ ",\n\t List of uncovered Stereotypes=" + uncoveredStereotypes + ",\n\t uncoveredPublicProperties="
				+ uncoveredPublicProperties + "]";
	}

}
