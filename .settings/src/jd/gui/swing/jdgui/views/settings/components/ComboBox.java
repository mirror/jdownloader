package jd.gui.swing.jdgui.views.settings.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.appwork.storage.config.handler.KeyHandler;

public class ComboBox<ContentType> extends JComboBox implements SettingsComponent {

    private static final long                             serialVersionUID = -1580999899097054630L;
    private ListCellRenderer                              orgRenderer;
    private String[]                                      translations;
    private StateUpdateEventSender<ComboBox<ContentType>> eventSender;
    private boolean                                       setting;
    private KeyHandler<ContentType>                       keyHandler;
    {
        eventSender = new StateUpdateEventSender<ComboBox<ContentType>>();
        this.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // do not throw events of changed programmatically
                if (!setting) {
                    eventSender.fireEvent(new StateUpdateEvent<ComboBox<ContentType>>(ComboBox.this));
                    if (keyHandler != null) {
                        keyHandler.setValue((ContentType) getSelectedItem());
                    }
                }
            }
        });
    }

    public ComboBox(ContentType... options) {
        super(options);
        // this.setSelectedIndex(selection);

    }

    @SuppressWarnings("unchecked")
    public ContentType getValue() {
        return (ContentType) getSelectedItem();
    }

    public void setValue(ContentType selected) {
        setting = true;
        try {
            this.setSelectedItem(selected);
        } finally {
            setting = false;
        }
    }

    public ComboBox(ContentType[] values, String[] names) {
        super(values);
        orgRenderer = getRenderer();
        this.translations = names;
        this.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (index == -1) index = getSelectedIndex();
                return orgRenderer.getListCellRendererComponent(list, translations[index], index, isSelected, cellHasFocus);
            }
        });
    }

    public ComboBox(org.appwork.storage.config.handler.KeyHandler<ContentType> keyHandler, ContentType[] values, String[] strings) {
        this(values, strings);
        this.keyHandler = keyHandler;
        setSelectedItem(keyHandler.getValue());

    }

    public String getConstraints() {
        return "height 26!";
    }

    public boolean isMultiline() {
        return false;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);

    }
}
