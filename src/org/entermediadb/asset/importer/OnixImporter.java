package org.entermediadb.asset.importer;

import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.View;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageStreamer;
import org.openedit.util.XmlUtil;

public class OnixImporter extends BaseImporter{

	protected Map<String,Page> foundtypes = new HashMap();
	Map<String,JsonNode> multiplevalues = new HashMap();
	protected Data fieldEntity;
	public Data getEntity() {
		return fieldEntity;
	}


	public void setEntity(Data inEntity) {
		fieldEntity = inEntity;
	}

	public Data getModule() {
		return fieldModule;
	}

	public void setModule(Data inModule) {
		fieldModule = inModule;
	}

	protected Data fieldModule;
	
	@Override
	public void importData() throws Exception
	{
		fieldSearcher = loadSearcher(context);

		Page uploadedpage = (Page)context.getPageValue("uploadedpage");
		if( uploadedpage  == null )
		{
			uploadedpage = getPageManager().getPage("/WEB-INF/import/onix-example.xml");
		}
		Reader reader = uploadedpage.getReader();
		
		XmlUtil util = (XmlUtil)getMediaArchive().getBean("xmlUtil");
		Element root = util.getXml(reader, "UTF-8");
		
//		PropertyDetail rootdetail = new PropertyDetail();
//		detail.setId(detailid);
//		detail.setDataType("list");
//
//		ONIXMessage
		//Now render JSON. One for each level
		JsonNode rootjson = new JsonNode();
		rootjson.setLevel(1);
		rootjson.setName("ONIXMessage");
		//Add Header and Misc Metadata
		Element header = root.element("Header");
		JsonNode headernode = createJsonNodes(2,header);
	//	headernode.setLevel(1);
		rootjson.addChild(headernode);

		JsonNode datanode = new JsonNode();
		datanode.setLevel(2);
		datanode.setName("Metadata");
		rootjson.addChild(datanode);

		Element toplevel = root.element("Product");
		multiplevalues.clear();
		findRedundant(toplevel, datanode, multiplevalues);
		
		defineDataView();

		addTopChildren(datanode, toplevel);		

		//Now render allJSON recursively
		Map<Integer,Integer> rowcounts = new HashMap();
		fixMath(rootjson,rowcounts);

		Map<Integer,Integer> rowspent= new HashMap();
		renderJson(rootjson,rowcounts,rowspent);
		
		//getSearcher().saveData(getEntity());
		getMediaArchive().saveData(getModule().getId(),getEntity());
		//Now collect all the nodes into a big list
		List collecteddatarows = new ArrayList();
		Searcher componentsearcher = getMediaArchive().getSearcher("componentdata");
		Collection hits = componentsearcher.query().exact("entityid", getEntity()).search();
		componentsearcher.deleteAll(hits,null);
		collectJson(componentsearcher,rootjson,collecteddatarows);
		//getEntity().setValue("onixjson",collectedjson);
		
		int counted = 0;
		for (Iterator iterator = collecteddatarows.iterator(); iterator.hasNext();) 
		{
			Data node = (Data)iterator.next();
			node.setValue("ordering",counted++);
		}
		
		componentsearcher.saveAllData(collecteddatarows, null);
	}


	protected void addTopChildren(JsonNode datanode, Element toplevel) {
		for (Iterator iterator = toplevel.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			JsonNode placeholder = multiplevalues.get(child.getName());
			JsonNode childdata = createJsonNodes(3,child);
			if( placeholder == null)
			{
				datanode.addChild(childdata);
			}
			else
			{
				childdata.addToLevel(1); //Push out
				placeholder.addChild(childdata);
			}
		}
	}


	protected void defineDataView() {
		Searcher searcher = getMediaArchive().getSearcher("view");
		Data data = (Data) searcher.searchById(getModule().getId() + "onix");
		if( data == null)
		{
			data = searcher.createNewData();
			data.setValue("moduleid",getModule().getId());
			data.setId(getModule().getId() + "onix");
			data.setValue("entitytabrendertype","tabrenderonix");
			data.setName("ONIX");
			searcher.saveData(data);
		}
	}

	protected void fixMath(JsonNode inRootjson, Map<Integer, Integer> inRowcounts) 
	{
		Integer row = inRowcounts.get(inRootjson.getLevel());
		if(row == null)
		{
			row = 0;
		}
		row++;
		inRowcounts.put(inRootjson.getLevel(),row);
		inRootjson.setRow(row);
		for (Iterator iterator = inRootjson.getChildren().iterator(); iterator.hasNext();) 
		{
			JsonNode node = (JsonNode)iterator.next();
			fixMath(node,inRowcounts);
			node.setSourceId(inRootjson.getId());
		}
	}


	private void collectJson(Searcher inOnixSearcher, JsonNode inRootjson, List inCollectedjson) 
	{
		Data tosave = inOnixSearcher.createNewData();
		tosave.setName(inRootjson.getName());
		tosave.setValue("nodelevel",inRootjson.getLevel());
		tosave.setValue("json",inRootjson.getJson());
		tosave.setValue("entityid",getEntity().getId());
		inCollectedjson.add(tosave);
		for (Iterator iterator = inRootjson.getChildren().iterator(); iterator.hasNext();) 
		{
			JsonNode node = (JsonNode)iterator.next();
			collectJson(inOnixSearcher,node,inCollectedjson);
		}

	}


	protected void findRedundant(Element toplevel, JsonNode topnode, Map<String, JsonNode> multiplevalues) 
	{
		Set allnodes = new HashSet();
		
		for (Iterator iterator = toplevel.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			if( allnodes.contains(child.getName()) )  //Its a duplicates. Put it under a placeholder
			{
				JsonNode placeholdernode = multiplevalues.get(child.getName()); //Placeholder
				if( placeholdernode == null )
				{
					placeholdernode = new JsonNode();
				}
				placeholdernode.setLevel(2); //connector
				placeholdernode.setName(child.getName());
				if( !placeholdernode.getName().endsWith("s"))
				{
					placeholdernode.setName(placeholdernode.getName() + "s"); //Silly
				}
				multiplevalues.put(child.getName(), placeholdernode); //Only have one of these
				
				topnode.addChild(placeholdernode);
			}
			allnodes.add(child.getName());
		}
	}
	
	private JsonNode createJsonNodes(int level,Element inChildNode) {
		JsonNode datanode = new JsonNode();
		datanode.setName(inChildNode.getName());
		datanode.setLevel(level);
		datanode.setElement(inChildNode);
		
		for (Iterator iterator = inChildNode.elementIterator(); iterator.hasNext();) {
			Element child = (Element) iterator.next();
			JsonNode node = createJsonNodes(level + 1,child);
			datanode.addChild(node);
		}
		return datanode;
	}

	protected Page getType(JsonNode inParent)
	{
		String type = null;

		if( inParent.getLevel() == 1)
		{
			type = "root";
		}
		else if( inParent.getLevel() == 2 && inParent.getChildren() != null)
		{
			type ="connector";
		}
		if( type == null)
		{
			type = inParent.getName();
		}
		Page found = foundtypes.get(type);

		if( found == null && !type.equals("connector") && !type.equals("root") )
		{
			Data module = getMediaArchive().query("module").match("externalid",type).searchOne();
			if( module != null)
			{
				type = module.get("onixrender");
				if( type == null)
				{
					type= "entity";
				}
			}
			else
			{
				type ="component";
			}
		}
		String path = "/" + getContext().findPathValue("applicationid") + "/views/modules/" + getContext().findPathValue("searchtype") + "/components/renderonix/types/" + type + ".json";
		found = getPageManager().getPage(path);
		foundtypes.put(type,found);
		return found;
	}
	protected void renderJson(JsonNode inNode , Map<Integer,Integer> maxrowcounts,Map<Integer,Integer> rowsspent) 
	{	
		Page render = getType(inNode);
		Integer spent = rowsspent.get(inNode.getLevel());
		if( spent == null)
		{
			spent = 0;
		}
		spent++;
		inNode.setRowPosition(spent + 1);
		rowsspent.put(inNode.getLevel(),spent);
		String json = renderVelocity(render,inNode,maxrowcounts);
		inNode.setJson(json);

		
		List<JsonNode> children = inNode.getChildren();
		if( children != null && !children.isEmpty())
		{
			for (Iterator iterator = children.iterator(); iterator.hasNext();) {
				JsonNode jsonNode = (JsonNode) iterator.next();
				renderJson(jsonNode,maxrowcounts,rowsspent);
			}
		}
		else
		{
			//Create property?
			saveValueIsPossible(inNode);
			
		}
	}

	protected String renderVelocity(Page inRender, JsonNode inNode ,Map<Integer,Integer> rowcounts) 
	{
		if( !inRender.exists())
		{
			return null;
		}
		PageStreamer streamer = getContext().getPageStreamer().copy();
		
		Output out = new Output();
		StringWriter outtext = new StringWriter();

		out.setWriter(outtext);
		streamer.setOutput(out);

		WebPageRequest context = getContext().copy(inRender);
		context.putPageStreamer(streamer);
		context.putPageValue("node", inNode);
		context.putPageValue("rowcounts", rowcounts);
		
		int maxcount = 0;
		for (Iterator iterator = rowcounts.keySet().iterator(); iterator.hasNext();) {
			Integer level = (Integer) iterator.next();
			Integer count = rowcounts.get(level);
			if( count > maxcount)
			{
				maxcount = count; 
			}
		}
		
		context.putPageValue("maxcounts",maxcount);
		//Map params = context.getParameterMap();
		//Set keys = params.keySet();
		streamer.include(inRender, context);

		return outtext.toString();
	}

	protected PropertyDetail saveValueIsPossible(JsonNode inRoot) {
		
		if( multiplevalues.get(inRoot.getName()) != null )
		{
			return null;
		}
		
		String text = inRoot.getTextTrim();
		if( text != null)
		{
			//TODO: Make sure no children?
			PropertyDetail detail = getSearcher().getDetail(inRoot.getName());
			if( detail == null)
			{
				detail = new PropertyDetail();
				detail.setId(inRoot.getName());
				
				//See if its a list or not
				String path = "/" + getSearcher().getCatalogId() + "/lists/onix/" + inRoot.getName() + ".xml"; 
				boolean islist = getPageManager().getRepository().doesExist(text);
				if( islist)
				{
					detail.setDataType("list");
					detail.setListId("onix/" + inRoot.getName());
				}
				detail.setName(inRoot.getName());
				detail.setEditable(true);
				detail.setStored(true);
				detail.setIndex(true);
				detail.setKeyword(true);
				getSearcher().getPropertyDetailsArchive().savePropertyDetail(detail, getSearcher().getSearchType(), null);
			}
			//Add to the view for this panel
			String viewpath  = getModule().getId() + "/" + getModule().getId() + "general";
			View onixview = getSearcher().getPropertyDetailsArchive().getView(getModule().getId(), viewpath, null);
			if( onixview == null)
			{
				onixview = new View();
				onixview.setId(viewpath);
			}
			boolean findview = false;
			for (Iterator iterator = onixview.iterator(); iterator.hasNext();) {
				PropertyDetail found = (PropertyDetail) iterator.next();
				if( found.getId().equals(inRoot.getName()) )
				{
					findview = true;
					break;
				}
			}
			if( !findview)
			{
				onixview.add(detail);
				getSearcher().getPropertyDetailsArchive().saveView(onixview, null);
			}
			getEntity().setValue(inRoot.getName(), text);
			return detail;
		}
		return null;
	}

}
