package org.entermediadb.modules.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.openedit.Data;

public class UserChats
{
	String fieldUserId;

	public String getUserId()
	{
		return fieldUserId;
	}

	public void setUserId(String inUserId)
	{
		fieldUserId = inUserId;
	}

	Map fieldAssetMessages = new HashMap();
	Map fieldAssets = new HashMap();

	public Map getAssetMessages()
	{
		return fieldAssetMessages;
	}

	public void setAssetMessages(Map inAssetMessages)
	{
		fieldAssetMessages = inAssetMessages;
	}

	public Collection getAssets()
	{
		return fieldAssets.values();
	}

	public void addAssetMessage(Asset inAsset, Data inMessage)
	{
		Asset existing = (Asset) fieldAssets.get(inAsset.getId());
		if (existing == null)
		{
			fieldAssets.put(inAsset.getId(), inAsset);
		}
		Collection existingmessages = (Collection) fieldAssetMessages.get(existing.getId());
		if (existingmessages == null)
		{
			existingmessages = new ArrayList();
			fieldAssetMessages.put(inAsset.getId(), existingmessages);
		}
		for (Iterator iterator = existingmessages.iterator(); iterator.hasNext();)
		{
			Data message = (Data) iterator.next();
			if (message.getId().equals(inMessage.getId()))
			{
				return;
			}
		}
		existingmessages.add(inMessage);
	}

	public Collection getMessages(Data inAsset)
	{
		return getMessages(inAsset.getId());
	}

	public Collection getMessages(String inAssetId)
	{
		Collection existingmessages = (Collection) fieldAssetMessages.get(inAssetId);
		//TODO: Sort by date
		return existingmessages;
	}

}
