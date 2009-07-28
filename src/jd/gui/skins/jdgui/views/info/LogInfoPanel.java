package jd.gui.skins.jdgui.views.info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import jd.controlling.JDLogger;
import jd.gui.skins.jdgui.swing.GuiRunnable;
import jd.gui.skins.swing.Factory;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LogInfoPanel extends InfoPanel implements ActionListener {

    private static final long serialVersionUID = -1910950245889164423L;
    private static final String JDL_PREFIX = "jd.gui.skins.jdgui.views.info.LogInfoPanel.";

    public static final int ACTION_SAVE = 1;
    public static final int ACTION_UPLOAD = 2;

    private JButton btnSave;
    private JButton btnUpload;
    private int severeCount = 0;
    private int warningCount = 0;
    private int httpCount = 0;
    private int exceptionCount = 0;

    public LogInfoPanel() {
        super();
        this.setIcon(JDTheme.II("gui.images.taskpanes.log", 32, 32));

        btnSave = Factory.createButton(JDL.L(JDL_PREFIX + "save", "Save Log As"), JDTheme.II("gui.images.save", 16, 16), this);
        btnUpload = Factory.createButton(JDL.L(JDL_PREFIX + "upload", "Upload Log"), JDTheme.II("gui.images.upload", 16, 16), this);
        this.addInfoEntry("", JDL.LF(JDL_PREFIX + "loglevel", "Log Level %s", JDLogger.getLogger().getLevel().getLocalizedName()), 0, 0);
        addComponent(btnSave, 1, 0);
        addComponent(btnUpload, 1, 1);
        this.addInfoEntry(JDL.L(JDL_PREFIX + "info.severe", "Error(s)"), severeCount + "", 2, 0);
        this.addInfoEntry(JDL.L(JDL_PREFIX + "info.warning", "Warning(s)"), warningCount + "", 2, 1);

        this.addInfoEntry(JDL.L(JDL_PREFIX + "info.warninghttp", "HTTP Notify"), httpCount + "", 3, 0);
        this.addInfoEntry(JDL.L(JDL_PREFIX + "info.exceptions", "Fatal error(s)"), exceptionCount + "", 3, 1);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, e.getActionCommand()));
        } else if (e.getSource() == btnUpload) {
            this.broadcastEvent(new ActionEvent(this, ACTION_UPLOAD, e.getActionCommand()));
        }
    }

    /**
     * @param severeCount
     *            the severeCount to set
     */
    public void setSevereCount(int severeCount) {
        this.severeCount = severeCount;
    }

    /**
     * @return the severeCount
     */
    public int getSevereCount() {
        return severeCount;
    }

    /**
     * @param warningCount
     *            the warningCount to set
     */
    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    /**
     * @return the warningCount
     */
    public int getWarningCount() {
        return warningCount;
    }

    /**
     * @param httpCount
     *            the httpCount to set
     */
    public void setHttpCount(int httpCount) {
        this.httpCount = httpCount;
    }

    /**
     * @return the httpCount
     */
    public int getHttpCount() {
        return httpCount;
    }

    /**
     * @param exceptionCount
     *            the exceptionCount to set
     */
    public void setExceptionCount(int exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    /**
     * @return the exceptionCount
     */
    public int getExceptionCount() {
        return exceptionCount;
    }

    public void update() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                updateInfo("", JDL.LF(JDL_PREFIX + "loglevel", "Log Level %s", JDLogger.getLogger().getLevel().getLocalizedName()));
                updateInfo(JDL.L(JDL_PREFIX + "info.severe", "Error(s)"), severeCount + "");
                updateInfo(JDL.L(JDL_PREFIX + "info.warning", "Warning(s)"), warningCount + "");
                updateInfo(JDL.L(JDL_PREFIX + "info.warninghttp", "HTTP Notify"), httpCount + "");
                updateInfo(JDL.L(JDL_PREFIX + "info.exceptions", "Fatal error(s)"), exceptionCount + "");
                return null;
            }
        }.start();
    }
}
