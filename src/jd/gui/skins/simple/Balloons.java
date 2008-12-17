package jd.gui.skins.simple;

import java.util.HashMap;

import javax.swing.JComponent;

import net.java.balloontip.BalloonTip;

public class Balloons {

    private static HashMap<String, JComponent> attachableBallons = new HashMap<String, JComponent>();

    private Balloons() {
    }

    public static void attachComponent(JComponent comp, String name) {
        attachableBallons.put(name, comp);
    }

    public static void removeComponent(String name) {
        attachableBallons.remove(name);
    }

    public static void showBalloon(String name, String message) {
        JComponent com = attachableBallons.get(name);
        if (com != null && com.isShowing() && message != null && message.length() > 0) {
            @SuppressWarnings("unused")
            BalloonTip myBalloonTip = new BalloonTip(com, message);
        }
    }
}
