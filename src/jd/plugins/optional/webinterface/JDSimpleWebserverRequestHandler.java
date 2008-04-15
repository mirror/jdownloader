package jd.plugins.optional.webinterface;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Logger;

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

        String[] requ = request.split(" ");

        String method = requ[0];
        String cPath = requ[1];
        String protocol = requ[2];
        String path, querry;
        path=cPath.substring(1);
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
        
        String  url=path.replaceAll("\\.\\.", "");
        
        File fileToRead = JDUtilities.getResourceFile("plugins/webserver/"+url);        
        if(!fileToRead.isFile())fileToRead= new File(fileToRead,"index.html");

        if (!fileToRead.exists()) {            
          response.setNotFound(url);          
        }else
        {
            if ( url.endsWith(".tmpl"))
            {
                JDSimpleWebserverTemplateFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverTemplateFileRequestHandler(this.response);
                filerequest.handleRequest(url);
            }else
            {
                JDSimpleWebserverStaticFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverStaticFileRequestHandler(this.response);
                filerequest.handleRequest(url);
            };
            }
            logger.info("RequestParams: " + requestParameter);
             
    }
}
