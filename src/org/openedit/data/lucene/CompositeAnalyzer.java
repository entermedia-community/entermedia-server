package org.openedit.data.lucene;

import org.apache.lucene.analysis.Analyzer;

/** An Analyzer that filters LetterTokenizer with LowerCaseFilter. */

/**
 * @deprecated
 * Replaced with PerFieldAnalyzerWrapper
 * PerFieldAnalyzerWrapper composite = new PerFieldAnalyzerWrapper( new RecordLookUpAnalyzer() , analyzermap);
 * @author cburkey
 *
 */
public abstract class CompositeAnalyzer extends Analyzer 
{
	//some fields use StandardAnaliser
//	protected Analyzer fieldDefaultAnalyzer = new RecordLookUpAnalyzer();
//	protected Map fieldChoices = new HashMap();
//	 public TokenStream tokenStream(String fieldName, Reader reader)
//	 {
//		Analyzer ana = getAnalizer(fieldName);
//		return ana.tokenStream(fieldName, reader);
//	 }
//	 public Analyzer getAnalizer(String inField)
//	 {
//		 if( inField == null)
//		 {
//			 return fieldDefaultAnalyzer;
//		 }
//		 Analyzer res = (Analyzer)getChoices().get(inField);
//		 if ( res == null)
//		 {
//			 return fieldDefaultAnalyzer;
//		 }
//		 return res;
//	 }
//	protected Map getChoices()
//	{
//		return fieldChoices;
//	}
//	protected void setChoices(Map inChoices)
//	{
//		fieldChoices = inChoices;
//	}
//	public void setAnalyzer(String inField, Analyzer inAnalyzer)
//	{
//		getChoices().put(inField, inAnalyzer);
//	}
//	
//	public void setDefaultAnalyzer(Analyzer inAnalyzer)
//	{
//		fieldDefaultAnalyzer = inAnalyzer;
//	}
//
}

