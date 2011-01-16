package jd.updater;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.swing.dialog.Dialog;

public class UpdaterGui implements DefaultEventListener<UpdaterEvent> {
    private static final UpdaterGui INSTANCE = new UpdaterGui();

    public static UpdaterGui getInstance() {
        return UpdaterGui.INSTANCE;
    }

    private JFrame  frame;
    private Storage storage;

    private UpdaterGui() {
        setLaf();
        storage = JSonStorage.getPlainStorage("WebUpdaterGUI");
        this.frame = new JFrame("JDownloader Updater");
        Dialog.getInstance().setParentOwner(this.frame);
        UpdaterController.getInstance().getEventSender().addListener(this);
        this.frame.addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent arg0) {
            }

            public void windowClosed(WindowEvent arg0) {
            }

            public void windowClosing(WindowEvent arg0) {
                UpdaterController.getInstance().requestExit();
            }

            public void windowDeactivated(WindowEvent arg0) {
            }

            public void windowDeiconified(WindowEvent arg0) {
            }

            public void windowIconified(WindowEvent arg0) {
            }

            public void windowOpened(WindowEvent arg0) {
            }

        });
        this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // set appicon
        final ArrayList<Image> list = new ArrayList<Image>();

        try {
            list.add(ImageProvider.getBufferedImage("icon", true));

            this.frame.setIconImages(list);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set Application dimensions and locations

        final Dimension dim = new Dimension(storage.get("DIMENSION_WIDTH", 1000), storage.get("DIMENSION_HEIGHT", 600));
        // restore size
        this.frame.setSize(dim);
        this.frame.setPreferredSize(dim);

        this.frame.setMinimumSize(new Dimension(100, 100));
        this.layoutGUI();
        // restore location. use center of screen as default.
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = screenSize.width / 2 - this.frame.getSize().width / 2;
        final int y = screenSize.height / 2 - this.frame.getSize().height / 2;

        this.frame.setLocation(storage.get("LOCATION_X", x), storage.get("LOCATION_Y", y));

        this.frame.pack();
        this.frame.setVisible(true);
    }

    private void layoutGUI() {
    }

    private void setLaf() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {

        }
    }

    public void start() {
    }

    public void dispose() {
        if (this.frame.getExtendedState() == Frame.NORMAL && this.frame.isShowing()) {

            storage.put("LOCATION_X", this.frame.getLocationOnScreen().x);
            storage.put("LOCATION_Y", this.frame.getLocationOnScreen().y);
            storage.put("DIMENSION_WIDTH", this.frame.getSize().width);
            storage.put("DIMENSION_HEIGHT", this.frame.getSize().height);

        }

        this.frame.setVisible(false);
        this.frame.dispose();
    }

    public void onEvent(UpdaterEvent event) {
        switch (event.getType()) {

        case EXIT_REQUEST:
            dispose();
            break;
        }
    }
}
