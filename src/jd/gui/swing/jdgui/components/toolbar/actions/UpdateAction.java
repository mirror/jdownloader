package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import jd.utils.WebUpdate;

import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;

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

    }

    public boolean isDefaultVisible() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        /* WebUpdate is running in its own Thread */
        WebUpdate.doUpdateCheck(true);
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
