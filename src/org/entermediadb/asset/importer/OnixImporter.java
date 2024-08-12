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
		
		Map<String,JsonNode> multiplevalues = new HashMap();
		defineDataFields(toplevel, datanode, multiplevalues);
		
		for (Iterator iterator = toplevel.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			JsonNode duplicate = multiplevalues.get(child.getName());
			JsonNode childdata = createJsonNodes(3,child);
			if( duplicate != null)
			{
				duplicate.addChild(childdata);
			}
			else
			{
				datanode.addChild(childdata);
			}
		}		
		
		//Now render allJSON recursively
		Map<Integer,Integer> rowcounts = new HashMap();
		fixMath(rootjson,rowcounts);
		renderJson(rootjson,rowcounts);
		
		
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


	protected void defineDataFields(Element toplevel, JsonNode datanode, Map<String, JsonNode> multiplevalues) 
	{
		Set allnodes = new HashSet();
		
		for (Iterator iterator = toplevel.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			if( allnodes.contains(child.getName()) )  //Its a duplicates. Put it under a placeholder
			{
				JsonNode placeholdernode = new JsonNode(); //Placeholder
				placeholdernode.setLevel(2); //connector
				placeholdernode.setName(child.getName());
				if( !placeholdernode.getName().endsWith("s"))
				{
					placeholdernode.setName(placeholdernode.getName() + "s"); //Silly
				}
				multiplevalues.put(child.getName(), placeholdernode); //To Store children
				datanode.addChild(placeholdernode);
			}
			allnodes.add(child.getName());
			List children = child.elements();
			if( children.isEmpty())  //Create properties
			{
				saveFieldData(toplevel.getName() + "-" + child.getName(), child); //These are nice to have on Detail Editor
			}
		}
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
		getSearcher().saveData(getEntity());
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
	protected void renderJson(JsonNode inParent , Map<Integer,Integer> rowcounts) 
	{	
		Page render = getType(inParent);
		String json = renderVelocity(render,inParent,rowcounts);
		inParent.setJson(json);
		
		List<JsonNode> children = inParent.getChildren();
		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			JsonNode jsonNode = (JsonNode) iterator.next();
			renderJson(jsonNode,rowcounts);
		}
		
	}

	protected String renderVelocity(Page inRender, JsonNode inParent ,Map<Integer,Integer> rowcounts) 
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
		context.putPageValue("node", inParent);
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

	protected void saveFieldData(String parentId, Element inRoot) {
		String text = inRoot.getTextTrim();
		if( text != null)
		{
		
			PropertyDetail detail = getSearcher().getDetail(parentId);
			if( detail == null)
			{
				detail = new PropertyDetail();
				detail.setId(parentId);
				detail.setDataType("list");
				detail.setListId("ONIX" + inRoot.getName());
				detail.setName(inRoot.getName());
				getSearcher().getPropertyDetailsArchive().savePropertyDetail(detail, getSearcher().getSearchType(), null);
			}
			else
			{
				if(  getEntity().getValue(parentId) != null)
				{
					//If there is already a field here then it might be a multi value situation
					//Create a table and delete the old detail
					//Also we need to add the views to the relationship view DB
				}
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
				if( found.getId().equals(parentId) )
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
			getEntity().setValue(parentId, text);
		}
	}

}
