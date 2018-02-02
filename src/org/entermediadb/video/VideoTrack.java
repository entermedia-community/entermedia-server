package org.entermediadb.video;

import org.entermediadb.asset.util.MathUtils;
import org.openedit.data.BaseData;

public class VideoTrack extends BaseData
{
	
	public String formatTime(long inTime)
	{
		return MathUtils.toDuration(inTime);
	}
}
