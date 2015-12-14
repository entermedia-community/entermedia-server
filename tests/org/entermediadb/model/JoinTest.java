package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class JoinTest extends BaseEnterMediaTest
{
	public void testFindLibraries() throws Exception
	{
		Searcher lsearcher  = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "library");
		Searcher dsearcher  = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "division");

		//  <property id="division.tag" type="searchjoin" viewtype="text" index="false" listid="division" editable="false" stored="false" >Division Tag</property>


		Data division = dsearcher.createNewData();
		division.setProperty("tag", "test");
		dsearcher.saveData(division,null);
		
		Data ldata = lsearcher.createNewData();
		ldata.setProperty("division", division.getId() );
		ldata.setName("stuff");
		lsearcher.saveData(ldata,null);

		SearchQuery dq = dsearcher.createSearchQuery();
		dq.addExact("tag", "test");
		
		//from libraries 
		SearchQuery  lq = lsearcher.createSearchQuery();
		lq.addMatches("name","stuff");
		lq.addExact("division.tag","test");
		//lq.addRemoteJoin(dq, "id", false, "division", "division");
		HitTracker hits = lsearcher.search(lq);
		assertTrue( hits.size() > 0);
		
		//Parent Joins. Give me the parents based on a child thing: divistions where library tag = "dog"
		//1. Give me the children based on a parent thing?   libraries where division status is enabled

	
	}
}
