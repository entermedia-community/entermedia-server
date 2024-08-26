package org.entermediadb.asset.importer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

public class JsonNode {
	protected String fieldId;
	protected String fieldCode;
	protected int fieldRowPosition;
	protected JsonNode fieldParent;
	protected String fieldTopLevelParent;
	protected String fieldDataJson;
	
	public String getDataJson() {
		return fieldDataJson;
	}
	public void setDataJson(String inDataJson) {
		fieldDataJson = inDataJson;
	}
	protected boolean fieldAlwaysRender;
	protected List fieldChildren;
	protected List fieldDataChildren;
	public List getDataChildren() {
		if (fieldDataChildren == null) {
			fieldDataChildren = new ArrayList();
		}
		return fieldDataChildren;
	}
	protected String fieldName;
	protected String fieldJson;
	
	public boolean isAlwaysRender() {
		return fieldAlwaysRender;
	}
	public void setAlwaysRender(boolean inAlwaysRender) {
		fieldAlwaysRender = inAlwaysRender;
	}
	public String getTopLevelParent() {
		if (fieldTopLevelParent == null && fieldParent != null)
		{
			return getParent().getTopLevelParent();
		}

		return fieldTopLevelParent;
	}
	public void setTopLevelParent(String inTopLevelParent) {
		fieldTopLevelParent = inTopLevelParent;
	}
	public JsonNode getParent() {
		return fieldParent;
	}
	public void setParent(JsonNode inParent) {
		fieldParent = inParent;
	}
	public int getRowPosition() {
		return fieldRowPosition;
	}
	public void setRowPosition(int inRowPosition) {
		fieldRowPosition = inRowPosition;
	}
	public String getSourceId() {
		if( fieldParent != null)
		{
			return getParent().getId();
		}
		return "0";
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
		if( getParent() == null)
		{
			return "0";
		}
		return getParent().getId() + "_" + getRow();
	}
	public void setId(String inId) {
		fieldId = inId;
	}
	protected int fieldRow;

	public int getRow() {
		return fieldRow;
	}
	public void setRow(int inRow) {
		fieldRow = inRow;
	}

	public Element getElement() {
		return fieldElement;
	}
	public void setElement(Element inElement) {
		fieldElement = inElement;
	}
	protected Element fieldElement;
	public int getLevel() 
	{
		int level = 1;
		JsonNode node = getParent();
		while( node != null)
		{
			node = node.getParent();
			level++;
		}
		return level;
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
	
	public void addChildToTop(JsonNode inChildNode) 
	{
		inChildNode.setParent(this);
		if( getChildren().isEmpty())
		{
			getChildren().add(inChildNode);
		}
		else
		{
			getChildren().add(0,inChildNode);
		}
		
	}
	public void addChild(JsonNode inChildNode) 
	{
		inChildNode.setParent(this);
		if( inChildNode.hasChildren() || inChildNode.getDataChildren().size() > 0)
		{
			getChildren().add(inChildNode);
		}
		else
		{
			getDataChildren().add(inChildNode);
		}
	}
	public boolean hasChildren() {
		return !getChildren().isEmpty();
	}
	public String getTextTrim() {
		
		if( getElement() == null)
		{
			return getName();
		}
		String text = null;
		if(getChildren().isEmpty())
		{
			text =  getElement().getTextTrim();
		}
		if( text == null || text.isEmpty())
		{
			text = getElement().getName();
		}
			
		if( text != null && text.length() > 20)
		{
			text = text.substring(0,20) + "~";
		}
		
		return text;
	}

	public int offetX(int width)
	{
		return offetX(width,0);
	}

	public int offetX(int width, int offset)
	{
		int total = (getLevel() - 1) * width + offset;
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
	public int rowoffet(int height, int padding)
	{
		int total = (getRowPosition() * height )+ padding;
		return total;
	}

	public int getDataTotalHeight(int height)
	{
		int total = (getDataChildren().size() * height );
		return total;
	}

		
	
	@Override
	public String toString() {
		StringBuffer text = new StringBuffer();
		renderTree(this,0,text);
		return text.toString();
	}
	
	protected void renderTree(JsonNode inRoot,int deep, StringBuffer inBuffer)
	{
		for (int i = 0; i < deep; i++) {
			inBuffer.append("  ");
		}
		inBuffer.append(inRoot.getLevel() + ":" + inRoot.getName() + inRoot.getId() + "\n");	
		deep++;
		for (Iterator iterator = inRoot.getChildren().iterator(); iterator.hasNext();) {
			JsonNode node = (JsonNode) iterator.next();
			renderTree(node,deep,inBuffer);
		}
	}
	
//	public void addToLevel(int inI) {
//		setLevel(getLevel()+inI);
//		for (Iterator iterator = getChildren().iterator(); iterator.hasNext();) {
//			JsonNode node = (JsonNode) iterator.next();
//			node.addToLevel(inI);
//			
//		}
//	}
	
}
