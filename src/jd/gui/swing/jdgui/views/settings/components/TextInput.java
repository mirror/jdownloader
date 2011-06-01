package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TextInput extends JTextField implements SettingsComponent {

    /**
     * 
     */
    private static final long                 serialVersionUID = 1L;
    private StateUpdateEventSender<TextInput> eventSender;
    private boolean                           setting;
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
        super(nick);

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
