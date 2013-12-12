package org.openedit.entermedia.util;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openedit.Data;
import com.openedit.WebPageRequest;


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
				html = getShortenedHTML(item.get(field),length);
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
		String input = inHTML.replace("\n", "").replace("&nbsp;", "").trim();
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

}
