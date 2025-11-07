package org.entermediadb.markdown.internal.inline;

import org.entermediadb.markdown.internal.util.Html5Entities;
import org.entermediadb.markdown.node.Text;
import org.entermediadb.markdown.parser.beta.*;
import org.entermediadb.markdown.text.AsciiMatcher;

import java.util.Set;

/**
 * Attempts to parse an HTML entity or numeric character reference.
 */
public class EntityInlineParser implements InlineContentParser {

    private static final AsciiMatcher hex = AsciiMatcher.builder().range('0', '9').range('A', 'F').range('a', 'f').build();
    private static final AsciiMatcher dec = AsciiMatcher.builder().range('0', '9').build();
    private static final AsciiMatcher entityStart = AsciiMatcher.builder().range('A', 'Z').range('a', 'z').build();
    private static final AsciiMatcher entityContinue = entityStart.newBuilder().range('0', '9').build();

    @Override
    public ParsedInline tryParse(InlineParserState inlineParserState) {
        Scanner scanner = inlineParserState.scanner();
        Position start = scanner.position();
        // Skip `&`
        scanner.next();

        char c = scanner.peek();
        if (c == '#') {
            // Numeric
            scanner.next();
            if (scanner.next('x') || scanner.next('X')) {
                int digits = scanner.match(hex);
                if (1 <= digits && digits <= 6 && scanner.next(';')) {
                    return entity(scanner, start);
                }
            } else {
                int digits = scanner.match(dec);
                if (1 <= digits && digits <= 7 && scanner.next(';')) {
                    return entity(scanner, start);
                }
            }
        } else if (entityStart.matches(c)) {
            scanner.match(entityContinue);
            if (scanner.next(';')) {
                return entity(scanner, start);
            }
        }

        return ParsedInline.none();
    }

    private Html5Entities entities = null;

    private ParsedInline entity(Scanner scanner, Position start) {
        String text = scanner.getSource(start, scanner.position()).getContent();
        
        if( entities == null)
    	{
    		entities = new Html5Entities();
    	}
        
        return ParsedInline.of(new Text(entities.entityToString(text)), scanner.position());
    }

    public static class Factory implements InlineContentParserFactory {

        @Override
        public Set<Character> getTriggerCharacters() {
            return Set.of('&');
        }

        @Override
        public InlineContentParser create() {
            return new EntityInlineParser();
        }
    }
}
