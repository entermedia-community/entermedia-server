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
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.lucene.BaseLuceneSearcher;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.BaseUser;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

/**
 *
 */
public class LuceneUserSearcher extends BaseLuceneSearcher implements
		UserSearcher {
	private static final Log log = LogFactory.getLog(LuceneUserSearcher.class);
	protected UserManager fieldUserManager;
	protected String fieldUserCatalogId;

	public String getUserCatalogId() {
		if (fieldUserCatalogId ==  null) {
			Data usercat = getSearcherManager().getData(getCatalogId(), "catalogsettings", "user_catalog_id");
			if(usercat != null){
				fieldUserCatalogId = usercat.get("value");
			}
			if(fieldUserCatalogId == null){
				fieldUserCatalogId = "system";
			}
			
			
		}

		return fieldUserCatalogId;
	}

	public void setUserCatalogId(String inUserCatalogId) {
		fieldUserCatalogId = inUserCatalogId;
	}

	public HitTracker getAllHits(WebPageRequest inReq) {
		SearchQuery query = createSearchQuery();
		query.addMatches("enabled", "true");
		query.addMatches("enabled", "false");
		query.addSortBy("namesorted");
		query.setAndTogether(false);
		if (inReq == null) {
			return search(query);
		} else {
			return cachedSearch(inReq, query);
		}
		// return new ListHitTracker().setList(getCustomerArchive().)
	}

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public void reIndexAll(IndexWriter writer, TaxonomyWriter inWriter)
			throws OpenEditException {
	
		setUserCatalogId(null);
		String catid = getUserCatalogId();
		log.info("Reindex of customer users directory");
		try {
			// writer.setMergeFactor(50);
			getUserManager().flush();
			PropertyDetails details = getPropertyDetailsArchive()
					.getPropertyDetails(getSearchType());
			//Collection usernames = getUserManager(catid).listUserNames();
			Collection usernames = getUserManager().listUserNames();
			if (usernames != null) {
				for (Iterator iterator = usernames.iterator(); iterator
						.hasNext();) {
					String userid = (String) iterator.next();
					Document doc = new Document();
					User data = getUserManager().getUser(userid);
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
			getUserManager().saveUser((User) inData);
		}
		updateIndex((User) inData);
	}

	// TODO: Replace with search?
	public Object searchById(String inId) {
		return getUserManager().getUser(inId);
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
		return getUserManager().getUserByEmail(inEmail);
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

//	public void setCatalogId(String inCatalogId) {
//		// This can be removed in the future once we track down a singleton bug
//		if (inCatalogId != null && !inCatalogId.equals("system")) {
//			OpenEditException ex = new OpenEditException(
//					"Invalid catalogid catalogid=" + inCatalogId);
//			ex.printStackTrace();
//			log.error(ex);
//
//			// throw ex;
//		}
//
//		super.setCatalogId("system");
//		if (fieldPropertyDetailsArchive != null) {
//			fieldPropertyDetailsArchive.setCatalogId("system");
//
//		}
//	}

	public PropertyDetailsArchive getPropertyDetailsArchive() {
		if (fieldPropertyDetailsArchive == null) {
			fieldPropertyDetailsArchive = (PropertyDetailsArchive) getSearcherManager()
					.getModuleManager().getBean("system",
							"propertyDetailsArchive");
		}
		fieldPropertyDetailsArchive.setCatalogId("system");
		return fieldPropertyDetailsArchive;
	}

	@Override
	public Data createNewData() {
		// return getUserManager().createUser(null, null);
		return new BaseUser();
	}

	@Override
	public void deleteData(Data inData) {
		getUserManager().deleteUser((User) inData);
	}
}
