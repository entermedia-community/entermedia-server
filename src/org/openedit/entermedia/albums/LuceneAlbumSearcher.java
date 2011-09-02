package org.openedit.entermedia.albums;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.comments.Comment;
import com.openedit.comments.CommentArchive;
import com.openedit.hittracker.CompositeHitTracker;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;

public class LuceneAlbumSearcher extends BaseLuceneSearcher implements AlbumSearcher
{
	private static final Log log = LogFactory.getLog(LuceneAlbumSearcher.class);
	protected PageManager fieldPageManager;
	protected EnterMedia fieldEnterMedia;
	protected ModuleManager fieldModuleManager;

	public EnterMedia getEnterMedia()
	{
		if (fieldEnterMedia == null)
		{
			fieldEnterMedia = (EnterMedia) getModuleManager().getBean(getCatalogId(), "enterMedia");
		}
		return fieldEnterMedia;
	}

	public void setEnterMedia(EnterMedia inEnterMedia)
	{
		fieldEnterMedia = inEnterMedia;
	}

	public AlbumArchive getAlbumArchive()
	{
		return getEnterMedia().getAlbumArchive();
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}

	public void reIndexAll(IndexWriter inWriter) 
	{
		int count = 0;
		// get the path to a reindex
		String homepath = "/" + getCatalogId() + "/users/";// +
		// inUser.getUserName()
		// + "/albums/";
		List users = getPageManager().getChildrenPaths(homepath);
		PropertyDetails details = getPropertyDetails();
		try
		{
			for (Iterator iterator = users.iterator(); iterator.hasNext();)
			{
				String user = (String) iterator.next();
				List paths = getPageManager().getChildrenPaths(user + "/albums");
				for (Iterator iterator2 = paths.iterator(); iterator2.hasNext();)
				{
					String path = (String) iterator2.next();
					Page folder = getPageManager().getPage(path);
					if (folder.isFolder())
					{
						String dataPath = path + "/data.xml";
						Page data = getPageManager().getPage(dataPath);
						if (data.exists())
						{
							Album album = getAlbumArchive().loadAlbum(dataPath);
							Document doc = new Document();
							updateIndex(album, doc, details);
							inWriter.addDocument(doc);
							count++;
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		log.info("" + count + " albums indexed.");
	}
	protected void updateIndex(Data inData, Document inDoc, PropertyDetails inDetails) 
	{
		super.updateIndex(inData, inDoc, inDetails);
		//now take care of comments and participants
		StringBuffer all = new StringBuffer();
		Album album = (Album)inData;
		
		for (Iterator iterator = album.getParticipants().iterator(); iterator.hasNext();)
		{
			User type = (User) iterator.next();
			all.append(type.getId());
			if( iterator.hasNext())
			{
				all.append(" ");
			}
		}
		inDoc.add(new Field("participants", all.toString(), Field.Store.NO, Field.Index.ANALYZED));
	
		CommentArchive ca = (CommentArchive) getSearcherManager().getModuleManager().getBean(getCatalogId(), "commentArchive");
		String sourcepath = getCatalogId() + "/" + inData.getSourcePath() + "/comments.xml";
		Comment comment = ca.getLastComment(sourcepath);
		if(comment != null)
		{
			String commenttext = comment.getComment();
			if(commenttext.length() > 300)
			{
				commenttext = commenttext.substring(0, 299) + "...";
			}
			inDoc.add(new Field("lastcomment", commenttext, Field.Store.YES, Field.Index.ANALYZED));
			inDoc.add(new Field("lastcommentauthor", comment.getUser().getUserName(), Field.Store.YES, Field.Index.ANALYZED));
			inDoc.add(new Field("lastcommentdate", comment.getDate().toString(), Field.Store.YES, Field.Index.ANALYZED));
		}
	
	}

/*	protected void populateItems(HitTracker inAlbumItems, Document inDoc) throws CorruptIndexException, IOException
	{
		StringBuffer buf = new StringBuffer();
		for(Object item: inAlbumItems)
		{
			Hit hit = (Hit) item;
			buf.append(hit.get("catalogid") + "_" + hit.get("id") + " ");
		}
		inDoc.add(new Field("asset", buf.toString(), Store.YES, Index.TOKENIZED));
	}
*/
	
//	protected void updateIndex(Data inData, Document inDoc, PropertyDetails inDetails) throws CorruptIndexException, IOException
//	{
//		super.updateIndex(inData, inDoc, inDetails);
//		populateItems((Album) inData, inDoc);
//	}

	public void saveData(Data inObject, User inUser)
	{
		Album album = (Album) inObject;
		getAlbumArchive().save(album, inUser);
		//populateItems(album);
		updateIndex(album);
	}

	public void deleteData(Data inData)
	{
		Album album = (Album) inData;
		//clearAlbum(album);
		getAlbumArchive().deleteAlbum(album.getId(), album.getUser());
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public void addAssetToAlbum( Asset inAsset, Album inAlbum, WebPageRequest inReq )
	{
		MediaArchive mediaArchive = getEnterMedia().getMediaArchive(inAsset.getCatalogId());
		if( inAsset.getId()  == null)
		{
			String id = mediaArchive.getAssetSearcher().nextAssetNumber();
			inAsset.setId(id);
			mediaArchive.saveAsset(inAsset, inReq.getUser());
		}
		
		Searcher assetalbums = getSearcherManager().getSearcher(inAsset.getCatalogId(), "assetalbums");
		Data item = assetalbums.createNewData();
		item.setSourcePath(inAsset.getSourcePath());
		item.setProperty("albumid", inAlbum.getUserName() + "_" + inAlbum.getId());
		item.setProperty("assetid", inAsset.getId());
		String id = item.get("albumid") + "_" + inAsset.getCatalogId() + "_" + inAsset.getId();
		item.setId(id);
		
		assetalbums.saveData(item, null);
		mediaArchive.getAssetSearcher().updateIndex(inAsset);
	}
	
	public void removeAssetFromAlbum( Asset inAsset, Album inAlbum, WebPageRequest inReq )
	{
		BaseData item = new BaseData();
		item.setSourcePath(inAsset.getSourcePath());
		item.setProperty("albumid", inAlbum.getUserName() + "_" + inAlbum.getId());
		item.setProperty("assetid", inAsset.getId());
		String itemid = item.get("albumid") + "_" + inAsset.getCatalogId() + "_" + inAsset.getId();
		item.setId(itemid);
		Searcher assetalbums = getSearcherManager().getSearcher(inAsset.getCatalogId(), "assetalbums");
		assetalbums.delete(item, null);
		getEnterMedia().getMediaArchive(inAsset.getCatalogId()).getAssetSearcher().updateIndex(inAsset);
	}
	public CompositeHitTracker getAlbumItems(String inAlbumId, String inUserName, WebPageRequest inReq)
	{
		String hitsname = 	"albumhits" + inAlbumId + inUserName;

		return getAlbumItems(inAlbumId, inUserName,hitsname, inReq);
		
	}

	/**
	 * TODO: Move to the multi asset searcher
	 */
	public CompositeHitTracker getAlbumItems(String inAlbumId, String inUserName, String inHitsName,  WebPageRequest inReq)
	{

		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "compositeLucene");
		SearchQuery query = searcher.createSearchQuery();
		query.setHitsName(inHitsName);
		query.setFireSearchEvent(false);

		//TODO: Go lookup the property detail for al album
		//searcher.getPropertyDetails().getDetail("album");
		
		query.addMatches("album", inUserName + "_" + inAlbumId);
		if( "1".equals(inAlbumId) || "2".equals(inAlbumId) || "3".equals(inAlbumId))
		{
			query.setResultType("selection" + inAlbumId);
		}
		else
		{
			query.setResultType("album");			
		}
		inReq.setRequestParameter("catalogid", new String[]{});
		CompositeHitTracker tracker = (CompositeHitTracker) searcher.cachedSearch(inReq, query);
		return tracker;
	}
	
	public HitTracker getAssets(String inCatalogId, String inAlbumId,String inUserName, String inHitsName, WebPageRequest inReq)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "asset");
		SearchQuery query = searcher.createSearchQuery();
		query.setHitsName(inHitsName);
		query.setFireSearchEvent(false);

		query.addMatches("album", inUserName + "_" + inAlbumId);
		if( "1".equals(inAlbumId) || "2".equals(inAlbumId) || "3".equals(inAlbumId))
		{
			query.setResultType("selection" + inAlbumId);
		}
		else
		{
			query.setResultType("album");			
		}
		query.addMatches("editstatus", "*");
		HitTracker tracker = searcher.cachedSearch(inReq, query);
		return tracker;
	}

	
	
	public Album getAlbum(String inAlbumId, String inUserName) 
	{
		Album album = getAlbumArchive().loadAlbum(inAlbumId, inUserName);
		return album;
	}

	public void clearAlbum(Album inAlbum, WebPageRequest inReq)
	{
		EnterMedia em = getEnterMedia();
		int i = 0;
		for(Object o: inAlbum.getAlbumItems(inReq))
		{
			try
			{
				Data hit = (Data)o;
				Asset asset = em.getAssetBySourcePath(hit.get("catalogid"), hit.getSourcePath());
				removeAssetFromAlbum(asset, inAlbum, inReq);
				i++;
			}
			catch (Exception e)
			{
				log.error("Could not remove asset from album '" + inAlbum.getId() + "'. Skip.");
			}
		}
		log.info("removed " + i);
	}

	public void addAssetsToAlbum(Collection<Asset> inAssets, Album inAlbum,
			WebPageRequest inReq)
	{
		for(Asset asset: inAssets)
		{
			addAssetToAlbum(asset, inAlbum, inReq);
		}
	}
	
	public void removeAssetsFromAlbum(Collection<Asset> inAssets,
			Album inAlbum, WebPageRequest inReq)
	{
		for(Asset asset: inAssets)
		{
			removeAssetFromAlbum(asset, inAlbum, inReq);
		}
	}

	public HitTracker searchForAlbums(String inOwner, boolean inAsParticipant, WebPageRequest inReq)
	{
		SearchQuery query = createSearchQuery();
		query.setHitsName("myalbums");
		if( inAsParticipant )
		{
			//add an big or group
			query.addMatches("participants",inOwner);
		}
		else
		{
			query.addMatches("owner", inOwner);
		}
		query.addNot("id", "1");
		query.addNot("id", "2");
		query.addNot("id", "3");
		query.addSortBy("lastmodifiedDown");
		HitTracker tracker = cachedSearch(inReq, query);
		return tracker;
	}

	public Album createAlbum(String albumid) 
	{
		return getAlbumArchive().createAlbum(albumid);
	}
}
