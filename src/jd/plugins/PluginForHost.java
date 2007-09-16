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
//    public abstract URLConnection        getURLConnection();
    
    private String data;

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
     * Diese methode führt den Nächsten schritt aus. Der gerade ausgeführte Schritt wir zurückgegeben
     * 
     * @param parameter Ein Übergabeparameter
     * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
     */
    public PluginStep doNextStep(Object parameter){
       currentStep=nextStep(currentStep); 
       if(currentStep==null){
           logger.info(this+" PLuginende erreicht!");
           return null;
       }
       return doStep(currentStep,parameter);
    }
    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text gesucht.
     * Gefundene Links werden dann in einem Vector zurückgeliefert
     *
     * @param data Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data){
        this.data=data;
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
    public abstract boolean checkAvailability (DownloadLink parameter);  
    
   public abstract PluginStep doStep(PluginStep step,DownloadLink parameter);
    
    public PluginStep doStep(PluginStep step,Object parameter){
        return doStep(step, (DownloadLink) parameter);
    }
    
    @Override
    public String getLinkName() {
        
        return data;
    }
}
