package org.entermediadb.asset.convert;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.locks.Lock;
import org.openedit.users.User;

public class AssetConversions implements Runnable
{
	private static final Log log = LogFactory.getLog(AssetConversions.class);
	
	String fieldAssetId;
	MediaArchive fieldMediaArchive;
	List<ConversionTask> runners = new ArrayList<ConversionTask>();
	User user;
	boolean fieldCompleted;
	Lock fieldLock;
	ConversionEventListener fieldEventListener;

	public ConversionEventListener getEventListener()
	{
		return fieldEventListener;
	}

	public void setEventListener(ConversionEventListener fieldEventListener)
	{
		this.fieldEventListener = fieldEventListener;
	}

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
	}

	public AssetConversions(MediaArchive inArchive, String inAssetId, Lock lock)
	{
		fieldMediaArchive = inArchive;
		fieldAssetId = inAssetId;
		fieldLock = lock;
	}

	public void run()
	{
		Asset asset = fieldMediaArchive.getAsset(fieldAssetId);
		try
		{
			for (ConversionTask runner : runners)
			{
				runner.asset = asset;
				runner.convert();
				if (runner.isComplete())
				{
					fieldCompleted = true;
					//Slow? fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
				}
				if (runner.isError())
				{
					fieldCompleted = true;
					//fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
					break;
				}
			}
		}
		catch (Exception e)
		{
			log.error("ERRORS converting: " + fieldAssetId ,e);
		}
		finally
		{
			if (hasComplete())
			{
				getEventListener().finishedConversions(this);
			}
			else
			{
				getEventListener().ranConversions(this);
			}
		}
	}

	public String getAssetId()
	{
		return fieldAssetId;
	}

	public void addTask(ConversionTask task)
	{
		log.info("Added: " + task.hashCode() + " " + task.hit.get("status"));
		runners.add(task);
	}
}
