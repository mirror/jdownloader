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

package jd.gui.swing.jdgui.views;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;

import jd.gui.swing.jdgui.borders.JDBorderFactory;
import jd.gui.swing.jdgui.settings.ConfigPanel;

public class ConfigPanelView extends ClosableView {

    private static final long serialVersionUID = -4273043756814096939L;
    private ConfigPanel panel;
    private Icon icon;
    private String title;

    public ConfigPanelView(ConfigPanel premium, String title, Icon icon) {
        super();
        panel = premium;
        this.title = title;
        this.icon = icon;
        this.setContent(panel);
        this.init();
    }

    @Override
    protected void initMenu(JMenuBar menubar) {
        JLabel label;
        menubar.add(label = new JLabel(title));
        label.setIcon(icon);
        label.setIconTextGap(10);

        menubar.setBorder(JDBorderFactory.createInsideShadowBorder(0, 0, 1, 0));
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getTooltip() {
        return null;
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

}
