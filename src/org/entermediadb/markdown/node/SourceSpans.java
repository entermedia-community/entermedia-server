package org.entermediadb.markdown.node;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of source spans that can be added to. Takes care of merging adjacent source spans.
 *
 * @since 0.16.0
 */
public class SourceSpans {

    private List<SourceSpan> sourceSpans;

    public static SourceSpans empty() {
        return new SourceSpans();
    }

    public List<SourceSpan> getSourceSpans() {
        return sourceSpans != null ? sourceSpans : List.of();
    }

    public void addAllFrom(Iterable<? extends Node> nodes) {
        for (Node node : nodes) {
            addAll(node.getSourceSpans());
        }
    }

    public void addAll(List<SourceSpan> other) {
        if (other.isEmpty()) {
            return;
        }

        if (sourceSpans == null) {
            sourceSpans = new ArrayList<>();
        }

        if (sourceSpans.isEmpty()) {
            sourceSpans.addAll(other);
        } else {
            int lastIndex = sourceSpans.size() - 1;
            SourceSpan a = sourceSpans.get(lastIndex);
            SourceSpan b = other.get(0);
            if (a.getInputIndex() + a.getLength() == b.getInputIndex()) {
                sourceSpans.set(lastIndex, SourceSpan.of(a.getLineIndex(), a.getColumnIndex(), a.getInputIndex(), a.getLength() + b.getLength()));
                sourceSpans.addAll(other.subList(1, other.size()));
            } else {
                sourceSpans.addAll(other);
            }
        }
    }
}
