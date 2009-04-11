package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSeparator;

import jd.OptionalPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigGroup;
import jd.config.MenuItem;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class AddonTaskPane extends TaskPanel implements ActionListener, ControlListener {

    private HashMap<JButton, OptionalPluginWrapper> buttonMap;
    private JButton config;
    private ArrayList<CollapseButton> entries;

    public AddonTaskPane(String string, ImageIcon ii) {
        super(string, ii, "addons");
        JDUtilities.getController().addControlListener(this);
        this.buttonMap = new HashMap<JButton, OptionalPluginWrapper>();
        this.entries = new ArrayList<CollapseButton>();
        initGUI();
    }

    private void initGUI() {
        for (final OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!wrapper.isEnabled()) continue;
            if (wrapper.getPlugin().createMenuitems() != null && JDUtilities.getConfiguration().getBooleanProperty(wrapper.getConfigParamKey(), false)) {
                if (wrapper.getPlugin().createMenuitems().size() > 1) {

                    CollapseButton bt = new CollapseButton(wrapper.getPlugin().getHost(), JDTheme.II(wrapper.getPlugin().getIconKey(), 16, 16));
                    add(bt, D1_BUTTON_ICON);

                    for (MenuItem entry : wrapper.getPlugin().createMenuitems()) {

                        JComponent comp = createMenu(entry);

                        bt.getContentPane().add(comp, "gapleft 20");

                    }
                    bt.getButton().addActionListener(this);
                    buttonMap.put(bt.getButton(), wrapper);
                    entries.add(bt);

                }
            }
        }
        add(new JSeparator());

        config = createButton(JDLocale.L("gui.tasks.addons.edit", "Addonmanager"), JDTheme.II("gui.images.config.addons", 16, 16));
        add(config, D1_BUTTON_ICON);

    }

    private JComponent createMenu(final MenuItem entry) {

        JButton bt;
        switch (entry.getID()) {
        case MenuItem.CONTAINER:
            CollapseButton col = new CollapseButton(entry.getTitle(), JDTheme.II("gui.images.taskpanes.download", 16, 16));
            for (int i = 0; i < entry.getSize(); i++) {
                col.getContentPane().add(createMenu(entry.get(i)), "gapleft 10");
            }

            col.getButton().addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    entry.getActionListener().actionPerformed(new ActionEvent(entry, entry.getActionID(), arg0.getActionCommand()));

                }

            });
            return col;
        case MenuItem.NORMAL:
            bt = createButton(entry.getTitle(), null);
            bt.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    entry.getActionListener().actionPerformed(new ActionEvent(entry, entry.getActionID(), arg0.getActionCommand()));

                }

            });

            return bt;
        case MenuItem.SEPARATOR:
            return new JSeparator();

        case MenuItem.TOGGLE:
            JCheckBox ch = new JCheckBox(entry.getTitle(), entry.isSelected());

            ch.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    entry.getActionListener().actionPerformed(new ActionEvent(entry, entry.getActionID(), arg0.getActionCommand()));

                }

            });
            return ch;
        }
        return null;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7720749076951577192L;

    public void actionPerformed(ActionEvent e) {
      
        OptionalPluginWrapper wrapper = buttonMap.get(e.getSource());
        if (wrapper != null) {
            ConfigContainer cfg = wrapper.getPlugin().getConfig();
            if (cfg != null) {
                if (cfg.getContainerNum() == 0) {
                    int i = 0;
                    ConfigGroup group = new ConfigGroup(wrapper.getPlugin().getHost(), JDTheme.II(wrapper.getPlugin().getIconKey(), 32, 32));
                    while (cfg.getEntryAt(i) != null) {
                        cfg.getEntryAt(i).setGroup(group);

                        i++;

                    }
                }
                SimpleGUI.CURRENTGUI.getContentPane().display(new ConfigEntriesPanel(cfg));
            }

            for(CollapseButton entry : entries) {
                if (entry.getButton() != e.getSource()){
                    entry.setCollapsed(true);
                }
            }
        }
        if (e.getSource() == config) {
            SimpleGuiConstants.GUI_CONFIG.setProperty("LAST_CONFIG_PANEL", ConfigTaskPane.ACTION_ADDONS);
            SimpleGUI.CURRENTGUI.getTaskPane().switcher(SimpleGUI.CURRENTGUI.getCfgTskPane());

        }
    }

    public void controlEvent(ControlEvent event) {
        // if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
        // if (ContentPanel.PANEL != null && ContentPanel.PANEL.getRightPanel()
        // instanceof ConfigPanel) {
        // if (((ConfigPanel) ContentPanel.PANEL.getRightPanel()).hasChanges()
        // != PropertyType.NONE) {
        // this.changes = true;
        // System.out.println("CHANGES !");
        // if (((ConfigPanel) ContentPanel.PANEL.getRightPanel()).hasChanges()
        // == PropertyType.NEEDS_RESTART) {
        // System.out.println("RESTART !");
        // this.restart = true;
        // }
        // }
        // if (changes) {
        // sav.setEnabled(true);
        // }
        //
        // }
        // }

    }

}
