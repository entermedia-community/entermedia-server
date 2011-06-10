package org.openedit.entermedia.model;

import org.openedit.data.Searcher;
import org.openedit.data.lucene.NumberUtils;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.AssetEditModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

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

		mod.voteForAsset(asset, archive, req.getUser());

		HitTracker morehits = searcher.fieldSearch("assetid", "101");
		assertEquals( before + 1, morehits.size() );
		
		//Now search the count
	}
	
	public void testVotingCountSearch() throws Exception
	{
		EnterMedia media = getEnterMedia("entermedia");
		MediaArchive archive = media.getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset("101");
		
		WebPageRequest req = getFixture().createPageRequest();
		AssetEditModule mod = (AssetEditModule)getFixture().getModuleManager().getModule("AssetEditModule");

		mod.removeVote(asset, archive, req.getUser());

		Searcher searcher = media.getSearcherManager().getSearcher(archive.getCatalogId(), "asset");
		HitTracker hits = searcher.fieldSearch("assetvotes.count_sortable", new NumberUtils().long2sortableStr(1));
		int before = hits.size();

		mod.voteForAsset(asset, archive, req.getUser());

		HitTracker morehits = searcher.fieldSearch("assetvotes.count_sortable", new NumberUtils().long2sortableStr(1));
		assertEquals( before + 1, morehits.size() );
		
		//Now search the count
	}
	
	
}
