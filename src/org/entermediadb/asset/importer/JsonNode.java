package org.entermediadb.asset.importer;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

public class JsonNode {
	protected String fieldId;
	protected String fieldCode;
	protected String fieldSourceId;
	public String getSourceId() {
		return fieldSourceId;
	}
	public void setSourceId(String inSourceId) {
		fieldSourceId = inSourceId;
	}
	protected String fieldTargetId;
	
	public String getCode() {
		return fieldCode;
	}
	public void setCode(String inCode) {
		fieldCode = inCode;
	}
	public String getText() {
		return fieldText;
	}
	public void setText(String inText) {
		fieldText = inText;
	}
	protected String fieldText;
	public String getId() {
		if( fieldId == null)
		{
			return getLevel() + "_" + getRow();
		}
		return fieldId;
	}
	public void setId(String inId) {
		fieldId = inId;
	}
	protected int fieldLevel; //Column
	protected int fieldRow;

	public int getRow() {
		return fieldRow;
	}
	public void setRow(int inRow) {
		fieldRow = inRow;
	}
	protected List fieldChildren;
	protected String fieldName;
	protected String fieldJson;

	public Element getElement() {
		return fieldElement;
	}
	public void setElement(Element inElement) {
		fieldElement = inElement;
	}
	protected Element fieldElement;
	public int getLevel() {
		return fieldLevel;
	}
	public void setLevel(int inLevel) {
		fieldLevel = inLevel;
	}
	public List getChildren() {
		if (fieldChildren == null) {
			fieldChildren = new ArrayList();
		}

		return fieldChildren;
	}
	public void setChildren(List inChildren) {
		fieldChildren = inChildren;
	}
	public String getName() {
		return fieldName;
	}
	public void setName(String inName) {
		fieldName = inName;
	}
	public String getJson() {
		return fieldJson;
	}
	public void setJson(String inJson) {
		fieldJson = inJson;
	}
	public void addChild(JsonNode inChildNode) 
	{
		//Source is us
		inChildNode.setSourceId(getId());
		
		//Target is them
		getChildren().add(inChildNode);
	
	}
	public String getTextTrim() {
		// TODO Auto-generated method stub
		return getElement().getTextTrim();
	}

	public int offetX(int width)
	{
		return offetX(width,0);
	}

	public int offetX(int width, int offset)
	{
		int total = (getLevel()) * width + offset;
		return total;
	}
	public int offetY(int width)
	{
		return offetY(width,0);
	}

	public int offetY(int height, int offset)
	{
		int total = (getRow()) * height + offset;
		return total;
	}

	@Override
	public String toString() {
		return getName() + " " + getId() + " children: " + getChildren();
	}
	
}
