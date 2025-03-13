package org.entermediadb.asset.facedetect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.util.MathUtils;

public class FaceProfileModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(FaceProfileModule.class);

	public FaceProfileManager getProjectManager(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		FaceProfileManager manager = (FaceProfileManager) getModuleManager().getBean(archive.getCatalogId(), "faceProfileManager");
		inReq.putPageValue("faceprofilemanager", manager);
		return manager;
	}
	
	
	
	public void removeAsset(WebPageRequest inReq) {
		
		MediaArchive archive = getMediaArchive(inReq);
		String assetid = inReq.getRequiredParameter("assetid");
		String removeprofileid = inReq.getRequiredParameter("profileid");
		
		if (assetid != null && removeprofileid != null) {
			
			Asset asset = archive.getAsset(assetid);

			if (asset != null) {
				
				List<Map> newfaceprofiles = new ArrayList();
				Collection<Map> faceprofiles = (Collection)asset.getValue("faceprofiles");
				for( Map facedata : faceprofiles)
				{
					String faceprofilegroupid = (String)facedata.get("faceprofilegroup");
					if (!faceprofilegroupid.equals(removeprofileid)) {
						newfaceprofiles.add(facedata);
					
					}
					else 
					{
						//add to removed faceprofiles
						asset.addValue("removedfaceprofilegroups", removeprofileid);
						
						//add new faceprofile to preserve the detected face
						MultiValued newgroup = (MultiValued) archive.getSearcher("faceprofilegroup").createNewData();
						newgroup.setValue("creationdate", new Date());
						newgroup.setValue("samplecount",1);
						newgroup.setValue("entity_date", new Date());
						newgroup.setValue("primaryimage", asset.getId());
						archive.getSearcher("faceprofilegroup").saveData(newgroup);
						facedata.put("faceprofilegroup", newgroup.getId() );
						newfaceprofiles.add(facedata);
						
						
						//remove count from old profile and main image if is this asset.
						MultiValued group = (MultiValued)archive.getData("faceprofilegroup",faceprofilegroupid);
						if (group != null) {
							Integer count = group.getInt("samplecount");
							count = count -1;
							group.setValue("samplecount", count.toString());
							//remove as primary image if is
							String primaryimage = (String)group.getValue("primaryimage");
							if (primaryimage!= null && primaryimage.equals(assetid)) {
								group.setValue("primaryimage", "");
							}
							archive.getSearcher("faceprofilegroup").saveData(group);
						}
					}
				}
				asset.setValue("faceprofiles",newfaceprofiles);
				archive.saveAsset(asset);
				inReq.putPageValue("asset", asset);
			}
		}
		
	}
	
	
	
	public void addPersonToProfileGroup(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String assetid = inReq.getRequestParameter("assetid");
		String personid = inReq.getRequestParameter("dataid");
		String faceprofilegroupid =inReq.getRequestParameter("faceprofilegroupid");
		
		if (faceprofilegroupid != null && personid != null)
		{
			MultiValued group = (MultiValued)archive.getData("faceprofilegroup",faceprofilegroupid);
			if (group != null)
			{
				group.setValue("entityperson", personid);
				archive.getSearcher("faceprofilegroup").saveData(group);
			}
		}
		Asset asset = archive.getAsset(assetid);
		inReq.putPageValue("asset", asset);
	}
	
	
	public void addManualFaceProfile (WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String assetid = inReq.getRequestParameter("assetid");
		String boxlocation = inReq.getRequestParameter("boxlocation");
		
		Asset asset = archive.getAsset(assetid);
		
		if (boxlocation == null || asset  == null)
		{
			return;
		}
		
		MultiValued newgroup = (MultiValued) archive.getSearcher("faceprofilegroup").createNewData();
		newgroup.setValue("creationdate", new Date());
		newgroup.setValue("entity_date", new Date());
		newgroup.setValue("automatictagging", false);
		newgroup.setValue("taggedby",inReq.getUser());
		newgroup.setValue("samplecount",1);
		newgroup.setValue("primaryimage", asset.getId());
		
		archive.getSearcher("faceprofilegroup").saveData(newgroup);
		Map newfacedata = new HashMap();
		newfacedata.put("faceprofilegroup", newgroup.getId() );
		
		
		String inputw = inReq.getRequestParameter("assetwidth");
		String inputh = inReq.getRequestParameter("assethight");
		String thumbwidth = inReq.getRequestParameter("thumbwidth");
		
		
		
		try
		{
			JSONObject locationarray = new JSONObject();
			JSONParser parser = new JSONParser();
			locationarray = (JSONObject) parser.parse(boxlocation);
			
			Double scale = MathUtils.divide(inputw, thumbwidth);
			
			Number left = (Number)locationarray.get("left");
			Double x = left.doubleValue() * scale;
			
			Number top = (Number)locationarray.get("top");
			Double y = top.doubleValue() * scale;
			
			Number width = (Number)locationarray.get("width");
			Double w = width.doubleValue() * scale;
			
			Number height = (Number)locationarray.get("height");
			Double h = height.doubleValue() * scale;
			
			newfacedata.put("locationx",x);
			newfacedata.put("locationy",y);
			newfacedata.put("locationw",w);
			newfacedata.put("locationh",h);
				
			newfacedata.put("inputwidth",inputw);
			newfacedata.put("inputheight",inputh);
			
			Collection<Map> faceprofiles = (Collection)asset.getValue("faceprofiles");
			faceprofiles.add(newfacedata);
			
			asset.setValue("faceprofiles",faceprofiles);
			archive.saveAsset(asset);
			inReq.putPageValue("asset", asset);
		}
		catch ( Throwable ex)
		{
			log.error("Could not read profile location", ex );
		}
		
	}
	
}
