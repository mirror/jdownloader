package jd.gui.skins.simple;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

public class TreeTabbedNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 4915789292173918601L;
    private static final int MAX_ICON_HEIGHT = 16;
    private String name = null;

    private Icon icon;
    private LeafPanelWrapper leafWrapper;
    private int displayed = 0;

    public TreeTabbedNode(String name) {
        this(null, name, null);

    }

    public TreeTabbedNode(JTabbedPanel panel, String name, ImageIcon ii) {
        this.name = name;

        this.leafWrapper = new LeafPanelWrapper(panel);
        if (ii != null) {
            if (ii.getIconHeight() > MAX_ICON_HEIGHT) {

                ii = new ImageIcon(ii.getImage().getScaledInstance(MAX_ICON_HEIGHT, MAX_ICON_HEIGHT, Image.SCALE_SMOOTH));
            }
        }
        icon = ii;
    }

    public TreeTabbedNode() {
        this(null, null, null);
    }

    public TreeTabbedNode(JTabbedPanel panel, String name) {
        this(panel, name, null);

    }

    public TreeTabbedNode(String name, ImageIcon ii) {
        this(null, name, ii);
    }

    public TreeTabbedNode(Class<?> clazz, Object[] objects, String name, ImageIcon ii) {
        this(name, ii);
        this.leafWrapper = new LeafPanelWrapper(clazz, objects);
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public JTabbedPanel getPanel() {
        return this.leafWrapper.getPanel();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public Icon getIcon() {
        // TODO Auto-generated method stub
        return icon;
    }

    public int increaseShowCounter() {
        displayed++;
        return displayed;
    }

}
