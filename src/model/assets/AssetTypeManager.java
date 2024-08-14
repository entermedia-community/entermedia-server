package model.assets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.*;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.scripts.EnterMediaObject;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.PathUtilities;



public class AssetTypeManager extends EnterMediaObject
{

	public void setAssetTypes(Collection<Asset> inAssets, boolean force)
	{
		MediaArchive mediaarchive = (MediaArchive) context.getPageValue("mediaarchive");//Search for all files looking for videos

		AssetSearcher searcher = mediaarchive.getAssetSearcher();

		for (Asset hit : inAssets)
		{
			checkForEdits(mediaarchive, hit, force);
			checkLibrary(mediaarchive, hit);
			checkCustomFields(mediaarchive, hit);
		}
		validateAssetTypes(mediaarchive,inAssets);
	}

	public void validateAssetTypes(MediaArchive mediaarchive, Collection<Asset> inAssets)
	{
		//Read the fileformat and validate the extention to that type
		//MediaArchive archive = getMediaArchive(inReq);
		
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String detected = data.get("detectedfileformat");
			if( detected != null)
			{
				//Make sure they match?
				if( detected.toLowerCase().endsWith("exe") )
				{
					String fileformat = data.get("fileformat");
					if( !fileformat.equals("exe"))
					{
						data.setValue("importstatus", "invalidformat");
						data.setValue("fileformat", "exe");
						data.setValue("assettype", "none");
						data.setProperty("previewstatus","mime");
					}
				}
			}
		}
	}	
	
	protected void checkCustomFields(MediaArchive inArchive, Asset inHit)
	{
	}

	public void checkForEdits(MediaArchive inArchive,  Asset hit, boolean always)
	{
		if( hit.get("assettype") != null && !always)
		{
			return;
		}
		
		String sourcepath = hit.get("sourcepath");
		Searcher typesearcher = inArchive.getSearcher("assettype");
		String fileformat = hit.get("fileformat");
		//First check to see if we have a path mask for the asset type

		HitTracker types = typesearcher.query().all().sort("ordering").named("typehits").search(context);
		for (Iterator iterator = types.iterator(); iterator.hasNext();)
		{
			Data it = (Data) iterator.next();
			String paths =it.get("paths");
			String type = it.getId();

			if(paths != null)
			{
				String[] splits = paths.split(",");
				for(String pathcheck : splits)
				{
					if(PathUtilities.match(sourcepath, pathcheck))
					{
						hit.setProperty("assettype", type);
					}
				}
			}
		}

		//now check extensions
		HashMap<String,String> typemap = new HashMap();
		for (Iterator iterator = types.iterator(); iterator.hasNext();)
		{
			Data it = (Data) iterator.next();
			String extentions = it.get("extensions");
			String type = it.getId();
			if(extentions != null)
			{
				String[] splits = extentions.split(" ");
				for(String ext  : splits )
				{
					typemap.put(ext, type);
				}
			}
		}
	
		String currentassettype = hit.get("assettype");  //Could have been set by the user
		String assettype = typemap.get(fileformat);
		if(assettype == null)
		{
			Data found = inArchive.getDefaultAssetTypeForFile(hit.getName());
			if( found != null)
			{
				assettype = found.getId();
			}
		}
		assettype = findCorrectAssetType(hit,assettype);
		
		if(currentassettype == null || currentassettype.length()==0 || !assettype.equals(currentassettype))
		{
			hit.setProperty("assettype", assettype);
		}
		else 
		{
			return;
		}
	}

	public Asset checkLibrary(MediaArchive mediaarchive, Asset real)
	{
		//Load up asset if needed to change the library?
		return real;
	}

	public void saveAssets(Searcher inSearcher, Collection tosave)
	{
		//Do any other checks on the asset. Add to library?
		inSearcher.saveAllData(tosave, context.getUser());
	}

	public String findCorrectAssetType(Data inAssetHit, String inSuggested)
	{
		/*
		 * String path = inAssetHit.getSourcePath().toLowercase(); if(
		 * path.contains("/links/") ) { return "links"; } if(
		 * path.contains("/press ready pdf/") || path.endsWith("_pfinal.pdf") )
		 * { return "printreadyfinal"; }
		 */
		return inSuggested;
	}

}