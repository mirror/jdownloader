package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

import jd.gui.swing.jdgui.MainTabbedPane;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.event.AppActionListener;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;

public abstract class AbstractMoveAction extends AppAction implements AppActionListener, GUIListener {

    protected AppAction delegate;

    public AbstractMoveAction() {
        GUIEventSender.getInstance().addListener(this, true);
        onGuiMainTabSwitch(null, MainTabbedPane.getInstance().getSelectedView());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        delegate.actionPerformed(e);
    }

    protected void setDelegateAction(AppAction moveTopAction) {
        if (delegate != null) {
            delegate.getEventSender().removeListener(this);
        }
        this.delegate = moveTopAction;
        if (delegate != null) {

            setTooltipText(delegate.getTooltipText());
            delegate.getEventSender().addListener(this, true);

            setEnabled(delegate.isEnabled());
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void onActionPropertyChanged(PropertyChangeEvent parameter) {
        if ("enabled".equals(parameter.getPropertyName())) {
            setEnabled((Boolean) parameter.getNewValue());
        }
    }

}
