package jd.plugins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {
    /**
     * Diese Methode entschlüsselt Links.
     * 
     * @param cryptedLinks Ein Vector, mit jeweils einem verschlüsseltem Link.
     *            Die einzelnen verschlüsselten Links werden aufgrund des
     *            Patterns
     *            {@link jd.plugins.Plugin#getSupportedLinks() getSupportedLinks()}
     *            herausgefiltert
     * @return Ein Vector mit Klartext-links
     */
    public Vector<String> decryptLinks(Vector<String> cryptedLinks) {
        Vector<String> decryptedLinks = new Vector<String>();
        Iterator<String> iterator = cryptedLinks.iterator();
        while (iterator.hasNext()) {
            decryptedLinks.addAll(decryptLink(iterator.next()));
        }
        return decryptedLinks;
    }

    private String cryptedLink = null;

    private String statusText  = "";

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden
     * durchlaufen. Der letzte step muss als parameter einen Vector<String> mit
     * den decoded Links setzen
     * 
     * @param cryptedLink Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public Vector<String> decryptLink(String cryptedLink) {
        this.cryptedLink = cryptedLink;

        PluginStep step = null;
        while ((step = nextStep(step)) != null) {
            doStep(step, cryptedLink);
            if (nextStep(step) == null) {
                try {
                    if (step.getParameter() == null) {
                        logger.severe("ACHTUNG Decrypt PLugins müssen im letzten schritt einen  Vector<String> parameter  übergeben!");
                        return new Vector<String>();
                    }
                    Vector<String>decryptedLinks= (Vector<String>) step.getParameter();
                    logger.info("Got "+decryptedLinks.size()+" links");
                    return decryptedLinks;
                }
                catch (Exception e) {
                    logger.severe("DecryptFehler! " + e.getMessage());
                }
            }
        }
        return new Vector<String>();

    }
    /**
     * Sucht in data nach allen passenden links und gibt diese als vektor zurück
     * @param data
     * @return
     */
    public Vector<String> getDycryptableLinks(String data){
       
      
        Vector<String> hits = getMatches(data, getSupportedLinks());
        if(hits != null && hits.size()>0){
           
            for(int i=0;i<hits.size();i++){
                String file = hits.get(i);
             while(file.charAt(0)=='"')file=file.substring(1);
             while(file.charAt(file.length()-1)=='"')file=file.substring(0,file.length()-1);
               hits.setElementAt(file, i);
            }
        }
        return hits;
    }
    public abstract PluginStep doStep(PluginStep step, String parameter);

    public PluginStep doStep(PluginStep step, Object parameter) {
        return doStep(step, (String) parameter);
    }

    public String getLinkName() {
        if (cryptedLink == null) return "";
        try {
            return new URL(cryptedLink).getFile();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }



}
