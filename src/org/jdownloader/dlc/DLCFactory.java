package org.jdownloader.dlc;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilderFactory;

import jd.config.SubConfiguration;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

import org.appwork.utils.IO;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.container.D;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.gui.translate._GUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class DLCFactory extends D {

    public String encryptDLC(String xml) {

        final String[] encrypt = encrypt(xml);
        if (encrypt == null) {
            logger.severe("Container Encryption failed.");
            return null;
        }
        final String key = encrypt[1];
        xml = encrypt[0];
        final String service = "http://service.jdownloader.org/dlcrypt/service.php";
        try {
            final String dlcKey = callService(service, key);
            if (dlcKey == null) return null;
            return xml + dlcKey;
        } catch (final Exception e) {
            logger.log(e);
        }
        return null;
    }

    private String callService(final String service, final String key) throws Exception {
        logger.finer("Call " + service);
        final Browser br = new Browser();
        br.postPage(service, "jd=1&srcType=plain&data=" + key);
        logger.info("Call re: " + br.toString());
        if (!br.getHttpConnection().isOK() || !br.containsHTML("<rc>")) {
            return null;
        } else {
            final String dlcKey = br.getRegex("<rc>(.*?)</rc>").getMatch(0);
            if (dlcKey.trim().length() < 80) return null;
            return dlcKey;
        }
    }

    public ArrayList<DownloadLink> getContainerLinks(final File file) {
        LinkCrawler lc = new LinkCrawler();
        lc.setFilter(LinkFilterController.getInstance());
        lc.crawl("file://" + file.getAbsolutePath());
        lc.waitForCrawling();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (CrawledLink link : lc.getCrawledLinks()) {
            if (link.getDownloadLink() == null) continue;
            ret.add(link.getDownloadLink());
        }
        return ret;
    }

    public void createDLC(List<DownloadLink> links) {

        try {

            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.CreateDLCAction_actionPerformed_title_(), null, null);
            d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
            d.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {

                    return "*.dlc (DownloadLinkContainer)";

                }

                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase(Locale.ENGLISH).endsWith(".dlc");

                }
            });
            d.setType(FileChooserType.SAVE_DIALOG);
            d.setMultiSelection(false);
            Dialog.I().showDialog(d);
            File file = d.getSelectedFile();

            if (file == null) return;

            if (!file.getAbsolutePath().endsWith(".dlc")) {
                file = new File(file.getAbsolutePath() + ".dlc");
            }
            String xml = createContainerString(links);

            if (xml != null) {
                final String cipher = encryptDLC(xml);
                if (cipher != null) {
                    IO.writeStringToFile(file, cipher);
                    Dialog.getInstance().showMessageDialog(_GUI._.DLCFactory_createDLC_created_(file.getAbsolutePath()));
                    return;
                }
            }
            logger.severe("Container creation failed");

            Dialog.getInstance().showErrorDialog("Container encryption failed");
        } catch (DialogCanceledException e) {

        } catch (DialogClosedException e) {

        } catch (IOException e) {
            Dialog.getInstance().showExceptionDialog("DLC Error", e.getMessage(), e);
        }

    }

    public void createDLCByCrawledLinks(List<CrawledLink> links) {

        try {

            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.CreateDLCAction_actionPerformed_title_(), null, null);
            d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
            d.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {

                    return "*.dlc (DownloadLinkContainer)";

                }

                @Override
                public boolean accept(File f) {
                    return f.getName().toLowerCase(Locale.ENGLISH).endsWith(".dlc");

                }
            });
            d.setType(FileChooserType.SAVE_DIALOG);
            d.setMultiSelection(false);
            Dialog.I().showDialog(d);
            File file = d.getSelectedFile();

            if (file == null) return;

            if (!file.getAbsolutePath().endsWith(".dlc")) {
                file = new File(file.getAbsolutePath() + ".dlc");
            }
            String xml = createContainerStringByCrawledLinks(links);

            if (xml != null) {
                final String cipher = encryptDLC(xml);
                if (cipher != null) {
                    IO.writeStringToFile(file, cipher);
                    Dialog.getInstance().showMessageDialog(_GUI._.DLCFactory_createDLC_created_(file.getAbsolutePath()));
                    return;
                }
            }
            logger.severe("Container creation failed");

            Dialog.getInstance().showErrorDialog("Container encryption failed");
        } catch (DialogCanceledException e) {

        } catch (DialogClosedException e) {

        } catch (IOException e) {
            Dialog.getInstance().showExceptionDialog("DLC Error", e.getMessage(), e);
        }
    }

    private String createContainerStringByCrawledLinks(List<CrawledLink> links) {
        HashMap<String, CrawledLink> map = new HashMap<String, CrawledLink>();
        ArrayList<CrawledLink> filter = new ArrayList<CrawledLink>();
        // filter
        for (CrawledLink l : links) {
            String url = l.getDownloadLink().getBrowserUrl();
            if (!map.containsKey(url)) {
                filter.add(l);
            }
            map.put(url, l);
        }
        links = filter;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        SubConfiguration cfg = SubConfiguration.getConfig("DLCCONFIG");
        InputSource inSourceHeader = new InputSource(new StringReader("<header><generator><app></app><version/><url></url></generator><tribute/><dlcxmlversion/></header>"));
        InputSource inSourceContent = new InputSource(new StringReader("<content/>"));

        try {
            Document content = factory.newDocumentBuilder().parse(inSourceContent);
            Document header = factory.newDocumentBuilder().parse(inSourceHeader);
            Node header_generator_app = header.getFirstChild().getFirstChild().getChildNodes().item(0);
            Node header_generator_version = header.getFirstChild().getFirstChild().getChildNodes().item(1);
            Node header_generator_url = header.getFirstChild().getFirstChild().getChildNodes().item(2);
            header_generator_app.appendChild(header.createTextNode(Encoding.Base64Encode("JDownloader")));
            header_generator_version.appendChild(header.createTextNode(Encoding.Base64Encode(JDUtilities.getRevision())));
            header_generator_url.appendChild(header.createTextNode(Encoding.Base64Encode("http://jdownloader.org")));

            Node header_tribute = header.getFirstChild().getChildNodes().item(1);

            if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {
                Element element = header.createElement("name");
                header_tribute.appendChild(element);

                element.appendChild(header.createTextNode(Encoding.Base64Encode(UserIO.getInstance().requestInputDialog("Uploader Name"))));

            }
            if (cfg.getStringProperty("UPLOADERNAME", null) != null && cfg.getStringProperty("UPLOADERNAME", null).trim().length() > 0) {
                Element element = header.createElement("name");
                header_tribute.appendChild(element);
                element.appendChild(header.createTextNode(Encoding.Base64Encode(cfg.getStringProperty("UPLOADERNAME", null))));

            }
            Node header_dlxxmlversion = header.getFirstChild().getChildNodes().item(2);

            header_dlxxmlversion.appendChild(header.createTextNode(Encoding.Base64Encode("20_02_2008")));

            ArrayList<CrawledPackage> packages = new ArrayList<CrawledPackage>();
            HashMap<CrawledPackage, ArrayList<CrawledLink>> packageLinksMap = new HashMap<CrawledPackage, ArrayList<CrawledLink>>();
            for (int i = 0; i < links.size(); i++) {

                if (!packages.contains(links.get(i).getParentNode())) {
                    packages.add(links.get(i).getParentNode());
                }
                ArrayList<CrawledLink> list = packageLinksMap.get(links.get(i).getParentNode());
                if (list == null) {
                    list = new ArrayList<CrawledLink>();
                    packageLinksMap.put(links.get(i).getParentNode(), list);
                }
                list.add(links.get(i));

            }

            for (int i = 0; i < packages.size(); i++) {
                Element pkg = content.createElement("package");
                if (packages.get(i) == null) {
                    pkg.setAttribute("name", Encoding.Base64Encode("various"));
                } else {
                    pkg.setAttribute("name", Encoding.Base64Encode(packages.get(i).getName()));
                    pkg.setAttribute("comment", Encoding.Base64Encode(packages.get(i).getComment()));
                    String category = Encoding.Base64Encode("various");
                    if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {
                        category = Encoding.Base64Encode(UserIO.getInstance().requestInputDialog("Category for package " + packages.get(i).getName()));
                    }
                    pkg.setAttribute("category", category);

                }
                // <package name="cGFrZXQx" passwords="eyJwYXNzIiwgInBhc3MyIn0="
                // comment="RGFzIGlzdCBlaW4gVGVzdGNvbnRhaW5lcg=="
                // category="bW92aWU=">

                content.getFirstChild().appendChild(pkg);

                ArrayList<CrawledLink> tmpLinks = packageLinksMap.get(packages.get(i));

                for (int x = 0; x < tmpLinks.size(); x++) {
                    Element file = content.createElement("file");
                    pkg.appendChild(file);
                    Element url = content.createElement("url");
                    Element filename = content.createElement("filename");
                    Element size = content.createElement("size");
                    String link = tmpLinks.get(x).getDownloadLink().getBrowserUrl();
                    String encode = Encoding.Base64Encode(link);
                    // String decoded=JDUtilities.Base64Decode(encode);
                    url.appendChild(content.createTextNode(encode));

                    // url.appendChild(content.createTextNode(JDUtilities.
                    // Base64Encode(tmpLinks.get(x).getDownloadURL())));
                    filename.appendChild(content.createTextNode(Encoding.Base64Encode(tmpLinks.get(x).getName())));

                    size.appendChild(content.createTextNode(Encoding.Base64Encode(tmpLinks.get(x).getSize() + "")));

                    pkg.getLastChild().appendChild(url);
                    pkg.getLastChild().appendChild(filename);
                    pkg.getLastChild().appendChild(size);

                }

            }

            int ind1 = xmltoStr(header).indexOf("<header");
            int ind2 = xmltoStr(content).indexOf("<content");
            String ret = xmltoStr(header).substring(ind1) + xmltoStr(content).substring(ind2);

            return "<dlc>" + ret + "</dlc>";
        } catch (Exception e) {
            logger.log(e);
        }
        return null;
    }
}
