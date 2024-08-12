package org.entermediadb.asset.importer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

public class JsonNode {
	protected String fieldId;
	protected String fieldCode;
	protected String fieldSourceId;
	protected int fieldRowPosition;
	
	public int getRowPosition() {
		return fieldRowPosition;
	}
	public void setRowPosition(int inRowPosition) {
		fieldRowPosition = inRowPosition;
	}
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
		getChildren().add(inChildNode);
	
	}
	public String getTextTrim() {
		String text =  getElement().getTextTrim();
		if( text == null)
		{
			text = getElement().getName();
		}
		if( text != null && text.length() > 30)
		{
			text = text.substring(0,30) + "~";
		}
		
		return text;
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
	public int rowoffet(int height)
	{
		int total = (getRowPosition()) * height;
		return total;
	}

	@Override
	public String toString() {
		return getName() + " " + getId() + " children: " + getChildren();
	}
	public void addToLevel(int inI) {
		setLevel(getLevel()+inI);
		for (Iterator iterator = getChildren().iterator(); iterator.hasNext();) {
			JsonNode node = (JsonNode) iterator.next();
			node.addToLevel(inI);
			
		}
	}
	
}
