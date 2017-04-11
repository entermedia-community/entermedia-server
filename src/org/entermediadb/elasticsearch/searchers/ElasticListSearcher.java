package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.data.DataArchive;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Reloadable;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.users.User;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlFile;
import org.openedit.xml.XmlSearcher;

public class ElasticListSearcher extends BaseElasticSearcher implements Reloadable
{
	protected Log log = LogFactory.getLog(ElasticListSearcher.class);
	protected DataArchive fieldDataArchive; //lazy loaded
	protected String fieldPrefix;
	protected String fieldDataFileName;
	protected XmlFile fieldXmlFile;
	protected XmlSearcher fieldXmlSearcher;

	
	
	public XmlSearcher getXmlSearcher() {
		
		if(fieldXmlSearcher.getCatalogId() == null){
			fieldXmlSearcher.setCatalogId(getCatalogId());
			fieldXmlSearcher.setSearchType(getSearchType());
			PropertyDetailsArchive newarchive = getSearcherManager().getPropertyDetailsArchive(getCatalogId());
			fieldXmlSearcher.setPropertyDetailsArchive(newarchive);
		}
		//fieldXmlSearcher.setCacheManager(null);//Important cb: Why in the world would you want always create new caches? 
		return fieldXmlSearcher;
	}

	public void setXmlSearcher(XmlSearcher inXmlSearcher) {
		fieldXmlSearcher = inXmlSearcher;
	}

	

	
	
	public String getDataFileName()
	{
		if (fieldDataFileName == null)
		{
			fieldDataFileName = getSearchType() + ".xml";
		}
		return fieldDataFileName;
	}
	public void setDataFileName(String inName)
	{
		fieldDataFileName = inName;
	}
	
	
	public Data createNewData()
	{
		if( fieldNewDataName == null)
		{
			ElementData data = new ElementData();
			
			return data;
		}
		return (Data)getModuleManager().getBean(getNewDataName());
	}

	@Override
	public void reindexInternal() throws OpenEditException
	{
		getXmlSearcher().clearIndex();
		HitTracker allhits = getXmlSearcher().getAllHits();
		allhits.enableBulkOperations();
		ArrayList tosave = new ArrayList();
		for (Iterator iterator2 = allhits.iterator(); iterator2.hasNext();)
		{
			Data hit = (Data) iterator2.next();
			Data real = (Data) loadData(hit);
			tosave.add(real);
			if(tosave.size() > 1000)
			{
				updateInBatch(tosave, null);
	
				tosave.clear();
			}
		}
		updateInBatch(tosave, null);
	}
	

	public synchronized void reIndexAll() throws OpenEditException
	{		
		//setReIndexing(false);
		if( isReIndexing())
		{
			return;
		}
		setReIndexing(true);
		try
		{
			//TODO: delete all before reindexing 
			
			
			//Someone is forcing a reindex
			//deleteOldMapping();
			putMappings(); 

			getXmlSearcher().clearIndex();
			HitTracker settings = getXmlSearcher().getAllHits();
			Collection toindex = new ArrayList();
			log.info("settings " + settings.size() + " " + getSearchType());
			for (Iterator iterator = settings.iterator(); iterator.hasNext();) 
			{
				ElementData data = (ElementData)iterator.next();					
				toindex.add(data); //loadData? nah
				//log.info(data.getName());
				if( toindex.size() > 1000)
				{
					updateIndex(toindex,null);
					toindex.clear();
				}
			}
			updateIndex(toindex,null);
			
			flushChanges();			
			
		}
		finally
		{
			setReIndexing(false);
		}
	}

	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		reIndexAll();
	}

	@Override
	public void reloadSettings()
	{
		super.reloadSettings();
		reIndexAll();
	} 

	@Override
	protected void deleteAll(Collection<Data> inBuffer, User inUser)
	{
		super.deleteAll(inBuffer, inUser);
		getXmlSearcher().deleteAll(inUser);

	}

	public void delete(Data inData, User inUser)
	{
		if(inData instanceof SearchHitData){
			inData = (Data) searchById(inData.getId());
		}
		if( inData == null ||  inData.getId() == null )
		{
			throw new OpenEditException("Cannot delete null data.");
		}
		Lock lock = getLockManager().lock(getSearchType() + "/" + inData.getSourcePath(),"admin");
		try
		{
			getXmlSearcher().delete(inData, inUser);
			super.delete(inData, inUser);
		}
		finally
		{
			getLockManager().release(lock);
		}
		// Remove from Index
	}

	

	//This is the main APU for saving and updates to the index
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
		
		for (Object object: inAll)
		{
			Data data = (Data)object;
			try
			{
				updateElasticIndex(details, data); //Cant use bulk operations because id wont be set
				getXmlSearcher().saveData(data, inUser);
			}
			catch(Throwable ex)
			{
				log.error("problem saving " + data.getId() , ex);
				throw new OpenEditException(ex);
			}
		}
	}

	public void saveData(Data inData, User inUser)
	{
		//update the index
		PropertyDetails details = getPropertyDetails();

		try
		{
			updateElasticIndex(details, inData);
			getXmlSearcher().saveData(inData, inUser);
			clearIndex();
		}
		catch(Throwable ex)
		{
			log.error("problem saving " + inData.getId() , ex);
			throw new OpenEditException(ex);
		}
	}
	
	
	public Object searchById(String inId)
	{
		//return getXmlSearcher().searchById(inId);
		return super.searchById(inId); //Dont ever read from XML, just write
	}
	
	
//	public PropertyDetails getPropertyDetails() {
//		return getXmlSearcher().getPropertyDetails();
//		
//	}
	
	@Override
	public boolean initialize()
	{
		if( !tableExists() || getCatalogId().equals("system"))
		{
			reIndexAll();
			return true;
		}				
		return false;
	}
	
	
	
	
}
