/*
 * Created on Aug 16, 2005
 */
package org.entermediadb.asset.util;


/*
 * Copyright (c) Ian F. Darwin, http://www.darwinsys.com/, 1996-2002.
 * All rights reserved. Software written by Ian F. Darwin and others.
 * $Id: Parser.java,v 1.1 2010/05/14 21:04:13 cburkey Exp $
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Java, the Duke mascot, and all variants of Sun's Java "steaming coffee
 * cup" logo are trademarks of Sun Microsystems. Sun's, and James Gosling's,
 * pioneering role in inventing and promulgating (and standardizing) the Java 
 * language and environment is gratefully acknowledged.
 * 
 * The pioneering role of Dennis Ritchie and Bjarne Stroustrup, of AT&T, for
 * inventing predecessor languages C and C++ is also gratefully acknowledged.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openedit.util.FileUtils;

/* Simple demo of CSV parser class.
 */
/** Parse comma-separated values (CSV), a common Windows file format.
 * Sample input: "LU",86.25,"11/4/1998","2:19PM",+4.0625
 * <p>
 * Inner logic adapted from a C++ original that was
 * Copyright (C) 1999 Lucent Technologies
 * Excerpted from 'The Practice of Programming'
 * by Brian W. Kernighan and Rob Pike.
 * <p>
 * Included by permission of the http://tpop.awl.com/ web site, 
 * which says:
 * "You may use this code for any purpose, as long as you leave 
 * the copyright notice and book citation attached." I have done so.
 * @author Brian W. Kernighan and Rob Pike (C++ original)
 * @author Ian F. Darwin (translation into Java and removal of I/O)
 * @author Ben Ballard (rewrote advQuoted to handle '""' and for readability)
 */
public class TabParser implements Parser {  

  public static final char DEFAULT_SEP = '\t';
  protected BufferedReader fieldBufferedReader;
  
  public BufferedReader getBufferedReader()
{
	return fieldBufferedReader;
}

public void setBufferedReader(BufferedReader inBufferedReader)
{
	fieldBufferedReader = inBufferedReader;
}

/** Construct a CSV parser, with the default separator (`,'). */
  public TabParser() {
    this(DEFAULT_SEP);
  }

  /** Construct a CSV parser with a given separator. 
   * @param sep The single char for the separator (not a list of
   * separator characters)
   */
  public TabParser(char sep) {
    fieldSep = sep;
    tabs = Pattern.compile(String.valueOf(sep));
  }

  /** The fields in the current String */
  protected List list = new ArrayList();
  protected Pattern tabs;

  /** the separator char for this parser */
  protected char fieldSep;

  
  public String[] parseRegEx(String line)
  {
	  //list.clear();      
	  String[] args = tabs.split(line);
	  //list.addAll(Arrays.asList(args));
	  return args;
  }
  
  /** parse: break the input String into fields
   * @return java.util.Iterator containing each field 
   * from the original as a String, in order.
   */
  public List parse(String line)
  {
    StringBuffer sb = new StringBuffer();
    list.clear();      // recycle to initial state
    int i = 0;

    if (line.length() == 0) {
      list.add(line);
      return list;
    }

    do {
            sb.setLength(0);
            if (i < line.length() && line.charAt(i) == '"')
                i = advQuoted(line, sb, ++i);  // skip quote
            else
                i = advPlain(line, sb, i);
            list.add(sb.toString());
            i++;
    } while (i < line.length());
    if (line.charAt(line.length()-1) == fieldSep)
    	list.add("");
    return list;
  }

  /** advQuoted: quoted field; return index of next separator */
  protected int advQuoted(String s, StringBuffer sb, int i)
  {
    int j;
    int len= s.length();
        for (j=i; j<len; j++) {
            if (s.charAt(j) == '"' && j+1 < len) {
                if (s.charAt(j+1) == '"') {
                    j++; // skip escape char
                } else if (s.charAt(j+1) == fieldSep) { //next delimeter
                    j++; // skip end quotes
                    break;
                }
            } else if (s.charAt(j) == '"' && j+1 == len) { // end quotes at end of line
                break; //done
      }
      sb.append(s.charAt(j));  // regular character.
    }
    return j;
  }

  /** advPlain: unquoted field; return index of next separator */
  protected int advPlain(String s, StringBuffer sb, int i)
  {
    int j;

    j = s.indexOf(fieldSep, i); // look for separator
        if (j == -1) {                 // none found
            sb.append(s.substring(i));
            return s.length();
        } else {
            sb.append(s.substring(i, j));
            return j;
        }
    }

public String[] readNext()
{
	try
	{
		String line = getBufferedReader().readLine();
		if( line == null)
		{
			return null;
		}
		line = line.replaceAll("\u001e"," , ");

		//Only keep valid ASCII text
		StringBuffer escapedSource = new StringBuffer(line.length());
		//String zeros = "000000";
		for ( int n = 0; n < line.length(); n++ )
		{
			char c = line.charAt( n );
			if ( c  > 31 && c < 127  )
			{
				escapedSource.append( c );
			}
			if ( c == '\t')
			{
				escapedSource.append( c );
			}
			else
			{ 
				//skip ISO just 32 - 126
			}
		}
		line = escapedSource.toString();
		
		List items = parse(line);
		
		return (String[])items.toArray(new String[items.size()]);
	}
	catch( IOException ex)
	{
		throw new RuntimeException(ex);
	}
}

	public void close()
	{
		FileUtils.safeClose( getBufferedReader() );
	}

}

