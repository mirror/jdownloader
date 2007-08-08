package jd.plugins;

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
    public abstract Vector<String> decryptLinks(Vector<String> cryptedLinks);
}
