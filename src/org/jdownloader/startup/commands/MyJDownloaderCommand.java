package org.jdownloader.startup.commands;

import org.appwork.console.AbstractConsole;
import org.appwork.console.ConsoleDialog;
import org.appwork.shutdown.ShutdownController;
import org.appwork.utils.Regex;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderCommand extends AbstractStartupCommand {
    public MyJDownloaderCommand() {
        super("myjd");
    }

    @Override
    public void run(String command, String... parameters) {
        synchronized (AbstractConsole.LOCK) {
            final ConsoleDialog cd = new ConsoleDialog("MyJDownloader");
            cd.start();
            try {
                try {
                    while (true) {
                        cd.waitYesOrNo(0, "Enter Logins", "Exit JDownloader");
                        final String email = cd.ask("Please Enter your MyJDownloader Email:");
                        if (new Regex(email, "..*?@.*?\\..+").matches()) {
                            final String password = cd.askHidden("Please Enter your MyJDownloader Password(not visible):");
                            if (MyJDownloaderController.validateLogins(email, password)) {
                                CFG_MYJD.EMAIL.setValue(email.trim());
                                CFG_MYJD.PASSWORD.setValue(password);
                                new Thread() {
                                    public void run() {
                                        MyJDownloaderController.getInstance().connect();
                                    }
                                }.start();
                                return;
                            }
                        }
                        cd.println("Invalid Logins");
                    }
                } catch (DialogNoAnswerException e) {
                    ShutdownController.getInstance().requestShutdown();
                }
            } finally {
                cd.end();
            }
        }
    }

    @Override
    public String getDescription() {
        return "Init MyJdownloader -myjd";
    }
}
