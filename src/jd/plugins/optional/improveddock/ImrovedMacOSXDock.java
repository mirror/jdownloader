package jd.plugins.optional.improveddock;

import jd.PluginWrapper;
import jd.controlling.DownloadWatchDog;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;

import java.util.ArrayList;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = false, id = "improvedmacosxdock", interfaceversion = 5, minJVM = 1.6, windows = false, linux = false)

public class ImrovedMacOSXDock extends PluginOptional {
    
    private Thread updateThread;

    public ImrovedMacOSXDock(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public void onControlEvent(ControlEvent event) {


        switch(event.getID()) {

            case ControlEvent.CONTROL_DOWNLOAD_START:
                updateThread = new Thread("Improved Mac OSX Dock Updater") {
                    @Override
                    public void run() {
                        while (true) {
                            if (DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.RUNNING) break;
                            //JDGui.getInstance().setWindowTitle("JD AC: " +  + " DL: " + Formatter.formatReadable(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage()));
                            /*
                            for(DownloadLink link : watchdog.getRunningDownloads()) {
                            }*/

                            //MacDockIconChanger.getInstance().setCompleteDownloadcount(DownloadWatchDog.getInstance().getDownloadssincelastStart());
                            //MacDockIconChanger.getInstance().changeToProcent(50);

                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                            }
                        }
                        JDGui.getInstance().setWindowTitle(JDUtilities.getJDTitle());
                    }
                };
                updateThread.start();
                break;

            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
            case ControlEvent.CONTROL_DOWNLOAD_STOP:
                if (updateThread != null) updateThread.interrupt();
                JDGui.getInstance().setWindowTitle(JDUtilities.getJDTitle());
                break;
        }
    }
}
