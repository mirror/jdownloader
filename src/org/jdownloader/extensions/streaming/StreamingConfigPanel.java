package org.jdownloader.extensions.streaming;

import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.ExtensionConfigPanel;

public class StreamingConfigPanel extends ExtensionConfigPanel<StreamingExtension> {

    public StreamingConfigPanel(StreamingExtension plg, StreamingConfig config) {
        super(plg);
        this.config = config;
        initComponents();
        layoutPanel();
    }

    private static final long     serialVersionUID = 1L;

    private final StreamingConfig config;

    private TextInput             vlcPath;
    private TextInput             brokerTopic;

    private JLabel                revision         = null;
    private String                vlcRevision      = null;

    private void initComponents() {
        vlcPath = new TextInput();
    }

    protected void layoutPanel() {
        if (!CrossSystem.isWindows()) {
            revision = addDescription("VLC Revision: unknown");
        }
        addPair("Customized path to VLC:", null, vlcPath);

    }

    @Override
    public void save() {
        if (!vlcPath.getText().equals(config.getVLCCommand())) {
            config.setVLCCommand(vlcPath.getText());
            vlcRevision = null;
        }
    }

    @Override
    protected void onShow() {
        super.onShow();

    }

    @Override
    public void updateContents() {
        vlcPath.setText(config.getVLCCommand());
    }
}
