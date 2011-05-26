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

import javax.swing.ImageIcon;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.StatusLabel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class HosterColumn extends JDTableColumn {

    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink      dLink;
    private FilePackage       fp;
    private StatusLabel       statuspanel;
    private int               counter          = 0;
    private ImageIcon         imgResume;
    private ImageIcon         imgPremium;
    private String            strResume;
    private String            strPremium;

    private JRendererLabel    jlr;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
        statuspanel = new StatusLabel();
        statuspanel.setBorder(null);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        imgResume = NewTheme.I().getIcon("resume", 16);
        imgPremium = NewTheme.I().getIcon("premium", 16);
        strResume = _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_resume();
        strPremium = _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_premium();
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
            counter = 0;
            if (dLink.getTransferStatus().usesPremium()) {
                statuspanel.setIcon(counter, imgPremium, null, strPremium);
                counter++;
            }
            if (dLink.getTransferStatus().supportsResume()) {
                statuspanel.setIcon(counter, imgResume, null, strResume);
                counter++;
            }
            if (dLink.getDefaultPlugin() == null) {
                statuspanel.setText("plugin missing", null);
            } else {
                PluginForHost plg = dLink.getLivePlugin();
                if (plg == null) plg = dLink.getDefaultPlugin();
                String s = plg.getSessionInfo();
                statuspanel.setText(s, null);
                statuspanel.setIcon(-1, plg.getHosterIconScaled(), null, dLink.getDownloadLinkInfo().getLoadingFrom());
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