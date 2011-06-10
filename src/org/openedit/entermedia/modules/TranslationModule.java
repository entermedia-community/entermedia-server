package org.openedit.entermedia.modules;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;
import com.openedit.modules.translations.Language;
import com.openedit.modules.translations.Translation;
import com.openedit.modules.translations.TranslationEventListener;
import com.openedit.modules.translations.TranslationParser;
import com.openedit.modules.translations.TranslationSearcher;
import com.openedit.page.FileFinder;
import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.page.PageSettings;
import com.openedit.util.PathUtilities;
import com.openedit.util.URLUtilities;
import com.openedit.web.Browser;

public class TranslationModule extends BaseModule
{
	public void listFilesInBase(WebPageRequest inReq)
	{
		//get a list
		String path = "/WEB-INF/base/entermedia/";
		String lang = inReq.getRequestParameter("lang");
		List translations = gatherTranslations(path, lang);
		StringBuffer out = new StringBuffer();
		for(Iterator iterator = translations.iterator(); iterator.hasNext();)
		{
			path = (String)iterator.next();
			Page page = getPageManager().getPage(path);
			out.append(path);
			out.append("\n");
			out.append(page.getContent());
			out.append("\n===\n");
		}
		inReq.putPageValue("translations",out.toString());
	}
	

	public List gatherTranslations(String inPath, String inlang)
	{
		List translations = new ArrayList();
		List children = getPageManager().getChildrenPaths(inPath);
		for(Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String path = (String)iterator.next();
			if( path.contains("/.versions") )
			{
				continue;
			}
			if(path.endsWith("_text_" + inlang + ".txt"))
			{
				translations.add(path);
				continue;
			}
			//System.out.println("Trying to get page for path: " + path);
			Page page = getPageManager().getPage(path);
			if(page.isFolder())
			{
				translations.addAll(gatherTranslations(path, inlang));
			}
		}	
		return translations;	
	}

	
	public void saveFiles(WebPageRequest inReq) throws Exception
	{
		//take the text and save it
		String text = inReq.getRequestParameter("translations");
		if( text != null)
		{
			boolean addnew = Boolean.parseBoolean( inReq.findValue("mergenewcontent") );
			
			BufferedReader read = new BufferedReader(new StringReader(text));
			String line = null;
			String filepath = read.readLine();
			StringBuffer textout = new StringBuffer();
			int c = 0;
			while( (line = read.readLine() ) != null)
			{
				if( line.equals("==="))
				{
					//save off the last stuff and get ready for next file
					Page page = getPageManager().getPage(filepath);
					//dont save to folders that do not exists
					if( !getPageManager().getPage(page.getParentPath()).exists() )
					{
						continue;
					}
					String existing = "";
					if(page.exists()){
						existing = page.getContent() + "\n";
					}
					String newcontent = textout.toString();
					if( !newcontent.equals(existing)) 
					{
						if( addnew)
						{
							//merge together old and new
							Properties existingprops = new Properties();
							existingprops.load(new StringReader(existing));
							existingprops.load(new StringReader(newcontent));
							StringWriter out = new StringWriter();
							existingprops.store(out, null);
							newcontent = out.toString();
						}						
						getPageManager().saveContent(page, inReq.getUser(), newcontent, null);
						c++;
					}
					textout.setLength(0);
					line = read.readLine();
					if( line == null)
					{
						break;
					}
					filepath = line;
				}
				else
				{
					textout.append(line);
					textout.append('\n');
				}
			}
			inReq.putPageValue("count", String.valueOf(c));
		}
	}
	public static final String PROPERTIES_FROM_MARKUP = "properties_from_markup";

	public Translation getTranslations( WebPageRequest inReq ) throws OpenEditException
	{
		Translation trans = new Translation();

		//get the languages
		init( inReq, trans );

		inReq.putPageValue( "pageManager", getPageManager() );
		inReq.putPageValue( "translations", trans );
		return trans;
	}

	protected void init( WebPageRequest inReq, Translation inTrans ) throws OpenEditException
	{
		//#set( $languages = $page.getPageSettings().getProperty("languages") )
		PageProperty prop = inReq.getPage().getPageSettings().getProperty( "languages" );

		if ( prop != null )
		{
			for ( Iterator iter = prop.getValues().keySet().iterator(); iter.hasNext(); )
			{
				String locale = (String) iter.next();
				String name = (String) prop.getValues().get( locale );
				Language lang = new Language();
				lang.setPageManager( getPageManager() );
				if ( locale.length() == 0 )
				{
					lang.setId( "default" );
					lang.setRootDirectory( "" );
				}
				else
				{
					lang.setId( locale );
					lang.setRootDirectory( "/translations/" + locale );
				}
				lang.setName( name );
				inTrans.addLanguage( lang );
			}
			inTrans.sort();
			Language browser = createBrowserLang( inReq );
			inTrans.getLanguages().add( 0, browser );
		}
		//This is for transition for people who do not have languages setup yet or upgrades
		if ( inTrans.getLanguages().size() == 0 )
		{
			Language browser = createBrowserLang( inReq );
			inTrans.getLanguages().add( browser );
			Language lang = new Language();
			lang.setPageManager( getPageManager() );
			lang.setId( "default" );
			lang.setName( "Language: Use Default" );
			lang.setRootDirectory( "" );
			inTrans.addLanguage( lang );
			//TODO: remove this section
			String done = (String) inReq.getSessionValue( "defaultset" );
			if ( done == null )
			{
				inReq.putSessionValue( "sessionlocale", "default" );
				inReq.putSessionValue( "defaultset", "true" );
			}
		}
		String selectedLang = inReq.getLanguage();
		inTrans.setSelectedLang( selectedLang );
	}

	protected Language createBrowserLang( WebPageRequest inReq )
	{
		Language lang = new Language();
		lang.setPageManager( getPageManager() );
		lang.setId( "browser" );
		Browser browser = (Browser) inReq.getPageValue( "browser" );
		if ( browser != null )
		{
			lang.setName( "Language: " + browser.getLocale() );
		}
		lang.setRootDirectory( "" );
		return lang;
	}

	public void changeLanguage( WebPageRequest inReq ) throws Exception
	{
		String newlang = inReq.getRequestParameter( "newlang" );
		getTranslations( inReq );
		if ( newlang != null )
		{
			if ( newlang.equals( "locale_browser" ) )
			{
				inReq.removeSessionValue( "sessionlocale" );
			}
			else
			{
				String locale = newlang.substring( "locale_".length() );
				inReq.putSessionValue( "sessionlocale", locale );
			}
			String orig = inReq.getRequestParameter( "origURL" );
			if ( orig != null )
			{
				inReq.redirect( orig );
			}
		}
	}

	//for editing
	protected Language getEditingLanguage( WebPageRequest inReq )
	{
		String id = (String) inReq.getSessionValue( "editinglanguage" );

		Translation trans = (Translation) inReq.getPageValue( "translations" );

		return trans.getLanguage( id );
	}

	public void selectElement( WebPageRequest inReq ) throws Exception
	{
		String eid = inReq.getRequestParameter( "elementid" );
		if ( eid != null )
		{
			Translation trans = (Translation) inReq.getPageValue( "translations" );
			Language lang = trans.getLanguage( eid );
			inReq.putSessionValue( "editinglanguage", lang.getId() );
		}
	}


	

	

	public void loadTranslations(WebPageRequest inReq){
		String translationid = inReq.findValue("translationsid");
		TranslationSearcher searcher = (TranslationSearcher) getSearcherManager().getSearcher(translationid, "translation");
		inReq.putPageValue("translations", searcher);
		String locale = inReq.getLocale();
		if(locale != null){
			if(locale.contains("_")){
		
				locale = locale.substring(0, locale.indexOf("_"));
		
		}
		inReq.putPageValue("lang", locale);
		}
		
	}

	

	
}
