package org.jdownloader.extensions.neembuu;

import javax.swing.ImageIcon;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.neembuu.gui.NeembuuGui;
import org.jdownloader.extensions.neembuu.translate._NT;
import org.jdownloader.images.NewTheme;

public class NeembuuExtension extends AbstractExtension<NeembuuConfig> {

    private NeembuuGui tab;

    public NeembuuExtension() {
        super(_NT._.title());

        tab = new NeembuuGui(this);
    }

    /**
     * Action "onStop". Is called each time the user disables the extension
     */
    @Override
    protected void stop() throws StopException {
        Log.L.finer("Stopped " + getClass().getSimpleName());
    }

    /**
     * Actions "onStart". is called each time the user enables the extension
     */
    @Override
    protected void start() throws StartException {
        Log.L.finer("Started " + getClass().getSimpleName());
    }

    /**
     * Has to return the Extension MAIN Icon. This icon will be used,for
     * example, in the settings pane
     */
    @Override
    public ImageIcon getIcon(int size) {
        return NewTheme.I().getIcon("ok", size);
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    /**
     * gets called once as soon as the extension is loaded.
     */
    @Override
    protected void initExtension() throws StartException {
    }

    /**
     * Returns the Settingspanel for this extension. If this extension does not
     * have a configpanel, null can be returned
     */
    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return null;
    }

    /**
     * Should return false of this extension has no configpanel
     */
    @Override
    public boolean hasConfigPanel() {
        return false;
    }

    /**
     * DO NOT USE THIS FUNCTION. it is only used for compatibility reasons
     */
    @Override
    @Deprecated
    public String getConfigID() {
        return null;
    }

    @Override
    public String getAuthor() {
        return "Shashaank";
    }

    @Override
    public String getDescription() {
        return _NT._.description();
    }

    /**
     * Returns the gui
     */
    @Override
    public NeembuuGui getGUI() {
        return tab;
    }

}
