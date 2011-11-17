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

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import jd.Main;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlIDListener;
import jd.gui.swing.jdgui.components.speedmeter.SpeedMeterPanel;

import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.downloads.QuickSettingsPopup;

public class MainToolBar extends ToolBar {

    private static final long  serialVersionUID = 922971719957349497L;
    private static MainToolBar INSTANCE         = null;

    private SpeedMeterPanel    speedmeter;

    public static synchronized MainToolBar getInstance() {
        if (INSTANCE == null) INSTANCE = new MainToolBar();
        return INSTANCE;
    }

    private MainToolBar() {
        super();
        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        speedmeter = new SpeedMeterPanel(true, false);
                        speedmeter.addMouseListener(new MouseAdapter() {

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                                    QuickSettingsPopup pu = new QuickSettingsPopup();
                                    pu.show((Component) e.getSource(), e.getX(), e.getY());

                                }
                            }

                        });
                        updateToolbar();
                    }
                };

                JDController.getInstance().addControlListener(new ControlIDListener(ControlEvent.CONTROL_DOWNLOADWATCHDOG_START, ControlEvent.CONTROL_DOWNLOADWATCHDOG_STOP) {
                    @Override
                    public void controlIDEvent(final ControlEvent event) {
                        new EDTHelper<Object>() {

                            @Override
                            public Object edtRun() {
                                switch (event.getEventID()) {
                                case ControlEvent.CONTROL_DOWNLOADWATCHDOG_START:
                                    if (speedmeter != null) speedmeter.start();
                                    break;
                                case ControlEvent.CONTROL_DOWNLOADWATCHDOG_STOP:
                                    if (speedmeter != null) speedmeter.stop();
                                    break;
                                }
                                return null;
                            }
                        }.start();
                    }
                });
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
        if (speedmeter != null) add(speedmeter, "hidemode 3, width 32:300:300");
    }

}