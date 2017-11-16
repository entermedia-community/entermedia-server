package org.entermediadb.asset.convert;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.locks.Lock;
import org.openedit.users.User;

public class AssetConversions implements Runnable
	{
	 	String fieldAssetId;
	 	MediaArchive fieldMediaArchive;
	 	List<ConversionTask> runners = new ArrayList<ConversionTask>();
	 	User user;
	 	ScriptLogger log;
	 	boolean fieldCompleted;
	 	Lock fieldLock;
	 	
	 	public Lock getLock()
		{
			return fieldLock;
		}
		public void setLock(Lock inLock)
		{
			fieldLock = inLock;
		}
		public boolean hasComplete()
	 	{
	 		return fieldCompleted;
	 	}
	 	public AssetConversions()
		{
			// TODO Auto-generated constructor stub
		}
	 	public AssetConversions(MediaArchive inArchive, String inAssetId, Lock lock)
	 	{
	 		fieldMediaArchive = inArchive;
	 		fieldAssetId = inAssetId;
	 		fieldLock = lock;
	 	}
	 	
	 	public void run()
	 	{
	 		Lock lock = fieldMediaArchive.getLockManager().lockIfPossible("assetconversions/" + fieldAssetId, "CompositeConvertRunner.run");
	 		
	 		if( lock == null)
	 		{
	 			log.info("asset already being processed ${fieldAssetId}");
	 			return;
	 		}
	 		Asset asset = fieldMediaArchive.getAsset(fieldAssetId);
	 		try
	 		{
	 			for( ConversionTask runner: runners )
	 			{
	 				runner.asset = asset;
	 				runner.convert();
	 				if( runner.isComplete())
	 				{
	 					fieldCompleted = true;
	 					//Slow? fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
	 				}
	 				if( runner.isError())
	 				{
	 					fieldCompleted = true;
	 					//fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
	 					break;
	 				}
	 			}
	 		}
	 		catch(Exception e){
	 			log.error("ERRORS ${fieldAssetId}");
	 		}
	 		finally
	 		{
	 			fieldMediaArchive.releaseLock(lock);
	 			
	 			if( hasComplete())
	 			{
	 				fieldMediaArchive.conversionCompleted(asset);
	 				fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
	 			}
	 		}
	 	}

	 	public String getAssetId()
		{
			return fieldAssetId;
		}
	 	public void addTask(ConversionTask task )
	 	{
	 		runners.add(task);
	 	}
 }
