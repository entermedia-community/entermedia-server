package org.entermediadb.markdown.parser.block;

import org.entermediadb.markdown.node.Block;
import org.entermediadb.markdown.node.DefinitionMap;
import org.entermediadb.markdown.node.SourceSpan;
import org.entermediadb.markdown.parser.InlineParser;
import org.entermediadb.markdown.parser.SourceLine;

import java.util.List;

public abstract class AbstractBlockParser implements BlockParser {

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public boolean canHaveLazyContinuationLines() {
        return false;
    }

    @Override
    public boolean canContain(Block childBlock) {
        return false;
    }

    @Override
    public void addLine(SourceLine line) {
    }

    @Override
    public void addSourceSpan(SourceSpan sourceSpan) {
        getBlock().addSourceSpan(sourceSpan);
    }

    @Override
    public List<DefinitionMap<?>> getDefinitions() {
        return List.of();
    }

    @Override
    public void closeBlock() {
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
    }

}
