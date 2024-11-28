package org.entermediadb.asset.xmp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.GenericsUtil;

public class XmpWriter {
	private static final Log log = LogFactory.getLog(XmpWriter.class);

	protected Exec fieldExec;

	public void addKeyword(String inKeyword, List<String> inComm) {
	//	inComm.add("-Subject-=" + inKeyword);
		inComm.add("-Subject=" + inKeyword);
	}

	public boolean writeTag(String inTag, String inValue, File inFile) {
		List<String> com = new ArrayList<String>();
		com.add("-" + inTag + "=" + inValue);
		com.add(inFile.getAbsolutePath());
		return runExec(com).isRunOk();
	}

	protected String runExecWithOutput(List<String> inCom) throws OpenEditException {
		ExecResult result = getExec().runExec("exiftool", inCom, true);
		return result.getStandardOut();
	}

	protected ExecResult runExec(List<String> inCom) throws OpenEditException {
		ExecResult result = getExec().runExec("exiftool", inCom, true);
		return result;
	}

	public void addSaveKeywords(Collection<String> inKeywords, List<String> inComm) throws Exception {
		for (String key : inKeywords) {
			addKeyword(key, inComm);
		}
	}
	
	public boolean saveId(MediaArchive inArchive, Asset inAsset)
	{
		ContentItem item = inArchive.getOriginalContent(inAsset);
		
		boolean ok = confirmId(inArchive, inAsset, item);

		if( ok )
		{
			return ok;
		}
		List comm = createCommand(inArchive,true);
		comm.add("-entermediaexif=" + inAsset.getId());
		comm.add(item.getAbsolutePath());
		Map props = new HashMap();
		props.put("absolutepath", item.getAbsolutePath());
		inArchive.fireMediaEvent("savingoriginal", "asset", inAsset.getSourcePath(), props, null);

		try
		{
			ExecResult result  = runExec(comm);
			
			if(result.isRunOk()) 
			{
				inAsset.setValue("assetmodificationdate", item.lastModified()); //This needs to be set or it will keep thinking it's changed
				inAsset.setValue("xmperror", false); //This needs to be set or it will keep thinking it's changed
				ok = confirmId(inArchive, inAsset, item);
				
				return ok;
			} 
			else
			{
				inAsset.setValue("xmperror", true); //This needs to be set or it will keep thinking it's changed
				String error = result.getStandardError();
				String output = result.getStandardOut();
				inAsset.setValue("xmperrormessage", error);
				inAsset.setValue("xmpoutput", output);
			
				inArchive.saveAsset(inAsset);
			}
			
		} finally {
			inArchive.fireMediaEvent("savingoriginalcomplete", "asset", inAsset.getSourcePath(), props, null);
		}
		return false;
		
	}

	protected boolean confirmId(MediaArchive inArchive, Asset inAsset, ContentItem item)
	{
		List comm = createCommand(inArchive,false);
		comm.add(item.getAbsolutePath());
		
		ExecResult result = getExec().runExec("exiftool", comm, true);

		if (!result.isRunOk())
		{
			String error = result.getStandardError();
			log.info("error " + error);
			return false;
		}
		String numberinfo = result.getStandardOut();
		if (numberinfo == null)
		{
			log.info("Exiftool found " + inAsset.getSourcePath() + " returned null");
		}
		else
		{
			log.debug("Exiftool found " + inAsset.getSourcePath() + " returned " + numberinfo.length());
		}
		int indexof = numberinfo.indexOf("Entermediaexif");
		if( indexof != -1)
		{
			int end = numberinfo.indexOf("\n",indexof);
			String val = numberinfo.substring(indexof + "entermediaexif".length() + 20, end);
			if( val != null)
			{
				if( inAsset.getId().equals(val))
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean saveMetadata(MediaArchive inArchive, ContentItem inItem, Asset inAsset, HashMap inExtraDetails)
			throws Exception {

		String path = inItem.getAbsolutePath();

		Map props = new HashMap();
		props.put("absolutepath", path);
		inArchive.fireMediaEvent("savingoriginal", "asset", inAsset.getSourcePath(), props, null);
		boolean ok = true;
		try {
			List<String> comm = createCommand(inArchive,true);
//			if(clearkeywords) {
//				comm.add("-Subject=");
//				comm.add("-subject=");
//				comm.add("-keywords=");
//				comm.add("-XMP-dc:Subject=");
//				comm.add(path);
//				ok = runExec(comm);
//
//
//			}
			comm = createCommand(inArchive,true);
			comm.add("-Keywords=");
			comm.add("-Subject=");
			comm.add("-XMP-dc:Subject=");
			addSaveFields(inArchive, inAsset, comm, inExtraDetails);
			
			addSaveKeywords(inAsset.getKeywords(), comm);
			
			comm.add(path);
			ExecResult result  = runExec(comm);
			
			if(result.isRunOk()) {
				inAsset.setValue("assetmodificationdate", inItem.lastModified()); //This needs to be set or it will keep thinking it's changed
				inAsset.setValue("xmperror", false); //This needs to be set or it will keep thinking it's changed
				
				inArchive.saveAsset(inAsset);

			} else {
				ok = false;
				inAsset.setValue("xmperror", true); //This needs to be set or it will keep thinking it's changed
				String error = result.getStandardError();
				String output = result.getStandardOut();
				inAsset.setValue("xmperrormessage", error);
				inAsset.setValue("xmpoutput", output);

				inArchive.saveAsset(inAsset);
			}
			
		} finally {
			inArchive.fireMediaEvent("savingoriginalcomplete", "asset", inAsset.getSourcePath(), props, null);
		}
		return ok;
	}

	public boolean saveMetadata(MediaArchive inArchive, Asset inAsset) throws Exception {
		ContentItem item = inArchive.getOriginalContent(inAsset);

		return saveMetadata(inArchive, item, inAsset, new HashMap());

	}

	public boolean saveMetadata(MediaArchive inArchive, Asset inAsset, HashMap inExtraDetails, boolean clearkeywords) throws Exception {
		ContentItem item = inArchive.getOriginalContent(inAsset);

		return saveMetadata(inArchive, item, inAsset, inExtraDetails);

	}

	public boolean saveKeywords(MediaArchive inArchive, Asset inAsset) throws Exception {
		String path = inArchive.getOriginalContent(inAsset).getAbsolutePath();

		Map props = new HashMap();
		props.put("absolutepath", path);
		inArchive.fireMediaEvent("savingoriginal", "asset", inAsset.getSourcePath(), props, null);
		boolean ok = false;
		try {
			List<String> comm = createCommand(inArchive,true);
			List removekeywords = new ArrayList(comm);
			removekeywords.add("-Subject="); // This only works on a line by
												// itself
			removekeywords.add(path);
			ok = runExec(removekeywords).isRunOk();
			if (ok) {
				addSaveKeywords(inAsset.getKeywords(), comm);
				comm.add(path);
				ok = runExec(comm).isRunOk();
			}
		} finally {
			inArchive.fireMediaEvent("savingoriginalcomplete", "asset", inAsset.getSourcePath(), props, null);
		}
		return ok;
	}

	protected List<String> createCommand(MediaArchive inArchive, boolean save) {
		List<String> comm = GenericsUtil.createList();
		Page etConfig = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/exiftool.conf");
		if (etConfig.exists()) {
			comm.add("-config");
			comm.add(etConfig.getContentItem().getAbsolutePath());
		}
		if(save )
		{
			comm.add("-overwrite_original");
			comm.add("-n");
			comm.add("-m");
		}
		return comm;
	}

	public void addSaveFields(MediaArchive inArchive, Asset inAsset, List<String> inComm, HashMap inExtraDetails) {
		PropertyDetails details = inArchive.getAssetPropertyDetails();
		for (Object o : details) {
			PropertyDetail detail = (PropertyDetail) o;
			Object value = inAsset.getValue(detail.getId());
			if (detail.getExternalId() == null || !detail.isEditable()) {
				continue;
			}
			
			//XMP-AGBU:Pagenumber
			if (value == null && detail.get("xmpmask") != null) {
				value = "";
			} 
			if(value ==null ) {
				continue;
			}
			String[] tags = detail.getExternalIds();

			String val = String.valueOf(value);
			if (detail.isList() && Boolean.parseBoolean(detail.get("writenametoexif"))) {
				Data remote = (Data) inArchive.getSearcherManager().getData(detail, val);
				if (remote != null) {
					val = remote.getName();
				}
			}

			if (detail.isList() || detail.isMultiValue()) {
				val = val.replace("[", "");
				val = val.replace("]", "");

			}

			if (detail.get("xmpmask") != null) {
				inExtraDetails.putAll(inAsset.getProperties());
				val = inArchive.replaceFromMask(detail.get("xmpmask"), inAsset, detail.getSearchType(), inExtraDetails, null); 
				//val = inArchive.getSearcherManager().getValue(inArchive.getCatalogId(), detail.get("xmpmask"), inExtraDetails);
				inExtraDetails.remove(detail.getId());
			}

			// if( detail.getId().equals("imageorientation"))
			// {
			// value = inAsset.get("rotation"); //custom rotation. this should
			// be set by the rotation tool?
			//// Searcher searcher =
			// inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(),
			// "imageorientation");
			//// Data rotationval = (Data)searcher.searchById(value);
			//// value = rotationval.get("rotation");
			// if( value == null )
			// {
			// continue; //Only set the value if rotation is set
			// }
			// }

			addTags(tags, val, inComm);
		}

		for (Iterator iterator = inExtraDetails.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			Object val2 = inExtraDetails.get(key);
			if (val2 instanceof String) {
				

				String[] tag = new String[1];
				tag[0] = key;
				addTags(tag, (String) val2, inComm); //Why is this called again?
			}

		}

	}

	public void addTags(String[] inTags, String inValue, List<String> inComm) {
		if (inValue == null) {
			inValue = "";
		}
		for (int i = 0; i < inTags.length; i++) // We need to add them all since
												// Photoshop adds them all.
		{
			if (inTags[i].contains(":")) // Only write back to iptc: or xmp:
											// fields
			{
				inComm.add("-" + inTags[i] + "=" + inValue);

			}
		}
	}

	// public boolean isIndesign(File inFile) throws IOException
	// {
	// byte[] guid = new byte[16];
	// InputStream in = new FileInputStream(inFile);
	// in.read(guid, 0, 16);
	// in.close();
	//
	// byte[] expected = new byte[]
	// {0x06,0x06,(byte)0xED,(byte)0xF5,
	// (byte)0xD8,0x1D,0x46,(byte)0xe5,
	// (byte)0xBD,0x31,(byte)0xEF,(byte)0xE7,
	// (byte)0xFE,0x74,(byte)0xB7,0x1D};
	// boolean isIndd = true;
	// for(int i=0;i<guid.length;i++)
	// {
	// if(guid[i] != expected[i])
	// {
	// isIndd = false;
	// break;
	// }
	// }
	// return isIndd;
	// }

	public Exec getExec() {
		return fieldExec;
	}

	public void setExec(Exec exec) {
		fieldExec = exec;
	}
}
