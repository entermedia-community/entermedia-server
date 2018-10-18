package notifications

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.asset.util.TimeParser
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init(){
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	AssetSearcher searcher = archive.getAssetSearcher();
	Searcher notifications = archive.getSearcher("notification");
	HitTracker renewals = searcher.query().match("renewalpolicy", "*").search();
	renewals.each{
		String renewalpolicy = it.renewalpolicy;
		Asset asset = searcher.loadData(it);
		Data current = notifications.query().exact("dataid", asset.getId()).exact("notificationstatus", "pending").searchOne();
		if(current ==null){
			current = notifications.createNewData();
			current.setValue("dataid",asset.getId());
			current.setValue("notificationstatus", "pending");
			TimeParser parser = new TimeParser();
			Data policy = archive.getData("renewalpolicy", asset.get("renewalpolicy"));
			if(policy == null){
				return;
			}
			long days = parser.parse(policy.renewalperiod);
			Date target = new Date(System.currentTimeMillis() +  days);
			asset.setValue("renewaldate", new Date());
			searcher.saveData(asset);
			
			current.setValue("senddate", target);
			current.setValue("notificationsubject", "Asset Renewal Request");
			current.setValue("notificationtype", "assetrenew");
			Data user = archive.getData("user", asset.get("renewalrecipient"));
			
			if(user == null) {
				String email = archive.getCatalogSettingValue("renewalrecipient");
				if(email == null) {
					log.info("User : " + asset.get("renewalrecpient") + "Not found when trying to create renewal notification.  Check asset ${asset.id}");
					return;
					
				} else {
					current.setValue("notificationemails", email);
					
				}
			}
			else {
		
				current.setValue("notificationemails", user.get("email"));
			}
			notifications.saveData(current);
			 
			
		}
		
	}
	
	
}

init();