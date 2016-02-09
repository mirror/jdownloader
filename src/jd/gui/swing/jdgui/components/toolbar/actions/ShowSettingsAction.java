package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

public class ShowSettingsAction extends AbstractToolBarAction {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ShowSettingsAction() {

        setIconKey(IconKey.ICON_SETTINGS);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {

                JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
            }
        };
    }

    @Override
    public String createTooltip() {
        return _GUI.T.action_settings_menu_tooltip();
    }

}
