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

package jd;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.nativeintegration.ScreenDevices;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class SplashScreen implements ControlListener {

    public static final int SPLASH_FINISH = 0;
    public static final int SPLASH_PROGRESS = 1;

    private final ImageIcon image;

    private JWindow window;

    private final int x;
    private final int y;

    private JProgressBar progress;

    private String curString = "";

    private final GraphicsDevice gd;

    public SplashScreen(JDController controller) {

        LookAndFeelController.setUIManager();
        this.image = new ImageIcon(JDTheme.I("gui.splash"));

        GraphicsDevice gd = null;
        try {
            final Object loc = GUIUtils.getConfig().getProperty("LOCATION_OF_MAINFRAME");
            if (loc instanceof Point) {
                final Point point = (Point) loc;
                if (point.x < 0) {
                    point.x = 0;
                }
                if (point.y < 0) {
                    point.y = 0;
                }
                gd = ScreenDevices.getGraphicsDeviceforPoint(point);
            } else {
                gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            }
        } catch (Exception e) {
            gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        }
        this.gd = gd;
        final Rectangle v = gd.getDefaultConfiguration().getBounds();
        final int screenWidth = (int) v.getWidth();
        final int screenHeight = (int) v.getHeight();
        x = screenWidth / 2 - image.getIconWidth() / 2;
        y = screenHeight / 2 - image.getIconHeight() / 2;

        initGui();

        controller.addControlListener(this);
    }

    private void initGui() {
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final JLabel label = new JLabel();
        label.setIcon(image);

        window = new JWindow(gc);

        window.setLayout(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]0[]"));
        window.setAlwaysOnTop(true);
        window.setSize(image.getIconWidth(), image.getIconHeight());
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        window.add(label);
        window.add(progress = new JProgressBar(), "hidemode 3,height 20!");
        progress.setVisible(true);

        progress.setIndeterminate(true);
        window.pack();
        final Rectangle b = gc.getBounds();
        window.setLocation(b.x + x, b.y + y);
        window.setVisible(true);

    }

    private void finish() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                window.dispose();
                return null;
            }
        }.start();
    }

    private void incProgress() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                progress.setStringPainted(true);
                progress.setString(curString);
                return null;
            }
        }.start();
    }

    public void controlEvent(final ControlEvent event) {
        final int id = event.getEventID();
        if (id == SPLASH_PROGRESS) {
            final Object parameter = event.getParameter();
            if (parameter instanceof String) {
                synchronized (curString) {
                    curString = (String) parameter;
                }
            }
            incProgress();
        } else if (id == ControlEvent.CONTROL_INIT_COMPLETE && event.getCaller() instanceof Main) {
            JDUtilities.getController().removeControlListener(this);
            finish();
        } else if (id == SPLASH_FINISH) {
            JDUtilities.getController().removeControlListener(this);
            finish();
        }

    }
}