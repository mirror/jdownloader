package jd.gui.skins.simple.tasks;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class DownloadTaskPane extends TaskPanel implements ActionListener, ControlListener {

    /**
     * 
     */
    private static final long serialVersionUID = -9134449913836967453L;
    public static final int ACTION_SHOW_PANEL = 1;
    public static final int ACTION_STARTSTOP = 2;
    private SpeedMeterPanel speedmeter;

    private JButton startStop;

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill][grow]"));
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        speedmeter = new SpeedMeterPanel();
        speedmeter.setPreferredSize(new Dimension(100, 30));
        addButton(startStop = createButton(getStartStopText(), new ImageIcon(JDTheme.getImage(getStartStopDownloadImage(), 32, 32))));
        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_SHOW_SPEEDMETER, true)) {
            add(speedmeter, "gaptop 10,hidemode 3");
        }

    }

    private String getStartStopText() {
       
        
          if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
            return  JDLocale.L("gui.menu.action.start.name", "Start");
        } else {
            return  JDLocale.L("gui.menu.action.stop.name", "Stop");
        }
    }

    private JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading");
        return bt;
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == startStop) {
            this.broadcastEvent(new ActionEvent(this, ACTION_STARTSTOP, ((JButton) e.getSource()).getName()));
            return;
        }

    }

    public String getStartStopDownloadImage() {
        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
            return JDTheme.V("gui.images.next");
        } else {
            return JDTheme.V("gui.images.stop");
        }
    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {
                case ControlEvent.CONTROL_INIT_COMPLETE:

                    break;
                case ControlEvent.CONTROL_PLUGIN_ACTIVE:

                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:

                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:

                    break;

                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

                    if (speedmeter != null) speedmeter.stop();

                    break;
                case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:

                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:

                    break;
                case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
                    break;
                case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                    // if ((Property) event.getSource() ==
                    // JDUtilities.getConfiguration()) {
                    // if (event.getParameter().equals(Configuration.
                    // PARAM_DISABLE_RECONNECT)) {
                    // // btnReconnect.setIcon(new
                    // // ImageIcon(JDUtilities.getImage
                    // // (getDoReconnectImage())));
                    // } else if (event.getParameter().equals(Configuration.
                    // PARAM_CLIPBOARD_ALWAYS_ACTIVE)) {
                    // // btnClipBoard.setIcon(new
                    // // ImageIcon(JDUtilities.getImage
                    // // (getClipBoardImage())));
                    // }
                    // }
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_START:

                    if (speedmeter != null) speedmeter.start();
                    startStop.setIcon(new ImageIcon(JDTheme.getImage(getStartStopDownloadImage(), 16, 16)));
                    startStop.setText(getStartStopText());
                    // btnStartStop.setEnabled(true);
                    // btnPause.setEnabled(true);
                    // btnStartStop.setIcon(new
                    // ImageIcon(JDImage.getImage(getStartStopDownloadImage
                    // ())));
                    // btnPause.setIcon(new
                    // ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    if (speedmeter != null) speedmeter.stop();
                    startStop.setIcon(new ImageIcon(JDTheme.getImage(getStartStopDownloadImage(), 16, 16)));
                    startStop.setText(getStartStopText());
                    //                  
                    // btnStartStop.setEnabled(true);
                    // btnPause.setEnabled(true);
                    // btnStartStop.setIcon(new
                    // ImageIcon(JDImage.getImage(getStartStopDownloadImage
                    // ())));
                    // btnPause.setIcon(new
                    // ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;
                }
            }
        });
    }

 

   

}
