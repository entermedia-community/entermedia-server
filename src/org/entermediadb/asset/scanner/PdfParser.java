package org.entermediadb.asset.scanner;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfParser
{
	private static final Log log = LogFactory.getLog(PdfParser.class);

	public Parse parse(InputStream inContent)
	{
		Parse results = new Parse();
		PDDocument pdf = null;
		try
		{
			
			pdf = PDDocument.load(inContent,"");

			// collect text
			PDFTextStripper stripper = new PDFTextStripper();
			
			//TODO: Write this out to a temp file that will be indexed seperately
			
			String text = null;
			String title = null;
			
			try{
				text = stripper.getText(pdf);
			}
			catch(Throwable e)
			{
				e.printStackTrace();
				log.error("Could not parse" , e);
				text = "";
			}
			text = scrubChars(text);
			results.setText(text);
			results.setPages(pdf.getNumberOfPages());

			// collect title
			PDDocumentInformation info = pdf.getDocumentInformation();
			title = info.getTitle();
			results.setTitle(title);
			if( pdf.getNumberOfPages()  > 0)
			{
				PDPage page = (PDPage)pdf.getDocumentCatalog().getPages().get(0);
				PDRectangle mediaBox = page.getMediaBox();
				if( mediaBox == null)
				{
					mediaBox = page.getArtBox();
				}
				if( mediaBox != null)
				{
					results.put("width", String.valueOf(Math.round(  mediaBox.getWidth()) ));
					results.put("height", String.valueOf(Math.round(  mediaBox.getHeight()) ));
				}
			}
	
			//Thread.sleep(500); // Slow down PDF's loading
		}
		catch (Exception e)
		{
			log.error("Can't be handled as pdf document. " + e);
		} 
		finally
		{
			try
			{
				if (pdf != null)
				{
					pdf.close();
				}
			} catch (IOException e)
			{
				// nothing to do
				log.info(e);
			}
		}
		return results;
	}
	protected String scrubChars(String inVal)
	{
		StringBuffer done = new StringBuffer(inVal.length());
		for (int i = 0; i < inVal.length(); i++)
		{
			char c = inVal.charAt(i);
			switch (c)
			{
				 case '\t':
		         case '\n':
		         case '\r':
		        	 done.append(c); //these are safe
		        	 break;
		         default:
		         {
		 			if (c > 31) //other skip unless over 31
					{
						done.append(c); 
					}
		         }
			}
		}
		return done.toString();
	}
/*
	public Parse getParse(Content content) throws OpenEditException
	{
		log.info("Parse " + content.getUrl());
		Parse results = null;

		try
		{
			byte[] raw = content.getContent();
			if (raw == null)
			{
				return null;
			}
			
			results = parse(raw);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		return results;
	}
*/	
}