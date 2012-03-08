package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.swing.components.ExtTextField;

public class TextInput extends ExtTextField implements SettingsComponent {

    /**
     * 
     */
    private static final long                 serialVersionUID = 1L;
    private StateUpdateEventSender<TextInput> eventSender;
    private boolean                           setting;
    private StringKeyHandler                  keyhandler;
    {
        eventSender = new StateUpdateEventSender<TextInput>();
        this.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<TextInput>(TextInput.this));
            }

            public void insertUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<TextInput>(TextInput.this));
            }

            public void changedUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<TextInput>(TextInput.this));
            }
        });
    }

    public TextInput(String nick) {
        super();
        setText(nick);

    }

    @Override
    public void setText(String t) {
        setting = true;
        try {
            super.setText(t);
        } finally {
            setting = false;
        }
    }

    public TextInput() {
        super();
    }

    @Override
    public void onChanged() {
        super.onChanged();
        keyhandler.setValue(getText());
    }

    public TextInput(StringKeyHandler keyhandler) {
        super();
        this.keyhandler = keyhandler;
        setText(keyhandler.getValue());

    }

    public String getConstraints() {
        return null;
    }

    public boolean isMultiline() {
        return false;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }

}
