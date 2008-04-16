package jd.plugins.optional.webinterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.optional.webinterface.template.Template;
import jd.utils.JDUtilities;

public class JDSimpleWebserverTemplateFileRequestHandler {

    private Logger logger = JDUtilities.getLogger();
    private JDSimpleWebserverResponseCreator response;
    private JDController controller= JDUtilities.getController();
    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverTemplateFileRequestHandler(JDSimpleWebserverResponseCreator response) {
      
      this.response=response;
    }
    
    public void handleRequest(String url)
    {
        try {
            
            Template t = new Template(JDUtilities.getResourceFile("plugins/webserver/"+url).getAbsolutePath());
            Vector v = new Vector();
            
            
            FilePackage filePackage;
            DownloadLink dLink;
            for(Iterator<FilePackage> packageIterator = JDUtilities.getController().getPackages().iterator();packageIterator.hasNext();){
            filePackage = packageIterator.next();
            for(Iterator<DownloadLink> linkIterator = filePackage.getDownloadLinks().iterator();linkIterator.hasNext();){
            dLink = linkIterator.next();
            Hashtable h = new Hashtable();
            h.put("download_name", dLink.getName());
            h.put("download_hoster", dLink.getHost());
            
            if (dLink.isEnabled()){
                 
            switch (dLink.getStatus()) {            
                    case DownloadLink.STATUS_DONE:
                        h.put("download_status_color", "#8bffa1");
                        break;
                        
                    case DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS:
                        h.put("download_status_color", "#1189ee");
                        break;
                        
                    default: h.put("download_status_color", "#92afc6");
            };                
                }
            else
            {
                h.put("download_status_color", "#c70000");
            }
            
            h.put("download_status", dLink.getStatusText());
            v.addElement(h);
            
            }            
            }
             
            
            t.setParam("config_max_downloads", JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 0));
            t.setParam("config_max_speed", JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)==true){
                t.setParam("config_autoreconnect","" );                
            }else
            {
            t.setParam("config_autoreconnect","checked" );
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
