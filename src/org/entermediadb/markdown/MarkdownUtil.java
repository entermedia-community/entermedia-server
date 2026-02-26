package org.entermediadb.markdown;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.markdown.node.FencedCodeBlock;
import org.entermediadb.markdown.node.Heading;
import org.entermediadb.markdown.node.HtmlBlock;
import org.entermediadb.markdown.node.ListBlock;
import org.entermediadb.markdown.node.Node;
import org.entermediadb.markdown.node.Nodes;
import org.entermediadb.markdown.node.ThematicBreak;
import org.entermediadb.markdown.parser.Parser;
import org.entermediadb.markdown.renderer.html.HtmlRenderer;
import org.entermediadb.markdown.renderer.text.TextContentRenderer;
import org.openedit.WebPageRequest;

public class MarkdownUtil 
{
	private static final Log log = LogFactory.getLog(MarkdownUtil.class);

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
	
	public List<Map<String, String>> getHtmlMaps(String markdown)
	{
		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdown);
		
		List<Map<String, String>> maps = new ArrayList<Map<String, String>>();
		
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		
		TextContentRenderer textRenderer = TextContentRenderer.builder().build();

		List<Node> nodes = new ArrayList<Node>();
		
		
		flattenDocument(nodes, document);
		
		boolean headerAdded = false;
		
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();) 
		{
			Node node = (Node) iterator.next();
			
			Map<String, String> map = new HashMap<String, String>();
			
			if( !headerAdded && isHeader(node))
			{
				map.put("type", "Heading");
				String textContent = textRenderer.render(node);
				map.put("content", textContent);
				headerAdded = true;
			}
			else
			{
				String nodeName = node.getClass().getSimpleName();
				if(nodeName.equals("Heading"))
				{
					headerAdded = true;
				}
				map.put("type", nodeName);
				String html = renderer.render(node);
				map.put("content", html);
			}
			
			
			maps.add(map);	
		}
		
		return maps;
	}
	
	private Collection<String> validTypes = Set.of(
			"Block",
			"BlockQuote",
			"BulletList",
			"Code",
			"FencedCodeBlock",
			"Heading",
			"OrderedList",
			"Paragraph"
	);
	
	public void flattenDocument(List<Node> nodes, Node root)
	{
		if(root == null)
		{
			return;
		}
		
		Node first = root.getFirstChild();
		
		if(first == null)
		{
			return;
		}
		
		if(validTypes.contains(first.getClass().getSimpleName()))
		{
			nodes.add(first);
			
			flattenDocument(nodes, first.getNext());
		}
		
		for (Iterator<Node> iterator = Nodes.between(first, null).iterator(); iterator.hasNext();) {
			Node node = (Node) iterator.next();
			
			if(node == null)
			{
				continue;
			}
			
			if(validTypes.contains(node.getClass().getSimpleName()))
			{
				nodes.add(node);
				continue;
			}
			
			flattenDocument(nodes, node);
		}
		
		flattenDocument(nodes, first);
	}
	
	public boolean isHeader(Node node)
	{
		String nodeName = node.getClass().getSimpleName();
		if(nodeName.equals("Heading"))
		{
			return true;
		}
		if(nodeName.equals("Paragraph"))
		{
			Node firstChild = node.getFirstChild();
			
			if(firstChild != null && firstChild.getClass().getSimpleName().equals("StrongEmphasis"))
			{
				Node next = firstChild.getNext();
				if(next != null)
				{
					TextContentRenderer textRenderer = TextContentRenderer.builder().build();
					String textContent = textRenderer.render(next);
					if(textContent == null || textContent.length() == 0)
					{
						return true;
					}
				}
				else
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
}
