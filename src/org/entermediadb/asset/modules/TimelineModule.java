package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.video.Block;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;

public class TimelineModule extends BaseModule
{
	public void loadChart(WebPageRequest inReq)
	{
		Asset asset = (Asset)inReq.getPageValue("asset");
		Double videolength = (Double)asset.getDouble("length");
		if( videolength == null)
		{
			return;
		}
		//divide into 60 blocks
		List blocks = new ArrayList();
		
		double chunck = videolength / 60;
		for (int i = 0; i < 60; i++)
		{
			Block block = new Block();
			//block.setTime(i * chunck);
			block.setLeft(i * 10);
			if( i % 5 == 0)
			{
				if( i == 0)
				{
					block.setLabel(0);
				}
				else
				{
					double rounded = ( videolength / (double)i);
					rounded = Math.round( videolength - rounded );
					block.setLabel(rounded);
				}	
			}
			blocks.add(block);
		}
		inReq.putPageValue("blocks", blocks);
	}
}
