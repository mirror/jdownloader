package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javqd.tv" }, urls = { "https?://(?:www\\.)?javqd\\.tv/movie/([^/]+)(.html?)?" })
public class JavQD extends PluginForDecrypt {
    public JavQD(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl());
        br.followRedirect();
        final String filename = br.getRegex("<h2 class=([\"\']?)info-box-heading\\1>([^<>]+)</h2>").getMatch(1);
        final String player = br.getRegex("<iframe id=\"avcms_player\" width=\"[0-9]+\" height=\"[0-9]+\" src=\"([^\"\']+)\" frameborder=\"0\"(?: allowfullscreen)?>").getMatch(0);
        String data = br.cloneBrowser().getPage("https://javqd.tv" + player);
        /*
         * String data = br.getRequest().getHtmlCode(); if (!br.getRedirectLocation().isEmpty()) { data = br.followRedirect(); }//
         */
        // String data = br.getPage(con.getRequest());
        final String filename2 = new Regex(data, "<meta name=\"title\" content=\"([^\"\'<>]+)\">").getMatch(0);
        final String data_links[] = new Regex(data, "<li data-status=([\"\']?)([0-9]+)\\1 data-video=([\"\']?)([^\\\"\\']+)\\3(?: class=([\\\"\\']?)active\\5)?>\\s*([^</]+)\\s*</li>").getColumn(3);
        if (data_links == null | data_links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int index = 0;
        for (String data_link : data_links) {
            logger.info("data_links: " + data_link);
            if (data_link.equals("post")) {
                continue;
            }
            final String name;
            if (data_links.length > 1) {
                name = filename + "_" + (char) ('a' + index);
            } else {
                name = filename;
            }
            final DownloadLink downloadLink;
            if (StringUtils.isEmpty(name)) {
                downloadLink = createDownloadlink(data_link);
            } else {
                downloadLink = createDownloadlink(data_link + "#javclName=" + HexFormatter.byteArrayToHex(name.getBytes("UTF-8")));
            }
            ret.add(downloadLink);
            index++;
        }
        return ret;
    }
}
