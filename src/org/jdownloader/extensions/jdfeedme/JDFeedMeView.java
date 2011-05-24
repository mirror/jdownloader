package org.jdownloader.extensions.jdfeedme;

import javax.swing.Icon;

import jd.plugins.AddonPanel;
import jd.utils.JDTheme;

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
         * CODE_FOR_INTERFACE_5_START return JDTheme.II("reconnect", ICON_SIZE,
         * ICON_SIZE); CODE_FOR_INTERFACE_5_END
         */
        /* CODE_FOR_INTERFACE_7_START */
        return JDTheme.II("rss", ICON_SIZE, ICON_SIZE);
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
