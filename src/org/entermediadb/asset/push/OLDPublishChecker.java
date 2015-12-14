package org.entermediadb.asset.push;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.entermediadb.asset.MediaArchive;
import org.openedit.ModuleManager;

public class OLDPublishChecker
{
	protected Timer fieldTimer;
	protected PushManager fieldPushManager;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected List<String> fieldCatalogIds;
	
	public PushManager getPushManager()
	{
		return fieldPushManager;
	}

	public void setPushManager(PushManager inPushManager)
	{
		fieldPushManager = inPushManager;
	}

	public List<String> getCatalogIds()
	{
		if (fieldCatalogIds == null)
		{
			fieldCatalogIds = new ArrayList<String>();
		}

		return fieldCatalogIds;
	}

	public void setCatalogIds(List inCatalogIds)
	{
		fieldCatalogIds = inCatalogIds;
	}

	protected Timer getTimer()
	{
		if (fieldTimer == null)
		{
			fieldTimer = new Timer("PublishChecker",true);
		}
		return fieldTimer;
	}

	public void reload()
	{
		if( fieldTimer != null )
		{
			fieldTimer.cancel();
			fieldTimer = null;
		}
		for (Iterator iterator = getCatalogIds().iterator(); iterator.hasNext();)
		{
			String catalogid = (String) iterator.next();
			addCatalogToMonitor(catalogid);
		}
	}
	public MediaArchive getMediaArchive(String inCatalogid)
	{
		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
	}
	
	public void addCatalogToMonitor(final String inCatalogid)
	{
		if( !getCatalogIds().contains(inCatalogid))
		{
			getCatalogIds().add(inCatalogid);
			
			TimerTask task = new TimerTask()
			{
				@Override
				public void run()
				{
					try
					{
						MediaArchive archive = getMediaArchive(inCatalogid);
						getPushManager().pollRemotePublish(archive);
					} catch ( Throwable ex )
					{
						ex.printStackTrace();
					}
				}
			};
			
			MediaArchive archive = getMediaArchive(inCatalogid);
			String p = archive.getCatalogSettingValue("push_pollpublish_period");
			int period = 5000;
			if( p != null )
			{
				period = Integer.parseInt(p);
			}
			getTimer().schedule(task, period,period);
		}
	}
	
}
