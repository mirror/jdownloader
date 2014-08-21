package org.jdownloader.startup.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.settings.staticreferences.CFG_MYJD;

public class MyJDownloaderCommand extends AbstractStartupCommand {

    public MyJDownloaderCommand() {
        super("myjd");
    }

    @Override
    public void run(String command, String... parameters) {

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String pw = CFG_MYJD.PASSWORD.getValue();
            while (!MyJDownloaderController.validateLogins(CFG_MYJD.EMAIL.getValue(), CFG_MYJD.PASSWORD.getValue())) {

                System.out.println("Please Enter the MYJDownloader Email Adress");

                CFG_MYJD.EMAIL.setValue(br.readLine());

                System.out.println("Please Enter the MYJDownloader Password");

                CFG_MYJD.PASSWORD.setValue(br.readLine());
                if (!MyJDownloaderController.validateLogins(CFG_MYJD.EMAIL.getValue(), CFG_MYJD.PASSWORD.getValue())) {
                    System.err.println("Invalid Logins");
                }
            }
            CFG_MYJD.AUTO_CONNECT_ENABLED.setValue(true);
            MyJDownloaderController.getInstance().connect();
            MyJDownloaderConnectThread th = MyJDownloaderController.getInstance().getConnectThread();
            if (th == null || th.getConnectionStatus() == MyJDownloaderConnectionStatus.UNCONNECTED) {
                CFG_MYJD.PASSWORD.setValue(null);
                System.err.println("Invalid Logins");
                System.err.println("Error Details:" + CFG_MYJD.CFG.getLatestError());
                System.exit(1);
            } else {
                System.out.println("My JDownloader Connection Estabilished");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Init MyJdownloader after startup -myjd";
    }

}
