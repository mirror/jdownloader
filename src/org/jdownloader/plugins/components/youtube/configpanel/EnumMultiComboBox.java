package org.jdownloader.plugins.components.youtube.configpanel;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;

import jd.gui.swing.jdgui.views.settings.components.MultiComboBox;

public class EnumMultiComboBox<T> extends MultiComboBox<T> implements GenericConfigEventListener<Object> {

    private KeyHandler<Object> keyHandler;
    private boolean            setting = false;

    @Override
    protected String getLabel(List<T> list) {
        return "[" + StringUtils.fillPre(list.size() + "", "0", 2) + "/" + StringUtils.fillPre(getValues().size() + "", "0", 2) + "] " + super.getLabel(list);
    }

    public EnumMultiComboBox(List<T> bitrates, ObjectKeyHandler keyHandler) {
        super(bitrates);
        this.keyHandler = keyHandler;
        keyHandler.getEventSender().addListener(this, true);
        onConfigValueModified(null, null);
    }

    @Override
    public void onChanged() {
        super.onChanged();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!setting) {
                    setting = true;
                    try {
                        List<T> selected = getSelectedItems();
                        ArrayList<T> all = new ArrayList<T>(getValues());
                        all.removeAll(selected);
                        keyHandler.setValue(all);
                    } finally {
                        setting = false;
                    }
                }
            }
        };

    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!setting) {
                    setting = true;
                    try {
                        ArrayList<T> selected = new ArrayList<T>(getValues());
                        List<T> blacklisted = (List<T>) EnumMultiComboBox.this.keyHandler.getValue();
                        if (blacklisted != null) {
                            selected.removeAll(blacklisted);
                        }
                        setSelectedItems(selected);
                    } finally {
                        setting = false;
                    }
                }
            }
        };

    }
}
