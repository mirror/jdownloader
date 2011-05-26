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

package jd.gui.swing.jdgui.views.downloads.columns;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.Border;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.StatusLabel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.images.NewTheme;

public class FileColumn extends JDTableColumn {

    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink      dLink;
    private Border            leftGap;
    private ImageIcon         icon_fp_open;
    private ImageIcon         icon_fp_open_error;
    private ImageIcon         icon_fp_closed;
    private ImageIcon         icon_fp_closed_error;
    private ImageIcon         imgFileFailed;
    private FilePackage       fp;
    private StatusLabel       jlr;

    public FileColumn(String name, JDTableModel table) {
        super(name, table);
        leftGap = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        icon_fp_open = NewTheme.I().getIcon("tree_package_open", 32);
        icon_fp_open_error = NewTheme.I().getIcon("tree_package_open_error", 32);
        icon_fp_closed = NewTheme.I().getIcon("tree_package_closed", 32);
        icon_fp_closed_error = NewTheme.I().getIcon("tree_package_closed_error" + "", 32);
        imgFileFailed = NewTheme.I().getIcon("file_error", 16);
        jlr = new StatusLabel();
        jlr.setBorder(null);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            if (fp.getLinksFailed() > 0) {
                jlr.setText(fp.getName(), !fp.isExpanded() ? icon_fp_closed_error : icon_fp_open_error);
            } else {
                jlr.setText(fp.getName(), !fp.isExpanded() ? icon_fp_closed : icon_fp_open);
            }
            jlr.setIcon(0, null, fp.getFilePackageInfo().getSize(), null);
            jlr.clearIcons(1);
            jlr.setBorder(null);
        } else {
            dLink = (DownloadLink) value;
            if (dLink.getLinkStatus().isFailed()) {
                jlr.setText(dLink.getName(), imgFileFailed);
            } else {
                jlr.setText(dLink.getName(), dLink.getIcon());
            }
            jlr.clearIcons(0);
            jlr.setBorder(leftGap);
        }
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        /*
         * DownloadView hat nur null(Header) oder ne ArrayList(FilePackage)
         */
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        throw new RuntimeException("GONE");
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

}
