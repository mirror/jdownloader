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
import jd.gui.swing.components.SpeedMeterPanel;

public class MainToolBar extends ToolBar implements ControlListener {
    /**
     * 
     */
    private static final long serialVersionUID = 922971719957349497L;
    private SpeedMeterPanel speedmeter;
    private static MainToolBar INSTANCE = null;


    public static synchronized MainToolBar getInstance() {
        if (INSTANCE == null) INSTANCE = new MainToolBar();
        return INSTANCE;
    }

    private MainToolBar() {
        super();

      

        INSTANCE = this;
        JDController.getInstance().addControlListener(this);
    }

    private void addSpeedMeter() {
        if(speedmeter==null) speedmeter = new SpeedMeterPanel();
        add(speedmeter, "dock east,hidemode 3,height 30!,width 30:200:300");
    }

    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_DOWNLOAD_START:
            speedmeter.start();
            break;
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            speedmeter.stop();
            break;
        }
    }

    protected void updateToolbar() {
        super.updateToolbar();
   
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
         
                addSpeedMeter();

                return null;
            }
        }.waitForEDT();
    }

}