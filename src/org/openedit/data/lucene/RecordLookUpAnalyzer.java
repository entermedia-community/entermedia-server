/*
 * Created on Apr 13, 2006
 */
package org.openedit.data.lucene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.util.Version;

import com.openedit.OpenEditException;

/**
 * ID's must be separated by spaces and will become case insenstive
 * You can use any chars in the ID
 * Will produce exact matches
 * 
 * @author cburkey
 *
 */
public class RecordLookUpAnalyzer extends Analyzer
{
	protected boolean fieldUseTokens = true;
	public RecordLookUpAnalyzer()
	{
		
	}
	public RecordLookUpAnalyzer( boolean inUseTokens )
	{
		setUseTokens(inUseTokens);
	}
	
	@Override
	protected TokenStreamComponents createComponents(String inFieldName, Reader inReader)
	{
		
	    Tokenizer source =  null;
	    TokenStream filter = null;
		if( isUseTokens())
		{
			source = new WhitespaceTokenizer(Version.LUCENE_31, inReader);
		}
		else
		{
			source = new OneToken(inReader);
		}
	    filter = new LowerCaseFilter(Version.LUCENE_31, source);
		return new TokenStreamComponents(source,filter);

	}
	/*
	public TokenStream tokenStream(String fieldName, Reader reader) 
	{
		if( isUseTokens())
		{
			TokenStream result = new WhitespaceTokenizer(Version.LUCENE_31, reader);
		    result = new LowerCaseFilter(Version.LUCENE_31, result);
	
			//result = new NullFilter(result); //for debug
		    return result;
		}
		else
		{
			return new LowerCaseFilter(Version.LUCENE_31, new OneToken(reader));
		}
	}
	*/

	public boolean isUseTokens()
	{
		return fieldUseTokens;
	}

	public void setUseTokens(boolean inTokens)
	{
		fieldUseTokens = inTokens;
	}

	class OneToken extends Tokenizer
	{
		Token fieldToken;
		public OneToken(Reader inStream)
		{
			super(inStream);
			String token;
			try
			{
				token = new BufferedReader(inStream).readLine();
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
