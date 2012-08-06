package importing;

import org.openedit.Data;
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive

import assets.model.AssetTypeManager
import assets.model.EmailNotifier;

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.manage.*;

public void setAssetTypes()
{
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );

	HitTracker assets = assetsearcher.search(q);
	AssetTypeManager manager = new LibraryChecker();
	manager.context = context;
	manager.saveAssetTypes(assets);
}
public void sendEmail()
{
	EmailNotifier emailer = new EmailNotifier();
	emailer.context = context;
	emailer.emailOnImport();
}

class LibraryChecker extends AssetTypeManager
{
	/*
	public Asset checkLibrary(MediaArchive mediaarchive, Data hit)
	{
		if( hit.get("assettype") == "printreadyfinal" )
		{
			String libraries = hit.get("libraries");
			if( libraries == null || !libraries.contains("printreadyfinal") )
			{
				Asset real = mediaarchive.getAssetBySourcePath(hit.getSourcePath());
				return checkLibrary(mediaarchive,real);
			}
		}
		return null;
	}
	public Asset checkLibrary(MediaArchive mediaarchive, Asset real)
	{
		if( real.get("assettype") == "printreadyfinal" )
		{
			Collection values = real.getValues("libraries");
			if( !values.contains("printreadyfinal") )
			{
				real.addValue("libraries", "printreadyfinal");
			}
		}
		return real;
	}
	public String findCorrectAssetType(Data inAssetHit, String inSuggested)
	{
		String path = inAssetHit.getSourcePath().toLowercase();
		if( path.contains("/links/") )
		{
			return "links";
		}
		if( path.contains("/press ready pdf/") || path.endsWith("_pfinal.pdf") )
		{
			return "printreadyfinal";
		}
		return inSuggested;
	}	
	*/
}


setAssetTypes();
//sendEmail();
