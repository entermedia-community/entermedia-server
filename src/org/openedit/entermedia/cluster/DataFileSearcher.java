package org.openedit.entermedia.cluster;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.manage.PageManager;
import com.openedit.util.PathUtilities;

public class DataFileSearcher extends BaseLuceneSearcher
{
	protected PageManager fieldPageManager;

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
	public Data createNewData()
	{
		return new BaseData();
	}
	
	public void reIndexAll(IndexWriter inWriter)
	{
		//loop over all the files
		//super.reIndexAll();
		ContentItem root = getPageManager().getRepository().getStub("/WEB-INF/data/" + getCatalogId());
		addFolderToIndex(root,inWriter);
	}
	
	protected void addFolderToIndex(ContentItem inRoot, IndexWriter inWriter)
	{
		List childrenItems = listChildren(inRoot.getPath());

		List tosavelist = new ArrayList();
		for (Iterator iterator = childrenItems.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			ContentItem item = getPageManager().getRepository().getStub(path);
			Data tosave = createNewData(item);
			tosavelist.add(tosave);
			if( item.isFolder())
			{
				addFolderToIndex(item, inWriter);
			}
		}
		updateIndex(inWriter,tosavelist);
	}
	protected List listChildren(String inPath)
	{
		List children = getPageManager().getChildrenPaths(inPath);
		List copy = new ArrayList(children);
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			if( path.endsWith("/CVS") || path.endsWith("/index") || path.endsWith("/.versions"))
			{
				copy.remove(path);
			}
		}
		return copy;
	}
	public void updateFileList()
	{
		//loop over all the files in the datadirectory
		//do one folder at a time
		ContentItem root = getPageManager().getRepository().getStub("/WEB-INF/data/" + getCatalogId());
		
		List todelete = new ArrayList();
		List toadd = new ArrayList();
		List tosave = new ArrayList();

		processFileList(root, todelete, toadd, tosave);

		saveChanges(todelete,toadd, tosave);
	}
	protected void processFileList(ContentItem inRoot, List inToDelete, List inToAdd, List inToSave)
	{
		List childrenItems = listChildren(inRoot.getPath());
		Map lookup = new HashMap(childrenItems.size());
		for (Iterator iterator = childrenItems.iterator(); iterator.hasNext();)
		{
			String path = (String)iterator.next(); 
			ContentItem item = getPageManager().getRepository().getStub(path);
			lookup.put(item.getPath(), item);
		}
		processFileList(inRoot,lookup, inToDelete, inToAdd, inToSave);
	}	
	protected void processFileList(ContentItem inRoot,Map inChildrenItems, List inToDelete, List inToAdd, List inToSave)
	{
		SearchQuery query = createSearchQuery();
		query.addExact("folder",inRoot.getId());
		//query.addSortBy("folderDown");
		HitTracker tracker = search(query);

	//	Collection allfiles = new ArrayList(inChildrenItems.values());
		mergeChanges(tracker, inRoot, inChildrenItems, inToDelete, inToAdd, inToSave);

		if( inToDelete.size() > 1000 || inToAdd.size() > 1000 || inToSave.size() > 1000)
		{
			saveChanges(inToDelete,inToAdd, inToSave);
		}
		for (Iterator iterator = inChildrenItems.values().iterator(); iterator.hasNext();)
		{
			ContentItem child = (ContentItem) iterator.next();
			if( child.isFolder())
			{
				processFileList( child, inToDelete, inToAdd, inToSave);
			}
		}
	}
	protected void saveChanges(List inToDelete, List inToAdd, List inToEdit)
	{
		List tosave = new ArrayList();
		tosave.addAll(inToDelete);
		tosave.addAll(inToAdd);
		tosave.addAll(inToEdit);
		clearIndex();
		//getLiveSearcher();
		updateIndex(tosave);
	}

	protected void mergeChanges(HitTracker inPreviousHits, ContentItem inRoot, Map inActualItems, List<Data> inToDelete, List<Data> inToAdd, List<Data> inToEdit)
	{
		List<ContentItem> remaining = new ArrayList<ContentItem>(inActualItems.values());
		for (Iterator iterator = inPreviousHits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			String path = hit.get("path");
			ContentItem item = (ContentItem)inActualItems.get(path); 
			if( item == null )
			{
				Data todelete = createNewData();
				String  id = PathUtilities.makeId(path);
				todelete.setId(id);
				todelete.setProperty("path",path);
				todelete.setProperty("folder",inRoot.getId());
				todelete.setProperty("status", "deleted");
				//TODO: Look in .versions for delete record timestamp
				todelete.setProperty("lastmodified", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				inToDelete.add(hit);
			}
			else
			{
				//did it change?
				if( !item.isFolder())
				{
					Date lastmod = inPreviousHits.getDateValue(hit, "lastmodified");
					String length = hit.get("length");
					long flastmod = item.getLastModified();
					String flength = String.valueOf( item.getLength() );
					if( lastmod.getTime() != flastmod || !length.equals(flength ) )
					{
						Data modified = createNewData(item);
						inToEdit.add(modified);
					}
				}
				remaining.remove(item);
			}
		}
		//anything left in children needs to be added
		for (Iterator iterator = remaining.iterator(); iterator.hasNext();)
		{
			ContentItem item = (ContentItem) iterator.next();
			inToAdd.add(createNewData(item));
		}
	}
	protected Data createNewData(ContentItem inItem)
	{
		Data data = createNewData();
		data.setId( inItem.getId());
		String folder = inItem.getPath();
		folder = PathUtilities.extractDirectoryPath(folder); //go up a level
		data.setProperty("folder", PathUtilities.makeId(folder) );
		data.setProperty("path", inItem.getPath());
		String mod = DateStorageUtil.getStorageUtil().formatForStorage(inItem.lastModified());
		data.setProperty("lastmodified", mod);
		data.setProperty("length", String.valueOf( inItem.getLength()) );
		return data;
	}

}
