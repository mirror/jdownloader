package jd.gui.userio.dialog;

import javax.swing.JDialog;
import javax.swing.JLabel;

import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.Formatter;
import jd.utils.JDLocale;
import jd.utils.JDSounds;

public abstract class JCountdownDialog extends JDialog {

    private static final long serialVersionUID = 8114522313158766965L;
    protected Thread countdownThread;
    private String ownTitle;
    protected int countdown;
    protected JLabel countDownLabel;

    public JCountdownDialog(SimpleGUI currentgui) {
        super(currentgui);
        this.countDownLabel = new JLabel();
        setTitle(JDLocale.L("gui.captchaWindow.askForInput", "Please enter..."));
    }


    public void interrupt() {
        if (countdownThread != null) {
            countdownThread.interrupt();
            countdownThread = null;
            countDownLabel.setText("");
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

                    new GuiRunnable() {

                        @Override
                        public Object runSave() {
                            countDownLabel.setText(JDLocale.LF("gui.dialogs.countdown.label", "%s sec", left));
                            return null;
                        }

                    }.start();
                    if (c <= 3) JDSounds.P("sound.captcha.onCaptchaInputEmergency");

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
}
