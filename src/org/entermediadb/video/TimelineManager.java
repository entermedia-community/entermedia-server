package org.entermediadb.video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.data.Searcher;

public class TimelineManager
{
	public Collection searchInVideo(MediaArchive inArchive, Asset inAsset, String searchby)
	{
		
		Searcher captionsearcher = inArchive.getSearcher("videotrack");
		
		if( searchby == null)
		{
			searchby = "*";
		}
		List<SearchResult> results = new ArrayList();
		
		//closed captions
		Collection tracks = captionsearcher.query().exact("assetid", inAsset.getId()).contains("captions.cliplabel", searchby).sort("timecodestart").search();
		for (Iterator iterator = tracks.iterator(); iterator.hasNext();)
		{
			Data videotrack = (Data) iterator.next();
			
			Collection captions = videotrack.getValues("captions");
			for (Iterator iterator2 = captions.iterator(); iterator2.hasNext();)
			{
				Map caption = (Map) iterator2.next();
				String label = (String)caption.get("cliplabel");
				if( matchesText(searchby,label))
				{
					Object start = (Object)caption.get("timecodestart");
					results.add(createResult("closedcaption", label,Double.parseDouble(start.toString())));			
				}
			}
		}
		
		//Load up the tracks and find all the mentions by hand
		Collection faceprofiles = inAsset.getValues("faceprofiles");
		if( faceprofiles != null)
		{
			for (Iterator iterator = faceprofiles.iterator(); iterator.hasNext();)
			{
				Map<String,Object> profile = (Map) iterator.next();
				//data.put( "timecodestart",profile.get("timecodestart"));
				String id = (String)profile.get("faceprofilegroup");
				Object start = profile.get("timecodestart");

				if( id != null && start != null)
				{
					Data group = inArchive.getCachedData("faceprofilegroup", id);
					if( group != null && matchesText(searchby,group.getName()))
					{
						double num = Double.parseDouble(start.toString());
						
						String name = group.getName();
						if( name == null)
						{
							name = group.get("facecounter");
						}
						
						results.add(createResult("faceprofile", name,num));
					}
				}
			}
		}
		
		//tags
		Collection clips = inAsset.getValues("clips");
		if( clips != null)
		{
			for (Iterator iterator = clips.iterator(); iterator.hasNext();)
			{
				Map<String,Object> clip = (Map) iterator.next();
				String label = (String)clip.get("cliplabel");
				if( matchesText(searchby,label))
				{
					Object start = clip.get("timecodestart");
					if( start != null)
					{
						results.add(createResult("clip", label,Double.parseDouble( start.toString() ) ) );
					}
				}
			}
		}
		
		Collections.sort(results,new Comparator<SearchResult>()
		{
			@Override
			public int compare(SearchResult inArg0, SearchResult inArg1)
			{
				if( inArg0.getStartTime() == inArg1.getStartTime() )
				{
					return 0;
				}
				if( inArg0.getStartTime() < inArg1.getStartTime() )
				{
					return -1;
				}
				return 1;
			}
		});
		
		/*
		Show Closed captions
		Show faceprofiles
		Show Tags
		Sort by time
		*/
		return results;

	}

	protected SearchResult createResult(String inType, String inName, Double inStart)
	{
		SearchResult result = new SearchResult();
		result.setType(inType);
		result.setLabel(inName);
		result.setStartTime(inStart);
		return result;
	}

	protected boolean matchesText(String inSearchby, String inName)
	{
		if( inName == null || inName.isEmpty())
		{
			return false;
		}
		if( inSearchby.equals("*"))
		{
			return true;
		}
		boolean contains = inName.toLowerCase().contains(inSearchby.toLowerCase());
		return contains;
	}
}
