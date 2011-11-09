//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantclip.com" }, urls = { "http://(www\\.)?deviantclip\\.com/Media\\-([0-9]+\\-[0-9]+|[0-9]+_).*?\\.html" }, flags = { 0 })
public class DeviantClipComGallery extends PluginForDecrypt {

    public DeviantClipComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String fpName = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">PICTURE GALLERY<") || parameter.matches(".*?deviantclip\\.com/Media-[0-9]+\\-[0-9]+_.*?\\.html")) {
            if (parameter.matches(".*?deviantclip\\.com/Media\\-[0-9]+\\-[0-9]+_.*?\\.html")) {
                getfpName();
                if (fpName != null) {
                    DownloadLink dl = createDownloadlink(parameter.replace("deviantclip.com", "gsghe366REHrtzegiolp") + "---picture");
                    dl.setName(fpName);
                    br.getPage(parameter);
                    String dllink = br.getRegex("\"(http://medias\\.deviantclip\\.com/media/[0-9]+/.*?)\"").getMatch(0);
                    if (dllink != null) {
                        dllink = dllink.replace("amp;", "");
                        URLConnectionAdapter con = br.openGetConnection(dllink);
                        if (!con.getContentType().contains("html")) {
                            String ending = LoadImage.getFileType(dllink, con.getContentType());
                            if (ending != null) dl.setFinalFileName(dl.getName() + ending);
                            long size = con.getLongContentLength();
                            if (size != 0) {
                                dl.setDownloadSize(con.getLongContentLength());
                                dl.setAvailable(true);
                            }
                        }
                    }
                    decryptedLinks.add(dl);
                } else {
                    decryptedLinks.add(createDownloadlink(parameter.replace("deviantclip.com", "gsghe366REHrtzegiolp") + "---picture"));
                }
            } else {
                getfpName();
                String[] links = br.getRegex("(http://www\\.deviantclip\\.com/Media\\-[0-9]+\\-[0-9]+_.*?\\.html)").getColumn(0);
                if (links == null || links.length == 0) return null;
                progress.setRange(links.length);
                if (fpName != null) {
                    int counter = 1;
                    for (String photolink : links) {
                        DownloadLink dl = createDownloadlink(photolink.replace("deviantclip.com", "gsghe366REHrtzegiolp") + "---picture");
                        dl.setName(fpName + "_" + counter);
                        br.getPage(photolink);
                        String dllink = br.getRegex("\"(http://medias\\.deviantclip\\.com/media/[0-9]+/.*?)\"").getMatch(0);
                        if (dllink != null) {
                            dllink = dllink.replace("amp;", "");
                            URLConnectionAdapter con = br.openGetConnection(dllink);
                            if (!con.getContentType().contains("html")) {
                                String ending = LoadImage.getFileType(dllink, con.getContentType());
                                if (ending != null) dl.setFinalFileName(dl.getName() + ending);
                                long size = con.getLongContentLength();
                                if (size != 0) {
                                    dl.setDownloadSize(con.getLongContentLength());
                                    dl.setAvailable(true);
                                }
                            }
                        }
                        decryptedLinks.add(dl);
                        counter = counter + 1;
                        progress.increase(1);
                    }
                } else {
                    for (String photolink : links) {
                        decryptedLinks.add(createDownloadlink(photolink.replace("deviantclip.com", "gsghe366REHrtzegiolp") + "---picture"));
                    }
                }
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }
            }
        } else {
            getfpName();
            if (fpName != null) {
                DownloadLink dl = createDownloadlink(parameter.replace("deviantclip.com", "gsghe366REHrtzegiolp") + "---video");
                dl.setFinalFileName(fpName + ".flv");
                decryptedLinks.add(dl);
            } else {
                decryptedLinks.add(createDownloadlink(parameter.replace("deviantclip.com", "gsghe366REHrtzegiolp") + "---video"));
            }
        }
        return decryptedLinks;
    }

    public void getfpName() throws NumberFormatException, PluginException {
        fpName = br.getRegex("<li class=\"text\"><h1>(.*?)</h1></li>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("title:\\'(.*?)\\'").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("class=\"main\\-sectioncontent\"><p class=\"footer\">.*?<b>(.*?)</b>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("name=\"DC\\.title\" content=\"(.*?)\">").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
                    }
                }
            }
        }
    }
}
