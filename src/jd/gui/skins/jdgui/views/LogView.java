package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.gui.skins.jdgui.views.info.LogInfoPanel;
import jd.gui.skins.simple.LogPane;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LogView extends View {

    private static final long serialVersionUID = -4440872942373187410L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.logview.";

    public LogView() {
        super();
        LogInfoPanel lip;
        LogPane lp;
        this.setContent(lp = new LogPane());
        this.setDefaultInfoPanel(lip = new LogInfoPanel());
        lip.addActionListener(lp);
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.log", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Log");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "See or Upload the Log");
    }

}