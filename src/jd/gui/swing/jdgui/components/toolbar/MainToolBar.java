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

package jd.gui.swing.jdgui.components.toolbar;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;

public class MainToolBar extends ToolBar implements ControlListener {

    private static final long serialVersionUID = 922971719957349497L;
    private static MainToolBar INSTANCE = null;

    private final SpeedMeterPanel speedmeter;

    public static synchronized MainToolBar getInstance() {
        if (INSTANCE == null) INSTANCE = new MainToolBar();
        return INSTANCE;
    }

    private MainToolBar() {
        super();

        speedmeter = new SpeedMeterPanel(true, false);
        JDController.getInstance().addControlListener(this);
    }

    public void controlEvent(final ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_DOWNLOAD_START:
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    switch (event.getID()) {
                    case ControlEvent.CONTROL_DOWNLOAD_START:
                        speedmeter.start();
                        ActionController.getToolBarAction("toolbar.control.stopmark").setEnabled(true);
                        break;
                    case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                        ActionController.getToolBarAction("toolbar.control.stopmark").setEnabled(false);
                        speedmeter.stop();
                        break;
                    }
                    return null;
                }
            }.start();
        }
    }

    @Override
    protected String getColConstraints(String[] list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; ++i) {
            sb.append("2[]");
        }
        sb.append("push[]");
        return sb.toString();
    }

    @Override
    protected void updateSpecial() {
        add(speedmeter, "hidemode 3,height 30!, width 30:200:300");
    }

}