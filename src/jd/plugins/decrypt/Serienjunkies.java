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

package jd.plugins.decrypt;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.skins.simple.SimpleGUI;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
    private static final String host = "Serienjunkies.org";

    public static String lastHtmlCode = "";

    private static final int saveScat = 1;

    private static final int sCatGrabb = 2;

    private static final int sCatNewestDownload = 1;

    private static final int sCatNoThing = 0;

    private static int[] useScat = new int[] { 0, 0 };

    private JCheckBox checkScat;
    private JComboBox methods;

    private boolean next = false;

    private boolean scatChecked = false;

    public Serienjunkies() {
        super();
        setConfigElements();
        default_password.add("serienjunkies.dl.am");
        default_password.add("serienjunkies.org");

    }

    public synchronized boolean canHandle(String data) {
        boolean cat = false;
        if(data==null)return false;
        data = data.replaceAll("http://vote.serienjunkies.org/?", "");
        if (data.contains("serienjunkies.org") && (data.contains("/?cat=") || data.contains("/?p="))) {
            cat = getSerienJunkiesCat(data.contains("/?p=")) != sCatNoThing;
        }
        boolean rscom = (Boolean) getPluginConfig().getProperty("USE_RAPIDSHARE_V2", true);
        boolean rsde = (Boolean) getPluginConfig().getProperty("USE_RAPIDSHAREDE_V2", true);
        boolean net = (Boolean) getPluginConfig().getProperty("USE_NETLOAD_V2", true);
        boolean uploaded = (Boolean) getPluginConfig().getProperty("USE_UPLOADED_V2", true);
        boolean simpleupload = (Boolean) getPluginConfig().getProperty("USE_SIMLEUPLOAD_V2", true);
        boolean filefactory = (Boolean) getPluginConfig().getProperty("USE_FILEFACTORY_V2", true);
        next = false;
        String hosterStr = "";
        if (rscom || rsde || net || uploaded) {
            hosterStr += "(";
            if (rscom) {
                hosterStr += isNext() + "rc[\\_\\-]";
            }
            if (rsde) {
                hosterStr += isNext() + "rs[\\_\\-]";
            }
            if (net) {
                hosterStr += isNext() + "nl[\\_\\-]";
            }
            if (uploaded) {
                hosterStr += isNext() + "ut[\\_\\-]";
            }
            if (simpleupload) {
                hosterStr += isNext() + "su[\\_\\-]";
            }
            if (filefactory) {
                hosterStr += isNext() + "ff[\\_\\-]";
            }
            if (cat) {
                hosterStr += isNext() + "cat\\=[\\d]+";
                hosterStr += isNext() + "p\\=[\\d]+";
            }

            hosterStr += ")";
        } else {
            hosterStr += "not";
        }
        Matcher matcher = Pattern.compile("http://[\\w\\.]{0,10}serienjunkies\\.org.*" + hosterStr + ".*", Pattern.CASE_INSENSITIVE).matcher(data);
        if (matcher.find()) {
            return true;
        } else {

            String[] links = new Regex(data, "http://[\\w\\.]{3,10}\\.serienjunkies.org/.*", Pattern.CASE_INSENSITIVE).getColumn(-1);
            Pattern pat = Pattern.compile("http://[\\w\\.]{3,10}\\.serienjunkies.org/.*(rc[\\_\\-]|rs[\\_\\-]|nl[\\_\\-]|ut[\\_\\-]|su[\\_\\-]|ff[\\_\\-]|cat\\=[\\d]+|p\\=[\\d]+).*", Pattern.CASE_INSENSITIVE);
            for (String element : links) {
                Matcher m = pat.matcher(element);

                if (!m.matches()) { return true; }
            }
        }
        return false;
    }

    public boolean collectCaptchas() {

        return false;
    }

    private DownloadLink createdl(String parameter, String[] info) {
        int size = 100;
        String name = null, linkName = null, title = null;
        String[] mirrors = null;
        if (info != null) {
            name = Encoding.htmlDecode(info[1]);
            size = Integer.parseInt(info[0]);
            title = Encoding.htmlDecode(info[3]);
            mirrors = getMirrors(parameter, info[2]);
        }
        try {
            linkName = ((title.length() > 10 ? title.substring(0, 10) : title) + "#" + name).replaceAll("\\.", " ").replaceAll("[^\\w \\#]", "").trim() + ".rar";
        } catch (Exception e) {
            // TODO: handle exception
        }
        if (linkName == null || parameter.matches("http://serienjunkies.org/sa[fv]e/.*") || parameter.matches("http://download.serienjunkies.org/..\\-.*")) {
            size = 100;
            linkName = parameter.replaceFirst(".*/..[\\_\\-]", "").replaceFirst("\\.html?", "");
        }
        String hostname = getHostname(parameter);
        DownloadLink dlink = new DownloadLink(null, name, getHost(), "http://sjdownload.org/" + hostname + "/" + linkName, true);
        dlink.setProperty("link", parameter);
        dlink.setProperty("mirrors", mirrors);
        if (name != null) {
            dlink.setSourcePluginComment(title + " ::: " + name);
            dlink.setDownloadSize(size * 1024 * 1024);
        }
        dlink.getLinkStatus().setStatusText(getHostname(parameter));
        return dlink;
    }

    public String cutMatches(String data) {
        return data.replaceAll("(?i)http://[\\w\\.]*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es).*", "--CUT--");
    }

    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        Browser.clearCookies("serienjunkies.org");
        br.getPage("http://serienjunkies.org/enter/");

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (parameter.matches(".*\\?(cat|p)\\=[\\d]+.*")) {
            boolean isP = parameter.contains("/?p=");
            int catst = getSerienJunkiesCat(isP);
            scatChecked = false;
            int cat = Integer.parseInt(parameter.replaceFirst(".*\\?(cat|p)\\=", "").replaceFirst("[^\\d].*", ""));
            if (sCatNewestDownload == catst) {
                br.getPage("http://serienjunkies.org/");

                Pattern pattern = Pattern.compile("<a href=\"http://serienjunkies.org/\\?cat\\=([\\d]+)\">(.*?)</a><br", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(br + "");
                String name = null;
                while (matcher.find()) {
                    if (Integer.parseInt(matcher.group(1)) == cat) {
                        name = matcher.group(2).toLowerCase();
                        break;
                    }
                }
                if (name == null) { return decryptedLinks; }
                br.getPage(parameter);
                name += " ";
                String[] bet = null;
                while (bet == null) {
                    name = name.substring(0, name.length() - 1);
                    if (name.length() == 0) { return decryptedLinks; }
                    try {
                        bet = br.getRegex("<p><strong>(" + name + ".*?)</strong>(.*?)</p>").getMatches()[0];
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
                lastHtmlCode = br + "";
                String[] links = HTMLParser.getHttpLinks(bet[1], br.getRequest().getUrl().toString());
                for (String element : links) {
                    decryptedLinks.add(createDownloadlink(element));
                }
                return decryptedLinks;
            } else if (catst == sCatGrabb) {

                String htmlcode = "";
                if (isP) {
                    br.getPage(parameter);
                    htmlcode = br + "";
                } else {
                    br.getPage("http://serienjunkies.org/?cat=" + cat);
                    htmlcode = br + "";
                    try {
                        int pages = Integer.parseInt(br.getRegex("<p align=\"center\">  Pages \\(([\\d]+)\\):").getMatch(0));
                        for (int i = 2; i < pages + 1; i++) {
                            htmlcode += "\n" + br.getPage("http://serienjunkies.org/?cat=" + cat + "&paged=" + i);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                String[] titles = htmlcode.replaceFirst("(?is).*?(<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*>)", "$1").split("<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*?>");
                for (String element : titles) {

                    String title = new Regex(element, "([^><]*?)</a>").getMatch(0);
                    String[] sp = element.split("(?is)<strong>Größe:</strong>[\\s]*");
                    for (String element2 : sp) {
                        String size = new Regex(element2, "[\\d]+").getMatch(-1);
                        String[][] links = new Regex(element2, "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();
                        for (String[] element3 : links) {
                            String[] links2 = HTMLParser.getHttpLinks(element3[1], parameter);
                            for (String element4 : links2) {
                                if (canHandle(element4)) {
                                    // if (getPluginConfig().getBooleanProperty(
                                    // "USE_DIREKTDECRYPT", false)) {
                                    // decryptedLinks.addAll((new
                                    // jd.plugins.host
                                    // .Serienjunkies()).getDLinks(element4));
                                    // } else {
                                    decryptedLinks.add(createdl(element4, new String[] { size, element3[0], element3[1], title }));
                                    // }

                                }

                            }
                        }
                    }
                }

                return decryptedLinks;
            } else {
                return decryptedLinks;
            }
        }
        // if (getPluginConfig().getBooleanProperty("USE_DIREKTDECRYPT", false))
        // {
        // // step.setParameter((new
        // // jd.plugins.host.Serienjunkies()).getDLinks(parameter));
        // } else {

        String[] info = getLinkName(parameter);

        if (info == null) {
            br.getPage("http://serienjunkies.org/?s=" + parameter.replaceFirst(".*/", "").replaceFirst("\\.html?$", "") + "&submit=Suchen");
            lastHtmlCode = br + "";
            info = getLinkName(parameter);
        }

        decryptedLinks.add(createdl(parameter, info));

        // }
        return decryptedLinks;
    }

    public String getCoder() {
        return "JD-Team";
    }

    public String[] getDecryptableLinks(String data) {
        String[] links = new Regex(data, "http://[\\w\\.]*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es)[^\"]*", Pattern.CASE_INSENSITIVE).getColumn(-1);
        Vector<String> ret = new Vector<String>();
        scatChecked = true;
        for (String element : links) {
            if (canHandle(element)) {
                ret.add(element);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public String getHost() {
        return host;
    }

    private String getHostname(String link) {
        if (link.matches(".*rc[\\_\\-].*")) {
            return "RapidshareCom";
        } else if (link.matches(".*rs[\\_\\-].*")) {
            return "RapidshareDe";
        } else if (link.matches(".*nl[\\_\\-].*")) {
            return "NetloadIn";
        } else if (link.matches(".*ut[\\_\\-].*")) {
            return "UploadedTo";
        } else if (link.matches(".*su[\\_\\-].*")) {
            return "SimpleUploadNet";
        } else if (link.matches(".*ff[\\_\\-].*")) {
            return "FileFactoryCom";
        } else {
            return "RapidshareCom";
        }
    }

    private String[] getLinkName(String link) {
        String[] titles = lastHtmlCode.replaceFirst("(?is).*?(<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*>)", "$1").split("<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*?>");
        for (String element : titles) {

            String title = new Regex(element, "([^><]*?)</a>").getMatch(0);
            String[] sp = element.split("(?is)<strong>Größe:</strong>[\\s]*");
            for (String element2 : sp) {
                String size = new Regex(element2, "[\\d]+").getMatch(0);
                String[][] links = new Regex(element2.replaceAll("<a href=\"http://vote.serienjunkies.org.*?</a>", ""), "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();

                for (String[] element3 : links) {
                    try {

                        if (element3[1].contains(link)) { return new String[] { size, element3[0], element3[1], title }; }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
        }

        return null;
    }

    private String[] getMirrors(String link, String htmlcode) {
        String[] sp = htmlcode.split("<strong>.*?</strong>");
        ArrayList<String> ret = new ArrayList<String>();
        int c = -1;
        for (int i = 0; i < sp.length; i++) {
            if (sp[i].contains(link)) {

                String[] links = HTMLParser.getHttpLinks(sp[i], link);
                sp[i] = null;
                for (int j = 0; j < links.length; j++) {
                    if (links[j].equals(link)) {
                        c = j;
                        break;
                    }
                }
                break;
            }
        }
        if (c == -1) { return null; }
        for (String element : sp) {
            String mirror = null;
            try {
                mirror = HTMLParser.getHttpLinks(element, link)[c];
            } catch (Exception e) {
                // TODO: handle exception
            }
            if (mirror != null && !mirror.matches("[\\s]*")) {
                ret.add(mirror);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public String getPluginName() {
        return host;
    }

    private int getSerienJunkiesCat(boolean isP) {

        sCatDialog(isP);
        return useScat[0];

    }

    public Pattern getSupportedLinks() {
        return null;
    }

    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    private String isNext() {
        if (next) {
            return "|";
        } else {
            next = true;
        }
        return "";

    }

    private void sCatDialog(final boolean isP) {
        if (scatChecked || useScat[1] == saveScat) { return; }
        new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {

            /**
             * 
             */
            private static final long serialVersionUID = -5144850223169000644L;

            void init() {
                setLayout(new BorderLayout());
                setModal(true);
                setTitle(JDLocale.L("plugins.SerienJunkies.CatDialog.title", "SerienJunkies ::CAT::"));
                setAlwaysOnTop(true);
                setLocation(20, 20);
                JPanel panel = new JPanel(new GridBagLayout());
                final class meth {
                    public String name;

                    public int var;

                    public meth(String name, int var) {
                        this.name = name;
                        this.var = var;
                    }

                    public String toString() {

                        return name;
                    }
                }
                ;
                addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {

                    }

                    public void windowClosed(WindowEvent e) {

                    }

                    public void windowClosing(WindowEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, 0 };
                        dispose();

                    }

                    public void windowDeactivated(WindowEvent e) {

                    }

                    public void windowDeiconified(WindowEvent e) {

                    }

                    public void windowIconified(WindowEvent e) {

                    }

                    public void windowOpened(WindowEvent e) {

                    }
                });
                meth[] meths = null;
                if (isP) {
                    meths = new meth[2];
                    meths[0] = new meth("Staffel nicht hinzufügen", sCatNoThing);
                    meths[1] = new meth("Alle Serien in dieser Staffel hinzufügen", sCatGrabb);
                } else {
                    meths = new meth[3];
                    meths[0] = new meth("Kategorie nicht hinzufügen", sCatNoThing);
                    meths[1] = new meth("Alle Serien in dieser Kategorie hinzufügen", sCatGrabb);
                    meths[2] = new meth("Den neusten Download dieser Kategorie hinzufügen", sCatNewestDownload);
                }
                methods = new JComboBox(meths);
                checkScat = new JCheckBox("Einstellungen für diese Sitzung beibehalten?", true);
                Insets insets = new Insets(0, 0, 0, 0);
                JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("plugins.SerienJunkies.CatDialog.action", "Wählen sie eine Aktion aus:")), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, methods, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JDUtilities.addToGridBag(panel, checkScat, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                JButton btnOK = new JButton(JDLocale.L("gui.btn_continue", "OK"));
                btnOK.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, checkScat.isSelected() ? saveScat : 0 };
                        dispose();
                    }

                });
                JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                add(panel, BorderLayout.CENTER);
                pack();
                setVisible(true);
            }

        }.init();
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        // config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // getPluginConfig(), "USE_DIREKTDECRYPT",
        // JDLocale.L("plugins.SerienJunkies.decryptImmediately",
        // "Decrypt immediately")));
        // cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster selection")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_RAPIDSHARE_V2", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_RAPIDSHAREDE_V2", "Rapidshare.de"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_NETLOAD_V2", "Netload.in"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_UPLOADED_V2", "Uploaded.to"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_SIMLEUPLOAD_V2", "SimpleUpload.net"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "USE_FILEFACTORY_V2", "FileFactory.com"));
        cfg.setDefaultValue(true);
    }

    public boolean useUserinputIfCaptchaUnknown() {

        return false;
    }
}
