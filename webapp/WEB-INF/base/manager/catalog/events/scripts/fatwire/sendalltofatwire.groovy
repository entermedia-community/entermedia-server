package fatwire;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.Data;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathProcessor;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.HitTracker;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.scanner.AssetImporter;
import com.openedit.util.XmlUtil;
import org.openedit.repository.ContentItem;
import com.openedit.entermedia.scripts.ScriptLogger;
import java.security.MessageDigest;

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.entermedia.scripts.ScriptLogger;
import com.openedit.page.Page
import com.openedit.servlet.OpenEditEngine
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.modules.OrderModule
import org.openedit.entermedia.orders.Order

import org.openedit.util.DateStorageUtil


public void init()
{
	log.info("Start \"Send All to Fatwire\"");
	
	//get mediaarchive
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	//setup searchers
	SearcherManager sm = archive.getSearcherManager();
	Searcher presetsearch = sm.getSearcher(archive.getCatalogId(), "convertpreset");
	Searcher publishqueuesearch = sm.getSearcher(archive.getCatalogId(), "publishqueue");
	Searcher conversionsearch = sm.getSearcher(archive.getCatalogId(), "conversiontask");
	Searcher publishdestinationsearch = sm.getSearcher(archive.getCatalogId(), "publishdestination");
	
	//get form data from page
	//regionid
	String regionId = context.findValue("regionid");
	if (regionId == null)
	{
		//get default regionid if available (failsafe)
		Searcher fatwireregionsearch = sm.getSearcher(archive.getCatalogId(), "fatwireregion");
		Data defaultfr = fatwireregionsearch.searchByField("default", "true");
		if (defaultfr!=null)
		{
			regionId = defaultfr.getId();
		}
	}
	if (regionId == null)
	{
		log.info("Unable to process request: cannot find a regionid, aborting");
		return;
	}
	//assetid
	String assetId = context.findValue("assetid");
	if (assetId == null)
	{
		log.info("Unable to process request: asset is null, aborting");
		return;
	}
	Asset asset = archive.getAsset(assetId);//get the asset from the asset id
	if (asset==null)
	{
		log.info("Unable to process request: cannot find an asset with id=${assetId}, aborting");
		return;
	}
	//add to context
	context.putPageValue("asset",asset);
	
	//execute searches
	SearchQuery fatwirequery = publishdestinationsearch.createSearchQuery().append("name", "FatWire");
	Data fatwireData = publishdestinationsearch.searchByQuery(fatwirequery);
	String fatwireId = fatwireData.getId();//this is required because we're passing a fatwire publication task to the publication queue
	
	SearchQuery presetquery = presetsearch.createSearchQuery().append("publishtofatwire", "true");//look for all publishtofatwire values set to true
	HitTracker presethits = presetsearch.search(presetquery);
	if (presethits.size() == 0)
	{
		log.info("Unable to process request: no fatwire presets have been defined, aborting");
		return;
	}
	presethits.each{
		Data preset = it;
		//get a few key variables
		String outputfile = preset.get("outputfile");
		String exportname = archive.asExportFileName(null, asset, preset);
		
		//create conversion task if necessary
		boolean needstobecreated = true;
		String conversiontaskid = null;
		if( "original".equals( preset.get("type") ) )
		{
			needstobecreated = !archive.getOriginalDocument(asset).exists();
		}
		else 
		{
			if( !archive.doesAttachmentExist(outputfile, asset) ){
				log.info("send to fatwire: $outputfile for $asset (${asset.id}) does not exist, skipping");
				return;
			}
//			needstobecreated = false;
		}
		if( needstobecreated )
		{
			SearchQuery q = conversionsearch.createSearchQuery().append("assetid", asset.getId()).append("presetid",preset.getId());
			Data newTask = conversionsearch.searchByQuery(q);
			if( newTask == null )
			{
				newTask = conversionsearch.createNewData();
				newTask.setProperty("status", "new");
				newTask.setProperty("assetid", asset.getId());
				newTask.setProperty("presetid", preset.getId());
				newTask.setSourcePath(asset.getSourcePath());
				conversionsearch.saveData(newTask, null);
			}
			conversiontaskid = newTask.getId();
		}
		//add entry to publishqueue
		Data data = publishqueuesearch.createNewData();
		data.setProperty("assetid", assetId);
		data.setProperty("assetsourcepath", asset.getSourcePath());
		data.setProperty("publishdestination", fatwireId);
		data.setProperty("exportname", exportname);
		data.setProperty("presetid", preset.getId());
		data.setProperty("status", "new");
		data.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		String homeUrl = "${context.siteRoot}${home}${apphome}";
		data.setProperty("homeurl",homeUrl);
		String username = context.getUserName();
		data.setProperty("username",username);
		data.setProperty("convertpresetoutputfile",outputfile);
		data.setProperty("regionid", regionId);
		//add reference to conversiontask id if one was created
		if (needstobecreated)
		{
			data.setProperty("conversiontaskid", conversiontaskid );
		}
		publishqueuesearch.saveData(data, null);//save to publishqueue	
	}
	//trigger publishing queue
	archive.fireSharedMediaEvent("publishing/publishassets");
	//done message
	log.info("Finished \"Send All to Fatwire\"");
}


init();