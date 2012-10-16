package org.openedit.entermedia.xmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.util.GenericsUtil;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.util.Exec;
import com.openedit.util.ExecResult;

public class XmpWriter {
	protected Exec fieldExec;
	
	public void addClearKeywords(List<String> inComm)
	{
		inComm.add("-Subject=");
	}
	
	
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
	
	public void addSaveKeywords(List<String> inKeywords, List<String> inComm) throws Exception
	{
		addClearKeywords(inComm);
		for (String key: inKeywords) 
		{
			addKeyword(key, inComm);
		}
	}

	public boolean saveMetadata(MediaArchive inArchive, Asset inAsset) throws Exception
	{
		File original = new File(inArchive.getOriginalDocument(inAsset).getContentItem().getAbsolutePath());
		return saveMetadata(inArchive, inAsset,original);
	}	
	public boolean saveMetadata(MediaArchive inArchive, Asset inAsset, File inFile) throws Exception
	{
		List<String> comm = GenericsUtil.createList();
		Page etConfig = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/exiftool.conf");
		if( etConfig.exists() )
		{
			comm.add("-config");
			comm.add(etConfig.getContentItem().getAbsolutePath());
		}
		
		boolean ok = true;
		if( !inAsset.getKeywords().isEmpty() )
		{
			List keywords = new ArrayList(comm);
			addSaveKeywords(inAsset.getKeywords(), keywords);
			keywords.add(inFile.getAbsolutePath());
			ok = runExec(keywords);
			if( !ok )
			{
				//log.error("Could not write keywords");
			}
		}
		addSaveFields(inArchive, inAsset, comm);
		comm.add(inFile.getAbsolutePath());
		return runExec(comm);
	}
	
	public void addSaveFields(MediaArchive inArchive, Asset inAsset, List<String> inComm)
	{
		PropertyDetails details = inArchive.getAssetPropertyDetails();
		for(Object o: details)
		{
			PropertyDetail detail = (PropertyDetail) o;
			if(detail.getExternalId() == null || !detail.isEditable())
			{
				continue;
			}
			String[] tags = detail.getExternalIds();
			
			String value = inAsset.get(detail.getId());
			if( detail.getId().equals("imageorientation"))
			{
				Searcher searcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "imageorientation");
				Data rotationval = (Data)searcher.searchById(value);
				value = rotationval.get("rotation");
				if( value != null )
				{
					continue; //Only set the value if rotation is set
				}
			}
			addTags(tags, value, inComm);
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
