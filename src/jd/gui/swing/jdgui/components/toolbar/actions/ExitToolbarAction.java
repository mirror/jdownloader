package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class ExitToolbarAction extends AbstractToolBarAction {

    public ExitToolbarAction() {

        setIconKey(IconKey.ICON_EXIT);
        setName(_GUI.T.action_exit());
        setTooltipText(_GUI.T.action_exit_tooltip());
        setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));

    }

    @Override
    public String createTooltip() {
        return _GUI.T.action_exit_tooltip();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest());

    }
}
