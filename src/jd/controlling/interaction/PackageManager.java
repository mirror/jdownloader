//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.OptionalPluginWrapper;
import jd.config.CFGConfig;
import jd.controlling.DistributeData;
import jd.event.ControlEvent;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.update.JDUpdateUtils;
import jd.update.PackageData;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class PackageManager extends Interaction implements Serializable {

    private static ArrayList<PackageData> PACKAGE_DATA = null;

    private static final long serialVersionUID = 1L;

    private void checkNewInstalled() {
        StringBuilder links = new StringBuilder();
        StringBuilder error = new StringBuilder();
        for (PackageData pa : getPackageData()) {
            if (pa.isInstalled()) {
                if (pa.getInstalledVersion() != Integer.parseInt(pa.getStringProperty("version"))) {
                    error.append(JDLocale.LF("system.update.error.message.infolink", "%s v.%s <a href='%s'>INFO</a><br/>", pa.getStringProperty("name"), pa.getStringProperty("version"), pa.getStringProperty("infourl")));
                } else {
                    links.append(JDLocale.LF("system.update.success.message.infolink", "%s v.%s <a href='%s'>INFO</a><br/>", pa.getStringProperty("name"), pa.getStringProperty("version"), pa.getStringProperty("infourl")));
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

        if (links.length() > 0) JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.LF("system.update.success.message", "Installed new updates<hr>%s", links.toString()), 15);
        if (error.length() > 0) JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.LF("system.update.error.message", "Installing updates FAILED for this packages:<hr>%s", links.toString()), 15);

    }

    @Override
    public boolean doInteraction(Object arg) {
        checkNewInstalled();

        CFGConfig config = CFGConfig.getConfig("JDU");
        boolean oldUpdatePackage = false;
        FilePackage fp = new FilePackage();
        fp.setName(JDLocale.L("modules.packagemanager.packagename", "JD-Update"));
        fp.setDownloadDirectory(JDUtilities.getResourceFile("packages").getAbsolutePath());

        // Existiert schon ein JD-Update Package in der DownloadListe?
        for (FilePackage fp_cur : JDUtilities.getController().getPackages()) {
            if (fp_cur.getName().equals(fp.getName()) && fp_cur.getDownloadDirectory().equals(fp.getDownloadDirectory())) {
                fp = fp_cur;
                oldUpdatePackage = true;
                break;
            }
        }

        ArrayList<PackageData> data = getPackageData();
        logger.finer("PM: " + data.size() + " packages found");
        for (PackageData pkg : data) {
            validate(pkg);

            if (pkg.isSelected() && !pkg.isUptodate() && !pkg.isUpdating()) {
                pkg.setUpdating(true);

                DistributeData distributeData = null;
                if (config.getBooleanProperty("SUPPORT_JD", true)) {
                    distributeData = new DistributeData(pkg.getStringProperty("url"));
                } else {
                    distributeData = new DistributeData(pkg.getStringProperty("light-url"));
                }
                Vector<DownloadLink> links = distributeData.findLinks();
                for (DownloadLink link : links) {
                    logger.info("Add link " + link /* + " : " + pkg */);
                    link.setFilePackage(fp);
                    link.setLinkType(DownloadLink.LINKTYPE_JDU);
                    link.setProperty("JDU", pkg);
                }
            }
        }
        config.save();
        if (fp.size() > 0) {
            if (!oldUpdatePackage) JDUtilities.getController().addPackageAt(fp, 0);
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
        if (!dat.isDownloaded() && dat.isUpdating() && !(JDUtilities.getController().hasDownloadLinkURL(dat.getStringProperty("url")) || JDUtilities.getController().hasDownloadLinkURL(dat.getStringProperty("light-url")))) {
            logger.info("PM: validate restet2");
            dat.setUpdating(false);
        }

    }

    @Override
    public String getInteractionName() {
        return JDLocale.L("interaction.packagemanager.name", "Pakete aktualisieren");
    }

    @SuppressWarnings("unchecked")
    public ArrayList<PackageData> getPackageData() {
        if (PACKAGE_DATA != null) return PACKAGE_DATA;
        CFGConfig config = CFGConfig.getConfig("JDU");
        Browser br = new Browser();
        br.setFollowRedirects(true);
        ArrayList<PackageData> data = (ArrayList<PackageData>) config.getProperty("PACKAGEDATA", new ArrayList<PackageData>());
        for (int i = data.size() - 1; i >= 0; --i) {
            if (data.get(i).getStringProperty("category").indexOf("[LIGHT]") >= 0) {
                data.remove(i);
                continue;
            }
            data.get(i).setSortID(-1);
        }
        config.setProperty("PACKAGEDATA", data);

        try {
            String list = JDUpdateUtils.get_AddonList();

            String xml = new Regex(list, "<packages>(.*?)</packages>").getMatch(-1);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            InputSource inSource = new InputSource(new StringReader(xml));
            Document doc = factory.newDocumentBuilder().parse(inSource);
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

            config.setProperty("PACKAGEDATA", PACKAGE_DATA);
            config.save();

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
                            StringBuilder list = new StringBuilder();
                            for (PackageData pa : getDownloadedPackages()) {
                                list.append(pa.getStringProperty("name"));
                                list.append(new char[] { ' ', 'v', '.' });
                                list.append(pa.getStringProperty("version"));
                                list.append(new char[] { '<', 'b', 'r', '/', '>' });
                            }
                            String message = JDLocale.LF("modules.packagemanager.downloadednewpackage.title2", "<p>Updates loaded. A JD restart is required.<br/> RESTART NOW?<hr>%s</p>", list.toString());
                            boolean ret = JDUtilities.getGUI().showCountdownConfirmDialog(message, 15);
                            if (ret) {
                                new WebUpdate().doWebupdate(true);
                            }
                        }

                    }
                }.start();

                break;

            }
        }
        if (!found) {
            logger.severe("Update Package " + d + " not found in packagelist");
        }

    }

    @Override
    public String toString() {
        return JDLocale.L("interaction.packagemanager.name", "Pakete aktualisieren");
    }
}
