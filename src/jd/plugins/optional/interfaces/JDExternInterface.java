package jd.plugins.optional.interfaces;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.gui.UserIO;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Encoding;
import jd.nutils.JDFlags;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
 
@OptionalPlugin(rev = "$Revision$", id = "externinterface", interfaceversion = 4)
public class JDExternInterface extends PluginOptional {

    private RequestHandler handler;
    private HttpServer server = null;

    public JDExternInterface(PluginWrapper wrapper) {
        super(wrapper);
        handler = new RequestHandler();
        initpanel();
    }

    // @Override
    public boolean initAddon() {
        logger.info("Extern Interface API initialized on port 9666");
        try {
            server = new HttpServer(this.getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666), handler);
            server.start();
            return true;
        } catch (Exception e) {
        }
        return false;
    }



    // @Override
    public void onExit() {
        try {
            if (server != null) server.sstop();
        } catch (Exception e) {
        }
        server = null;
    }

    // @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    public void initpanel() {

    }

    class RequestHandler implements Handler {
        String jdpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/JDownloader.jar";
        private String namespace;
        private String[] splitPath;

        public void handle(Request request, Response response) {
            splitPath = request.getRequestUrl().substring(1).split("[/|\\\\]");
            namespace = splitPath[0];
            try {

                if (namespace.equalsIgnoreCase("flash")) {

                    if (splitPath.length > 1 && splitPath[1].equalsIgnoreCase("add")) {
                        askPermission(request);
                        /* parse the post data */
                        String urls[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("urls")));
                        String desc[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("descriptions")));
                        String passwords[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("passwords")));
                        for (String p : passwords)
                            PasswordListController.getInstance().addPassword(p);
                        String referer = request.getParameter("referer");
                        if (urls.length != 0) {
                            ArrayList<DownloadLink> links = new DistributeData(Encoding.htmlDecode(request.getParameters().get("urls"))).findLinks();
                            SimpleGUI.CURRENTGUI.addLinksToGrabber(links, false);
                            response.addContent("success\r\n");

                        } else {
                            response.addContent("failed\r\n");
                        }
                    } else if (splitPath.length > 1 && splitPath[1].equalsIgnoreCase("addcrypted")) {
                        askPermission(request);
                        /* parse the post data */
                        String dlc = Encoding.htmlDecode(request.getParameters().get("crypted")).trim().replace(" ", "+");
                        File tmp;
                        try {
                            JDUtilities.getResourceFile("tmp").mkdirs();
                            tmp = File.createTempFile("jd_", ".dlc", JDUtilities.getResourceFile("tmp"));

                            JDIO.saveToFile(tmp, dlc.getBytes());
                            ArrayList<DownloadLink> links = JDUtilities.getController().getContainerLinks(tmp);

                            SimpleGUI.CURRENTGUI.addLinksToGrabber(links, false);
                            response.addContent("success\r\n");
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        response.addContent(JDUtilities.getJDTitle() + "\r\n");
                    }
                    //

                } else if (request.getRequestUrl().equalsIgnoreCase("/crossdomain.xml")) {
                    response.addContent("<?xml version=\"1.0\"?>\r\n");
                    response.addContent("<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">\r\n");
                    response.addContent("<cross-domain-policy>\r\n");
                    response.addContent("<allow-access-from domain=\"jdownloader.org\" />\r\n");
                    response.addContent("<allow-access-from domain=\"jdownloader.net\" />\r\n");
                    response.addContent("<allow-access-from domain=\"jdownloader.net:8081\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.org\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.net\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.net:8081\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.org\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.net\" />\r\n");
                    response.addContent("<allow-access-from domain=\"linksave.in\" />\r\n");
                    response.addContent("<allow-access-from domain=\"relink.us\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.linksave.in\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.relink.us\" />\r\n");
                    response.addContent("</cross-domain-policy>\r\n");

                } else {
                    askPermission(request);
                    /*
                     * path and commandline to JD, so FlashGot can check
                     * existence and start jd if needed
                     */
                    response.addContent(jdpath + "\r\n");
                    response.addContent("java -Xmx512m -jar " + jdpath + "\r\n");
                    ArrayList<DownloadLink> links = null;
                    /* parse the post data */
                    String urls[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("urls")));
                    String desc[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("descriptions")));
                    String referer = request.getParameter("referer");
                    String post = request.getParameter("post");
                    if (urls.length != 0) {
                        links = new DistributeData(Encoding.htmlDecode(request.getParameters().get("urls"))).findLinks();
                        SimpleGUI.CURRENTGUI.addLinksToGrabber(links, false);
                    }
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }

        private void askPermission(Request request) throws Exception {
            String app = "unknown application";
            if (request.getHeader("user-agent") != null) {
                app = request.getHeader("user-agent").replaceAll("\\(.*\\)", "");
            }
            JDLogger.getLogger().warning("\r\n\r\n-----------------------External request---------------------");
            JDLogger.getLogger().warning("An external tool adds links to JDownloader. Request details:");
            JDLogger.getLogger().warning(request.toString());
            JDLogger.getLogger().warning(request.getParameters().toString());
            JDLogger.getLogger().warning("\r\n-----------------------External request---------------------");
            if (!JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, JDL.LF("jd.plugins.optional.interfaces.jdflashgot.security.title", "External request from %s to %s interface!", app, namespace), JDL.LF("jd.plugins.optional.interfaces.jdflashgot.security.message", "An external application tries to add links. See Log for details."), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.btn_allow", "Allow it!"), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.btn_deny", "Deny access!")), UserIO.RETURN_OK)) {

                JDLogger.getLogger().warning("Denied access.");
                throw new Exception("User denied access");
            }

        }

    }

}
