package org.jdownloader.extensions.eventscripter;

import java.awt.event.ActionEvent;
import java.io.IOException;

import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.jdownloader.actions.AppAction;

public class ExampleMenuEntry extends AppAction {

    private String                 scriptFile;
    private EventTrigger           trigger;
    private EventScripterExtension extension;
    private String                 description;

    public ExampleMenuEntry(EventScripterExtension eventScripterExtension, String scriptFile, EventTrigger trigger, String description) {
        super();
        setName(T._.ExampleMenuEntry(trigger.getLabel(), description));
        this.description = description;
        this.scriptFile = scriptFile;
        this.trigger = trigger;
        extension = eventScripterExtension;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            ScriptEntry newScript = new ScriptEntry();
            newScript.setEventTrigger(trigger);
            newScript.setName(description);
            newScript.setEnabled(true);

            newScript.setScript(IO.readURLToString(EventScripterExtension.class.getResource("examples/" + scriptFile)));

            extension.addScriptEntry(newScript);
        } catch (IOException e1) {
            UIOManager.I().showException(e1.getMessage(), e1);
        }
    }
}
