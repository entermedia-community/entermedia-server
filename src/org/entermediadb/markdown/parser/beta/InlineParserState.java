package org.entermediadb.markdown.parser.beta;

public interface InlineParserState {

    /**
     * Return a scanner for the input for the current position (on the trigger character that the inline parser was
     * added for).
     * <p>
     * Note that this always returns the same instance, if you want to backtrack you need to use
     * {@link Scanner#position()} and {@link Scanner#setPosition(Position)}.
     */
    Scanner scanner();
}
