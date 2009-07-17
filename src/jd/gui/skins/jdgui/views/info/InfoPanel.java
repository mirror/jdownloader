package jd.gui.skins.jdgui.views.info;

import java.awt.Color;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.skins.simple.components.borders.JDBorderFactory;
import net.miginfocom.swing.MigLayout;

public class InfoPanel extends JPanel {
    private JLabel iconContainer;
//    private JLabel nameContainer;
//    private JLabel descContainer;

    private Color valueColor;
    private Color titleColor;
    private HashMap<String, JComponent> map;

    public InfoPanel() {

        this.setBorder(JDBorderFactory.createInsideShadowBorder(5, 0, 0, 0));
        map = new HashMap<String, JComponent>();
        this.setLayout(new MigLayout("ins 5", "[]5[]", "[][]"));
        valueColor = getBackground().darker().darker().darker().darker().darker();
        titleColor = getBackground().darker().darker();
        this.iconContainer = new JLabel();
        add(iconContainer, "spany 2,cell 0 0,gapleft 1");
//        this.nameContainer = new JLabel("Infopane");
//        add(nameContainer, "gapleft 10,cell 1 0");
//        nameContainer.setForeground(valueColor);
//        this.descContainer = new JLabel("");
//        add(descContainer, "gapleft 10,cell 1 1");
//        descContainer.setForeground(titleColor);

    }

    /**
     * UPdates an entry previously added my addInfoEntry. Use as key the
     * previously used title
     * 
     * @param key
     * @param string
     */
    protected void updateInfo(String key, Object value) {
        JComponent c = map.get(key);

        if (c instanceof JLabel) {
            ((JLabel) c).setText(value.toString());

        }

    }

    /**
     * Ads an info entry at x ,y title has to be constanz and value may be
     * updated later by using updateInfo(..)
     * 
     * @param title
     * @param value
     * @param x
     * @param y
     */
    protected void addInfoEntry(String title, Object value, int x, int y) {
        x *= 2;
        x += 1;
        JLabel myTitle = new JLabel(title + ":");
        myTitle.setForeground(titleColor);
        JLabel myValue = new JLabel(value.toString());
        myValue.setForeground(valueColor);
        add(myTitle, "gapleft 20,alignx right,cell " + x + " " + y);
        add(myValue, "cell " + (x + 1) + " " + y);
        map.put(title, myValue);
    }

  

    protected void setIcon(ImageIcon ii) {
        iconContainer.setIcon(ii);

    }
}
