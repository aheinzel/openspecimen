
package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;

public class PermissibleValueDetails {

	private Long id;

	private Long parentId;
	
	private String parentValue;

	private String value;

	private String attribute;

	private String conceptCode;

	private Map<String, String> props = new HashMap<>();

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String activityStatus = "Active";
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getParentValue() {
		return parentValue;
	}

	public void setParentValue(String parentValue) {
		this.parentValue = parentValue;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getConceptCode() {
		return conceptCode;
	}

	public void setConceptCode(String conceptCode) {
		this.conceptCode = conceptCode;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

	//
	// For BO Template
	//
	public void setPropMap(List<Map<String, String>> propMap) {
		this.props = propMap.stream().collect(Collectors.toMap(p -> p.get("name"), p -> p.get("value")));
	}

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	public List<Map<String, String>> getPropMap() {
		if (props == null || props.isEmpty()) {
			return Collections.emptyList();
		}

		List<Map<String, String>> result = new ArrayList<>();
		for (Map.Entry<String, String> pe : props.entrySet()) {
			Map<String, String> r = new HashMap<>();
			r.put("name",  pe.getKey());
			r.put("value", pe.getValue());
			result.add(r);
		}

		return result;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static PermissibleValueDetails fromDomain(PermissibleValue permissibleValue) {
		PermissibleValueDetails details = new PermissibleValueDetails();
		details.setConceptCode(permissibleValue.getConceptCode());
		details.setId(permissibleValue.getId());
		details.setAttribute(permissibleValue.getAttribute());
		details.setValue(permissibleValue.getValue());
		if (permissibleValue.getParent() != null) {
			details.setParentId(permissibleValue.getParent().getId());
			details.setParentValue(permissibleValue.getParent().getValue());
		}
		
		if (permissibleValue.getProps() != null && !permissibleValue.getProps().isEmpty()) {
			details.setProps(permissibleValue.getProps());
		}
		
		return details;
	}
}
