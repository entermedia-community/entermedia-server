package org.entermediadb.events;

import org.entermediadb.scripts.ScriptLogger;
import org.openedit.WebPageRequest;

public class EventTrigger
{
	public PathEvent getPathEvent()
	{
		return fieldPathEvent;
	}
	public void setPathEvent(PathEvent inPathEvent)
	{
		fieldPathEvent = inPathEvent;
	}
	public WebPageRequest getWebPageRequest()
	{
		return fieldWebPageRequest;
	}
	public void setWebPageRequest(WebPageRequest inWebPageRequest)
	{
		fieldWebPageRequest = inWebPageRequest;
	}
	public ScriptLogger getLogger()
	{
		return fieldLogger;
	}
	public void setLogger(ScriptLogger inLogger)
	{
		fieldLogger = inLogger;
	}
	PathEvent fieldPathEvent;
	WebPageRequest fieldWebPageRequest;
	ScriptLogger fieldLogger;
	
}
