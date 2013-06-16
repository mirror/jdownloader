//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "zomgupload.com" }, urls = { "http://(www\\.)?zomgupload\\.com/[a-z0-9]{12}\\.html" }, flags = { 0 })
public class ZomgUploadCom extends PluginForDecrypt {

    public ZomgUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookie("http://zomgupload.com", "lang", "english");
        br.getPage(parameter);
        if (br.containsHTML(">This folder is empty<")) {
            logger.info("Link offline (folder empty): " + parameter);
            return decryptedLinks;
        }

        Form download1 = getFormByKey("op", "download1");
        if (download1 != null) {
            // stable is lame, issue finding input data fields correctly. eg. closes at ' quotation mark - remove when jd2 goes stable!
            download1 = cleanForm(download1);
            // end of backward compatibility
            download1.remove("method_premium");
            br.submitForm(download1);
        }

        Form dlForm = getFormByKey("op", "download2");
        if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final long timeBefore = System.currentTimeMillis();

        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /** Ticket Time */
        String ttt = new Regex(br, "id=\"countdown_str\">[^<>\"]+<span id=\"[^<>\"]+\"( class=\"[^<>\"]+\")?>([\n ]+)?(\\d+)([\n ]+)?</span>").getMatch(2);
        if (ttt == null) ttt = new Regex(br, "id=\"countdown_str\"[^>]+>Wait[^>]+>(\\d+)\\s?+</span>").getMatch(0);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            tt -= passedTime;
            logger.info("Waittime detected, waiting " + ttt + " - " + passedTime + " seconds from now on...");
            if (tt > 0) Thread.sleep(tt * 1000l);
        }

        logger.info("Submitted DLForm");
        br.submitForm(dlForm);
        String dl = br.getRegex("<a href=\"(http[^\"]+)\"><img[^>]+/images/free_dl\\.png").getMatch(0);
        if (dl != null)
            decryptedLinks.add(createDownloadlink(dl));
        else
            return null;

        return decryptedLinks;
    }

    /**
     * If form contain both " and ' quotation marks within input fields it can return null values, thus you submit wrong/incorrect data re:
     * InputField parse(final String data). Affects revision 19688 and earlier!
     * 
     * TODO: remove after JD2 goes stable!
     * 
     * @author raztoki
     * */
    private Form cleanForm(Form form) {
        if (form == null) return null;
        String data = form.getHtmlCode();
        ArrayList<String> cleanupRegex = new ArrayList<String>();
        cleanupRegex.add("(\\w+\\s*=\\s*\"[^\"]+\")");
        cleanupRegex.add("(\\w+\\s*=\\s*'[^']+')");
        for (String reg : cleanupRegex) {
            String results[] = new Regex(data, reg).getColumn(0);
            if (results != null) {
                String quote = new Regex(reg, "(\"|')").getMatch(0);
                for (String result : results) {
                    String cleanedResult = result.replaceFirst(quote, "\\\"").replaceFirst(quote + "$", "\\\"");
                    data = data.replace(result, cleanedResult);
                }
            }
        }
        return new Form(data);
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     * 
     * @param key
     * @param value
     * @return
     * */
    private Form getFormByKey(final String key, final String value) {
        Form[] workaround = br.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                for (InputField field : f.getInputFields()) {
                    if (key != null && key.equals(field.getKey())) {
                        if (value == null && field.getValue() == null) return f;
                        if (value != null && value.equals(field.getValue())) return f;
                    }
                }
            }
        }
        return null;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}