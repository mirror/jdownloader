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

import java.io.File;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

//http://save.raidrush.ws/?id=8b891e864bc42ffa7bfcdaf72503f2a0
//http://save.raidrush.ws/?id=e7ccb3ee67daff310402e5e629ab8a91
//http://save.raidrush.ws/?id=c17ce92bc6154713f66b151b8f55684

public class RaidrushOrg extends PluginForDecrypt {

    static private final String host = "save.raidrush.ws";

    private String version = "0.1";
    // http://raidrush.org/ext/?fid=200634
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?raidrush\\.org/ext/\\?fid\\=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    // private Pattern patternCount = Pattern.compile("\',\'FREE\',\'");

    public RaidrushOrg() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Raidrush.org" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {

        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();

            try {

                Browser br = new Browser();
                String page = br.getPage(parameter);
                String title = new Regex(page, "<big><strong>(.*?)</strong></big>").getFirstMatch();
                String pass = new Regex(page, "<strong>Passwort\\:</strong> <small>(.*?)</small>").getFirstMatch();
                FilePackage fp = new FilePackage();
                Vector<String> passes = new Vector<String>();
                passes.add(pass);
                fp.setName(title);
                fp.setPassword(pass);

                String[][] matches = new Regex(page, "ddl\\(\\'(.*?)\\'\\,\\'([\\d]*?)\\'\\)").getMatches();
                this.progress.setRange(matches.length);
                for (String[] match : matches) {

                    // match[0]= (JDUtilities.Base64Decode(match[0]);

                    String page2 = br.getPage("http://raidrush.org/ext/exdl.php?go=" + match[0] + "&fid=" + match[1]);
                    String link = new Regex(page2, "unescape\\(\"(.*?)\"\\)").getFirstMatch();

                    link = JDUtilities.htmlDecode(link);
                    link = new Regex(link, "\"0\"><frame src\\=\"(.*?)\" name\\=\"GO_SAVE\"").getFirstMatch();
                    DownloadLink dl = this.createDownloadlink(link);
                    dl.setSourcePluginPasswords(passes);
                    dl.setFilePackage(fp);
                    decryptedLinks.add(dl);

                    progress.increase(1);
                }
                step.setParameter(decryptedLinks);
                return step;

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return null;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}