package jd.plugins.optional.webinterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.optional.webinterface.template.Template;
import jd.utils.JDUtilities;

public class JDSimpleWebserverTemplateFileRequestHandler {

    private Logger logger = JDUtilities.getLogger();
    private JDSimpleWebserverResponseCreator response;
    private JDController controller= JDUtilities.getController();
    /**
     * Create a new handler that serves files from a base directory
     *
     * @param base directory
     */
    public JDSimpleWebserverTemplateFileRequestHandler(JDSimpleWebserverResponseCreator response) {
      
      this.response=response;
    }
    
    public void handleRequest(String url)
    {
        try {
            
            Template t = new Template(JDUtilities.getResourceFile("plugins/webserver/"+url).getAbsolutePath());
            Vector v = new Vector();
            Vector<DownloadLink> links=controller.getDownloadLinks();
            for(int i=0; i<links.size();i++){ 
                DownloadLink link= links.get(i);
            Hashtable h = new Hashtable();
            h.put("download", link.getFileOutput());
            h.put("hoster", link.getHost());
            h.put("status", link.getStatusText());
            v.addElement(h);
            };
            t.setParam("downloads", v);
            response.addContent(t.output());
            response.setOk();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
   
}
