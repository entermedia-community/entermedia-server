package modules.assets;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.locks.Lock
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.modules.AssetEditModule
import org.openedit.entermedia.scanner.MetaDataReader

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.util.PathUtilities

public class AdvancedAssetEditModule extends AssetEditModule{
	
	private static final Log log = LogFactory.getLog(AdvancedAssetEditModule.class);
	
	public void replacePrimaryAsset(WebPageRequest inReq) throws Exception
	{
		log.info("replacePrimary started");
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		List<Page> temppages = getUploadedPages(inReq);
		if( temppages.isEmpty() ){
			throw new OpenEditException("No uploads found");
		}
		if(!asset.isFolder()){
			makeFolderAsset(inReq);
		}
		String destination = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + asset.getSourcePath();
		//copy the temppages in to the originals folder, but first check if this is a folder based asset
		Page dest = getPageManager().getPage(destination);
		Page page = (Page) temppages.iterator().next();
		if(!page.exists()){
			throw new OpenEditException("Could not attach file: uploaded file doesn't exist [" + page.getPath()+"]");
		}
		dest.setProperty("makeversion","true");
		getPageManager().movePage(page, dest);
		asset = archive.getAssetBySourcePath(asset.getSourcePath());//why load again?
		asset.setPrimaryFile(page.getName());
		
		//start insert
		String primary = page.getName();
		String path = "/WEB-INF/data/"+archive.getCatalogId()+"/originals/"+asset.getSourcePath()+"/"+primary;
		MetaDataReader reader = (MetaDataReader) archive.getModuleManager().getBean("metaDataReader");
		Page content = archive.getPageManager().getPage(path);
		Asset tempasset = new Asset();
		reader.populateAsset(archive, content.getContentItem(), tempasset);
		int pages = getPages(tempasset);
		asset.setProperty("pages", String.valueOf(pages));
		String fileformat = tempasset.get("fileformat");
		if (fileformat!=null){
			asset.setProperty("fileformat",fileformat);
		} else {
			asset.setProperty("fileformat","default");
		}
		Data type = archive.getDefaultAssetTypeForFile(primary);
		if(type!=null){
			asset.setProperty("assettype", type.getId());
		} else {
			asset.setProperty("assettype", "none");
		}
		removeGenerated(archive,asset);
		//done insert
		
		Page media = archive.getOriginalDocument(asset);
		updateMetadata(archive, asset, media);
		asset.setProperty("editstatus", "1");
		asset.setProperty("importstatus", "reimported");
		asset.setProperty("previewstatus", "converting");
		archive.saveAsset(asset, null);
		
		String[] assetids = [asset.getId()] as String[];
		inReq.setRequestParameter("assetids",assetids);
		originalModified(inReq);
		getAttachmentManager().processAttachments(archive, asset, true);
		inReq.putPageValue("asset", asset);
	}
	
	protected int getPages(Asset inAsset){
		if (inAsset.get("pages")!=null){
			try{
				return Integer.parseInt(inAsset.get("pages"));
			}catch (Exception e){}
		}
		return 1;
	}
	
	protected void removeGenerated(MediaArchive archive, Asset asset){
		String sourcepath = asset.getSourcePath();
		log.info("removing generated files and clearing conversiontask entries of assetid = "+asset.getId()+", sourcepath = "+sourcepath);
		String rendertype = archive.getMediaRenderType(asset.getFileFormat());
		if (rendertype == null){
			log.info("Warning: unable to determine rendertype, aborting remove generated");
			return;
		}
		Lock lock = archive.getLockManager().lockIfPossible(archive.getCatalogId(), "assetconversions/" + asset.getSourcePath(), "admin");
		if (lock!=null){
			try{
				Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
				Searcher presetsearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "convertpreset");
				SearchQuery query  = searcher.createSearchQuery();
				query.addMatches("assetid",asset.getId());
				HitTracker hits = searcher.search(query);
				List<Data> todelete = new ArrayList<Data>();
				Iterator<?> itr = hits.iterator();
				while(itr.hasNext()){
					Data d = (Data) itr.next();
					String presetid = d.get("presetid");
					if (presetid == null){
						continue;
					}
					Data presetdata = (Data) presetsearcher.searchById(presetid);
					if (presetdata == null){
						continue;
					}
					String presetinputtype = presetdata.get("inputtype");
//					if (presetinputtype == rendertype){
//						continue;
//					}
					Data data = (Data) searcher.searchById(d.getId());
					if (data!=null) {
						todelete.add(data);
					}
				}
				for(Data data:todelete){
					searcher.delete(data, null);
				}
				log.info("removed "+todelete.size()+" conversion tasks from queue for "+asset.getId());
				archive.removeGeneratedImages(asset);
			}
			catch (Exception e){
				throw new OpenEditException(e.getMessage(),e);
			}
			finally{
				archive.releaseLock(lock);
			}
		} else {
			throw new OpenEditException("Unable to proceed: cannot get lock on conversion task for "+asset.getId());
		}
	}
	
	public void selectPrimaryAsset(WebPageRequest inReq) throws Exception
	{
		log.info("selectPrimaryAsset starting");
		
		String primaryname = inReq.getRequestParameter("filename");
		String imagefilename = inReq.getRequestParameter("imagefilename");
		MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
		if (!archive){
			archive = getMediaArchive(inReq);
		}
		if (!archive){
			String catalogid = inReq.findValue("catalogid");
			if (catalogid){
				archive = getMediaArchive(catalogid);
			}
		}
		if (!archive){
			throw new OpenEditException("Archive is null");
		}
		String assetid = inReq.getRequestParameter("assetid");
		if (!assetid){
			throw new OpenEditException("Unable to find assetid");
		}
		Asset target = archive.getAsset(assetid);
		String ext = PathUtilities.extractPageType(primaryname);
		if (target != null)
		{
			if(ext != null)
			{
				target.setProperty("fileformat", ext.toLowerCase());
			}
			if(primaryname != null)
			{
				target.setPrimaryFile(PathUtilities.extractFileName(primaryname));
			}
			if(imagefilename != null)
			{
				target.setProperty("imagefile", PathUtilities.extractFileName(imagefilename));
			}
			Page itemFile = archive.getOriginalDocument(target);
			
			//start insert
			String primary = target.getPrimaryFile();
			String path = "/WEB-INF/data/"+archive.getCatalogId()+"/originals/"+target.getSourcePath()+"/"+primary;
			MetaDataReader reader = (MetaDataReader) archive.getModuleManager().getBean("metaDataReader");
			Page content = archive.getPageManager().getPage(path);
			Asset tempasset = new Asset();
			reader.populateAsset(archive, content.getContentItem(), tempasset);
			int pages = getPages(tempasset);
			target.setProperty("pages", String.valueOf(pages));
			String fileformat = tempasset.get("fileformat");
			if (fileformat!=null){
				target.setProperty("fileformat",fileformat);
			} else {
				target.setProperty("fileformat","default");
			}
			Data type = archive.getDefaultAssetTypeForFile(primary);
			if(type!=null){
				target.setProperty("assettype", type.getId());
			} else {
				target.setProperty("assettype", "none");
			}
			removeGenerated(archive,target);
			//done insert
			
			updateMetadata(archive, target, itemFile);
			target.setProperty("previewstatus", "converting");
			archive.saveAsset(target, inReq.getUser());
			String[] assetids = [target.getId()] as String[];
			inReq.setRequestParameter("assetids",assetids);
			originalModified(inReq);
			getAttachmentManager().processAttachments(archive, target, true);
			inReq.putPageValue("asset", target);
		}
	}
}