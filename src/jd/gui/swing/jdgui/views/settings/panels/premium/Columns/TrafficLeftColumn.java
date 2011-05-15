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

import java.awt.Color;
import java.awt.Component;

import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.components.JDProgressBarRender;
import jd.gui.swing.jdgui.views.settings.panels.premium.HostAccounts;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.gui.translate._GUI;

public class TrafficLeftColumn extends JDTableColumn {

    private static final long   serialVersionUID   = -5291590062503352550L;
    private Color               COL_PROGRESS       = null;
    private Color               COL_PROGRESS_ERROR = new Color(0xCC3300);
    private Color               COL_PROGRESS_NORMAL;
    private JRendererLabel      jlr;
    private JDProgressBarRender progress;

    public TrafficLeftColumn(String name, JDTableModel table) {
        super(name, table);
        progress = new JDProgressBarRender();
        progress.setStringPainted(true);
        progress.setOpaque(true);
        COL_PROGRESS_NORMAL = progress.getForeground();
        jlr = new JRendererLabel();
        jlr.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public void handleSelected(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        /* customized handleSelected for ProgressBar */
        if (c instanceof JDProgressBarRender) {
            ((JDProgressBarRender) c).setForeground(COL_PROGRESS);
            /* check selected state */
            if (isSelected) {
                ((JDProgressBarRender) c).setBackground(JDTableColumn.background);
                return;
            } else {
                ((JDProgressBarRender) c).setBackground(JDTableColumn.background);
                /* check if we have to highlight an unselected cell */
                for (JDRowHighlighter high : table.getJDRowHighlighter()) {
                    if (high.doHighlight(value)) {
                        ((JDProgressBarRender) c).setBackground(high.getColor());
                        return;
                    }
                }
            }
        } else {
            super.handleSelected(c, table, value, isSelected, row, column);
        }
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
            Account ac = (Account) value;
            AccountInfo ai = ac.getAccountInfo();
            if (!ac.isValid()) {
                value = "Invalid account";
                progress.setMaximum(10);
                progress.setValue(10);
                COL_PROGRESS = COL_PROGRESS_ERROR;
            } else if (ai == null) {
                value = "Unknown";
                progress.setMaximum(10);
                progress.setValue(10);
                COL_PROGRESS = COL_PROGRESS_ERROR;
            } else {
                COL_PROGRESS = COL_PROGRESS_NORMAL;
                if (ai.isUnlimitedTraffic()) {
                    value = "Unlimited";
                    progress.setMaximum(10);
                    progress.setValue(10);
                } else {
                    value = Formatter.formatReadable(ai.getTrafficLeft()) + "/" + Formatter.formatReadable(ai.getTrafficMax());
                    progress.setMaximum(ai.getTrafficMax());
                    progress.setValue(ai.getTrafficLeft());
                }
            }
            progress.setString((String) value);
            return progress;
        } else {
            HostAccounts ha = (HostAccounts) value;
            if (!ha.gotAccountInfos()) {
                jlr.setText(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_unknown());
            } else {
                if (ha.getTraffic() < 0) {
                    jlr.setText(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_unlimited());
                } else if (ha.getTraffic() == 0) {
                    jlr.setText(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_noTrafficLeft());
                } else {
                    jlr.setText(Formatter.formatReadable(ha.getTraffic()));
                }
            }
            return jlr;
        }
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}