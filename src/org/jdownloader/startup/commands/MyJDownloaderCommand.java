package org.jdownloader.startup.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

            System.out.println("Please Enter the MYJDownloader Email Adress");

            CFG_MYJD.EMAIL.setValue(br.readLine());

            System.out.println("Please Enter the MYJDownloader Password");

            CFG_MYJD.PASSWORD.setValue(br.readLine());
            if (!MyJDownloaderController.validateLogins(CFG_MYJD.EMAIL.getValue(), CFG_MYJD.PASSWORD.getValue())) {
                System.err.println("Invalid Logins");
            }

            CFG_MYJD.AUTO_CONNECT_ENABLED.setValue(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Init MyJdownloader -myjd";
    }

}
