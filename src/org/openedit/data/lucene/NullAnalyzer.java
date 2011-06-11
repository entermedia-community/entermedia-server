package org.openedit.data.lucene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
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
		return new KeywordTokenizer(inReader);
	}
}


