//jDownloader - Downloadmanager
//Copyright (C) 2010 JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.folderwatch;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class FolderWatchView extends ClosableView {

    private static final long serialVersionUID = 5113064941511136310L;

    private static final String JDL_PREFIX = "plugins.optional.folderwatch.view.";

    public FolderWatchView() {
        super();

        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.folderwatch", 16, 16);
    }

    @Override
    public String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "FolderWatch History");
    }

    @Override
    public String getTooltip() {
        return JDL.L(JDL_PREFIX + "tooltip", "Shows all container files that were found by FolderWatch");
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "folderwatchview";
    }

}
