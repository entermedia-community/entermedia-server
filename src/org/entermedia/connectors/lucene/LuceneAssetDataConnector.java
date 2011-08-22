package org.entermedia.connectors.lucene;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.openedit.Data;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.CompositeAnalyzer;
import org.openedit.data.lucene.NullAnalyzer;
import org.openedit.data.lucene.RecordLookUpAnalyzer;
import org.openedit.data.lucene.StemmerAnalyzer;
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
	protected LuceneAssetIndexer fieldIndexer;
	protected ModuleManager fieldModuleManager;
	protected CategoryArchive fieldCategoryArchive;
	protected MediaArchive fieldMediaArchive;
	protected IntCounter fieldIntCounter;

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
			CompositeAnalyzer composite = new CompositeAnalyzer();
			composite.setAnalyzer("description", new StemmerAnalyzer());
			composite.setAnalyzer("id", new NullAnalyzer());
			composite.setAnalyzer("foldersourcepath", new NullAnalyzer());
			composite.setAnalyzer("sourcepath", new NullAnalyzer());
			RecordLookUpAnalyzer record = new RecordLookUpAnalyzer();
			record.setUseTokens(false);
			composite.setAnalyzer("cumulusid", record);
			composite.setAnalyzer("name_sortable", record);
			fieldAnalyzer = composite;
		}
		return fieldAnalyzer;
	}

	protected LuceneAssetIndexer getIndexer()
	{
		if (fieldIndexer == null)
		{
			fieldIndexer = new LuceneAssetIndexer();
			fieldIndexer.setAnalyzer(getAnalyzer());
			fieldIndexer.setSearcherManager(getSearcherManager());
			fieldIndexer.setUsesSearchSecurity(true);
			fieldIndexer.setNumberUtils(getNumberUtils());
			fieldIndexer.setRootDirectory(getRootDirectory());
			fieldIndexer.setMediaArchive(getMediaArchive());
			if(getMediaArchive().getAssetSecurityArchive() == null)
			{
				log.error("Asset Security Archive Not Set");
			}
			fieldIndexer.setAssetSecurityArchive(getMediaArchive().getAssetSecurityArchive());
		}
		return fieldIndexer;
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
				getIndexer().populateAsset(getIndexWriter(), asset, false, details);
			}
			if (inOptimize)
			{
				getIndexWriter().optimize();
				log.info("Optimized");
			}

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

	protected void reIndexAll(IndexWriter writer)
	{
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		// writer.mergeFactor = 10;
		writer.setMergeFactor(100);
		writer.setMaxBufferedDocs(2000);

		try
		{
			IndexAllAssets reindexer = new IndexAllAssets();
			reindexer.setWriter(writer);
			reindexer.setPageManager(getPageManager());
			reindexer.setIndexer(getIndexer());
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
			writer.optimize();
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
		getAssetArchive().deleteAsset((Asset)inData);
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
			Query q = getQueryParser().parse("id:" + inId);
			getIndexWriter().deleteDocuments(q);
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
	}

	public void saveData(Data inData, User inUser)
	{
		if (inData instanceof Asset)
		{
			Asset asset = (Asset) inData;
			if( asset.getId() == null)
			{
				asset.setId(nextId());
			}
			getAssetArchive().saveAsset(asset);
			updateIndex(asset);
		}
		else if (inData instanceof CompositeData)
		{
			saveCompositeData((CompositeData) inData, inUser);
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
		getLiveSearcher(); //should flush the index
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

}
