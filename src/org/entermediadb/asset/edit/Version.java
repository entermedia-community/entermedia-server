package org.entermediadb.asset.edit;

import java.util.Date;

public class Version
{
	
	public static final String IMPORTED = "imported"; 
	public static final String UPLOADED = "uploaded"; 
	public static final String SYNCED = "synced"; 
	public static final String ONLINEEDIT = "edited"; 
	public static final String UIREPLACE = "uireplace"; 
	public static final String RESTORE = "restored"; 
	public static final String DELETE = "deleted"; 
			
	
	protected int fieldVersion;
	protected String fieldChangeType;
	protected String fieldUserMessage;
	protected String fieldEditUser;

	
	protected Date fieldEditDate;
	protected String fieldPreviewFileName;
	protected String fieldPreviewBackUpPath;
	protected String fieldBackUpPath;
	protected long fieldFileSize;
	
	public long getFileSize()
	{
		return fieldFileSize;
	}
	public void setFileSize(long inFileSize)
	{
		fieldFileSize = inFileSize;
	}
	public String getPreviewBackUpPath()
	{
		return fieldPreviewBackUpPath;
	}
	public void setPreviewBackUpPath(String inPreviewBackUpPath)
	{
		fieldPreviewBackUpPath = inPreviewBackUpPath;
	}
	public String getBackUpPath()
	{
		return fieldBackUpPath;
	}
	public void setBackUpPath(String inBackUpPath)
	{
		fieldBackUpPath = inBackUpPath;
	}
	public String getPreviewFileName()
	{
		return fieldPreviewFileName;
	}
	public void setPreviewFileName(String inPreviewFileName)
	{
		fieldPreviewFileName = inPreviewFileName;
	}
	
	public int getVersion()
	{
		return fieldVersion;
	}
	public void setVersion(int inVersion)
	{
		fieldVersion = inVersion;
	}
	public String getChangeType()
	{
		return fieldChangeType;
	}
	public void setChangeType(String inChangeType)
	{
		fieldChangeType = inChangeType;
	}
	public String getUserMessage()
	{
		return fieldUserMessage;
	}
	public void setUserMessage(String inUserMessage)
	{
		fieldUserMessage = inUserMessage;
	}
	public String getEditUser()
	{
		return fieldEditUser;
	}
	public void setEditUser(String inEditUser)
	{
		fieldEditUser = inEditUser;
	}
	public Date getEditDate()
	{
		return fieldEditDate;
	}
	public void setEditDate(Date inEditDate)
	{
		fieldEditDate = inEditDate;
	}
	
}
