package jd.gui.skins.simple.tasks;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import jd.gui.skins.simple.Factory;
import net.miginfocom.swing.MigLayout;

public class CollapseButton extends JPanel {

    private static final long serialVersionUID = -4630190465173892024L;
    private JButton button;
    private JPanel collapsible;

    public CollapseButton(String host, ImageIcon ii) {
        this.setLayout(new MigLayout("ins 0,wrap 1, gap 0 0", "grow,fill"));
        this.setOpaque(false);
        this.setBackground(null);
      
        button = createButton(host, ii);
        add(button,"width 165!");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (!collapsible.isVisible()) {
                    collapsible.setVisible(true);
                    CollapseButton.this.setBorder(BorderFactory.createLineBorder(getBackground().darker()));

                } else {
                    // collapsible.setCollapsed(true);
                }

            }

        });
        collapsible = new JPanel();
        collapsible.setVisible(false);
        collapsible.setLayout(new MigLayout("ins 0,wrap 1,gap 0 0", "grow,fill"));

        add(collapsible, "hidemode 3,gapbottom 10, width 165!");

    }

    public void setCollapsed(boolean b) {
        collapsible.setVisible(!b);
        this.setBorder(b ? null : BorderFactory.createLineBorder(getBackground().darker()));
    }

    public Container getContentPane() {
        return collapsible;
    }

    private JButton createButton(String string, Icon i) {
        return Factory.createButton(string, i);
    }

    public JButton getButton() {
        return button;
    }

}
