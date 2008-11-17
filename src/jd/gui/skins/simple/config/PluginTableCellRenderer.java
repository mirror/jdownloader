//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.config;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import jd.PluginWrapper;

public class PluginTableCellRenderer<T extends PluginWrapper> extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 412703637249573038L;
    private ArrayList<T> plugins;
    private Color bgNoConfig;
    private Color bgSelected = null;
    private Color bgWithConfig = null;

    public PluginTableCellRenderer(ArrayList<T> plugins) {
        this.plugins = plugins;
        this.bgNoConfig = getBackground();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (isSelected) {

            if (bgSelected == null) bgSelected = getBackground();
            c.setBackground(bgSelected);

        } else {

            if (bgWithConfig == null) bgWithConfig = getBackground().darker();

            if (plugins.get(row).hasConfig()) {
                c.setBackground(bgWithConfig);
            } else {
                c.setBackground(bgNoConfig);
            }

        }

        return c;
    }

}
