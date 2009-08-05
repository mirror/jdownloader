package jd.gui.swing.jdgui.views.sidebars.configuration;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import jd.OptionalPluginWrapper;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons;
import jd.gui.swing.jdgui.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.settings.panels.ConfigPanelPluginForHost;
import jd.gui.swing.jdgui.settings.panels.premium.Premium;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class ConfigTreeModel implements TreeModel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.ConfigTreeModel.";

    private TreeEntry root;

    private TreeEntry addons;

    public ConfigTreeModel() {
        this.root = new TreeEntry(JDL.L(JDL_PREFIX + "configuration.title", "Settings"));

        TreeEntry basics, modules, plugins;
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
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.Linkgrabber.class, jd.gui.swing.jdgui.settings.panels.gui.Linkgrabber.getTitle()).setIcon("gui.images.taskpanes.linkgrabber"));
        //

        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.Browser.class, jd.gui.swing.jdgui.settings.panels.gui.Browser.getTitle()).setIcon("gui.images.config.host"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.gui.Advanced.class, jd.gui.swing.jdgui.settings.panels.gui.Advanced.getTitle()).setIcon("gui.images.container"));

        root.add(modules = new TreeEntry(JDL.L(JDL_PREFIX + "modules.title", "Modules")).setIcon("gui.images.config.home"));

        modules.add(dl = new TreeEntry(jd.gui.swing.jdgui.settings.panels.ocr.General.class, jd.gui.swing.jdgui.settings.panels.ocr.General.getTitle()).setIcon("gui.images.config.ocr"));

        // dl.add(new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.ocr.General.class,
        // JDL.L(JDL_PREFIX + "captcha.general.title",
        // "Method List")).setIcon("gui.images.config.ocr"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.ocr.Advanced.class, jd.gui.swing.jdgui.settings.panels.ocr.Advanced.getTitle()).setIcon("gui.images.config.ocr"));

        modules.add(dl = new TreeEntry(jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.class, jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.getTitle()).setIcon("gui.images.config.reconnect"));
        // dl.add(new
        // TreeEntry(jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.class,
        // JDL.L(JDL_PREFIX + "reconnect.methodselection.title",
        // "Reconnect")).setIcon("gui.images.config.reconnect"));
        dl.add(new TreeEntry(jd.gui.swing.jdgui.settings.panels.reconnect.Advanced.class, jd.gui.swing.jdgui.settings.panels.reconnect.Advanced.getTitle()).setIcon("gui.images.reconnect_settings"));

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

            addons2.add(new TreeEntry(new AddonConfig(plg), plg.getHost()).setIcon(plg.getPlugin().getIconKey()));
        }

    }

    public void addTreeModelListener(TreeModelListener l) {
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

    public void removeTreeModelListener(TreeModelListener l) {
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        // TODO Auto-generated method stub

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
            // TODO Auto-generated method stub
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
        public static TreeEntry getTreeByClass(Class<? extends SwitchPanel> cl) {
            return PANELS.get(cl);
        }

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

}
