package org.jdownloader.startup.commands;

import jd.SecondLevelLaunch;
import jd.gui.UIConstants;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.swing.EDTRunner;

public class GuiMinimizeCommand extends AbstractStartupCommand {

    public GuiMinimizeCommand() {
        super("minimize", "m");
    }

    @Override
    public void run(final String command, final String... parameters) {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        UserIF.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_MINIMIZED);
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
                        JDGui.getInstance().setWindowToTray(true);
                    }
                };

            }
        });
    }

    @Override
    public String getDescription() {
        return "Minimize JDownloader to the Taskbar";
    }

}
