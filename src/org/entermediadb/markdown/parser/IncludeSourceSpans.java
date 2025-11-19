package org.entermediadb.markdown.parser;

/**
 * Whether to include {@link org.entermediadb.markdown.node.SourceSpan} or not while parsing,
 * see {@link Parser.Builder#includeSourceSpans(IncludeSourceSpans)}.
 *
 * @since 0.16.0
 */
public enum IncludeSourceSpans {
    /**
     * Do not include source spans.
     */
    NONE,
    /**
     * Include source spans on {@link org.entermediadb.markdown.node.Block} nodes.
     */
    BLOCKS,
    /**
     * Include source spans on block nodes and inline nodes.
     */
    BLOCKS_AND_INLINES,
}
