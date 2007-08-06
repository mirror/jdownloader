package jd.plugins;

import java.net.URLConnection;
import java.util.Vector;


public abstract class PluginForHost extends Plugin{
    public abstract URLConnection        getURLConnection();
    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text gesucht.
     * Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data Ein Text mit beliebig vielen Downloadlinks dieses Anbieters 
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data){
        Vector<DownloadLink> links=null;
        Vector<String> hits = getMatches(data);
        if(hits != null && hits.size()>0){
            links = new Vector<DownloadLink>();
            for(int i=0;i<hits.size();i++){
                links.add(new DownloadLink(this,hits.elementAt(i),getHost(), true));
            }
        }
        return links;
    }
}
