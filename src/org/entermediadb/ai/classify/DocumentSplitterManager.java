package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.asset.Asset;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.JSONParser;

public class DocumentSplitterManager extends InformaticsProcessor 
{
	private static final Log log = LogFactory.getLog(DocumentSplitterManager.class);

	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		//Do nothing
		
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRecords)
	{
		String searchtype = inConfig.get("searchtype");

		for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();
			
			String moduleid = entity.get("entitysourcetype");
			
			if( !searchtype.equals(moduleid) )
			{
				continue;
			}
			
			String assetid = entity.get("primarymedia");
			if( assetid == null)
			{
				assetid = entity.get("primaryimage");
			}
			if( assetid == null)
			{
				continue;
			}
			
			Asset document = getMediaArchive().getAsset(assetid);
			String rendertype = getMediaArchive().getMediaRenderType(document.getFileFormat());
			if(rendertype == null || !rendertype.equals("document"))
			{
				continue;
			}
			String created = entity.get("pagescreatedfor");
			if( created != null)
			{
				//Check everything
				String modtime = document.get("assetmodificationdate");
				if( created.equals( assetid + "|" + modtime) )
				{
					continue;
				}
			}
			splitDocument(inConfig, entity, document);
			String modtime = document.get("assetmodificationdate");
			entity.setValue("pagescreatedfor", assetid + "|" + modtime);
		}
		//Check the primarymedia
		//See if this has been indexed or not
	}

	public void splitDocument(MultiValued inConfig, MultiValued inEntity, Asset asset) 
	{
//		
//		Data entitydoc = getMediaArchive().query("entitydocument").exact("parentasset", inAssetId).searchOne();
//		
//		if(entitydoc == null)
//		{
//			entitydoc = archive.getSearcher("entitydocument").createNewData();
//			String entitydocname = asset.getName().replaceAll("\\..*$", "");
//			if(entitydocname.length() == 0) {
//				entitydocname = asset.getName();
//			}
//			entitydoc.setName(entitydocname);
//			entitydoc.setValue("parentasset", inAssetId);
//			entitydoc.setValue("entity_date", new Date());
//			archive.saveData("entitydocument", entitydoc);
//		}

		String fulltext = (String) asset.getValue("fulltext");

		if(fulltext == null || fulltext.trim().isEmpty())
		{
			log.info("No full text found");
			return;
		}
		
		JSONParser parser = new JSONParser();
		Collection pagesFulltext = parser.parseCollection(fulltext);

		String parentsearchtype = inConfig.get("searchtype");
		String generatedsearchtype = inConfig.get("generatedsearchtype");
		HitTracker existingPages = getMediaArchive().query(generatedsearchtype)
				.exact(parentsearchtype, inEntity.getId())
				.exact("parentasset", asset.getId()).search();

		List<Data> tosave = new ArrayList();
		int pagenum = 0;
		
		Searcher pageSearcher = getMediaArchive().getSearcher(generatedsearchtype);

		for (Iterator iterator = pagesFulltext.iterator(); iterator.hasNext();) {
			pagenum++;

			String pageText = (String) iterator.next();

			Data docpage = null;
			for (Iterator iterator2 = existingPages.iterator(); iterator2.hasNext();) {
				Data existingPage = (Data) iterator2.next();
				Object pageval = existingPage.getValue("pagenum");
				if( pageval == null || !(pageval instanceof Number) )
				{
					continue;
				}
				int p = (int) ((Number)pageval).doubleValue();
				if( p == pagenum)
				{
					docpage = existingPage;
					break;
				}
			}

			if(docpage != null)
			{
				docpage.setValue("taggedbyllm", false);
				docpage.setValue("semanticindexed", false);
				docpage.setValue("semantictopics", null);
			}
			else
			{
				docpage = pageSearcher.createNewData();
				String pagename = inEntity.getName() + " - Page " + pagenum;
				docpage.setName(pagename);
			}

			docpage.setValue("pagenum", pagenum);
			docpage.setValue("longcaption", pageText);
			docpage.setValue(parentsearchtype, inEntity.getId());
			docpage.setValue("primaryimage", asset.getId());
			docpage.setValue("parentasset", asset.getId());
			docpage.setValue("entity_date", new Date());

			tosave.add(docpage);

			if(tosave.size() > 20)
			{
				pageSearcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		pageSearcher.saveAllData(tosave, null);
		getMediaArchive().fireSharedMediaEvent("llm/addmetadata");
	}

	
}
