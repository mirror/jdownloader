package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.gui.skins.simple.components.DownloadView.DownloadLinksPanel;
import jd.gui.skins.simple.tasks.DownloadTaskPane;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class AddonView extends View{
    
    public AddonView() {
        super();
        this.setSideBar(new DownloadTaskPane(JDL.L("gui.taskpanes.download", "Download"), JDTheme.II("gui.images.taskpanes.download", 16, 16)));
        this.setContent(new DownloadLinksPanel());

    }
    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.addonview.";

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub
        return JDTheme.II("gui.images.taskpanes.addons", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return JDL.L(IDENT_PREFIX + "tab.title", "Addons");
    }

    @Override
    public String getTooltip() {
        // TODO Auto-generated method stub
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Addon settings");
    }

}
