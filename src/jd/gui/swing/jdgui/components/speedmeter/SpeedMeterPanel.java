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

package jd.gui.swing.jdgui.components.speedmeter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.Graph;

public class SpeedMeterPanel extends Graph implements ActionListener, MouseListener {

    private static final long serialVersionUID = 5571694800446993879L;

    private boolean           show;

    public SpeedMeterPanel(boolean contextMenu, boolean start) {
        show = contextMenu ? GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER, true) : true;

        setOpaque(false);
        setBorder(show ? BorderFactory.createEtchedBorder() : null);

        if (contextMenu) {
            addMouseListener(this);
            JDUtilities.getController().addControlListener(new ConfigPropertyListener(JDGuiConstants.PARAM_SHOW_SPEEDMETER) {

                @Override
                public void onPropertyChanged(Property source, String key) {
                    show = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER, true);
                    setBorder(show ? BorderFactory.createEtchedBorder() : null);
                }

            });
        }

        if (start) start();
    }

    public void start() {
        super.start();
    }

    public void stop() {
        super.stop();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER, !show);
            GUIUtils.getConfig().save();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (DownloadWatchDog.getInstance().getStateMonitor().isStartState() || DownloadWatchDog.getInstance().getStateMonitor().isFinal()) return;
            JMenuItem mi = new JMenuItem(show ? JDL.L("gui.speedmeter.hide", "Hide Speedmeter") : JDL.L("gui.speedmeter.show", "Show Speedmeter"));
            mi.addActionListener(this);

            JPopupMenu popup = new JPopupMenu();
            popup.add(mi);
            popup.show(this, e.getPoint().x, e.getPoint().y);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public int getValue() {
        return (int) DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
    }

}