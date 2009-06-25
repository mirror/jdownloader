package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.controlling.DistributeData;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Encoding;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.nutils.nativeintegration.LocaleBrowser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class FlashGotRequestHandler implements Handler {
    String jdpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/JDownloader.jar";

    public void handle(Request request, Response response) {
        /*
         * path and commandline to JD, so FlashGot can check existence and start
         * jd if needed
         */
        response.addContent(jdpath + "\r\n");
        response.addContent("java -Xmx512m -jar " + jdpath + "\r\n");
        ArrayList<DownloadLink> links = null;
        /* parse the post data */
        String urls[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("urls")));
        String desc[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("descriptions")));
        String referer = request.getParameter("referer");
        if (urls.length != 0) {
            links = new DistributeData(Encoding.htmlDecode(request.getParameters().get("urls"))).findLinks();
            SimpleGUI.CURRENTGUI.addLinksToGrabber(links, false);
        }
    }

}

public class JDFlashGot extends PluginOptional {

    private FlashGotRequestHandler handler;
    private HttpServer server = null;

    public JDFlashGot(PluginWrapper wrapper) {
        super(wrapper);
        handler = new FlashGotRequestHandler();
        initpanel();
    }

    public static int getAddonInterfaceVersion() {
        return 3;
    }

    @Override
    public boolean initAddon() {
        logger.info("FlashGot API initialized");
        try {
            server = new HttpServer(9666, handler);
            server.start();
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public String getIconKey() {
        return "gui.images.addons.flashgot";
    }

    @Override
    public void onExit() {
        try {
            if (server != null) server.sstop();
        } catch (Exception e) {
        }
        server = null;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 6033 $");
    }

    public String getHost() {
        return JDL.L("plugins.optional.flashgot.name", "FlashGot Integration");
    }

    public void initpanel() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                LocaleBrowser.openinFirefox("http://flashgot.net/getit");
            }

        }, JDL.L("gui.config.general.cnl.install", "Install")));
    }

}
