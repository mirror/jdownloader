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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.interfaces.View;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.SwingUtils;

public class TabHeader extends JPanel implements CustomTabHeader {

    private static final long serialVersionUID = 4463352125800695922L;
    private boolean           selected;
    private JLabel            label;
    private Font              fontUnselected;
    private Font              fontSelected;
    private int               maxWidth;
    private JLabel            labelIcon;

    public void setBounds(int x, int y, int width, int height) {
        // workaround to have proper pixel exact layouting of the tab header
        // http://svn.jdownloader.org/issues/43349
        if (selected) {
            super.setBounds(x - 2, y + 1, width + 2, height - 1);
        } else {
            super.setBounds(x - 2, y - 1, width + 2, height + 1);
        }

    }

    public TabHeader(final View view) {
        setLayout(new MigLayout("ins 0 0 0 0", "[][grow]", "0[22!]0"));
        setOpaque(false);
        setToolTipText(view.getTooltip());
        initMouseForwarder();

        putClientProperty("paintActive", Boolean.TRUE);
        labelIcon = new JLabel();
        labelIcon.setIcon(view.getIcon());
        SwingUtils.setOpaque(labelIcon, false);

        label = new JLabel(view.getTitle());

        SwingUtils.setOpaque(label, false);
        fontUnselected = label.getFont();

        fontSelected = fontUnselected.deriveFont(fontUnselected.getStyle() ^ Font.BOLD);
        add(labelIcon);
        add(label, "alignx center");
        label.setFont(fontSelected);
        Dimension pref = getPreferredSize();
        maxWidth = pref.width;
        setPreferredSize(new Dimension(maxWidth, pref.height));
        label.setFont(fontUnselected);

    }

    protected void initMouseForwarder() {
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
                // if (e.getButton() == MouseEvent.BUTTON2 || (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2)) {
                // view.close();
                // } else {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
                // }
            }

        });
    }

    public void setHidden() {
        selected = false;
        label.setFont(fontUnselected);
        repaint();
    }

    public void setShown() {
        selected = true;
        label.setFont(fontSelected);
        repaint();
    }

}
