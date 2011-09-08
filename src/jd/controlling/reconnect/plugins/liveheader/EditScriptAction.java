package jd.controlling.reconnect.plugins.liveheader;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.plugins.liveheader.translate.T;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.jdownloader.images.NewTheme;

public class EditScriptAction extends BasicAction {

    private LiveHeaderReconnect liveHeaderReconnect;

    public EditScriptAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;
        putValue(NAME, T._.EditScriptAction_EditScriptAction_());
        putValue(SMALL_ICON, NewTheme.I().getIcon("edit", 18));

        setTooltipFactory(new BasicTooltipFactory(getName(), T._.EditScriptAction_EditScriptAction_tt(), NewTheme.I().getIcon("edit", 32)));

    }

    public void actionPerformed(ActionEvent e) {
        liveHeaderReconnect.editScript();
    }

}
