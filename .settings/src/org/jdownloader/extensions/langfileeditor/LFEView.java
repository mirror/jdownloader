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

package org.jdownloader.extensions.langfileeditor;

import javax.swing.Icon;
import javax.swing.JMenuBar;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ClosableView;

import org.jdownloader.extensions.langfileeditor.translate.T;
import org.jdownloader.images.NewTheme;

public class LFEView extends ClosableView {

    private static final long serialVersionUID = -1676038853735547928L;
    private LFEGui            lfeGui;

    public LFEView(SwitchPanel panel) {
        super();
        this.setContent(panel);
        this.setInfoPanel(LFEInfoPanel.getInstance());
        this.lfeGui = (LFEGui) panel;
        init();
    }

    public LFEGui getLFEGui() {
        return lfeGui;
    }

    @Override
    protected void initMenu(JMenuBar menubar) {
        lfeGui.initMenu(menubar);
    }

    @Override
    public Icon getIcon() {
        return NewTheme.I().getIcon("language", 16);
    }

    @Override
    public String getTitle() {
        return T._.jd_plugins_optional_langfileeditor_LFEView_title();
    }

    @Override
    public String getTooltip() {
        return T._.jd_plugins_optional_langfileeditor_LFEView_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "lfeview";
    }

}