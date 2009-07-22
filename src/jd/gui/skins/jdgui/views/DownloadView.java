package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.gui.skins.jdgui.interfaces.View;
import jd.gui.skins.jdgui.views.downloadview.DownloadLinksPanel;
import jd.gui.skins.jdgui.views.info.DownloadInfoPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class DownloadView extends View {

    private static final long serialVersionUID = 2624923838160423884L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.downloadview.";

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
        updateToolbar(null, null);
    }

    @Override
    protected void onShow() {
        updateToolbar("downloadview", new String[] {
                "toolbar.control.start",
                "toolbar.control.pause",
                "toolbar.control.stop",
                "toolbar.separator",
                "toolbar.quickconfig.clipboardoberserver",
                "toolbar.quickconfig.reconnecttoggle",
                "toolbar.separator",
                "toolbar.interaction.reconnect",
                "toolbar.interaction.update",
                "toolbar.TESTER"
        });
    }

}
