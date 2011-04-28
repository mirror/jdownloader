//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter.LinkgrabberFilter;

import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class Linkgrabber extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;
    private Checkbox          checkLinks;
    private Checkbox          cnl;
    private Checkbox          rename;
    private LinkgrabberFilter filter;
    private ComboBox          blackOrWhite;

    public String getTitle() {
        return JDT._.gui_settings_linkgrabber_title();
    }

    public Linkgrabber() {
        super();

        this.addHeader(getTitle(), Theme.getIcon("settings/linkgrabber", 32));
        this.addDescription(JDT._.gui_settings_linkgrabber_description());

        checkLinks = new Checkbox();
        rename = new Checkbox();
        cnl = new Checkbox();

        addPair(T._.gui_config_linkgrabber_onlincheck(), checkLinks);
        addPair(T._.gui_config_linkgrabber_replacechars(), rename);
        addPair(T._.gui_config_linkgrabber_cnl2(), cnl);

        this.addHeader(T._.gui_config_linkgrabber_ignorelist(), Theme.getIcon("settings/filter", 32));
        this.addDescription(JDT._.gui_settings_linkgrabber_filter_description());
        filter = new LinkgrabberFilter();
        blackOrWhite = new ComboBox(new String[] { T._.settings_linkgrabber_filter_blackorwhite_black(), T._.settings_linkgrabber_filter_blackorwhite_white() });
        addPair(T._.gui_config_linkgrabber_filter_type(), blackOrWhite);

        add(filter);

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("settings/linkgrabber", 32);
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }
}