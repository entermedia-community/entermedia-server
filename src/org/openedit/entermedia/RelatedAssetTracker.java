package org.openedit.entermedia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.openedit.hittracker.ListHitTracker;

public class RelatedAssetTracker extends ListHitTracker
{
	private static final long serialVersionUID = 1L;

	public List getRelatedByType(String inType)
	{
		ArrayList list = new ArrayList();
		for (Iterator iterator = iterator(); iterator.hasNext();)
		{
			RelatedAsset asset = (RelatedAsset) iterator.next();
			if (inType.equals(asset.getType()))
			{
				list.add(asset);
			}

		}
		return list;
	}

}
