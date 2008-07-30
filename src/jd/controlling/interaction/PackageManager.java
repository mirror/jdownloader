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

package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class PackageManager extends Interaction implements Serializable {

    private static final String NAME = JDLocale.L("interaction.packagemanager.name", "Pakete aktualisieren");

    private static Vector<HashMap<String, String>> PACKAGE_DATA = null;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private SubConfiguration managerConfig;

    public PackageManager() {
        managerConfig = JDUtilities.getSubConfig("PACKAGEMANAGER");

    }

    @SuppressWarnings("unchecked")
    private void checkNewInstalled() {
        ArrayList<File> slist = (ArrayList<File>) managerConfig.getProperty("NEW_INSTALLED_SUCCESSFULL");
        if (slist != null) for (File del : slist){
            del.delete();
            
           String  html = JDLocale.L("modules.packagemanager.installednewpackage.title", "Package Installed") + "<hr><b>" + del.getName() + "</b><hr>";
            JDUtilities.getGUI().showCountdownConfirmDialog(html, 15);
        }
           

        ArrayList<File> flist = (ArrayList<File>) managerConfig.getProperty("NEW_INSTALLED_FAILED");
        if (flist != null) for (File del : flist) {
            del.delete();
            JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.L("modules.packagemanager.loadednewpackage.failed", "Installation failed. try again: ") + "<hr><b>" + del.getName() + "</b>", 15);

        }

        Object list = managerConfig.getProperty("NEW_INSTALLED");
        managerConfig.setProperty("NEW_INSTALLED_SUCCESSFULL", null);
        managerConfig.setProperty("NEW_INSTALLED", null);
        managerConfig.setProperty("NEW_INSTALLED_FAILED", null);
        managerConfig.save();
        if (list == null) return;

        ArrayList<File> readmes = (ArrayList<File>) list;
        for (File readme : readmes) {
            String html;
            if (readme == null) {
               
                continue;
            }
            html = JDUtilities.getLocalFile(readme);
            if (Regex.matches(html, "src\\=\"(.*?)\"")) {
                html = new Regex(html, "src\\=\"(.*?)\"").getFirstMatch();
                html = JDLocale.L("modules.packagemanager.infonewpackage.title", "Package information") + "<hr><b>" + readme.getAbsolutePath() + "</b><hr><a href='" + html + "'>" + JDLocale.L("modules.packagemanager.loadednewpackage.more", "More Information & Installnotes") + "</a>";
                JDUtilities.getGUI().showCountdownConfirmDialog(html, 15);
            } else {
                JDUtilities.getGUI().showCountdownConfirmDialog(html, 15);
            }

        }

    }

    @Override
    @SuppressWarnings("unchecked")
    
    public boolean doInteraction(Object arg) {

        checkNewInstalled();

        Vector<HashMap<String, String>> data = getPackageData();
        FilePackage fp = new FilePackage();
        fp.setName(JDLocale.L("modules.packagemanager.packagename", "JD-Update"));
        fp.setDownloadDirectory(JDUtilities.getResourceFile("packages").getAbsolutePath());
        HashMap<String, String> dat;
        int installed;
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        for (int i = 0; i < data.size(); i++) {
            dat = data.get(i);
            installed = managerConfig.getIntegerProperty("PACKAGE_INSTALLED_VERSION_" + dat.get("id"), 0);
            ArrayList<String> list=(ArrayList<String>)managerConfig.getProperty("CURRENT_JDU_LIST", new ArrayList<String>());
            
            
            if (!list.contains(dat.get("url"))&&dat.get("selected") != null && installed != Integer.parseInt(dat.get("version")) && !JDUtilities.getController().hasDownloadLinkURL(dat.get("url").trim())) {

                DistributeData distributeData = new DistributeData(dat.get("url"));
                Vector<DownloadLink> links = distributeData.findLinks();

                Iterator<DownloadLink> it = links.iterator();
                // while(it.hasNext())it.next().setDownloadPath(JDUtilities.getResourceFile("packages").getAbsolutePath());
                while (it.hasNext()) {
                    DownloadLink next = it.next();
                    next.setFilePackage(fp);
                    next.setLinkType(DownloadLink.LINKTYPE_JDU);
                    next.setSourcePluginComment(dat.get("id") + "_" + dat.get("version"));
                }

                // Decryptersystem wird verwendet, allerdings wird der weg über
                // den linkgrabber vermieden
                ret.addAll(links);

            }
        }

        if (fp.size() > 0) {
            JDUtilities.getController().addPackageAt(fp, 0);
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
        }
        return true;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    public Vector<HashMap<String, String>> getPackageData() {
        if (PACKAGE_DATA != null) return PACKAGE_DATA;
        RequestInfo ri = null;

        ProgressController progress = new ProgressController(JDLocale.L("interaction.packagemanager.progress.loadpackages", "Load packagedata"), 10);
        progress.setRange(3);
        try {
            //
            // ri = Plugin.getRequest(new URL("http://jdpackagelist.ath.cx"),
            // null, null, true);
            if (JDUtilities.getSubConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false)) {
                ri = HTTP.getRequest(new URL("http://jdservice.ath.cx/update/packages/betalist.php"), null, null, true);
            } else {
                ri = HTTP.getRequest(new URL("http://jdservice.ath.cx/update/packages/list.php"), null, null, true);
            }
            progress.increase(1);
            String xml = "<packages>" + ri.getFirstMatch("<packages>(.*?)</packages>") + "</packages>";
            DocumentBuilderFactory factory;
            InputSource inSource;
            Document doc;

            factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            inSource = new InputSource(new StringReader(xml));
            doc = factory.newDocumentBuilder().parse(inSource);
            NodeList packages = doc.getFirstChild().getChildNodes();
            HashMap<String, String> tmp;

            Vector<HashMap<String, String>> data = new Vector<HashMap<String, String>>();
            for (int i = 0; i < packages.getLength(); i++) {
                Node entry = packages.item(i);

                tmp = new HashMap<String, String>();
                NodeList values = entry.getChildNodes();
                for (int t = 0; t < values.getLength(); t++) {
                    tmp.put(values.item(t).getNodeName(), values.item(t).getTextContent());
                }
                if (tmp.containsKey("preselected")) {
                    tmp.put("selected", managerConfig.getBooleanProperty("PACKAGE_SELECTED_" + tmp.get("id"), tmp.get("preselected").equals("true")) ? "true" : null);
                    if (values.getLength() > 2) data.add(tmp);
                }
            }
            PACKAGE_DATA = data;
            progress.increase(1);
            progress.finalize();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            progress.finalize();
            return new Vector<HashMap<String, String>>();
        }

    }

    @Override
    public void initConfig() {

    }

    
    @SuppressWarnings("unchecked")
    public void onDownloadedPackage(final DownloadLink downloadLink) {
        // File dir = JDUtilities.getResourceFile("packages");
        // File[] list = dir.listFiles(new JDFileFilter(null, ".jdu", false));
        // for( int i=0;i<list.length;i++){
        String[] dat = downloadLink.getSourcePluginComment().split("_");
        managerConfig.setProperty("COMMENT_" + new File(downloadLink.getFileOutput()), downloadLink.getSourcePluginComment());
        managerConfig.save();
        ArrayList<String> list=(ArrayList<String>)managerConfig.getProperty("CURRENT_JDU_LIST", new ArrayList<String>());
        list.add(downloadLink.getDownloadURL());
        
        managerConfig.setProperty("CURRENT_JDU_LIST",list);
        managerConfig.save();
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    
                    e.printStackTrace();
                }
                JDUtilities.getController().removeDownloadLink(downloadLink);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
                
            }
        }.start();
        JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.L("modules.packagemanager.downloadednewpackage.title", "Package downloaded. Don't forget do restart JD to complete installation") + "<hr><b>" + downloadLink.getName() + " v" + dat[1] + "</b>", 15);

        // String[] dat = downloadLink.getSourcePluginComment().split("_");

        // }

    }

    
    @Override
    public void resetInteraction() {
    }

    
    @Override
    public void run() {
    }

    @Override
    public String toString() {
        return NAME;
    }
}
