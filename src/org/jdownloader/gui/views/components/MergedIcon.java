package org.jdownloader.gui.views.components;

import javax.swing.Icon;

import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;

public class MergedIcon extends ExtMergedIcon {

    private int gap = 5;

    public MergedIcon(Icon... icons) {
        super();
        int height = 0;
        int width = 0;
        for (Icon i : icons) {
            height = Math.max(height, i.getIconHeight());
            if (width > 0) {
                width += gap;
            }
            width += i.getIconWidth();
        }
        int x = 0;
        int i = 0;
        for (Icon c : icons) {
            add(c, x, (height - c.getIconHeight()) / 2, i++, null);
            x += c.getIconWidth() + gap;
        }

    }

    @Override
    public IconIdentifier getIdentifier() {
        if (internalID != null) {
            return internalID;
        }
        IconIdentifier map = new IconIdentifier("ColMerge");

        for (Entry e : entries) {
            if (e.icon instanceof IDIcon) {
                map.add(((IDIcon) e.icon).getIdentifier());
            } else {
                map.add(new IconIdentifier("unknown", e.icon.toString()));
            }
        }

        return map;
    }
}
