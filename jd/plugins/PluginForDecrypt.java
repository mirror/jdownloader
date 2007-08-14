package jd.plugins;

import java.util.Iterator;
import java.util.Vector;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin{
    /**
     * Diese Methode entschlüsselt Links.
     * 
     * @param cryptedLinks Ein Vector, mit jeweils einem verschlüsseltem Link. 
     *                     Die einzelnen verschlüsselten Links werden aufgrund des Patterns  
     *                     {@link jd.plugins.Plugin#getSupportedLinks() getSupportedLinks()} herausgefiltert
     * @return Ein Vector mit Klartext-links
     */
    public Vector<String> decryptLinks(Vector<String> cryptedLinks){
        Vector<String> decryptedLinks = new Vector<String>();
        Iterator<String> iterator = cryptedLinks.iterator();
        while(iterator.hasNext()){
            decryptedLinks.addAll(decryptLink(iterator.next()));
        }
        return decryptedLinks;
    }
    /**
     * Die Methode entschlüsselt einen einzelnen Link
     * 
     * @param cryptedLink Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public abstract Vector<String> decryptLink(String cryptedLink);
}
