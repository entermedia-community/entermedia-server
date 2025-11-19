package org.entermediadb.markdown.parser;

import org.entermediadb.markdown.node.Node;

/**
 * Parser for inline content (text, links, emphasized text, etc).
 */
public interface InlineParser {

    /**
     * @param lines the source content to parse as inline
     * @param node the node to append resulting nodes to (as children)
     */
    void parse(SourceLines lines, Node node);
}
