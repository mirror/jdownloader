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

package jd.gui.swing.jdgui.views.settings.panels.premium.Columns;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.panels.premium.HostAccounts;
import jd.plugins.Account;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.gui.translate.T;

public class HosterColumn extends JDTableColumn {

    private static Border     leftGap          = BorderFactory.createEmptyBorder(0, 30, 0, 0);
    private static final long serialVersionUID = -6741644821097309670L;
    private JRendererLabel    jlr;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            jlr.setText(T._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_account());
            jlr.setBorder(leftGap);
            jlr.setIcon(null);
            jlr.setHorizontalAlignment(SwingConstants.RIGHT);
        } else {
            HostAccounts ha = (HostAccounts) value;
            String host = ha.getHost();
            jlr.setBorder(null);
            jlr.setHorizontalAlignment(SwingConstants.LEFT);
            jlr.setIcon(JDUtilities.getPluginForHost(host).getHosterIconScaled());
            jlr.setText(host);
        }
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {

    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }
}