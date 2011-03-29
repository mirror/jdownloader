package jd.plugins;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.plugins.optional.ExtensionGuiEnableAction;
import jd.plugins.optional.PluginOptional;

import org.appwork.utils.swing.EDTRunner;

public abstract class AddonPanel extends ClosableView {

    /**
     * 
     */
    private static final long        serialVersionUID = 1L;
    private boolean                  active           = false;
    private PluginOptional           extension;
    private ExtensionGuiEnableAction action;

    public AddonPanel(PluginOptional plg) {
        extension = plg;

        getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(final SwitchPanelEvent event) {
                if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) {
                    setActive(false);
                }
            }

        });

    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean b) {
        active = b;
        getEnabledAction().setSelected(b);

        if (b) {

            onActivated();
        } else {
            onDeactivated();
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                extension.getStore().setGuiEnabled(b);

                if (b) {
                    SwingGui.getInstance().setContent(AddonPanel.this);

                } else {
                    SwingGui.getInstance().disposeView(AddonPanel.this);

                }
            }
        };

    }

    abstract protected void onDeactivated();

    abstract protected void onActivated();

    public synchronized ExtensionGuiEnableAction getEnabledAction() {
        if (action == null) {
            action = new ExtensionGuiEnableAction(extension);
        }
        return action;
    }

    public boolean isKeepGuiEnabledStatusAcrossSessions() {
        return true;
    }

    /**
     * Restores gui after startup
     */
    public void restore() {
        if (this.isKeepGuiEnabledStatusAcrossSessions() && extension.getStore().isGuiEnabled()) {
            setActive(true);

        }

    }

}
