package org.entermediadb.markdown.node;

/**
 * HTML block
 *
 * @see <a href="http://spec.commonmark.org/0.31.2/#html-blocks">CommonMark Spec</a>
 */
public class HtmlBlock extends Block {

    private String literal;

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getLiteral() {
        return literal;
    }

    public void setLiteral(String literal) {
        this.literal = literal;
    }
}
