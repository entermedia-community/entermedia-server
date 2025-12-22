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
	public boolean isUserSelected()
	{
		return fieldUserSelected;
	}
	public void setUserSelected(boolean inUserSelected)
	{
		fieldUserSelected = inUserSelected;
	}
	public String getSearchType()
	{
		return fieldSearchType;
	}
	public void setSearchType(String inSearchType)
	{
		fieldSearchType = inSearchType;
	}
	public int getCountReady()
	{
		return fieldCountReady;
	}
	public void setCountReady(int inCountReady)
	{
		fieldCountReady = inCountReady;
	}
	public int getCountPending()
	{
		return fieldCountPending;
	}
	public void setCountPending(int inCountPending)
	{
		fieldCountPending = inCountPending;
	}
	public int getCountTotal()
	{
		return fieldCountTotal;
	}
	public void setCountTotal(int inCountTotal)
	{
		fieldCountTotal = inCountTotal;
	}
	protected boolean fieldUserSelected;
	protected String fieldSearchType;
	protected int fieldCountReady;
	protected int fieldCountPending;
	protected int fieldCountTotal;
}
