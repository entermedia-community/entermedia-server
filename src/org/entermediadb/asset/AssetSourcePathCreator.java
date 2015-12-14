package org.entermediadb.asset;

import org.openedit.Data;


public class AssetSourcePathCreator implements SourcePathCreator
{
	public String createSourcePath(Data inAsset, String inUrl)
	{
		
		String sourcepath = inUrl.trim();
		
		if( sourcepath.startsWith("\\\\"))
		{
			//cut off the server name
//			int nextslash = sourcepath.indexOf('\\',2);
			sourcepath = sourcepath.substring(2);
			//get the server name and shorten it if it has periods in it
			int index = sourcepath.indexOf('\\');
			if(index != -1)
			{
				String serverName = sourcepath.substring(0, index);
				serverName = serverName.toLowerCase();
				sourcepath = sourcepath.substring(index); //cut off the server name
				
				int shareIndex = sourcepath.indexOf('\\');
				String shareName = sourcepath.substring(0, shareIndex);
				sourcepath = sourcepath.substring(shareIndex); //cut off the share name
				
				int servernameindex = serverName.indexOf('.');
				if(servernameindex != -1)
				{
					serverName = serverName.substring(0, servernameindex);
				}
				
				sourcepath = serverName + shareName +  sourcepath;
			}
		}
		else if (sourcepath.length() > 1 && sourcepath.charAt(1) == ':')
		{
			
			sourcepath = sourcepath.replace(":","");
		}
		else if (sourcepath.length() > 3 && sourcepath.contains(":)") )
		{			
			//This is a SERVER (C:)/  junk that Cumulus puts in there
			sourcepath = sourcepath.replace(":)","");
			int start = sourcepath.indexOf("(");
			sourcepath = sourcepath.substring(start+1, sourcepath.length());
			 
		
		}
		else if (sourcepath.startsWith("/"))
		{
			sourcepath = sourcepath.substring(1);
		}
		sourcepath = sourcepath.replace('\\','/');
		//We should not have done this, redundant:
		/*String cumuluscatname = inAsset.get("externalid");
		if (cumuluscatname != null && cumuluscatname.length() > 0)
		{
			sourcepath = cumuluscatname + "/" + sourcepath;
		}*/
		
//		String ext = PathUtilities.extractPageType(sourcepath);
//		if( ext == null)
//		{
//			ext = inAsset.get("fileformat");
//			sourcepath = sourcepath + "." + ext;
//		}
		
//		String  pagenumber = inAsset.get("pagenumber");
//		if( pagenumber != null)
//		{
//			sourcepath = PathUtilities.extractPagePath(sourcepath);
//			sourcepath = sourcepath + "_page" + pagenumber + "." + ext;
//		}
		//no ext
		sourcepath = sourcepath.trim();

		return sourcepath;
	}
}
