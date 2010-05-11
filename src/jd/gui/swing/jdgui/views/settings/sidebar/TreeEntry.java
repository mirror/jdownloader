package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;

public class TreeEntry {

    private static final HashMap<Class<? extends ConfigPanel>, TreeEntry> PANELS = new HashMap<Class<? extends ConfigPanel>, TreeEntry>();

    /**
     * Returns the TreeEntry to a class if it has been added using the
     * {@link TreeEntry#TreeEntry(Class, String)} constructor
     * 
     * @param clazz
     * @return
     */
    public static TreeEntry getTreeByClass(Class<?> clazz) {
        return PANELS.get(clazz);
    }

    private Class<? extends ConfigPanel> clazz;
    private ConfigPanel panel;

    private String title;
    private ImageIcon iconSmall;
    private ImageIcon icon;
    private ArrayList<TreeEntry> entries;

    private TreeEntry() {
        this.entries = new ArrayList<TreeEntry>();
    }

    public TreeEntry(String title, String iconKey) {
        this();
        setTitle(title);
        setIcon(iconKey);
    }

    /**
     * Adds a configpanel
     * 
     * @param panel
     * @param title
     * @param iconKey
     */
    public TreeEntry(ConfigPanel panel, String title, String iconKey) {
        this(title, iconKey);

        this.panel = panel;
    }

    /**
     * Creates a new TreeEntry. The title and the iconKey will be obtained via
     * the <code>public static String</code> methods <code>getTitle()</code> and
     * <code>getIconKey()</code>.
     * 
     * @param clazz
     */
    public TreeEntry(final Class<? extends ConfigPanel> clazz) {
        this();

        try {
            setTitle(clazz.getMethod("getTitle").invoke(null).toString());
        } catch (Exception e) {
            e.printStackTrace();
            setTitle(clazz.getSimpleName());
        }

        try {
            setIcon(clazz.getMethod("getIconKey").invoke(null).toString());
        } catch (Exception e) {
            e.printStackTrace();
            setIcon("gui.images.taskpanes.configuration");
        }

        PANELS.put(clazz, this);

        this.clazz = clazz;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setIcon(String iconKey) {
        if (iconKey != null) {
            this.iconSmall = JDTheme.II(iconKey, 16, 16);
            this.icon = JDTheme.II(iconKey, 20, 20);
        }
    }

    public Class<? extends ConfigPanel> getClazz() {
        return clazz;
    }

    public String getTitle() {
        return title;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public ImageIcon getIconSmall() {
        return iconSmall;
    }

    public ConfigPanel getPanel() {
        if (panel == null && clazz != null) {
            panel = (ConfigPanel) new SingletonPanel(clazz).getPanel();
        }
        return panel;
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

}
