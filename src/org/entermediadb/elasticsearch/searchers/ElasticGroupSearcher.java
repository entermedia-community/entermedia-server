/*
 * Created on Oct 19, 2004
 */
package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetails;
import org.openedit.users.Group;
import org.openedit.users.GroupSearcher;
import org.openedit.users.User;
import org.openedit.users.filesystem.XmlUserArchive;

/**
 * @author cburkey
 * 
 */
public class ElasticGroupSearcher extends BaseElasticSearcher implements
		GroupSearcher
{
	private static final Log log = LogFactory.getLog(ElasticGroupSearcher.class);
	protected XmlUserArchive fieldXmlUserArchive;


	public XmlUserArchive getXmlUserArchive() {
		if (fieldXmlUserArchive == null) {
			fieldXmlUserArchive = (XmlUserArchive) getModuleManager().getBean(
					getCatalogId(), "xmlUserArchive");

		}

		return fieldXmlUserArchive;
	}
	
	
	public void reIndexAll()
	{
		log.info("Reindex of customer groups directory");
		try
		{
			
			putMappings();

		
			Collection ids = getXmlUserArchive().listGroupIds();
			if( ids != null)
			{
				List groups = new ArrayList();
				PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
				for (Iterator iterator = ids.iterator(); iterator.hasNext();)
				{
					String id = (String) iterator.next();
					Group group = (Group)createNewData(); 
					group.setId(id);
					group = getXmlUserArchive().loadGroup(group);
					if( group != null)
					{
						groups.add(group);
						if( groups.size() > 1000)
						{
							updateIndex(groups, null);
							groups.clear();
						}
					}	
				}
				updateIndex(groups, null);
			}
		} 
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}
	
	@Override
	public void reindexInternal() throws OpenEditException
	{
		reIndexAll();
	}

	
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		reIndexAll();
	}

	public Group getGroup(String inGroupId)
	{
		Group group = (Group)getCacheManager().get(getCatalogId() + "groupSearcher", inGroupId);
		if( group == null)
		{
			group = (Group)searchById(inGroupId);
			if (group == null)
			{
				group = (Group)createNewData(); 
				group.setId(inGroupId);
				group = getXmlUserArchive().loadGroup(group);
				if(group == null) {
				log.error("Index is out of date, group " + inGroupId
						+ " has since been deleted");
				} else {
					super.saveData(group); //update the index

				}
			}
			else
			{
				getCacheManager().put(getCatalogId() +  "groupSearcher", inGroupId, group);
			}
		}	
		return group;
	}

	public void saveData(Data inData, User inUser)
	{
		getXmlUserArchive().saveGroup((Group) inData);
	
		super.saveData(inData, inUser); //update the index

	}
	public void delete(Data inData, User inUser)
	{
		getXmlUserArchive().deleteGroup((Group) inData);
		super.delete(inData, inUser); //update the index
	}
	@Override
	public boolean initialize()
	{
		if( !tableExists())
		{
			reIndexAll();
			return true;
		}				
		return false;
	}

}
