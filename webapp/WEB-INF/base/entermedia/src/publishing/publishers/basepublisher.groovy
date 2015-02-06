package publishing.publishers
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*

import com.openedit.page.Page

public abstract class basepublisher implements Publisher {
	private static final Log log = LogFactory.getLog(basepublisher.class);

	protected publishFailure(MediaArchive mediaArchive,Data inPublishRequest, String inMessage) {
		inPublishRequest.setProperty("errormessage", inMessage);
		inPublishRequest.setProperty("status", "error");
	}
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, Data inPreset) {
		if( inPreset.get("type") == "original") {
			return mediaArchive.getOriginalDocument(asset);
		}
		String input= "/WEB-INF/data/${mediaArchive.catalogId}/generated/${asset.sourcepath}/${inPreset.outputfile}";
		Page inputpage= mediaArchive.getPageManager().getPage(input);
		return inputpage;
	}
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, String presetid) {
		if( presetid == null) {
			return mediaArchive.getOriginalDocument(asset);
		}
		Data preset = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "convertpreset", presetid);
		return findInputPage(mediaArchive,asset,(Data)preset);
	}


	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, List inPresets) {
		PublishResult result = new PublishResult();
		for (Iterator iterator = inPresets.iterator(); iterator.hasNext();) {
			Data preset = (Data) iterator.next();
			result = publish(mediaArchive, inAsset, inPublishRequest, inDestination, preset);
		}
		return result;//should check all of these?
	}
	
	protected Data loadConversionTask(MediaArchive mediaArchive, Asset inAsset, String inPreset)
	{
		Searcher searcher = mediaArchive.getSearcher("conversiontask");
		Data found = searcher.query().match("assetid",inAsset.getId()).match("presetid",inPreset).searchOne();
		if( found == null)
		{
			found = searcher.createNewData();
			//save it
			found.setProperty("presetid",inPreset);
			found.setProperty("assetid",inAsset.getId());
			found.setProperty("status","new");
			found.setSourcePath(inAsset.getSourcePath()); // this is important for conversions to work
			searcher.saveData(found,null);
		}
		else
		{
			found = searcher.searchById(found.getId());
		}
		return found;
	}
	protected void saveConversionTask(MediaArchive mediaArchive, Data inTask)
	{
		Searcher searcher = mediaArchive.getSearcher("conversiontask");
		searcher.saveData(inTask, null); //locking?
	}
	protected PublishResult checkOnConversion(MediaArchive mediaArchive, Data inPublishRequest, Asset inAsset, Data inPreset)
	{
		if( inPreset.getId() == "0")// || isremote)
		{
			return null;
		}
		else if(inPublishRequest.get("status") == "new")
		{
			//make sure conversions task exists and are marked as new (not error)
			Data conversiontask = loadConversionTask(mediaArchive, inAsset, inPreset.getId());
			if( conversiontask.get("status") == "error" || conversiontask.get("status") == "new")
			{
				conversiontask.setProperty("status","retry");
				saveConversionTask(mediaArchive,conversiontask);
			}
			if( conversiontask.get("status") == "retry" || conversiontask.get("status") == "new")
			{
				mediaArchive.fireSharedMediaEvent("conversions/runconversions");				
			}
			//then return pending so we only do this once per publish
			PublishResult result = new PublishResult();
			result.setPending(true);
			return result;
		}
		else
		{
			PublishResult result = new PublishResult();
			Data conversiontask = loadConversionTask(mediaArchive, inAsset, inPreset.getId());
			if( conversiontask.get("status") == "error") //missinginput is ok
			{
				result.setErrorMessage(inPreset.getName()  + " conversion had an error: " + conversiontask.get("errordetails"));
				return result;
			}
			else if( conversiontask.get("status") != "complete") 
			{
				result.setPending(true);
				return result;
			}
			//must be complete, return null
			return null;
		}
		
		//		result = checkOnRemoteConversion()
		//		//If is remote then the file might not be here
		//		String isremote = destination.get("remotempublish");
		//		if( Boolean.parseBoolean(isremote) )
		//		{
		//			//see if it showed up yet
		//			if( !inputpage.exists() )
		//			{
		//				result.setPending(true);
		//				return result;
		//			}
		//		}
				
	}


	
}