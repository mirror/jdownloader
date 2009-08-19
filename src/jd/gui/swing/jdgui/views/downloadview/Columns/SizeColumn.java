package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JTable;

import jd.controlling.DownloadController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class SizeColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private FilePackage fp;

    public SizeColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            if (fp.getTotalEstimatedPackageSize() < 0) {
                ((JRendererLabel) co).setText("Unknown Filesize");
            } else {
                ((JRendererLabel) co).setText(Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
            }
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (dLink.getDownloadSize() <= 0) {
                ((JRendererLabel) co).setText("Unknown Filesize");
            } else {
                ((JRendererLabel) co).setText(Formatter.formatReadable(dLink.getDownloadSize()));
            }
        }
        ((JRendererLabel) co).setBorder(null);
        return co;
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
                            if (aa.getTotalEstimatedPackageSize() == bb.getTotalEstimatedPackageSize()) return 0;
                            return aa.getTotalEstimatedPackageSize() < bb.getTotalEstimatedPackageSize() ? -1 : 1;
                        }
                    });
                } else {
                    /*
                     * in obj stecken alle selektierten packages, sortiere die
                     * links nach namen
                     */
                    if (obj != null) packages = (ArrayList<FilePackage>) obj;
                    for (FilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinkList(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                if (aa.getDownloadSize() == bb.getDownloadSize()) return 0;
                                return aa.getDownloadSize() < bb.getDownloadSize() ? -1 : 1;
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
