package jd.plugins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import jd.controlling.ProgressController;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {
    protected ProgressController progress;

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
    
    public PluginForDecrypt(){
      
      
    }
    public Vector<String[]> decryptLinks(Vector<String> cryptedLinks) {
        Vector<String[]> decryptedLinks = new Vector<String[]>();
        Iterator<String> iterator = cryptedLinks.iterator();
        while (iterator.hasNext()) {
            decryptedLinks.addAll(decryptLink(iterator.next()));
        }
        return decryptedLinks;
    }

    private String cryptedLink              = null;

    private String decrypterDefaultPassword = null;

    private String decrypterDefaultComment  = null;

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden
     * durchlaufen. Der letzte step muss als parameter einen Vector<String> mit
     * den decoded Links setzen
     * 
     * @param cryptedLink Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public Vector<String[]> decryptLink(String cryptedLink) {
        this.cryptedLink = cryptedLink;
        progress=new ProgressController();
        progress.setStatusText("decrypt-"+getPluginName()+": "+cryptedLink);
        PluginStep step = null;
      
        while ((step = nextStep(step)) != null) {
            doStep(step, cryptedLink);

            if (nextStep(step) == null) {
                Vector links = (Vector) step.getParameter();
                if (links == null ) {
                    logger.severe("ACHTUNG Decrypt Plugins müssen im letzten schritt einen  Vector<String[]> oder Vector<String> parameter  übergeben!");
                    progress.finalize();
                    return new Vector<String[]>();
                }
                if(links.size()==0){
                    progress.finalize();
                    return new Vector<String[]>();
                }
                Vector<String[]> decryptedLinks = new Vector<String[]>();
                try {
                    if (links.get(0) instanceof String) {

                        for (int i = 0; i < links.size(); i++) {

                            decryptedLinks.add(new String[] { (String) links.get(i), null, null });
                        }
                        logger.info("Got " + decryptedLinks.size() + " links1 "+links);
                        progress.finalize();
                        return decryptedLinks;
                    }
                    else if (links.get(0) instanceof String[]) {
                        logger.info("Got " + links.size() + " links2 "+links);
                        progress.finalize();
                        return (Vector<String[]>) links;
                    }
                    else {
                        logger.severe("ACHTUNG Decrypt Plugins müssen im letzten schritt einen  Vector<String[]> oder Vector<String> parameter  übergeben!");
                        progress.finalize();
                        return new Vector<String[]>();
                    }

                }
                catch (Exception e) {

                     e.printStackTrace();
                    logger.severe("Decrypterror: " + e.getMessage());
                }

            }
        }
        progress.finalize();
        return new Vector<String[]>();

    }

    /**
     * Sucht in data nach allen passenden links und gibt diese als vektor zurück
     * 
     * @param data
     * @return
     */
    public Vector<String> getDecryptableLinks(String data) {
        Vector<String> hits = getMatches(data, getSupportedLinks());
        if (hits != null && hits.size() > 0) {

            for (int i = 0; i < hits.size(); i++) {
                String file = hits.get(i);
                while (file.charAt(0) == '"')
                    file = file.substring(1);
                while (file.charAt(file.length() - 1) == '"')
                    file = file.substring(0, file.length() - 1);
                hits.setElementAt(file, i);
            }
        }
        return hits;
    }

    /**
     * Diese Methode arbeitet die unterschiedlichen schritte ab. und gibt den
     * gerade abgearbeiteten Schritt jeweisl zurück.
     * 
     * @param step
     * @param parameter
     * @return gerade abgeschlossener Schritt
     */
    public abstract PluginStep doStep(PluginStep step, String parameter);

    /**
     * Deligiert den doStep Call weiter und ändert dabei nur den parametertyp.
     */
    public PluginStep doStep(PluginStep step, Object parameter) {
        return doStep(step, (String) parameter);
    }

    /**
     * Gibt den namen des internen CryptedLinks zurück
     * 
     * @return encryptedLink
     */

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
