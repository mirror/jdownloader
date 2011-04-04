package org.jdownloader.gui.views;

import javax.swing.Icon;

import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class DownloadsView extends View {

    private static final long   serialVersionUID = 2624923838160423884L;

    private static final String IDENT_PREFIX     = "jd.gui.swing.jdgui.views.downloadview.";

    public DownloadsView() {
        super();
        this.setContent(new DownloadsPanel());
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.download", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Download");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Downloadlist and Progress");
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "downloadsview";
    }

}
