package org.entermediadb.elasticsearch.searchers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseAsset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.search.AssetSecurityArchive;
import org.entermediadb.asset.search.DataConnector;
import org.entermediadb.data.DataArchive;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetails;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.IntCounter;
import org.openedit.util.PathUtilities;

public class ElasticAssetDataConnector extends ElasticXmlFileSearcher implements DataConnector
{
	protected AssetSecurityArchive fieldAssetSecurityArchive;
	protected MediaArchive fieldMediaArchive;
	protected IntCounter fieldIntCounter;

	public Data createNewData()
	{
		return new BaseAsset(getMediaArchive());
	}

	protected DataArchive getDataArchive()
	{
		if (fieldDataArchive == null)
		{
			DataArchive archive = (DataArchive) getModuleManager().getBean(getCatalogId(), "assetDataArchive");
			archive.setDataFileName(getDataFileName());
			archive.setElementName(getSearchType());
			archive.setPathToData(getPathToData());
			fieldDataArchive = archive;
		}

		return fieldDataArchive;
	}

	public void deleteFromIndex(String inId)
	{
		// TODO Auto-generated method stub
		DeleteRequestBuilder delete = getClient().prepareDelete(toId(getCatalogId()), getSearchType(), inId);
		delete.setRefresh(true).execute().actionGet();
		// delete()
	}

	public void deleteFromIndex(HitTracker inOld)
	{
		for (Iterator iterator = inOld.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			deleteFromIndex(hit.getId());
		}

	}

	public void reIndexAll() throws OpenEditException
	{
		try
		{
			setOptimizeReindex(false);
			putMappings();
			getMediaArchive().getCategorySearcher().clearCategories();
			super.reindexInternal();
			log.info(getSearchType() + " Reindex complete");
		}
		finally
		{
			setOptimizeReindex(true);
		}
	}
//	public void reIndexAll() throws OpenEditException
//	{
//		if (isReIndexing())
//		{
//			return; //TODO: Make a lock so that two servers startin up dont conflict?
//		}
//		setReIndexing(true);
//		try
//		{
//			getMediaArchive().getAssetArchive().clearAssets();
//			//For now just add things to the index. It never deletes
//
//			//Someone is forcing a reindex
//			//deleteOldMapping();
//			putMappings();
//
//			//this is for legacy support
//			final List tosave = new ArrayList(500);
//
//			PathProcessor processor = new PathProcessor()
//			{
//				public void processFile(ContentItem inContent, User inUser)
//				{
//					if (!inContent.getName().equals(getDataFileName()))
//					{
//						return;
//					}
//					String sourcepath = inContent.getPath();
//					sourcepath = sourcepath.substring(getPathToData().length() + 1, sourcepath.length() - getDataFileName().length() - 1);
//					Asset asset = getMediaArchive().getAssetArchive().getAssetBySourcePath(sourcepath);
//					tosave.add(asset);
//					if (tosave.size() == 500)
//					{
//						updateIndex(tosave, null);
//						log.info("reindexed " + getExecCount());
//						tosave.clear();
//					}
//					incrementCount();
//				}
//			};
//			processor.setRecursive(true);
//			processor.setRootPath(getPathToData());
//			processor.setPageManager(getPageManager());
//			processor.setIncludeMatches("*.xml");
//			processor.process();
//			updateIndex(tosave, null);
//			log.info("reindexed " + processor.getExecCount());
//			flushChanges();
//			
//			//super.reIndexAll();//Old elastic data
//			
//		}
//		catch (Exception e)
//		{
//			throw new OpenEditException(e);
//		}
//		finally
//		{
//			setReIndexing(false);
//		}
//	}

	/**
	 * @deprecated Need to simplify
	 */
	public void updateIndex(Data one)
	{
		List all = new ArrayList(1);
		all.add(one);
		updateIndex(all, null);
	}
	
	public boolean shoudSkipField(String inKey)
	{
		if(inKey.equals("category-exact") || inKey.equals("category") || inKey.equals("description") || inKey.equals("foldersourcepath"))
		{
			return true;
		}
		return super.shoudSkipField(inKey);
	}
	
	@Override
	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails,User inUser)
	{
		try
		{
			if( isOptimizeReindex() && !(inData instanceof Asset)) //Low level performance fix
			{
				MultiValued values = (MultiValued)inData;
				saveArray(inContent, "category",values.getValues("category"));
				saveArray(inContent, "category-exact",values.getValues("category-exact"));
				String desc = values.get("description");
				inContent.field("description", desc);
				setFolderPath(inData, inContent);
				super.updateIndex(inContent, inData, inDetails,inUser);
			
				return;
			}
			Asset asset = (Asset) loadData(inData);
		
			String fileformat = asset.getFileFormat();
			if (fileformat != null)
			{
				inContent.field("fileformat", fileformat);
			}
			Object folderval = asset.getValue("isfolder");
			if(folderval == null){
				ContentItem item = getMediaArchive().getAssetManager().getOriginalContent(asset);
				asset.setFolder(item.isFolder());				
			}
//			if (asset.getCatalogId() == null)
//			{
//				asset.setCatalogId(getCatalogId());
//			}
			//inContent.field("catalogid", asset.getCatalogId());

			Set categories = asset.buildCategorySet();
			String desc = populateDescription(asset, inDetails, categories);
			categories.add(getMediaArchive().getCategorySearcher().getRootCategory());
			inContent.field("description", desc);

			saveArray(inContent, "category",categories);

			// Searcher searcher =
			// getSearcherManager().getSearcher(asset.getCatalogId(),"assetalbums");
			// SearchQuery query = searcher.createSearchQuery();
			// query.addMatches("assetid", asset.getId());
			// HitTracker tracker = searcher.search(query);
			// populateJoinData("album", doc, tracker, "albumid", true);

			// populateSecurity(doc, asset, catalogs);
			assignCategoryPermissions(categories, asset);
			super.updateIndex(inContent, inData, inDetails,inUser);
			// for (Iterator iterator =
			// inDetails.findIndexProperties().iterator(); iterator.hasNext();)
			// {
			// PropertyDetail detail = (PropertyDetail)iterator.next();
			// String value = inData.get(detail.getId());
			// if( value != null)
			// {
			// //TODO: Deal with data types and move to indexer object
			// inContent.field(detail.getId(),value);
			// //log.info("Saved" + detail.getId() + "=" + value );
			// }
			// }

			//This is for saving and loading.
			saveArray(inContent, "category-exact",asset.getCategories());
			//populatePermission(inContent, asset, "viewasset");
			setFolderPath(asset, inContent);
			
			
			
			
			

		}
		catch (Exception ex)
		{
			if( ex instanceof OpenEditException)
			{
				throw (OpenEditException)ex;
			}
			throw new OpenEditException(ex);
		}
	}

	protected void assignCategoryPermissions(Set inCategories, Asset inAsset) {
	    HashSet viewusers = new HashSet();
	    HashSet viewgroups = new HashSet();
	    HashSet viewroles = new HashSet();

	    for (Iterator iterator = inAsset.getCategories().iterator(); iterator.hasNext();) {
	        Category cat = (Category) iterator.next();

	        // Use the ternary operator to check for null and addAll only if not null
	        viewusers.addAll(cat.findValues("viewusers") != null ? cat.findValues("viewusers") : Collections.emptySet());
	        viewgroups.addAll(cat.findValues("viewgroups") != null ? cat.findValues("viewgroups") : Collections.emptySet());
	        viewroles.addAll(cat.findValues("viewroles") != null ? cat.findValues("viewroles") : Collections.emptySet());
	    }

	    inAsset.setValue("viewusers", viewusers);
	    inAsset.setValue("viewgroups", viewgroups);
	    inAsset.setValue("viewroles", viewroles);
	}

	

	protected void setFolderPath(Data asset, XContentBuilder inContent) throws IOException
	{
		String archivesourcepath = asset.get("archivesourcepath"); 
		if( archivesourcepath == null)
		{
			archivesourcepath = asset.getSourcePath();
		}
		String foldersourcepath = PathUtilities.extractDirectoryPath(archivesourcepath);
		inContent.field("foldersourcepath", foldersourcepath);				
	}

	protected void saveArray(XContentBuilder inContent, String inType, Collection inData) throws IOException
	{
		if( inData == null)
		{
			return;
		}
		List ids = new ArrayList(inData.size());
		for (Iterator iterator = inData.iterator(); iterator.hasNext();)
		{
			Object object = iterator.next();
			String id = null;
			if( object instanceof Data)
			{
				id = ((Data)object).getId();
			}
			else
			{
				id = String.valueOf( object );
			}
			ids.add(id);
		}
		if( ids.size() > 0)
		{
			String[] array = new String[ids.size()];
			Object oa = ids.toArray(array);
			inContent.field(inType, oa);
		}
	}

	/*
	 * protected void hydrateData(ContentItem inContent, String sourcepath, List
	 * buffer) { Asset data =
	 * getMediaArchive().getAssetBySourcePath(sourcepath); if (data == null) {
	 * return; } buffer.add(data); if (buffer.size() > 99) { updateIndex(buffer,
	 * null); } }
	 */
	protected void populatePermission(XContentBuilder inContent, Asset inAsset, String inPermission) throws IOException
	{
		List add = getAssetSecurityArchive().getAccessList(getMediaArchive(), inAsset);
		if (add.size() == 0)
		{
			add.add("true");
		}
		inContent.array(inPermission, add.toArray());

	}
	//TODO: Migrate this into populateKeywords
	protected String populateDescription(Asset asset, PropertyDetails inDetails, Set inCategories)
	{
		// Low level reading in of text
		StringBuffer fullDesc = new StringBuffer();
		// fullDesc.append(asset.getName());
		// fullDesc.append(' ');
		//
		// fullDesc.append(asset.getFileFormat());
		// fullDesc.append(' ');

		// fullDesc.append(asset.getId());
		// fullDesc.append(' ');

		String keywords = asTokens(asset.getKeywords());
		fullDesc.append(keywords);
		fullDesc.append(' ');

		populateKeywords(fullDesc, asset, inDetails);
		// add a bunch of stuff to the full text field
//		for (Iterator iter = inCategories.iterator(); iter.hasNext();)
//		{
//			Category cat = (Category) iter.next();
//			fullDesc.append(cat.getName());
//			fullDesc.append(' ');
//		}
		if (asset.getSourcePath() != null)
		{
			String[] dirs = asset.getSourcePath().split("/");

			for (int i = 0; i < dirs.length; i++)
			{
				fullDesc.append(dirs[i]);
				fullDesc.append(' ');
			}
			populateFullText(asset,getSearchType(),fullDesc);
		}
//		if (fullDesc.length() > getFullTextCap())
//		{
//			return fullDesc.substring(0, getFullTextCap());
//		}

		String result = fullDesc.toString();// fixInvalidCharacters(fullDesc.toString());
		return result;
	}

	protected String asTokens(Collection inList)
	{
		StringBuffer buffer = new StringBuffer();
		for (Iterator iter = inList.iterator(); iter.hasNext();)
		{
			String desc = (String) iter.next();
			buffer.append(desc);
			if (iter.hasNext())
			{
				buffer.append(' ');
			}
		}
		return buffer.toString();
	}


	//	/**
	//	 * @deprecated Need to simplify
	//	 */
	//	public void updateIndex(Collection<Data> all, boolean b)
	//	{
	//		updateIndex(all, null);
	//	}

	public AssetSecurityArchive getAssetSecurityArchive()
	{
		return fieldAssetSecurityArchive;
	}

	public void setAssetSecurityArchive(AssetSecurityArchive inAssetSecurityArchive)
	{
		fieldAssetSecurityArchive = inAssetSecurityArchive;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}

	public void flush()
	{
	}

	public void setRootDirectory(File inRoot)
	{
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = super.getIntCounter();
			fieldIntCounter.setLabelName(getSearchType() + "IdCount");
		}
		return fieldIntCounter;
	}

	public synchronized String nextId()
	{
		Lock lock = getLockManager().lock(loadCounterPath(), "admin");
		try
		{
			return String.valueOf(getIntCounter().incrementCount());
		}
		finally
		{
			getLockManager().release(lock);
		}
	}

	public Object searchByField(String inField, String inValue)
	{
		if (inField.equals("id") || inField.equals("_id"))
		{
			GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
			if (!response.isExists())
			{
				return null;
			}
			Map source = response.getSource();
			if( isDeleted(source))
			{
				return null;
			}
			Asset asset = createAssetFromResponse(response.getId(), source);
			return asset;
			// String path = (String)response.getSource().get("sourcepath");

			// return getAssetArchive().getAssetBySourcePath(path);
		}
		if (inField.equals("sourcepath") || inField.equals("_sourcepath"))
		{

			SearchRequestBuilder search = getClient().prepareSearch(toId(getCatalogId()));

			search.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			search.setTypes(getSearchType());

			QueryBuilder b = QueryBuilders.matchQuery("sourcepath", inValue);
			search.setQuery(b);
			SearchResponse response = search.execute().actionGet();
			Iterator<SearchHit> responseiter = response.getHits().iterator();
			if (responseiter.hasNext())
			{
				SearchHit hit = responseiter.next();
				return createAssetFromResponse(hit.getId(), hit.getSource());

			}
			return null;
		}
		return super.searchByField(inField, inValue);
	}

	protected Asset createAssetFromResponse(String inId, Map inSource)
	{
		BaseAsset asset = (BaseAsset) createNewData();
		
		if (inSource == null)
		{
			return null;
		}
		asset.setSearchData(inSource);
		asset.setId(inId);
		
		
		return asset;
	}

	//TODO: Make an ElasticAsset bean type that can be searched and saved
	@Override
	public Data loadData(Data inHit)
	{
		if(inHit == null){
			return null;
		}
		if (inHit instanceof Asset)
		{
			return inHit;
		}
		//Stuff might get out of date?
		SearchHitData db = (SearchHitData) inHit;
		return createAssetFromResponse(inHit.getId(), db.getSearchData());
	}

	public Data loadData(WebPageRequest inReq,String dataid)
	{
		Data data = null;
		
		if (dataid.startsWith("multiedit"))
		{
			CompositeData compositeasset = (CompositeData) inReq.getSessionValue(dataid);
			String hitssessionid = dataid.substring("multiedit".length() + 1);
			HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (compositeasset!= null && !compositeasset.getSelectedResults().hasChanged(hits)) 
			{
				data = compositeasset;
			}

			if (data == null)
			{
				if (hits == null)
				{
					log.error("Could not find " + hitssessionid);
					return null;
				}
				CompositeData composite = new CompositeAsset(this,getEventManager(), hits);
				composite.setId(dataid);
				data = composite;
				inReq.putSessionValue(dataid, data);
			}
		}
		else
		{
			data = loadData(dataid);
		}
		return data;

	}
	
	public Data getDataBySourcePath(String inSourcePath)
	{
		if (inSourcePath.endsWith("/"))
		{
			inSourcePath = inSourcePath.substring(0, inSourcePath.length() - 1);
		}
		return (Data) query().or().exact("sourcepath",  inSourcePath).exact("archivesourcepath",  inSourcePath).searchOne();
	}

	public Data getDataBySourcePath(String inSourcePath, boolean inAutocreate)
	{
		if(inSourcePath.startsWith("/")){
			inSourcePath = inSourcePath.substring(1, inSourcePath.length());
		}
		return (Data) searchByField("sourcepath", inSourcePath);

	}

	public String getPathToData()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/assets";
	}

	@Override
	public void reindexInternal() throws OpenEditException
	{
		log.info("Asset Reindex started optimized:" + isOptimizeReindex());
		super.reindexInternal();
	}

}
