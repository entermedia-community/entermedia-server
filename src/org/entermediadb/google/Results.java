package org.entermediadb.google;

import java.util.ArrayList;
import java.util.Collection;

import org.json.simple.JSONObject;

import com.google.gson.JsonObject;

public class Results
{
Collection fieldFolders;
Collection fieldFiles;
String fieldResultToken;
String fieldParentId;

public String getParentId()
{
	return fieldParentId;
}
public void setParentId(String inParentId)
{
	fieldParentId = inParentId;
}
public Collection getFolders()
{
	return fieldFolders;
}
public void setFolders(Collection inFolders)
{
	fieldFolders = inFolders;
}
public Collection getFiles()
{
	return fieldFiles;
}
public void setFiles(Collection inFiles)
{
	fieldFiles = inFiles;
}
public String getResultToken()
{
	return fieldResultToken;
}
public void setResultToken(String inToken)
{
	fieldResultToken = inToken;
}

public void addFolder(JSONObject inFolder)
{
	if( fieldFolders == null)
	{
		fieldFolders = new ArrayList();
	}
	fieldFolders.add(inFolder);
}
public void addFile(JSONObject inFile)
{
	if( fieldFiles == null)
	{
		fieldFiles = new ArrayList();
	}
	fieldFiles.add(inFile);
}

}
