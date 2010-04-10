package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.SingletonPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.utils.JDTheme;

public class TreeEntry {

    private static final HashMap<Class<? extends SwitchPanel>, TreeEntry> PANELS = new HashMap<Class<? extends SwitchPanel>, TreeEntry>();

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

    private Class<? extends SwitchPanel> clazz;
    private SwitchPanel panel;

    private String title;
    private ImageIcon iconSmall;
    private ImageIcon icon;
    private ArrayList<TreeEntry> entries;

    public TreeEntry(String title, String iconKey) {
        this.title = title;
        if (iconKey != null) {
            this.iconSmall = JDTheme.II(iconKey, 16, 16);
            this.icon = JDTheme.II(iconKey, 20, 20);
        }
        this.entries = new ArrayList<TreeEntry>();
    }

    /**
     * Adds a configpanel
     * 
     * @param panel
     * @param title
     * @param iconKey
     */
    public TreeEntry(SwitchPanel panel, String title, String iconKey) {
        this(title, iconKey);

        this.panel = panel;
    }

    public TreeEntry(final Class<? extends SwitchPanel> clazz, String title, String iconKey) {
        this(title, iconKey);

        PANELS.put(clazz, this);

        this.clazz = clazz;
        this.panel = new SingletonPanel(clazz).getPanel();
    }

    public Class<? extends SwitchPanel> getClazz() {
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

    public SwitchPanel getPanel() {
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
