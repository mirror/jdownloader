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

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.JDInit;
import jd.config.CFGConfig;
import jd.config.Configuration;
import jd.controlling.DistributeData;
import jd.event.ControlEvent;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.update.PackageData;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class PackageManager extends Interaction implements Serializable {

    private static final String NAME = JDLocale.L("interaction.packagemanager.name", "Pakete aktualisieren");

    private static ArrayList<PackageData> PACKAGE_DATA = null;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PackageManager() {

    }

    private void checkNewInstalled() {

        // ArrayList<PackageData> ret= new ArrayList<PackageData>();
   
        String links = "";
        String error = "";
        for (PackageData pa : getPackageData()) {
            if (pa.isInstalled()) {
                if(pa.getInstalledVersion()!=Integer.parseInt(pa.getStringProperty("version"))){
                    error += JDLocale.LF("system.update.error.message.infolink", "%s v.%s <a href='%s'>INFO</a><br/>", pa.getStringProperty("name"), pa.getStringProperty("version"), pa.getStringProperty("infourl"));

                }else{
                    
                    links += JDLocale.LF("system.update.success.message.infolink", "%s v.%s <a href='%s'>INFO</a><br/>", pa.getStringProperty("name"), pa.getStringProperty("version"), pa.getStringProperty("infourl"));

                    
                }
                pa.setInstalled(false);
             
                
            }
        }
     

        if(!links.equals(""))JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.LF("system.update.success.message", "Installed new updates<hr>%s", links), 15);
        if(!error.equals(""))JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.LF("system.update.error.message", "Installing updates FAILED for this packages:<hr>%s", links), 15);
        // ArrayList<File> slist = (ArrayList<File>)
        // managerConfig.getProperty("NEW_INSTALLED_SUCCESSFULL");
        // if (slist != null) {
        // for (File del : slist) {
        // del.delete();
        //
        // String html =
        // JDLocale.L("modules.packagemanager.installednewpackage.title",
        // "Package Installed") + "<hr><b>" + del.getName() + "</b><hr>";
        // JDUtilities.getGUI().showCountdownConfirmDialog(html, 15);
        // }
        // }
        //
        // ArrayList<File> flist = (ArrayList<File>)
        // managerConfig.getProperty("NEW_INSTALLED_FAILED");
        // if (flist != null) {
        // for (File del : flist) {
        // del.delete();
        // JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.L(
        // "modules.packagemanager.loadednewpackage.failed",
        // "Installation failed. try again: ") + "<hr><b>" + del.getName() +
        // "</b>", 15);
        //
        // }
        // }
        //
        // Object list = managerConfig.getProperty("NEW_INSTALLED");
        // managerConfig.setProperty("NEW_INSTALLED_SUCCESSFULL", null);
        // managerConfig.setProperty("NEW_INSTALLED", null);
        // managerConfig.setProperty("NEW_INSTALLED_FAILED", null);
        // managerConfig.save();
        // if (list == null) { return; }
        //
        // ArrayList<File> readmes = (ArrayList<File>) list;
        // for (File readme : readmes) {
        // String html;
        // if (readme == null) {
        //
        // continue;
        // }
        // html = JDUtilities.getLocalFile(readme);
        // if (Regex.matches(html, "src\\=\"(.*?)\"")) {
        // html = new Regex(html, "src\\=\"(.*?)\"").getMatch(0);
        // html = JDLocale.L("modules.packagemanager.infonewpackage.title",
        // "Package information") + "<hr><b>" + readme.getAbsolutePath() +
        // "</b><hr><a href='" + html + "'>" +
        // JDLocale.L("modules.packagemanager.loadednewpackage.more",
        // "More Information & Installnotes") + "</a>";
        // JDUtilities.getGUI().showCountdownConfirmDialog(html, 15);
        // } else {
        // JDUtilities.getGUI().showCountdownConfirmDialog(html, 15);
        // }
        //
        // }

    }

    @Override
    public boolean doInteraction(Object arg) {

        checkNewInstalled();

        ArrayList<PackageData> data = getPackageData();
        FilePackage fp = new FilePackage();
        fp.setName(JDLocale.L("modules.packagemanager.packagename", "JD-Update"));
        fp.setDownloadDirectory(JDUtilities.getResourceFile("packages").getAbsolutePath());
        PackageData dat;

        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        for (int i = 0; i < data.size(); i++) {
            dat = data.get(i);

            if (dat.isSelected() && !dat.isUptodate() && !dat.isUpdating()) {

                DistributeData distributeData = new DistributeData(dat.getStringProperty("url"));
                Vector<DownloadLink> links = distributeData.findLinks();
                dat.setUpdating(true);
                Iterator<DownloadLink> it = links.iterator();
                // while(it.hasNext())it.next().setDownloadPath(JDUtilities.
                // getResourceFile("packages").getAbsolutePath());
                while (it.hasNext()) {
                    DownloadLink next = it.next();
                    next.setFilePackage(fp);
                    next.setLinkType(DownloadLink.LINKTYPE_JDU);
                    next.setProperty("JDU", dat);

                }

                // Decryptersystem wird verwendet, allerdings wird der weg über
                // den linkgrabber vermieden
                ret.addAll(links);

            }
        }
        CFGConfig.getConfig("JDU").save();
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

    @SuppressWarnings("unchecked")
    public ArrayList<PackageData> getPackageData() {
        if (PACKAGE_DATA != null) { return PACKAGE_DATA; }
        Browser br = new Browser();
        br.setFollowRedirects(true);
        ArrayList<PackageData> data = (ArrayList<PackageData>) CFGConfig.getConfig("JDU").getProperty("PACKAGEDATA", new ArrayList<PackageData>());
        for (PackageData pd : data) {
            pd.setSortID(-1);
        }
        CFGConfig.getConfig("JDU").setProperty("PACKAGEDATA", data);

        try {
            //
            // ri = Plugin.getRequest(new URL("http://jdpackagelist.ath.cx"),
            // null, null, true);
            if (CFGConfig.getConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false)) {
                br.getPage("http://jdservice.ath.cx/update/packages/betalist.php");
            } else {
                br.getPage("http://jdservice.ath.cx/update/packages/list.php");
            }
          
            String xml = "<packages>" + br.getMatch("<packages>(.*?)</packages>") + "</packages>";

            // xml=xml.replaceAll("<!\\-\\-", "").replaceAll("\\-\\->", "");
            DocumentBuilderFactory factory;
            InputSource inSource;
            Document doc;

            factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            inSource = new InputSource(new StringReader(xml));
            doc = factory.newDocumentBuilder().parse(inSource);
            NodeList packages = doc.getFirstChild().getChildNodes();
            PackageData tmp;
            int ii = 0;
            all: for (int i = 0; i < packages.getLength(); i++) {
                Node entry = packages.item(i);

                tmp = new PackageData();
                NodeList values = entry.getChildNodes();
                int id = -1;
                for (int t = 0; t < values.getLength(); t++) {
                    if (values.item(t).getNodeName().equalsIgnoreCase("preselected") && values.item(t).getTextContent().equalsIgnoreCase("true")) {
                        tmp.setPreselected(true);
                    }
                    if (values.item(t).getNodeName().equalsIgnoreCase("id")) {
                        id = Integer.parseInt(values.item(t).getTextContent().trim());
                        tmp.setId(id);
                    }
                    tmp.setProperty(values.item(t).getNodeName(), values.item(t).getTextContent());
                }
                if (id < 0) continue;
                for (PackageData pd : data) {
                    if (pd.getId() == tmp.getId()) {
                        ii++;

                        pd.setSortID(ii);
                        pd.getProperties().putAll(tmp.getProperties());
                        continue all;
                    }
                }
                if (tmp.isPreselected()) {
                    tmp.setSelected(true);
                }
                ii++;
                tmp.setSortID(ii);
                data.add(tmp);

            }
            PACKAGE_DATA = data;
          
            CFGConfig.getConfig("JDU").save();
            return data;
        } catch (Exception e) {
      
            return new ArrayList<PackageData>();
        }

    }

    @Override
    public void initConfig() {

    }

    public ArrayList<PackageData> getDownloadedPackages() {
        ArrayList<PackageData> ret = new ArrayList<PackageData>();
        for (PackageData pa : getPackageData()) {
            if (pa.isDownloaded() && pa.isUpdating()) {
                ret.add(pa);
            }
        }
        return ret;

    }

    public void onDownloadedPackage(final DownloadLink downloadLink) {
        // File dir = JDUtilities.getResourceFile("packages");
        // File[] list = dir.listFiles(new JDFileFilter(null, ".jdu", false));
        // for( int i=0;i<list.length;i++){
        final PackageData dat = (PackageData) downloadLink.getProperty("JDU");
        if (dat == null) {
            logger.severe("Dat==null");
            return;
        }
        dat.setProperty("LOCALPATH", downloadLink.getFileOutput());
        dat.setDownloaded(true);
        CFGConfig.getConfig("JDU").save();
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
                boolean ch = false;
                all: for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    for (DownloadLink dLink : fp.getDownloadLinks()) {
                        if (dLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
                            ch = true;
                            break all;
                        }
                    }
                }
                if (!ch) {
                    String list = "";
                    for (PackageData pa : getDownloadedPackages()) {
                        list += pa.getStringProperty("name") + " v." + pa.getStringProperty("version") + "<br/>";
                    }
                    String message = JDLocale.LF("modules.packagemanager.downloadednewpackage.title2", "<p>Updates loaded. A JD restart is required.<br/> RESTART NOW?<hr>%s</p>", list);
                    boolean ret = JDUtilities.getGUI().showCountdownConfirmDialog(message, 15);
                    if (ret) {
                        new JDInit().doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1), true);

                    }
                }

            }
        }.start();

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
