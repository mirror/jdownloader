package jd.gui.swing.jdgui.views.settings.panels.advanced;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.Theme;
import org.jdownloader.translate._JDT;

public class AdvancedSettings extends AbstractConfigPanel {

    public String getTitle() {
        return _GUI._.gui_settings_advanced_title();
    }

    public AdvancedSettings() {
        super();
        this.addHeader(getTitle(), Theme.getIcon("advancedConfig", 32));
        this.addDescription(_JDT._.gui_settings_advanced_description());
        add(new JScrollPane(new AdvancedTable()));
    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("advancedConfig", 32);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {
        Dialog.getInstance().showMessageDialog(_JDT._.gui_settings_advanced_description());
    }
}