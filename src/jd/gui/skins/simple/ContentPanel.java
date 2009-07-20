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

package jd.gui.skins.simple;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JViewport;

import jd.gui.skins.jdgui.components.JDCollapser;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

public class ContentPanel extends JPanel {
    private static final Object LOCK = new Object();
    private static final long serialVersionUID = 1606909731977454208L;
    private SwitchPanel rightPanel = null;
    private JViewport viewport;

    // public static ContentPanel PANEL;

    public ContentPanel() {

        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));

    }

    public void display(SwitchPanel panel) {
        synchronized (LOCK) {
            // System.out.println(panel);
            // new Exception().printStackTrace();
            if (rightPanel == panel) {

            return; }

            JDCollapser.getInstance().setCollapsed(true);
            if (rightPanel != null) {

                this.remove(viewport);
                this.remove(rightPanel);
                rightPanel.setEnabled(false);
                rightPanel.setVisible(false);

                rightPanel.hide();
            }
            rightPanel = panel;

            if (rightPanel.needsViewport()) {
                viewport.setView(rightPanel);
                this.add(viewport, "cell 0 0");

            } else {
                this.add(rightPanel, "cell 0 0");
            }
            rightPanel.setEnabled(true);
            rightPanel.setVisible(true);
            rightPanel.show();
            this.setPreferredSize(new Dimension(100, 10));
            this.revalidate();
            this.repaint();
        }
    }

    public SwitchPanel getRightPanel() {
        return rightPanel;
    }

}
