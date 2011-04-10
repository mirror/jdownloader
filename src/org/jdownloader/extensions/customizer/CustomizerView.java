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

package org.jdownloader.extensions.customizer;

import javax.swing.Icon;

import jd.plugins.AddonPanel;
import jd.utils.JDTheme;

import org.jdownloader.extensions.customizer.translate.T;

public class CustomizerView extends AddonPanel {

    private static final long serialVersionUID = -8077441680881378656L;

    public CustomizerView(PackageCustomizerExtension packageCustomizerExtension) {
        super(packageCustomizerExtension);

        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.newpackage", 16, 16);
    }

    @Override
    public String getTitle() {
        return T._.jd_plugins_optional_customizer_CustomizerView_title();
    }

    @Override
    public String getTooltip() {
        return T._.jd_plugins_optional_customizer_CustomizerView_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "customizerview";
    }

    @Override
    protected void onDeactivated() {
    }

    @Override
    protected void onActivated() {
    }

}