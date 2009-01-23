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
