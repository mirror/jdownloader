package jd.gui.swing.jdgui.views.settings.components;

import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.swing.components.ExtPasswordField;

public class PasswordInput extends ExtPasswordField implements SettingsComponent {
    private StringKeyHandler keyhandler;

    public PasswordInput(StringKeyHandler keyhandler) {
        super();
        this.keyhandler = keyhandler;
        setText(keyhandler.getValue());

    }

    @Override
    public void onChanged() {
        super.onChanged();
        keyhandler.setValue(getText());
    }

    @Override
    public String getConstraints() {
        return null;
    }

    @Override
    public boolean isMultiline() {
        return false;
    }

}
