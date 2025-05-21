package org.entermediadb.asset.scanner;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.jsoup.Jsoup;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;

public class TextMetadataExtractor extends MetadataExtractor {
	private static final Log log = LogFactory.getLog(TextMetadataExtractor.class);
	protected PageManager fieldPageManager;
	OutputFiller filler = new OutputFiller();

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset) {
		String[] supportedTypes = new String[] { "text", "xml" };
		String type = PathUtilities.extractPageType(inFile.getName());

		if (type != null) {
			String mediatype = inArchive.getMediaRenderType(type);
			if (!Arrays.asList(supportedTypes).contains(mediatype)) {
				return false;
			}
		}

		try (InputStream input = inFile.getInputStream()) {
			String fulltext = Jsoup.parse(input, "UTF-8", "").text();

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
			
			
			
			

			// Optionally save the stripped text to a file or log it
			log.info("Extracted text for asset: " + inAsset.getId());

			return true; // Successfully extracted text
		} catch (Exception e) {

			log.error("Failed to extract text from file: " + inFile.getPath(), e);
			return false; // Extraction failed

		}

	}

}
