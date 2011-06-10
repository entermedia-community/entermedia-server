package org.openedit.data.lucene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

import com.openedit.OpenEditException;

/** An Analyzer that filters With minimal changes
 * 
 *  Be aware that Lucene search will almost always use White Space and Lower Case filters
 *  So you will probably want to use RecordLookUpAnalyser and pass in "Some Term" with quotes to get exact matches
 * */


public class NullAnalyzer extends Analyzer 
{
	public TokenStream tokenStream(String inFieldName, Reader inReader)
	{
		return new OneToken(inReader);
	}

	class OneToken extends TokenStream
	{
		Token fieldToken;
		public OneToken(Reader inStream)
		{
			String token;
			try
			{
				token = new BufferedReader(inStream).readLine();
				if(token == null){
					return;
				}
				fieldToken = new Token(token,0,token.length());

			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				throw new OpenEditException(e);
			}
		}

		public boolean incrementToken() throws IOException
		{
			if( fieldToken != null)
			{
				Token toreturn = fieldToken;
				fieldToken = null;
				return true;
			}
			return false;
		}
	}

}


