package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.updatev2.UpdateController;

public class UpdateAction extends ToolBarAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public UpdateAction(SelectionInfo<?, ?> selection) {

        setIconKey("update");
        setEnabled(true);
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
    protected String createTooltip() {
        return _GUI._.action_start_update_tooltip();
    }

}
