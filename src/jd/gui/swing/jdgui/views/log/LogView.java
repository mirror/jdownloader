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

package jd.gui.swing.jdgui.views.log;

import javax.swing.Icon;

import jd.controlling.GarbageController;
import jd.gui.swing.jdgui.GraphicalUserInterfaceSettings;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.storage.config.JsonConfig;

/**
 * The tab for the log.
 */
public class LogView extends ClosableView {
    private static final long   serialVersionUID = -4440872942373187410L;

    private static final String IDENT_PREFIX     = "jd.gui.swing.jdgui.views.log.";

    private static LogView      INSTANCE         = null;

    private LogInfoPanel        lip;

    /**
     * Logview Singleton
     * 
     * @return
     */
    public synchronized static LogView getInstance() {
        if (INSTANCE == null) INSTANCE = new LogView();
        return INSTANCE;
    }

    /**
     * @see #getLogView()
     */
    private LogView() {
        super();
        LogPane lp = new LogPane();
        this.setContent(lp);
        this.setDefaultInfoPanel(lip = new LogInfoPanel());
        lip.addActionListener(lp);
        getBroadcaster().addListener(new SwitchPanelListener() {
            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) {

                    JsonConfig.create(GraphicalUserInterfaceSettings.class).setLogViewVisible(false);
                    getBroadcaster().removeListener(this);
                    synchronized (LogView.this) {
                        INSTANCE = null;
                        GarbageController.requestGC();
                    }
                }
            }
        });
        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.log", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Log");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "See or Upload the Log");
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setLogViewVisible(true);
    }

    @Override
    public String getID() {
        return "logview";
    }
}