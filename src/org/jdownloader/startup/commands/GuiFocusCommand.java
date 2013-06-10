package org.jdownloader.startup.commands;

import jd.SecondLevelLaunch;
import jd.gui.UIConstants;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.swing.EDTRunner;

public class GuiFocusCommand extends AbstractStartupCommand {

    public GuiFocusCommand() {
        super("focus", "f", "afterupdate");

    }

    @Override
    public void run(final String command, final String... parameters) {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        UserIF.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_FOREGROUND);
                    }
                };

            }

        });

        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDGui.getInstance().setWindowToTray(false);
                    }
                };

            }
        });
    }

    @Override
    public String getDescription() {
        return "Focus JDownloader and bring JD to TOP";
    }

}
