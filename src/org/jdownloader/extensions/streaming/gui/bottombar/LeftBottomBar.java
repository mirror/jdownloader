package org.jdownloader.extensions.streaming.gui.bottombar;

import javax.swing.JButton;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.VLCGui;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddOptionsAction;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LeftBottomBar extends MigPanel {

    private GraphicalUserInterfaceSettings config;
    private JButton                        addLinks;

    private JButton                        popup;
    private JButton                        popupRemove;

    private StreamingExtension             extension;

    // private JToggleButton showHideSidebar;

    public LeftBottomBar(StreamingExtension plg, final VLCGui vlcGui) {
        super("ins 0 0 1 0,debug", "[]1[]1[]", "[]");
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
        popupRemove = new JButton(/* todo */);

        add(addLinks, "height 24!,aligny top");

        add(popup, "height 24!,width 12!,aligny top");
        add(popupRemove, "width 24!,height 24!,gapleft 2,aligny top");

    }

    private ExtButton addButton(AppAction action) {
        ExtButton bt = new ExtButton(action);
        // bt.setText("");
        // bt.setRolloverEffectEnabled(true);

        add(bt, "width 24!,height 24!,gapleft 2,aligny top");
        return bt;
    }

}
