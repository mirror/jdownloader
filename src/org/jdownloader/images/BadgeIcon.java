/**
 * Copyright (c) 2009 - 2013 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.swing.components
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.jdownloader.images;

import javax.swing.Icon;

import org.appwork.swing.components.ExtMergedIcon;

/**
 * @author Thomas
 * 
 */
public class BadgeIcon extends ExtMergedIcon {

    /**
     * @param iconCompress
     * @param badge
     * @param size
     * @param badgesize
     * @param xOffset
     * @param yOffset
     */
    public BadgeIcon(final String mainIconKEy, final String badge, final int size, final int badgesize, final int xOffset, final int yOffset) {
        this(new AbstractIcon(mainIconKEy, size), new AbstractIcon(badge, badgesize), xOffset, yOffset);

    }

    protected void addMain() {
        add(mainIcon, 0, 0, 0, null);
    }

    public BadgeIcon(final String mainIconKEy, String badge, int size, int badgesize) {

        this(mainIconKEy, badge, size, badgesize, 0, 0);
    }

    public BadgeIcon(String iconSkipped, String iconKey, int size) {
        this(iconSkipped, iconKey, size, (size * 2) / 3);
    }

    public BadgeIcon(Icon main, Icon badgeIcon, int xOffset, int yOffset) {
        mainIcon = main;
        addMain();
        add(badgeIcon, xOffset + mainIcon.getIconWidth() - badgeIcon.getIconWidth(), yOffset + mainIcon.getIconHeight() - badgeIcon.getIconHeight());
    }

    protected Icon mainIcon;

    public ExtMergedIcon crop() {
        return crop(mainIcon.getIconWidth(), mainIcon.getIconHeight());
    }

}
