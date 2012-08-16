package org.jdownloader.extensions.streaming.gui.bottombar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.MediaArchiveTable;
import org.jdownloader.extensions.streaming.gui.actions.RemoveOptionsAction;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.gui.views.downloads.QuickSettingsPopup;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddOptionsAction;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class BottomBar extends MigPanel {

    private GraphicalUserInterfaceSettings                                  config;
    private JButton                                                         addLinks;

    private JButton                                                         popup;
    private JButton                                                         popupRemove;
    private SearchField<MediaArchiveSearchCategory, MediaFolder, MediaItem> searchField;

    private StreamingExtension                                              extension;

    // private JToggleButton showHideSidebar;

    public BottomBar(StreamingExtension plg, final MediaArchiveTable table) {
        super("ins 0 0 1 0,debug", "[]1[]1[]1[]1[]", "[]");
        extension = plg;

        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);

        addLinks = new JButton(new AddLinksAction(T._.add_files_action()));

        popup = new JButton(new AddOptionsAction(addLinks)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        popupRemove = new JButton(new RemoveOptionsAction(table));
        searchField = new SearchField<MediaArchiveSearchCategory, MediaFolder, MediaItem>(table, MediaArchiveSearchCategory.FILENAME) {

            @Override
            public void setSelectedCategory(MediaArchiveSearchCategory selectedCategory) {
                super.setSelectedCategory(selectedCategory);
                extension.getSettings().setSelectedSearchCategory(selectedCategory);
            }

            @Override
            public boolean isFiltered(MediaFolder e) {

                return false;
            }

            @Override
            public boolean isFiltered(MediaItem v) {

                return false;
            }

        };
        searchField.setSelectedCategory(extension.getSettings().getSelectedSearchCategory());
        searchField.setCategories(new MediaArchiveSearchCategory[] { MediaArchiveSearchCategory.FILENAME });
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
        add(popupRemove, "width 24!,height 24!,gapleft 2,aligny top");

        add(searchField, "height 24!,aligny top,gapleft 2,pushx,growx");

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
