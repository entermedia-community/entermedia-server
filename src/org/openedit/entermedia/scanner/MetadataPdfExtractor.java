package org.openedit.entermedia.scanner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;
import com.openedit.util.PathUtilities;

public class MetadataPdfExtractor extends MetadataExtractor
{
	public boolean extractData(MediaArchive inArchive, File inFile, Asset inAsset)
	{
		String type = PathUtilities.extractPageType(inFile.getPath());
		if (type == null || "data".equals(type.toLowerCase()))
		{
			type = inAsset.get("fileformat");
		}

		if (type != null)
		{
			type = type.toLowerCase();
			if( inAsset.get("fileformat") == null)
			{
				inAsset.setProperty("fileformat", type);
			}
			if (type.equals("pdf"))
			{
				PdfParser parser = new PdfParser();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream in;
				try
				{
					in = new FileInputStream(inFile);
					try
					{
						new OutputFiller().fill(in, out);
					}
					finally
					{
						FileUtils.safeClose(in);
					}
					byte[] bytes = out.toByteArray();
					Parse results = parser.parse(bytes); //Do we deal with encoding?
					//We need to limit this size
					inAsset.setProperty("fulltext", results.getText());
					if( inAsset.getInt("width") == 0)
					{
						String val = results.get("width");
						inAsset.setProperty("width", val);
					}
					if( inAsset.getInt("height") == 0)
					{
						String val = results.get("height");
						inAsset.setProperty("height", val);
					}
					inAsset.setProperty("pages", String.valueOf(results.getPages()));
					if (inAsset.getProperty("assettitle") == null)
					{
						String title  = results.getTitle();
						if( title != null && title.length() < 300)
						{
							inAsset.setProperty("assettitle", title);
						}
					}

				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
					return false;
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return false;
				}

				return true;
			}
		}
		return false;
	}

}
