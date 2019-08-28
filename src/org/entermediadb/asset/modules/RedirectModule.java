package org.entermediadb.asset.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditRuntimeException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.PageRequestKeys;
import org.openedit.util.URLUtilities;

public class RedirectModule extends BaseMediaModule {

	protected Searcher hostSearcher;
	protected SearcherManager fieldSearcherManager;
	private static final Log log = LogFactory.getLog(RedirectModule.class);

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public Searcher getHostSearcher(String inCatalogId) {
		return getSearcherManager().getSearcher(inCatalogId, "redirect");

	}

	public void setHostSearcher(Searcher hostSearcher) {
		this.hostSearcher = hostSearcher;
	}

	// little virtual hosting trick
	public void hostRedirect(WebPageRequest inReq) {
		String page = inReq.getPage().getName();
		if(page.length() == 0){
			return;
		}
		if(page.indexOf(".") != -1){
			if(!page.endsWith(".html")){
				return;
			}
		}
		String catalogid = inReq.findValue("catalogid");
		URLUtilities utils = (URLUtilities) inReq
				.getPageValue(PageRequestKeys.URL_UTILITIES);
		if (utils != null) {

			String base = utils.siteRoot() + utils.relativeHomePrefix();
			base = toNormalizedBase(base);
			log.info("normalized base was " + base);
			String root = inReq.findValue("redirectroot");
			log.info("redirect root was" + root);
		
			if (!root.equals(base)) {
				return;
			}
			
			Searcher searcher = getHostSearcher(catalogid);
			SearchQuery query = searcher.createSearchQuery();
			query.addMatches("key", page);
			HitTracker hits = searcher.search(query);

			if (hits.size() == 0) {
				return; // nothing to do.
			}
			Object hit = hits.get(0);
			String hostfolder = hits.getValue(hit, "url");
			log.debug("found match.  URL was: " + hostfolder);
			if (hostfolder != null) {
				String originalpath = utils.getOriginalPath();
				if (!originalpath.startsWith(hostfolder)) {
					inReq.redirect(hostfolder);
				}
			}
		}
	}
	
	public void virtualHost(WebPageRequest inReq) {
		String skipredirect = inReq.findValue("skipvirtualhost");
		if(Boolean.parseBoolean(skipredirect)){
			return;
		}
		URLUtilities utils = (URLUtilities) inReq
				.getPageValue(PageRequestKeys.URL_UTILITIES);
		if (utils != null) {

			String base = utils.siteRoot() + utils.relativeHomePrefix();
			base = toSubdomain(base);
			//log.info("normalized base was" + base);
			String catalogid = inReq.findValue("catalogid");

			Searcher searcher = getHostSearcher(catalogid);
			SearchQuery query = searcher.createSearchQuery();
			query.addMatches("domain", base);
			HitTracker hits = searcher.search(query);
			if (hits.size() > 1) {
				throw new OpenEditRuntimeException();
			}
			if (hits.size() == 0) {
				return; // nothing to do.
			}
			Object hit = hits.get(0);
			String hostfolder = hits.getValue(hit, "folder");
			log.info("found match.  Folder was: " + hostfolder);
			if (hostfolder != null) {
				String originalpath = utils.getOriginalPath();
				if (!originalpath.startsWith(hostfolder)) {
					inReq.redirect(hostfolder);
				}
			}
		}
	}
	
	
	private String toSubdomain(String base) {
			// string off start

			String basestring = base.substring(base.lastIndexOf("//") + 1,
					base.length());
			// strip of any subdomains (including www)

			// remove any slashes
			basestring = basestring.replaceAll("/", "");
//			while (basestring.lastIndexOf('.') != basestring.indexOf(".")) {
//				basestring = basestring.substring(basestring.indexOf(".") + 1,
//						basestring.length());
//			}
			if (basestring.indexOf(":") != -1) {
				basestring = basestring.substring(0, basestring.lastIndexOf(':'));
			}
			basestring = basestring.toLowerCase();
			return basestring;
	}

	public String toNormalizedBase(String base) {
		// string off start

		String basestring = base.substring(base.lastIndexOf("//") + 1,
				base.length());
		// strip of any subdomains (including www)

		// remove any slashes
		basestring = basestring.replaceAll("/", "");
		while (basestring.lastIndexOf('.') != basestring.indexOf(".")) {
			basestring = basestring.substring(basestring.indexOf(".") + 1,
					basestring.length());
		}
		if (basestring.indexOf(":") != -1) {
			basestring = basestring.substring(0, basestring.lastIndexOf(':'));
		}
		basestring = basestring.toLowerCase();
		return basestring;
	}

	private String createfolder(String inRootfolder, Data inDomain) {
		String domain = inDomain.get("domain");
		domain.replaceAll(".", "");
		domain.replaceAll("/", "");
		String folder = inRootfolder + "/" + domain;

		return folder;
	}

	public static String toBase26(int number) {
		number = Math.abs(number);
		String converted = "";
		// Repeatedly divide the number by 26 and convert the
		// remainder into the appropriate letter.
		do {
			int remainder = number % 26;
			converted = (char) (remainder + 'A') + converted;
			number = (number - remainder) / 26;
		} while (number > 0);
		return converted;
	}

	public static int fromBase26(String number) {
		int s = 0;
		if (number != null && number.length() > 0) {
			s = (number.charAt(0) - 'A');
			for (int i = 1; i < number.length(); i++) {
				s *= 26;
				s += (number.charAt(i) - 'A');
			}
		}
		return s;
	}

}
