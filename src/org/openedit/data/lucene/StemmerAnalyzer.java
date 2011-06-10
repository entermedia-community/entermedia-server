package org.openedit.data.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Version;

public class StemmerAnalyzer extends Analyzer
{
	 public final TokenStream tokenStream(String fieldName, Reader reader) 
	 {
		 return new PorterStemFilter(new LowerCaseTokenizer(Version.LUCENE_30,reader));
	 }

}
