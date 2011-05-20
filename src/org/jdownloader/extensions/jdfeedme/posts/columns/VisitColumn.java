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

package org.jdownloader.extensions.jdfeedme.posts.columns;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.SwingConstants;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;

import org.appwork.utils.os.CrossSystem;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.extensions.jdfeedme.posts.JDFeedMePost;
import org.jdownloader.images.NewTheme;

public class VisitColumn extends JDTableColumn {

    private static final long serialVersionUID = 8660656283327573254L;

    private JRendererLabel    labelRend;
    private JRendererLabel    labelLink;
    private Object            obj;

    public VisitColumn(String name, JDTableModel table) {
        super(name, table);

        labelRend = new JRendererLabel();
        labelRend.setBorder(null);
        labelRend.setHorizontalAlignment(SwingConstants.CENTER);
        labelRend.setIcon(NewTheme.I().getIcon("browse", 16));
        labelRend.setToolTipText("Open post in browser");
        labelRend.setOpaque(false);

        labelLink = new JRendererLabel();
        labelLink.setBorder(null);
        labelLink.setHorizontalAlignment(SwingConstants.CENTER);
        labelLink.setIcon(NewTheme.I().getIcon("browse", 16));
        labelLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        labelLink.setOpaque(false);

        labelLink.addMouseListener(new JDMouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent evt) {

            }

            @Override
            public void mouseExited(MouseEvent evt) {
            }

            public void mouseClicked(MouseEvent e) {
                actionPerformed();
            }

        });
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        this.obj = value;
        return labelLink;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return labelRend;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed() {
        JDFeedMePost post = ((JDFeedMePost) this.obj);
        CrossSystem.openURLOrShowMessage(post.getLink());
        this.fireEditingStopped();
    }

    @Override
    protected int getMaxWidth() {
        return 50;
    }

}