package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

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
		int count = 0;
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
			
			Asset asset = getMediaArchive().getAsset(assetid);
			String rendertype = getMediaArchive().getMediaRenderType(asset.getFileFormat());
			if(rendertype == null || !rendertype.equals("document"))
			{
				continue;
			}
			String created = entity.get("pagescreatedfor");
			if( created != null)
			{
				//Check everything
				String modtime = asset.get("assetmodificationdate");
				if( created.equals( assetid + "|" + modtime) )
				{
					continue;
				}
			}
			if(count == 0)
			{
				inLog.headline("Splitting " + inRecords.size() + " documents"); 
			}
			count++;
			
			inLog.info("Splitting " + asset.getValue("pages") + " pages in document " + asset.getName());
			entity.setValue("totalpages", asset.getValue("pages"));
			splitDocument(inLog, inConfig, entity, asset);
			String modtime = asset.get("assetmodificationdate");
			entity.setValue("pagescreatedfor", assetid + "|" + modtime);

			getMediaArchive().fireSharedMediaEvent("llm/addmetadata");
		}
		//Check the primarymedia
		//See if this has been indexed or not
	}

	public void splitDocument(ScriptLogger inLog, MultiValued inConfig, MultiValued inEntity, Asset asset) 
	{

		String parentsearchtype = inConfig.get("searchtype");
		String generatedsearchtype = inConfig.get("generatedsearchtype");
		HitTracker existingPages = getMediaArchive().query(generatedsearchtype)
				.exact(parentsearchtype, inEntity.getId())
				.exact("parentasset", asset.getId()).search();
		
		Map<Integer, MultiValued> pagenums = new HashMap();
		
		for (Iterator iterator = existingPages.iterator(); iterator.hasNext();)
		{
			MultiValued object = (MultiValued) iterator.next();
			pagenums.put(object.getInt("pagenum"), object);
			
		}
				

		List<Data> tosave = new ArrayList();
		int totalpages = inEntity.getInt("totalpages");
		
		Searcher pageSearcher = getMediaArchive().getSearcher(generatedsearchtype);
		
		Long starttime = System.currentTimeMillis();
		if(inConfig.getBoolean("generatemarkdown"))
		{
			log.info("Generating markdown for " + totalpages + " pages of " + inEntity);
		}

		
		for (int i = 0; i < totalpages; i++) 
		{
			int pagenum = i + 1;
			MultiValued docpage = pagenums.get(pagenum);
			
			if( docpage == null)
			{
				docpage = (MultiValued) pageSearcher.createNewData();
				String pagename = inEntity.getName() + " - Page " + pagenum;
				docpage.setName(pagename);
				docpage.setValue("pagenum", pagenum);
				docpage.setValue(parentsearchtype, inEntity.getId());
				docpage.setValue("primaryimage", asset.getId());
				docpage.setValue("parentasset", asset.getId());
				docpage.setValue("entity_date", new Date());
			}
			
			if((!"embedded".equals(inEntity.get("entityembeddingstatus")) || docpage.get("markdowncontent") == null) && inConfig.getBoolean("generatemarkdown"))
			{
				log.info("Generating markdown for page: " + docpage);
				generateMarkdown(docpage);
			}

			tosave.add(docpage);

			if(tosave.size() > 20)
			{
				pageSearcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		Long endtime = System.currentTimeMillis();
		inLog.info("Generated: " + totalpages + " pages for:" + inEntity + " in: " + (endtime - starttime)/1000L + "s");
		pageSearcher.saveAllData(tosave, null);
	}
	
	public void generateMarkdown(MultiValued pageEntity) 
	{
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("generateMarkdown");

		String base64Img = loadDocumentContent(pageEntity);
		
		if(base64Img == null)
		{
			log.error("No image found for page: " + pageEntity);
			return;
		}
			
		LlmResponse result = (LlmResponse) llmconnection.callOCRFunction(new HashMap(), base64Img);
		String markdown = result.getMessage();
			
		pageEntity.setValue("markdowncontent", markdown);
	}
	
	protected String loadDocumentContent(MultiValued inEntity)
	{
		String base64EncodedString = null;
		if(inEntity.hasValue("pagenum") )
		{
			String parentasset = inEntity.get("parentasset");
			if(parentasset != null)
			{
				Asset parentAsset = getMediaArchive().getAsset(parentasset);
				//Do the conversion with page number in it
				Map params = new HashMap();
				params.put("pagenum",inEntity.get("pagenum") );
				ConvertResult result = getMediaArchive().getTranscodeTools().createOutputIfNeeded(null,params,parentAsset.getSourcePath(), "image3000x3000.webp"); 
				if( result.isOk() )
				{
					base64EncodedString = loadBase64Png(result.getOutput());
				}
				else
				{
					log.error("Could not convert document page thumbnail for asset: " + parentasset);
				}
			}
		}
		else
		{
			String primarymedia = inEntity.get("primarymedia");
			Asset inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
			if(inPrimaryAsset == null)
			{
				primarymedia = inEntity.get("primaryimage");
				inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
			}
			if(inPrimaryAsset != null)
			{
				base64EncodedString = loadBase64Png(inPrimaryAsset, "image3000x3000");
			}
			else
			{
				log.error("Could not find primary asset for entity: " + inEntity.getId());
			}
		}
		
		return base64EncodedString;
	}
	
}
