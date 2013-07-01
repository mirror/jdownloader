package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitOrRestartRequest;

public class ExitToolbarAction extends ToolBarAction {

    public ExitToolbarAction(SelectionInfo<?, ?> selection) {

        setIconKey("exit");
        setName(_GUI._.action_exit());

    }

    @Override
    public String createTooltip() {
        return _GUI._.action_exit_tooltip();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RestartController.getInstance().exitAsynch(new SmartRlyExitOrRestartRequest());

    }
}
