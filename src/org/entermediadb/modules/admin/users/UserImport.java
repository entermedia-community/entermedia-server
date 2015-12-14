/*
 * Created on Jan 9, 2006
 */
package org.entermediadb.modules.admin.users;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.openedit.OpenEditException;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.FileUtils;

public class UserImport
{
	protected UserManager fieldUserManager;
	
	//we are passed in a text file name and return 
	
	public List listUsers(File inFile) throws OpenEditException
	{
		try
		{
			//check to see if its XML
			BufferedReader reader = null;
			String text;
			try
			{
				reader = new BufferedReader(new FileReader( inFile ) );
				text = reader.readLine();
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
			if( text == null)
			{
				throw new OpenEditException("Empty input file");
			}
			if ( text.startsWith("<?xml"))
			{
				//parse with XML and return
				return parseXml( inFile );
			}
			else
			{
				return parseTabs( inFile );
			}
		} 
		catch ( IOException ex)
		{
			throw new OpenEditException( ex);
		}
	}
	protected String cleanId( String inId)
	{
		//remove spaces
		//replace &
		if ( inId != null)
		{
			inId = inId.replaceAll(" ","");
			inId = inId.replaceAll("&","");
			inId = inId.replaceAll("/","");
			inId = inId.replaceAll("\\\\","");

		}
		
		return inId;
	}

	protected List parseTabs(File inText) throws OpenEditException
	{
		
		List accounts = new ArrayList();
		//take off the headers
		Header header = new Header();

		try
		{
			header.loadColumns(inText);

			Row row = header.getNextRow();
			while( row != null )
			{
				String id = row.getData("id");
				if ( id == null)
				{
					id = cleanId( row.getData("email") );
				}
				User user = null;
				//this uses ID if it finds it otherwise it uses email address
				if ( id != null)
				{
					user = getUserManager().getUser(id);
					if( user == null)
					{
						String password = row.getData("password");
						user = getUserManager().createUser(id,password);
					}
				}
				user.setEmail(	row.getData("email") );
				user.setFirstName(row.getData("firstname") );
				user.setLastName(row.getData("lastname") );
				for (int i = 0; i < header.getSize(); i++)
				{
					String prop = (String) header.getColumn(i);
					if ( prop.startsWith("property."))
					{
						String name = prop.substring("property.".length());
						user.put(name,row.getData(i));
					}
				}
				accounts.add( user );
				getUserManager().saveUser(user);

				row = header.getNextRow();
			}
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		return accounts;
	}

	protected List parseXml(File inText) throws OpenEditException
	{
		List accounts = new ArrayList();
		try
		{
			Document doc = new SAXReader().read(new FileReader( inText ) );
			for (Iterator iter = doc.getRootElement().elementIterator("account"); iter.hasNext();)
			{
				Element	element = (Element) iter.next();
				String id = element.attributeValue("id");
				String password = element.attributeValue("password");
				
				User user = null;
				//this uses ID if it finds it otherwise it uses email address
				if ( id != null)
				{
					user = getUserManager().getUser(id);
					if( user == null)
					{
						user = getUserManager().createUser(id,password);
					}
				}
				user.setEmail(	element.attributeValue("email") );
				user.setFirstName(element.attributeValue("firstname") );
				user.setLastName(element.attributeValue("lastname") );
				for (Iterator iterator = element.elementIterator("property"); iterator.hasNext();)
				{
					Element prop = (Element) iterator.next();
					user.put(prop.attributeValue("id"),prop.getTextTrim());
				}
				getUserManager().saveUser(user);

				accounts.add( user );
			}
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		return accounts;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	
}
