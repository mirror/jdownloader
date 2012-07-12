package jd.gui.swing.jdgui.views.settings.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;

public class ComboBox<ContentType> extends JComboBox implements SettingsComponent, GenericConfigEventListener<ContentType> {

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
        orgRenderer = getRenderer();
        this.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component ret;
                renderComponent(ret = orgRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus), list, (ContentType) value, index, isSelected, cellHasFocus);
                return ret;
            }
        });
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
                Component ret;
                renderComponent(ret = orgRenderer.getListCellRendererComponent(list, translations[index], index, isSelected, cellHasFocus), list, (ContentType) value, index, isSelected, cellHasFocus);
                return ret;
            }
        });
    }

    protected void renderComponent(Component component, JList list, ContentType value, int index, boolean isSelected, boolean cellHasFocus) {

    }

    public ComboBox(org.appwork.storage.config.handler.KeyHandler<ContentType> keyHandler, ContentType[] values, String[] strings) {
        this(values, strings);
        this.keyHandler = keyHandler;
        keyHandler.getEventSender().addListener(this, true);
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

    @Override
    public void onConfigValidatorError(KeyHandler<ContentType> keyHandler, ContentType invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<ContentType> keyHandler, ContentType newValue) {
        setSelectedItem(keyHandler.getValue());
    }
}
