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

    public static BufferedImage getScreenShot(Rectangle r) {
        synchronized (screens) {
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
