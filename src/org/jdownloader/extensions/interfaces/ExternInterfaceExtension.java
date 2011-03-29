//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.interfaces;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.update.JDUpdater;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public class ExternInterfaceExtension extends AbstractExtension {

    @Override
    protected void stop() throws StopException {
        try {
            if (server != null) server.sstop();
        } catch (Exception e) {
            throw new StopException(e);
        }
        server = null;
    }

    @Override
    protected void start() throws StartException {
        logger.info("Extern Interface API initialized on port " + this.getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666));

        try {
            server = new HttpServer(this.getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666), handler, getPluginConfig().getBooleanProperty(LOCALONLY, true));
            server.start();
        } catch (Exception e) {
            throw new StartException(e);
        }

    }

    @Override
    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), LOCALONLY, JDL.L("jd.plugins.optional.interfaces.JDExternInterface.localonly", "Listen only on localhost?")).setDefaultValue(true));

    }

    @Override
    public String getConfigID() {
        return "externinterface";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Extern Interface required by Click'n'Load, Flashgot, and others";
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;

    }

    private RequestHandler      handler;
    private HttpServer          server    = null;
    private final static String LOCALONLY = "localonly";
    private static String       jdpath    = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + File.separator + "JDownloader.jar";

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public ExternInterfaceExtension() throws StartException {
        super("Extern Interface");

        handler = new RequestHandler();
    }

    public static String decrypt(byte[] b, byte[] key) {
        Cipher cipher;
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(key);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b));
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    /**
     * @param crypted
     * @param jk
     * @param k
     * @param passwords
     * @param source
     */
    public static void decrypt(String crypted, final String jk, String k, String password, String source) {
        byte[] key = null;

        if (jk != null) {

            Context cx = null;
            try {
                try {
                    cx = ContextFactory.getGlobal().enterContext();
                    cx.setClassShutter(new ClassShutter() {
                        public boolean visibleToScripts(String className) {
                            if (className.startsWith("adapter")) {
                                return true;
                            } else {
                                throw new RuntimeException("Security Violation");
                            }

                        }
                    });
                } catch (java.lang.SecurityException e) {
                    /* in case classshutter already set */
                }
                Scriptable scope = cx.initStandardObjects();
                String fun = jk + "  f()";
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                key = JDHexUtils.getByteArray(Context.toString(result));
            } finally {
                if (cx != null) Context.exit();
            }

        } else {
            key = JDHexUtils.getByteArray(k);
        }
        byte[] baseDecoded = Base64.decode(crypted);
        String decryted = decrypt(baseDecoded, key).trim();

        String passwords[] = Regex.getLines(password);
        /* links were not encoded, so we dont have to htmlDecode them again */
        ArrayList<DownloadLink> links = new DistributeData(decryted).findLinks();
        for (DownloadLink link : links)
            link.addSourcePluginPasswords(passwords);
        for (DownloadLink l : links) {
            if (source != null) {
                l.setBrowserUrl(source);
            }
        }
        LinkGrabberController.getInstance().addLinks(links, false, false);

    }

    /**
     * This function will transform a message into a JSONP function if callback
     * is specified
     * 
     * @param message
     *            content of the request
     * @param callback
     *            the request's callback parameter
     * @return the content of the message. if callback != null, packed in valid
     *         JSONP construction.
     */
    private static String addJSONPCallback(String message, String callback) {
        if (callback != null) {
            return Encoding.urlDecode(callback, false) + "({\"content\": \"" + message + "\"})\r\n";
        } else {
            return message + "\r\n";
        }
    }

    /**
     * @see #addJSONPCallback(String,String)
     */
    private static String addJSONPCallback(String message, Request request) {
        return addJSONPCallback(message, request.getParameter("callback"));
    }

    @Override
    public String getIconKey() {
        return "gui.images.flashgot";
    }

    class RequestHandler implements Handler {
        private String   namespace;
        private String[] splitPath;

        public void handle(Request request, Response response) {
            splitPath = request.getRequestUrl().substring(1).split("[/|\\\\]");
            namespace = splitPath[0];
            JDLogger.getLogger().finer(request.toString());
            JDLogger.getLogger().finer(request.getParameters().toString());

            try {
                if (namespace.equalsIgnoreCase("update")) {

                    String branch = request.getParameters().get("branch");
                    String ref = request.getHeader("referer");

                    if (!ref.equalsIgnoreCase("http://jdownloader.org/beta") && !ref.equalsIgnoreCase("http://jdownloader.net:8081/beta")) return;
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            SwingGui.getInstance().getMainFrame().toFront();
                            SwingGui.getInstance().getMainFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
                            return null;
                        }

                    }.waitForEDT();

                    if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, JDL.L("updater.beta.rlyupdate.title", "Update to beta now?"), JDL.LF("updater.beta.rlyupdate.message", "Do you want to update to JD-%s", branch)))) {
                        JDUpdater.getInstance().setBranchInUse(branch);

                        WebUpdate.doUpdateCheck(false);
                    }

                } else if (namespace.equalsIgnoreCase("flash") || namespace.equalsIgnoreCase("jsonp")) {
                    if (splitPath.length > 1) {
                        if (splitPath[1].equalsIgnoreCase("add")) {
                            askPermission(request);
                            /* parse the post data */
                            String urls[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("urls")));
                            String passwords[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("passwords")));
                            String source = Encoding.urlDecode(request.getParameters().get("source"), false);
                            String comment = Encoding.urlDecode(request.getParameters().get("comment"), false);
                            if (urls.length != 0) {
                                ArrayList<DownloadLink> links = new DistributeData(Encoding.htmlDecode(request.getParameters().get("urls"))).findLinks();
                                for (DownloadLink link : links) {
                                    link.addSourcePluginPasswords(passwords);
                                    if (comment != null) link.setSourcePluginComment(comment);
                                    if (source != null) link.setBrowserUrl(source);
                                }
                                LinkGrabberController.getInstance().addLinks(links, false, false);
                                new GuiRunnable<Object>() {

                                    @Override
                                    public Object runSave() {
                                        SwingGui.getInstance().getMainFrame().toFront();

                                        return null;
                                    }

                                }.waitForEDT();
                                response.addContent(addJSONPCallback("success", request));
                            } else {
                                response.addContent(addJSONPCallback("failed", request));
                            }
                        } else if (splitPath[1].equalsIgnoreCase("addcrypted")) {
                            askPermission(request);
                            /* parse the post data */
                            String dlc = Encoding.htmlDecode(request.getParameters().get("crypted")).trim().replace(" ", "+");
                            File tmp = JDUtilities.getResourceFile("tmp/jd_" + System.currentTimeMillis() + ".dlc", true);
                            tmp.deleteOnExit();

                            JDIO.saveToFile(tmp, dlc.getBytes());
                            ArrayList<DownloadLink> links = JDUtilities.getController().getContainerLinks(tmp);

                            LinkGrabberController.getInstance().addLinks(links, false, false);
                            new GuiRunnable<Object>() {
                                @Override
                                public Object runSave() {
                                    SwingGui.getInstance().getMainFrame().toFront();
                                    return null;
                                }
                            }.waitForEDT();
                            response.addContent(addJSONPCallback("success", request));
                        } else if (splitPath[1].equalsIgnoreCase("addcrypted2")) {
                            askPermission(request);
                            /* parse the post data */
                            String crypted = Encoding.htmlDecode(request.getParameters().get("crypted")).trim().replace(" ", "+");
                            String jk = Encoding.urlDecode(request.getParameters().get("jk"), false);
                            String k = Encoding.urlDecode(request.getParameters().get("k"), false);
                            String passwords = Encoding.urlDecode(request.getParameters().get("passwords"), false);
                            String source = Encoding.urlDecode(request.getParameters().get("source"), false);
                            try {
                                decrypt(crypted, jk, k, passwords, source);

                                response.addContent(addJSONPCallback("success", request));
                                new GuiRunnable<Object>() {
                                    @Override
                                    public Object runSave() {
                                        SwingGui.getInstance().getMainFrame().toFront();
                                        return null;
                                    }
                                }.waitForEDT();
                            } catch (Exception e) {
                                JDLogger.exception(e);

                                response.addContent(addJSONPCallback("failed " + e.getMessage(), request.getParameters().get("callback")));
                            }
                        } else if (splitPath[1].equalsIgnoreCase("checkSupportForUrl")) {

                            // TODO: Bugtracker Issue #1729
                            String url = Encoding.htmlDecode(request.getParameters().get("url"));
                            String urlSupported = Boolean.valueOf(DistributeData.hasPluginFor(url, true)).toString();
                            response.addContent(addJSONPCallback(urlSupported, request.getParameters().get("callback")));
                        } else {
                            response.addContent(addJSONPCallback("unknowncommand", request.getParameters().get("callback")));
                        }
                    } else {
                        response.addContent(addJSONPCallback(JDUtilities.getJDTitle(), request.getParameters().get("callback")));
                    }
                } else if (request.getRequestUrl().equalsIgnoreCase("/jdcheck.js")) {

                    response.addContent("jdownloader=true;\r\n");
                    response.addContent("var version='" + JDUtilities.getRevision() + "';\r\n");

                } else if (request.getRequestUrl().equalsIgnoreCase("/crossdomain.xml")) {
                    response.addContent("<?xml version=\"1.0\"?>\r\n");
                    response.addContent("<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">\r\n");
                    response.addContent("<cross-domain-policy>\r\n");
                    response.addContent("<allow-access-from domain=\"*\" />\r\n");

                    response.addContent("</cross-domain-policy>\r\n");
                } else if (namespace.equalsIgnoreCase("flashgot")) {
                    /*
                     * path and commandline to JD, so FlashGot can check
                     * existence and start jd if needed
                     */
                    response.addContent(jdpath + "\r\n");
                    response.addContent("java -Xmx512m -jar " + jdpath + "\r\n");

                    if (request.getHeader("referer") == null || (!request.getHeader("referer").equalsIgnoreCase("http://localhost:" + getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666) + "/flashgot") && !request.getHeader("referer").equalsIgnoreCase("http://127.0.0.1:" + getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666) + "/flashgot"))) {
                        /*
                         * security check for flashgot referer, skip asking if
                         * we find valid flashgot referer
                         */
                        askPermission(request);
                    } else {
                        JDLogger.getLogger().info("Valid FlashGot Referer found, skipping AskPermission");
                    }
                    String urls[] = Regex.getLines(Encoding.urlDecode(request.getParameters().get("urls"), false));
                    String desc[] = Regex.getLines(Encoding.urlDecode(request.getParameters().get("descriptions"), false));
                    String dir = null;
                    FilePackage fp = FilePackage.getInstance();
                    String packname = Encoding.urlDecode(request.getParameters().get("package"), false);
                    if (packname != null) {
                        fp.setName(packname);
                    } else {
                        fp.setName("FlashGot");
                    }
                    fp.setProperty(LinkGrabberController.DONTFORCEPACKAGENAME, "yes");
                    if (request.getParameters().get("dir") != null) {
                        dir = Encoding.urlDecode(request.getParameters().get("dir"), false).trim();
                        fp.setDownloadDirectory(dir);
                    }
                    String cookies = null;
                    if (request.getParameters().get("cookies") != null) cookies = Encoding.urlDecode(request.getParameters().get("cookies"), false);
                    String post = null;
                    if (request.getParameters().get("postData") != null) post = Encoding.urlDecode(request.getParameters().get("postData"), false);
                    boolean autostart = false;
                    if (request.getParameters().get("autostart") != null && request.getParameter("autostart").startsWith("1")) autostart = true;
                    String referer = Encoding.urlDecode(request.getParameter("referer"), false);
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                    if (urls.length != 0) {
                        for (int i = 0; i < urls.length; i++) {
                            String url = urls[i];
                            DistributeData dt = new DistributeData(url);
                            dt.setFilterNormalHTTP(true);
                            ArrayList<DownloadLink> foundlinks = dt.findLinks();
                            if (foundlinks.size() > 0) {
                                for (DownloadLink dl : foundlinks) {
                                    if (!dl.gotBrowserUrl()) dl.setBrowserUrl(referer);
                                    if (i < desc.length) dl.setSourcePluginComment(desc[i]);
                                }
                                links.addAll(foundlinks);
                            } else {
                                /* directlinks here */
                                PluginForHost defaultplg = JDUtilities.getPluginForHost("DirectHTTP");
                                PluginForHost liveplg = defaultplg.getWrapper().getNewPluginInstance();
                                String name = Plugin.getFileNameFromURL(new URL(url));
                                DownloadLink direct = new DownloadLink(defaultplg, name, "DirectHTTP", url, true);
                                direct.setBrowserUrl(referer);
                                if (i < desc.length) direct.setSourcePluginComment(desc[i]);
                                direct.setProperty("cookies", cookies);
                                direct.setProperty("post", post);
                                direct.setProperty("referer", referer);
                                liveplg.correctDownloadLink(direct);
                                links.add(direct);
                            }
                        }
                        fp.addLinks(links);
                        LinkGrabberController.getInstance().addLinks(links, autostart, autostart);
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                SwingGui.getInstance().getMainFrame().toFront();

                                return null;
                            }

                        }.waitForEDT();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            String url = request.getHeader("referer");
            if (url == null) {
                url = request.getHeader("referrer");
            }
            if (url == null) {
                url = request.getHeader("source");
            }
            String jdrandomNumber = request.getHeader("jd.randomnumber");
            if (jdrandomNumber != null && jdrandomNumber.equalsIgnoreCase(System.getProperty("jd.randomNumber"))) {
                JDLogger.getLogger().warning("Request by JD granted");
                return;
            }
            app = url != null ? new URL(url).getHost() : app;
            if (!JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN, JDL.LF("jd.plugins.optional.interfaces.jdflashgot.security.title", "External request from %s to %s interface!", app, namespace), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.message", "An external application tries to add links. See Log for details."), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.btn_allow", "Allow it!"), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.btn_deny", "Deny access!")), UserIO.RETURN_OK)) {
                JDLogger.getLogger().warning("Denied access.");
                throw new Exception("User denied access");
            }

        }

        public void finish(Request req, Response res) {

        }

    }

    @Override
    protected void initExtension() throws StartException {
    }

}
