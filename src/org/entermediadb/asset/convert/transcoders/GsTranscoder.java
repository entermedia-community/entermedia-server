package org.entermediadb.asset.convert.transcoders;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

public class GsTranscoder extends BaseTranscoder
{
	private static final Log log = LogFactory.getLog(GsTranscoder.class);

	
	@Override
	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOutput(inStructions.getOutputFile());
		Asset asset = inStructions.getAsset();
		ContentItem inOutFile = inStructions.getOutputFile();
		String outputpath = inOutFile.getAbsolutePath();

		String ext = null;
	
		List<String> com = createCommand(inStructions);
		//sOutputFile=cover.png -r144 cover.pdf	
		com.add("-sOutputFile=" + outputpath);
		com.add(inStructions.getInputFile().getAbsolutePath() );
		long start = System.currentTimeMillis();
		new File(outputpath).getParentFile().mkdirs();

		long timeout = inStructions.getConversionTimeout();
		ExecResult execresult = getExec().runExec("gs", com, false, timeout);

		boolean ok = execresult.isRunOk();
		result.setOk(ok);

		if (ok)
		{
			result.setComplete(true);

			log.info("Convert complete in:" + (System.currentTimeMillis() - start) + " " + inOutFile.getName());

			return result;
		}
		//problems
		log.info("Could not exec: " + com  + execresult.getStandardOut());
		if (execresult.getReturnValue() == 124)
		{
			result.setError("Exec timed out after " + timeout);
		}
		else
		{
			result.setError(execresult.getStandardOut());
		}
		return result;
	}


	protected List<String> createCommand(ConvertInstructions inStructions)
	{
		List<String> com = new ArrayList<String>();
	
		int page = inStructions.getPageNumber();
		//page--;
		page = Math.max(1, page);
		
		com.add("-sDEVICE=pngalpha");
//		com.add("-sPageList=" + page );
		com.add("-dFirstPage=" + page);
		com.add("-dLastPage=" + page);
		com.add("-q");
		com.add("-dBATCH");
		com.add("-dQUIET");
		com.add("-dSAFER");
		com.add("-dNOPAUSE");
		com.add("-dNOPROMPT");
		com.add("-dMaxBitmap=500000000");
		com.add("-dAlignToPixels=0");
		com.add("-dGridFitTT=2");
		com.add("-dTextAlphaBits=4");
		com.add("-dUseCIEColor");
		//com.add("-r72x72");
		//com.add("-r200");
		com.add("-r150x150");
		
		//gs -sDEVICE=pngalpha -sPageList=1 -q -dQUIET   -dBATCH  -dSAFER -dNOPAUSE -dNOPROMPT -dMaxBitmap=500000000 -dAlignToPixels=0 -dGridFitTT=2 -dTextAlphaBits=4 -r72x72 -sOutputFile="/home/shanti/git/demoall/webapp/WEB-INF/data/assets/catalog/generated/2017/11/34/49934288d/Villages in the Sky (1).pdf/image1500x1500.png" "/home/shanti/git/demoall/webapp/WEB-INF/data/assets/catalog/originals/2017/11/34/49934288d/Villages in the Sky (1).pdf"

		
		//-q -dQUIET -dSAFER -dBATCH -dNOPAUSE -dNOPROMPT -dMaxBitmap=500000000 -dAlignToPixels=0 -dGridFitTT=2 "-sDEVICE=pngalpha" 
		//-dTextAlphaBits=4 -dGraphicsAlphaBits=4 "-r72x72"
		//com.add("-r300");
//		-r144 cover.pdf");
//		com.add("thread");
//		com.add("1");
		return com;
	}

}
