package jd.dynamics;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

import jd.controlling.DynamicPluginInterface;
import jd.gui.swing.jdgui.JDGui;
import jd.nutils.Screen;
import jd.nutils.nativeintegration.LocalBrowser;
import net.miginfocom.swing.MigLayout;

public class Facebook extends DynamicPluginInterface {

    @Override
    public void execute() {

        // try {
        // Thread.sleep(5000);
        //
        // final String id = "2";
        // if (SubConfiguration.getConfig("facebook").getGenericProperty(id,
        // false) && JDInitFlags.SWITCH_DEBUG == false) {
        // // Schon durchgefuehrt
        //
        // return;
        // }
        // SubConfiguration.getConfig("facebook").setProperty(id, true);
        //
        // SubConfiguration.getConfig("facebook").save();
        // File fb = JDUtilities.getResourceFile("tmp/fb.png");
        // File fbMouseOver = JDUtilities.getResourceFile("tmp/fbmo.png");
        // fb.getParentFile().mkdirs();
        //
        // fb.deleteOnExit();
        // fbMouseOver.deleteOnExit();
        //
        // Browser.download(fb, "http://update0.jdownloader.org/facebook.png");
        // Browser.download(fbMouseOver,
        // "http://update0.jdownloader.org/facebook_mo.png");
        //
        // final ImageIcon ico = new ImageIcon(ImageIO.read(fb));
        // final ImageIcon icoMO = new ImageIcon(ImageIO.read(fbMouseOver));
        //
        // new FBDialog(ico, icoMO);
        //
        // } catch (final InterruptedException e1) {
        // e1.printStackTrace();
        // } catch (IOException e) {
        // e.printStackTrace();
        //
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    private static class FBDialog extends JDialog {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public FBDialog(final ImageIcon ico, final ImageIcon icoMO) {
            super(JDGui.getInstance().getMainFrame());

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
                    try {
                        LocalBrowser.openDefaultURL(new URL("http://www.jdownloader.org/facebook"));
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                    dispose();
                }
            });
            this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
            lbl.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, getBackground().darker()));
            add(lbl);

            this.pack();

            this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            this.setTitle("JDownloader on Facebook");
            this.setResizable(false);
            this.setLocation(Screen.getCenterOfComponent(JDGui.getInstance().getMainFrame(), this));

            /*
             * Fixes Always-on-Top-Bug in windows. Bugdesc: found in svn
             */
            JDGui.getInstance().getMainFrame().setAlwaysOnTop(true);
            JDGui.getInstance().getMainFrame().setAlwaysOnTop(false);

            this.setVisible(true);
        }
    }

}
