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
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import jd.JDInit;
import jd.OptionalPluginWrapper;
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

    private static final long serialVersionUID = 1L;

    public PackageManager() {

    }

    private void checkNewInstalled() {
        String links = "";
        String error = "";
        for (PackageData pa : getPackageData()) {
            if (pa.isInstalled()) {
                if (pa.getInstalledVersion() != Integer.parseInt(pa.getStringProperty("version"))) {
                    error += JDLocale.LF("system.update.error.message.infolink", "%s v.%s <a href='%s'>INFO</a><br/>", pa.getStringProperty("name"), pa.getStringProperty("version"), pa.getStringProperty("infourl"));
                } else {
                    links += JDLocale.LF("system.update.success.message.infolink", "%s v.%s <a href='%s'>INFO</a><br/>", pa.getStringProperty("name"), pa.getStringProperty("version"), pa.getStringProperty("infourl"));
                }
                pa.setInstalled(false);

                for (OptionalPluginWrapper plg : OptionalPluginWrapper.getOptionalWrapper()) {
                    if (JDUtilities.getConfiguration().getBooleanProperty(plg.getConfigParamKey(), false) != JDUtilities.getConfiguration().getBooleanProperty(plg.getConfigParamKey(), true)) {
                        JDUtilities.getConfiguration().setProperty(plg.getConfigParamKey(), true);
                        plg.getPlugin().initAddon();
                    }
                }

            }
        }

        if (!links.equals("")) JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.LF("system.update.success.message", "Installed new updates<hr>%s", links), 15);
        if (!error.equals("")) JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.LF("system.update.error.message", "Installing updates FAILED for this packages:<hr>%s", links), 15);

    }

    @Override
    public boolean doInteraction(Object arg) {
        checkNewInstalled();

        FilePackage fp = new FilePackage();
        fp.setName(JDLocale.L("modules.packagemanager.packagename", "JD-Update"));
        fp.setDownloadDirectory(JDUtilities.getResourceFile("packages").getAbsolutePath());

        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        ArrayList<PackageData> data = getPackageData();
        logger.finer("PM: " + data.size() + " packages found");
        for (PackageData pkg : data) {
            validate(pkg);

            if (pkg.isSelected() && !pkg.isUptodate() && !pkg.isUpdating()) {
                pkg.setUpdating(true);

                DistributeData distributeData = new DistributeData(pkg.getStringProperty("url"));
                Vector<DownloadLink> links = distributeData.findLinks();
                for (DownloadLink link : links) {
                    logger.info("Add link " + link + " : " + pkg);
                    link.setFilePackage(fp);
                    link.setLinkType(DownloadLink.LINKTYPE_JDU);
                    link.setProperty("JDU", pkg);
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

    private void validate(PackageData dat) {
        // Installation fehgeschlagen, oder lokale JDU File von User entfernt
        if (dat.isDownloaded() && !new File(dat.getStringProperty("LOCALPATH", "")).exists()) {
            logger.info("PM: validate restet1");
            dat.setDownloaded(false);
            dat.setUpdating(false);
        }
        // Updatelink wurde vermutlich aus der Liste entfernt
        if (!dat.isDownloaded() && dat.isUpdating() && !JDUtilities.getController().hasDownloadLinkURL(dat.getStringProperty("url"))) {
            logger.info("PM: validate restet2");
            dat.setUpdating(false);
        }

    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<PackageData> getPackageData() {
        if (PACKAGE_DATA != null) return PACKAGE_DATA;
        Browser br = new Browser();
        br.setFollowRedirects(true);
        ArrayList<PackageData> data = (ArrayList<PackageData>) CFGConfig.getConfig("JDU").getProperty("PACKAGEDATA", new ArrayList<PackageData>());
        for (PackageData pd : data) {
            pd.setSortID(-1);
        }
        CFGConfig.getConfig("JDU").setProperty("PACKAGEDATA", data);

        try {
            br.getPage("http://service.jdownloader.org/update/packages/list.php?jd=" + JDUtilities.getRevision() + "&r=" + System.currentTimeMillis());

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

            CFGConfig.getConfig("JDU").setProperty("PACKAGEDATA", PACKAGE_DATA);

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

    public synchronized void onDownloadedPackage(final DownloadLink downloadLink) {
        PackageData d = (PackageData) downloadLink.getProperty("JDU");
        final PackageData dat;
        logger.finer("downloaded addon");

        ArrayList<PackageData> data = this.getPackageData();
        boolean found = false;
        for (PackageData pd : data) {

            if (pd.equals(d)) {
                logger.finer("Update found in list");
                {
                    dat = pd;
                    found = true;

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

                    break;
                }

            }
        }
        if (!found) {
            logger.severe("Update Package " + d + " not found in packagelist");
        }

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
