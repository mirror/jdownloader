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

package jd.gui.swing.jdgui.views.myjd;

import java.awt.Color;

import javax.swing.Icon;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ClosableView;

public class MyJDownloaderView extends ClosableView {

    private static final long        serialVersionUID = -5607304856678049342L;

    private static MyJDownloaderView INSTANCE         = null;

    public synchronized static MyJDownloaderView getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MyJDownloaderView();
        }
        return INSTANCE;
    }

    private MyJDownloaderView() {
        super();

        setBackground(new Color(0xF5FCFF));
        init();
        this.setContent(new MyJDownloaderPanel());
    }

    public void setSelectedSubPanel(Class<?> class1) {
        ((MyJDownloaderPanel) getContent()).setSelectedSubPanel(class1);
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return _GUI.T.MyJDownloaderView_title();
    }

    @Override
    public String getTooltip() {
        return _GUI.T.MyJDownloaderView_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setMyJDownloaderViewVisible(true);
        SwitchPanel panel = this.getContent();
        if (panel != null) {
            panel.setShown();
        }

    }

    @Override
    public void onClosed() {
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setMyJDownloaderViewVisible(false);
    }

    @Override
    public String getID() {
        return "myjdownloaderview";
    }

}