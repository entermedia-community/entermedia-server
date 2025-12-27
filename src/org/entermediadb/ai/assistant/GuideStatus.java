package org.entermediadb.ai.assistant;

import org.openedit.Data;

public class GuideStatus
{
	protected Data fieldViewData;
	public Data getViewData()
	{
		return fieldViewData;
	}
	public void setViewData(Data inViewData)
	{
		fieldViewData = inViewData;
	}
	
	public String getSearchType()
	{
		return fieldSearchType;
	}
	public void setSearchType(String inSearchType)
	{
		fieldSearchType = inSearchType;
	}
	public int getCountNotEmbedded()
	{
		return fieldCountNotEmbedded;
	}
	public void setCountNotEmbedded(int inCountNotEmbedded)
	{
		fieldCountNotEmbedded = inCountNotEmbedded;
	}
	public int getCountEmbedded()
	{
		return fieldCountEmbedded;
	}
	public void setCountEmbedded(int inCountEmbedded)
	{
		fieldCountEmbedded = inCountEmbedded;
	}
	public int getCountPending()
	{
		return fieldCountPending;
	}
	public void setCountPending(int inCountPending)
	{
		fieldCountPending = inCountPending;
	}
	public int getCountFailed()
	{
		return fieldCountFailed;
	}
	public void setCountFailed(int inCountFailed)
	{
		fieldCountFailed = inCountFailed;
	}
	public int getCountTotal()
	{
		return fieldCountTotal;
	}
	public void setCountTotal(int inCountTotal)
	{
		fieldCountTotal = inCountTotal;
	}
	
	public boolean isReady()
	{
		return getCountPending() == 0;
	}

	protected String fieldSearchType;
	protected int fieldCountNotEmbedded = 0;
	protected int fieldCountEmbedded = 0;
	protected int fieldCountPending = 0;
	protected int fieldCountFailed = 0;
	protected int fieldCountTotal = 0;
}
