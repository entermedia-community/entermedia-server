package org.entermedia.attachments;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.scanner.MetaDataReader;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;

public class AttachmentManager {

	
	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	protected MetaDataReader fieldMetaDataReader;

	public MetaDataReader getMetaDataReader()
	{
		return fieldMetaDataReader;
	}

	public void setMetaDataReader(MetaDataReader inMetaDataReader)
	{
		fieldMetaDataReader = inMetaDataReader;
	}
	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public HitTracker processAttachments(MediaArchive inArchive, Asset inAsset, boolean reprocess) {
		HitTracker hittracker = new ListHitTracker();
		String inCatalogId = inArchive.getCatalogId();
		Searcher attachmentSearcher = getAttachmentSearcher(inCatalogId);
//		if(reprocess){
//			deleteEntries(inAsset, attachmentSearcher);
//		}
		String destination = "/WEB-INF/data/" + inCatalogId + "/originals/" + inAsset.getSourcePath();
		List files = getPageManager().getChildrenPaths(destination);
		for (Iterator iterator = files.iterator(); iterator.hasNext();) {
			String file = (String) iterator.next();
			String fullpath = file;
			
			file = file.substring(file.lastIndexOf("/")+1, file.length());
			SearchQuery query = attachmentSearcher.createSearchQuery();
			query.addMatches("assetid", inAsset.getId());
			query.addMatches("name", file);
			HitTracker hits = attachmentSearcher.search(query);
			if(hits.size() == 0){
				//need a new entry
				Data attachment = attachmentSearcher.createNewData();
				
				attachment.setSourcePath(inAsset.getSourcePath());
				
				attachment.setProperty("name", file);
				attachment.setProperty("assetid", inAsset.getId());
				Asset asset = new Asset();
				Page target = getPageManager().getPage(fullpath);
				File input = new File(target.getContentItem().getAbsolutePath());
				getMetaDataReader().populateAsset(inArchive, input, asset);
				for (Iterator iterator2 = asset.getProperties().keySet().iterator(); iterator2.hasNext();) {
					String key = (String) iterator2.next();
					String value = inAsset.get(key);
					attachment.setProperty(key, value);
					
				}
				attachmentSearcher.saveData(attachment, null);
				hittracker.add(attachment);
				
			}
			
		}
		return hittracker;
		
	}

	private void deleteEntries(Asset inAsset, Searcher inAttachmentSearcher) {
		HitTracker hits = inAttachmentSearcher.fieldSearch("assetid", inAsset.getId());
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			inAttachmentSearcher.delete(hit, null);
			
		}
		
		
	}

	public Searcher getAttachmentSearcher(String inCatalogId) {
		Searcher attachmentSearcher = getSearcherManager().getSearcher(inCatalogId, "attachment");
		return attachmentSearcher;
	}

	public HitTracker findAssetAttachments(WebPageRequest inReq,
			MediaArchive inArchive, Asset inAsset) {
		HitTracker hits = getAttachmentSearcher(inArchive.getCatalogId()).fieldSearch("assetid", inAsset.getId());
		if(hits.size() == 0){
			processAttachments(inArchive, inAsset, true);		
		}
		processAttachments(inArchive, inAsset, true);		
		hits = getAttachmentSearcher(inArchive.getCatalogId()).fieldSearch("assetid", inAsset.getId());
		return hits;
		
	}
	
	
	
	
}
