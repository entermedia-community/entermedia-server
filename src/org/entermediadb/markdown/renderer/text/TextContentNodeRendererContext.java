package org.entermediadb.markdown.renderer.text;

import org.entermediadb.markdown.node.Node;

public interface TextContentNodeRendererContext {

    /**
     * Controls how line breaks should be rendered, see {@link LineBreakRendering}.
     */
    LineBreakRendering lineBreakRendering();

    /**
     * @return true for stripping new lines and render text as "single line",
     * false for keeping all line breaks.
     * @deprecated Use {@link #lineBreakRendering()} instead
     */
    @Deprecated
    boolean stripNewlines();

    /**
     * @return the writer to use
     */
    TextContentWriter getWriter();

    /**
     * Render the specified node and its children using the configured renderers. This should be used to render child
     * nodes; be careful not to pass the node that is being rendered, that would result in an endless loop.
     *
     * @param node the node to render
     */
    void render(Node node);
}
