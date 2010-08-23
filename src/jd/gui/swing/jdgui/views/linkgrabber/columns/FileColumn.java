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

package jd.gui.swing.jdgui.views.linkgrabber.columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.Border;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.StatusLabel;
import jd.nutils.NaturalOrderComparator;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDTheme;

public class FileColumn extends JDTableColumn {

    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink dLink;
    private Border leftGap;
    private ImageIcon icon_fp_open;
    private ImageIcon icon_fp_open_error;
    private ImageIcon icon_fp_closed;
    private ImageIcon icon_fp_closed_error;
    private ImageIcon imgFileFailed;
    private LinkGrabberFilePackage fp;
    private StatusLabel jlr;

    public FileColumn(String name, JDTableModel table) {
        super(name, table);
        leftGap = BorderFactory.createEmptyBorder(0, 32, 0, 0);
        icon_fp_open = JDTheme.II("gui.images.package_opened_tree", 32, 16);
        icon_fp_open_error = JDTheme.II("gui.images.package_open_error_tree", 32, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed_tree", 32, 16);
        icon_fp_closed_error = JDTheme.II("gui.images.package_closed_error_tree", 32, 16);
        imgFileFailed = JDTheme.II("gui.images.offlinefile", 16, 16);
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
        if (value instanceof LinkGrabberFilePackage) {
            fp = (LinkGrabberFilePackage) value;
            if (fp.countFailedLinks(false) > 0) {
                jlr.setText(fp.getName(), !fp.getBooleanProperty(LinkGrabberController.PROPERTY_EXPANDED, false) ? icon_fp_closed_error : icon_fp_open_error);
            } else {
                jlr.setText(fp.getName(), !fp.getBooleanProperty(LinkGrabberController.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            }
            jlr.setIcon(0, null, "[" + fp.size() + "]", null);
            jlr.clearIcons(1);
            jlr.setBorder(null);
        } else {
            dLink = (DownloadLink) value;
            if (dLink.isAvailabilityStatusChecked() && !dLink.isAvailable()) {
                jlr.setText(dLink.getName(), this.imgFileFailed);
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

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        /*
         * LinkGrabber hat nur null(Header) oder ne
         * ArrayList(LinkGrabberFilePackage)
         */
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<LinkGrabberFilePackage> packages = null;
        final NaturalOrderComparator nc = new NaturalOrderComparator();
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                packages = LinkGrabberController.getInstance().getPackages();
                if (obj == null && packages.size() > 1) {
                    /* header, sortiere die packages nach namen */
                    Collections.sort(packages, new Comparator<LinkGrabberFilePackage>() {
                        public int compare(LinkGrabberFilePackage a, LinkGrabberFilePackage b) {
                            LinkGrabberFilePackage aa = a;
                            LinkGrabberFilePackage bb = b;
                            if (sortingToggle) {
                                aa = b;
                                bb = a;
                            }
                            return nc.compare(aa.getName(), bb.getName());
                        }
                    });
                } else {
                    /*
                     * in obj stecken alle selektierten packages, sortiere die
                     * links nach namen
                     */
                    if (obj != null) packages = (ArrayList<LinkGrabberFilePackage>) obj;
                    for (LinkGrabberFilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinks(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                return nc.compare(aa.getName(), bb.getName());
                            }
                        });
                    }
                }
            }
        }
        /* inform LinkGrabberController that structure changed */
        LinkGrabberController.getInstance().throwRefresh();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof LinkGrabberFilePackage) return ((LinkGrabberFilePackage) obj).countEnabledLinks(false) > 0;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        return true;
    }

}
