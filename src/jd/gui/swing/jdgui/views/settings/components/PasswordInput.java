package jd.gui.swing.jdgui.views.settings.components;

import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.swing.components.ExtPasswordField;

public class PasswordInput extends ExtPasswordField implements SettingsComponent, GenericConfigEventListener<String> {
    private StringKeyHandler keyhandler;

    public PasswordInput(StringKeyHandler keyhandler) {
        super();
        this.keyhandler = keyhandler;
        setText(keyhandler.getValue());
        keyhandler.getEventSender().addListener(this, true);

    }

    private AtomicInteger setting = new AtomicInteger(0);

    @Override
    public void onChanged() {
        super.onChanged();
        setting.incrementAndGet();
        try {
            keyhandler.setValue(getText());
        } finally {
            setting.decrementAndGet();
        }
    }

    @Override
    public String getConstraints() {
        return null;
    }

    @Override
    public boolean isMultiline() {
        return false;
    }

    @Override
    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
        if (setting.get() > 0) {
            return;
        }
        setText(keyhandler.getValue());
    }

}
