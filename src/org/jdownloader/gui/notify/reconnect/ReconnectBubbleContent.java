package org.jdownloader.gui.notify.reconnect;

import java.util.ArrayList;

import javax.swing.JLabel;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IPConnectionState;
import jd.controlling.reconnect.ipcheck.IPController;

public class ReconnectBubbleContent extends AbstractBubbleContentPanel {

    private JLabel newIP;

    private long   startTime;

    private JLabel duration;

    public ReconnectBubbleContent() {
        super("auto-reconnect");
        startTime = System.currentTimeMillis();
        // super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

        // , _GUI._.balloon_reconnect_start_msg(), new AbstractIcon(IconKey.ICON_RECONNECT, 32)

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
            add(new JLabel(_GUI._.ReconnectDialog_onIPValidated_(), new AbstractIcon(IconKey.ICON_OK, 20), JLabel.LEFT));

            break;
        case FAILED:
            add(createHeaderLabel(_GUI._.ReconnectBubbleContent_onResult_result()));
            add(new JLabel(_GUI._.ReconnectDialog_failed(), new AbstractIcon(IconKey.ICON_ERROR, 20), JLabel.LEFT));

            break;
        }

    }

    @Override
    public void updateLayout() {
    }

    public static void fill(ArrayList<Element> elements) {
    }

}
