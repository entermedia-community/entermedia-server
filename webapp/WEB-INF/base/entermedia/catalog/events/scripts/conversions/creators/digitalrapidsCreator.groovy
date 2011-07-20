package conversions.creators;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.ConvertResult;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.BaseCreator 
import org.openedit.entermedia.creator.ConvertInstructions 
import org.openedit.entermedia.creator.ConvertResult 
import org.openedit.entermedia.creator.MediaCreator 
import com.openedit.OpenEditException 
import com.openedit.page.Page;
import com.openedit.util.FileUtils;
import org.openedit.Data;

public class digitalrapidsCreator extends BaseCreator implements MediaCreator
{
	private static final Log log = LogFactory.getLog(this.class);

	protected FileUtils fieldFileUtils;
	
	
	public boolean canReadIn(MediaArchive inArchive, String inInput)
	{
		return true;//"flv".equals(inOutput) || mpeg; //This has a bunch of types
	}
	

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page converted, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();

		Page inputpage = inArchive.findOriginalMediaByType("video",inAsset);
		if( inputpage == null || !inputpage.exists())
		{
			//no such original
			log.info("Input does not exist: " + inAsset.getSourcePath());
			result.setOk(false);
			return result;
		}
		
		String inputfolderholding = inStructions.getProperty("inputfolderholding");
		if( inputfolderholding == null)
		{
			throw new OpenEditException("No inputfolderholding property set");
		}
		File folder = new File(inputfolderholding);
		folder.mkdirs();
		
		FileOutputStream output = new FileOutputStream(inputfolderholding + "/" + inputpage.getName() );
		InputStream inputstream  = inputpage.getInputStream();
		try
		{
			getFileUtils().copyFiles(inputstream, output);
		}
		finally
		{
			getFileUtils().safeClose(inputstream);
			getFileUtils().safeClose(output);
		}
		result.setOk(true);
		result.setComplete(false);
		return result;
	}
	public ConvertResult updateStatus(MediaArchive inArchive,Data inTask, Asset inAsset,ConvertInstructions inStructions )
	{
		String completefolder = inStructions.getProperty("outputfoldercomplete");
		if( completefolder == null)
		{
			throw new OpenEditException("No outputfoldercomplete property set");
		}
		
		String postfix = inStructions.getProperty("outputfilenamepostfix");
		File tempname = new File( completefolder, inAsset.getName() + postfix);
		
		ConvertResult result = new ConvertResult();
		result.setOk(true);
		if( tempname.exists() )
		{
			String catalogid = inArchive.getCatalogId();
			//TODO: use task variable for proxy filename
			Page proxy = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated/" + inAsset.getSourcePath() + "/video.mp4");
			log.info("moving proxy to " + proxy);
			getFileUtils().move(tempname.getAbsolutePath(), proxy.getContentItem().getAbsolutePath() );
			result.setComplete(true);
		}
		return result;
	}

	
	public ConvertResult applyWaterMark(MediaArchive inArchive, File inConverted, File inWatermarked, ConvertInstructions inStructions)
	{
		return null;
	}
	
	public String createConvertPath(ConvertInstructions inStructions)
	{
		String path = inStructions.getAssetSourcePath();
		
		return path;
	}
	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		Asset asset = inArchive.getAssetBySourcePath(inStructions.getAssetSourcePath());
		Page inputpage = inArchive.getOriginalDocument(asset);
		inStructions.setOutputPath(inputpage.getPath());
		return inStructions.getOutputPath();
	}
	protected FileUtils getFileUtils()
	{
		if (fieldFileUtils == null)
		{
			fieldFileUtils = new FileUtils();
		}
		return fieldFileUtils;
	}
	
}