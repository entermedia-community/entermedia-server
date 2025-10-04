package org.entermediadb.model;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.AssetEditModule;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class VotingTest extends BaseEnterMediaTest
{
	public void testVoting() throws Exception
	{
		EnterMedia media = getEnterMedia("entermedia");
		MediaArchive archive = media.getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset("101");
		
		WebPageRequest req = getFixture().createPageRequest();
		AssetEditModule mod = (AssetEditModule)getFixture().getModuleManager().getModule("AssetEditModule");

		mod.removeVote(asset, archive, req.getUser());

		Searcher searcher = media.getSearcherManager().getSearcher(archive.getCatalogId(), "assetvotes");
		HitTracker hits = searcher.fieldSearch("assetid", "101");
		int before = hits.size();

		mod.voteForAsset(req);

		HitTracker morehits = searcher.fieldSearch("assetid", "101");
		assertEquals( before + 1, morehits.size() );
		
		//Now search the count
	}
	
	
	
	
}
