package org.entermediadb.markdown.internal.renderer.text;

import org.entermediadb.markdown.node.BulletList;

public class BulletListHolder extends ListHolder {
    private final String marker;

    public BulletListHolder(ListHolder parent, BulletList list) {
        super(parent);
        marker = list.getMarker();
    }

    public String getMarker() {
        return marker;
    }
}
