package jd.plugins.components;

import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.plugins.PluginConfigPanelNG;

import org.appwork.storage.config.handler.KeyHandler;

public class UsenetConfigPanel<T extends UsenetConfigInterface> extends PluginConfigPanelNG {

    private final T              cf;
    private final UsenetServer[] availableServers;

    public UsenetConfigPanel(final String host, final UsenetServer[] availableServers, final T cf) {
        this.cf = cf;
        addStartDescription("Usenet settings for " + host);
        this.availableServers = availableServers;
        addPair("Select server", null, null, new ComboBox<UsenetServer>(cf._getStorageHandler().getKeyHandler("UsenetServer", KeyHandler.class), availableServers, null) {
            @Override
            public void setSelectedItem(Object anObject) {
                if (anObject == null) {
                    super.setSelectedItem(availableServers[0]);
                } else {
                    super.setSelectedItem(anObject);
                }
            }
        });
    }

    @Override
    public void reset() {
        this.cf.setUsenetServer(availableServers[0]);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

}
