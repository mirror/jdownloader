package jd;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SysTray {

    public SysTray() throws AWTException {
        // Checks if the system tray is supported
        if (!SystemTray.isSupported()) {

        } else {
            MenuItem quitItem = new MenuItem("Quit");
            MenuItem showInterfaceItem = new MenuItem("Show GUI");

            String tooltip = "A simple tray tooltip";
            PopupMenu popup = new PopupMenu("tray pop-up");
            popup.add(showInterfaceItem);
            popup.addSeparator();
            popup.add(quitItem);

            Image img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            TrayIcon trayIcon = new TrayIcon(img, tooltip, popup);
            trayIcon.setPopupMenu(popup);
            trayIcon.displayMessage("Message", "Text", TrayIcon.MessageType.INFO);

            SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);

            JOptionPane.showMessageDialog(null, "Message", "Title", JOptionPane.NO_OPTION, null);
            // JOptionPane.showOptionDialog(null, "Message", "Title",
            // JOptionPane.NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
            // null, null);
            // JRadioButtonMenuItem
        }
    }

    /*
     * For testing purpose ...
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    new SysTray();
                } catch (AWTException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}