package org.openedit.data.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

/** An Analyzer that filters With minimal changes
 * 
 *  Be aware that Lucene search will almost always use White Space and Lower Case filters
 *  So you will probably want to use RecordLookUpAnalyser and pass in "Some Term" with quotes to get exact matches
 * */


public class NullAnalyzer extends Analyzer 
{
	protected TokenStreamComponents createComponents(String inFieldName, Reader inReader)
	{
	    Tokenizer source =  new KeywordTokenizer(inReader);
		return new TokenStreamComponents(source);
	}

}


