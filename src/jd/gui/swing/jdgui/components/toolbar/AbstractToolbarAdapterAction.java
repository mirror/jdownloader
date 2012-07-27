package jd.gui.swing.jdgui.components.toolbar;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;
import jd.gui.swing.jdgui.menu.actions.ActionAdapter;

@Deprecated
public class AbstractToolbarAdapterAction extends AbstractToolbarAction {

    private ActionAdapter delegate;

    public AbstractToolbarAdapterAction(ActionAdapter settingsAction) {
        super();
        delegate = settingsAction;
        setIconKey(createIconKey());
        setIconSizes(32);

    }

    public void actionPerformed(ActionEvent e) {
        delegate.actionPerformed(e);

    }

    @Override
    public String createIconKey() {
        if (delegate == null) return null;
        return delegate.getIconKey();
    }

    @Override
    protected String createAccelerator() {
        return delegate.createAccelerator();
    }

    @Override
    protected String createTooltip() {
        return delegate.createTooltip();
    }

    @Override
    protected void doInit() {
        delegate.initDefaults();
    }

}
