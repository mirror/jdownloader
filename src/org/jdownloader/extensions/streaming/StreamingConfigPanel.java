package org.jdownloader.extensions.streaming;

import javax.swing.JTextArea;

import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionConfigPanel;

public class StreamingConfigPanel extends ExtensionConfigPanel<StreamingExtension> {

    public StreamingConfigPanel(StreamingExtension plg, StreamingConfig config) {
        super(plg);
        this.config = config;
        initComponents();
        layoutPanel();
    }

    private static final long        serialVersionUID = 1L;

    private final StreamingConfig config;

    private TextInput                vlcPath;
    private TextInput                brokerTopic;

    private JTextArea                revision         = null;
    private String                   vlcRevision      = null;

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
        updateVLCRevision();
    }

    private void updateVLCRevision() {
        if (vlcRevision == null && revision != null) {
            new Thread() {

                @Override
                public void run() {
                    vlcRevision = getExtension().getVLCRevision(getExtension().getVLCBinary());
                    if (StringUtils.isEmpty(vlcRevision)) {
                        vlcRevision = "unknown";
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            revision.setText("VLC Revision: " + vlcRevision);
                        }
                    };
                }

            }.start();
        }
    }

    @Override
    public void updateContents() {
        vlcPath.setText(config.getVLCCommand());
    }
}
