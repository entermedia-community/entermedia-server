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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

			// collect title
			PDDocumentInformation info = pdf.getDocumentInformation();
			String title = info.getTitle();
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
				// collect text
				int pages = pdf.getNumberOfPages();
				JSONArray savedpages = new JSONArray();
				
				PDFTextStripper stripper = new PDFTextStripper();
				
				//TODO: Write this out to a temp file that will be indexed seperately
				for (int i = 1; i <= pages; i++)
				{
					String text = null;
	
					stripper.setStartPage(i);
					stripper.setEndPage(i);
					
					try
					{
						text = stripper.getText(pdf);
					}
					catch(Throwable e)
					{
						e.printStackTrace();
						log.error("Could not parse" , e);
						text = "";
					}
					text = scrubChars(text);
					savedpages.add(text);
				}
				results.setText(savedpages.toJSONString());
				results.setPages(pdf.getNumberOfPages());
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
					done.append(' '); //these are safe
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
		String finalText = done.toString().trim();
		finalText = finalText.replaceAll("\\s+", " "); 
		finalText = decodeUnicodeEscapes(finalText);
		return finalText;
	}
	public static String decodeUnicodeEscapes(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == '\\' && i + 5 < str.length() && str.charAt(i+1) == 'u') {
                String hex = str.substring(i + 2, i + 6);
                sb.append((char) Integer.parseInt(hex, 16));
                i += 6;
            } else {
                sb.append(str.charAt(i));
                i++;
            }
        }
        return sb.toString();
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