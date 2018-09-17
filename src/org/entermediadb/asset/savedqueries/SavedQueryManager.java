package org.entermediadb.asset.savedqueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class SavedQueryManager
{
	private static final Log log = LogFactory.getLog(SavedQueryManager.class);
	protected SearcherManager fieldSearcherManager;

	public Collection loadSavedQueryList(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		Searcher savedsearcher = getSearcherManager().getSearcher(catalogid, "savedquery");
		SearchQuery query = savedsearcher.createSearchQuery();
		String userid = inReq.getUserName();
		if( userid == null)
		{
			userid = "guest";
		}
		query.addExact("userid", userid);
//		if( showsaved != null)
//		{
//			//filter by show saved
//			query.addExact("usersaved", showsaved);
			query.addSortBy("saveddateDown");
//		}
//		else
//		{
//			query.addSortBy("name");
//			
//		}

		//This filter allows the cachedSearch to be cached for this user.
		//Otherwise the two searches would be run each time the page loads per user
		Collection hits = savedsearcher.cachedSearch(inReq,query);

		Collection copy = new ArrayList(hits.size());
		String showsaved = inReq.findValue("showsaved");
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data row = (Data) iterator.next();
			if( showsaved == null || showsaved.equals(row.get("usersaved")) )
			{
				copy.add(row);
			}
		}
		inReq.putPageValue("filderedquerylist",copy);
		
		
		return hits;
	}
	public Data saveQuery(String inCatalogId, SearchQuery inQuery, User inUser) throws Exception
	{
		Searcher savedsearcher = getSearcherManager().getSearcher(inCatalogId, "savedquery");
		Data data = (Data)savedsearcher.createNewData();
		saveQuery(inCatalogId,inQuery,null,data,inUser);
		
		for (Iterator iterator = inQuery.getChildren().iterator(); iterator.hasNext();)
		{
			SearchQuery searchq = (SearchQuery) iterator.next();
			if( !searchq.isFilter() )
			{
				Data child = (Data)savedsearcher.createNewData();
				child.setProperty("parentid", data.getId());
				saveQuery(inCatalogId,searchq,data.getId(),child,inUser);
			}
		}

		return data;
	}
	public Data saveQuery(String inCatalogId, SearchQuery inQuery,String parentid, Data data, User inUser) throws Exception
	{
		if( inQuery.getName() == null)
		{
			throw new OpenEditException("Name is required");
		}
		Searcher savedsearcher = getSearcherManager().getSearcher(inCatalogId, "savedquery");
		Searcher termssearcher = getSearcherManager().getSearcher(inCatalogId, "savedqueryterm");

		saveData(inQuery, data, inUser, savedsearcher);
		
		//now save the terms
		saveTerms(inQuery, data, termssearcher, inUser);

		if( parentid != null )
		{
			//save the children!
			Collection oldqueries = savedsearcher.fieldSearch("parentid", data.getId());
			for (Iterator iterator = oldqueries.iterator(); iterator.hasNext();)
			{
				Data row = (Data) iterator.next();
				deleteOldTerms(row, inUser, termssearcher);
				savedsearcher.delete(row, inUser);
			}
		}
		return data;
	}
	protected void saveData(SearchQuery inQuery, Data data, User inUser, Searcher savedsearcher)
	{
		data.setProperties(inQuery.getProperties());
		data.setId(inQuery.getId());
		data.setName(inQuery.getName());
		data.setProperty("andtogether", String.valueOf( inQuery.isAndTogether() ) );
		data.setSourcePath(inUser.getId()); 
		data.setProperty("userid", inUser.getId());
		data.setProperty("saveddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		savedsearcher.saveData(data, inUser);
		inQuery.setId(data.getId());
	}
	protected void saveTerms(SearchQuery inQuery, Data data, Searcher termssearcher, User inUser)
	{
		//delete old ones
		deleteOldTerms(data, inUser, termssearcher);
		//save terms
		List newterms = new ArrayList();
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			if( inQuery.isSecurityAttached() && "viewasset".equals( term.getId() ) )
			{
				continue;
			}
			Data row = termssearcher.createNewData();
			row.setProperty("queryid",data.getId());
			//row.setProperty("searchtype","asset"); //hard coded for now
			row.setProperty("detailid",term.getDetail().getId());
			//TODO: Need the view to load up the detail id
			//row.setProperty("searchcatalogid",term.getDetail().getCatalogId());
			row.setProperty("op",term.getOperation());
			row.setProperty("val",term.getValue());
			//TODO: load up more date properties
			row.setSourcePath(inUser.getId());
			newterms.add(row);
		}
		termssearcher.saveAllData(newterms, inUser);
	}
	protected void deleteOldTerms(Data data, User inUser, Searcher termssearcher)
	{
		if( data.getId() != null )
		{
			Collection oldterms = termssearcher.fieldSearch("queryid", data.getId());
			for (Iterator iterator = oldterms.iterator(); iterator.hasNext();)
			{
				Data row = (Data) iterator.next();
				termssearcher.delete(row, inUser);
			}
		}
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Data loadSavedQuery(String inCatalogId, String inId, User inUser)
	{
		Searcher savedsearcher = getSearcherManager().getSearcher(inCatalogId, "savedquery");
		Data saveddata = (Data)savedsearcher.searchById(inId);
		return saveddata;
	}

	public Collection loadSavedQueryTerms(String inCatalogId, String inId)
	{
		Searcher savedsearcher = getSearcherManager().getSearcher(inCatalogId, "savedqueryterm");
		Collection terms = savedsearcher.fieldSearch("queryid", inId);
		return terms;
	}

//	protected void readInputs(Element inInputs, SearchQuery inQuery)
//	{
//		if (inInputs == null)
//		{
//			return;
//		}
//		for (Iterator i = inInputs.elementIterator("input"); i.hasNext();)
//		{
//			Element input = (Element) i.next();
//			String key = input.attributeValue("key");
//			ArrayList valueList = new ArrayList();
//			//TODO: This is not needed, just save the values as a single space delimited "orgroups"
//			for (Iterator valiterator = input.elementIterator("value"); valiterator.hasNext();)
//			{
//				valueList.add(((Element) valiterator.next()).getText());
//			}
//			String[] values = (String[]) valueList.toArray(new String[valueList.size()]);
//			inQuery.setProperty(key, values);
//		}
//	}

	public void deleteQuery(String inCatalogId, String inId, User inUser) 
	{
		Searcher savedsearcher = getSearcherManager().getSearcher(inCatalogId, "savedquery");
		Data data = loadSavedQuery(inCatalogId, inId, inUser);
		if( data != null)
		{
			savedsearcher.delete(data, inUser);
		}
		//TODO: Delete the terms
	}
	public SearchQuery loadSearchQuery(String inCatalogid, Data inQuery, User inUser) 
	{
		return loadSearchQuery(inCatalogid,inQuery,true,inUser);
	}

	public SearchQuery loadSearchQuery(String inCatalogId, Data inSavedQuery,boolean inWithChildren, User inUser) 
	{
		Searcher assetsearcher = getSearcherManager().getSearcher(inCatalogId, "asset");

		SearchQuery query = assetsearcher.createSearchQuery();
		String description = inSavedQuery.get("caption");
		query.setDescription(description);
		String name = inSavedQuery.get("name");
		query.setName(name);
		query.setId(inSavedQuery.getId());
		String and =  inSavedQuery.get("andtogether");
		if (Boolean.parseBoolean(and))
		{
			query.setAndTogether(true);
		}
		else
		{
			query.setAndTogether(false);
		}

		query.setProperties(inSavedQuery.getProperties()); //all the inputs

		Collection terms = loadSavedQueryTerms(inCatalogId,inSavedQuery.getId());
		for (Iterator iterator = terms.iterator(); iterator.hasNext();)
		{
			Data term = (Data) iterator.next();
			addTerm(query, term, inCatalogId, inUser);
		}
		if( inWithChildren )
		{
			Searcher savedsearcher = getSearcherManager().getSearcher(inCatalogId, "savedquery");
			Collection children = savedsearcher.fieldSearch("parentid",inSavedQuery.getId());
			for (Iterator iterator = children.iterator(); iterator.hasNext();)
			{
				Data child = (Data) iterator.next();
				SearchQuery subquery = loadSearchQuery(inCatalogId,child,false,inUser);
				query.addChildQuery(subquery);
			}
		}
		
//		int count = 0;
//		for (Iterator iterator = root.elementIterator("query"); iterator.hasNext();)
//		{
//			Element element = (Element) iterator.next();
//			SearchQuery child = searcher.createSearchQuery();
//			loadQuery(child, inCatalogId, inId + count++, inUser, searcher, element);
//			query.addChildQuery(child);
//		}
//		readInputs(root.element("inputs"), query);
		return query;
	}
	protected void addTerm(SearchQuery query, Data term, String inCatalogId, User inUser)
	{
		String op = term.get("op");
		String realop = term.get("realop");
		String val = term.get("val");
		String field = term.get("detailid");

		String catalogid = term.get("catalogid");
		if (catalogid == null)
		{
			catalogid = inCatalogId;
		}
		String view = term.get("view");
		String searchtype = term.get("searchtype");
		if (searchtype == null)
		{
			searchtype = "asset"; //Was not being set by albums
		}
		PropertyDetailsArchive property = getSearcherManager().getPropertyDetailsArchive(catalogid);
		PropertyDetail detail = property.getDataProperty(searchtype, view, field, inUser);

		if (detail == null)
		{
			detail = getSearcherManager().getSearcher(catalogid, searchtype).getDetail(field);
		}
		if (detail == null)
		{
			// create a virtual one?
			detail = new PropertyDetail();
			detail.setId(field);
			detail.setView(view);
			detail.setCatalogId(catalogid);
			detail.setSearchType(searchtype);
			//	continue;
		}

		query.setProperty(field, val);
		Term t = null;

		op = op.toLowerCase();

		if ("matches".equals(op))
		{
			t = query.addMatches(detail, val);
		}
		else if ("exact".equals(op))
		{
			t = query.addExact(detail, val);
		}
		else if ("startswith".equals(op))
		{
			t = query.addStartsWith(detail, val);
		}
		else if ("not".equals(op))
		{
			t = query.addNot(detail, val);
		}
		//TODO: Add date support
//			else if ("afterdate".equals(op))
//			{
//				Date after = query.getDateFormat().parse(val);
//				query.setProperty("datedirection" + field, "after");
//				t = query.addAfter(detail, after);
//			}
//			else if ("betweendates".equals(op))
//			{
//				String low = term.get("afterDate");
//				String high = term.get("beforeDate");
//				Date lowdate = query.getDateFormat().parse(low);
//				Date highdate = query.getDateFormat().parse(high);
//				t = query.addBetween(detail, lowdate, highdate);
//				query.setProperty(field + ".before", low);
//				query.setProperty(field + ".after", high);
//			}
//			else if ("beforedate".equals(op))
//			{
//				Date before = query.getDateFormat().parse(val);
//				query.setProperty("datedirection" + field, "before");
//				t = query.addBefore(detail, before);
//			}
		else if ("orgroup".equals(op))
		{
			t = query.addOrsGroup(detail, val);
		}
		else if ("orsgroup".equals(op))
		{
			t = query.addOrsGroup(detail, val);
		}
		else if ("notgroup".equals(op))
		{
			t = query.addNots(detail, val);
		}
//			else if ("categoryfilter".equals(op))
//			{ //not used?
//				List categories = new ArrayList();
//				for (Iterator iterator2 = term.elementIterator("categories"); iterator2.hasNext();)
//				{
//					String category = (String) iterator2.next();
//					categories.add(category);
//				}
//				query.addCategoryFilter(categories);
//			}
		else if ("betweennumbers".equals(op))
		{
			String low = term.get("lowval");
			String high = term.get("highval");
			t = query.addBetween(detail, Long.parseLong(low), Long.parseLong(high));
		}
		else if ("greaterthannumber".equals(op))
		{
			t = query.addGreaterThan(detail, Long.parseLong(val));
		}
		else if ("lessthannumber".equals(op))
		{
			t = query.addLessThan(detail, Long.parseLong(val));
		}
		else if ("equastonumber".equals(op))
		{
			t = query.addExact(detail, Long.parseLong(val));
		}
		else
		{
			log.error("Operation not recognized: " + op);
		}
		if (t != null && realop != null)
		{
			t.addValue("op", realop);
		}
		t.setId(term.getId());
	}

}
