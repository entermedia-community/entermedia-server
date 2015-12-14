/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.scripts;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import org.openedit.config.Configuration;
import org.openedit.page.Page;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;


/**
 * This class represents a script.  It may be either a standalone JavaScript file or part of a
 * form.
 *
 * @author Eric Galluzzo
 */
public class Script
{
	protected String fieldDescription;
	protected String fieldScriptText;
	protected int fieldStartCharNumber;
	protected int fieldStartLineNumber;
	protected Page fieldPage;
	protected String fieldType;
	protected String fieldMethod;
	protected Configuration fieldConfiguration;
	protected String fieldCatalogId;
	
	
	public void setConfiguration(Configuration inConfiguration) {
		fieldConfiguration = inConfiguration;
	}

	public Configuration getConfiguration() {
		return fieldConfiguration;
	}

	public String getMethod()
	{
		return fieldMethod;
	}

	public void setMethod(String inMethod)
	{
		fieldMethod = inMethod;
	}

	public String getType()
	{
		return fieldType;
	}

	public void setType(String inType)
	{
		fieldType = inType;
	}

	public Page getPage()
	{
		return fieldPage;
	}

	public void setPage(Page inPage)
	{
		fieldPage = inPage;
	}

	/**
	 * Create a new script, with parameters to be filled in later.
	 */
	public Script()
	{
	}



	/**
	 * Create a new script with the given script text, description, and starting position within
	 * its file.
	 *
	 * @param inScriptText DOCUMENT ME!
	 * @param inDescription DOCUMENT ME!
	 * @param inStartLineNumber DOCUMENT ME!
	 * @param inStartCharNumber DOCUMENT ME!
	 */
	public Script(
		String inScriptText, String inDescription, int inStartLineNumber, int inStartCharNumber)
	{
		fieldScriptText = inScriptText;
		fieldDescription = inDescription;
		fieldStartLineNumber = inStartLineNumber;
		fieldStartCharNumber = inStartCharNumber;
	}

	/**
	 * Sets the description of this script, usually containing the file from which it came.
	 *
	 * @param description The description to set
	 */
	public void setDescription(String description)
	{
		fieldDescription = description;
	}

	/**
	 * Returns the description of this script, usually containing the file from which it came.
	 *
	 * @return String
	 */
	public String getDescription()
	{
		return fieldDescription;
	}

	/**
	 * Sets the script itself.
	 *
	 * @param scriptText The script to set
	 */
	public void setScriptText(String scriptText)
	{
		fieldScriptText = scriptText;
	}

	/**
	 * Returns the script itself.
	 *
	 * @return String
	 */
	public String getScriptText()
	{
		if( fieldScriptText == null)
		{
			Reader in = getPage().getReader();
			StringWriter out = new StringWriter();
			
			try
			{
				new OutputFiller().fill(in,out);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			FileUtils.safeClose(in);
			FileUtils.safeClose(out);
			fieldScriptText = out.toString();
		}
		return fieldScriptText;
	}

	/**
	 * Sets the character number within the file named in the description at which the script
	 * started.
	 *
	 * @param startCharNumber The starting character number
	 */
	public void setStartCharNumber(int startCharNumber)
	{
		fieldStartCharNumber = startCharNumber;
	}

	/**
	 * Returns the character number within the file named in the description at which the script
	 * started.
	 *
	 * @return int
	 */
	public int getStartCharNumber()
	{
		return fieldStartCharNumber;
	}

	/**
	 * Sets the line number within the file named in the description at which the script started.
	 *
	 * @param startLineNumber The starting line number
	 */
	public void setStartLineNumber(int startLineNumber)
	{
		fieldStartLineNumber = startLineNumber;
	}

	/**
	 * Returns the line number within the file named in the description at which the script
	 * started.
	 *
	 * @return int
	 */
	public int getStartLineNumber()
	{
		return fieldStartLineNumber;
	}

	
}
