//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.unrar.UnZip;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class PackageManager extends Interaction implements Serializable {

    private static final String NAME = JDLocale.L("interaction.packagemanager.name", "Pakete aktualisieren");

    private SubConfiguration    managerConfig;

    private static Vector<HashMap<String, String>> PACKAGE_DATA=null;

    public PackageManager() {
        managerConfig = JDUtilities.getSubConfig("PACKAGEMANAGER");

    }

    @Override
    public boolean doInteraction(Object arg) {
        Vector<HashMap<String, String>> data = getPackageData();
FilePackage fp= new FilePackage();
fp.setName(JDLocale.L("modules.packagemanager.packagename","JD-Update"));
fp.setDownloadDirectory(JDUtilities.getResourceFile("packages").getAbsolutePath());
        HashMap<String, String> dat;
        int installed;
  Vector<DownloadLink> ret= new Vector<DownloadLink>();
        for (int i = 0; i < data.size(); i++) {
            dat = data.get(i);
            installed = managerConfig.getIntegerProperty("PACKAGE_INSTALLED_VERSION_" + dat.get("id"), 0);
            if (dat.get("selected") != null && installed != Integer.parseInt(dat.get("version")) && !JDUtilities.getController().hasDownloadLinkURL(dat.get("url").trim())) {
                logger.info(dat+"");
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
        if(fp.size()>0){
        JDUtilities.getController().addPackage(fp);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
        }
        return true;
    }

    public Vector<HashMap<String, String>> getPackageData() {
if(PACKAGE_DATA!=null)return PACKAGE_DATA;
        RequestInfo ri = null;
        try {
            //
            // ri = Plugin.getRequest(new URL("http://jdpackagelist.ath.cx"),
            // null, null, true);
            ri = Plugin.getRequest(new URL("http://jdservice.ath.cx/update/packages/list.php"), null, null, true);
          
            String xml = "<packages>" + Plugin.getSimpleMatch(ri.getHtmlCode(), "<packages>°</packages>", 0) + "</packages>";
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
           PACKAGE_DATA=data;
            return data;
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Vector<HashMap<String, String>>();
        }
    }

    public void run() {}

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {}

    public void onDownloadedPackage(DownloadLink downloadLink) {
        File dir = JDUtilities.getResourceFile("packages");
        // File[] list = dir.listFiles(new JDFileFilter(null, ".jdu", false));
        // for( int i=0;i<list.length;i++){
        String[] dat = downloadLink.getSourcePluginComment().split("_");
        logger.info(dat[0] + " - " + dat[1]);
        UnZip u = new UnZip(new File(downloadLink.getFileOutput()), JDUtilities.getResourceFile("."));
        File[] files;
        try {
            files = u.extract();
            if (files != null) {
                boolean c = false;
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getAbsolutePath().endsWith("readme.html")) {

                        JDUtilities.getGUI().showHTMLDialog(JDLocale.L("modules.packagemanager.loadednewpackage.title", "Paket Update installiert"), JDUtilities.getLocalFile(files[i]));
                        c = true;
                    }
                }
                if (!c) {
                    JDUtilities.getGUI().showMessageDialog(JDLocale.L("modules.packagemanager.loadednewpackage.title", "Paket Update installiert") + "\r\n" + downloadLink.getName() + " v" + dat[1]);
                }
                managerConfig.setProperty("PACKAGE_INSTALLED_VERSION_" + dat[0], dat[1]);
                managerConfig.save();
                new File(downloadLink.getFileOutput()).deleteOnExit();
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // }

    }
}
