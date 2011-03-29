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

import jd.controlling.reconnect.ReconnectPluginConfigGUI;
import jd.plugins.optional.ExtensionController;
import jd.plugins.optional.PluginOptional;
import jd.utils.locale.JDL;


public class ConfigTreeModel implements TreeModel {
    private static final String JDL_PREFIX   = "jd.gui.swing.jdgui.views.ConfigTreeModel.";

    private final TreeEntry     root;
    /** Listeners. */
    protected EventListenerList listenerList = new EventListenerList();
    private TreeEntry           addons;

    private TreeEntry           plugins;

    public ConfigTreeModel() {
        this.root = new TreeEntry("_ROOT_", null, null);

        TreeEntry teTop, teLeaf;
        this.root.add(teTop = new TreeEntry(JDL.L(ConfigTreeModel.JDL_PREFIX + "basics.title", "Basics"), "gui.images.config.home"));

        teTop.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral.class));

        teTop.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.General.class));

        teTop.add(teLeaf = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.General.class));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.ToolbarController.class));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.Linkgrabber.class));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.Browser.class));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.gui.Advanced.class));

        this.root.add(teTop = new TreeEntry(JDL.L(ConfigTreeModel.JDL_PREFIX + "modules.title", "Modules"), "gui.images.config.home"));

        teTop.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.ConfigPanelCaptcha.class));

        teTop.add(teLeaf = new TreeEntry(ReconnectPluginConfigGUI.getInstance(), JDL.L("jd.controlling.reconnect.plugins.ReconnectPluginConfigGUI.sidebar.title", "Reconnection"), "gui.images.config.reconnect"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.reconnect.Advanced.class));

        teTop.add(teLeaf = new TreeEntry(JDL.L(ConfigTreeModel.JDL_PREFIX + "passwordsAndLogins", "Passwords & Logins"), "gui.images.list"));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordList.class));
        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordListHTAccess.class));

        this.root.add(this.plugins = new TreeEntry(JDL.L(ConfigTreeModel.JDL_PREFIX + "plugins.title", "Plugins & Add-ons"), "gui.images.config.home"));

        this.plugins.add(teLeaf = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.hoster.ConfigPanelPluginForHost.class));

        teLeaf.add(new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.premium.Premium.class));

        this.plugins.add(this.addons = new TreeEntry(jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons.class));

        this.initExtensions(this.addons);
    }

    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     * 
     * @see #removeTreeModelListener(TreeModelListener)
     * @param l
     *            the listener to add
     */
    public void addTreeModelListener(final TreeModelListener l) {
        this.listenerList.add(TreeModelListener.class, l);
    }

    private void fireTreeStructureChanged(final TreePath path) {

        final Object[] listeners = this.listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(this, path);
                }
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    public Object getChild(final Object parent, final int index) {
        return ((TreeEntry) parent).get(index);
    }

    public int getChildCount(final Object parent) {
        return ((TreeEntry) parent).size();
    }

    public int getIndexOfChild(final Object parent, final Object child) {
        return ((TreeEntry) parent).indexOf(child);
    }

    public Object getRoot() {
        return this.root;
    }

    private void initExtensions(final TreeEntry addons) {
        for (final PluginOptional plg : ExtensionController.getInstance().getExtensions()) {
            if (!plg.isRunning() || (!plg.hasSettings() && !plg.hasConfigPanel())) {
                continue;
            }
            if (plg.hasConfigPanel()) {
                addons.add(new TreeEntry(plg.getConfigPanel(), plg.getName(), plg.getIcon(16), plg.getIcon(20)));
            } else {
                addons.add(new TreeEntry(AddonConfig.getInstance(plg.getSettings(), "", true), plg.getName(), plg.getIcon(16), plg.getIcon(20)));
            }
        }
    }

    public boolean isLeaf(final Object node) {
        return ((TreeEntry) node).size() == 0;
    }

    /**
     * Removes a listener previously added with
     * {@link #addTreeModelListener(TreeModelListener)}.
     * 
     * @see #addTreeModelListener(TreeModelListener)
     * @param l
     *            the listener to remove
     */
    public void removeTreeModelListener(final TreeModelListener l) {
        this.listenerList.remove(TreeModelListener.class, l);
    }

    /**
     * Is called to update The Addons subtree after changes
     * 
     * @return
     */
    public TreePath updateAddons() {
        this.addons.getEntries().clear();
        this.initExtensions(this.addons);
        final TreePath path = new TreePath(new Object[] { this.getRoot(), this.plugins, this.addons });
        this.fireTreeStructureChanged(path);
        return path;
    }

    public void valueForPathChanged(final TreePath path, final Object newValue) {
    }
}
