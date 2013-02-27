package jd.gui.swing.jdgui.views.settings.components;

import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;

public class SettingsButton extends ExtButton implements SettingsComponent {

    public SettingsButton(AppAction appAction) {
        super(appAction);
    }

    @Override
    public String getConstraints() {
        return "height 26!";
    }

    @Override
    public boolean isMultiline() {
        return false;
    }

}
