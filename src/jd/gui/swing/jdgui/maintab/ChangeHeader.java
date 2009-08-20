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

package jd.gui.swing.jdgui.maintab;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.interfaces.View;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class ChangeHeader extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 4463352125800695922L;
    private JLabel change;
    private JButton closeIcon;
    private Action action;

    public ChangeHeader(View view) {
        setLayout(new MigLayout("ins 0", "[grow,fill]"));
        JLabel l1 = new JLabel(view.getTitle());
        change = new JLabel("*");
        closeIcon = new JButton(JDTheme.II("gui.tab.close", 12, 12));
        closeIcon.setContentAreaFilled(false);
        // closeIcon.setBorderPainted(false);

        closeIcon.setText(null);
        closeIcon.setVisible(false);
        putClientProperty("paintActive", Boolean.TRUE);
        l1.setIcon(view.getIcon());
        add(l1);
        add(change, "dock west,hidemode 3");
        add(closeIcon, "dock east, hidemode 3,gapleft 5,height 16!, width 16!");
        change.setVisible(false);
        setOpaque(false);
        l1.setOpaque(false);
        change.setOpaque(false);
    }

    /**
     * enables or disables the changed mark
     * 
     * @param b
     */
    public void setChanged(boolean b) {
        change.setVisible(b);
    }

    /**
     * ENables the close button by adding an action
     * 
     * @param a
     */
    public void setCloseEnabled(Action a) {
        if (action != null) {
            closeIcon.removeActionListener(action);
        }
        if (a != null) {
            action = a;
            closeIcon.addActionListener(a);
            closeIcon.setVisible(true);
        } else {
            action = null;
            closeIcon.setVisible(false);
        }

    }

}
