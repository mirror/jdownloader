package jd.plugins.optional.jdpremserv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.controlling.DownloadWatchDog;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.optional.interfaces.Handler;
import jd.plugins.optional.interfaces.HttpServer;
import jd.plugins.optional.interfaces.Request;
import jd.plugins.optional.interfaces.Response;
import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServUser;

public class JDPremServServer implements Handler {

    private static final JDPremServServer INSTANCE = new JDPremServServer();
    private HttpServer server = null;

    private JDPremServServer() {
    }

    public synchronized void start() throws IOException {
        if (server != null) return;
        server = new HttpServer(8080, this, false);
        server.start();
        JDPremServController.getInstance().start();
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
        if (username != null) username = Encoding.urlDecode(username, false);
        if (password != null) password = Encoding.urlDecode(password, false);
        if (username == null || username.length() == 0 || password == null || password.length() == 0 || !UserController.getInstance().isUserAllowed(username, password)) {
            /* ERROR: -10 = invalid user */
            response.setReturnStatus(Response.ERROR);
            response.addContent(new String("ERROR: -10"));
            return;
        }
        if (request.getParameter("info") != null) {
            response.setReturnStatus(Response.OK);
            response.addContent(new String("OK: USER"));
        } else if (request.getParameter("get") != null || request.getParameter("force") != null) {
            String wantedUrl = request.getParameter("force");
            if (wantedUrl == null) wantedUrl = request.getParameter("get");
            /* user wants to download the give url */
            DownloadLink ret = JDPremServController.getInstance().getDownloadLink(Encoding.urlDecode(wantedUrl, true));
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
            if (ret.getLinkStatus().isFinished()) {
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
                response.addContent(new String("ERROR: -50"));
            } else {
                /* not finished? */
                if (ret.getLinkStatus().isPluginActive()) {
                    /* download läuft */
                    response.setReturnStatus(Response.OK);
                    StringBuilder sb = new StringBuilder("OK: 1 || ");
                    sb.append(ret.getDownloadCurrent() + "/" + ret.getDownloadSize() + "/" + ret.getDownloadSpeed());
                    response.addContent(sb.toString());
                } else {
                    if (request.getParameter("force") != null) {
                        /* forced download */
                        ArrayList<DownloadLink> forced = new ArrayList<DownloadLink>();
                        forced.add(ret);
                        DownloadWatchDog.getInstance().forceDownload(forced);
                    } else {
                        /* normal download */
                        DownloadWatchDog.getInstance().startDownloads();
                    }
                    /* download läuft nicht */
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
}
