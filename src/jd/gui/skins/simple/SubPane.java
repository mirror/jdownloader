package jd.gui.skins.simple;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jd.gui.skins.jdgui.borders.LineTitleBorder;
import net.miginfocom.swing.MigLayout;

public class SubPane extends JScrollPane {

    private static final long serialVersionUID = -1727950693360506752L;

    public SubPane(String l, ImageIcon ii) {
        super(new JPanel(new MigLayout("ins 0 8 5 5, wrap 1", "[fill,grow]","[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]")));
        this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // this.setBorder(JDBorderFactory.createTitleBorder(ii, l, 0, 0));
        this.setBorder(new LineTitleBorder(ii, l, 2));
    }

    public JPanel getContentPanel() {
        return (JPanel) this.getViewport().getView();
    }

    public void add(JComponent c) {
        getContentPanel().add(c);

    }

    public void add(JComponent c, Object constraints) {
        getContentPanel().add(c, constraints);

    }
}
