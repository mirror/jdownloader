package org.jdownloader.startup.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.appwork.shutdown.ShutdownController;
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
            int i = 15;
            while ((th = MyJDownloaderController.getInstance().getConnectThread()) == null || th.getConnectionStatus() == MyJDownloaderConnectionStatus.UNCONNECTED) {
                System.out.println("Wait For MyJDownloader Connection");
                Thread.sleep(1000);
                if (i-- <= 0) {
                    break;
                }
            }
            if ((th = MyJDownloaderController.getInstance().getConnectThread()) == null || th.getConnectionStatus() == MyJDownloaderConnectionStatus.UNCONNECTED) {
                CFG_MYJD.PASSWORD.setValue(null);
                System.out.println("Invalid Logins");
                System.out.println("Error Details:" + CFG_MYJD.CFG.getLatestError());
                ShutdownController.getInstance().requestShutdown();
            } else {
                System.out.println("My JDownloader Connection Estabilished");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Init MyJdownloader after startup -myjd";
    }

}
