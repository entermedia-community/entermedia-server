/*
 * Created on Feb 8, 2006
 */
package org.entermediadb.asset.generators;

import java.io.IOException;
import java.io.Writer;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;

public class WikiGenerator extends BaseGenerator
{
	PageManager fieldPageManager;
	
	
	public void generate(WebPageRequest inContext, Page inPage, Output inOut) throws OpenEditException
	{
		String text = inPage.getContent();
		String directory = PathUtilities.extractDirectoryPath(inPage.getPath() );
		String converted = parseWiki( text , directory );
		Writer inOutput = inOut.getWriter();
		try
		{
			inOutput.write(converted);
		} catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public String parseWiki(String text, String directory) throws OpenEditException
	{
		//first check for []
		StringBuffer out = new StringBuffer(text.length() + 100);
		StringBuffer link = new StringBuffer();
		boolean inlink = false;
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if ( c == '[')
			{
				inlink = true;
				out.append("<a href=\"");				
			} 
			else if( c == ']' )
			{
				inlink = false;
				String path = makelink(link.toString() );
				out.append(path);
				out.append("\">");
				out.append(link);
				if( !path.startsWith("http"))
				{
					Page page = getPageManager().getPage(directory + "/" + path);
					if ( !page.exists() )
					{
						out.append("*");
					}
				}
				out.append("</a>");
				link = new StringBuffer();
			}	
			else if( inlink )
			{
				link.append(c);
			}
			else
			{
				out.append(c);
			}
		}
		return out.toString();
	}

	private String makelink(String inString)
	{
		String lower = inString.toLowerCase();
		{
			if ( lower.startsWith("http") || lower.startsWith("mailto"))
			return inString;
		}
		String shortname = inString.replaceAll(" ","_");
		return shortname + ".html";
	}

	protected void markLinks(String[] words, Writer inOutput) throws IOException
	{
		for (int i = 0; i < words.length; i++)
		{
			String word = words[i];
			
			//break this into more parts based on html and other tags?
			StringBuffer part = new StringBuffer();
			for (int j = 0; j < word.length(); j++)
			{
				//build up a word
				if ( Character.isLetter( word.charAt(j) ) )
				{
					part.append(word.charAt(j));
				}
				else
				{
					if ( part.length() > 0 )
					{
						dumpWord(inOutput, part.toString());					
						part = new StringBuffer();
					}
					inOutput.append(word.charAt(j));
				}					
			}
			if ( part.length() > 0 )
			{
				dumpWord(inOutput, part.toString());					
			}

			inOutput.write(' ');
		}
	}

	private void dumpWord(Writer inOutput, String part) throws IOException
	{
		if (isWiki(part))
		{
			
			inOutput.write("<a href='");
			
			//trip some junk off the word

			if ( part.indexOf("OpenEdit") > -1 ) //TODO: Read in ignore list
			{
				inOutput.write("http://www.openedit.org/'>");
			}
			else
			{
				inOutput.write(part);
				inOutput.write(".html'>");
			}
			inOutput.write(part);
			inOutput.write("</a>");

		}
		else
		{
			inOutput.write(part);
		}
	}

	/**
	 * @param inWord
	 * @return
	 */
	protected boolean isWiki(String inWord)
	{
		
		if (inWord != null && inWord.length() > 4)
		{
			int start = 0;
			int type = Character.getType(inWord.charAt(start));
			int secondtype = Character.getType(inWord.charAt(start+1));

			if (type == Character.UPPERCASE_LETTER && secondtype == Character.LOWERCASE_LETTER)
			{
				//make sure there is another upper case in here
				for (int i = start + 2; i < inWord.length(); i++)
				{
					int ctype = Character.getType(inWord.charAt(i));
					if (ctype == Character.UPPERCASE_LETTER)
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

}
