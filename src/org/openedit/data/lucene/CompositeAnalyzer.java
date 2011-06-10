package org.openedit.data.lucene;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

/** An Analyzer that filters LetterTokenizer with LowerCaseFilter. */


public class CompositeAnalyzer extends Analyzer 
{
	//some fields use StandardAnaliser
	protected Analyzer fieldDefaultAnalyzer = new RecordLookUpAnalyzer();
	protected Map fieldChoices = new HashMap();
	 public TokenStream tokenStream(String fieldName, Reader reader)
	 {
		Analyzer ana = getAnalizer(fieldName);
		return ana.tokenStream(fieldName, reader);
	 }
	 public Analyzer getAnalizer(String inField)
	 {
		 if( inField == null)
		 {
			 return fieldDefaultAnalyzer;
		 }
		 Analyzer res = (Analyzer)getChoices().get(inField);
		 if ( res == null)
		 {
			 return fieldDefaultAnalyzer;
		 }
		 return res;
	 }
	protected Map getChoices()
	{
		return fieldChoices;
	}
	protected void setChoices(Map inChoices)
	{
		fieldChoices = inChoices;
	}
	public void setAnalyzer(String inField, Analyzer inAnalyzer)
	{
		getChoices().put(inField, inAnalyzer);
	}
	
	public void setDefaultAnalyzer(Analyzer inAnalyzer)
	{
		fieldDefaultAnalyzer = inAnalyzer;
	}

}

