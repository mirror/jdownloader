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
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;

import jd.controlling.DownloadController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.StatusLabel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class HosterColumn extends JDTableColumn {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.downloadview.TableRenderer.";

    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink dLink;
    private FilePackage fp;
    private StatusLabel statuspanel;
    private int counter = 0;
    private ImageIcon imgResume;
    private ImageIcon imgPremium;
    private String strResume;
    private String strPremium;

    private String strLoadingFrom;

    private JRendererLabel jlr;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
        statuspanel = new StatusLabel();
        statuspanel.setBorder(null);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        imgResume = JDTheme.II("gui.images.resume", 16, 16);
        imgPremium = JDTheme.II("gui.images.premium", 16, 16);
        strResume = JDL.L(JDL_PREFIX + "resume", "Resumable download");
        strPremium = JDL.L(JDL_PREFIX + "premium", "Loading with Premium");
        strLoadingFrom = JDL.L(JDL_PREFIX + "loadingFrom", "Loading from") + " ";
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
            jlr.setText(fp.getHoster());
            return jlr;
        } else {
            dLink = (DownloadLink) value;
            statuspanel.setText(dLink.getLinkStatus().getStatusString(), null);
            counter = 0;
            if (dLink.getTransferStatus().usesPremium()) {
                statuspanel.setIcon(counter, imgPremium, null, strPremium);
                counter++;
            }
            if (dLink.getTransferStatus().supportsResume()) {
                statuspanel.setIcon(counter, imgResume, null, strResume);
                counter++;
            }
            if (dLink.getPlugin() == null) {
                statuspanel.setText("plugin missing", null);
            } else {
                String s = dLink.getPlugin().getHost() + dLink.getPlugin().getSessionInfo();
                statuspanel.setText(s, null);
                statuspanel.setIcon(-1, dLink.getPlugin().getHosterIconScaled(), null, strLoadingFrom + dLink.getPlugin().getHost());
            }
            statuspanel.clearIcons(counter);
        }
        return statuspanel;
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
                            return aa.getHoster().compareToIgnoreCase(bb.getHoster());
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
                                return aa.getHost().compareToIgnoreCase(bb.getHost());
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
