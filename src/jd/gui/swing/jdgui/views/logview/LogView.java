package jd.gui.swing.jdgui.views.logview;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.Icon;

import jd.controlling.JDLogHandler;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.views.info.LogInfoPanel;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class LogView extends View implements ControlListener {

    private static final long serialVersionUID = -4440872942373187410L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.logview.";

    private LogInfoPanel lip;

    public LogView() {
        super();
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

    @Override
    protected void onHide() {
        JDUtilities.getController().removeControlListener(this);
    }

    @Override
    protected void onShow() {
        JDUtilities.getController().addControlListener(this);
        int severe = 0;
        int warning = 0;
        int exceptions = 0;
        int http = 0;
        ArrayList<LogRecord> buff = JDLogHandler.getHandler().getBuffer();

        for (LogRecord r : buff) {
            if (r.getMessage().contains("exception"))
                exceptions++;
            else if (r.getLevel() == Level.SEVERE)
                severe++;
            else if (r.getLevel() == Level.WARNING)
                warning++;
            else if (r.getMessage().contains("--Request--")) http++;
        }
        lip.setSevereCount(severe);
        lip.setWarningCount(warning);
        lip.setExceptionCount(exceptions);
        lip.setHttpCount(http);
        lip.update();
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED) {
            LogRecord r = (LogRecord) event.getParameter();
            if (r.getMessage().contains("exception")) {
                lip.setExceptionCount(lip.getExceptionCount() + 1);
                lip.update();
            } else if (r.getLevel() == Level.SEVERE) {
                lip.setSevereCount(lip.getSevereCount() + 1);
                lip.update();
            } else if (r.getLevel() == Level.WARNING) {
                lip.setWarningCount(lip.getWarningCount() + 1);
                lip.update();
            } else if (r.getMessage().contains("--Request--")) {
                lip.setHttpCount(lip.getHttpCount() + 1);
                lip.update();
            }
        }
    }

}