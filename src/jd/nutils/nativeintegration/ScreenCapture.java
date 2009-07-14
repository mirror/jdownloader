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

package jd.nutils.nativeintegration;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import jd.config.SubConfiguration;
import jd.gui.skins.simple.SimpleGuiConstants;

public class ScreenCapture {
    class DeviceInfo {
        Robot robot;
        int width;
        int height;
        int posx;
        int posy;
        int maxx;
        int maxy;
    }

    private static boolean XineramaWorkaround = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty("XINERAMAWORKAROUND", false);
    private static ScreenCapture INSTANCE = new ScreenCapture();

    private static DeviceInfo screens[] = new DeviceInfo[0];
    static {
        try {
            ArrayList<DeviceInfo> robint = new ArrayList<DeviceInfo>();
            for (GraphicsDevice dv : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                try {
                    DeviceInfo tmp = INSTANCE.new DeviceInfo();
                    tmp.robot = new Robot(dv);
                    tmp.height = dv.getDisplayMode().getHeight();
                    tmp.width = dv.getDisplayMode().getWidth();
                    tmp.posx = dv.getDefaultConfiguration().getBounds().x;
                    tmp.maxx = tmp.posx + tmp.width;
                    tmp.posy = dv.getDefaultConfiguration().getBounds().y;
                    tmp.maxy = tmp.posy + tmp.height;
                    robint.add(tmp);
                } catch (AWTException e) {
                    e.printStackTrace();
                }
            }
            screens = robint.toArray(new DeviceInfo[robint.size()]);
        } catch (Exception e) {
        }
    }

    public static boolean gotRobots() {
        return screens.length > 0;
    }

    public static BufferedImage getScreenShot(Rectangle r) throws AWTException {
        synchronized (screens) {
            if (screens.length == 0) { return new Robot().createScreenCapture(r); }
            if (XineramaWorkaround) {
                for (DeviceInfo dv : screens) {
                    if (r.x >= dv.posx && r.x <= dv.maxx && r.y >= dv.posy && r.y <= dv.maxy) {
                        r.x = r.x - dv.posx;
                        r.y = r.y - dv.posy;
                        return dv.robot.createScreenCapture(r);
                    }
                }
                return screens[0].robot.createScreenCapture(r);
            } else {
                return screens[0].robot.createScreenCapture(r);
            }
        }
    }
}
