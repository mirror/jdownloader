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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import jd.controlling.DownloadController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class DateFinishedColumn extends JDTableColumn {

    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink dLink;
    private Date date;
    private SimpleDateFormat dateFormat;
    private FilePackage fp;
    private JRendererLabel jlr;

    public DateFinishedColumn(String name, JDTableModel table) {
        super(name, table);
        date = new Date();
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        try {
            dateFormat = new SimpleDateFormat(JDL.L("jd.gui.swing.jdgui.views.downloadview.TableRenderer.TableRenderer.dateformat", "dd.MM.yy HH:mm"));
        } catch (Exception e) {
            dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        }
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean defaultVisible() {
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
            if (fp.getFinishedDate() <= 0) {
                jlr.setText("");
            } else {
                date.setTime(fp.getFinishedDate());
                jlr.setText(dateFormat.format(date));
            }
        } else {
            dLink = (DownloadLink) value;
            if (dLink.getFinishedDate() <= 0) {
                jlr.setText("");
            } else {
                date.setTime(dLink.getFinishedDate());
                jlr.setText(dateFormat.format(date));
            }
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
        if (obj == null) return true;
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<FilePackage> packages = null;
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                packages = DownloadController.getInstance().getPackages();
                if (obj == null && packages.size() > 1) {
                    /* header, sortiere die packages nach namen */
                    Collections.sort(packages, new Comparator<FilePackage>() {
                        public int compare(FilePackage a, FilePackage b) {
                            FilePackage aa = a;
                            FilePackage bb = b;
                            if (sortingToggle) {
                                aa = b;
                                bb = a;
                            }
                            if (aa.getFinishedDate() == bb.getFinishedDate()) return 0;
                            return aa.getFinishedDate() < bb.getFinishedDate() ? -1 : 1;
                        }
                    });
                } else {
                    if (obj != null && packages.size() > 1) packages = (ArrayList<FilePackage>) obj;
                    for (FilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinkList(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                if (aa.getFinishedDate() == bb.getFinishedDate()) return 0;
                                return aa.getFinishedDate() < bb.getFinishedDate() ? -1 : 1;
                            }
                        });
                    }
                }
            }
        }
        /* inform DownloadController that structure changed */
        DownloadController.getInstance().fireStructureUpdate();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

}
