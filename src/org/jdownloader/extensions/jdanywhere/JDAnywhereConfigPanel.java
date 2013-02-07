package org.jdownloader.extensions.jdanywhere;

import jd.gui.swing.jdgui.views.settings.components.TextInput;

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
    private TextInput              password;

    private void initComponents() {
        username = new TextInput();
        password = new TextInput();
    }

    protected void layoutPanel() {
        addPair("Username:", null, username);
        addPair("Password:", null, password);
    }

    @Override
    public void save() {
        if (!username.getText().equals(config.getUsername())) {
            config.setUsername(username.getText());
        }
        if (!password.getText().equals(config.getPassword())) {
            config.setPassword(password.getText());
        }
    }

    @Override
    public void updateContents() {
        username.setText(config.getUsername());
        password.setText(config.getPassword());
    }

}
