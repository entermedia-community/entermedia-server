package org.entermediadb.markdown.internal.inline;

import org.entermediadb.markdown.node.Node;
import org.entermediadb.markdown.parser.beta.ParsedInline;
import org.entermediadb.markdown.parser.beta.Position;

public class ParsedInlineImpl implements ParsedInline {
    private final Node node;
    private final Position position;

    public ParsedInlineImpl(Node node, Position position) {
        this.node = node;
        this.position = position;
    }

    public Node getNode() {
        return node;
    }

    public Position getPosition() {
        return position;
    }
}
