package org.entermedia.profile;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.XmlFileSearcher;
import org.openedit.profile.UserProfile;

import com.openedit.users.User;
import com.openedit.users.UserManager;

public class UserProfileSearcher extends XmlFileSearcher {

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	protected UserManager fieldUserManager;

	@Override
	public Data createNewData() {
		UserProfile userProfile = (UserProfile) getModuleManager().getBean(
				"userProfile");
		userProfile.setCatalogId(getCatalogId());

		// Create new should never save things
		// User current = getUserManager().createUser(null, new
		// PasswordGenerator().generate());
		//
		// userProfile.setUser(current);
		// userProfile.setProperty("userid", current.getId());
		return userProfile;
	}

	@Override
	public Object searchById(String inId) {

		UserProfile search = (UserProfile) super.searchById(inId);
		if (search == null) {
			return null;
		}
		String userid = search.getUserId();
		User user = getUserManager().getUser(userid);
		search.setUser(user);
		return search;
	}

	// public void saveData(Data inData, User inUser) {
	// UserProfile profile= (UserProfile)inData;
	//
	// // if(profile.getUser() == null)
	// // {
	// // User current = getUserManager().getUser(profile.get("userid"));
	// // if( current == null )
	// // {
	// // log.info("No user found, creating new one");
	// // current = getUserManager().createUser(null, null);
	// // }
	// // profile.setUser(current);
	// // profile.setProperty("userid", current.getId());
	// // }
	// // Searcher usersearcher = getSearcherManager().getSearcher("system",
	// "user");
	// // usersearcher.saveData(profile.getUser(), inUser);
	//
	// // TODO Auto-generated method stub
	// super.saveData(inData, inUser);
	//
	// }

	public synchronized String nextId() {
		return getUserManager().nextId();
	}

	protected void updateIndex(Data inData, Document doc,
			PropertyDetails inDetails) {

		if (inData instanceof UserProfile) {
			UserProfile up = (UserProfile) inData;
			User user = up.getUser();
			if(user == null){
				user = getUserManager().getUser(up.getId());
			}
			up.setUser(user);
			if (user != null) {
				if (user.getFirstName() != null) {
					doc.add(new Field("firstname", user.getFirstName(),
							Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
				if (user.getLastName() != null) {
					doc.add(new Field("lastname", user.getFirstName(),
							Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
				doc.add(new Field("deleted", "false",
						Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			} else{
				doc.add(new Field("deleted", "true",
						Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		}

		super.updateIndex(inData, doc, inDetails);

	}

}
