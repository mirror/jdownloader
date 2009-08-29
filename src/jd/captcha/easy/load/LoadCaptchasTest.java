package jd.captcha.easy.load;

import java.awt.Toolkit;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;
import jd.gui.userio.DummyFrame;

public class LoadCaptchasTest {
    public static void main(String[] args) throws Exception {
        new GuiRunnable<Object>() {
            public Object runSave() {
                LookAndFeelController.setUIManager();
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTEventQueue());
                new LoadCaptchas(DummyFrame.getDialogParent(), null, true).start();
                return null;
            }
        }.waitForEDT();

        System.exit(0);
    }
}
