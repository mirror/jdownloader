package jd.gui.swing.jdgui.views;

import javax.swing.Icon;

import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.views.downloadview.DownloadLinksPanel;
import jd.gui.swing.jdgui.views.info.DownloadInfoPanel;
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
        ActionController.getToolBarAction("action.downloadview.movetotop").setEnabled(false);
        ActionController.getToolBarAction("action.downloadview.moveup").setEnabled(false);
        ActionController.getToolBarAction("action.downloadview.movedown").setEnabled(false);
        ActionController.getToolBarAction("action.downloadview.movetobottom").setEnabled(false);
    }

    @Override
    protected void onShow() {

        ActionController.getToolBarAction("action.downloadview.movetotop").setEnabled(true);
        ActionController.getToolBarAction("action.downloadview.moveup").setEnabled(true);
        ActionController.getToolBarAction("action.downloadview.movedown").setEnabled(true);
        ActionController.getToolBarAction("action.downloadview.movetobottom").setEnabled(true);
    }

}
