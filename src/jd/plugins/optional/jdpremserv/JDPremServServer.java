package jd.plugins.optional.jdpremserv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.optional.interfaces.Handler;
import jd.plugins.optional.interfaces.HttpServer;
import jd.plugins.optional.interfaces.Request;
import jd.plugins.optional.interfaces.Response;
import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServUser;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;

public class JDPremServServer implements Handler, ControlListener {

    private static final JDPremServServer INSTANCE = new JDPremServServer();
    private HttpServer server = null;
    private static final HashMap<String, ArrayList<DownloadLink>> startedLinks = new HashMap<String, ArrayList<DownloadLink>>();
    private static final Object LOCK = new Object();
    private boolean listenerAdded = false;

    private JDPremServServer() {
    }

    public synchronized void start(int port) throws IOException {
        if (server != null) return;
        server = new HttpServer(port, this, false);
        server.start();
        JDPremServController.getInstance().start();
        if (!listenerAdded) {
            JDController.getInstance().addControlListener(this);
            listenerAdded = true;
        }
    }

    public synchronized void stop() throws IOException {
        if (server == null) return;
        try {
            server.sstop();
        } finally {
            server = null;
            JDPremServController.getInstance().stop();
        }
    }

    public static JDPremServServer getInstance() {
        return INSTANCE;
    }

    public void handle(Request request, Response response) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String dlpw = request.getParameter("dlpw");
        String resetUrl = request.getParameter("reset");
        if (resetUrl != null) resetUrl = Encoding.urlDecode(resetUrl, false);
        if (username != null) username = Encoding.urlDecode(username, false);
        if (password != null) password = Encoding.urlDecode(password, false);
        if (dlpw != null) dlpw = Encoding.urlDecode(dlpw, false);
        if (resetUrl == null) {
            /* do not validate userinfo if reset function is called */
            if (username == null || username.length() == 0 || password == null || password.length() == 0 || !UserController.getInstance().isUserAllowed(username, password)) {
                /* ERROR: -10 = invalid user */
                response.setReturnStatus(Response.ERROR);
                response.addContent(new String("ERROR: -10"));
                return;
            }
        }
        if (request.getParameter("reset") != null) {
            /* reset downloadlink if possible */
            if (JDPremServController.getInstance().resetDownloadLink(resetUrl)) {
                response.setReturnStatus(Response.OK);
                response.addContent(new String("OK: RESET"));
            } else {
                response.setReturnStatus(Response.ERROR);
                response.addContent(new String("ERROR: RESET"));
            }
        } else if (request.getParameter("info") != null) {
            /* build user info for fetchaccountinfo */
            response.setReturnStatus(Response.OK);
            StringBuilder sb = new StringBuilder("OK: USER || HOSTS: ");
            for (HostPluginWrapper plugin : JDUtilities.getPremiumPluginsForHost()) {
                if (AccountController.getInstance().getValidAccount(plugin.getHost()) != null) {
                    sb.append(plugin.getHost() + "||");
                }
            }
            response.addContent(sb.toString());
        } else if (request.getParameter("get") != null || request.getParameter("force") != null) {
            String wantedUrl = request.getParameter("force");
            if (wantedUrl == null) wantedUrl = request.getParameter("get");
            /* user wants to download the give url */
            DownloadLink ret = JDPremServController.getInstance().getDownloadLink(Encoding.urlDecode(wantedUrl, true));
            /* set downloadpassword if available */
            if (dlpw != null) ret.setProperty("pass", dlpw);
            response.setAdditionalData(ret);
            if (ret == null) {
                /* ERROR: 0 = no downloadlink available */
                response.setReturnStatus(Response.ERROR);
                response.addContent(new String("ERROR: 0"));
                return;
            }
            if (!UserController.getInstance().isUserAllowed(username, password, ret.getHost())) {
                /* ERROR: -20 = not allowed */
                response.setReturnStatus(Response.ERROR);
                response.addContent(new String("ERROR: -20"));
                return;
            }
            if (ret.getLinkStatus().isFinished() && !ret.getLinkStatus().isPluginActive()) {
                /* finished? */
                File file = new File(ret.getFileOutput());
                if (file.exists() && file.isFile()) {
                    if (request.getParameter("download") != null) {
                        /* download file */
                        response.setReturnStatus(Response.OK);
                        JDPremServController.getInstance().addRequestedDownload(ret);
                        if (request.getHeader("range") == null) {
                            response.setFileServe(ret.getFileOutput(), 0, -1, new File(ret.getFileOutput()).length(), false);
                        } else {
                            String[] dat = new Regex(request.getHeader("range"), "bytes=(\\d+)-(\\d+)?").getRow(0);
                            if (dat[1] == null) {
                                response.setFileServe(ret.getFileOutput(), Long.parseLong(dat[0]), -1, new File(ret.getFileOutput()).length(), true);
                            } else {
                                response.setFileServe(ret.getFileOutput(), Long.parseLong(dat[0]), Long.parseLong(dat[1]), new File(ret.getFileOutput()).length(), true);
                            }
                        }
                    } else {
                        /* request status info */
                        /* OK: 1 = finished and still available on disk */
                        response.setReturnStatus(Response.OK);
                        response.addContent(new String("OK: 100 || " + ret.getDownloadSize()));
                    }
                } else {
                    /*
                     * ERROR: -100 = finished but no longer available on disk
                     */
                    response.setReturnStatus(Response.ERROR);
                    response.addContent(new String("ERROR: -100"));
                }
            } else if (ret.getLinkStatus().isFailed()) {
                /* download failed */
                /* ERROR: -50 = failed */
                response.setReturnStatus(Response.ERROR);
                response.addContent(new String("ERROR: -50 || " + ret.getLinkStatus().getStatusString()));
            } else {
                /* not finished? */
                if (ret.getLinkStatus().isPluginActive()) {
                    if (!ret.getTransferStatus().usesPremium()) {
                        /* download does not use premium account */
                        response.setReturnStatus(Response.OK);
                        StringBuilder sb = new StringBuilder("OK: 2 || ");
                        sb.append(ret.getDownloadCurrent() + "/" + ret.getDownloadSize() + "/" + ret.getDownloadSpeed());
                        response.addContent(sb.toString());
                    } else {
                        /* download uses premium account */
                        response.setReturnStatus(Response.OK);
                        StringBuilder sb = new StringBuilder("OK: 1 || ");
                        sb.append(ret.getDownloadCurrent() + "/" + ret.getDownloadSize() + "/" + ret.getDownloadSpeed());
                        response.addContent(sb.toString());
                    }
                } else {
                    synchronized (LOCK) {
                        ArrayList<DownloadLink> userSt = startedLinks.get(username.toLowerCase());
                        if (userSt == null) {
                            userSt = new ArrayList<DownloadLink>();
                            startedLinks.put(username.toLowerCase(), userSt);
                        }
                        if (userSt.size() < 1) {
                            userSt.add(ret);
                            /* user can still force downloads */
                            ArrayList<DownloadLink> forced = new ArrayList<DownloadLink>();
                            forced.add(ret);
                            DownloadWatchDog.getInstance().forceDownload(forced);
                        } else {
                            /* normal download */
                            DownloadWatchDog.getInstance().startDownloads();
                            if (!ret.isEnabled()) ret.setEnabled(true);
                        }
                    }
                    /* download not running */
                    response.setReturnStatus(Response.OK);
                    StringBuilder sb = new StringBuilder("OK: 0 || ");
                    sb.append(ret.getDownloadCurrent() + "/" + ret.getDownloadSize());
                    response.addContent(sb.toString());
                }
            }
        } else {
            /* ERROR: -1000 = unknown request */
            response.setReturnStatus(Response.ERROR);
            response.addContent(new String("ERROR: -1000"));
        }
    }

    public void finish(Request request, Response res) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        DownloadLink link = null;
        if (res.getAdditionalData() != null && res.getAdditionalData() instanceof DownloadLink) {
            link = (DownloadLink) res.getAdditionalData();
            JDPremServController.getInstance().removeRequestedDownload(link);
        }
        if (username != null && password != null && res.getAdditionalData() != null && res.getAdditionalData() instanceof DownloadLink) {
            PremServUser user = UserController.getInstance().getUserByUserName(username);
            if (user != null) user.addTrafficLog(link.getHost(), Math.max(0, res.getFileBytesServed()));
        }

    }

    public void controlEvent(ControlEvent event) {
        if (event == null) return;
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getCaller() instanceof PluginForHost)) return;
            DownloadLink lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            synchronized (LOCK) {
                startedLinks.values().remove(lastDownloadFinished);
            }
            break;
        }

    }
}
