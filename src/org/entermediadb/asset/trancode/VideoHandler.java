package org.entermediadb.asset.trancode;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.manage.PageManager;
import org.openedit.util.Exec;
import org.openedit.util.PathUtilities;

public class VideoHandler extends BaseHandler implements TranscodeHandler
{

	@Override
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath, String inOutputType)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPageManager(PageManager inPageManager)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExec(Exec inExec)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInputFinders(Collection inList)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String calculateOutputPath(ConvertInstructions inStructions)
	{
		StringBuffer outputpage = new StringBuffer();
		outputpage.append("/WEB-INF/data/" );
		outputpage.append(getMediaArchive().getCatalogId());
		outputpage.append("/generated/" );
		outputpage.append(inStructions.getAssetSourcePath() );
		outputpage.append("/" );
//		String output = inPreset.get("outputfile");
//		int pagenumber = inStructions.getPageNumber();
//		if( pagenumber > 1 )
//		{
//			String name = PathUtilities.extractPageName(output);
//			String ext = PathUtilities.extractPageType(output);
//			output = name + "page" + pagenumber + "." + ext;
//		}
//		outputpage.append(output);

		return outputpage.toString();
	}
}
