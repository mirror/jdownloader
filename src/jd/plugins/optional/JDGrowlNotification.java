package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.Executer;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.OSDetector;

public class JDGrowlNotification extends PluginOptional {
    public static int getAddonInterfaceVersion() {
        return 2;
    }

    public JDGrowlNotification(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PROPERTY_ENABLED = "PROPERTY_ENABLED";

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdgrowlnotification.name", "JDGrowlNotification");
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        logger.info("Growl OK");

        return true;
    }

    public void actionPerformed(ActionEvent e) {
        MenuItem mi = (MenuItem) e.getSource();
        if (mi.getActionID() == 0) {
            getPluginConfig().setProperty(PROPERTY_ENABLED, !getPluginConfig().getBooleanProperty(PROPERTY_ENABLED, false));
            getPluginConfig().save();
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;

        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdgrowlnotification.menu.enable", "Meldungen aktivieren"), 0).setActionListener(this));
        m.setSelected(this.getPluginConfig().getBooleanProperty(PROPERTY_ENABLED, false));

        return menu;
    }

    public void controlEvent(ControlEvent event) {

        super.controlEvent(event);

        if (getPluginConfig().getBooleanProperty(PROPERTY_ENABLED, false)) {
            switch (event.getID()) {
            case ControlEvent.CONTROL_INIT_COMPLETE:
                growlNotification("jDownloader gestartet...", getDateAndTime(), "Programmstart");
                break;
            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                growlNotification("Alle Downloads beendet", "", "Alle Downloads fertig");
                break;
            case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                if (!(event.getSource() instanceof PluginForHost)) { return; }
                DownloadLink lastLink = ((SingleDownloadController) event.getParameter()).getDownloadLink();
                if (lastLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    growlNotification("Download beendet", lastLink.getFinalFileName(), "Download erfolgreich beendet");
                }
                break;
            case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
                growlNotification("Download abgebrochen", "", "Download abgebrochen");
                break;
            default:
                break;
            }
        }
    }

    public void growlNotification(String headline, String message, String title) {
        if (OSDetector.isMac()) {
            Executer exec = new Executer("/usr/bin/osascript");
            exec.addParameter(JDUtilities.getResourceFile("jd/osx/growlNotification.scpt").getAbsolutePath());
            exec.addParameter(headline);
            exec.addParameter(message);

            exec.addParameter(title);
            exec.start();
        }
    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    public String getDateAndTime() {
        DateFormat dfmt = new SimpleDateFormat("'Am 'EEEE.', den' dd.MM.yy 'um' hh:mm:ss");
        return dfmt.format(new Date());
    }
}
