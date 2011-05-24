package org.jdownloader.extensions.jdfeedme;

import javax.swing.Icon;

import jd.plugins.AddonPanel;

import org.jdownloader.images.NewTheme;

public class JDFeedMeView extends AddonPanel<FeedMeExtension> {

    private static final long serialVersionUID = -8074441650881378626L;

    public JDFeedMeView(FeedMeExtension owner) {
        super(owner);

        init();
    }

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub

        /*
         * CODE_FOR_INTERFACE_5_START return NewTheme.I().getIcon("reconnect",
         * ICON_SIZE, ICON_SIZE); CODE_FOR_INTERFACE_5_END
         */
        /* CODE_FOR_INTERFACE_7_START */
        return NewTheme.I().getIcon("rss", ICON_SIZE);
        /* CODE_FOR_INTERFACE_7_END */

    }

    @Override
    public String getTitle() {

        return "JD FeedMe";
    }

    @Override
    public String getTooltip() {

        return "Autodownload files from RSS feeds";
    }

    /* CODE_FOR_INTERFACE_7_START */
    @Override
    protected void onHide() {
    }

    /* CODE_FOR_INTERFACE_7_END */

    /* CODE_FOR_INTERFACE_7_START */
    @Override
    protected void onShow() {
    }

    /* CODE_FOR_INTERFACE_7_END */

    @Override
    public String getID() {
        return "jdfeedme";
    }

    @Override
    protected void onDeactivated() {
    }

    @Override
    protected void onActivated() {
    }

}
