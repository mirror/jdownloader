package org.jdownloader.extensions.captchapush;

import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.settings.TextInput;

public class CaptchaPushConfigPanel extends ExtensionConfigPanel<CaptchaPushExtension> {

    private static final long       serialVersionUID = 1L;

    private final CaptchaPushConfig config;

    private TextInput               brokerHost;
    private TextInput               brokerTopic;

    public CaptchaPushConfigPanel(CaptchaPushExtension extension, CaptchaPushConfig config) {
        super(extension);
        this.config = config;
        initComponents();
        layoutPanel();
    }

    private void initComponents() {
        brokerHost = new TextInput();
        brokerTopic = new TextInput();
    }

    @Override
    protected void onShow() {
        brokerHost.setText(config.getBrokerHost());
        brokerTopic.setText(config.getBrokerTopic());
    }

    @Override
    protected void onHide() {
        boolean changes = false;
        if (!brokerHost.getText().equals(config.getBrokerHost())) {
            config.setBrokerHost(brokerHost.getText());
            changes = true;
        }
        if (!brokerTopic.getText().equals(config.getBrokerTopic())) {
            config.setBrokerTopic(brokerTopic.getText());
            changes = true;
        }

        if (changes) showRestartRequiredMessage();
    }

    protected void layoutPanel() {
        addPair("Host of the Broker:", brokerHost);
        addPair("Topic of the Broker:", brokerTopic);
    }

}