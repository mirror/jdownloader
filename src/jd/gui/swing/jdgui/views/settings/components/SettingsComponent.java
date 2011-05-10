package jd.gui.swing.jdgui.views.settings.components;

public interface SettingsComponent {

    String getConstraints();

    boolean isMultiline();

    void setEnabled(boolean b);

    boolean isEnabled();

    public void setToolTipText(String text);
}
