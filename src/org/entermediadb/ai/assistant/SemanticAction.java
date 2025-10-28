package org.entermediadb.ai.assistant;

import java.util.Collection;
import java.util.List;

import org.openedit.Data;

public class SemanticAction
{
	
	protected Data fieldParentData;
	protected Data fieldChildData;
	
	public Data getChildData()
	{
		return fieldChildData;
	}
	public void setChildData(Data inChildData)
	{
		fieldChildData = inChildData;
	}
	public Data getParentData()
	{
		return fieldParentData;
	}
	public void setParentData(Data inRelatedData)
	{
		fieldParentData = inRelatedData;
	}
	protected Collection<String> fieldFunctionNames;
	
	public Collection<String> getFunctionNames()
	{
		return fieldFunctionNames;
	}
	public void setFunctionNames(Collection<String> inFunctionNames)
	{
		fieldFunctionNames = inFunctionNames;
	}
	public String getRequestType()
	{
		return fieldRequestType;
	}
	public void setRequestType(String inRequestType)
	{
		fieldRequestType = inRequestType;
	}
	protected String fieldRequestType;
	
	protected String fieldSemanticText;
	public String getSemanticText()
	{
		return fieldSemanticText;
	}
	public void setSemanticText(String inSemanticText)
	{
		fieldSemanticText = inSemanticText;
	}
	public List<Double> getVectors()
	{
		return fieldVectors;
	}
	public void setVectors(List<Double> inVectors)
	{
		fieldVectors = inVectors;
	}
	protected List<Double> fieldVectors;
	
}
