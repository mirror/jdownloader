package jd.gui.swing.jdgui.views;

import javax.swing.Icon;

import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.views.downloadview.DownloadLinksPanel;
import jd.gui.swing.jdgui.views.info.DownloadInfoPanel;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class DownloadView extends View {

    private static final long serialVersionUID = 2624923838160423884L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.downloadview.";

    public DownloadView() {
        super();
        this.setContent(new DownloadLinksPanel());
        this.setDefaultInfoPanel(new DownloadInfoPanel());
        ViewToolbar toolbar = new ViewToolbar();
        toolbar.setList(new String[] { "action.downloadview.movetotop", "action.downloadview.movetobottom" });
        this.setToolBar(toolbar);
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

}
