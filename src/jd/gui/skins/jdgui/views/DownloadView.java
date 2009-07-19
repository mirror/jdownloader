package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.gui.skins.jdgui.components.downloadview.DownloadLinksPanel;
import jd.gui.skins.jdgui.views.info.DownloadInfoPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class DownloadView extends View {

    /**
     * 
     */
    private static final long serialVersionUID = 2624923838160423884L;

    public DownloadView() {
        super();
        this.setContent(new DownloadLinksPanel());
        this.setDefaultInfoPanel(new DownloadInfoPanel());
    }

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.downloadview.";

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub
        return JDTheme.II("gui.images.taskpanes.download", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return JDL.L(IDENT_PREFIX + "tab.title", "Download");
    }

    @Override
    public String getTooltip() {
        // TODO Auto-generated method stub
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Downloadlist and Progress");
    }

}
