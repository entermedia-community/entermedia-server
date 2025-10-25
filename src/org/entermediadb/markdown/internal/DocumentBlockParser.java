package org.entermediadb.markdown.internal;

import org.entermediadb.markdown.node.Block;
import org.entermediadb.markdown.node.Document;
import org.entermediadb.markdown.parser.SourceLine;
import org.entermediadb.markdown.parser.block.AbstractBlockParser;
import org.entermediadb.markdown.parser.block.BlockContinue;
import org.entermediadb.markdown.parser.block.ParserState;

public class DocumentBlockParser extends AbstractBlockParser {

    private final Document document = new Document();

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public boolean canContain(Block block) {
        return true;
    }

    @Override
    public Document getBlock() {
        return document;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        return BlockContinue.atIndex(state.getIndex());
    }

    @Override
    public void addLine(SourceLine line) {
    }

}
