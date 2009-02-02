//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple;

import java.awt.EventQueue;

import javax.swing.JLabel;

public class LabelHandler extends Thread {

    private JLabel label;
    private String defaultmsg = "";
    private String curmsg = null;
    private String newmsg = null;
    private boolean waitflag = false;
    private int curtimeout = 0;
    private int newtimeout = 0;

    public LabelHandler(JLabel label2) {
        label = label2;
        defaultmsg = "";
        start();
    }

    public LabelHandler(JLabel label2, String defaultmsg2) {
        label = label2;
        if (defaultmsg2 == null) {
            defaultmsg = "";
        } else
            defaultmsg = defaultmsg2;
        start();
    }

    public synchronized void changeTxt(String txt, int tout, boolean force) {
        newmsg = txt;
        if (force == true) {
            newtimeout = tout;
            if (waitflag) interrupt();
            notifyAll();
        } else {
            newtimeout = tout;
            notifyAll();
        }
    }

    public synchronized void changeDefault(String txt, boolean force) {
        defaultmsg = txt;
        if (force == true) {
            if (waitflag) interrupt();
            notifyAll();
        }
    }

    @Override
    public void run() {
        setName("LabelHandler");
        while (true) {
            curtimeout = newtimeout;
            curmsg = newmsg;
            newtimeout = 0;
            newmsg = null;
            setText(curmsg);
            if (curtimeout > 0) {
                try {
                    waitflag = true;
                    sleep(curtimeout);
                } catch (InterruptedException e) {
                }
                waitflag = false;
            } else {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }

            }
        }
    }

    private void setText(final String text) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                label.setText((text == null) ? defaultmsg : text);
            }
        });
    }
}
