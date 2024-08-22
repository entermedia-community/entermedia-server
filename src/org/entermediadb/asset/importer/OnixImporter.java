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
	Map<String,JsonNode> level2dupnodes = new HashMap();
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

		JsonNode metadatanode = new JsonNode();
		metadatanode.setLevel(2);
		metadatanode.setName("Metadata");
		rootjson.addChild(metadatanode);

		Element productelement = root.element("Product");
		level2dupnodes.clear();
		allnodes.clear();
		findRedundant(productelement, rootjson);
		
		defineDataView();

		addTopChildren(metadatanode, productelement);		

		//Now render allJSON recursively
		fixMath(rootjson);  //Levels shoudl be good?

		
		rootjson.setRowPosition(0);
		rootjson.setAlwaysRender(true);
		Page render = getType(rootjson);
		String json = renderVelocity(render,rootjson);
		rootjson.setJson(json);

		
		//Loop over top ones
		Map<Integer,Integer> rowspent= new HashMap();
		for (Iterator iterator = rootjson.getChildren().iterator(); iterator.hasNext();) {
			JsonNode toplevel = (JsonNode) iterator.next();
			toplevel.setAlwaysRender(true);
			toplevel.setTopLevelParent(toplevel.getId());
			renderJson(toplevel,rowspent);
		}
		
		//getSearcher().saveData(getEntity());
		getMediaArchive().saveData(getModule().getId(),getEntity());
		//Now collect all the nodes into a big list
		List collecteddatarows = new ArrayList();
		Searcher componentsearcher = getMediaArchive().getSearcher("componentdata");
		Collection hits = componentsearcher.query().exact("entityid", getEntity()).search();
		componentsearcher.deleteAll(hits,null);
		ordering=0;
		collectJson(componentsearcher,rootjson,collecteddatarows);
		//getEntity().setValue("onixjson",collectedjson);
		componentsearcher.saveAllData(collecteddatarows, null);
	}
	int ordering = 0;
	//Hard code levels. 1 is root 2 is metadata 3 are children 2 is placeholder for dups

	protected void addTopChildren(JsonNode metadatanode, Element productelement) {
		for (Iterator iterator = productelement.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			JsonNode level2dupholder = level2dupnodes.get(child.getName());
		
			//Need to make this recursive?
			if( level2dupholder == null)
			{
				JsonNode level3child = createJsonNodes(3,child);
				metadatanode.addChild(level3child);
			}
			else
			{
				JsonNode level3child = createJsonNodes(3,child);
				level2dupholder.addChild(level3child);
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

	protected void fixMath(JsonNode inRootjson) 
	{
		int row = 0;
		for (Iterator iterator = inRootjson.getChildren().iterator(); iterator.hasNext();) 
		{
			JsonNode node = (JsonNode)iterator.next();
			node.setRow(row++);
			fixMath(node);
		}
	}


	private void collectJson(Searcher inOnixSearcher, JsonNode inRootjson, List inCollectedjson) 
	{
		if( inRootjson.getJson() != null)
		{
			Data tosave = inOnixSearcher.createNewData();
			tosave.setName(inRootjson.getName());
			tosave.setValue("nodelevel",inRootjson.getLevel());
			tosave.setValue("json",inRootjson.getJson());
			tosave.setValue("alwaysrender",inRootjson.isAlwaysRender());
			tosave.setValue("toplevelparent",inRootjson.getTopLevelParent());
			tosave.setValue("entityid",getEntity().getId());
			tosave.setValue("ordering",ordering++);
			inCollectedjson.add(tosave);
		}
		for (Iterator iterator = inRootjson.getChildren().iterator(); iterator.hasNext();) 
		{
			JsonNode node = (JsonNode)iterator.next();
			collectJson(inOnixSearcher,node,inCollectedjson);
		}

	}

	Set allnodes = new HashSet();

	protected void findRedundant(Element productelement, JsonNode inRootNode) 
	{
		
		for (Iterator iterator = productelement.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			if( allnodes.contains(child.getName()) )  //Its a duplicates. Put it under a placeholder
			{
				JsonNode placeholdernode = level2dupnodes.get(child.getName()); //Placeholder
				if( placeholdernode == null )
				{
					placeholdernode = new JsonNode();
					placeholdernode.setLevel(2); //connector
					placeholdernode.setName(child.getName());
					if( !placeholdernode.getName().endsWith("s"))
					{
						placeholdernode.setName(placeholdernode.getName() + "s"); //Silly
					}
					level2dupnodes.put(child.getName(), placeholdernode); //Only have one of these
					inRootNode.addChild(placeholdernode);
				}
				
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
		else if( inParent.getLevel() == 2)
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
	protected void renderJson(JsonNode inNode ,Map<Integer,Integer> rowsspent) 
	{	
//		if( inNode.getLevel() > 3)
//		{
//			return;
//		}
		
		Page render = getType(inNode);
		Integer spent = rowsspent.get(inNode.getLevel());
		if( spent == null)
		{
			spent = 0;
		}
		spent++;
		inNode.setRowPosition(spent);
		rowsspent.put(inNode.getLevel(),spent);
		String json = renderVelocity(render,inNode);
		inNode.setJson(json);

		
		List<JsonNode> children = inNode.getChildren();
		if( children != null && !children.isEmpty())
		{
			Map<Integer,Integer> childrowspent= new HashMap();

			for (Iterator iterator = children.iterator(); iterator.hasNext();) {
				JsonNode jsonNode = (JsonNode) iterator.next();
				renderJson(jsonNode,childrowspent);
			}
		}
		else
		{
			//Create property?
			saveValueIsPossible(inNode);
			
		}
	}

	protected String renderVelocity(Page inRender, JsonNode inNode) 
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
	
		//Map params = context.getParameterMap();
		//Set keys = params.keySet();
		streamer.include(inRender, context);

		return outtext.toString();
	}

	protected PropertyDetail saveValueIsPossible(JsonNode inMetadata) {
		
		if( inMetadata.getLevel() != 3 |inMetadata.getChildren().isEmpty())
		{
			return null;
		}
		
		String text = inMetadata.getTextTrim();
		if( text != null)
		{
			//TODO: Make sure no children?
			PropertyDetail detail = getSearcher().getDetail(inMetadata.getName());
			if( detail == null)
			{
				detail = new PropertyDetail();
				detail.setId(inMetadata.getName());
				
				//See if its a list or not
				String path = "/" + getSearcher().getCatalogId() + "/lists/onix/" + inMetadata.getName() + ".xml"; 
				boolean islist = getPageManager().getRepository().doesExist(text);
				if( islist)
				{
					detail.setDataType("list");
					detail.setListId("onix/" + inMetadata.getName());
				}
				detail.setName(inMetadata.getName());
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
				if( found.getId().equals(inMetadata.getName()) )
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
			getEntity().setValue(inMetadata.getName(), text);
			return detail;
		}
		return null;
	}

}
