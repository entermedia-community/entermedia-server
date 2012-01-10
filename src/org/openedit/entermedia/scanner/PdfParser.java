package org.openedit.entermedia.scanner;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pdfbox.encryption.DocumentEncryption;
import org.pdfbox.exceptions.CryptographyException;
import org.pdfbox.exceptions.InvalidPasswordException;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.pdmodel.common.PDRectangle;
import org.pdfbox.util.PDFTextStripper;

import com.openedit.OpenEditException;

public class PdfParser
{
	private static final Log log = LogFactory.getLog(PdfParser.class);

	public Parse parse(InputStream inContent)
	{
		Parse results = new Parse();
		PDDocument pdf = null;
		try
		{

			PDFParser parser = new PDFParser(inContent);
//					new ByteArrayInputStream(inContent));
			parser.parse();

			pdf = parser.getPDDocument();

			if (pdf.isEncrypted())
			{
				DocumentEncryption decryptor = new DocumentEncryption(pdf);
				// Just try using the default password and move on
				decryptor.decryptDocument("");
			}

			// collect text
			PDFTextStripper stripper = new PDFTextStripper();
			String text = null;
			String title = null;
			
			try{
				text = stripper.getText(pdf);
			}
			catch(Throwable e)
			{
				log.error(e);
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
				PDPage page = (PDPage)pdf.getDocumentCatalog().getAllPages().get(0);
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
		} catch (CryptographyException e)
		{
			log.error("Error decrypting document. " + e);
		} catch (InvalidPasswordException e)
		{
			log.error("Can't decrypt document - invalid password. " + e);
		} catch (Exception e)
		{ // run time exception
			log.error("Can't be handled as pdf document. " + e);
		} finally
		{
			try
			{
				if (pdf != null)
					pdf.close();
			} catch (IOException e)
			{
				// nothing to do
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