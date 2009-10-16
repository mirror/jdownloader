//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.sidebars.configuration;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.OptionalPluginWrapper;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.settings.panels.addons.ConfigPanelAddons;
import jd.gui.swing.jdgui.settings.panels.hoster.ConfigPanelPluginForHost;
import jd.gui.swing.jdgui.settings.panels.premium.Premium;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class ConfigTreeModel implements TreeModel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.ConfigTreeModel.";

    private TreeEntry root;
    /** Listeners. */
    protected EventListenerList listenerList = new EventListenerList();
    private TreeEntry addons;

    private TreeEntry plugins;

    public ConfigTreeModel() {
        this.root = new TreeEntry(JDL.L(JDL_PREFIX + "CONFIGURATION.title", "Settings"));

        TreeEntry basics, modules;
        root.add(basics = new TreeEntry(JDL.L(JDL_PREFIX + "basics.title", "Basics")).setIcon("gui.images.config.home"));
        basics.add(new TreeEntry(ConfigPanelGeneral.class, ConfigPanelGeneral.getTitle()).setIcon("gui.images.config.home"));
        TreeEntry dl;
        basics.add(dl = new TreeEntry(jd.gui.swing.jdgui.settings.panels.downloadandnetwork.General.class, jd.gui.swing.jdgui.settings.panels.downloadandnetwork.General.getTitle()).setIcon("gui.images.config.network_local"));

        // dl.add(new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.downloadandnetwork.General.class,
        // JDL.L(JDL_PREFIX + "download.general.title",
        // "Main")).setIcon("gui.images.package_opened"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.downloadandnetwork.InternetAndNetwork.class, jd.gui.swing.jdgui.settings.panels.downloadandnetwork.InternetAndNetwork.getTitle()).setIcon("gui.images.networkerror"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.downloadandnetwork.Advanced.class, jd.gui.swing.jdgui.settings.panels.downloadandnetwork.Advanced.getTitle()).setIcon("gui.images.network"));

        basics.add(dl = new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.General.class, jd.gui.swing.jdgui.settings.panels.gui.General.getTitle()).setIcon("gui.images.config.gui"));
        // dl.add(new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.General.class,
        // JDL.L(JDL_PREFIX + "gui.general.title",
        // "Main")).setIcon("gui.images.config.gui"));

        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.ToolbarController.class, jd.gui.swing.jdgui.settings.panels.gui.ToolbarController.getTitle()).setIcon("gui.images.toolbar"));

        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.Linkgrabber.class, jd.gui.swing.jdgui.settings.panels.gui.Linkgrabber.getTitle()).setIcon("gui.images.taskpanes.linkgrabber"));
        //

        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.Browser.class, jd.gui.swing.jdgui.settings.panels.gui.Browser.getTitle()).setIcon("gui.images.config.host"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.Advanced.class, jd.gui.swing.jdgui.settings.panels.gui.Advanced.getTitle()).setIcon("gui.images.container"));

        root.add(modules = new TreeEntry(JDL.L(JDL_PREFIX + "modules.title", "Modules")).setIcon("gui.images.config.home"));

        // modules.add(dl = new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.ocr.General.class,
        // jd.gui.swing.jdgui.settings.panels.ocr.General.getTitle()).setIcon("gui.images.config.ocr"));

        // dl.add(new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.ocr.General.class,
        // JDL.L(JDL_PREFIX + "captcha.general.title",
        // "Method List")).setIcon("gui.images.config.ocr"));
        modules.add(dl = new TreeEntry(jd.gui.swing.jdgui.settings.panels.ConfigPanelCaptcha.class, jd.gui.swing.jdgui.settings.panels.ConfigPanelCaptcha.getTitle()).setIcon("gui.images.config.ocr"));

        modules.add(dl = new TreeEntry(jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.class, jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.getTitle()).setIcon("gui.images.config.reconnect"));
        // dl.add(new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.class,
        // JDL.L(JDL_PREFIX + "reconnect.methodselection.title",
        // "Reconnect")).setIcon("gui.images.config.reconnect"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.reconnect.Advanced.class, jd.gui.swing.jdgui.settings.panels.reconnect.Advanced.getTitle()).setIcon("gui.images.reconnect_settings"));

        modules.add(dl = new TreeEntry(JDL.L(JDL_PREFIX + "passwordsANdLogins", "Passwords & Logins")).setIcon("gui.images.list"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.passwords.PasswordList.class, jd.gui.swing.jdgui.settings.panels.passwords.PasswordList.getTitle()).setIcon("gui.images.addons.unrar"));

        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.passwords.PasswordListHTAccess.class, jd.gui.swing.jdgui.settings.panels.passwords.PasswordListHTAccess.getTitle()).setIcon("gui.images.htaccess"));

        root.add(plugins = new TreeEntry(JDL.L(JDL_PREFIX + "plugins.title", "Plugins & Add-ons")).setIcon("gui.images.config.packagemanager"));
        TreeEntry hoster;
        plugins.add(hoster = new TreeEntry(ConfigPanelPluginForHost.class, ConfigPanelPluginForHost.getTitle()).setIcon("gui.images.config.host"));

        hoster.add(new TreeEntry(Premium.class, Premium.getTitle()).setIcon("gui.images.premium"));
        plugins.add(addons = new TreeEntry(ConfigPanelAddons.class, ConfigPanelAddons.getTitle()).setIcon("gui.images.config.packagemanager"));
        initExtensions(addons);
    }

    private void initExtensions(TreeEntry addons2) {
        for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded() || !plg.isEnabled() || plg.getPlugin().getConfig().getEntries().size() == 0) continue;

            addons2.add(new TreeEntry(AddonConfig.getInstance(plg.getPlugin().getConfig(), plg.getHost(), ""), plg.getHost()).setIcon(plg.getPlugin().getIconKey()));
        }

    }

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     * 
     * @see #removeTreeModelListener
     * @param l
     *            the listener to add
     */
    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener previously added with <B>addTreeModelListener()</B>.
     * 
     * @see #addTreeModelListener
     * @param l
     *            the listener to remove
     */
    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    public Object getChild(Object parent, int index) {

        return ((TreeEntry) parent).get(index);
    }

    public int getChildCount(Object parent) {

        return ((TreeEntry) parent).size();
    }

    public int getIndexOfChild(Object parent, Object child) {

        return ((TreeEntry) parent).indexOf(child);
    }

    public Object getRoot() {

        return root;
    }

    public boolean isLeaf(Object node) {

        return ((TreeEntry) node).size() == 0;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    static class TreeEntry {

        private Class<? extends SwitchPanel> clazz;
        private String title;
        private ImageIcon icon;
        private String iconKey;

        public Class<? extends SwitchPanel> getClazz() {
            return clazz;
        }

        public TreeEntry setIcon(String string) {
            iconKey = string;
            icon = JDTheme.II(string, 20, 20);
            return this;
        }

        public String getIconKey() {
            return iconKey;
        }

        public void setIconKey(String iconKey) {
            this.iconKey = iconKey;
        }

        public void setClazz(Class<? extends SwitchPanel> clazz) {
            this.clazz = clazz;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public ImageIcon getIcon() {
            return icon;
        }

        public TreeEntry setIcon(ImageIcon icon) {
            this.icon = icon;
            return this;
        }

        public String getTooltip() {
            return tooltip;
        }

        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        private String tooltip;
        private ArrayList<TreeEntry> entries;
        private SingletonPanel panel;
        private static final HashMap<Class<? extends SwitchPanel>, TreeEntry> PANELS = new HashMap<Class<? extends SwitchPanel>, TreeEntry>();

        /**
         * Returns the TreeEntry to a class if it has been added using the
         * public static TreeEntry getTreeByClass(Class<? extends SwitchPanel>
         * cl) constructor
         * 
         * @param cl
         * @return
         */
        public static TreeEntry getTreeByClass(Class<?> cl) {
            return PANELS.get(cl);
        }

        public TreeEntry(final Class<? extends SwitchPanel> class1, String l) {
            this.clazz = class1;

            if (class1 != null) {
                panel = new SingletonPanel(class1, JDUtilities.getConfiguration());
                // init this panel in an extra thread..
                new Thread() {
                    @Override
                    public void run() {
                        new GuiRunnable<Object>() {
                            @Override
                            public Object runSave() {
                                panel.getPanel();
                                return null;
                            }

                        }.start();

                    }
                }.start();
            }
            this.title = l;
            this.entries = new ArrayList<TreeEntry>();
            PANELS.put(class1, this);
        }

        public SingletonPanel getPanel() {
            return panel;
        }

        public void setPanel(SingletonPanel panel) {
            this.panel = panel;
        }

        public ArrayList<TreeEntry> getEntries() {
            return entries;
        }

        public int indexOf(Object child) {
            return entries.indexOf(child);
        }

        public int size() {
            return entries.size();
        }

        public Object get(int index) {

            return entries.get(index);
        }

        public void add(TreeEntry treeEntry) {
            entries.add(treeEntry);
        }

        public TreeEntry(String l) {
            this((Class<? extends SwitchPanel>) null, l);
        }

        /**
         * Adds a configpanel
         * 
         * @param panel
         * @param host
         */
        public TreeEntry(ConfigPanel panel, String host) {
            this.panel = new SingletonPanel(panel);
            this.title = host;
            this.entries = new ArrayList<TreeEntry>();
        }
    }

    private void fireTreeStructureChanged(TreePath path) {

        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {

                if (e == null) e = new TreeModelEvent(this, path);
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    /**
     * Is called to update The Addons subtree after changes
     * 
     * @return
     */
    public TreePath updateAddons() {
        addons.getEntries().clear();
        initExtensions(addons);
        TreePath path = new TreePath(new Object[] { getRoot(), plugins, addons });
        fireTreeStructureChanged(path);
        return path;
    }
}
