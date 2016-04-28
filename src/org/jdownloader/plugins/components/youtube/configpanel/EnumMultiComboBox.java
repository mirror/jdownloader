package org.jdownloader.plugins.components.youtube.configpanel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;

import jd.gui.swing.jdgui.views.settings.components.MultiComboBox;

public class EnumMultiComboBox<T> extends MultiComboBox<T> implements GenericConfigEventListener<Object> {
    private static int           LABEL_HEIGHT = new JLabel("Test").getPreferredSize().height;
    protected KeyHandler<Object> keyHandler;
    private boolean              setting      = false;
    private boolean              shrinkedMode = false;
    private boolean              inverted     = false;

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    protected String getLabel(List<T> list) {
        return "[" + StringUtils.fillPre(list.size() + "", "0", 2) + "/" + StringUtils.fillPre(getValues().size() + "", "0", 2) + "] " + super.getLabel(list);
    }

    public EnumMultiComboBox(List<T> bitrates, ObjectKeyHandler keyHandler, boolean inverted) {
        super(bitrates);
        this.setInverted(inverted);
        this.keyHandler = keyHandler;
        if (keyHandler != null) {
            keyHandler.getEventSender().addListener(this, true);
            onConfigValueModified(null, null);
        }
    }

    @Override
    public String getConstraints() {
        if (!shrinkedMode) {
            return null;
        }
        int h = LABEL_HEIGHT + 4;
        return "height " + h + "!";
    }

    @Override
    public void onChanged() {
        super.onChanged();
        if (keyHandler != null) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (!setting) {
                        setting = true;
                        try {
                            saveValuesToKeyHandler();
                        } finally {
                            setting = false;
                        }
                    }
                }

            };
        }

    }

    protected void loadValuesFromKeyHandler() {
        if (isInverted()) {
            ArrayList<T> selected = new ArrayList<T>(getValues());
            List<T> blacklisted = (List<T>) EnumMultiComboBox.this.keyHandler.getValue();
            if (blacklisted != null) {
                selected.removeAll(blacklisted);
            }
            setSelectedItems(selected);
        } else {
            List<T> blacklisted = (List<T>) EnumMultiComboBox.this.keyHandler.getValue();

            setSelectedItems(blacklisted);
        }
    }

    protected void saveValuesToKeyHandler() {
        List<T> selected = getSelectedItems();
        if (isInverted()) {
            ArrayList<T> all = new ArrayList<T>(getValues());
            all.removeAll(selected);
            keyHandler.setValue(all);
        } else {
            keyHandler.setValue(selected);
        }

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
                        loadValuesFromKeyHandler();
                    } finally {
                        setting = false;
                    }
                }
            }

        };

    }

    public void setShrinkedMode(boolean b) {
        this.shrinkedMode = b;
    }

}
