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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.appwork.shutdown.ShutdownController;
import org.appwork.swing.MigPanel;
import org.appwork.utils.ColorUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigEntry.PropertyType;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

public abstract class ConfigPanel extends SwitchPanel {
    private static final long              serialVersionUID = 3383448498625377495L;
    public static final int                ICON_SIZE        = 32;
    private java.util.List<GUIConfigEntry> entries          = new ArrayList<GUIConfigEntry>();
    protected JPanel                       panel;
    private ConfigGroup                    currentGroup;
    protected GUIConfigEntry               over;
    private MouseMotionListener            ml;

    public class ScrollablePanel extends MigPanel implements Scrollable {
        public ScrollablePanel() {
            super("ins 0, wrap 2", "[fill,grow 10]10[fill,grow]", "[]");
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public ConfigPanel() {
        this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
        // this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setOpaque(false);
        panel = new ScrollablePanel() {
            @Override
            protected void paintComponent(Graphics g) {
                GUIConfigEntry p = over;
                if (p != null) {
                    Graphics2D g2 = (Graphics2D) ((Graphics2D) g).create(0, p.getYMin(), getWidth(), p.getYMax() - p.getYMin());
                    g2.setClip(null);
                    g2.setColor(ColorUtils.getAlphaInstance(getForeground(), 15));
                    g2.fillRect(0, 0, getWidth(), p.getYMax() - p.getYMin());
                }
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        ml = new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean found = false;
                Point point = e.getPoint();
                point = SwingUtilities.convertPoint(e.getComponent(), point, panel);
                for (GUIConfigEntry p : entries) {
                    if (point.y >= p.getYMin() - 5 && point.y < p.getYMax()) {
                        found = true;
                        if (over != p) {
                            GUIConfigEntry last = over;
                            over = p;
                            {
                                int yMin = p.getYMin();
                                if (last != null) {
                                    yMin = Math.min(yMin, last.getYMin());
                                }
                                int yMax = p.getYMax();
                                if (last != null) {
                                    yMax = Math.max(yMax, last.getYMax());
                                }
                                repaint(0, yMin - 10, getWidth(), yMax - yMin + 20);
                            }
                        }
                        break;
                    }
                }
                if (!found) {
                    if (over != null) {
                        GUIConfigEntry last = over;
                        over = null;
                        repaint(0, last.getYMin(), getWidth(), last.getYMax() - last.getYMin());
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        };
        panel.addMouseMotionListener(ml);
    }

    public void init() {
        for (ConfigEntry cfgEntry : setupContainer().getEntries()) {
            addConfigEntry(cfgEntry);
        }
        add(panel);
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
        guiEntry.addMouseMotionListener(ml);
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
                panel.add(new JScrollPane(guiEntry.getInput()), gapLeft + "gaptop 0,spanx,growy,pushy,gapbottom 5,wmin 10");
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

    public void reload() {
        for (GUIConfigEntry akt : entries) {
            akt.reload();
        }
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
            if (!ShutdownController.getInstance().isShutDownRequested()) {
                int answer = UserIO.getInstance().requestConfirmDialog(0, _GUI.T.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_title(), _GUI.T.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion(), null, _GUI.T.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_ok(), null);
                if (UserIO.isOK(answer)) {
                    RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
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
        HashSet<SubConfiguration> subs = new HashSet<SubConfiguration>();
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
        return new AbstractIcon(IconKey.ICON_SETTINGS, 32);
    }
}