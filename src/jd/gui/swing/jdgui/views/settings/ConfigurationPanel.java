package jd.gui.swing.jdgui.views.settings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.sidebar.ConfigSidebar;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class ConfigurationPanel extends SwitchPanel implements ListSelectionListener {

    private static final long              serialVersionUID = -6554600142198250742L;
    private ConfigSidebar                  sidebar;
    private SwitchPanel                    panel;
    private GraphicalUserInterfaceSettings cfg;
    private MigPanel                       right;

    public ConfigurationPanel() {
        super(new MigLayout("ins 0", "[][grow,fill]", "[grow,fill]"));
        sidebar = new ConfigSidebar();
        right = new RightPanel();

        add(sidebar, "");
        JScrollPane sp;
        add(sp = new JScrollPane(right));
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cfg = JsonConfig.create(GraphicalUserInterfaceSettings.class);
        // add(viewport);
        sidebar.addListener(this);
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        if (c >= 0) {
            setBackground(new Color(c).brighter());
            super.setOpaque(true);
        }
    }

    public void setOpaque(boolean isOpaque) {
    }

    @Override
    protected void onShow() {
        if (sidebar.treeInitiated() == false) SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                sidebar.updateAddons();
            }
        });
        new EDTRunner() {
            protected void runInEDT() {
                if (sidebar.getSelectedPanel() == null) {
                    sidebar.getTreeCompleteState().executeWhenReached(new Runnable() {

                        @Override
                        public void run() {
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    if (sidebar.getSelectedPanel() == null) {
                                        restoreSelection(true);
                                    }
                                }
                            };
                        }
                    });
                }
            };
        };

    }

    public void restoreSelection(final boolean onlyOnEmptySelection) {
        Class<?> selected = ConfigPanelGeneral.class;
        try {
            String panelClass = cfg.getActiveConfigPanel();
            selected = Class.forName(panelClass);
        } catch (Throwable e) {

        }
        final Class<?> finalSelected = selected;
        if (finalSelected != null) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    if (onlyOnEmptySelection && sidebar.getSelectedPanel() != null) return;
                    sidebar.setSelectedTreeEntry(finalSelected);
                }
            };
        }
    }

    @Override
    protected void onHide() {

    }

    public void valueChanged(ListSelectionEvent e) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                final SwitchPanel p = sidebar.getSelectedPanel();
                if (p == null) return;
                setContent(p);
            }
        };
        // invalidate();
    }

    @SuppressWarnings("rawtypes")
    private void setContent(SwitchPanel selectedPanel) {
        if (selectedPanel == null || selectedPanel == panel) return;
        if (panel != null) {
            panel.setHidden();
            panel.setVisible(false);

        }
        boolean found = false;
        for (final Component c : right.getComponents()) {
            // c.setVisible(false);
            if (c == selectedPanel) {
                found = true;
                break;
            }
        }
        if (selectedPanel instanceof ExtensionConfigPanel) {
            cfg.setActiveConfigPanel(((ExtensionConfigPanel) selectedPanel).getExtension().getClass().getName());
        } else {
            cfg.setActiveConfigPanel(selectedPanel.getClass().getName());
        }

        if (!found) {
            right.add(selectedPanel, "hidemode 3");
        } else {
            selectedPanel.setVisible(true);
        }
        panel = selectedPanel;
        panel.setShown();

    }

    // public boolean isOpaque() {
    // return true;
    // }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    @Override
    public void paint(Graphics g) {

        super.paint(g);
    }

    public void setSelectedSubPanel(Class<?> class1) {
        sidebar.setSelectedTreeEntry(class1);
    }

}
