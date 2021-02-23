package org.jdownloader.startup.commands;

import org.appwork.shutdown.ExceptionShutdownRequest;
import org.appwork.shutdown.ShutdownController;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;

public class MyJDownloaderCommand extends AbstractStartupCommand {
    public MyJDownloaderCommand() {
        super("myjd");
    }

    @Override
    public void run(String command, String... parameters) {
        final MyJDownloaderController myJDownloaderController = MyJDownloaderController.getInstance();
        while (true) {
            try {
                if (myJDownloaderController.askLoginsOnConsole(true)) {
                    new Thread() {
                        public void run() {
                            myJDownloaderController.connect();
                        }
                    }.start();
                    break;
                }
            } catch (DialogNoAnswerException e) {
                if (!myJDownloaderController.isLoginValid(true) && myJDownloaderController.isAlwaysConnectRequired()) {
                    ShutdownController.getInstance().requestShutdown(new ExceptionShutdownRequest(e, false, false));
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Init MyJdownloader -myjd";
    }
}
