package org.entermediadb.ai.assistant;

import java.util.Collection;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;

public class AiCurrentStatus
{
	
	AssistantManager fieldAssistantManager;
	MediaArchive fieldMediaArchive;
	
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}
	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	public AssistantManager getAssistantManager()
	{
		return fieldAssistantManager;
	}
	public void setAssistantManager(AssistantManager inAssistantManager)
	{
		fieldAssistantManager = inAssistantManager;
	}
	Data fieldChannel;
	public Data getChannel()
	{
		return fieldChannel;
	}
	public void setChannel(Data inChannel)
	{
		fieldChannel = inChannel;
	}
	public Collection<Data> getMessageHistory()
	{
		fieldMessageHistory = getMediaArchive().query("chatterbox").exact("channel", getChannel().getId()).sort("dateUp").search();
		return fieldMessageHistory;
	}
	public void setMessageHistory(Collection<Data> inMessageHistory)
	{
		fieldMessageHistory = inMessageHistory;
	}
	public Collection<Data> getFunctions()
	{
		return getAssistantManager().getFunctions();
	}
	public Collection<Data> getLastSearchResults()
	{
		return fieldLastSearchResults;
	}
	public void setLastSearchResults(Collection<Data> inLastSearchResults)
	{
		fieldLastSearchResults = inLastSearchResults;
	}
	public Data getLastCreatedEntity()
	{
		return fieldLastCreatedEntity;
	}
	public void setLastCreatedEntity(Data inLastCreatedEntity)
	{
		fieldLastCreatedEntity = inLastCreatedEntity;
	}
	public Data getLastCreatedAsset()
	{
		return fieldLastCreatedAsset;
	}
	public void setLastCreatedAsset(Data inLastCreatedAsset)
	{
		fieldLastCreatedAsset = inLastCreatedAsset;
	}
	public Data getRecentModule()
	{
		return fieldRecentModule;
	}
	public void setRecentModule(Data inRecentModule)
	{
		fieldRecentModule = inRecentModule;
	}
	Collection<Data> fieldMessageHistory;
	Collection<Data> fieldLastSearchResults;
	Data fieldLastCreatedEntity;
	Data fieldLastCreatedAsset;
	Data fieldRecentModule;
}
