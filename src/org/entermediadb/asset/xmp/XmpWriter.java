package org.entermediadb.asset.xmp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.page.Page;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.GenericsUtil;

public class XmpWriter 
{
	private static final Log log = LogFactory.getLog(XmpWriter.class);

	protected Exec fieldExec;
	

	public void addKeyword(String inKeyword, List<String> inComm)
	{
		inComm.add("-Subject+="+inKeyword);
	}
	
	public boolean writeTag(String inTag, String inValue, File inFile)
	{
		List<String> com = new ArrayList<String>();
		com.add("-" + inTag + "=" + inValue);
		com.add(inFile.getAbsolutePath());
		return runExec(com);
	}
	
	protected String runExecWithOutput(List<String> inCom) throws OpenEditException
	{
		ExecResult result = getExec().runExec("exiftool", inCom);
		return result.getStandardOut();
	}
	
	protected boolean runExec(List<String> inCom) throws OpenEditException
	{
		ExecResult result = getExec().runExec("exiftool", inCom);
		return result.isRunOk();
	}
	
	public void addSaveKeywords(Collection<String> inKeywords, List<String> inComm) throws Exception
	{
		for (String key: inKeywords) 
		{
			addKeyword(key, inComm);
		}
	}
	
	
	public boolean saveMetadata(MediaArchive inArchive, String path, Asset inAsset) throws Exception
	{
		Map props = new HashMap();
		props.put("absolutepath", path);
		inArchive.fireMediaEvent("savingoriginal","asset",inAsset.getSourcePath(),props,null);
		boolean ok = false;
		try
		{
			List<String> comm = createCommand(inArchive);
			addSaveFields(inArchive, inAsset, comm);		
			List removekeywords = new ArrayList(comm);
			removekeywords.add("-Subject="); //This only works on a line by itself
			removekeywords.add(path);
			addSaveKeywords(inAsset.getKeywords(), comm);
			comm.add(path);
			ok = runExec(comm);
		}
		finally
		{
			inArchive.fireMediaEvent("savingoriginalcomplete","asset",inAsset.getSourcePath(),props,null);			
		}
		return ok;
	}
	

	public boolean saveMetadata(MediaArchive inArchive, Asset inAsset) throws Exception
	{
		String path = inArchive.getOriginalDocument(inAsset).getContentItem().getAbsolutePath();
		return saveMetadata(inArchive, path, inAsset);
		

	}	
	
	public boolean saveKeywords(MediaArchive inArchive, Asset inAsset) throws Exception
	{
		String path = inArchive.getOriginalDocument(inAsset).getContentItem().getAbsolutePath();

		Map props = new HashMap();
		props.put("absolutepath", path);
		inArchive.fireMediaEvent("savingoriginal","asset",inAsset.getSourcePath(),props,null);
		boolean ok = false;
		try
		{
			List<String> comm = createCommand(inArchive);
			List removekeywords = new ArrayList(comm);
			removekeywords.add("-Subject="); //This only works on a line by itself
			removekeywords.add(path);
			ok = runExec(removekeywords);
			if( ok ) 
			{
				addSaveKeywords(inAsset.getKeywords(), comm);
				comm.add(path);
				ok = runExec(comm);
			}
		}
		finally
		{
			inArchive.fireMediaEvent("savingoriginalcomplete","asset",inAsset.getSourcePath(),props,null);			
		}
		return ok;
	}

	protected List<String> createCommand(MediaArchive inArchive)
	{
		List<String> comm = GenericsUtil.createList();
		Page etConfig = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/exiftool.conf");
		if( etConfig.exists() )
		{
			comm.add("-config");
			comm.add(etConfig.getContentItem().getAbsolutePath());
		}
		comm.add("-overwrite_original");
		comm.add("-n");
		return comm;
	}

	
	public void addSaveFields(MediaArchive inArchive, Asset inAsset, List<String> inComm)
	{
		PropertyDetails details = inArchive.getAssetPropertyDetails();
		for(Object o: details)
		{
			PropertyDetail detail = (PropertyDetail) o;
			Object value = inAsset.getValue(detail.getId());
			if(value == null || detail.getExternalId() == null || !detail.isEditable())
			{
				continue;
			}
			String[] tags = detail.getExternalIds();
			
			String val = String.valueOf(value);
			if(detail.isList() && Boolean.parseBoolean(detail.get("writenametoexif")))
			{
				Data remote = (Data)inArchive.getSearcherManager().getData(detail,val);
				if(remote != null)
				{
					val = remote.getName();
				}
			}
			
			if(detail.isList() || detail.isMultiValue()){
				val = val.replace("[", "");
				val = val.replace("]", "");

			}
//			if( detail.getId().equals("imageorientation"))
//			{
//				value = inAsset.get("rotation"); //custom rotation. this should be set by the rotation tool?
////				Searcher searcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "imageorientation");
////				Data rotationval = (Data)searcher.searchById(value);
////				value = rotationval.get("rotation");
//				if( value == null )
//				{
//					continue; //Only set the value if rotation is set
//				}
//			}
			addTags(tags, val, inComm);
		}
	}

	public void addTags(String[] inTags, String inValue, List<String> inComm)
	{
		if(inValue == null)
		{
			inValue = "";
		}
		for (int i = 0; i < inTags.length; i++) //We need to add them all since Photoshop adds them all. 
		{
			if( inTags[i].contains(":") ) //Only write back to iptc: or xmp: fields
			{
				inComm.add("-" + inTags[i] + "=" + inValue);
			}
		}
	}
	
//	public boolean isIndesign(File inFile) throws IOException
//	{
//		byte[] guid = new byte[16];
//		InputStream in = new FileInputStream(inFile);
//		in.read(guid, 0, 16);
//		in.close();
//		
//		byte[] expected = new byte[] 
//		               		           {0x06,0x06,(byte)0xED,(byte)0xF5,
//										(byte)0xD8,0x1D,0x46,(byte)0xe5,
//										(byte)0xBD,0x31,(byte)0xEF,(byte)0xE7,
//										(byte)0xFE,0x74,(byte)0xB7,0x1D};
//		boolean isIndd = true;
//		for(int i=0;i<guid.length;i++)
//		{
//			if(guid[i] != expected[i])
//			{
//				isIndd = false;
//				break;
//			}
//		}
//		return isIndd;
//	}


	public Exec getExec() {
		return fieldExec;
	}


	public void setExec(Exec exec) {
		fieldExec = exec;
	}
}
