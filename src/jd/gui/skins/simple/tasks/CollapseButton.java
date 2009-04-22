package jd.gui.skins.simple.tasks;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import jd.gui.skins.simple.Factory;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;

public class CollapseButton extends JPanel {

    private static final long serialVersionUID = -4630190465173892024L;
    private JButton button;
    private JXCollapsiblePane collapsible;

    public CollapseButton(String host, ImageIcon ii) {
        this.setLayout(new MigLayout("ins 0,wrap 1, gap 0 0", "grow,fill"));
        this.setOpaque(false);
        this.setBackground(null);

        button = createButton(host, ii);
        add(button);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (collapsible.isCollapsed()) {
                    collapsible.setCollapsed(false);
                } else {
                    // collapsible.setCollapsed(true);
                }

            }

        });
        collapsible = new JXCollapsiblePane();
        collapsible.setCollapsed(true);
        collapsible.setLayout(new MigLayout("ins 0,wrap 1,gap 0 0", "grow,fill"));
        add(collapsible);

    }

    public void setCollapsed(boolean b) {
        collapsible.setCollapsed(b);
    }

    public Container getContentPane() {
        return collapsible.getContentPane();
    }

    private JButton createButton(String string, Icon i) {
        return Factory.createButton(string, i);
    }

    public JButton getButton() {
        return button;
    }

}
