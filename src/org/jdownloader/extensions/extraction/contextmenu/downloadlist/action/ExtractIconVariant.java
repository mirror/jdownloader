package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.AlphaComposite;

import org.jdownloader.images.BadgeIcon;

public class ExtractIconVariant extends BadgeIcon {

    public ExtractIconVariant(String badge, int size) {
        super(org.jdownloader.gui.IconKey.ICON_COMPRESS, badge, size);
    }

    public ExtractIconVariant(String badge, int size, int badgesize) {
        super(org.jdownloader.gui.IconKey.ICON_COMPRESS, badge, size, badgesize, 0, 0);

    }

    protected void addMain() {
        add(mainIcon, 0, 0, 0, AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
    }

    public ExtractIconVariant(String badge, int size, int badgesize, int xOffset, int yOffset) {
        super(org.jdownloader.gui.IconKey.ICON_COMPRESS, badge, size, badgesize, xOffset, yOffset);

    }

}
