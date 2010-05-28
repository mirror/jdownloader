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

package jd.gui.swing.dialog;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.userio.DummyFrame;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public abstract class JCountdownDialog extends JDialog {

    private static final long serialVersionUID = 8114522313158766965L;

    protected Thread countdownThread;
    protected int countdown;

    protected JLabel countDownLabel;

    public JCountdownDialog(JFrame currentgui) {
        super(currentgui);
        // fixes always on top bug in windows
        /*
         * Bugdesc: found in svn
         */
        DummyFrame.getDialogParent().setAlwaysOnTop(true);
        DummyFrame.getDialogParent().setAlwaysOnTop(false);
        initCountdown();
    }

    protected void initCountdown() {
        countDownLabel = new JLabel("");
        countDownLabel.setIcon(JDTheme.II("gui.images.cancel", 16, 16));
        countDownLabel.setToolTipText(JDL.L("gui.dialog.countdown.tooltip", "This dialog closes after a certain time. Click here to stop the countdown"));
        countDownLabel.addMouseListener(new JDMouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                interrupt();
                countDownLabel.removeMouseListener(this);
            }

        });
    }

    public void interrupt() {
        if (countdownThread != null) {
            countdownThread.interrupt();
            countdownThread = null;
            countDownLabel.setEnabled(false);
        }
    }

    protected abstract void onCountdown();

    protected void countdown(int time) {
        this.countdown = time;
        countdownThread = new Thread() {

            @Override
            public void run() {
                while (!isVisible()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                int c = countdown;

                while (--c >= 0) {
                    if (!isVisible()) return;
                    if (countdownThread == null) return;
                    final String left = Formatter.formatSeconds(c);

                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            countDownLabel.setText(left);
                            return null;
                        }

                    }.start();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (countdown < 0) return;
                    if (!isVisible()) return;
                }
                if (countdown < 0) return;
                if (!this.isInterrupted()) onCountdown();
            }
        };

        countdownThread.start();
    }

    /**
     * Wrapper für Java 1.5 (Mac User)
     */
    public void setIconImage(Image image) {
        if (JDUtilities.getJavaVersion() >= 1.6) super.setIconImage(image);
    }

    /**
     * Wrapper für Java 1.5 (Mac User)
     */
    public void setIconImages(List<? extends Image> icons) {
        if (JDUtilities.getJavaVersion() >= 1.6) super.setIconImages(icons);
    }

}
