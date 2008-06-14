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
import java.io.File;
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
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
    private static final String host = "Serienjunkies.org";

    private String version = "5.0.0.0";

    private boolean next = false;

    private static final int saveScat = 1;

    private static final int sCatNoThing = 0;

    private static final int sCatNewestDownload = 1;

    private static final int sCatGrabb = 2;

    // private static final String PATTERN_FOR_CAPTCHA_BOT = "Du hast zu oft das
    // Captcha falsch eingegeben";

    private static int[] useScat = new int[] { 0, 0 };
    public static String lastHtmlCode = "";

    private boolean scatChecked = false;

    private JComboBox methods;

    private JCheckBox checkScat;

    public Serienjunkies() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigElements();

        default_password.add("serienjunkies.dl.am");
        default_password.add("serienjunkies.org");

    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        // von coa gefixed
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    private void sCatDialog(final boolean isP) {
        if (scatChecked || useScat[1] == saveScat) return;
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
                    public int var;

                    public String name;

                    public meth(String name, int var) {
                        this.name = name;
                        this.var = var;
                    }

                    @Override
                    public String toString() {
                        // TODO Auto-generated method stub
                        return name;
                    }
                }
                ;
                addWindowListener(new WindowListener() {

                    public void windowActivated(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowClosed(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowClosing(WindowEvent e) {
                        useScat = new int[] { ((meth) methods.getSelectedItem()).var, 0 };
                        dispose();

                    }

                    public void windowDeactivated(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowDeiconified(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowIconified(WindowEvent e) {
                        // TODO Auto-generated method stub

                    }

                    public void windowOpened(WindowEvent e) {
                        // TODO Auto-generated method stub

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

    private int getSerienJunkiesCat(boolean isP) {

        sCatDialog(isP);
        return useScat[0];

    }

    private String isNext() {
        if (next)
            return "|";
        else
            next = true;
        return "";

    }

    @Override
    public boolean collectCaptchas() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean useUserinputIfCaptchaUnknown() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Pattern getSupportedLinks() {
        return null;
    }

    @Override
    public synchronized boolean canHandle(String data) {
        boolean cat = false;
        // http://serienjunkies.org/?cat=3217
        data = data.replaceAll("http://vote.serienjunkies.org/?", "");
        if (data.contains("serienjunkies.org") && (data.contains("/?cat=") || data.contains("/?p="))) {
            cat = (getSerienJunkiesCat(data.contains("/?p=")) != sCatNoThing);
        }
        boolean rscom = (Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true);
        boolean rsde = (Boolean) this.getProperties().getProperty("USE_RAPIDSHAREDE", false);
        boolean net = (Boolean) this.getProperties().getProperty("USE_NETLOAD", false);
        boolean uploaded = (Boolean) this.getProperties().getProperty("USE_UPLOADED", false);
        boolean simpleupload = (Boolean) this.getProperties().getProperty("USE_SIMLEUPLOAD", false);
        boolean filefactory = (Boolean) this.getProperties().getProperty("USE_FILEFACTORY", false);
        next = false;
        String hosterStr = "";
        if (rscom || rsde || net || uploaded) {
            hosterStr += "(";
            if (rscom) hosterStr += isNext() + "rc[\\_\\-]";
            if (rsde) hosterStr += isNext() + "rs[\\_\\-]";
            if (net) hosterStr += isNext() + "nl[\\_\\-]";
            if (uploaded) hosterStr += isNext() + "ut[\\_\\-]";
            if (simpleupload) hosterStr += isNext() + "su[\\_\\-]";
            if (filefactory) hosterStr += isNext() + "ff[\\_\\-]";
            if (cat) {
                hosterStr += isNext() + "cat\\=[\\d]+";
                hosterStr += isNext() + "p\\=[\\d]+";
            }

            hosterStr += ")";
        } else {
            hosterStr += "not";
        }
        // http://links.serienjunkies.org/f-3bd58945ab43eae0/Episode%2006.html
        Matcher matcher = Pattern.compile("http://.{0,10}serienjunkies\\.org.*" + hosterStr + ".*", Pattern.CASE_INSENSITIVE).matcher(data);
        if (matcher.find()) {
            return true;
        } else {
            String[] links = new Regex(data, "http://.{3,10}\\.serienjunkies.org/.*", Pattern.CASE_INSENSITIVE).getMatches(0);
            for (int i = 0; i < links.length; i++) {
                if (!links[i].matches("(?i).*http://.{3,10}\\.serienjunkies.org/.*(rc[\\_\\-]|rs[\\_\\-]|nl[\\_\\-]|ut[\\_\\-]|su[\\_\\-]|ff[\\_\\-]|cat\\=[\\d]+|p\\=[\\d]+).*")) return true;
            }
        }
        return false;
    }

    @Override
    public Vector<String> getDecryptableLinks(String data) {
        String[] links = new Regex(data, "http://.*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es)[^\"]*", Pattern.CASE_INSENSITIVE).getMatches(0);
        Vector<String> ret = new Vector<String>();
        scatChecked = true;
        for (int i = 0; i < links.length; i++) {
            if (canHandle(links[i])) ret.add(links[i]);
        }
        return ret;
    }

    @Override
    public String cutMatches(String data) {
        return data.replaceAll("(?i)http://.*?(serienjunkies\\.org|85\\.17\\.177\\.195|serienjunki\\.es).*", "--CUT--");
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        switch (step.getStep()) {
        case PluginStep.STEP_DECRYPT:
            request.redirect = true;
            request.withHtmlCode = false;
            request.getRequest("http://serienjunkies.org/enter/");
            request.withHtmlCode = true;
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            if (parameter.matches(".*\\?(cat|p)\\=[\\d]+.*")) {
                boolean isP = parameter.contains("/?p=");
                int catst = getSerienJunkiesCat(isP);
                scatChecked = false;
                int cat = Integer.parseInt(parameter.replaceFirst(".*\\?(cat|p)\\=", "").replaceFirst("[^\\d].*", ""));
                if (sCatNewestDownload == catst) {
                    request.getRequest("http://serienjunkies.org/");

                    Pattern pattern = Pattern.compile("<a href=\"http://serienjunkies.org/\\?cat\\=([\\d]+)\">(.*?)</a><br", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(request.getHtmlCode());
                    String name = null;
                    while (matcher.find()) {
                        if (Integer.parseInt(matcher.group(1)) == cat) {
                            name = matcher.group(2).toLowerCase();
                            break;
                        }
                    }
                    if (name == null) return null;
                    request.getRequest(parameter);
                    name += " ";
                    String[] bet = null;
                    while (bet == null) {
                        name = name.substring(0, name.length() - 1);
                        if (name.length() == 0) return null;
                        try {
                            bet = request.getRegexp("<p><strong>(" + name + ".*?)</strong>(.*?)</p>").getMatches()[0];
                        } catch (Exception e) {
                            // TODO: handle exception
                        }

                    }
                    lastHtmlCode = request.getHtmlCode();
                    String[] links = HTMLParser.getHttpLinks(bet[1], request.urlToString());
                    for (int i = 0; i < links.length; i++) {
                        decryptedLinks.add(this.createDownloadlink(links[i]));
                    }

                    step.setParameter(decryptedLinks);
                    return null;
                } else if (catst == sCatGrabb) {

                    String htmlcode = "";
                    if (isP) {
                        request.getRequest(parameter);
                        htmlcode = request.getHtmlCode();
                    } else {
                        request.getRequest("http://serienjunkies.org/?cat=" + cat);
                        htmlcode = request.getHtmlCode();
                        try {
                            int pages = Integer.parseInt(request.getRegexp("<p align=\"center\">  Pages \\(([\\d]+)\\):").getFirstMatch());
                            for (int i = 2; i < pages + 1; i++) {
                                htmlcode += "\n" + request.getRequest("http://serienjunkies.org/?cat=" + cat + "&paged=" + i);
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                    String[] titles = htmlcode.replaceFirst("(?is).*?(<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*>)", "$1").split("<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*?>");
                    for (int g = 0; g < titles.length; g++) {

                        String title = new Regex(titles[g], "([^><]*?)</a>").getFirstMatch();
                        String[] sp = titles[g].split("(?is)<strong>Größe:</strong>[\\s]*");
                        for (int d = 0; d < sp.length; d++) {
                            String size = new Regex(sp[d], "[\\d]+").getFirstMatch();
                            String[][] links = new Regex(sp[d], "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();
                            for (int i = 0; i < links.length; i++) {
                                String[] links2 = HTMLParser.getHttpLinks(links[i][1], parameter);
                                for (int j = 0; j < links2.length; j++) {
                                    if (canHandle(links2[j])) {
                                        if (this.getProperties().getBooleanProperty("USE_DIREKTDECRYPT", false)) {
                                            decryptedLinks.addAll((new jd.plugins.host.Serienjunkies()).getDLinks(links2[j]));
                                        } else {
                                            decryptedLinks.add(createdl(links2[j], new String[] {size, links[i][0], links[i][1], title}));
                                        }

                                    }

                                }
                            }
                        }
                    }

                    step.setParameter(decryptedLinks);
                    return null;
                } else {
                    return null;
                }
            }
            if (this.getProperties().getBooleanProperty("USE_DIREKTDECRYPT", false)) {
                step.setParameter((new jd.plugins.host.Serienjunkies()).getDLinks(parameter));
            } else {


                // if (!parameter
                // .matches("http://serienjunkies.org/(sjsa[fv]e|sa[fv]e)/.*"))
                // {
                String[] info = getLinkName(parameter);

                if (info == null) {
                    request.getRequest("http://serienjunkies.org/?s=" + parameter.replaceFirst(".*/", "").replaceFirst("\\.html?$", "") + "&submit=Suchen");
                    lastHtmlCode = request.getHtmlCode();
                    info = getLinkName(parameter);
                }

                decryptedLinks.add(createdl(parameter, info));
                step.setParameter(decryptedLinks);
            }
        }
        return null;
    }
    private DownloadLink createdl(String parameter, String[] info)
    {
        int size = 100;
        String name = null, linkName = null, title = null;
        String[] mirrors = null;
        if (info != null) {
            name = JDUtilities.htmlDecode(info[1]);
            size = Integer.parseInt(info[0]);
            title = JDUtilities.htmlDecode(info[3]);
            mirrors = getMirrors(parameter, info[2]);
        }
        try {
            linkName = (((title.length() > 10) ? title.substring(0, 10) : title) + "#" + name).replaceAll("\\.", " ").replaceAll("[^\\w \\#]", "").trim() + ".rar";
        } catch (Exception e) {
            // TODO: handle exception
        }
        if (linkName == null || parameter.matches("http://serienjunkies.org/sa[fv]e/.*") || parameter.matches("http://download.serienjunkies.org/..\\-.*")) {
            size = 100;
            linkName = parameter.replaceFirst(".*/..[\\_\\-]", "").replaceFirst("\\.html?", "");
        }
        String hostname = getHostname(parameter);
        DownloadLink dlink = new DownloadLink(this, name, this.getHost(), "http://sjdownload.org/" + hostname + "/" + linkName, true);
        dlink.setProperty("link", parameter);
        dlink.setProperty("mirrors", mirrors);
        if (name != null) {
            dlink.setSourcePluginComment(title + " ::: " + name);
            dlink.setDownloadMax(size * 1024 * 1024);
        }
        dlink.setStatusText(getHostname(parameter));
        return dlink;
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
        if (c == -1) return null;
        for (int i = 0; i < sp.length; i++) {
            String mirror = null;
            try {
                mirror = HTMLParser.getHttpLinks(sp[i], link)[c];
            } catch (Exception e) {
                // TODO: handle exception
            }
            if (mirror != null && !mirror.matches("[\\s]*")) {
                ret.add(mirror);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    private String getHostname(String link) {
        if (link.matches(".*rc[\\_\\-].*"))
            return "RapidshareCom";
        else if (link.matches(".*rs[\\_\\-].*"))
            return "RapidshareDe";
        else if (link.matches(".*nl[\\_\\-].*"))
            return "NetloadIn";
        else if (link.matches(".*ut[\\_\\-].*"))
            return "UploadedTo";
        else if (link.matches(".*su[\\_\\-].*"))
            return "SimpleUploadNet";
        else if (link.matches(".*ff[\\_\\-].*"))
            return "FileFactoryCom";
        else
            return "RapidshareCom";
    }

    private String[] getLinkName(String link) {
        String[] titles = lastHtmlCode.replaceFirst("(?is).*?(<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*>)", "$1").split("<h2><a href=\"http://serienjunkies.org/[^\"]*\" rel=\"bookmark\"[^>]*?>");
        for (int d = 0; d < titles.length; d++) {

            String title = new Regex(titles[d], "([^><]*?)</a>").getFirstMatch();
            String[] sp = titles[d].split("(?is)<strong>Größe:</strong>[\\s]*");
            for (int j = 0; j < sp.length; j++) {
                String size = new Regex(sp[j], "[\\d]+").getFirstMatch();
                String[][] links = new Regex(sp[j].replaceAll("<a href=\"http://vote.serienjunkies.org.*?</a>", ""), "<p><strong>(.*?)</strong>(.*?)</p>").getMatches();

                for (int i = 0; i < links.length; i++) {
                    try {

                        if (links[i][1].contains(link)) return new String[] { size, links[i][0], links[i][1], title };
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
        }

        return null;
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_DIREKTDECRYPT", "Sofort entschlüsseln"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHAREDE", "Rapidshare.de"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SIMLEUPLOAD", "SimpleUpload.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_FILEFACTORY", "FileFactory.com"));
        cfg.setDefaultValue(false);

    }

}
