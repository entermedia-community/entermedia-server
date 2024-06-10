package org.entermediadb.asset.facedetect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

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
						
						//remove as primary image if is
						MultiValued group = (MultiValued)archive.getData("faceprofilegroup",faceprofilegroupid);
						if (group != null) {
							String primaryimage = (String)group.getValue("primaryimage");
							if (primaryimage!= null && primaryimage.equals(assetid)) {
								group.setValue("primaryimage", "");
								Integer count = group.getInt("samplecount");
								count = count -1;
								group.setValue("samplecount", count.toString());
								archive.getSearcher("faceprofilegroup").saveData(group);
							}
						}
					}
				}
				asset.setValue("faceprofiles",newfaceprofiles);
				archive.saveAsset(asset);
			}
		}
		
	}
	
}
