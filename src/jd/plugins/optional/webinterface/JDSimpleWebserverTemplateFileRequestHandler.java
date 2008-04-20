package jd.plugins.optional.webinterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.optional.webinterface.template.Template;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDSimpleWebserverTemplateFileRequestHandler {

    private Logger logger = JDUtilities.getLogger();
    private JDSimpleWebserverResponseCreator response;
    private JDController controller = JDUtilities.getController();
    private DecimalFormat f = new DecimalFormat("#0.00"); 

    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverTemplateFileRequestHandler(JDSimpleWebserverResponseCreator response) {

        this.response = response;
    }

    @SuppressWarnings("unchecked")
    public void handleRequest(String url) {
        try {

            Template t = new Template(JDUtilities.getResourceFile("plugins/webinterface/" + url).getAbsolutePath());
            Vector v,v2 = new Vector();
            Hashtable h, h2 = new Hashtable();
           v=new Vector();
           String value;
           

            FilePackage filePackage;
            DownloadLink dLink;
            Integer Package_ID=-1;
            Integer Download_ID=-1;
            Double percent=0.0;
            
            for (Iterator<FilePackage> packageIterator = JDUtilities.getController().getPackages().iterator(); packageIterator.hasNext();) {
                filePackage = packageIterator.next();
                Package_ID++;
                h=new Hashtable();
                /* Paket Infos */
                h.put("download_name", filePackage.getName());
                
                
                value = "";
                percent=filePackage.getPercent();
                h.put("download_status_percent",f.format(percent));
                
                if (filePackage.getLinksInProgress() > 0) {
                    value = filePackage.getLinksInProgress() + "/" + filePackage.size() + " " + JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
                }
                if (filePackage.getTotalDownloadSpeed() > 0) value = "[" + filePackage.getLinksInProgress() + "/" + filePackage.size() + "] " + "ETA " + JDUtilities.formatSeconds(filePackage.getETA()) + " @ " + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s";
                
                h.put("package_id", Package_ID.toString());
                h.put("download_hoster", value);
                h.put("download_status_color", "#8bffa1");
                h.put("download_status", f.format(percent)+" % ("+JDUtilities.formatKbReadable(filePackage.getTotalKBLoaded())+" / "+JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize())+")");
                
                v2 = new Vector();
                Download_ID=-1;
                for (Iterator<DownloadLink> linkIterator = filePackage.getDownloadLinks().iterator(); linkIterator.hasNext();) {
                    dLink = linkIterator.next();
                    Download_ID++;
                    /* Download Infos */
                    
                   
                   percent=(double)(dLink.getDownloadCurrent() * 100.0 / Math.max(1,dLink.getDownloadMax()));
                   
                    h2 = new Hashtable();
                    h2.put("download_status_percent",f.format(percent));
                    h2.put("package_id", Package_ID.toString());
                    h2.put("download_id", Download_ID.toString());
                    h2.put("download_name", dLink.getName());
                             
                    h2.put("download_hoster", dLink.getHost());                   
                    
                    if (dLink.isEnabled()) {

                        switch (dLink.getStatus()) {
                        case DownloadLink.STATUS_DONE:
                            h2.put("download_status_color", "#8bffa1");
                            break;

                        case DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS:
                            h2.put("download_status_color", "#1189ee");
                            break;

                        default:
                            h2.put("download_status_color", "#92afc6");
                        }
                        ;
                    } else {
                        h2.put("download_status_color", "#c70000");
                    }
                    
                    
                    h2.put("download_status", f.format(percent)+"% "+dLink.getStatusText());
                    v2.addElement(h2);
                    
                    
                }
                h.put("downloads", v2);
                v.addElement(h);
            }

            t.setParam("config_max_downloads", JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 0));
            t.setParam("config_max_speed", JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));

            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false) == true) {
                t.setParam("config_autoreconnect", "");
            } else {
                t.setParam("config_autoreconnect", "checked");
            }
            ;

            if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
                t.setParam("config_startstopbutton", "Stop Downloads");
            } else {
                t.setParam("config_startstopbutton", "Start Downloads");
            }
            ;

            t.setParam("pakete", v);
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
