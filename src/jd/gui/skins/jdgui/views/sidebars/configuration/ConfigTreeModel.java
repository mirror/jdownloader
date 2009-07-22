package jd.gui.skins.jdgui.views.sidebars.configuration;

import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelAddons;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelCaptcha;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelDownload;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelGUI;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelGeneral;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelPluginForHost;
import jd.gui.skins.jdgui.settings.panels.ConfigPanelReconnect;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SingletonPanel;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class ConfigTreeModel implements TreeModel {
    private static final String JDL_PREFIX = "jd.gui.skins.jdgui.views.ConfigTreeModel.";

    private TreeEntry root;


    public ConfigTreeModel() {
        this.root = new TreeEntry(JDL.L(JDL_PREFIX + "configuration.title", "Settings"));

        TreeEntry basics, modules, plugins;
        root.add(basics = new TreeEntry(JDL.L(JDL_PREFIX + "basics.title", "Basics")).setIcon("gui.images.config.home"));
        basics.add(new TreeEntry(ConfigPanelGeneral.class, JDL.L(JDL_PREFIX + "general.title", "General")).setIcon("gui.images.config.home"));
        basics.add(new TreeEntry(ConfigPanelDownload.class, JDL.L(JDL_PREFIX + "download.title", "Download & Network")).setIcon("gui.images.config.network_local"));
        basics.add(new TreeEntry(ConfigPanelGUI.class, JDL.L(JDL_PREFIX + "gui.title", "User Interface")).setIcon("gui.images.config.gui"));
        root.add(modules = new TreeEntry(JDL.L(JDL_PREFIX + "modules.title", "Modules")).setIcon("gui.images.config.home"));

        modules.add(new TreeEntry(ConfigPanelCaptcha.class, JDL.L(JDL_PREFIX + "captcha.title", "OCR")).setIcon("gui.images.config.ocr"));
        modules.add(new TreeEntry(ConfigPanelReconnect.class, JDL.L(JDL_PREFIX + "reconnect.title", "Reconnection")).setIcon("gui.images.config.reconnect"));
        root.add(plugins = new TreeEntry(JDL.L(JDL_PREFIX + "plugins.title", "Plugins & Add-ons")).setIcon("gui.images.config.packagemanager"));
        plugins.add(new TreeEntry(ConfigPanelPluginForHost.class, JDL.L(JDL_PREFIX + "host.title", "Hoster & Premium")).setIcon("gui.images.config.host"));
        plugins.add(new TreeEntry(ConfigPanelAddons.class, JDL.L(JDL_PREFIX + "addons.title", "Extensions")).setIcon("gui.images.config.packagemanager"));

      
    }

  

    public void addTreeModelListener(TreeModelListener l) {    }

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

    public void removeTreeModelListener(TreeModelListener l) {}

    public void valueForPathChanged(TreePath path, Object newValue) {
        // TODO Auto-generated method stub

    }

    class TreeEntry {

        private Class<? extends SwitchPanel> clazz;
        private String title;
        private ImageIcon icon;

        public Class<? extends SwitchPanel> getClazz() {
            return clazz;
        }

        public TreeEntry setIcon(String string) {
            // TODO Auto-generated method stub
             icon=JDTheme.II(string,16,16);
             return this;
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

        public TreeEntry(final Class<? extends SwitchPanel> class1, String l) {
            this.clazz = class1;

            if (class1 != null) {
                panel = new SingletonPanel(class1, JDUtilities.getConfiguration());
                // init this panel in an extra thread..
                new Thread() {
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
            // TODO Auto-generated method stub
            return entries.indexOf(child);
        }

        public int size() {
            // TODO Auto-generated method stub
            return entries.size();
        }

        public Object get(int index) {
            // TODO Auto-generated method stub
            return entries.get(index);
        }

        public void add(TreeEntry treeEntry) {
            entries.add(treeEntry);

        }

        public TreeEntry(String l) {
            this(null, l);
        }

    }

}
