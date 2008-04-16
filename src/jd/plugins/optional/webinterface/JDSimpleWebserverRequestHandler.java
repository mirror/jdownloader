package jd.plugins.optional.webinterface;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.config.Configuration;
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
        logger.info(request);
        String[] requ = request.split(" ");

        String method = requ[0];
        String cPath = requ[1];
        String protocol = requ[2];
        String path, querry;
        path = cPath.substring(1);
        String[] params;
        HashMap<String, String> requestParameter = new HashMap<String, String>();
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

                requestParameter.put(key, value);

            }
        }
        if (requestParameter.containsKey("do")) {
            if (requestParameter.get("do").compareToIgnoreCase("submit")==0) {
                logger.info("submit wurde gedrückt");
                if (requestParameter.containsKey("speed")) {
                    int setspeed = JDUtilities.filterInt(requestParameter.get("speed"));
                    if (setspeed < 0) setspeed=0;
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, setspeed);
                };
                
                if (requestParameter.containsKey("maxdls")) {
                    int maxdls = JDUtilities.filterInt(requestParameter.get("maxdls"));
                    if (maxdls < 1) maxdls=1;
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls);
                }
                
                if (requestParameter.containsKey("autoreconnect")) {                    
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                }else JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
                
            }else
                if (requestParameter.get("do").compareToIgnoreCase("reconnect+now")==0) {
                    logger.info("reconnect now wurde gedrückt");
                    JDUtilities.getController().requestReconnect();
                }else
                    if (requestParameter.get("do").compareToIgnoreCase("close+jd")==0) {
                        logger.info("close jd now wurde gedrückt");
                        JDUtilities.getController().exit();
                    }else
                        if (requestParameter.get("do").compareToIgnoreCase("start+downloads")==0) {
                            logger.info("start now wurde gedrückt");
                            JDUtilities.getController().startDownloads();
                        }else
                            if (requestParameter.get("do").compareToIgnoreCase("stop+downloads")==0) {
                                logger.info("stop now wurde gedrückt");
                                JDUtilities.getController().stopDownloads();
                            };
                            
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
        logger.info("RequestParams: " + requestParameter);

    }
}
