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

package jd.gui.swing.components;

import java.awt.MouseInfo;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JWindow;

import org.appwork.utils.swing.EDTHelper;

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
                        new EDTHelper<Object>() {

                            private Point loc;

                            @Override
                            public Object edtRun() {
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
            // window.setSize(100, 60);
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
