package jd.gui.swing.jdgui.views.settings.components;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.swing.components.ExtTextField;

public class TextInput extends ExtTextField implements SettingsComponent, GenericConfigEventListener<String> {

    /**
     * 
     */
    private static final long                 serialVersionUID = 1L;
    private StateUpdateEventSender<TextInput> eventSender;
    private AtomicBoolean                     settings         = new AtomicBoolean(false);
    private StringKeyHandler                  keyhandler;
    {
        eventSender = new StateUpdateEventSender<TextInput>();
        this.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                if (!settings.get()) eventSender.fireEvent(new StateUpdateEvent<TextInput>(TextInput.this));
            }

            public void insertUpdate(DocumentEvent e) {
                if (!settings.get()) eventSender.fireEvent(new StateUpdateEvent<TextInput>(TextInput.this));
            }

            public void changedUpdate(DocumentEvent e) {
                if (!settings.get()) eventSender.fireEvent(new StateUpdateEvent<TextInput>(TextInput.this));
            }
        });
    }

    public TextInput(String nick) {
        super();
        setText(nick);

    }

    @Override
    public void setText(String t) {
        if (!settings.compareAndSet(false, true)) return;
        try {
            super.setText(t);
        } finally {
            settings.set(false);
        }
    }

    public TextInput() {
        super();
    }

    @Override
    public void onChanged() {
        super.onChanged();
        if (!settings.compareAndSet(false, true)) { return; }
        try {
            if (keyhandler != null) keyhandler.setValue(getText());
        } finally {
            settings.set(false);
        }

    }

    public TextInput(StringKeyHandler keyhandler) {
        super();
        this.keyhandler = keyhandler;
        setText(keyhandler.getValue());
        keyhandler.getEventSender().addListener(this, true);
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

    @Override
    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
        if (!settings.get()) {
            setText(keyhandler.getValue());
        }
    }

}
