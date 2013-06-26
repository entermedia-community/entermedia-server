package org.openedit.users;

import java.text.DateFormat;
import java.util.Collection;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.profile.UserProfile;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.Group;
import com.openedit.users.User;

public class CatalogUserSearcher extends BaseSearcher implements Searcher, UserSearcher {
	
	
	protected UserSearcher fieldUserSearcher;
	protected String fieldCatalogId;
	protected SearcherManager fieldSearcherManager;
	
	
	
	public UserSearcher getUserSearcher() {
		return fieldUserSearcher;
	}

	public void setUserSearcher(UserSearcher inUserSearcher) {
		fieldUserSearcher = inUserSearcher;
	}

	public String nextId() {
			return getUserSearcher().nextId();
	}

	@Override
	public HitTracker cachedSearch(WebPageRequest inPageRequest,
			SearchQuery inQuery) throws OpenEditException {
		return getUserSearcher().cachedSearch(inPageRequest, inQuery);
	}

	@Override
	public HitTracker loadHits(WebPageRequest inReq) throws OpenEditException {
return getUserSearcher().loadHits(inReq);
	}

	@Override
	public HitTracker loadHits(WebPageRequest inReq, String inHitsname)
			throws OpenEditException {
		return getUserSearcher().loadHits(inReq, inHitsname);
	}

	@Override
	public DateFormat getDefaultDateFormat() {
		return getUserSearcher().getDefaultDateFormat();
	}

	@Override
	public void setDefaultDateFormat(DateFormat inDefaultDateFormat) {
		getUserSearcher().setDefaultDateFormat(inDefaultDateFormat);		
	}

	@Override
	public HitTracker fieldSearch(WebPageRequest inReq)
			throws OpenEditException {
		return getUserSearcher().fieldSearch(inReq);
	}

	@Override
	public HitTracker fieldSearch(String inAttr, String inValue) {
	return getUserSearcher().fieldSearch(inAttr, inValue);
	}

	@Override
	public HitTracker fieldSearch(String inAttr, String inValue,
			String inOrderby) {
		return getUserSearcher().fieldSearch(inAttr, inValue, inOrderby);	
				}

	@Override
	public SearchQuery addStandardSearchTerms(WebPageRequest inPageRequest)
			throws OpenEditException {
		return getUserSearcher().addStandardSearchTerms(inPageRequest);
	}

	@Override
	public Data updateData(WebPageRequest inReq, String[] inFields, Data inData) {
		// TODO Auto-generated method stub
		return getUserSearcher().updateData(inReq, inFields, inData);
	}

	@Override
	public List deselect(String inField, String[] inToremove)
			throws OpenEditException {
		return getUserSearcher().deselect(inField, inToremove);
	}

	@Override
	public SearchQuery addActionFilters(WebPageRequest inReq,
			SearchQuery inSearch) {
		return getUserSearcher().addActionFilters(inReq, inSearch);
	}

	@Override
	public HitTracker loadPageOfSearch(WebPageRequest inPageRequest)
			throws OpenEditException {
		return getUserSearcher().loadPageOfSearch(inPageRequest);
	}

	@Override
	public void reIndexAll() throws OpenEditException {
		getUserSearcher().reIndexAll();
		
	}

	@Override
	public SearchQuery createSearchQuery() {
		return getUserSearcher().createSearchQuery();
	}

	@Override
	public Object searchById(String inId) {
		return getUserSearcher().searchById(inId);
	
	}

	@Override
	public Object searchByField(String inField, String inValue) {
		return getUserSearcher().searchByField(inField, inValue);
	}

	@Override
	public Data searchByQuery(SearchQuery inQuery) {
		return getUserSearcher().searchByQuery(inQuery);
	}

	@Override
	public HitTracker search(SearchQuery inQuery) {
		return getUserSearcher().search(inQuery);
	}

	@Override
	public String getIndexId() {
		return getUserSearcher().getIndexId();
	}

	@Override
	public void clearIndex() {
		getUserSearcher().clearIndex();
		
	}

	@Override
	public PropertyDetailsArchive getPropertyDetailsArchive() {
		return getUserSearcher().getPropertyDetailsArchive();
	}

	@Override
	public void setPropertyDetailsArchive(
			PropertyDetailsArchive inPropertyDetailsArchive) {
		//DO NOTHING?
		
		
	}

	@Override
	public PropertyDetails getPropertyDetails() {
		return getUserSearcher().getPropertyDetails();
	}

	@Override
	public List getDetailsForView(String inView, User inUser) {

		return getUserSearcher().getDetailsForView(inView, inUser);
	}

	@Override
	public List getDetailsForView(String inView, UserProfile inUserProfile) {
		return getUserSearcher().getDetailsForView(inView, inUserProfile);
	}

	
	@Override
	public List getProperties() {
	return getUserSearcher().getProperties();
	}

	@Override
	public HitTracker getAllHits(WebPageRequest inReq) {
	return getUserSearcher().getAllHits();
	}

	@Override
	public HitTracker getAllHits() {
		return getUserSearcher().getAllHits();
	}

	@Override
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	@Override
	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
		
	}

	

	public void saveData(Data inData, User inUser) {
		getUserSearcher().saveData(inData, inUser);
		
	}


	public Data createNewData() {
		return getUserSearcher().createNewData();
	}

	@Override
	public void deleteAll(User inUser) {
		getUserSearcher().deleteAll(inUser);
		
	}

	@Override
	public void delete(Data inData, User inUser) {
		getUserSearcher().delete(inData, inUser);
		
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser) {
		getUserSearcher().saveAllData(inAll, inUser);
		
	}

	@Override
	public PropertyDetail getDetail(String inId) {
		// TODO Auto-generated method stub
	return 	getUserSearcher().getDetail(inId);
	}

	@Override
	public void changeSort(WebPageRequest inReq) {
		getUserSearcher().changeSort(inReq);
		
	}

	@Override
	public void addChildQuery(WebPageRequest inReq) {
		getUserSearcher().addChildQuery(inReq);
		
	}

	@Override
	public void saveDetails(WebPageRequest inReq, String[] inFields,
			Data inData, String inId) {
		getUserSearcher().saveDetails(inReq, inFields, inData, inId);
		
	}

	@Override
	public Data uniqueResult(SearchQuery inQ) {
	return getUserSearcher().uniqueResult(inQ);
	}

	@Override
	public User getUser(String inAccount) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User getUserByEmail(String inEmail) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HitTracker getUsersInGroup(Group inGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveUsers(List inUserstosave, User inUser) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PropertyDetail getDetailForView(String inView, String inFieldName,
			User inUser) {
		// TODO Auto-generated method stub
		return null;
	}

}
