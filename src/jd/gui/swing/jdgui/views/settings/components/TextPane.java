package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TextPane extends JScrollPane implements SettingsComponent {

    private static final long                serialVersionUID = -5196573922145630308L;
    protected JTextPane                      txt;
    private StateUpdateEventSender<TextPane> eventSender;
    private boolean                          setting;

    public void setText(String t) {
        setting = true;
        try {
            txt.setText(t);
        } finally {
            setting = false;
        }
    }

    public TextPane() {
        this.txt = new JTextPane();
        // this.txt.setLineWrap(true);
        // this.txt.setWrapStyleWord(false);

        this.getViewport().setView(this.txt);
        eventSender = new StateUpdateEventSender<TextPane>();
        this.txt.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<TextPane>(TextPane.this));
                }
            }

            public void insertUpdate(DocumentEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<TextPane>(TextPane.this));
                }
            }

            public void changedUpdate(DocumentEvent e) {
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<TextPane>(TextPane.this));
                }
            }
        });
    }

    public JTextPane getTxt() {
        return txt;
    }

    public String getConstraints() {
        return "wmin 10,height 60:150:n";
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
