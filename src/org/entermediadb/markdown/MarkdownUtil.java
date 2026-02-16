package org.entermediadb.markdown;

import java.util.Set;

import org.entermediadb.markdown.node.FencedCodeBlock;
import org.entermediadb.markdown.node.Heading;
import org.entermediadb.markdown.node.HtmlBlock;
import org.entermediadb.markdown.node.ListBlock;
import org.entermediadb.markdown.node.Node;
import org.entermediadb.markdown.node.ThematicBreak;
import org.entermediadb.markdown.parser.Parser;
import org.entermediadb.markdown.renderer.html.HtmlRenderer;

public class MarkdownUtil 
{

	public String render(String markdown) 
	{
		Parser parser = Parser.builder()
			.enabledBlockTypes(
				Set.of(
					Heading.class,
					HtmlBlock.class,
					ThematicBreak.class,
					FencedCodeBlock.class,
					ListBlock.class
				)
			).build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().softbreak("<br>").build();
		return renderer.render(document);
	}
	
	public String renderPlain(String markdown) 
	{
		Parser parser = Parser.builder()
			.enabledBlockTypes(
				Set.of(
					FencedCodeBlock.class,
					ListBlock.class
				)
			).build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
	}
}
