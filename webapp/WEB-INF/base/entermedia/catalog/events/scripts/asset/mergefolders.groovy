package asset;


import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.repository.ContentItem



public void init()
{
	
//	String goodcategory = "2037";
//	String badcategory = "171";

	String goodcategory = "AXVWNpWj_Q05pZusg8dM";
	String badcategory = "AXKFyLGvrCB6NlUGlukf";
	
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	
	HitTracker all = searcher.query().exact("category", badcategory).sort("assetaddeddate").search();
	all.enableBulkOperations();
	HashSet tosave = new HashSet();

	Collection todelete = new ArrayList();
		
	for( Data hit : all)
	{
		Asset todeleteasset = searcher.loadData( hit );
		//search for other if found use it and copy the data over
		//If not found then move it over
		
		String md5 = todeleteasset.get("md5hex");
		Asset goodone = searcher.query().exact("category", goodcategory).exact("md5hex", md5).searchOne();
		if( goodone != null)
		{
			todelete.add(todeleteasset);
			//Copy any metadata from the one we are about to delete
			fixField("assettitle",todeleteasset,goodone);
			fixField("longcaption",todeleteasset,goodone);
			fixField("documentype",todeleteasset,goodone);
			fixField("subject",todeleteasset,goodone);
			fixField("itemdimensions",todeleteasset,goodone);
			fixField("creator",todeleteasset,goodone);
			fixField("copyrightnotice",todeleteasset,goodone);
			fixField("copyrightstatus",todeleteasset,goodone);
			fixField("rightsusagetrestrictions",todeleteasset,goodone);
			fixField("rightsusageterms",todeleteasset,goodone);
			fixField("eventorprogram",todeleteasset,goodone);
			fixField("eventtype",todeleteasset,goodone);
			fixField("personpictured",todeleteasset,goodone);
			
			//Add all the categories just in case
			for(Category cat : todeleteasset.getCategories() )
			{
				goodone.addCategory(cat);
			}
		}
		else
		{
			//Load Next
			//Move this asset over to the other place. By fixing sourcepath and adding categories for path
			goodone = todeleteasset;
		}
		
		String sourcepath = goodone.getSourcePath();
		if( sourcepath.startsWith("Pictures/") )
		{
			sourcepath = "Event and Programs/" + sourcepath.substring("Pictures/".length())
			goodone.setSourcePath(sourcepath);
		}
		if( !goodone.isFolder())
		{
			sourcepath = org.openedit.util.PathUtilities.extractDirectoryPath(sourcepath);
		}

		//This will leave it in both cats for now until it reindexes
		Category newcat = archive.getCategorySearcher().createCategoryPath(sourcepath);
		goodone.addCategory(newcat);
		goodone.setValue("duplicate",false);
		tosave.add(goodone);
		
		if(tosave.size() > 2000 || todelete.size() > 2000 )
		{
			log.info("Inside Saving and Deleting " + tosave.size() + " " + todelete.size());
			searcher.saveAllData(tosave, null);
			for(Asset oldjunk : todelete)
			{
				ContentItem item = archive.getOriginalContent(oldjunk);
				archive.getPageManager().getRepository().remove(item);
				log.info("Deduplication deleted " + item.getAbsolutePath());
				archive.removeGeneratedImages(oldjunk);
			}
			searcher.deleteAll(todelete, null)
		}
	}
	log.info("Final Saving and Deleting " + tosave.size() + " " + todelete.size());
	searcher.saveAllData(tosave, null);
	for(Asset oldjunk : todelete)
	{
		ContentItem item = archive.getOriginalContent(oldjunk);
		archive.getPageManager().getRepository().remove(item);
		log.info("Deduplication deleted " + item.getAbsolutePath());
		archive.removeGeneratedImages(oldjunk);
	}	
	searcher.deleteAll(todelete, null)
		
}

public void fixField(String inField, Asset todelete, Asset goodone)
{
	String val = todelete.getValue(inField);
	if( val != null && goodone.getValue(inField) == null )
	{
		goodone.setValue(inField,val);
	}
}

init();
