package jd.plugins.optional.jdnotifyme;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.update.WebUpdater;

@OptionalPlugin(rev = "$Revision: 10393 $", defaultEnabled = true, id = "jdnotifyme", interfaceversion = 5, minJVM = 1.5)
public class JDNotifyMe extends PluginOptional implements Runnable {

    private long wait = 0;
    private final static String WAITTIME = "WAITTIME";
    private String currentBranch;
    private Thread checkThread = null;

    public JDNotifyMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public synchronized boolean initAddon() {
        try {
            WebUpdater dd = new WebUpdater();
            currentBranch = dd.getBranch();
            wait = Math.max(0, getPluginConfig().getIntegerProperty(WAITTIME, 10 * 60) * 1000);
            if (wait == 0) return false;
            checkThread = new Thread(this);
            checkThread.start();
            return true;
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
        return false;

    }

    @Override
    public void onExit() {
        this.notify();
        checkThread = null;
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    public void run() {
        while (checkThread != null) {
            try {
                int lastRead = getPluginConfig().getIntegerProperty(currentBranch.toLowerCase() + "_lastRead", 0);
                String lastAvail = br.getPage("http://update3.jdownloader.org/branches/" + currentBranch + "/notify_lastAvail?random=" + System.currentTimeMillis());
                if (br.getHttpConnection().isOK()) {
                    int last = Integer.parseInt(lastAvail.trim());
                    if (last < lastRead) {
                        getPluginConfig().setProperty(currentBranch.toLowerCase() + "_lastRead", lastRead);
                    } else {
                        int newAvail = last - lastRead;
                        if (newAvail > 0) {
                            /* hier noch das anzeigen der meldungen rein */
                            getPluginConfig().setProperty(currentBranch.toLowerCase() + "_lastRead", last);
                        }
                    }
                }
            } catch (Throwable e) {
            }
            synchronized (this) {
                try {
                    wait(wait);
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
