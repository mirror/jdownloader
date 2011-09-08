package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.jdownloader.images.NewTheme;

public class ReconnectRecorderAction extends BasicAction {

    private LiveHeaderReconnect liveHeaderReconnect;

    public ReconnectRecorderAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;
        setName(T._.ReconnectRecorderAction_ReconnectRecorderAction_());
        setSmallIcon(NewTheme.I().getIcon("record", 18));
        setTooltipFactory(new BasicTooltipFactory(getName(), T._.ReconnectRecorderAction_ReconnectRecorderAction_tt(), NewTheme.I().getIcon("record", 32)));
    }

    public void actionPerformed(ActionEvent e) {
        liveHeaderReconnect.routerRecord();
    }

}
