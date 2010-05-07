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

package jd.gui.swing.jdgui.views.settings.sidebar;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.OptionalPluginWrapper;
import jd.utils.locale.JDL;

public class ConfigTreeModel implements TreeModel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.ConfigTreeModel.";

    private TreeEntry root;
    /** Listeners. */
    protected EventListenerList listenerList = new EventListenerList();
    private TreeEntry addons;

    private TreeEntry plugins;

    public ConfigTreeModel() {
        root = new TreeEntry("_ROOT_", null);

        TreeEntry teTop, teLeaf;
        root.add(teTop = new TreeEntry(JDL.L(JDL_PREFIX + "basics.title", "Basics"), "gui.images.config.home"));

        teTop.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral.class, jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral.getTitle(), "gui.images.config.home"));

        teTop.add(teLeaf = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.General.class, jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.General.getTitle(), "gui.images.config.network_local"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.InternetAndNetwork.class, jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.InternetAndNetwork.getTitle(), "gui.images.networkerror"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.Advanced.class, jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.Advanced.getTitle(), "gui.images.network"));

        teTop.add(teLeaf = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.General.class, jd.gui.swing.jdgui.views.settings.panels.gui.General.getTitle(), "gui.images.config.gui"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.ToolbarController.class, jd.gui.swing.jdgui.views.settings.panels.gui.ToolbarController.getTitle(), "gui.images.toolbar"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.Linkgrabber.class, jd.gui.swing.jdgui.views.settings.panels.gui.Linkgrabber.getTitle(), "gui.images.taskpanes.linkgrabber"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.Browser.class, jd.gui.swing.jdgui.views.settings.panels.gui.Browser.getTitle(), "gui.images.config.host"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.Advanced.class, jd.gui.swing.jdgui.views.settings.panels.gui.Advanced.getTitle(), "gui.images.container"));

        root.add(teTop = new TreeEntry(JDL.L(JDL_PREFIX + "modules.title", "Modules"), "gui.images.config.home"));

        teTop.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.ConfigPanelCaptcha.class, jd.gui.swing.jdgui.views.settings.panels.ConfigPanelCaptcha.getTitle(), "gui.images.config.ocr"));

        teTop.add(teLeaf = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.reconnect.MethodSelection.class, jd.gui.swing.jdgui.views.settings.panels.reconnect.MethodSelection.getTitle(), "gui.images.config.reconnect"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.reconnect.Advanced.class, jd.gui.swing.jdgui.views.settings.panels.reconnect.Advanced.getTitle(), "gui.images.reconnect_settings"));

        teTop.add(teLeaf = new TreeEntry(JDL.L(JDL_PREFIX + "passwordsAndLogins", "Passwords & Logins"), "gui.images.list"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordList.class, jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordList.getTitle(), "gui.images.addons.unrar"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordListHTAccess.class, jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordListHTAccess.getTitle(), "gui.images.htaccess"));

        root.add(plugins = new TreeEntry(JDL.L(JDL_PREFIX + "plugins.title", "Plugins & Add-ons"), "gui.images.config.packagemanager"));

        plugins.add(teLeaf = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.hoster.ConfigPanelPluginForHost.class, jd.gui.swing.jdgui.views.settings.panels.hoster.ConfigPanelPluginForHost.getTitle(), "gui.images.config.host"));

        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.premium.Premium.class, jd.gui.swing.jdgui.views.settings.panels.premium.Premium.getTitle(), "gui.images.premium"));

        plugins.add(addons = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons.class, jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons.getTitle(), "gui.images.config.packagemanager"));

        initExtensions(addons);
    }

    private void initExtensions(TreeEntry addons) {
        for (final OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!plg.isLoaded() || !plg.isEnabled() || !plg.hasConfig()) continue;
            addons.add(new TreeEntry(AddonConfig.getInstance(plg.getPlugin().getConfig(), ""), plg.getHost(), plg.getPlugin().getIconKey()));
        }
    }

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     * 
     * @see #removeTreeModelListener(TreeModelListener)
     * @param l
     *            the listener to add
     */
    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener previously added with
     * {@link #addTreeModelListener(TreeModelListener)}.
     * 
     * @see #addTreeModelListener(TreeModelListener)
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
