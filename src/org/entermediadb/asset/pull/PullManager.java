package org.entermediadb.asset.pull;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;

public class PullManager implements CatalogEnabled
{
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	protected DataPuller fieldDataPuller;
	protected OriginalPuller fieldOriginalPuller;
	public DataPuller getDataPuller()
	{
		if (fieldDataPuller == null)
		{
			fieldDataPuller = (DataPuller)getModuleManager().getBean(getCatalogId(),"dataPuller");
		}

		return fieldDataPuller;
	}
	public void setDataPuller(DataPuller inDataPuller)
	{
		fieldDataPuller = inDataPuller;
	}
	public OriginalPuller getOriginalPuller()
	{
		if (fieldOriginalPuller == null)
		{
			fieldOriginalPuller =  (OriginalPuller)getModuleManager().getBean(getCatalogId(),"originalPuller");
		}

		return fieldOriginalPuller;
	}
	public void setOriginalPuller(OriginalPuller inOriginalPuller)
	{
		fieldOriginalPuller = inOriginalPuller;
	}
	//Send in pages
	
	public void receiveFile(WebPageRequest inReq, MediaArchive inArchive)
	{
	
		FileUpload command = (FileUpload) inArchive.getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
			
		String remotecatalogid = (String) inReq.getRequestParameter("catalogid");
		String localpath =  inReq.getRequestParameter("localpath");
		String savepath = localpath.replace(remotecatalogid, inArchive.getCatalogId());
	
		FileUploadItem item = properties.getFirstItem();
		properties.saveFileAs(item, savepath, inReq.getUser());
	}
}
