package org.entermediadb.asset.scanner;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileCleaningTracker;

public class UploadDiskFileItemFactory extends DiskFileItemFactory
{
	protected String fieldDestinationPath;
	protected long fieldSeek;
	
	public void setDestinationPath(String inPath)
	{
		fieldDestinationPath = inPath;
	}

	public FileItem createItem(String inFieldName, String inContentType, boolean inIsFormField, String inFileName)
	{
		if (inFieldName.startsWith("file"))
		{
			UploadDiskFileItem result = new UploadDiskFileItem(inFieldName, inContentType, inIsFormField, inFileName, getSizeThreshold(), getRepository());
			result.setDestinationPath(fieldDestinationPath);
			result.fieldSeek = getSeek();
			FileCleaningTracker tracker = getFileCleaningTracker();
			if (tracker != null)
			{
				tracker.track(result.getTempFile(), this);
			}
			return result;
		}
		else
		{
			return super.createItem(inFieldName, inContentType, inIsFormField, inFileName);
		}
	}

	public long getSeek()
	{
		return fieldSeek;
	}

	public void setSeek(long inSeek)
	{
		fieldSeek = inSeek;
	}
}
