/*
 * Created on Oct 19, 2004
 */
package org.openedit.users;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.BaseGroup;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.users.filesystem.XmlUserArchive;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class LuceneGroupSearcher extends BaseLuceneSearcher implements
		GroupSearcher
{
	private static final Log log = LogFactory.getLog(LuceneGroupSearcher.class);

	protected XmlUserArchive fieldXmlUserArchive;

	public XmlUserArchive getXmlUserArchive() {
		if (fieldXmlUserArchive == null) {
			fieldXmlUserArchive = (XmlUserArchive) getModuleManager().getBean(
					getCatalogId(), "xmlUserArchive");

		}

		return fieldXmlUserArchive;
	}

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		return getXmlUserArchive().getGroups();
		// return new ListHitTracker().setList(getCustomerArchive().)
	}

	// public HitTracker findUserNoLucene(String inQuery, int maxNum) throws
	// UserManagerException
	// {
	// ListHitTracker tracker = new ListHitTracker();
	// SearchQuery q = new SearchQuery();
	// q.addMatches(inQuery);
	// tracker.setSearchQuery(q);
	//		
	//		
	//		
	// if ( inQuery == null || inQuery.equalsIgnoreCase("all") ||
	// inQuery.length() == 0)
	// {
	// for (Iterator iter = getUserManager().getUsers().getAllHits();
	// iter.hasNext() && tracker.getTotal() < maxNum;)
	// {
	// String username = (String) iter.next();
	// User user = getUserManager().getUser(username);
	// tracker.add(user);
	// }
	// return tracker;
	// }
	// inQuery = inQuery.toLowerCase();
	// for (Iterator iter = getUserManager().getUsers().getAllHits();
	// iter.hasNext();)
	// {
	// String username = (String) iter.next();
	// if( matches(username,inQuery) )
	// {
	// User user = getUserManager().getUser(username);
	// tracker.add(user);
	// }
	// else if( maxNum < 1001)
	// {
	// //check email
	// User user = getUserManager().getUser(username);
	// for (Iterator iterator = user.getProperties().values().iterator();
	// iterator.hasNext();)
	// {
	// String val = (String) iterator.next();
	// if( matches(val,inQuery ) )
	// {
	// tracker.add(user);
	// }
	// }
	//				
	// }
	// if ( tracker.getTotal() >= maxNum)
	// {
	// break;
	// }
	// }
	// return tracker;
	// }
	// protected boolean matches(String inText, String inQuery)
	// {
	// if ( inText != null)
	// {
	// if (PathUtilities.match(inText, inQuery) )
	// {
	// return true;
	// }
	// }
	// return false;
	// }

	public Object searchById(String inId) 
	{
		return getGroup(inId);
	}
	public void reIndexAll(IndexWriter writer, TaxonomyWriter inWriter)
	{
		log.info("Reindex of customer groups directory");
		try
		{
			//writer.setMergeFactor(50);
			// FIXME: Move this to XmlCustomerArchive, e.g. getAllUserNames()
			File groupsDirectory = new File(getRootDirectory(),
					"/WEB-INF/data/system/groups");
			if( !groupsDirectory.exists())
			{
				groupsDirectory = new File(getRootDirectory(),
				"/WEB-INF/groups");
			}
			
			File[] groupsxml = groupsDirectory.listFiles(new FilenameFilter() {
				public boolean accept(File inDir, String inName)
				{
					if (inName.endsWith(".xml"))
					{
						return true;
					}
					return false;
				}
			});
			if( groupsxml != null)
			{
				
				PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
				for (int i = 0; i < groupsxml.length; i++)
				{
					Document doc = new Document();
					File xconf = groupsxml[i];
					String groupid = PathUtilities.extractPageName(xconf.getPath());
	
					Group group = getXmlUserArchive().getGroup(groupid);
					updateIndex( group, doc, details);
					writer.addDocument(doc);
	
				}
			}
			//writer.optimize();
		} catch (Exception e)
		{
			// TODO Auto-generated catch block

			throw new OpenEditException(e);
		}

	}

	@Override
	public Data createNewData()
	{
		return new BaseGroup();
	}
	
	public Group getGroup(String inGroupId)
	{
		Group group = getXmlUserArchive().getGroup(inGroupId);
		if (group == null)
		{
			log.error("Index is out of date, group " + inGroupId
					+ " has since been deleted");
		} 
		return group;
	}

	public void saveData(Data inData, User inUser)
	{
		getXmlUserArchive().saveGroup((Group) inData);
		updateIndex((Group) inData);
	}
	
	

	public void deleteData(Data inData)
	{
		getXmlUserArchive().deleteGroup((Group)inData);
	}
	
}
