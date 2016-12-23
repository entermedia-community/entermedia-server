package org.entermediadb.asset.convert.transcoders;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.InputLoader;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;

public class ExiftoolThumbTranscoder extends BaseTranscoder
{
	@Override
	public ConvertResult convert(ConvertInstructions inStructions)
	{
		log.info("Extracting thumb from");

		ConvertResult result = new ConvertResult();
		result.setOutput(inStructions.getOutputFile());

		result.setOk(false);
		
		ContentItem item = inStructions.getOutputFile();
		File output = new File( item.getAbsolutePath() );
		output.getParentFile().mkdirs();
		List base = new ArrayList();
		//command.add("-b");
		//command.add("-ThumbnailImage");
		base.add(inStructions.getInputFile().getAbsolutePath());
		//command.add("-o");
		base.add(item.getAbsolutePath());

		List command = new ArrayList(base);			
		command.add("PageImage");
		long timeout = inStructions.getConversionTimeout();
		ExecResult done = getExec().runExec("exiftoolthumb",command,timeout);
		if(output.length() == 0)
		{
			command = new ArrayList(base);
			command.add("ThumbnailImage");
			done = getExec().runExec("exiftoolthumb",command,timeout);
		}	
		result.setOk(done.isRunOk());
		
		if(output.length() == 0){
			output.delete();
			result.setOk(false);
			result.setError("no embeded thumbnail found in file");
		}
		return result;
	}


	
}
