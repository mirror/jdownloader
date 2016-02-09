package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.event.ActionEvent;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

public class ReconnectRecorderAction extends BasicAction {

    private LiveHeaderReconnect liveHeaderReconnect;

    public ReconnectRecorderAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;
        setName(T.T.ReconnectRecorderAction_ReconnectRecorderAction_());
        setSmallIcon(new AbstractIcon(IconKey.ICON_RECORD, 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T.T.ReconnectRecorderAction_ReconnectRecorderAction_tt(), new AbstractIcon(IconKey.ICON_RECORD, 32)));
    }

    public void actionPerformed(ActionEvent e) {
        liveHeaderReconnect.routerRecord();
    }

}
