package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.event.ActionEvent;

import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.tooltips.BasicTooltipFactory;
import org.appwork.utils.StringUtils;
import org.jdownloader.images.NewTheme;

public class EditScriptAction extends BasicAction implements GenericConfigEventListener<String> {

    private LiveHeaderReconnect liveHeaderReconnect;

    public EditScriptAction(LiveHeaderReconnect liveHeaderReconnect) {
        this.liveHeaderReconnect = liveHeaderReconnect;

        putValue(SMALL_ICON, NewTheme.I().getIcon("edit", 18));

        setTooltipFactory(new BasicTooltipFactory(getName(), T._.EditScriptAction_EditScriptAction_tt(), NewTheme.I().getIcon("edit", 32)));
        CFG_LIVEHEADER.SCRIPT.getEventSender().addListener(this, true);
        updateName();
    }

    private void updateName() {
        String script = CFG_LIVEHEADER.SCRIPT.getValue();
        // if (script != null) {
        // script = script.replaceAll("\\[\\[\\[", "<");
        // script = script.replaceAll("\\]\\]\\]", ">");
        // script = script.replaceAll("<REQUEST(.*?)>", "<REQUEST$1><![CDATA[");
        // script = script.replaceAll("</REQUEST>", "]]></REQUEST>");
        // script = script.replaceAll("<RESPONSE(.*?)>", "<RESPONSE$1><![CDATA[");
        // script = script.replaceAll("</RESPONSE.*>", "]]></RESPONSE>");
        // }
        //
        // Document xml = JDUtilities.parseXmlString(script, false);

        if (StringUtils.isEmpty(script)) {
            putValue(NAME, T._.EditScriptAction_EditScriptAction_add());
        } else {
            putValue(NAME, T._.EditScriptAction_EditScriptAction_());
        }
    }

    public void actionPerformed(ActionEvent e) {
        liveHeaderReconnect.editScript(true);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
        updateName();
    }

}
