package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewAction(PackagizerFilterTable table) {
        super();

    }

    public void actionPerformed(ActionEvent e) {
        final PackagizerRule rule = new PackagizerRule();
        add(rule);
    }

    public static void add(final PackagizerRule rule) {
        try {
            PackagizerFilterRuleDialog.showDialog(rule);
            rule.setEnabled(true);
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    PackagizerController.getInstance().add(rule);
                    return null;
                }
            });
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
