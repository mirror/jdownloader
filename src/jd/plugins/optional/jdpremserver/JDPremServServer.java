package jd.plugins.optional.jdpremserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.optional.interfaces.Handler;
import jd.plugins.optional.interfaces.HttpServer;
import jd.plugins.optional.interfaces.Request;
import jd.plugins.optional.interfaces.Response;

public class JDPremServServer implements Handler {

    private static final JDPremServServer INSTANCE = new JDPremServServer();
    private HttpServer server = null;

    private JDPremServServer() {

    }

    public synchronized void start() throws IOException {
        if (server != null) return;
        server = new HttpServer(31000, this, false);
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
        String[] splitPath = request.getRequestUrl().substring(1).split("[/|\\\\]");
        String namespace = splitPath[0];
        JDLogger.getLogger().finer(request.toString());
        JDLogger.getLogger().finer(namespace);
        JDLogger.getLogger().finer(request.getParameters().toString());
        if (request.getParameter("get") != null || request.getParameter("force") != null) {
            String wantedUrl = request.getParameter("force");
            if (wantedUrl == null) wantedUrl = request.getParameter("get");
            /* user wants to download the give url */
            DownloadLink ret = JDPremServController.getInstance().getDownloadLink(Encoding.urlDecode(wantedUrl, true));
            if (ret == null) {
                /* ERROR: 0 = no downloadlink available */
                response.setReturnStatus(Response.ERROR);
                response.addContent(new String("ERROR: 0"));
            } else {
                if (ret.getLinkStatus().isFinished()) {
                    /* finished? */
                    File file = new File(ret.getFileOutput());
                    if (file.exists() && file.isFile()) {
                        /* OK: 1 = finished and still available on disk */
                        response.setReturnStatus(Response.OK);
                        response.addContent(new String("OK: 100 || " + ret.getDownloadSize()));
                    } else {
                        /*
                         * ERROR: -100 = finished but no longer available on
                         * disk
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
                        StringBuilder sb = new StringBuilder("OK: 1 ||");
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
                        StringBuilder sb = new StringBuilder("OK: 0 ||");
                        sb.append(ret.getDownloadCurrent() + "/" + ret.getDownloadSize());
                        response.addContent(sb.toString());
                    }
                }
            }
            return;
        }
    }
}
