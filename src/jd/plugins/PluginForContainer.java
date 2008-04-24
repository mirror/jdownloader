//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 * 
 * @author astaldo/JD-Team
 */

public abstract class PluginForContainer extends PluginForDecrypt {
    private static final int STATUS_NOTEXTRACTED = 0;

    private static final int STATUS_ERROR_EXTRACTING = 1;

    // private static final int STATUS_SUCCESS = 2;

    protected String md5;

    private int status = STATUS_NOTEXTRACTED;

    protected Vector<String> downloadLinksURL;

    private static HashMap<String, Vector<DownloadLink>> CONTAINER = new HashMap<String, Vector<DownloadLink>>();

    private static HashMap<String, Vector<String>> CONTAINERLINKS = new HashMap<String, Vector<String>>();

    private static HashMap<String, PluginForContainer> PLUGINS = new HashMap<String, PluginForContainer>();

    protected Vector<DownloadLink> containedLinks = new Vector<DownloadLink>();

    protected Vector<String> EXTENSIONS = new Vector<String>();

    /**
     * Diese Methode liefert eine URL zurück, von der aus der Download gestartet
     * werden kann
     * 
     * @param downloadLink
     *            Der DownloadLink, dessen URL zurückgegeben werden soll
     * @return Die URL als String
     */

    public String extractDownloadURL(DownloadLink downloadLink) {
        logger.info("EXTRACT " + downloadLink);
        if (downloadLinksURL == null) initContainer(downloadLink.getContainerFile());
        if (downloadLinksURL == null || downloadLinksURL.size() <= downloadLink.getContainerIndex()) return null;
        return downloadLinksURL.get(downloadLink.getContainerIndex());
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        // TODO Auto-generated method stub
        return null;
    }

    public ArrayList<String> createMenuitems() {
        return null;
    }

    /**
     * Wird von der parentklasse für jeden step aufgerufen. Diese Methode muss
     * alle steps abarbeiten und abgecshlossene schritte zurückgeben
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, File container);

    public abstract String[] encrypt(String plain);

    public String createContainerString(Vector<DownloadLink> downloadLinks) {
        return null;
    }

    /**
     * Erstellt eine Kopie des Containers im Homedir.
     */
    public PluginStep doDecryptStep(PluginStep step, String parameter) {
        logger.info("DO STEP");
        String file = (String) parameter;
        if (status == STATUS_ERROR_EXTRACTING) {
            logger.severe("Expired JD Version. Could not extract links");
            return step;
        }
        if (file == null) {
            logger.severe("Containerfile == null");
            return step;
        }
        File f = new File(file);
        if (md5 == null) md5 = JDUtilities.getLocalHash(f);

        String extension = JDUtilities.getFileExtension(f);
        if (f.exists()) {

            File res = JDUtilities.getResourceFile("container/" + md5 + "." + extension);
            if (!res.exists()) {
                JDUtilities.copyFile(f, res);

            }
            if (!res.exists()) {
                logger.severe("Could not copy file to homedir");

            }

            return doStep(step, res);

        }
        return null;
    }

    public Vector<String> getExtensions() {
        return this.EXTENSIONS;
    }

    public synchronized boolean canHandle(String data) {

        if (data == null) return false;
        if (this.getExtensions().contains(JDUtilities.getFileExtension(new File(data.toLowerCase())))) return true;

        return false;
    }

    public void initContainer(String filename) {
        if (filename == null) return;
        if (CONTAINER.containsKey(filename) &&CONTAINER.get(filename)!=null&& CONTAINER.get(filename).size() > 0) {
            logger.info("Cached " + filename);
            containedLinks = CONTAINER.get(filename);
            if (containedLinks != null) {
                Iterator<DownloadLink> it = containedLinks.iterator();
                while (it.hasNext()) {
                    it.next().setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                }
            }

            downloadLinksURL = CONTAINERLINKS.get(filename);

            return;

        }

        if (containedLinks == null || containedLinks.size() == 0) {
            logger.info("Init Container");
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, this);
            if(progress!=null)progress.finalize();
            progress = new ProgressController(JDLocale.L("plugins.container.open","Open Container"), 10);
            progress.increase(1);
          
            doDecryptStep(new PluginStep(PluginStep.STEP_OPEN_CONTAINER, null), filename);
            progress.increase(1);
            progress.setStatusText(String.format(JDLocale.L("plugins.container.found","Prozess %s links"),""+containedLinks.size()));
            logger.info(filename + " Parse");
            if (containedLinks != null&&downloadLinksURL!=null) {
                decryptLinkProtectorLinks();
                progress.setStatusText(String.format(JDLocale.L("plugins.container.exit","Finished. Found %s links"),""+containedLinks.size()));
                Iterator<DownloadLink> it = containedLinks.iterator();
                while (it.hasNext()) {
                    it.next().setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                }
                progress.increase(1);
            }
            if (containedLinks == null || containedLinks.size() == 0) {
                CONTAINER.put(filename, null);
                CONTAINERLINKS.put(filename, null);

            } else {

                CONTAINER.put(filename, containedLinks);
                CONTAINERLINKS.put(filename, downloadLinksURL);
            }
            progress.finalize();
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, this);

        }
    }
/**
 * geht die containedLinks liste durch und decrypted alle links die darin sind.
 */
    private void decryptLinkProtectorLinks() {
        Vector<DownloadLink> tmpDlink = new Vector<DownloadLink>();
        ;
        Vector<String> tmpURL = new Vector<String>();

        int i = 0;
        int c = 0;
        progress.addToMax(downloadLinksURL.size());
       // logger.info("PRE: "+downloadLinksURL);
        for (Iterator<String> it1 = this.downloadLinksURL.iterator(); it1.hasNext();) {
            progress.increase(1);
            progress.setStatusText(String.format(JDLocale.L("plugins.container.decrypt","Decrypt link %s"),""+i));
            
            DistributeData distributeData = new DistributeData(it1.next());
            Vector<DownloadLink> links = distributeData.findLinks();
            DownloadLink srcLink = containedLinks.get(i);
            Iterator<DownloadLink> it = links.iterator();
            progress.addToMax(links.size());
            // while(it.hasNext())it.next().setDownloadPath(JDUtilities.getResourceFile("packages").getAbsolutePath());
            while (it.hasNext()) {
                progress.increase(1);
                DownloadLink next = it.next();
                tmpDlink.add(next);
                tmpURL.add(next.getDownloadURL());
             
                next.setContainerFile(srcLink.getContainerFile());
                next.setContainerIndex(c++);
           
                    next.setName(srcLink.getName());
              
                if(next.getDownloadMax()<10){
                    next.setDownloadMax((int)srcLink.getDownloadMax());
                }
                
                next.getSourcePluginPasswords().addAll(srcLink.getSourcePluginPasswords());
             
                next.setSourcePluginComment(srcLink.getSourcePluginComment()+"->"+next.getSourcePluginComment());
                
                next.setLoadedPluginForContainer(this);
                next.setFilePackage(srcLink.getFilePackage());
                next.setUrlDownload(null);
                next.setLinkType(DownloadLink.LINKTYPE_CONTAINER);
                

            }
            i++;
        }
        containedLinks=tmpDlink;
        downloadLinksURL=tmpURL;
       // logger.info("downloadLinksURL: "+downloadLinksURL);
    }

    /**
     * Liefert alle in der Containerdatei enthaltenen Dateien als DownloadLinks
     * zurück.
     * 
     * @param filename
     *            Die Containerdatei
     * @return Ein Vector mit DownloadLinks
     */
    public Vector<DownloadLink> getContainedDownloadlinks() {

        return containedLinks == null ? new Vector<DownloadLink>() : containedLinks;
    }

    /**
     * Findet anhand des Hostnamens ein passendes Plugiln
     * 
     * @param data
     *            Hostname
     * @return Das gefundene Plugin oder null
     */
    protected PluginForHost findHostPlugin(String data) {
        Vector<PluginForHost> pluginsForHost = JDUtilities.getPluginsForHost();
        PluginForHost pHost;
        for (int i = 0; i < pluginsForHost.size(); i++) {
            pHost = pluginsForHost.get(i);
            if (pHost.canHandle(data)) { return pHost; }
        }
        return null;
    }

    /**
     * Gibt das passende plugin für diesen container zurück. falls schon eins
     * exestiert wird dieses zurückgegeben.
     * 
     * @param containerFile
     * @return
     */
    public PluginForContainer getPlugin(String containerFile) {
        if (PLUGINS.containsKey(containerFile)) return PLUGINS.get(containerFile);
        try {
            PluginForContainer newPlugin = this.getClass().newInstance();
            PLUGINS.put(containerFile, newPlugin);
            return newPlugin;
        } catch (InstantiationException e) {

            e.printStackTrace();
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            e.printStackTrace();
        }
        return null;
    }
}
