package jd.dynamics;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.JDInitFlags;
import jd.config.SubConfiguration;
import jd.controlling.DynamicPluginInterface;
import jd.http.Browser;
import jd.nutils.nativeintegration.LocalBrowser;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class Facebook extends DynamicPluginInterface {

    private boolean called;

    @Override
    public void execute() {

        try {
            Thread.sleep(10000);

            final String id = "1";
            if (SubConfiguration.getConfig("facebook").getGenericProperty(id, false) && JDInitFlags.SWITCH_DEBUG == false) {
                // Schon durchgefuehrt
                return;
            }
            File fb = Application.getResource("tmp/fb.png");
            File fbMouseOver = Application.getResource("tmp/fbmo.png");
            fb.deleteOnExit();
            fbMouseOver.deleteOnExit();

            Browser.download(fb, "http://update0.jdownloader.org/facebook.png");
            Browser.download(fbMouseOver, "http://update0.jdownloader.org/facebook_mo.png");
            final ImageIcon ico = new ImageIcon(ImageIO.read(fb));
            final ImageIcon icoMO = new ImageIcon(ImageIO.read(fbMouseOver));
            SubConfiguration.getConfig("facebook").setProperty(id, true);

            final JLabel lbl = new JLabel(ico);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new MouseListener() {

                public void mouseReleased(MouseEvent e) {
                    lbl.setIcon(icoMO);
                }

                public void mousePressed(MouseEvent e) {
                    lbl.setIcon(ico);
                }

                public void mouseExited(MouseEvent e) {
                    lbl.setIcon(ico);
                }

                public void mouseEntered(MouseEvent e) {
                    lbl.setIcon(icoMO);
                }

                public void mouseClicked(MouseEvent e) {
                    callURL();
                }
            });
            JPanel p = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
            lbl.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, p.getBackground().darker()));
            p.add(lbl);
            SubConfiguration.getConfig("facebook").save();

            ContainerDialog d = new ContainerDialog(Dialog.STYLE_HIDE_ICON, "JDownloader on Facebook", p, null, null, null);
            Dialog.getInstance().showDialog(d);
            callURL();
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    protected void callURL() {
        if (called) return;
        called = true;
        try {
            LocalBrowser.openDefaultURL(new URL("http://www.jdownloader.org/facebook"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
