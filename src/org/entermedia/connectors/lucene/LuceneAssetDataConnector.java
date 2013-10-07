package org.entermedia.connectors.lucene;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Version;
import org.openedit.Data;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.FullTextAnalyzer;
import org.openedit.data.lucene.LuceneIndexer;
import org.openedit.data.lucene.NullAnalyzer;
import org.openedit.data.lucene.RecordLookUpAnalyzer;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.CategoryArchive;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.DataConnector;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.IntCounter;


public class LuceneAssetDataConnector extends BaseLuceneSearcher implements DataConnector
{
	static final Log log = LogFactory.getLog(LuceneAssetDataConnector.class);
	protected static final String CATALOGIDX = "catalogid";
	protected static final String CATEGORYID = "categoryid";
	protected DecimalFormat fieldDecimalFormatter;
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	protected CategoryArchive fieldCategoryArchive;
	protected MediaArchive fieldMediaArchive;
	protected IntCounter fieldIntCounter;
	protected Map fieldAssetPaths;

	public LuceneAssetDataConnector()
	{
		setFireEvents(true);
	}
	
	public Data createNewData()
	{
		Asset temp = new Asset();
		return temp;
		
	}
	public String nextId()
	{
		String countString = String.valueOf(getIntCounter().incrementCount());
		return countString;
	}
	public void updateIndex(Asset inAsset)
	{
		List all = new ArrayList(1);
		all.add(inAsset);
		updateIndex(all, false);
		clearIndex(); // Does not flush because it will flush if needed
		// anyways on a search

	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null)
		{
			Map analyzermap = new HashMap();
			//analyzermap.put("description",  new EnglishAnalyzer(Version.LUCENE_36));
			analyzermap.put("description",  new FullTextAnalyzer(Version.LUCENE_41));
			
			analyzermap.put("id", new NullAnalyzer());
			analyzermap.put("foldersourcepath", new NullAnalyzer());
			analyzermap.put("sourcepath", new NullAnalyzer());
			PerFieldAnalyzerWrapper composite = new PerFieldAnalyzerWrapper( new RecordLookUpAnalyzer() , analyzermap);

			fieldAnalyzer = composite;
		}
		return fieldAnalyzer;
	}

	public LuceneIndexer getLuceneIndexer()
	{
		if (fieldLuceneIndexer == null)
		{
			LuceneAssetIndexer luceneIndexer = new LuceneAssetIndexer();
			luceneIndexer.setAnalyzer(getAnalyzer());
			luceneIndexer.setSearcherManager(getSearcherManager());
			luceneIndexer.setUsesSearchSecurity(true);
			luceneIndexer.setNumberUtils(getNumberUtils());
			luceneIndexer.setRootDirectory(getRootDirectory());
			luceneIndexer.setMediaArchive(getMediaArchive());
			if(getMediaArchive().getAssetSecurityArchive() == null)
			{
				log.error("Asset Security Archive Not Set");
			}
			luceneIndexer.setAssetSecurityArchive(getMediaArchive().getAssetSecurityArchive());
			fieldLuceneIndexer = luceneIndexer;
		}
		return fieldLuceneIndexer;
	}

	public synchronized void updateIndex(List<Data> inAssets, boolean inOptimize)
	{
		if (log.isDebugEnabled())
		{
			log.debug("update index");
		}

		try
		{
			PropertyDetails details = getPropertyDetails();

			for (Iterator iter = inAssets.iterator(); iter.hasNext();)
			{
				Asset asset = (Asset) iter.next();
				IndexWriter writer = getIndexWriter();
				Document doc = getIndexer().populateAsset(writer, asset, false, details);
				
				updateFacets(doc,  getTaxonomyWriter());
				getIndexer().writeDoc(writer, asset.getId().toLowerCase() , doc, false);


			}
//			if (inOptimize)
//			{
//				getIndexWriter().optimize();
//				log.info("Optimized");
//			}
			if (inOptimize || inAssets.size() > 100)
			{
				flush();
			}
			else
			{
				clearIndex();
			}
			//else will be flushed next time someone searches. This is a key performance improvement for things like voting that need to be fast
			//BaseLuceneSearcher implements Shutdownable
		}
		catch (Exception ex)
		{
			clearIndex(); //try to recover
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}
	}

	protected LuceneAssetIndexer getIndexer()
	{
		return (LuceneAssetIndexer)getLuceneIndexer();
	}

	protected void reIndexAll(IndexWriter writer, TaxonomyWriter inTaxonomyWriter)
	{
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		// writer.mergeFactor = 10;
		// writer.setMergeFactor(100);
		// writer.setMaxBufferedDocs(2000);

		try
		{
			IndexAllAssets reindexer = new IndexAllAssets();
			reindexer.setWriter(writer);
			
			reindexer.setPageManager(getPageManager());
			reindexer.setIndexer(getIndexer());
			reindexer.setTaxonomyWriter(inTaxonomyWriter);
			reindexer.setMediaArchive(getMediaArchive());
			
			/* Search in the new path, if it exists */
			Page root = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/assets/");
			if( root.exists())
			{
				reindexer.setRootPath(root.getPath());
				reindexer.process();
			}
			
			/* Search in the old place */
//			reindexer.setRootPath("/" + getCatalogId() + "/assets/");
//			reindexer.process();
			
			log.info("Reindex completed on with " + reindexer.getExecCount() + " assets");
			//writer.optimize();
			inTaxonomyWriter.commit();
			writer.commit();

		}
		catch(Exception ex)
		{
			throw new OpenEditException(ex);
		}
		// HitCollector
	}


	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}
	
	public void deleteData(Data inData)
	{
		if(inData instanceof Asset){
			getAssetArchive().deleteAsset((Asset)inData);
		} else{
			
			Asset asset = (Asset) searchById(inData.getId());
			if(asset != null){
				getAssetArchive().deleteAsset(asset);

			}
		}
	}
	
	public void deleteFromIndex(Asset inAsset)
	{
		deleteFromIndex(inAsset.getId());
	}

	public void deleteFromIndex(String inId)
	{
		// TODO Auto-generated method stub
		log.info("delete from index " + inId);

		try
		{
			//Query q = getQueryParser().parse("id:" + inId);
			Term term = new Term("id", inId);
			getIndexWriter().deleteDocuments(term);
			//getIndexWriter().deleteDocuments(q);
			clearIndex();
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public void deleteFromIndex(HitTracker inOld)
	{
		if (inOld.size() == 0)
		{
			return;
		}
		Term[] all = new Term[inOld.getTotal()];
		for (int i = 0; i < all.length; i++)
		{
			Object hit = (Object) inOld.get(i);
			String id = inOld.getValue(hit, "id");
			Term term = new Term("id", id);
			all[i] = term;
		}
		try
		{
			getIndexWriter().deleteDocuments(all);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		clearIndex();
	}

	public void saveData(Data inData, User inUser)
	{
		if (inData instanceof CompositeData)
		{
			saveCompositeData((CompositeData) inData, inUser);
		}
		else if (inData instanceof Asset)
		{
			Asset asset = (Asset) inData;
			if( asset.getId() == null)
			{
				asset.setId(nextId());
			}
			getAssetArchive().saveAsset(asset);
			getCacheManager().put(getIndexPath(),asset.getId(),asset);
			updateIndex(asset);
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public ModuleManager getModuleManager()
	{
		return getSearcherManager().getModuleManager();
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public CategoryArchive getCategoryArchive()
	{
		if (fieldCategoryArchive == null)
		{
			fieldCategoryArchive = (CategoryArchive)getModuleManager().getBean(getCatalogId(),"categoryArchive");
		}
		return fieldCategoryArchive;
	}

	public void setCategoryArchive(CategoryArchive inCategoryArchive)
	{
		fieldCategoryArchive = inCategoryArchive;
	}

	public AssetArchive getAssetArchive()
	{
		return getMediaArchive().getAssetArchive();
	}

	public void saveAllData(Collection inAll, User inUser)
	{
		//check that all have ids
		for (Object object: inAll)
		{
			Data data = (Data)object;
			if(data.getId() == null)
			{
				data.setId(nextId());
			}
		}
		getAssetArchive().saveAllData(inAll, inUser);
		updateIndex(inAll);
	}

	public IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();
			fieldIntCounter.setLabelName("assetIdCount");
			Page prop = getPageManager().getPage("/WEB-INF/data/" + getMediaArchive().getCatalogHome()+ "/assets/idcounter.properties");
			File file = new File(prop.getContentItem().getAbsolutePath());
			fieldIntCounter.setCounterFile(file);
		}
		return fieldIntCounter;
	}

	public void setIntCounter(IntCounter inIntCounter)
	{
		fieldIntCounter = inIntCounter;
	}
	public Object searchByField(String inField, String inValue)
	{
		if( inField.equals("id") || inField.equals("_id"))
		{
			Data data = (Data)getCacheManager().get(getIndexPath(), inValue);
			if( data == null)
			{
				data = (Data)super.searchByField(inField, inValue);
				if( data == null)
				{
					return null;
				}
			}
			if( data != null && !(data instanceof Asset) )
			{
				data = getAssetArchive().getAssetBySourcePath(data.getSourcePath());
				if( data != null)
				{
					getCacheManager().put(getIndexPath(), inValue, data);
				}
			}
			return data;
		}
		return super.searchByField(inField, inValue);
	}

//	public String idToPath(String inAssetId)
//	{
//		String path = (String) getAssetPaths().get(inAssetId);
//		if (path == null && inAssetId != null)
//		{
//			SearchQuery query = createSearchQuery();
//			query.addExact("id", inAssetId);
//
//			HitTracker hits = search(query);
//			if (hits.size() > 0)
//			{
//				Data hit = hits.get(0);
//				path = hit.getSourcePath();
//				//mem leak? Will this hold the entire DB?
//				getAssetPaths().put(inAssetId, path);
//			}
//			else
//			{
//				log.info("No such asset in index: " + inAssetId);
//			}
//		}
//		return path;
//	}

//	public Map getAssetPaths()
//	{
//		if (fieldAssetPaths == null)
//		{
//			fieldAssetPaths = new HashMap();
//		}
//		return fieldAssetPaths;
//	}

}
