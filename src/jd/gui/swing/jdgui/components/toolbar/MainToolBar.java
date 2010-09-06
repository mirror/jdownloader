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
import jd.event.ControlIDListener;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;

public class MainToolBar extends ToolBar {

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
        JDController.getInstance().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOAD_START, ControlEvent.CONTROL_DOWNLOAD_STOP) {
            @Override
            public void controlIDEvent(final ControlEvent event) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        switch (event.getEventID()) {
                        case ControlEvent.CONTROL_DOWNLOAD_START:
                            speedmeter.start();
                            break;
                        case ControlEvent.CONTROL_DOWNLOAD_STOP:
                            speedmeter.stop();
                            break;
                        }
                        return null;
                    }
                }.start();
            }
        });
    }

    @Override
    protected String getColConstraints(String[] list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; ++i) {
            sb.append("2[]");
        }
        sb.append("push[]2");
        return sb.toString();
    }

    @Override
    protected void updateSpecial() {
        add(speedmeter, "hidemode 3, width 32:200:300");
    }

}