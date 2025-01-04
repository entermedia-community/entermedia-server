package org.entermediadb.asset.publishing;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.page.Page;

public abstract class BasePublisher implements Publisher 
{
	public BasePublisher()
	{
		
	}
	
	private static final Log log = LogFactory.getLog(BasePublisher.class);

	protected void publishFailure(MediaArchive mediaArchive,Data inOrderItem, String inMessage) {
		inOrderItem.setProperty("errormessage", inMessage);
		inOrderItem.setProperty("status", "error");
	}
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, Data inPreset) {
		String transcodeid = inPreset.get("transcoderid");
		if( "original".equals( transcodeid ) )
		{
			return mediaArchive.getOriginalDocument(asset);
		}
		String input= "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + inPreset.get("generatedoutputfile");
		Page inputpage= mediaArchive.getPageManager().getPage(input);
		return inputpage;
	}
	/*
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, String presetid) {
		if( presetid == null) {
			return mediaArchive.getOriginalDocument(asset);
		}
		Data preset = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "convertpreset", presetid);
		return findInputPage(mediaArchive,asset,(Data)preset);
	}
	 */
	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem, Data inDestination, List inPresets, Asset inAsset)
	{
		PublishResult result = new PublishResult();
		for (Iterator iterator = inPresets.iterator(); iterator.hasNext();) {
			Data preset = (Data) iterator.next();
			result = publish(mediaArchive, inOrder, inOrderItem, inDestination, preset, inAsset);
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
			found = (Data)searcher.searchById(found.getId());
		}
		return found;
	}
	protected void saveConversionTask(MediaArchive mediaArchive, Data inTask)
	{
		Searcher searcher = mediaArchive.getSearcher("conversiontask");
		searcher.saveData(inTask, null); //locking?
	}
	protected PublishResult checkOnConversion(MediaArchive mediaArchive, Data inOrderItem, Asset inAsset, Data inPreset)
	{
		PublishResult result = new PublishResult();
		String status = inOrderItem.get("publishstatus");
		if( "0".equals(inPreset.getId()))// || isremote)
		{
			result.setReadyToPublish(true);
			long size = mediaArchive.getPresetManager().getLengthOfOutForPreset(mediaArchive, inAsset, inPreset);
			inOrderItem.setValue("downloaditemtotalfilesize", size);
			return result;
		}
		else if("new".equals(status))
		{
			//make sure conversions task exists and are marked as new (not error)
			Data conversiontask = loadConversionTask(mediaArchive, inAsset, inPreset.getId());
			String cstatus = conversiontask.get("status");
			if(  "error".equals(cstatus) || "new".equals(cstatus))
			{
				conversiontask.setProperty("status","retry");
				saveConversionTask(mediaArchive,conversiontask);
			}
			if( "retry".equals(cstatus) || "new".equals(cstatus))
			{
				mediaArchive.fireSharedMediaEvent("conversions/runconversions");				
			}
			//then return pending so we only do this once per publish
			result.setPending(true);
			return result;
		}
		else
		{
			Data conversiontask = loadConversionTask(mediaArchive, inAsset, inPreset.getId());
			String cstatus = conversiontask.get("status");
			if( "error".equals(cstatus)) //missinginput is ok
			{
				result.setErrorMessage(inPreset.getName()  + " conversion had an error: " + conversiontask.get("errordetails"));
				return result;
			}
			else if( !"complete".equals(cstatus)) 
			{
				result.setPending(true);
				return result;
			}
			result.setReadyToPublish(true);
			long size = mediaArchive.getPresetManager().getLengthOfOutForPreset(mediaArchive, inAsset, inPreset);
			inOrderItem.setValue("downloaditemtotalfilesize", size);
			return result;
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