package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.jdownloader.images.NewTheme;

public class EditScriptAction extends AbstractAction {

    private LiveHeaderReconnect liveHeaderReconnect;

    public EditScriptAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;
        putValue(NAME, T._.EditScriptAction_EditScriptAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("edit", 18));
    }

    public void actionPerformed(ActionEvent e) {
        liveHeaderReconnect.editScript();
    }

}
