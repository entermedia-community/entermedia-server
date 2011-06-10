package publishing.publishers
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher

import com.openedit.page.Page

public abstract class basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(basepublisher.class);

	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, String presetid)
	{
		Page inputpage=null;
		if(presetid != null )
		{
			Data preset = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "convertpreset", presetid);
			if( preset.get("type") != "original")
			{
				String input= "/WEB-INF/data/${mediaArchive.catalogId}/generated/${asset.sourcepath}/${preset.outputfile}";
				inputpage= mediaArchive.getPageManager().getPage(input);
			}
		}
		if( inputpage == null)
		{
			inputpage = mediaArchive.getOriginalDocument(asset);
		}
		return inputpage;
	}
}