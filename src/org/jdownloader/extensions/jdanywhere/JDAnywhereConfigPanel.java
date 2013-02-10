package org.jdownloader.extensions.jdanywhere;

import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;

public class JDAnywhereConfigPanel extends ExtensionConfigPanel<JDAnywhereExtension> {

    private static final long serialVersionUID = 1L;

    public JDAnywhereConfigPanel(JDAnywhereExtension plg, JDAnywhereConfig config) {
        super(plg);
        this.config = config;
        initComponents();
        layoutPanel();
    }

    private final JDAnywhereConfig config;

    private TextInput              username;
    private PasswordInput          password;
    private Spinner                port;

    private void initComponents() {
        username = new TextInput(config.getStorageHandler().getKeyHandler("Username", StringKeyHandler.class));
        password = new PasswordInput(config.getStorageHandler().getKeyHandler("Password", StringKeyHandler.class));
        port = new Spinner(config.getStorageHandler().getKeyHandler("Port", IntegerKeyHandler.class));
    }

    protected void layoutPanel() {
        addPair("Username:", null, username);
        addPair("Password:", null, password);
        addPair("Port:", null, port);
    }

    @Override
    public void save() {
        showRestartRequiredMessage();
    }

    @Override
    public void updateContents() {
    }

}
