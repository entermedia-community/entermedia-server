package org.entermediadb.markdown;

import org.entermediadb.markdown.node.*;
import org.entermediadb.markdown.parser.Parser;
import org.entermediadb.markdown.renderer.html.HtmlRenderer;

public class MarkdownUtil 
{

	public String render(String markdown) 
	{
		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
	}
}
