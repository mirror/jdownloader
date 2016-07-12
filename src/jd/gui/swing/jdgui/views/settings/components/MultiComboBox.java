package jd.gui.swing.jdgui.views.settings.components;

import java.util.List;

import org.jdownloader.gui.views.components.PseudoMultiCombo;

public class MultiComboBox<ContentType> extends PseudoMultiCombo<ContentType> implements SettingsComponent {
    private static final long serialVersionUID = -1580999899097054630L;

    public MultiComboBox(List<ContentType> options) {
        super(options);
    }

    public MultiComboBox(ContentType... options) {
        super(options);
    }

    public String getConstraints() {
        return "sgy LINE";
    }

    public boolean isMultiline() {
        return false;
    }
}
