//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.translate.T;
import org.jdownloader.update.RestartController;

public abstract class ConfigPanel extends SwitchPanel {

    private static final long         serialVersionUID = 3383448498625377495L;

    public static final int           ICON_SIZE        = 32;

    private ArrayList<GUIConfigEntry> entries          = new ArrayList<GUIConfigEntry>();

    protected Logger                  logger           = JDLogger.getLogger();

    protected JPanel                  panel;

    private ConfigGroup               currentGroup;

    public ConfigPanel() {
        this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel = new JPanel(new MigLayout("ins 5, wrap 2", "[fill,grow 10]10[fill,grow]"));
    }

    public void init(boolean useScrollPane) {
        for (ConfigEntry cfgEntry : setupContainer().getEntries()) {
            addConfigEntry(cfgEntry);
        }
        if (useScrollPane) {
            JScrollPane scroll = new JScrollPane(panel);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);

            this.add(scroll);
        } else {
            add(panel);
        }
        this.load();
    }

    protected abstract ConfigContainer setupContainer();

    /**
     * Overwrite this, when no ConfigGroups Headers should be displayed
     */
    protected boolean showGroups() {
        return true;
    }

    private void addConfigEntry(ConfigEntry entry) {
        GUIConfigEntry guiEntry = new GUIConfigEntry(entry);

        ConfigGroup group = showGroups() ? entry.getGroup() : null;
        if (currentGroup != group) {
            if (group != null) {
                panel.add(Factory.createHeader(group), "spanx");
            } else {
                panel.add(new JSeparator(), "spanx, gapbottom 15, gaptop 15");
            }
            currentGroup = group;
        }

        String gapLeft = (group == null) ? "" : "gapleft 37,";
        if (guiEntry.getDecoration() != null) {
            switch (entry.getType()) {
            case ConfigContainer.TYPE_TEXTAREA:
            case ConfigContainer.TYPE_LISTCONTROLLED:
                panel.add(guiEntry.getDecoration(), gapLeft + "spanx");
                break;
            case ConfigContainer.TYPE_COMPONENT:
                panel.add(guiEntry.getDecoration(), gapLeft + "spanx," + guiEntry.getConfigEntry().getConstraints());
                break;
            default:
                panel.add(guiEntry.getDecoration(), gapLeft + (guiEntry.getInput() == null ? "spanx" : ""));
                break;
            }
        }

        if (guiEntry.getInput() != null) {
            switch (entry.getType()) {
            case ConfigContainer.TYPE_BUTTON:
                panel.add(guiEntry.getInput(), (guiEntry.getDecoration() == null ? gapLeft + "spanx," : "") + "wmax 250");
                break;
            case ConfigContainer.TYPE_TEXTAREA:
            case ConfigContainer.TYPE_LISTCONTROLLED:
                panel.add(new JScrollPane(guiEntry.getInput()), gapLeft + "spanx, growy, pushy");
                break;
            default:
                panel.add(guiEntry.getInput(), guiEntry.getDecoration() == null ? gapLeft + "spanx" : "");
                break;
            }
        }

        entries.add(guiEntry);
    }

    public final void load() {
        this.loadConfigEntries();
        this.loadSpecial();
    }

    private final void loadConfigEntries() {
        for (GUIConfigEntry akt : entries) {
            akt.load();
        }
    }

    public final void save() {
        this.saveConfigEntries();
        this.saveSpecial();
    }

    /**
     * Should be overwritten to do special loading.
     */
    protected void loadSpecial() {
    }

    /**
     * Should be overwritten to do special saving.
     */
    protected void saveSpecial() {
    }

    @Override
    public void onShow() {
        load();
    }

    @Override
    public void onHide() {
        PropertyType changes = hasChanges();
        this.save();
        if (changes == PropertyType.NEEDS_RESTART) {
            if (!JDGui.getInstance().isExitRequested()) {
                int answer = UserIO.getInstance().requestConfirmDialog(0, T._.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_title(), T._.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion(), null, T._.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_ok(), null);

                if (UserIO.isOK(answer)) {
                    RestartController.getInstance().directRestart();
                }
            }
        }
    }

    public PropertyType hasChanges() {
        PropertyType ret = PropertyType.NONE;
        Object old;
        synchronized (entries) {
            for (GUIConfigEntry akt : entries) {
                if (akt.getConfigEntry().getPropertyInstance() != null && akt.getConfigEntry().getPropertyName() != null) {
                    old = akt.getConfigEntry().getPropertyInstance().getProperty(akt.getConfigEntry().getPropertyName());
                    if (old == null && akt.getText() != null) {
                        ret = ret.getMax(akt.getConfigEntry().getPropertyType());
                    } else if (old != null && !old.equals(akt.getText())) {
                        ret = ret.getMax(akt.getConfigEntry().getPropertyType());
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Saves the ConfigEntries in THIS panel.
     */
    private final void saveConfigEntries() {
        ArrayList<SubConfiguration> subs = new ArrayList<SubConfiguration>();
        for (GUIConfigEntry akt : entries) {
            if (akt.getConfigEntry().getPropertyInstance() instanceof SubConfiguration && !subs.contains(akt.getConfigEntry().getPropertyInstance())) {
                subs.add((SubConfiguration) akt.getConfigEntry().getPropertyInstance());
            }
            akt.save();
        }

        for (SubConfiguration subConfiguration : subs) {
            subConfiguration.save();
        }
    }

    public String getTitle() {
        return "No Title set!";
    }

    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.configuration", 32, 32);
    }

}