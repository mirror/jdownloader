package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.UpdateController;

public class UpdateAction extends AbstractToolbarAction {
    private static final UpdateAction INSTANCE = new UpdateAction();

    /**
     * get the only existing instance of UpdateAction. This is a singleton
     * 
     * @return
     */
    public static UpdateAction getInstance() {
        return UpdateAction.INSTANCE;
    }

    /**
     * Create a new instance of UpdateAction. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private UpdateAction() {
        setEnabled(false);
    }

    public boolean isDefaultVisible() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        /* WebUpdate is running in its own Thread */
        new Thread() {
            public void run() {
                // runUpdateChecker is synchronized and may block
                UpdateController.getInstance().setGuiVisible(true);
                UpdateController.getInstance().runUpdateChecker(true);
            }
        }.start();

    }

    @Override
    public String createIconKey() {
        return "update";
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getDoUpdateCheckAction();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_start_update_tooltip();
    }

    @Override
    protected void doInit() {
    }

}
