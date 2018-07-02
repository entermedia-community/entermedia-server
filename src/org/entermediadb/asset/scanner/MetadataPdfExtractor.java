package org.entermediadb.asset.scanner;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;

public class MetadataPdfExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(MetadataPdfExtractor.class);
	protected PageManager fieldPageManager;
	OutputFiller filler = new OutputFiller();
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
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

//				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream in = null;
				try
				{
					long maxsize = 100000000;
					String sizeval = inArchive.getCatalogSettingValue("maxpdfsize");
					if(sizeval != null){
						maxsize = Long.valueOf(sizeval);
					}
					if(inFile.getLength() > maxsize){
						log.info("PDF was too large to extract metadata. Consider increasing max size: " + sizeval);
						return false;
					}
						

					in = inFile.getInputStream();
//					try
//					{
//						new OutputFiller().fill(in, out);
//					}
//					finally
//					{
//						FileUtils.safeClose(in);
//					}
//					byte[] bytes = out.toByteArray();
					
					
					Parse results = parser.parse(in); //Do we deal with encoding?
					//We need to limit this size
					String fulltext = results.getText();
					if( fulltext != null && fulltext.length() > 0)
					{
						
						ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + inArchive.getCatalogId() +"/assets/" + inAsset.getSourcePath() + "/fulltext.txt");
						if( item instanceof FileItem)
						{
							((FileItem)item).getFile().getParentFile().mkdirs();
						}
						PrintWriter output = new PrintWriter(item.getOutputStream());
						filler.fill(new StringReader(fulltext), output );
						filler.close(output);
						inAsset.setProperty("hasfulltext", "true");
					}
					
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
					if (inAsset.get("assettitle") == null)
					{
						String title  = results.getTitle();
						if( title != null && title.length() < 300)
						{
							inAsset.setProperty("assettitle", title);
						}
					}

				}
				catch( Exception ex)
				{
					log.error("cant process" , ex);
					return false;
				}
				finally
				{
					FileUtils.safeClose(in);
				}

				return true;
			}
		}
		return false;
	}

}
