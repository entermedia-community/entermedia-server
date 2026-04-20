package org.entermediadb.ai.automation.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.scanner.MetaDataReader;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;

public class ProcessNeedsMetadataAgent extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{
		Collection<Asset> assets = (Collection<Asset>) inContext.getContextValue("hits");
		Searcher searcher = getMediaArchive().getAssetSearcher();
		List assetsToSave = new ArrayList();
		MetaDataReader reader = (MetaDataReader) getModuleManager().getBean("metaDataReader");
		for (Data hit : assets)
		{
			Asset asset = (Asset) searcher.loadData(hit);
			// log.info("${asset.getSourcePath()}");
			if (asset != null)
			{
				ContentItem content = getMediaArchive().getOriginalContent(asset);
				reader.populateAsset(getMediaArchive(), content, asset);
				asset.setProperty("importstatus", "imported");
				assetsToSave.add(asset);
				if (assetsToSave.size() == 100)
				{
					getMediaArchive().saveAssets(assetsToSave);
					// archive.firePathEvent("importing/assetsimported",user,assetsToSave);
					assetsToSave.clear();
					inContext.info("saved 100 metadata readings");
				}
			}
		}
		getMediaArchive().saveAssets(assetsToSave);
		inContext.info("read " + assetsToSave.size() + " metadata readings");

		super.process(inContext);
	}
}
