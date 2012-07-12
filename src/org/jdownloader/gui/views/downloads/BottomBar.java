package org.jdownloader.gui.views.downloads;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Pattern;

import javax.swing.JButton;

import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.SearchCategory;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.gui.views.downloads.action.ClearAction;
import org.jdownloader.gui.views.downloads.action.RemoveOptionsAction;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddOptionsAction;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class BottomBar extends MigPanel {

    private GraphicalUserInterfaceSettings         config;
    private JButton                                addLinks;

    private JButton                                clearAll;
    private JButton                                popup;
    private JButton                                popupRemove;
    private SearchField<FilePackage, DownloadLink> searchField;
    private PseudoCombo                            combo;

    // private JToggleButton showHideSidebar;

    public BottomBar(final DownloadsTable table) {
        super("ins 0 0 1 0", "[]1[]1[]1[]", "[]");

        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);

        addLinks = new JButton(new AddLinksAction());

        clearAll = new JButton(new ClearAction());
        popup = new JButton(new AddOptionsAction(addLinks)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        popupRemove = new JButton(new RemoveOptionsAction(table, clearAll)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        searchField = new SearchField<FilePackage, DownloadLink>(table) {

            @Override
            public void setSelectedCategory(SearchCategory selectedCategory) {
                super.setSelectedCategory(selectedCategory);
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setSelectedDownloadSearchCategory(selectedCategory);
            }

            @Override
            public boolean isFiltered(FilePackage e) {
                if (SearchCategory.PACKAGE == selectedCategory) {

                    for (Pattern filterPattern : filterPatterns) {
                        if (filterPattern.matcher(e.getName()).find()) return false;
                    }
                    return true;

                }
                return false;
            }

            @Override
            public boolean isFiltered(DownloadLink v) {
                switch (selectedCategory) {
                case FILENAME:
                    for (Pattern filterPattern : filterPatterns) {
                        if (filterPattern.matcher(v.getName()).find()) return false;
                    }
                    return true;
                case HOSTER:
                    for (Pattern filterPattern : filterPatterns) {
                        if (filterPattern.matcher(v.getHost()).find()) return false;
                    }
                    return true;
                }
                return false;
            }

        };
        searchField.setSelectedCategory(JsonConfig.create(GraphicalUserInterfaceSettings.class).getSelectedDownloadSearchCategory());
        searchField.setCategories(new SearchCategory[] { SearchCategory.FILENAME, SearchCategory.HOSTER, SearchCategory.PACKAGE });
        searchField.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    searchField.setText("");

                }
            }

            public void keyPressed(KeyEvent e) {
            }
        });
        add(addLinks, "height 24!,aligny top");

        add(popup, "height 24!,width 12!,aligny top");
        add(clearAll, "width 24!,height 24!,gapleft 2,aligny top");
        add(popupRemove, "height 24!,width 12!,aligny top");

        add(searchField, "height 24!,aligny top,gapleft 2,pushx,growx");
        combo = new PseudoCombo(new View[] { View.ALL, View.RUNNING, View.FAILED, View.SUCCESSFUL }) {

            private PackageControllerTableModelFilter<FilePackage, DownloadLink> filter = new PackageControllerTableModelFilter<FilePackage, DownloadLink>() {

                                                                                            public void reset() {
                                                                                            }

                                                                                            public boolean isFiltered(DownloadLink v) {
                                                                                                switch (selectedItem) {
                                                                                                case RUNNING:
                                                                                                    return !v.isEnabled() || !v.getLinkStatus().isPluginActive();
                                                                                                case SUCCESSFUL:
                                                                                                    return !(v.getLinkStatus().isFinished());
                                                                                                case FAILED:
                                                                                                    return !(v.getLinkStatus().isFailed());
                                                                                                }
                                                                                                return false;
                                                                                            }

                                                                                            public boolean isFiltered(FilePackage e) {
                                                                                                return false;
                                                                                            }

                                                                                            public boolean highlightFilter() {
                                                                                                return true;
                                                                                            }
                                                                                        };

            @Override
            public void onChanged(View value) {
                if (View.ALL.equals(value)) {
                    table.getPackageControllerTableModel().removeFilter(filter);
                } else {
                    table.getPackageControllerTableModel().addFilter(filter);
                }
                table.getPackageControllerTableModel().recreateModel(true);
                CFG_GUI.DOWNLOAD_VIEW.setValue(value);
            }
        };
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isSaveDownloadViewCrossSessionEnabled()) {
            combo.setSelectedItem((View) CFG_GUI.DOWNLOAD_VIEW.getValue());
        } else {
            combo.setSelectedItem(View.ALL);
        }
        add(combo, "height 24!,aligny top,gapleft 2");

        if (config.isShowMoveToTopButton()) {
            addButton(table.getMoveTopAction()).setFocusable(false);
        }
        if (config.isShowMoveUpButton()) {
            addButton(table.getMoveUpAction()).setFocusable(false);

        }
        if (config.isShowMoveDownButton()) {
            addButton(table.getMoveDownAction()).setFocusable(false);
        }
        if (config.isShowMoveToBottomButton()) {
            addButton(table.getMoveToBottomAction()).setFocusable(false);
        }
        addButton(new AppAction() {
            {
                setTooltipText(_GUI._.BottomBar_BottomBar_settings());
                setIconKey("settings");
                // setIconSizes(18);
            }

            public void actionPerformed(ActionEvent e) {
                QuickSettingsPopup pu = new QuickSettingsPopup();
                int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();
                pu.show((Component) e.getSource(), -pu.getPreferredSize().width + insets[3] + ((Component) e.getSource()).getWidth() + 1, -pu.getPreferredSize().height + insets[2]);
                // new CleanupMenu()
            }
        });
        // showHideSidebar = new JToggleButton(new AppAction() {
        // /**
        // *
        // */
        // private static final long serialVersionUID = 1L;
        //
        // {
        //
        // putValue(SMALL_ICON, NewTheme.I().getIcon("sidebar", -1));
        // setTooltipText(_GUI._.LinkGrabberPanel_LinkGrabberPanel_btn_showsidebar_tt_up());
        //
        // }
        //
        // public void actionPerformed(ActionEvent e) {
        // org.jdownloader.settings.statics.GUI.CFG.setDownloadViewSidebarVisible(!org.jdownloader.settings.statics.GUI.CFG.isDownloadViewSidebarVisible());
        // }
        // });
        //
        // if
        // (org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_TOGGLE_BUTTON_ENABLED.getValue()
        // &&
        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_ENABLED.getValue())
        // {
        // //
        // add(showHideSidebar, "height 24!,width 24!,gapleft 3,aligny top");
        // }

    }

    private ExtButton addButton(AppAction action) {
        ExtButton bt = new ExtButton(action);
        // bt.setText("");
        // bt.setRolloverEffectEnabled(true);

        add(bt, "width 24!,height 24!,gapleft 2,aligny top");
        return bt;
    }

}
