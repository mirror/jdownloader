package org.jdownloader.extensions.jdanywhere;

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

    private void initComponents() {

    }

    protected void layoutPanel() {

    }

    @Override
    public void save() {
        // showRestartRequiredMessage();
    }

    @Override
    public void updateContents() {
    }

}
