package jd.gui.swing.jdgui.components.modules;

import jd.controlling.ProgressController;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.translate.T;

public class ProgressControllerDialog {

    public static void show(final ProgressController source) {

        final ProgressGetter progressAdapter = new ProgressGetter() {

            public int getProgress() {

                return source.isFinalizing() ? 100 : (int) source.getPercent();
            }

            public String getString() {
                return source.getStatusText();
            }

            public void run() throws Exception {
                while (true) {
                    Thread.sleep(1000);
                }
            }

        };
        final ProgressDialog dialog = new ProgressDialog(progressAdapter, 0, "Please Wait",

        source.getName(), null, null, T._.ProgressControllerDialog_minimize());
        new Thread() {
            public void run() {
                try {
                    Dialog.getInstance().showDialog(dialog);
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

}