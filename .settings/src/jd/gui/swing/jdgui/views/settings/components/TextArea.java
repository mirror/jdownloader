package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TextArea extends JScrollPane implements SettingsComponent {

    private static final long                serialVersionUID = -5196573922145630308L;
    private JTextArea                        txt;
    private StateUpdateEventSender<TextArea> eventSender;
    private boolean                          setting;

    public void setText(String t) {
        setting = true;
        try {
            txt.setText(t);
        } finally {
            setting = false;
        }
    }

    public TextArea() {
        this.txt = new JTextArea();
        this.txt.setLineWrap(true);
        this.txt.setWrapStyleWord(false);

        this.getViewport().setView(this.txt);
        eventSender = new StateUpdateEventSender<TextArea>();
        this.txt.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<TextArea>(TextArea.this));
            }

            public void insertUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<TextArea>(TextArea.this));
            }

            public void changedUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<TextArea>(TextArea.this));
            }
        });
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n";
    }

    public String getText() {
        return txt.getText();
    }

    public boolean isMultiline() {
        return true;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }

}
