package jd.gui.swing.jdgui.views.settings;

import java.awt.Component;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.GuiConfig;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.sidebar.ConfigSidebar;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.swing.EDTRunner;

public class ConfigurationPanel extends SwitchPanel implements ListSelectionListener {

    private static final long serialVersionUID = -6554600142198250742L;
    private ConfigSidebar     sidebar;
    private SwitchPanel       panel;
    private GuiConfig         cfg;

    public ConfigurationPanel() {
        super(new MigLayout("ins 0", "[200!,grow,fill][grow,fill]", "[grow,fill]"));
        sidebar = new ConfigSidebar();

        add(sidebar, "");
        cfg = JsonConfig.create(GuiConfig.class);
        // add(viewport);
        sidebar.addListener(this);
        Class<?> selected = null;
        try {
            selected = Class.forName(cfg.getActiveConfigPanel());
        } catch (Throwable e) {

        }
        if (selected != null) {
            sidebar.setSelectedTreeEntry(selected);
        } else {
            sidebar.setSelectedTreeEntry(ConfigPanelGeneral.class);
        }
        // int c =
        // LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        // if (c >= 0) {
        // setBackground(new Color(c));
        // super.setOpaque(true);
        // putClientProperty("Synthetica.opaque", Boolean.TRUE);
        // }
    }

    public void setOpaque(boolean isOpaque) {
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    public void valueChanged(ListSelectionEvent e) {
        new Thread() {
            public void run() {
                final SwitchPanel p = sidebar.getSelectedPanel();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setContent(p);
                    }
                };
            }
        }.start();

        // invalidate();
    }

    private void setContent(SwitchPanel selectedPanel) {
        if (selectedPanel == null) return;
        if (panel != null) {
            panel.setHidden();
            panel.setVisible(false);

        }
        boolean found = false;
        for (final Component c : getComponents()) {
            // c.setVisible(false);
            if (c == selectedPanel) {
                found = true;
                break;
            }
        }
        cfg.setActiveConfigPanel(selectedPanel.getClass().getName());
        if (!found) {
            add(selectedPanel, "hidemode 3");
        } else {
            selectedPanel.setVisible(true);
        }
        panel = selectedPanel;
        panel.setShown();
    }

}
