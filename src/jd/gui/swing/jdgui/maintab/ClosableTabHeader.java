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

import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.views.ClosableView;
import net.miginfocom.swing.MigLayout;

public class ClosableTabHeader extends JPanel {

    private static final long serialVersionUID = 4463352125800695922L;

    public ClosableTabHeader(final ClosableView view) {
        setLayout(new MigLayout("ins 0", "[grow,fill]push[]", "[16!]"));
        setOpaque(false);
        setToolTipText(view.getTooltip());
        addMouseListener(new JDMouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 || (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)) {
                    view.close();
                } else {
                    JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
                }
            }

        });

        putClientProperty("paintActive", Boolean.TRUE);

        final JLabel label = new JLabel(view.getTitle());
        label.setIcon(view.getIcon());
        label.setOpaque(false);

        add(label);
        add(view.getCloseButton(), "gapleft 5, h pref!, w pref!");
    }

}
