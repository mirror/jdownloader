package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.jdownloader.images.NewTheme;

public class ReconnectRecorderAction extends AbstractAction {

    private LiveHeaderReconnect liveHeaderReconnect;

    public ReconnectRecorderAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;
        putValue(NAME, T._.ReconnectRecorderAction_ReconnectRecorderAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("record", 18));
    }

    public void actionPerformed(ActionEvent e) {
        liveHeaderReconnect.routerRecord();
    }

}
