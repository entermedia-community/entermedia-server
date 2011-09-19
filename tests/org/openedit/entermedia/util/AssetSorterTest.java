package org.openedit.entermedia.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openedit.entermedia.Asset;

import com.openedit.hittracker.ListHitTracker;

public class AssetSorterTest
{

	@Test
	public void testCompare()
	{
		List idList = new ArrayList();
		idList.add("1");
		idList.add("2");
		idList.add("3");
		List<Asset> assets = new ArrayList<Asset>();
		Asset a1 = new Asset(); 
		a1.setId("1");
		Asset a2 = new Asset(); 
		a2.setId("2");
		Asset a3 = new Asset(); 
		a3.setId("3");
		assets.add(a3);
		assets.add(a1);
		assets.add(a2);
		
		Collections.sort(assets, new AssetSorter(idList));
		ListHitTracker hitTracker = new ListHitTracker(null);
		assertEquals("1", assets.get(0).getId());
		assertEquals("2", assets.get(1).getId());
		assertEquals("3", assets.get(2).getId());
	}

}
