package org.entermediadb.markdown.internal;

import org.entermediadb.markdown.node.Block;
import org.entermediadb.markdown.node.HtmlBlock;
import org.entermediadb.markdown.node.Paragraph;
import org.entermediadb.markdown.parser.SourceLine;
import org.entermediadb.markdown.parser.block.*;

import java.util.regex.Pattern;

public class HtmlBlockParser extends AbstractBlockParser {

    private static final String TAGNAME = "[A-Za-z][A-Za-z0-9-]*";
    private static final String ATTRIBUTENAME = "[a-zA-Z_:][a-zA-Z0-9:._-]*";
    private static final String UNQUOTEDVALUE = "[^\"'=<>`\\x00-\\x20]+";
    private static final String SINGLEQUOTEDVALUE = "'[^']*'";
    private static final String DOUBLEQUOTEDVALUE = "\"[^\"]*\"";
    private static final String ATTRIBUTEVALUE = "(?:" + UNQUOTEDVALUE + "|" + SINGLEQUOTEDVALUE
            + "|" + DOUBLEQUOTEDVALUE + ")";
    private static final String ATTRIBUTEVALUESPEC = "(?:" + "\\s*=" + "\\s*" + ATTRIBUTEVALUE
            + ")";
    private static final String ATTRIBUTE = "(?:" + "\\s+" + ATTRIBUTENAME + ATTRIBUTEVALUESPEC
            + "?)";

    private static final String OPENTAG = "<" + TAGNAME + ATTRIBUTE + "*" + "\\s*/?>";
    private static final String CLOSETAG = "</" + TAGNAME + "\\s*[>]";

    private static final Pattern[][] BLOCK_PATTERNS = new Pattern[][]{
            {null, null}, // not used (no type 0)
            {
                    Pattern.compile("^<(?:script|pre|style|textarea)(?:\\s|>|$)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("</(?:script|pre|style|textarea)>", Pattern.CASE_INSENSITIVE)
            },
            {
                    Pattern.compile("^<!--"),
                    Pattern.compile("-->")
            },
            {
                    Pattern.compile("^<[?]"),
                    Pattern.compile("\\?>")
            },
            {
                    Pattern.compile("^<![A-Z]"),
                    Pattern.compile(">")
            },
            {
                    Pattern.compile("^<!\\[CDATA\\["),
                    Pattern.compile("\\]\\]>")
            },
            {
                    Pattern.compile("^</?(?:" +
                            "address|article|aside|" +
                            "base|basefont|blockquote|body|" +
                            "caption|center|col|colgroup|" +
                            "dd|details|dialog|dir|div|dl|dt|" +
                            "fieldset|figcaption|figure|footer|form|frame|frameset|" +
                            "h1|h2|h3|h4|h5|h6|head|header|hr|html|" +
                            "iframe|" +
                            "legend|li|link|" +
                            "main|menu|menuitem|" +
                            "nav|noframes|" +
                            "ol|optgroup|option|" +
                            "p|param|" +
                            "search|section|summary|" +
                            "table|tbody|td|tfoot|th|thead|title|tr|track|" +
                            "ul" +
                            ")(?:\\s|[/]?[>]|$)", Pattern.CASE_INSENSITIVE),
                    null // terminated by blank line
            },
            {
                    Pattern.compile("^(?:" + OPENTAG + '|' + CLOSETAG + ")\\s*$", Pattern.CASE_INSENSITIVE),
                    null // terminated by blank line
            }
    };

    private final HtmlBlock block = new HtmlBlock();
    private final Pattern closingPattern;

    private boolean finished = false;
    private BlockContent content = new BlockContent();

    private HtmlBlockParser(Pattern closingPattern) {
        this.closingPattern = closingPattern;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (finished) {
            return BlockContinue.none();
        }

        // Blank line ends type 6 and type 7 blocks
        if (state.isBlank() && closingPattern == null) {
            return BlockContinue.none();
        } else {
            return BlockContinue.atIndex(state.getIndex());
        }
    }

    @Override
    public void addLine(SourceLine line) {
        content.add(line.getContent());

        if (closingPattern != null && closingPattern.matcher(line.getContent()).find()) {
            finished = true;
        }
    }

    @Override
    public void closeBlock() {
        block.setLiteral(content.getString());
        content = null;
    }

    public static class Factory extends AbstractBlockParserFactory {

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            int nextNonSpace = state.getNextNonSpaceIndex();
            CharSequence line = state.getLine().getContent();

            if (state.getIndent() < 4 && line.charAt(nextNonSpace) == '<') {
                for (int blockType = 1; blockType <= 7; blockType++) {
                    // Type 7 can not interrupt a paragraph (not even a lazy one)
                    if (blockType == 7 && (
                            matchedBlockParser.getMatchedBlockParser().getBlock() instanceof Paragraph ||
                                    state.getActiveBlockParser().canHaveLazyContinuationLines())) {
                        continue;
                    }
                    Pattern opener = BLOCK_PATTERNS[blockType][0];
                    Pattern closer = BLOCK_PATTERNS[blockType][1];
                    boolean matches = opener.matcher(line.subSequence(nextNonSpace, line.length())).find();
                    if (matches) {
                        return BlockStart.of(new HtmlBlockParser(closer)).atIndex(state.getIndex());
                    }
                }
            }
            return BlockStart.none();
        }
    }
}
