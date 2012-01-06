package org.entermedia.creator;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.BaseCreator;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.ConvertResult;

import com.openedit.OpenEditException;

import com.openedit.page.Page;

public class oofficeDocumentCreator extends BaseCreator
{
	protected final def formats = ["doc","docx","rtf","ppt","wps","odt","html","xml","csv", "xls", "xlsx"];
	protected OfficeManager fieldOfficeManager;
	private static final Log log = LogFactory.getLog(OofficeDocumentCreator.class);
	
	public boolean canReadIn(MediaArchive inArchive, String inInputType)
	{
		for (int i = 0; i < formats.length; i++)
		{
			if( inInputType.equals(formats[i]))
			{
				return true;
			}
		}
		return false;
	}
	protected OfficeManager getOfficeManager()
	{
		if (fieldOfficeManager == null)
		{
			String path = "/usr/lib/libreoffice";
			if(new File(path).exists() == false )
			{
				path = "/usr/lib/openoffice";
				if(new File(path).exists() == false )
				{
					throw new OpenEditException("Could not find path to open office");
				}
			}
			OfficeManager temp = new DefaultOfficeManagerConfiguration()
		      .setOfficeHome(path)
//		      .setConnectionProtocol(OfficeConnectionProtocol.PIPE)
//		      .setPipeNames("office1", "office2")
		      .setTaskExecutionTimeout(30000L)
		      .buildOfficeManager();

			//make sure any old sooffice.bin are off
			temp.start();

			fieldOfficeManager = temp;
		}

		return fieldOfficeManager;
	}
	public synchronized ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOk(false);
		
		Page input = inArchive.findOriginalMediaByType("document",inAsset);
		if( input == null )
		{
			return result;
		}
		OfficeDocumentConverter converter = new OfficeDocumentConverter(getOfficeManager());
	    
	    File inputfile = new File(input.getContentItem().getAbsolutePath());
	    File outfile = new File(inOut.getContentItem().getAbsolutePath());
	    outfile.getParentFile().mkdirs();
	    converter.convert(inputfile, outfile);
	    result.setOk(true);
	    return result;
	}

	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		//we only generate PDF for now
		StringBuffer path = new StringBuffer();

		//legacy for people who want to keep their images in the old location
		String prefix = inStructions.getProperty("pathprefix");
		if( prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(inArchive.getCatalogHome());
			path.append("/generated/");
		}
		path.append(inStructions.getAssetSourcePath());
		path.append("/");

		String postfix = inStructions.getProperty("pathpostfix");
		if( postfix != null)
		{
			path.append(postfix);
		}	
		inStructions.setOutputExtension("pdf");
		path.append("document." + inStructions.getOutputExtension());

		inStructions.setOutputPath(path.toString());
		return path.toString();
	}

}
