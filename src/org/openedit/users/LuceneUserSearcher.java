/*
 * Created on Oct 19, 2004
 */
package org.openedit.users;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.BaseUser;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.filesystem.XmlUserArchive;

/**
 *
 */
public class LuceneUserSearcher extends BaseLuceneSearcher implements
		UserSearcher {
	private static final Log log = LogFactory.getLog(LuceneUserSearcher.class);
	protected XmlUserArchive fieldXmlUserArchive;

	public XmlUserArchive getXmlUserArchive() {
		if (fieldXmlUserArchive == null) {
			fieldXmlUserArchive = (XmlUserArchive) getModuleManager().getBean(
					getCatalogId(), "xmlUserArchive");

		}

		return fieldXmlUserArchive;
	}

	//
	// public void setXmlUserArchive(XmlUserArchive inXmlUserArchive) {
	// fieldXmlUserArchive = inXmlUserArchive;
	// }

	public void reIndexAll(IndexWriter writer, TaxonomyWriter inWriter)
			throws OpenEditException {

		// setUserCatalogId(null);
		// String catid = getUserCatalogId();
		log.info("Reindex of customer users directory");
		try {
			// writer.setMergeFactor(50);
			getXmlUserArchive().flush();
			PropertyDetails details = getPropertyDetailsArchive()
					.getPropertyDetails(getSearchType());
			Collection usernames = getXmlUserArchive().listUserNames(
					);
			if (usernames != null) {
				for (Iterator iterator = usernames.iterator(); iterator
						.hasNext();) {
					String userid = (String) iterator.next();
					Document doc = new Document();
					User data = getXmlUserArchive().getUser(userid);
					if (data != null) {
						updateIndex(data, doc, details);
						writer.addDocument(doc);
					}

				}

			}
			// writer.optimize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new OpenEditException(e);
		}

	}

	protected void updateIndex(Data inData, Document doc,
			PropertyDetails inDetails) {
		User user = (User) inData;
		doc.add(new Field("enabled", Boolean.toString(user.isEnabled()),
				Field.Store.YES, Field.Index.ANALYZED));
		StringBuffer groups = new StringBuffer();
		for (Iterator iterator = user.getGroups().iterator(); iterator
				.hasNext();) {
			Group group = (Group) iterator.next();
			groups.append(group.getId());
			if (iterator.hasNext()) {
				groups.append(" | ");
			}
		}
		if (groups.length() > 0) {
			doc.add(new Field("groups", groups.toString(), Field.Store.NO,
					Field.Index.ANALYZED));
		}

		super.updateIndex(inData, doc, inDetails);
	}

	public void saveData(Data inData, User inUser) {
		if (inData instanceof User) {
			getXmlUserArchive().saveUser((User) inData);
		}
		updateIndex((User) inData);
	}

	// TODO: Replace with search?
	public Object searchById(String inId) {
		return getXmlUserArchive().getUser(inId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openedit.users.UserSearcherI#getUser(java.lang.String)
	 */
	public User getUser(String inAccount) {
		User user = (User) searchById(inAccount);
		return user;
	}

	/**
	 * @deprecate use standard field search API
	 */
	public User getUserByEmail(String inEmail) {
		return getXmlUserArchive().getUserByEmail(inEmail);
	}

	public HitTracker getUsersInGroup(Group inGroup) {
		SearchQuery query = createSearchQuery();
		if (inGroup == null) {
			throw new OpenEditException("No group found");
		}
		query.addMatches("groups", inGroup.getId());
		query.setSortBy("namesorted");
		HitTracker tracker = search(query);
		return tracker;
	}

	public void saveUsers(List userstosave, User inUser) {
		for (Iterator iterator = userstosave.iterator(); iterator.hasNext();) {
			User user = (User) iterator.next();
			saveData(user, inUser);
		}

	}

	// public void setCatalogId(String inCatalogId) {
	// // This can be removed in the future once we track down a singleton bug
	// if (inCatalogId != null && !inCatalogId.equals("system")) {
	// OpenEditException ex = new OpenEditException(
	// "Invalid catalogid catalogid=" + inCatalogId);
	// ex.printStackTrace();
	// log.error(ex);
	//
	// // throw ex;
	// }
	//
	// super.setCatalogId("system");
	// if (fieldPropertyDetailsArchive != null) {
	// fieldPropertyDetailsArchive.setCatalogId("system");
	//
	// }
	// }

	

	@Override
	public Data createNewData() {
		// return getXmlUserArchive().createUser(null, null);
		return new BaseUser();
	}

	@Override
	public void deleteData(Data inData) {
		getXmlUserArchive().deleteUser((User) inData);
	}
}
