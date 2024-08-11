package org.entermediadb.asset.importer;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.View;
import org.openedit.page.Page;
import org.openedit.util.XmlUtil;

public class OnixImporter extends BaseImporter{

	
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
		Element toplevel = root.element("Product");
		for (Iterator iterator = toplevel.elements().iterator(); iterator.hasNext();) 
		{
			Element child = (Element) iterator.next();
			List children = child.elements();
			if( children.isEmpty())
			{
				saveFieldData(toplevel.getName() + "-" + child.getName(), child);
			}
			else
			{
				//One
			}
		}
		makeFields(null,root);

		Searcher searcher = getMediaArchive().getSearcher("view");
		Data data = (Data) searcher.searchById(getModule().getId() + "onix");
		if( data == null)
		{
			data = searcher.createNewData();
			data.setValue("moduleid",getModule().getId());
			data.setId(getModule().getId() + "onix");
			data.setName("ONIX");
			searcher.saveData(data);
		}
		getSearcher().saveData(getEntity());
	}

	protected void makeFields(String parentId, Element inRoot) 
	{	

		List<Element> children = inRoot.elements();
		if(children.isEmpty() )
		{
			//Look for content
			if( inRoot.hasContent())
			{
				saveFieldData(parentId, inRoot);
			}
		}
		else
		{
			
			View before = getSearcher().getPropertyDetailsArchive().getView(getModule().getId(), getModule().getId() + "/onix", null);
			for (Iterator iterator = children.iterator(); iterator.hasNext();) 
			{
				Element element = (Element) iterator.next();
				String detailid = null;
				if( parentId == null)
				{
					detailid = inRoot.getName();
				}
				else
				{
					detailid = parentId + "-" + inRoot.getName();
				}
				makeFields(detailid,element);
			}
			View after = getSearcher().getPropertyDetailsArchive().getView(getModule().getId(), getModule().getId() + "/onix", null);

			//Add content section to view here based on parent
			if( before != null && after != null && after.size() > before.size())
			{
				PropertyDetail newone = (PropertyDetail)after.get(before.size()); //Loop backwards?
				newone.setValue("header", "Next section here"); //TODO: Support laguage map
				getSearcher().getPropertyDetailsArchive().saveView(after, null);
			}

			
		}
		//
		
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
			String viewpath  = getModule().getId() + "/" + getModule().getId() + "onix";
			View onixview = getSearcher().getPropertyDetailsArchive().getView(getModule().getId(), viewpath, null);
			if( onixview == null)
			{
				onixview = new View();
				onixview.setId(viewpath);
			}
			boolean findview = false;
			for (Iterator iterator = onixview.iterator(); iterator.hasNext();) {
				PropertyDetail found = (PropertyDetail) iterator.next();
				if( found.equals(parentId) )
				{
					findview = true;
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
