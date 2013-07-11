package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTable;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class QuickSettingsHeader extends MigPanel implements GenericConfigEventListener<Boolean>, HeaderInterface {

    private JLabel    lbl;

    private ExtButton btn;

    private ExtButton config;

    public QuickSettingsHeader() {
        super("ins 0", "2[][grow,fill][]8[]8", "[]");
        setOpaque(false);
        setBackground(null);
        // add(new JSeparator(), "gapleft 15");

        config = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("exttable/columnButton", 14));
            }

            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                SwingGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                // LinkgrabberFilter.getInstance().setSelectedIndex(1);
                // ConfigurationView.getInstance().setSelectedSubPanel(jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.Linkgrabber.class);
            }
        });
        config.setRolloverEffectEnabled(true);
        add(config, "height 16!,width 16!");

        lbl = SwingUtils.toBold(new JLabel(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_settings()));

        // lbl.setForeground(new
        // Color(LAFOptions.getInstance().getPanelHeaderLineColor()));
        add(Box.createGlue());
        add(lbl);

        btn = new ExtButton(new BasicAction() {

            public void actionPerformed(ActionEvent e) {
                boolean nv = !org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getValue();
                org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_QUICK_SETTINGS_VISIBLE.setValue(nv);
            }

        });

        add(btn, "height 14!,width 14!");
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JTable().getGridColor()));
        org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getEventSender().addListener(this);
        onConfigValueModified(null, null);
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getValue()) {
            btn.setIcon(NewTheme.I().getIcon("popdownButton", -1));
        } else {
            btn.setIcon(NewTheme.I().getIcon("popupButton", -1));
        }
    }

    public void setFilterCount(int i) {
    }
}
