package org.jdownloader.gui.notify.reconnect;

import java.util.ArrayList;

import javax.swing.JLabel;

import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IPConnectionState;
import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ReconnectBubbleContent extends AbstractBubbleContentPanel {

    private JLabel newIP;

    private long   startTime;

    private JLabel duration;

    public ReconnectBubbleContent() {
        super("auto-reconnect");
        startTime = System.currentTimeMillis();
        // super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

        // , _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32)

        add(createHeaderLabel((_GUI._.ReconnectDialog_layoutDialogContent_duration())));
        add(duration = new JLabel(""));
        add(createHeaderLabel((_GUI._.ReconnectDialog_layoutDialogContent_old())));
        add(new JLabel(IPController.getInstance().getIpState().toString()));
        add(createHeaderLabel((_GUI._.ReconnectDialog_layoutDialogContent_currentip())));
        add(newIP = new JLabel(""));
        progressCircle.setIndeterminate(true);
        progressCircle.setValue(0);

    }

    public void update() {
        IPConnectionState ip = IPController.getInstance().getIpState();
        newIP.setText(ip.toString());
        duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
    }

    public void onResult(ReconnectResult result) {
        switch (result) {
        case SUCCESSFUL:
            add(createHeaderLabel(_GUI._.ReconnectBubbleContent_onResult_result()));
            add(new JLabel(_GUI._.ReconnectDialog_onIPValidated_(), NewTheme.I().getIcon("ok", 20), JLabel.LEFT));

            break;
        case FAILED:
            add(createHeaderLabel(_GUI._.ReconnectBubbleContent_onResult_result()));
            add(new JLabel(_GUI._.ReconnectDialog_failed(), NewTheme.I().getIcon("error", 20), JLabel.LEFT));

            break;
        }

    }

    @Override
    public void updateLayout() {
    }

    public static void fill(ArrayList<Element> elements) {
    }

}
