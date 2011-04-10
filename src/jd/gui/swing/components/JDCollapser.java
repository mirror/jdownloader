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

package jd.gui.swing.components;

import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.borders.JDBorderFactory;
import jd.gui.swing.jdgui.interfaces.DroppedPanel;
import jd.nutils.JDImage;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.translate.T;

/**
 * class for an infopanel with close button.
 * 
 * @author Coalado
 */
public abstract class JDCollapser extends DroppedPanel {

    private static final long serialVersionUID = 6864885344815243560L;

    protected JMenuBar        menubar;
    private JDCloseButton     closeButton;
    protected JLabel          menutitle;

    protected JPanel          content;

    protected JDCollapser() {
        super();

        Box panel = new Box(1);
        panel.add(closeButton = new JDCloseButton() {
            private static final long serialVersionUID = -5490387734487354668L;

            public void actionPerformed(ActionEvent e) {
                JDCollapser.this.onClosed();
            }
        });
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 5));

        menubar = new JMenuBar();
        menubar.setBorder(JDBorderFactory.createInsideShadowBorder(0, 0, 1, 0));
        menubar.add(menutitle = new JLabel(""));
        menubar.add(Box.createHorizontalGlue());
        menubar.add(panel);

        content = new JPanel();
        content.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]"));

        setLayout(new MigLayout("ins 0 5 0 0, wrap 1", "[fill,grow]", "[fill,grow]"));
        add(menubar, "dock NORTH, height " + Math.max(closeButton.getIconHeight() + 3, 18) + "!, gapbottom 2, growx");
        add(content);
        setVisible(true);
    }

    public void setInfos(String name, ImageIcon icon) {
        menutitle.setText(name);

        try {
            menutitle.setIcon(JDImage.getScaledImageIcon(icon, 16, 16));
        } catch (Exception e) {
            menutitle.setIcon(icon);
        }

        closeButton.setToolTipText(T._.jd_gui_swing_components_JDCollapser_closetooltip(name));
    }

    abstract public void onClosed();

    public JPanel getContent() {
        return content;
    }

}