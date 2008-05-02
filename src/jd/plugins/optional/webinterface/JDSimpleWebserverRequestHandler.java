package jd.plugins.optional.webinterface;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.DistributeData;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

public class JDSimpleWebserverRequestHandler {

    private HashMap<String, String> headers;
    private JDSimpleWebserverResponseCreator response;

    private Logger logger = JDUtilities.getLogger();

    public JDSimpleWebserverRequestHandler(HashMap<String, String> headers, JDSimpleWebserverResponseCreator response) {
        this.response = response;
        this.headers = headers;
    }

    public void handle() {

        String request = headers.get(null);
        // logger.info(request);
        String[] requ = request.split(" ");

        String method = requ[0];
        String cPath = requ[1];
        String protocol = requ[2];
        String path, querry;
        path = cPath.substring(1);
        String[] params;
        HashMap<String, String> requestParameter = new HashMap<String, String>();

        /* bekanntgebung der mehrfach belegbaren parameter */
        requestParameter.put("package_all_downloads_counter", "0");
        requestParameter.put("package_single_download_counter", "0");

        if (cPath.indexOf("?") >= 0) {
            querry = cPath.substring(cPath.indexOf("?") + 1);
            path = cPath.substring(1, cPath.indexOf("?"));
            params = querry.split("\\&");

            for (String entry : params) {
                entry = entry.trim();
                int index = entry.indexOf("=");
                String key = entry;

                String value = null;
                if (index >= 0) {
                    key = entry.substring(0, index);
                    value = entry.substring(index + 1);
                }

                if (requestParameter.containsKey(key) || requestParameter.containsKey(key + "_counter")) {
                    /*
                     * keys mit _counter können mehrfach belegt werden, müssen
                     * vorher aber bekannt gegeben werden
                     */
                    if (requestParameter.containsKey(key + "_counter")) {
                        Integer keycounter = 0;
                        keycounter = JDUtilities.filterInt(requestParameter.get(key + "_counter"));
                        keycounter++;
                        requestParameter.put(key + "_counter", keycounter.toString());
                        requestParameter.put(key + "_" + keycounter.toString(), value);
                    }
                    ;

                } else
                    requestParameter.put(key, value);
            }
        }
        // logger.info(requestParameter.toString());

        /* parsen der paramter */
        if (requestParameter.containsKey("do")) {
            if (requestParameter.get("do").compareToIgnoreCase("submit") == 0) {
                logger.info("submit wurde gedrückt");
                if (requestParameter.containsKey("speed")) {
                    int setspeed = JDUtilities.filterInt(requestParameter.get("speed"));
                    if (setspeed < 0) setspeed = 0;
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, setspeed);
                }
                ;

                if (requestParameter.containsKey("maxdls")) {
                    int maxdls = JDUtilities.filterInt(requestParameter.get("maxdls"));
                    if (maxdls < 1) maxdls = 1;
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls);
                }

                if (requestParameter.containsKey("autoreconnect")) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                } else
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, true);

                if (requestParameter.containsKey("package_single_download_counter")) {
                    Integer download_id = 0;
                    Integer package_id = 0;
                    String[] ids;
                    int counter_max = JDUtilities.filterInt(requestParameter.get("package_single_download_counter"));
                    int counter_index = 0;
                    DownloadLink link;
                    Vector<DownloadLink> links = new Vector<DownloadLink>();
                    for (counter_index = 1; counter_index <= counter_max; counter_index++) {
                        if (requestParameter.containsKey("package_single_download_" + counter_index)) {
                            ids = requestParameter.get("package_single_download_" + counter_index).toString().split("[+]", 2);
                            requestParameter.remove("package_single_download_" + counter_index);
                            package_id = JDUtilities.filterInt(ids[0].toString());
                            download_id = JDUtilities.filterInt(ids[1].toString());

                            links.add(JDUtilities.getController().getPackages().get(package_id).getDownloadLinks().get(download_id));
                        }
                    }

                    if (requestParameter.containsKey("selected_dowhat")) {
                        String dowhat = requestParameter.get("selected_dowhat");
                        if (dowhat.compareToIgnoreCase("activate") == 0) {
                            /* aktivieren */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setEnabled(true);
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("deactivate") == 0) {
                            /* deaktivieren */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setEnabled(false);
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("reset") == 0) {
                            /*
                             * reset
                             */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setStatus(DownloadLink.STATUS_TODO);
                                link.setStatusText("");
                                link.reset();
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("remove") == 0) {
                            /*
                             * entfernen
                             */
                            JDUtilities.getController().removeDownloadLinks(links);
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("abort") == 0) {
                            /*
                             * abbrechen
                             */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setAborted(true);
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }

                    }

                }

            } else if (requestParameter.get("do").compareToIgnoreCase("reconnect+now") == 0) {
                logger.info("reconnect now wurde gedrückt");
                JDUtilities.getController().requestReconnect();
            } else if (requestParameter.get("do").compareToIgnoreCase("close+jd") == 0) {
                logger.info("close jd wurde gedrückt");
                class JDClose implements Runnable { /* zeitverzögertes beenden */
                    JDClose() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        JDUtilities.getController().exit();
                    }
                }
                JDClose jds = new JDClose();

            } else if (requestParameter.get("do").compareToIgnoreCase("start+downloads") == 0) {
                logger.info("start wurde gedrückt");
                JDUtilities.getController().startDownloads();
            } else if (requestParameter.get("do").compareToIgnoreCase("stop+downloads") == 0) {
                logger.info("stop wurde gedrückt");
                JDUtilities.getController().stopDownloads();
            } else if (requestParameter.get("do").compareToIgnoreCase("restart+jd") == 0) {
                logger.info("restart wurde gedrückt");
                class JDRestart implements Runnable {
                    /*
                     * zeitverzögertes neustarten
                     */
                    JDRestart() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        JDUtilities.restartJD();
                    }
                }
                JDRestart jdr = new JDRestart();

            } else if (requestParameter.get("do").compareToIgnoreCase("add") == 0) {
                logger.info("add wurde gedrückt");

                if (requestParameter.containsKey("addlinks")) {
                    /*
                     * TODO: mehr add features
                     */
                    String AddLinks = JDUtilities.htmlDecode(requestParameter.get("addlinks"));
                    JDUtilities.getController().addAllLinks(new DistributeData(AddLinks).findLinks());
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
                }
            }
            ;

        }

        String url = path.replaceAll("\\.\\.", "");

        File fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
        if (!fileToRead.isFile()) {
            /*
             * default soll zur index.tmpl gehen, fall keine angabe gemacht
             * wurde
             */
            String tempurl = url + "index.tmpl";
            File fileToRead2 = JDUtilities.getResourceFile("plugins/webinterface/" + tempurl);
            if (fileToRead2.isFile()) {
                url = tempurl;
                fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
            }
            ;
        }

        if (!fileToRead.exists()) {
            response.setNotFound(url);
        } else {
            if (url.endsWith(".tmpl")) {
                JDSimpleWebserverTemplateFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverTemplateFileRequestHandler(this.response);
                filerequest.handleRequest(url);
            } else {
                JDSimpleWebserverStaticFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverStaticFileRequestHandler(this.response);
                filerequest.handleRequest(url);
            }
            ;
        }
        // logger.info("RequestParams: " + requestParameter);

    }
}
