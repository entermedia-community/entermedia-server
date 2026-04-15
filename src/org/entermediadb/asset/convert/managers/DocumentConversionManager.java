package org.entermediadb.asset.convert.managers;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.MediaTranscoder;
import org.entermediadb.asset.convert.transcoders.CMYKTranscoder;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public class DocumentConversionManager extends ImageConversionManager
{
	private static final Log log = LogFactory.getLog(DocumentConversionManager.class);

	Collection pdfFormats = Arrays.asList("gddoc", "gdsheet", "gdslide", "gddraw", "doc", "docx", "rtf", "ppt", "pptx", "wps", "odt", "xls", "xlsx", "odp");

	public ConvertResult transcode(ConvertInstructions instructions)
	{
		// if output == jpg and no time offset - standard
		String fileFormat = instructions.getAsset().getFileFormat();

		if (pdfFormats.contains(fileFormat))
		{
			Data preset = getMediaArchive().getPresetManager().getPresetByOutputNameCached(instructions.getMediaArchive(), "document", "document.pdf");
			ConvertInstructions instructions2 = createInstructions(instructions.getAsset(), preset);

			// Always have a PDF version of all document formats
			instructions2.setInputFile(instructions.getOriginalDocument());

			MediaTranscoder findTranscoder = findTranscoder(instructions2);
			ConvertResult result = findTranscoder.convertIfNeeded(instructions2);
			log.info("Created document.pdf");

			if (instructions.getOutputExtension().equals("pdf"))
			{
				return result; // Not expected to be here, but just in case
			}
			// set the imput for the next conversion
			instructions.setInputFile(instructions2.getOutputFile());

		}

		return super.transcode(instructions);

	}

	protected String getRenderType()
	{
		return "document";
	}

}
