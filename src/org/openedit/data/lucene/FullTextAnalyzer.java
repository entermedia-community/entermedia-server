package org.openedit.data.lucene;

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

public class FullTextAnalyzer extends StopwordAnalyzerBase
{
	Version matchVersion = LuceneIndexer.INDEX_VERSION;

	//Stemming causes * * not to work on searches. It also does not split on -
	
	public final TokenStream XXXtokenStream(String fieldName, Reader reader)
	{
		// return new PorterStemFilter(new LowerCaseTokenizer(Version.LUCENE_36,reader));


//		TokenStream source = new WhitespaceTokenizer(matchVersion, reader);
//		source = new LowerCaseFilter(matchVersion, source);

		//		    return source;

		//These dont work with -
		final Tokenizer source = new StandardTokenizer(matchVersion, reader);
		//final  LowerCaseTokenizer source = new LowerCaseTokenizer(matchVersion, reader);
		TokenStream result = new StandardFilter(matchVersion, source);
		result = new EnglishPossessiveFilter(matchVersion, result);
		result = new LowerCaseFilter(matchVersion, result);
		//result = new PorterStemFilter(result);

		//New TokenWordSpliter(source)
		/*
		 * TokenStream result = new StandardFilter(matchVersion, source); //
		 * prior to this we get the classic behavior, standardfilter does it for
		 * us. if (matchVersion.onOrAfter(Version.LUCENE_31)) result = new
		 * EnglishPossessiveFilter(matchVersion, result); result = new
		 * LowerCaseFilter(matchVersion, result); //result = new
		 * StopFilter(matchVersion, result, stopwords); //result = new
		 * PorterStemFilter(result);
		 */
		return result;
	}

	private final Set<?> stemExclusionSet;

	/**
	 * Returns an unmodifiable instance of the default stop words set.
	 * 
	 * @return default stop words set.
	 */
	public static Set<?> getDefaultStopSet()
	{
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	/**
	 * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer
	 * class accesses the static final set the first time.;
	 */
	private static class DefaultSetHolder
	{
		static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
	}

	/**
	 * Builds an analyzer with the default stop words:
	 * {@link #getDefaultStopSet}.
	 */
	public FullTextAnalyzer(Version matchVersion)
	{
		this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
	}

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion
	 *            lucene compatibility version
	 * @param stopwords
	 *            a stopword set
	 */
	public FullTextAnalyzer(Version matchVersion, CharArraySet stopwords)
	{
		this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
	}

	/**
	 * Builds an analyzer with the given stop words. If a non-empty stem
	 * exclusion set is provided this analyzer will add a
	 * {@link KeywordMarkerFilter} before stemming.
	 * 
	 * @param matchVersion
	 *            lucene compatibility version
	 * @param stopwords
	 *            a stopword set
	 * @param stemExclusionSet
	 *            a set of terms not to be stemmed
	 */
	public FullTextAnalyzer(Version matchVersion, CharArraySet stopwords, Set<?> stemExclusionSet)
	{
		super(matchVersion, stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stemExclusionSet));
	}

	/**
	 * Creates a
	 * {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
	 * which tokenizes all the text in the provided {@link Reader}.
	 * 
	 * @return A
	 *         {@link org.apache.lucene.analysis.ReusableAnalyzerBase.TokenStreamComponents}
	 *         built from an {@link StandardTokenizer} filtered with
	 *         {@link StandardFilter}, {@link LowerCaseFilter},
	 *         {@link StopFilter} , {@link KeywordMarkerFilter} if a stem
	 *         exclusion set is provided and {@link PorterStemFilter}.
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader)
	{
		//these mess with words like abc-efg, do not use 
//		final Tokenizer source = new StandardTokenizer(matchVersion, reader);
//		TokenStream result = new StandardFilter(matchVersion, source);
		
		Tokenizer source = new WhitespaceTokenizer(matchVersion, reader);
		TokenStream result = new LowerCaseFilter(matchVersion, source);
		
		// prior to this we get the classic behavior, standardfilter does it for us.
		result = new EnglishPossessiveFilter(matchVersion, result);
//		result = new LowerCaseFilter(matchVersion, result);
		result = new StopFilter(matchVersion, result, stopwords);
//		if (!stemExclusionSet.isEmpty())
//			result = new KeywordMarkerFilter(result, stemExclusionSet);

		/**
		 * We can't stem because we are using a leading wildcard search, Analyzing Wildcard Query Parser
		 * does not deal with leading wildcards correctly. So the stemmer does not kick in when searching
		 * for *swimming*
		 */
		
		//result = new PorterStemFilter(result);  
		return new TokenStreamComponents(source, result);
	}
}
