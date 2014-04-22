package org.openedit.entermedia.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.openedit.Data;
import org.openedit.data.Searcher;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;


public class HtmlUtil {
	
	public static final int DEFAULT_HTML_LENGTH = 256;
	
	public void trimHtml(WebPageRequest inRequest){
		String maxlength = inRequest.findValue("maxlength");
		int length = DEFAULT_HTML_LENGTH;
		if (maxlength != null && !maxlength.isEmpty()){
			try{
				length = Integer.parseInt(maxlength);
			}catch (Exception e){}//not handled
		}
		String htmlfields = inRequest.findValue("htmlfields");
		if (htmlfields == null){
			return;
		}
		Data item = (Data) inRequest.getPageValue("item");
		if (item == null){
			item = (Data) inRequest.getPageValue("data");
		}
		if (item == null){
			return;
		}
		String [] fields = htmlfields.split(",");
		for(String field:fields){
			if (item.get(field)==null || item.get(field).isEmpty()){
				continue;
			}
			String html = null;
			try{
//				html = getShortenedHTML(item.get(field),length);
				html = truncateHTML(item.get(field),length);
			}catch (Exception e){}
			if (html == null){
				continue;
			}
			inRequest.putPageValue("trimmed", html);
			break;
		}
	}
	
	protected String getShortenedHTML(String inHTML, int inMaxLength) throws Exception{
		StringBuilder content = new StringBuilder();
		Stack<String> stack = new Stack<String>();
		int contentCount = 0;
		int lastStart = -1;
		Pattern pattern = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>");
		String input = inHTML;//.replace("\n", "").trim();
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()){
			String tag = matcher.group();
			String htmlContent = null;
			if (lastStart > 0 && lastStart < matcher.start()){
				String substring = input.substring(lastStart, matcher.start()).trim();
				if (!substring.isEmpty()){
					if ( (contentCount + substring.length()) < inMaxLength){
						contentCount += substring.length();
						htmlContent = substring;
					} else {//figure out best place to break up content
						int delta = (inMaxLength - contentCount);
						String [] tokens = substring.split("\\s");
						StringBuilder buf = new StringBuilder();
						for (String token:tokens){
							buf.append(token);
							if (buf.toString().length() > delta){
								if (token.endsWith(".")){
									buf.append("..");
								} else {
									buf.append("...");
								}
								break;
							} else {
								buf.append(" ");
							}
						}
						contentCount += buf.toString().length();
						htmlContent = buf.toString();
					}
				}
			}
			lastStart = matcher.end();
			if (tag.endsWith("/>")){// solo tag like <br/>
				if (htmlContent!=null){//reassemble in correct order
					content.append(htmlContent);
					content.append(tag);
					if (contentCount > inMaxLength){
						break;
					}
				}
			} else if (tag.startsWith("</")){// end tag
				String endtag = tag.replace("</", "").replace(">", "");
				if (!stack.isEmpty() && endtag.equals(stack.peek())){
					stack.pop();
				}
				if (htmlContent!=null){//reassemble in correct order
					content.append(htmlContent);
					content.append(tag);
					if (contentCount > inMaxLength){
						break;
					}
				}
			} else {//start tag
				String starttag = tag.replace("<", "").replace(">", "");
				stack.push(starttag);
				content.append(tag);//reassemble in correct order
				if (htmlContent!=null){
					content.append(htmlContent);
					if (contentCount > inMaxLength){
						break;
					}
				}
			}
		}
		while(!stack.isEmpty()){
			content.append("</").append(stack.pop()).append(">");
		}
		if (content.toString().isEmpty()){
			content.append(inHTML);//input did not have any tags, return as whole
		}
		return content.toString();
	}
	
	public String truncateHTML(Data inData, String inField, int inLength){
		return truncateHTML(inData.get(inField),inLength);
	}
	
	public String truncateHTML(String text, int length) {
	    // if the plain text is shorter than the maximum length, return the whole text
	  if(text == null || text.length()== 0){
		  return null;
	  }
		if (text.replaceAll("<.*?>", "").length() <= length) {
	        return text;
	    }
	    StringBuilder result = new StringBuilder();
	    boolean trimmed = false;
	    /*
	     * This pattern creates tokens, where each line starts with the tag.
	     * For example, "One, <b>Two</b>, Three" produces the following:
	     *     One,
	     *     <b>Two
	     *     </b>, Three
	     */
	    Pattern tagPattern = Pattern.compile("(<.+?>)?([^<>]*)");

	    /*
	     * Checks for an empty tag, for example img, br, etc.
	     */
	    Pattern emptyTagPattern = Pattern.compile("^<\\s*(img|br|input|hr|area|base|basefont|col|frame|isindex|link|meta|param).*>$");

	    /*
	     * Modified the pattern to also include H1-H6 tags
	     * Checks for closing tags, allowing leading and ending space inside the brackets
	     */
	    Pattern closingTagPattern = Pattern.compile("^<\\s*/\\s*([a-zA-Z]+[1-6]?)\\s*>$");

	    /*
	     * Modified the pattern to also include H1-H6 tags
	     * Checks for opening tags, allowing leading and ending space inside the brackets
	     */
	    Pattern openingTagPattern = Pattern.compile("^<\\s*([a-zA-Z]+[1-6]?).*?>$");

	    /*
	     * Find &nbsp; &gt; ...
	     */
	    Pattern entityPattern = Pattern.compile("(&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};)");

	    // splits all html-tags to scanable lines
	    Matcher tagMatcher =  tagPattern.matcher(text);
	    int numTags = tagMatcher.groupCount();

	    int totalLength = 3;
	    List<String> openTags = new ArrayList<String>();

	    boolean proposingChop = false;
	    while (tagMatcher.find()) {
	        String tagText = tagMatcher.group(1);
	        String plainText = tagMatcher.group(2);

	        if (proposingChop &&
	                tagText != null && tagText.length() != 0 &&
	                plainText != null && plainText.length() != 0) {
	            trimmed = true;
	            break;
	        }

	        // if there is any html-tag in this line, handle it and add it (uncounted) to the output
	        if (tagText != null && tagText.length() > 0) {
	            boolean foundMatch = false;

	            // if it's an "empty element" with or without xhtml-conform closing slash
	            Matcher matcher = emptyTagPattern.matcher(tagText);
	            if (matcher.find()) {
	                foundMatch = true;
	                // do nothing
	            }

	            // closing tag?
	            if (!foundMatch) {
	                matcher = closingTagPattern.matcher(tagText);
	                if (matcher.find()) {
	                    foundMatch = true;
	                    // delete tag from openTags list
	                    String tagName = matcher.group(1);
	                    openTags.remove(tagName.toLowerCase());
	                }
	            }

	            // opening tag?
	            if (!foundMatch) {
	                matcher = openingTagPattern.matcher(tagText);
	                if (matcher.find()) {
	                    // add tag to the beginning of openTags list
	                    String tagName = matcher.group(1);
	                    openTags.add(0, tagName.toLowerCase());
	                }
	            }

	            // add html-tag to result
	            result.append(tagText);
	        }

	        // calculate the length of the plain text part of the line; handle entities (e.g. &nbsp;) as one character
	        int contentLength = plainText.replaceAll("&[0-9a-z]{2,8};|&#[0-9]{1,7};|[0-9a-f]{1,6};", " ").length();
	        if (totalLength + contentLength > length) {
	            // the number of characters which are left
	            int numCharsRemaining = length - totalLength;
	            int entitiesLength = 0;
	            Matcher entityMatcher = entityPattern.matcher(plainText);
	            while (entityMatcher.find()) {
	                String entity = entityMatcher.group(1);
	                if (numCharsRemaining > 0) {
	                    numCharsRemaining--;
	                    entitiesLength += entity.length();
	                } else {
	                    // no more characters left
	                    break;
	                }
	            }

	            // keep us from chopping words in half
	            int proposedChopPosition = numCharsRemaining + entitiesLength;
	            int endOfWordPosition = plainText.indexOf(" ", proposedChopPosition-1);
	            if (endOfWordPosition == -1) {
	                endOfWordPosition = plainText.length();
	            }
	            int endOfWordOffset = endOfWordPosition - proposedChopPosition;
	            if (endOfWordOffset > 6) { // chop the word if it's extra long
	                endOfWordOffset = 0;
	            }

	            proposedChopPosition = numCharsRemaining + entitiesLength + endOfWordOffset;
	            if (plainText.length() >= proposedChopPosition) {
	                result.append(plainText.substring(0, proposedChopPosition));
	                proposingChop = true;
	                if (proposedChopPosition < plainText.length()) {
	                    trimmed = true;
	                    break; // maximum length is reached, so get off the loop
	                }
	            } else {
	                result.append(plainText);
	            }
	        } else {
	        	result.append(plainText);
	            totalLength += contentLength;
	        }
	        // if the maximum length is reached, get off the loop
	        if(totalLength >= length) {
	            trimmed = true;
	            break;
	        }
	    }
	    if (trimmed) {
	    	appendSuffix(result);
	    }
	    for (String openTag : openTags) {
	    	result.append("</" + openTag + ">");
	    }
	    return result.toString();
	}
	
	protected void appendSuffix(StringBuilder buf){
		if (buf.toString().endsWith("...")){
			//no op
		} else if (buf.toString().endsWith("..")){
			buf.append(".");
		} else if (buf.toString().endsWith(".")){
			buf.append("..");
		} else {
			buf.append("...");
		}
	}
	
	public String truncateHTMLtoPlainText(String inHTML, int inMaxLength){
		String html = truncateHTML(inHTML,inMaxLength);
	   if(html != null){
		return html.replaceAll("<.*?>", "");
	   } else{
		   return null;
	   }
	}
	
	public void stripHTML(String inHTML, StringBuilder buf){
		buf.append(inHTML.replaceAll("<.*?>", ""));
	}
	
	public String toHTML(String inText)
	{
		StringBuilder buf = new StringBuilder();
		String [] lines = inText.split("\n");
		boolean isList = false;
		for(String line:lines)
		{
			if (line.startsWith("*") || line.startsWith("•") || line.startsWith("-")) //if bullet then put in list
			{
				isList = true;
				buf.append("<ul><li>").append(line).append("</li>");
			} else {
				if (isList){
					buf.append("</ul>");
				}
				isList = false;
				buf.append("<p>").append(line).append("</p>");
			}
		}
		return buf.toString();
	}
	
	public ArrayList<String> generateKeywords(String inCatalogId, Data inData, ArrayList<String> inFields) throws Exception{
		HashMap<String,String> map = new HashMap<String,String>();
		for(String field:inFields){
			String value = inData.get(field);
			if (value == null || value.isEmpty()){
				continue;
			}
			StringBuilder buf = new StringBuilder();
			stripHTML(value,buf);
			ArrayList<String> keywords = getAllKeywords(inCatalogId, buf.toString());
			for(String keyword:keywords){
				map.put(keyword, keyword);
			}
		}
		Iterator<String> itr = map.keySet().iterator();
		ArrayList<String> keywords = new ArrayList<String>();
		while (itr.hasNext()){
			String key = itr.next();
			keywords.add(key);
		}
		return keywords;
	}
	
	protected ArrayList<String> getAllKeywords(String inCatalogId, String inValue) throws Exception{
		ArrayList<String> keywords = new ArrayList<String>();
		Analyzer analyzer = null;
		TokenStream stream = null;
		try{
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
					/*new FullTextAnalyzer(Version.LUCENE_40);*/
					/*new EnglishAnalyzer(Version.LUCENE_40); */
			stream = analyzer.tokenStream(null, new StringReader(inValue));
		    stream.reset();
		    while (stream.incrementToken()) {
		    	String str = stream.getAttribute(CharTermAttribute.class).toString().trim();
		    	if (str.isEmpty() || str.matches(".*\\d.*") || isStopWord(inCatalogId,str) ){
		    		continue;
		    	}
		        keywords.add(str.trim());
		    }
		} finally {
			try{
				if (stream!=null) stream.close();
			}catch (Exception e){}//not handled
			try{
				if (analyzer!=null) analyzer.close();
			}catch (Exception e){}//not handled
		}
		return keywords;
	}
	
	protected boolean isStopWord(String inCatalogId, String inWord){
		if (inWord.matches(".*\\d.*")){
			return true;
		}
//		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "stopword");
//		SearchQuery query = searcher.createSearchQuery();
//		query.addMatches("name",inWord);
//		HitTracker hits = searcher.search(query);
//		return hits.size() > 0;
		return false;
	}
	
	/*protected Set<String> getStopWords(String inCatalogId) throws Exception{
		Set<String> set = new HashSet<String>();
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "stopword");
		HitTracker hits = searcher.getAllHits();
		Iterator<?> itr = hits.iterator();
		while(itr.hasNext()){
			String name = ((Data) itr.next()).getName();
			set.add(name);
		}
		return set;
	}*/

}
