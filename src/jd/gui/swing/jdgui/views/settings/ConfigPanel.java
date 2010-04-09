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

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ConfigPanel extends SwitchPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    protected ArrayList<GUIConfigEntry> entries = new ArrayList<GUIConfigEntry>();

    protected Logger logger = JDLogger.getLogger();

    protected JPanel panel;

    private ConfigGroup currentGroup;

    private JPanel header;

    public ConfigPanel() {
        this.setLayout(new MigLayout("ins 0 0 0 0", "[fill,grow]", "[fill,grow]"));
        panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0 10 10 10,wrap 2", "[fill,grow 10]10[fill,grow]"));
    }

    /**
     * Constructor to display a ConfigPanel with contents of container
     * 
     * @param container
     */
    public ConfigPanel(ConfigContainer container) {
        this();
        for (ConfigEntry cfgEntry : container.getEntries()) {
            GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
            if (ce != null) addGUIConfigEntry(ce);
        }

        this.load();
        this.add(panel);
    }

    public String getBreadcrum() {
        return "";
    }

    public static String getTitle() {
        return "NOTITLE";
    }

    public void addGUIConfigEntry(GUIConfigEntry entry, JPanel panel) {
        ConfigGroup group = entry.getConfigEntry().getGroup();

        if (group == null) {
            if (currentGroup != null) {
                panel.add(new JSeparator(), "spanx,gapbottom 15,gaptop 15");
                currentGroup = null;
            }

            if (entry.getDecoration() != null) {
                switch (entry.getConfigEntry().getType()) {
                case ConfigContainer.TYPE_TEXTAREA:
                case ConfigContainer.TYPE_LISTCONTROLLED:
                    panel.add(entry.getDecoration(), "spany " + entry.getInput().length + ",spanx, gapright " + getGapRight());
                    break;
                case ConfigContainer.TYPE_CONTAINER:
                    /**
                     * TODO . handly different containers
                     */
                    break;
                default:
                    panel.add(entry.getDecoration(), "spany " + Math.max(1, entry.getInput().length) + (entry.getInput().length == 0 ? ",spanx" : ""));
                }
            }

            int i = 0;
            for (JComponent c : entry.getInput()) {
                i++;
                switch (entry.getConfigEntry().getType()) {
                case ConfigContainer.TYPE_BUTTON:
                    panel.add(c, entry.getDecoration() == null ? "spanx,gapright " + getGapRight() : "width n:n:160,gapright " + getGapRight());
                    break;
                case ConfigContainer.TYPE_TEXTAREA:
                case ConfigContainer.TYPE_LISTCONTROLLED:
                    panel.add(new JScrollPane(c), "spanx,gapright " + getGapRight() + ",growy,pushy");
                    break;
                case ConfigContainer.TYPE_CONTAINER:
                    /**
                     * TODO . handly different containers
                     */
                    break;
                default:
                    panel.add(c, entry.getDecoration() == null ? "spanx,gapright " + getGapRight() : "gapright " + getGapRight());
                    break;
                }
            }
            entries.add(entry);
            currentGroup = null;
            return;
        } else {
            if (currentGroup != group) {
                panel.add(header = Factory.createHeader(group), "spanx,hidemode 3");
                header.setVisible(false);
                currentGroup = group;
            }

            if (entry.getDecoration() != null) {
                switch (entry.getConfigEntry().getType()) {
                case ConfigContainer.TYPE_TEXTAREA:
                case ConfigContainer.TYPE_LISTCONTROLLED:
                    panel.add(entry.getDecoration(), "gapleft " + getGapLeft() + ",spany " + entry.getInput().length + ",spanx");
                    break;
                default:
                    panel.add(entry.getDecoration(), "gapleft " + getGapLeft() + ",spany " + entry.getInput().length + (entry.getInput().length == 0 ? ",spanx" : ""));
                }
            }
            int i = 0;
            for (JComponent c : entry.getInput()) {
                i++;
                switch (entry.getConfigEntry().getType()) {
                case ConfigContainer.TYPE_BUTTON:
                    panel.add(c, entry.getDecoration() == null ? "spanx,gapright " + this.getGapRight() + ",gapleft " + this.getGapLeft() : "width n:n:160,gapright " + this.getGapRight());
                    header.setVisible(true);
                    break;
                case ConfigContainer.TYPE_TEXTAREA:
                    panel.add(new JScrollPane(c), "spanx,gapright " + getGapRight() + ",growy,pushy,gapleft " + getGapLeft());
                    header.setVisible(true);
                    break;
                default:
                    panel.add(c, entry.getDecoration() == null ? "spanx,gapright " + this.getGapRight() + ",gapleft " + this.getGapLeft() : "gapright " + this.getGapRight());
                    header.setVisible(true);
                    break;
                }
            }
        }
        entries.add(entry);
    }

    private String getGapLeft() {
        return "35";
    }

    private String getGapRight() {
        return "20";
    }

    public void addGUIConfigEntry(GUIConfigEntry entry) {
        addGUIConfigEntry(entry, panel);
    }

    /**
     * Should be overwritten to initialise the contentpanel.
     */
    public void initPanel() {
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
        this.saveSpecial();
        this.saveConfigEntries();
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
                int answer = UserIO.getInstance().requestConfirmDialog(0, JDL.L("jd.gui.swing.jdgui.settings.ConfigPanel.restartquestion.title", "Restart required!"), JDL.L("jd.gui.swing.jdgui.settings.ConfigPanel.restartquestion", "This option needs a JDownloader restart."), null, JDL.L("jd.gui.swing.jdgui.settings.ConfigPanel.restartquestion.ok", "Restart NOW!"), null);

                if (JDFlags.hasSomeFlags(answer, UserIO.RETURN_DONT_SHOW_AGAIN | UserIO.RETURN_OK)) {
                    JDUtilities.restartJD(false);
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
                    if (akt.getConfigEntry().hasChanges()) {
                        ret = ret.getMax(PropertyType.NORMAL);
                    }
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
     * Saves the configentries in THIS panel.
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

}
