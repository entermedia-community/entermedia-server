package org.entermediadb.data;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.PageAccessListener;
import org.openedit.data.BaseSearcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.DataHitTracker;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class FileSearcher extends BaseSearcher implements PageAccessListener {

	protected PageManager fieldPageManager;
	protected SearcherManager fieldSearcherManager;
	protected File fieldRoot;
	private static final Log log = LogFactory.getLog(FileSearcher.class);

	public PageManager getPageManager() {
		return fieldPageManager;

	}

	public HitTracker searchFiles(String rootFolder, String field, String mask) {
		return searchFiles(rootFolder, field, mask, null);
	}

	public HitTracker searchFiles(String rootFolder, String field, String mask,
			String orderby) {
		FileSearchQuery query = (FileSearchQuery) createSearchQuery();
		query.setRootFolder(rootFolder);
		query.addMatches(field, mask);
		if (orderby != null) {
			query.setSortBy(orderby);
		}
		return search(query);

	}

	public void setPageManager(PageManager inPageManager) {
		if (fieldPageManager != null) {
			fieldPageManager.removePageAccessListener(this);
		}
		fieldPageManager = inPageManager;
		inPageManager.addPageAccessListener(this);
	}

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public File getRoot() {
		return fieldRoot;
	}

	public void setRoot(File inRoot) {
		fieldRoot = inRoot;
	}

	public void clearIndex() {
		// TODO Auto-generated method stub

	}

	public SearchQuery createSearchQuery() {
		return new FileSearchQuery();
	}

	public void delete(Data inData, User inUser) {
		if (inData instanceof Page) {

			getPageManager().removePage((Page) inData);
		}

	}

	public void deleteAll(User inUser) {

		// TODO Auto-generated method stub

	}

	public String getIndexId() {
		return getSearchType() + getCatalogId();
	}

	public void reIndexAll() throws OpenEditException {
		// TODO Auto-generated method stub

	}

	public void saveAllData(Collection inAll, User inUser) {
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();) {

			Data object = (Data) iterator.next();
			saveData(object, inUser);
		}

	}

	public void saveData(Data inData, User inUser) {
		if (inData instanceof Page) {
			getPageManager().saveSettings((Page) inData);
		}

	}

	 public Object searchById(String inId){
         
         return getPageManager().getPage(inId);
         
 }

	public HitTracker search(SearchQuery inQuery) {

		try {

			List results = new ArrayList();

			FileSearchQuery query = (FileSearchQuery) inQuery;
			Page root = getPageManager().getPage(query.getRootFolder());
			//getPageManager().clearCache();

			collectHits(root, results, (FileSearchQuery) inQuery);
			sortResults(inQuery, results);
			HitTracker hits = new DataHitTracker();
			hits.setSearchQuery(inQuery);
			hits.setIndexId(getSearchType() + getCatalogId());
			hits.addAll(results);

			return hits;
		} catch (ParseException e) {

			throw new OpenEditRuntimeException(e);
		}
	}

	protected void collectHits(Page inRoot, List inResults,
			FileSearchQuery inQuery) throws ParseException {
		if (inRoot == null || inQuery == null || inRoot.getName() == null) {
			return;
		}
		if (inRoot.getName().equals(".versions")
				&& !inQuery.isIncludeVersions()) {
			return;
		}
		List childpaths = getPageManager().getChildrenPaths(inRoot.getPath(),
				inQuery.isIncludeFallback());
		for (Iterator iterator = childpaths.iterator(); iterator.hasNext();) {
			String nextfile = (String) iterator.next();
			Page page = getPageManager().getPage(nextfile, false);
			if (passes(page, inQuery)) {
				page = getPageManager().getPage(nextfile, true);
				inResults.add(page);
			}
			if (inQuery.isRecursive()) {

				if (page.isFolder()) {
					collectHits(page, inResults, inQuery);
				}
			}
		}

	}

	protected boolean passes(Page inPage, FileSearchQuery inQuery)
			throws ParseException {
		if (inPage.getName().equals(".versions")) {
			return false;
		}
		for (Iterator iterator = inQuery.getTerms().iterator(); iterator
				.hasNext();) {

			Term term = (Term) iterator.next();
			if ("betweendates".equals(term.getOperation())) {
				Date before = 	(Date) term.getValue("lowDate");
				Date after = (Date) term.getValue("highDate");
				String id = term.getDetail().getId();// effectivedate
				String date = inPage.getProperty(id);
				if (date == null) {
					return false;
				}
				Date target = getDefaultDateFormat().parse(date);
				if (!(before.before(target) && after.after(target))) {
					return false;
				}

			} else if ("afterdate".equals(term.getOperation())) {
				Date after = inQuery.getDateFormat().parse(
						(String) term.getValue("highDate"));
				String id = term.getDetail().getId();// effectivedate
				String date = inPage.getProperty(id);
				if (date == null) {
					return false;
				}
				Date target = getDefaultDateFormat().parse(date);
				if (!target.after(after)) {
					return false;
				}

			} else if ("beforedate".equals(term.getOperation())) {
				Date before = inQuery.getDateFormat().parse(
						(String) term.getValue("lowDate"));
				String id = term.getDetail().getId();// effectivedate
				String date = inPage.getProperty(id);
				if (date == null) {
					return false;
				}
				Date target = getDefaultDateFormat().parse(date);
				if (!target.before(before)) {
					return false;
				}

			} else {

				String name = term.getDetail().getId();
				if (name == null) {
					name = "id";
				}
				String value = term.getValue().toLowerCase();
				String attribval = null;
				if ("name".equals(name)) {
					attribval = inPage.getName();
				} else {
					attribval = inPage.getProperty(name);
				}

				if (attribval != null
						&& (value.equals("*") || PathUtilities.match(attribval
								.toLowerCase(), value))) {
					if (!inQuery.isAndTogether()) {
						return true;
					}
				} else if (inQuery.isAndTogether()) {
					return false;
				}
			}
		}
		return true;
	}

	private void sortResults(SearchQuery inQuery, List results) {
		String sortby = inQuery.getSortBy();

		if (sortby != null) {
			if (sortby.endsWith("Up")) {
				sortby = sortby.substring(0, sortby.length() - 2);
				sortResultsUp(results, sortby);
			} else if (sortby.endsWith("Down")) {
				sortby = sortby.substring(0, sortby.length() - 4);
				sortResultsDown(results, sortby);
			} else {
				sortResultsUp(results, sortby);
			}

		}
	}

	private void sortResultsUp(List results, final String inProperty) {
		Collections.sort(results, new Comparator() {
			public int compare(Object o1, Object o2) {
				Page ed1 = (Page) o1;
				Page ed2 = (Page) o2;

				String s1, s2;
				if ("text".equals(inProperty) || "name".equals(inProperty)) {
					s1 = ed1.getName();
					s2 = ed2.getName();
				} else {
					s1 = ed1.get(inProperty);
					s2 = ed2.get(inProperty);
				}
				if (s1 == null && s2 == null) {
					return 0;
				}

				if (s1 != null && s2 == null) {
					return -1;
				}
				if (s1 == null && s2 != null) {
					return 1;
				}

				if (s1 == null) {
					return -s2.compareTo(s1);
				}
				s1 = String.format("%1$#" + 6 + "s", s1);
				s2 = String.format("%1$#" + 6 + "s", s2);

				return s1.toLowerCase().compareTo(s2.toLowerCase());
			}
		});
	}

	private void sortResultsDown(List results, final String inProperty) {
		Collections.sort(results, new Comparator() {
			public int compare(Object o1, Object o2) {
				Page ed2 = (Page) o1;
				Page ed1 = (Page) o2;

				String s1, s2;
				if ("text".equals(inProperty) || "name".equals(inProperty)) {
					s1 = ed1.getName();
					s2 = ed2.getName();
				} else {
					s1 = ed1.get(inProperty);
					s2 = ed2.get(inProperty);
				}
				if (s1 == null && s2 == null) {
					return 0;
				}
				if (s1 != null && s2 == null) {
					return 1;
				}
				if (s1 == null && s2 != null) {
					return -1;
				}
				if (s1 == null) {
					return -s2.compareTo(s1);
				}
				return s1.toLowerCase().compareTo(s2.toLowerCase());
			}
		});
	}

	public void pageAdded(Page inPage) {
		// TODO Auto-generated method stub

	}

	public void pageModified(Page inPage) {
		// TODO Auto-generated method stub

	}

	public void pageRemoved(Page inPage) {
		// TODO Auto-generated method stub

	}

	public void pageRequested(Page inPage) {
		// TODO Auto-generated method stub

	}

}
