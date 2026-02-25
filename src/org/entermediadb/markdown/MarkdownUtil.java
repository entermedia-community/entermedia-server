package org.entermediadb.markdown;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.entermediadb.markdown.node.FencedCodeBlock;
import org.entermediadb.markdown.node.Heading;
import org.entermediadb.markdown.node.HtmlBlock;
import org.entermediadb.markdown.node.ListBlock;
import org.entermediadb.markdown.node.Node;
import org.entermediadb.markdown.node.Nodes;
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
		String rendered = renderPlainHtml(markdown);
		// Remove any HTML elements
		rendered = rendered.replaceAll("<[^>]+>", "");
		return rendered;
	}
	public String renderPlainHtml(String markdown) 
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
	
	public Collection<Map<String, String>> getHtmlMaps(String markdown)
	{
		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdown);
		
		Collection<Map<String, String>> maps = new ArrayList<Map<String, String>>();
		
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		
		for (Iterator<Node> iterator = Nodes.between(document.getFirstChild(), null).iterator(); iterator.hasNext();) {
			Node node = (Node) iterator.next();
			Map<String, String> map = new HashMap<String, String>();
			map.put("type", node.getClass().getSimpleName());
			
			String html = renderer.render(node);
			map.put("content", html);
			
			maps.add(map);
			
		}
		
		return maps;
	}
	
	
}
