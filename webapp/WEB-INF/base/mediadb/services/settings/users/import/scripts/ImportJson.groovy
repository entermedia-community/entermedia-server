import groovy.json.JsonSlurper

import org.openedit.data.Searcher

import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.page.Page


class JsonImporter extends EnterMediaObject
{
	protected Map<String,String> fieldLookUps;
	protected Searcher fieldSearcher;
	protected boolean fieldMakeId;
	
	public Searcher getSearcher()
	{
		return fieldSearcher;
	}
	public void importData() throws Exception
	{
		fieldSearcher = loadSearcher(context);
		
		String importpath = context.findValue("importpath");
		Page upload = getPageManager().getPage(importpath);
		
		JsonSlurper slurper = new JsonSlurper();
		Reader reader = upload.getReader();
		try
		{
			saveCategoryTree(reader, slurper);
		}
		finally
		{
			reader.close();
		}	   
	}
	protected void saveCategoryTree(Reader inStream, JsonSlurper slurper)
	{
		Map all = new HashMap();
		Map resultant = slurper.parse(inStream);
		List items = resultant.get("items");
		for( Map category in items)
		{
			all.put(category.get("id"),category);
		}
		
		for( Map category in items)
		{
			createTree(all,category);
		}
		getMediaArchive().getCategoryArchive().saveAll();
	}


	public org.openedit.entermedia.Category createTree(Map all, Map one)
	{
		String id = one.get("id");
		org.openedit.entermedia.Category child = getMediaArchive().getCategory(id);
		if( child == null)
		{
			child = getMediaArchive().getCategoryArchive().createNewCategory(id);
			child.setId(id);
			child.setName(one.get("name"));
			String parentid = one.get("parent_id");
			if( parentid != null)
			{
				Map parentmap = all.get(parentid);
				org.openedit.entermedia.Category parent = createTree(all, parentmap);
				parent.addChild(child);
			}
			else
			{
				getMediaArchive().getCategoryArchive().getRootCategory().addChild(child);
			}
			getMediaArchive().getCategoryArchive().cacheCategory(child);
		}
		return child;
	}
	
}

//Datamanager
JsonImporter importer = new JsonImporter();
importer.setModuleManager(moduleManager);
importer.setContext(context);
importer.importData();
