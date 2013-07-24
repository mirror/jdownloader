package org.jdownloader.extensions.streaming.gui.bottombar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.gui.VLCGui;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.QuickSettingsPopup;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.updatev2.gui.LAFOptions;

public class RightBottomBar extends MigPanel {

    private GraphicalUserInterfaceSettings          config;

    private StreamingExtension                      extension;

    private SearchField<MediaArchiveSearchCategory> searchField;

    // private JToggleButton showHideSidebar;

    public RightBottomBar(StreamingExtension plg, final VLCGui vlcGui) {
        super("ins 0 0 1 0", "[grow,fill]1[]", "[]");
        extension = plg;

        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);

        searchField = new SearchField<MediaArchiveSearchCategory>(MediaArchiveSearchCategory.FILENAME) {

            @Override
            public void setSelectedCategory(MediaArchiveSearchCategory selectedCategory) {
                super.setSelectedCategory(selectedCategory);
                extension.getSettings().setSelectedSearchCategory(selectedCategory);
            }

            @Override
            protected void onFiltering(List<Pattern> object) {
                System.out.println("TODO: org.jdownloader.extensions.streaming.gui.bottombar.SearchField<MediaArchiveSearchCategory>");
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
                int[] insets = LAFOptions.getInstance().getPopupBorderInsets();
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
