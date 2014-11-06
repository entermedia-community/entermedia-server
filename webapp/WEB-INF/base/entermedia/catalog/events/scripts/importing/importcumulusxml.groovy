package importing;

import org.openedit.entermedia.MediaArchive
import groovy.util.slurpersupport.GPathResult

import org.openedit.*
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.search.AssetSearcher
import org.openedit.entermedia.xmldb.XmlCategoryArchive

import com.openedit.hittracker.*
import com.openedit.page.Page

public void init() {
	MediaArchive archive = context.getPageValue("mediaarchive");





	Page page = archive.getPageManager().getPage("/cumulus/CUMULUSMETADATA/assetMetaData.xml");
	GPathResult path = new XmlSlurper().parse(page.getReader());

	HashMap detailmap = new HashMap();
	AssetSearcher assetsearcher = archive.getAssetSearcher();
	PropertyDetails details = assetsearcher.getPropertyDetails();

	path.Layout.Fields.Field.each{
		String uid = it.@uid;
		uid = uid.replace("{", "").replace("}", "");

		String name =  it.text();

		String type = it.@type;
		PropertyDetail detail = assetsearcher.getDetail(uid);

		if(detail == null){
			detail = new PropertyDetail();
			detail.setId(uid);
			detail.setText(name);
			detail.setIndex(true);
			detail.setStored(true);
			details.addDetail(detail);
		}		else{
			detail.setText(name);
		}
	}
	assetsearcher.getPropertyDetailsArchive().savePropertyDetails(details, "asset", null);


	ArrayList toSave = new ArrayList();
	path.Items.Item.each{
		Asset asset = new Asset();
		it.FieldValue.each{
			String uid = it.@uid;
			uid = uid.replace("{", "").replace("}", "");
			String value = it.text();
			asset.setProperty(uid, value);
			asset.setId(assetsearcher.nextAssetNumber());
			asset.setSourcePath(uid);
		}
		String filename = asset.get("af4b2e69-5f6a-11d2-8f20-0000c0e166dc");
		
		
		asset.setName(filename);
		String categories = asset.get("af4b2e0c-5f6a-11d2-8f20-0000c0e166dc");
		String catalog = asset.get("c02adb32-5c2c-4014-b86a-a53cf83f7e6c");
		asset.setFolder(false);
		
		
		
		if(categories){
				categories = categories.replace("/", "_");
				String replacementcat = "HSM -  Forschung & Entwicklung"
				String[] cattree = categories.split(":");
				StringBuffer sourcepath = new StringBuffer();
				
				cattree.each{
					String node = it;
					if(node.contains("HSM")){
						sourcepath.append(replacementcat);						
					} else{
						sourcepath.append(node);
					}
					sourcepath.append("/");
						
				}
				String sp = sourcepath.toString().substring(0, sourcepath.length() -1)
				
				org.openedit.entermedia.Category category = archive.getCategoryArchive().createCategoryTree("${catalog}/${sp}");
				asset.addCategory(category);
				
				asset.setSourcePath("${catalog}/${sp}/${filename}");
				Page dest = archive.getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + asset.getSourcePath());
				if(dest.exists()){
					asset = archive.getAssetImporter().getAssetUtilities().populateAsset(asset, dest.getContentItem(), archive, asset.getSourcePath(), context.getUser());
				}
				
		
		} else{
			asset.addCategory(archive.getCategoryArchive().getRootCategory());
		}
		
		toSave.add(asset);
		if(toSave.size() > 1000){
			assetsearcher.saveAllData(toSave, null);
			toSave.clear();
		}
	}
	archive.getCategoryArchive().saveAll();
	assetsearcher.saveAllData(toSave, null);
}


public void createCategories(){

	MediaArchive archive = context.getPageValue("mediaarchive");

	Page page = archive.getPageManager().getPage("/cumulus/CUMULUSMETADATA/categoryTreeMetaData.xml");
	GPathResult path = new XmlSlurper().parse(page.getReader());
	String parentuid = "{5251a747-727c-11d2-a73d-0000c000cdd3}";
	String categoryname = "{5251a740-727c-11d2-a73d-0000c000cdd3}";
	String catid = "{c02adb31-5c2c-4014-b86a-a53cf83f7e6c}";
	String catalogname = "{c02adb32-5c2c-4014-b86a-a53cf83f7e6c}";
	XmlCategoryArchive catarc = archive.getCategoryArchive();
	ArrayList categories = new ArrayList();
	path.Items.Item.each{
		HashMap properties = new HashMap();
		it.FieldValue.each{
			String uid = it.@uid;
			String value = it.text();
			if(value){
				properties.put(uid, value);
			}
		}
		String id = properties.get(catid);
		String pid = properties.get(parentuid);
		String name = properties.get(categoryname);
		org.openedit.entermedia.Category category = catarc.getCategory(id);
		if(category == null){
			category = new org.openedit.entermedia.Category();
		}
		if(pid != null && pid != "0"){
			category.setParentId(pid);
			//category.setProperty("parentid", pid);
		}
		if(pid ==  "0"){
			catarc.getRootCategory().addChild(category);
		}
		category.setId(id);
		category.setName(name);
		categories.add(category);
		
	}
	
	categories.each{
		findParent(categories, it);
	}
	categories.each{
		if(it.getParentCategory() == null){
			catarc.getRootCategory().addChild(it);
		}
	}
	archive.getCategoryArchive().saveAll();
	
}


public void findParent(ArrayList categories, org.openedit.entermedia.Category inCategory){
	Category parent = null;
	categories.each{
		if(it.getId().equals(inCategory.getParentId())){
			it.addChild(inCategory);
			
			return;
		}
	}
}








//createCategories();




init();
