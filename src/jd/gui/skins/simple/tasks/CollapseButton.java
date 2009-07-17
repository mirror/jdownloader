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

package jd.gui.skins.simple.tasks;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import jd.gui.skins.jdgui.borders.JDBorderFactory;
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
        add(button, "width 165!,hidemode 3");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (!collapsible.isVisible()) {
                    collapsible.setVisible(true);
                    button.setVisible(false);
                    CollapseButton.this.setBorder(JDBorderFactory.createLineTitleBorder(button.getIcon(), button.getText()));

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
        button.setVisible(b);
        this.setBorder(b ? null : JDBorderFactory.createLineTitleBorder(button.getIcon(), button.getText()));
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
