package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class Spinner extends JSpinner implements SettingsComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Spinner(int min, int max) {

        super(new SpinnerNumberModel(min, min, max, 1));
        setEditor(new JSpinner.NumberEditor(this, "#"));
    }

    public String getConstraints() {
        return null;
    }

    public boolean isMultiline() {
        return false;
    }

}
