package jd.gui.skins.simple;

import java.awt.MouseInfo;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JWindow;

import net.miginfocom.swing.MigLayout;

public class MouseFollower {

    private static JWindow window;
    private static Thread follower;

    public static void show(JComponent mouseOver) {
        if (window == null) {
            window = new JWindow();
       
       

            window.setAlwaysOnTop(true);

            follower = new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            follower = null;
                            return;
                        }
                        new GuiRunnable() {

                            private Point loc;

                            @Override
                            public Object runSave() {
                                try {
                                    loc = MouseInfo.getPointerInfo().getLocation();
                                    loc.x += 10;
                                    loc.y += 10;

                                    window.setLocation(loc);
                                } catch (Exception e) {
                                }
                                return null;
                            }

                        }.start();

                    }
                }
            };
            follower.start();
//            window.setSize(100, 60);
            window.setBackground(null);

        
        }
        window.getContentPane().add(mouseOver);
        
        window.setVisible(true);
        window.pack();
    }

    public static void hide() {
        if (window != null) {
            window.dispose();
            window = null;
            follower.interrupt();
            follower = null;
        }

    }

}
