package jd.updater;

import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;

public class Main {
    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                UpdaterGui.getInstance().start();
            }
        };
    }
}
