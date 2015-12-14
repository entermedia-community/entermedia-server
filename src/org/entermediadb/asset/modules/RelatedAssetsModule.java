package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.RelatedAsset;
import org.entermediadb.asset.RelatedAssetTracker;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.CompositeHitTracker;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;

public class RelatedAssetsModule extends BaseMediaModule
{
	public void addRelatedAsset( WebPageRequest inRequest ) throws OpenEditException
	{
		MediaArchive mediaArchive = getMediaArchive(inRequest);
		
		String[] relatedassetid = inRequest.getRequestParameters( "relatedtoassetid" );
		if( relatedassetid == null)
		{
			//someone created a new Asset
			relatedassetid = inRequest.getRequestParameters( "newassetid" );
		}
		
		if(relatedassetid == null)
		{
			return;
		}
		String type = inRequest.getRequestParameter( "type" );

		boolean redirect = Boolean.parseBoolean(inRequest.findValue("redirect"));
		String parentrelationship = inRequest.findValue("parentrelationship");

		String assetid = inRequest.getRequestParameter("assetid");
		Asset source = mediaArchive.getAsset(assetid);
		if(source == null)
		{
			throw new OpenEditException("Source is null");
		}
		String[] catalogs = inRequest.getRequestParameters("relatedtocatalogid");
		String catalogid = mediaArchive.getCatalogId();
		for (int i = 0; i < relatedassetid.length; i++)
		{
			if( catalogs != null)
			{
				catalogid = catalogs[i];
			}
			MediaArchive savestore = getMediaArchive(catalogid);
			Asset target = savestore.getAsset(relatedassetid[i]);
			target.setProperty("datatype", type);

			createRelationship(savestore, type, parentrelationship, source, target, inRequest.getUser());
		}
		if(redirect)
		{
			String path = source.getSourcePath(); //TODO: Should go back to related Asset list
			inRequest.redirect(mediaArchive.getLinkToAssetDetails(path));		
		}
	}

	private void createRelationship(MediaArchive MediaArchive, String type, String parentrelationship, Asset source, Asset target, User inUser)
	{
		if(  target == null)
		{
			throw new OpenEditException("target is null");
		}
		List<RelatedAsset> relatives = (List)source.getRelatedAssets();
		//Check to see if this relationship already exists.
		for (Iterator iterator = relatives.iterator(); iterator.hasNext();) {
			RelatedAsset rel = (RelatedAsset) iterator.next();
			if(rel.get("relatedtoassetid") == target.getId() && rel.get("relatedtocatalogid") == target.getCatalogId())
			{
				return;
			}
		}
		RelatedAsset related = new RelatedAsset();
		related.setAssetId(source.getId());
		related.setRelatedToAssetId(target.getId());
		related.setRelatedToCatalogId(target.getCatalogId());
		related.setType(type);				
		source.addRelatedAsset(related );
		MediaArchive.saveAsset( source ,null);
	
		if(parentrelationship != null)
		{
			RelatedAsset back = new RelatedAsset();
			back.setAssetId(target.getId());
			back.setRelatedToAssetId(source.getId());
			back.setRelatedToCatalogId(source.getCatalogId());
			
			back.setType(parentrelationship);
			target.addRelatedAsset( back );
			MediaArchive.saveAsset( target ,inUser);
		}
	}
	
	public void loadRelatedAssets( WebPageRequest inRequest ) throws OpenEditException
	{

		Asset asset = getAsset(inRequest);
		MediaArchive archive = getMediaArchive(inRequest);
		Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "relatedasset");
		HitTracker related = searcher.fieldSearch("assetid", asset.getId());

		inRequest.putPageValue("relatedhits", related);
	}
	
	/*
	 * This will load a tracker of all the assets that happen to be related to this one, even transitively. 
	 */
	public RelatedAssetTracker loadAllRelatedAssets( WebPageRequest inRequest ) throws OpenEditException
	{
		RelatedAssetTracker list = (RelatedAssetTracker)inRequest.getPageValue("relatedhits");
		if( list != null )
		{
			return list;
		}
		MediaArchive MediaArchive = getMediaArchive(inRequest);
		Asset Asset = getAsset(inRequest);
		if( Asset == 	null)
		{
			return null;
		}
		List all = new ArrayList();
		all.addAll(Asset.getRelatedAssets()); //initial list
		HashSet targets = new HashSet();
		targets.add(Asset.getCatalogId()+":::"+Asset.getId());
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			RelatedAsset rp = (RelatedAsset) iterator.next();
			targets.add(rp.getRelatedToCatalogId()+":::"+rp.getRelatedToAssetId());
		}
		
		for (int i = 0; i < all.size(); i++)  //iterate through all. new ones added to the end of the list during iteration will also be checked.
		{
			RelatedAsset relatedAsset = (RelatedAsset) all.get(i);
			String catalogid = relatedAsset.getRelatedToCatalogId();
			if(catalogid == null)
			{
				catalogid = MediaArchive.getCatalogId();
			}
			MediaArchive targetstore = getMediaArchive(catalogid);
			Asset target = targetstore.getAsset( relatedAsset.getRelatedToAssetId());
			if(target == null)
			{
				Asset.removeRelatedAsset(catalogid, relatedAsset.getRelatedToAssetId());
				MediaArchive.saveAsset(Asset, inRequest.getUser());
				continue;
			}
			//Go one level deeper
			Collection newOnes = target.getRelatedAssets(); //for each item in the list, add all their relatives
			for (Iterator iterator = newOnes.iterator(); iterator.hasNext();) {
				RelatedAsset newRelated = (RelatedAsset) iterator.next();
				if (!all.contains(newRelated) && !targets.contains(newRelated.getRelatedToCatalogId()+":::"+newRelated.getRelatedToAssetId()))
				{
					targets.add(newRelated.getRelatedToCatalogId()+":::"+newRelated.getRelatedToAssetId());
					all.add(newRelated); //put this new one at the end of the list. we will check check its relations eventually
				}
			}
		}

		RelatedAssetTracker tracker = new RelatedAssetTracker();
		tracker.addAll(all);
		inRequest.putPageValue("relatedhits", tracker);

		return tracker;
	}
	
	public CompositeHitTracker getAllTracker(WebPageRequest inReq) throws Exception
	{
		MediaArchive MediaArchive = getMediaArchive(inReq);
		Asset Asset = getAsset(inReq);
		
		CompositeHitTracker composite = new CompositeHitTracker();
		composite.ensureHasSubTracker(inReq, MediaArchive.getCatalogId());
		composite.addToSubTracker(MediaArchive.getCatalogId(), Asset);
		
		RelatedAssetTracker relatedtracker = loadAllRelatedAssets(inReq);
		
		for (Iterator iterator = relatedtracker.iterator(); iterator.hasNext();)
		{
			RelatedAsset item = (RelatedAsset) iterator.next();
			String catalogid = item.getRelatedToCatalogId();
			MediaArchive targetstore = getMediaArchive(catalogid);
			Asset target = targetstore.getAsset( item.getRelatedToAssetId());
			composite.ensureHasSubTracker(inReq, catalogid);
			composite.addToSubTracker(catalogid, target);
		}
		
		composite.setHitsName("allrelatedhits");
		composite.setCatalogId(MediaArchive.getCatalogId());
		
		inReq.putPageValue(composite.getHitsName(), composite);
		inReq.putSessionValue(composite.getSessionId(), composite);
		return composite;
	}
	
	public void removeRelatedAsset(WebPageRequest inRequest) throws OpenEditException
	{
		String sourceid = inRequest.getRequestParameter("assetid");
	
		String targetid = inRequest.getRequestParameter("targetid");
		String catalogid = inRequest.getRequestParameter("targetcatalogid");
		MediaArchive sourcestore = getMediaArchive(inRequest);
		if(catalogid == null)
		{
			catalogid = sourcestore.getCatalogId();
		}
		MediaArchive targetStore = getMediaArchive(catalogid);
		Asset target = targetStore.getAsset(targetid);
		target.removeRelatedAsset(sourcestore.getCatalogId(), sourceid);
		targetStore.saveAsset(target, inRequest.getUser());
		
		Asset source = sourcestore.getAsset(sourceid);
		source.removeRelatedAsset(targetStore.getCatalogId(),targetid);
		sourcestore.saveAsset(source, inRequest.getUser());
		
		inRequest.removePageValue("relatedhits");
		loadAllRelatedAssets(inRequest);
	}
//	
//	private Asset loadAsset(WebPageRequest inRequest) {
//		String sourcepath = inRequest.findValue("sourcepath");		
//		return getMediaArchive(inRequest).getAssetBySourcePath(sourcepath);
//		
//	}
//
//	public void removeRelatedAsset( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Asset Asset = editor.getCurrentAsset();
//		String[] assetIds = inRequest.getRequestParameters( "relatedid" );
//		for ( int i = 0; i < assetIds.length; i++ )
//		{
//			Asset.removeRelatedAsset( assetIds[i] );
//		}
//		editor.saveAsset( Asset );
//	}
//	
//	public void updateRelatedAssetIds( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Asset Asset = editor.getCurrentAsset();
//		
//		String[] assetIds = inRequest.getRequestParameters( "assetid" );
//		for ( int i = 0; i < assetIds.length; i++ )
//		{
//			String add = inRequest.getRequestParameter( assetIds[i] + ".value" );
//			if(add != null)
//			{
//			     Asset.addRelatedAssetId( assetIds[i] );
//			}
//			else 
//			{
//				Asset.removeRelatedAssetId(assetIds[i]);	
//			}
//		}
//		editor.saveAsset( Asset );
//	}
//	
//	public void relateAssetsInCategory( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Asset Asset = editor.getCurrentAsset();
//		String catalogid = inRequest.getRequestParameter("categoryid");
//		
//		MediaArchive MediaArchive = getMediaArchive(inRequest);
//		if(catalogid == null){
//			return;
//		}
//		Category cat = MediaArchive.getCategory(catalogid);
//		if(cat != null)
//		{
//			List assetList = MediaArchive.getAssetsInCatalog(cat);
//			for (Iterator iter = assetList.iterator(); iter.hasNext();) 
//			{
//				Asset current = (Asset) iter.next();
//				current.addRelatedAssetId( Asset.getId() );
//				editor.saveAsset( current );
//
//			}
//		}
//	
//	}
//	
//	public void unrelateAssetsInCategory( WebPageRequest inRequest ) throws OpenEditException
//	{
//		StoreEditor editor = getStoreEditor( inRequest );
//		Asset Asset = editor.getCurrentAsset();
//		String catalogid = inRequest.getRequestParameter("categoryid");
//		MediaArchive MediaArchive = getMediaArchive(inRequest);
//		if(catalogid == null)
//		{
//			return;
//		}
//		Category cat = MediaArchive.getCatalog(catalogid);
//		if(cat != null)
//		{
//			List assetList = MediaArchive.getAssetsInCatalog(cat);
//			for (Iterator iter = assetList.iterator(); iter.hasNext();)
//			{
//				Asset current = (Asset) iter.next();
//			    current.removeRelatedAssetId( Asset.getId() );
//				editor.saveAsset( current );		
//			}	
//		}
//	}
	
	
	
}
