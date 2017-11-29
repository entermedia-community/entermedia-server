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
	
	Asset fieldAsset;
	public Asset getAsset()
	{
		return fieldAsset;
	}

	public void setAsset(Asset inAsset)
	{
		fieldAsset = inAsset;
	}

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

	public AssetConversions(MediaArchive inArchive, Lock lock)
	{
		fieldMediaArchive = inArchive;
		fieldLock = lock;
	}

	public void run()
	{
		try
		{
			for (ConversionTask runner : runners)
			{
				runner.asset = getAsset();
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
			log.error("ERRORS converting: " + getAssetId() ,e);
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
		return getAsset().getId();
	}

	public void addTask(ConversionTask task)
	{
		log.info("Added assetid:" + task.hit.get("assetid") + " presetid:" + task.hit.get("presetid") + " status:" + task.hit.get("status"));
		runners.add(task);
	}
}
