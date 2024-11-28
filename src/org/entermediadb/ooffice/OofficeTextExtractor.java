package org.entermediadb.ooffice;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.scanner.ExiftoolMetadataExtractor;
import org.entermediadb.asset.scanner.MetadataExtractor;
import org.entermediadb.asset.scanner.MetadataPdfExtractor;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class OofficeTextExtractor extends MetadataExtractor
{
	public static final Collection FORMATS = Arrays.asList(new String[] {"doc","docx","rtf","ppt","pptx","wps","odt"}); //,"html","xml","csv", "xls", "xlsx"

	private static final Log log = LogFactory.getLog(OofficeTextExtractor.class);
	
	protected MetadataPdfExtractor fieldMetadataPdfExtractor;
	protected ExiftoolMetadataExtractor fieldExiftoolMetadataExtractor;

	public MetadataPdfExtractor getMetadataPdfExtractor()
	{
		return fieldMetadataPdfExtractor;
	}
	
	public ExiftoolMetadataExtractor getExiftoolMetadataExtractor()
	{
		return fieldExiftoolMetadataExtractor;
	}

	public void setMetadataPdfExtractor(MetadataPdfExtractor inMetadataPdfExtractor)
	{
		fieldMetadataPdfExtractor = inMetadataPdfExtractor;
	}
	public void setExiftoolMetadataExtractor(ExiftoolMetadataExtractor inExiftoolMetadataExtractor)
	{
		fieldExiftoolMetadataExtractor = inExiftoolMetadataExtractor;
	}

	public boolean extractData(MediaArchive inArchive, ContentItem inputFile, Asset inAsset)
	{
		String type = PathUtilities.extractPageType(inputFile.getPath());
		if (type == null || "data".equals(type.toLowerCase()))
		{
			type = inAsset.getFileFormat();
		}

		if (type != null)
		{
			type = type.toLowerCase();
		}
		if( type == null )
		{
			return false;
		}
		if ( type.equals("pdf"))
		{
			return false; //PDF's are already being extracted
		}
		
		if( !FORMATS.contains(type) )
		{
			return false;
		}
		
		ContentItem custom = inArchive.getContent( "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/document.pdf");
		if( !custom.exists() )
		{
	        ConversionManager c = inArchive.getTranscodeTools().getManagerByRenderType("document");
			ConvertInstructions instructions = c.createInstructions(inAsset);
			
			instructions.setAssetSourcePath(inAsset.getSourcePath());
			instructions.setAsset(inAsset);
			instructions.setOutputExtension("pdf");
			instructions.setInputFile(inputFile);
			instructions.setOutputFile(custom);
			c.createOutput(instructions);
		}
	 	//c.(instructions);

		//ooffice can create a PDF then we can pull the size and text from it
		
		//String tmppath = getMediaCreator().populateOutputPath(inArchive, inst);
		
		//use exiftool to extract standard data
		getExiftoolMetadataExtractor().extractData(inArchive, custom, inAsset);
		//now use the PDF extractor
		getMetadataPdfExtractor().extractData(inArchive, custom, inAsset);
		

		
//		//now get the page info out of the PDF?
//		Asset tmp = inArchive.createAsset("tmp/" + inAsset.getSourcePath());
//		getPdfMetadataExtractor().extractData(inArchive, pdf, tmp);
//		
//		inAsset.setProperty(type, tmppath)
		
		return true;
	}
}
