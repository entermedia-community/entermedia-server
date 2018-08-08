package org.entermediadb.desktops;

public class Desktop
{
	protected DesktopEventListener fieldListener;
	
	public void setListener(DesktopEventListener inListener)
	{
		fieldListener = inListener;
	}
	protected String fieldUserId;
	public String getUserId()
	{
		return fieldUserId;
	}
	public void setUserId(String inUserId)
	{
		fieldUserId = inUserId;
	}
	public String getDesktopId()
	{
		return fieldDesktopId;
	}
	public void setDesktopId(String inDesktopId)
	{
		fieldDesktopId = inDesktopId;
	}
	public String getLastCommand()
	{
		return fieldLastCommand;
	}
	public void setLastCommand(String inLastCommand)
	{
		fieldLastCommand = inLastCommand;
	}
	protected String fieldDesktopId;
	protected String fieldLastCommand;
	protected int fieldLastCompletedPercent;
	protected String fieldHomeFolder;
	protected String fieldSpaceLeft;
	
	public String getSpaceLeft()
	{
		return fieldSpaceLeft;
	}
	public void setSpaceLeft(String inSpaceLeft)
	{
		fieldSpaceLeft = inSpaceLeft;
	}
	public String getHomeFolder()
	{
		return fieldHomeFolder;
	}
	public void setHomeFolder(String inHomeFolder)
	{
		fieldHomeFolder = inHomeFolder;
	}
	public int getLastCompletedPercent()
	{
		return fieldLastCompletedPercent;
	}
	public void setLastCompletedPercent(int inLastCompletedPercent)
	{
		fieldLastCompletedPercent = inLastCompletedPercent;
	}
	
}
