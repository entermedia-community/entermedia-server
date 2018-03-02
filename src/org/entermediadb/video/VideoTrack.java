package org.entermediadb.video;

import java.util.Map;

import org.entermediadb.asset.util.MathUtils;
import org.openedit.data.BaseData;

public class VideoTrack extends BaseData
{
	
	public String formatTime(long inTime)
	{
		return MathUtils.toDuration(inTime);
	}
	public String formatEnd(Map inClip)
	{
		Long start = (Long)inClip.get("timecodestart");
		Long length = (Long)inClip.get("timecodelength");
		return MathUtils.toDuration(start + length);
	}

}
