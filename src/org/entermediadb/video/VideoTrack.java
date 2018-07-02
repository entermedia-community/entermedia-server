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
		Number start = (Number)inClip.get("timecodestart");
		Number length = (Number)inClip.get("timecodelength");
		return MathUtils.toDuration(toLong(start) + toLong(length));
	}

	
}
