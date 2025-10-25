package org.entermediadb.markdown.internal.renderer.text;

import org.entermediadb.markdown.node.OrderedList;

public class OrderedListHolder extends ListHolder {
    private final String delimiter;
    private int counter;

    public OrderedListHolder(ListHolder parent, OrderedList list) {
        super(parent);
        delimiter = list.getMarkerDelimiter() != null ? list.getMarkerDelimiter() : ".";
        counter = list.getMarkerStartNumber() != null ? list.getMarkerStartNumber() : 1;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public int getCounter() {
        return counter;
    }

    public void increaseCounter() {
        counter++;
    }
}
