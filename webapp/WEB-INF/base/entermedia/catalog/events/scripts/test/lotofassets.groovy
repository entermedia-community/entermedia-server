import org.openedit.Data
import org.openedit.data.Searcher
import org.entermediadb.asset.*
import org.entermediadb.asset.creator.*
import org.entermediadb.asset.edit.*
import org.entermediadb.asset.episode.*
import org.entermediadb.asset.modules.*
import org.entermediadb.asset.util.*
import org.openedit.util.DateStorageUtil
import org.openedit.xml.*


public void init() {
	MediaArchive archive = (MediaArchive)context.getPageValue("mediaarchive");
	
	BigInteger count = 0;
	List assetsToSave = new ArrayList();
	for (; count<1000000;) {
	Asset asset  = archive.createAsset("Collections/General/100/asset"+count.toString()+".jpg");
	asset.setProperty("assetaddeddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	//asset.setId("Bs1"+count.toString()+((((int) Math.random())).toString()));
	asset.setProperty("assettitle","Bulk Asset ABC "+count.toString());
	asset.setProperty("category","AW2x1V_vxffYADblHTXZ");
	//archive.saveAsset(asset, null);
	assetsToSave.add(asset);
	if(assetsToSave.size() == 200)
	{
		archive.saveAssets( assetsToSave );
		assetsToSave.clear();
		log.info("saved 200 assets");
	}
	count = count+1;
	}
	log.info("done!");
}


init();