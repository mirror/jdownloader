package jd.plugins;

import java.net.URLConnection;
import java.util.Vector;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter
 * Dateien herunterladen können
 *
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin{
    public abstract URLConnection        getURLConnection();
    
    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren etc)
     */
    public abstract void reset();
    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen Neustart vor
     */
    public void resetPlugin(){
        this.resetSteps();
        this.reset();
        this.aborted=false;
    }
    
    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text gesucht.
     * Gefundene Links werden dann in einem Vector zurückgeliefert
     *
     * @param data Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data){
        Vector<DownloadLink> links=null;
        Vector<String> hits = getMatches(data, getSupportedLinks());
        if(hits != null && hits.size()>0){
            links = new Vector<DownloadLink>();
            for(int i=0;i<hits.size();i++){
                String file = hits.get(i);
                links.add(new DownloadLink(
                        this,
                        file.substring(file.lastIndexOf("/")+1,file.length()),
                        getHost(),
                        file,
                        true));
            }
        }
        return links;
    }
   
}
