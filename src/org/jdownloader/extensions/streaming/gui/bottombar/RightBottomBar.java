package org.jdownloader.extensions.streaming.gui.bottombar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.gui.MediaArchiveTable;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.SearchField;
import org.jdownloader.gui.views.downloads.QuickSettingsPopup;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class RightBottomBar extends MigPanel {

    private GraphicalUserInterfaceSettings                                  config;

    private SearchField<MediaArchiveSearchCategory, MediaFolder, MediaItem> searchField;

    private StreamingExtension                                              extension;

    // private JToggleButton showHideSidebar;

    public RightBottomBar(StreamingExtension plg, final MediaArchiveTable table) {
        super("ins 0 0 1 0", "[grow,fill]1[]", "[]");
        extension = plg;

        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);

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

        add(searchField, "height 24!,aligny top,pushx,growx");

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

    }

    private ExtButton addButton(AppAction action) {
        ExtButton bt = new ExtButton(action);
        // bt.setText("");
        // bt.setRolloverEffectEnabled(true);

        add(bt, "width 24!,height 24!,gapleft 2,aligny top");
        return bt;
    }

}
