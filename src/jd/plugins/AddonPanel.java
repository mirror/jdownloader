package jd.plugins;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.views.ClosableView;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionGuiEnableAction;

/**
 * Abstract Superclass which should be used for all Extension Gui Panels
 * 
 * @author thomas
 * 
 */
public abstract class AddonPanel<T extends AbstractExtension<? extends ExtensionConfigInterface>> extends ClosableView {

    /**
     * 
     */
    private static final long        serialVersionUID = 1L;
    private boolean                  active           = false;
    private T                        extension;
    private ExtensionGuiEnableAction action;

    public AddonPanel(T plg) {
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

    public T getExtension() {
        return extension;
    }

    /**
     * If gui is active and available in the main tabbed pane
     * 
     * @return
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Enables the gui and adds it top the main tabbed pane. use
     * {@link #toFront()} to make the gui the currently selected Tab afterwards
     * 
     * @param b
     */
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

                extension.getSettings().setGuiEnabled(b);

                if (b) {
                    SwingGui.getInstance().setContent(AddonPanel.this, false);

                } else {
                    SwingGui.getInstance().disposeView(AddonPanel.this);

                }
            }
        };

    }

    /**
     * is called if the gui gets closed
     */
    abstract protected void onDeactivated();

    /**
     * is called as soon as the gui gets activated and visible in the main
     * tabbed pane
     */
    abstract protected void onActivated();

    /**
     * returns the action which can be used as toggleaction to show/hide the gui
     * 
     * @return
     */
    public synchronized ExtensionGuiEnableAction getEnabledAction() {
        if (action == null) {
            action = new ExtensionGuiEnableAction(extension);
        }
        return action;
    }

    /**
     * if this returns true, the guis visibility is stored across sessions. This
     * means that the pannel will be reactivated after JDownloader restart
     * 
     * @return
     */
    public boolean isKeepGuiEnabledStatusAcrossSessions() {
        return true;
    }

    /**
     * Restores gui after startup
     */
    public void restore() {
        if (this.isKeepGuiEnabledStatusAcrossSessions() && extension.getSettings().isGuiEnabled()) {
            setActive(true);

        }

    }

    /**
     * Sets the gui active. The panel will become the active tab in the main
     * tabbed pane
     */
    public void toFront() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                SwingGui.getInstance().setContent(AddonPanel.this, true);

            }
        };
    }

}
