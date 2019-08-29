//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "4shared.com" }, urls = { "https?://(?:www\\.)?4shared(?:\\-china)?\\.com/(?:dir|folder|minifolder)/[A-Za-z0-9\\-_]+/(?:\\d+/)?[A-Za-z0-9\\-_]+" })
public class FourSharedComFolder extends PluginForDecrypt {
    public FourSharedComFolder(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String                  sid                         = null;
    private String                  host                        = null;
    private String                  uid                         = null;
    private String                  folderID                    = null;
    private String                  foldername                  = null;
    private String                  parameter                   = null;
    private String                  pass                        = null;
    private Browser                 br2                         = new Browser();
    private ArrayList<DownloadLink> decryptedLinks              = new ArrayList<DownloadLink>();
    private String                  type_folder_with_pagenumber = "https?://(?:www\\.)?4shared(?:\\-china)?\\.com/(?:dir|folder|minifolder)/[A-Za-z0-9\\-_]+/\\d+/[A-Za-z0-9\\-_]+";

    /**
     * TODO: Implement API: http://www.4shared.com/developer/ 19.12.12: Their support never responded so we don't know how to use the API...
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        parameter = param.toString();
        if (param.toString().matches(type_folder_with_pagenumber)) {
            /* Remove pagenumber from added URL - important! */
            final String pagenumber = new Regex(parameter, "\\.com/[^/]+/[A-Za-z0-9\\-_]+/(\\d+/)[A-Za-z0-9\\-_]+").getMatch(0);
            parameter = parameter.replace(pagenumber, "");
        }
        final boolean crawlFolderNew = true;
        host = new Regex(parameter, "(https?://[^/]+)").getMatch(0);
        uid = new Regex(parameter, "\\.com/(dir|folder|minifolder)/(.+)").getMatch(1);
        folderID = new Regex(parameter, "\\.com/(?:dir|folder|minifolder)/([^/]+)").getMatch(0);
        foldername = new Regex(parameter, "\\.com/(?:dir|folder|minifolder)/[^/]+/([^/]+)").getMatch(0);
        if (!crawlFolderNew) {
            parameter = new Regex(parameter, "(https?://(?:www\\.)?4shared(?:\\-china)?\\.com/)").getMatch(0);
            parameter = parameter + "folder/" + uid;
        }
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "4langcookie", "en");
        // check the folder/ page for password stuff and validity of url
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("The file link that you requested is not valid") || br.containsHTML("This folder was deleted") || br.containsHTML("This folder is no longer available because of a claim")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "shared(\\-china)?\\.com/(dir|folder|minifolder)/(.+)").getMatch(2));
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (br.containsHTML(">You need owner\\'s permission to access this folder")) {
            logger.info("Link offline (no permissions): " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "shared(\\-china)?\\.com/(dir|folder|minifolder)/(.+)").getMatch(2));
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (br.containsHTML("class=\"emptyFolderPlaceholder\"")) {
            logger.info("Link offline (empty): " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "shared(\\-china)?\\.com/(dir|folder|minifolder)/(.+)").getMatch(2));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        /* Important: Make sure this check is language independant! */
        if (folderNeedsPassword()) {
            /*
             * 2019-08-30: Check if password protected folders still exist. At least via free account I was not able to password-protect a
             * self created folder!!
             */
            Form form = br.getFormbyProperty("name", "theForm");
            if (form == null) {
                form = new Form();
                form.setMethod(MethodType.POST);
                form.put("dirId", folderID);
            }
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            pass = this.getPluginConfig().getStringProperty("lastusedpassword");
            for (int retry = 5; retry > 0; retry--) {
                if (pass == null) {
                    pass = Plugin.getUserInput(null, param);
                    if (pass == null || pass.equals("")) {
                        logger.info("User abored/entered blank password");
                        return decryptedLinks;
                    }
                }
                form.put("ppfPassword", pass);
                br.submitForm(form);
                if (!folderNeedsPassword()) {
                    this.getPluginConfig().setProperty("lastusedpassword", pass);
                    this.getPluginConfig().save();
                    break;
                } else {
                    this.getPluginConfig().setProperty("lastusedpassword", Property.NULL);
                    pass = null;
                    if (retry == 1) {
                        logger.severe("Wrong Password!");
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                }
            }
        }
        if (crawlFolderNew) {
            this.crawlFolderNew();
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?)\\- 4shared</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>4shared folder \\- (.*?)[\r\n\t ]+</title>").getMatch(0);
        }
        if (fpName == null) {
            fpName = br.getRegex("<h1 id=\"folderNameText\">(.*?)[\r\n\t ]+<h1>").getMatch(0);
        }
        if (fpName == null) {
            fpName = "4Shared - Folder";
        }
        sid = br.getRegex("sId:'([a-zA-Z0-9]+)',").getMatch(0);
        if (sid == null) {
            sid = br.getRegex("<input type=\"hidden\" name=\"sId\" value=\"([a-zA-Z0-9]+)\"").getMatch(0);
        }
        if (sid != null) {
            parsePage("0");
            parseNextPage();
        } else {
            int pagemax = 1;
            final String[] pageLinks = this.br.getRegex("<a href=\"(/folder/[^<>\"]*?)\"").getColumn(0);
            if (pageLinks != null && pageLinks.length > 0) {
                for (String aPage : pageLinks) {
                    final String page_str_temp = new Regex(aPage, "/folder/[^/]+/(\\d+)/[^/]+\\.html").getMatch(0);
                    if (page_str_temp == null) {
                        continue;
                    }
                    final int pagetmp = Integer.parseInt(page_str_temp);
                    if (pagetmp > pagemax) {
                        pagemax = pagetmp;
                    }
                }
            }
            for (int pagecounter = 1; pagecounter <= pagemax; pagecounter++) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                if (pagecounter > 1) {
                    logger.info("Decrypting page " + pagecounter + " of " + pagemax);
                    br.getPage("http://www.4shared.com/folder/" + this.folderID + "/" + pagecounter + "/" + this.foldername + ".html?detailView=false&sortAsc=true&sortsMode=NAME");
                }
                final String subfolder_html = br.getRegex("id=\"folderContent\"(.*?)class=\"simplePagerAndUpload\"").getMatch(0);
                String[] linkInfo = br.getRegex("<tr align=\"center\">(.*?)</tr>").getColumn(0);
                if (linkInfo == null || linkInfo.length == 0) {
                    /* E.g. video */
                    linkInfo = br.getRegex("<div class=\"jsThumbPreview simpleTumbPreviewWrapper\\s*\">(.*?)</div>[\t\n\r ]*?</div>").getColumn(0);
                }
                if ((linkInfo == null || linkInfo.length == 0) && subfolder_html == null) {
                    /*
                     * Do NOT return null here -- errorhandling is in the code below. This place right here might as well be reached when
                     * the "pagemax" parser fails and returns bad values --> We try to access a folder page which does not exist.
                     */
                    break;
                }
                if (linkInfo != null && linkInfo.length > 0) {
                    for (final String singleInfo : linkInfo) {
                        final String dlink = new Regex(singleInfo, "\"(https?://(www\\.)?4shared(\\-china)?\\.com/[^<>\"]*?)\"").getMatch(0);
                        String filename = new Regex(singleInfo, "data\\-element=\"72\">([^<>\"]*?)<").getMatch(0);
                        if (filename == null) {
                            /* E.g. video */
                            filename = new Regex(singleInfo, "class=\"bluelink\" target=\"_blank\">([^<>\"]*?)<").getMatch(0);
                        }
                        final String filesize = new Regex(singleInfo, "<div class=\"itemSizeInfo\">([^<>\"]*?)</div>").getMatch(0);
                        if (dlink == null || filename == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        final DownloadLink fina = createDownloadlink(dlink);
                        fina.setName(Encoding.htmlDecode(filename.trim()));
                        fina.setProperty("decrypterfilename", filename);
                        if (filesize != null) {
                            fina.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize.trim()).replace(",", "")));
                        }
                        /* 2017-03-22: Do NOT set available true because status is not known! */
                        // fina.setAvailable(true);
                        decryptedLinks.add(fina);
                        distribute(fina);
                    }
                }
                if (subfolder_html != null) {
                    final String[] subfolders = new Regex(subfolder_html, "(/folder/[^<>\"]*?)\"").getColumn(0);
                    if (subfolders != null && subfolders.length > 0) {
                        for (String aSubfolder : subfolders) {
                            if (!parameter.contains(aSubfolder)) {
                                aSubfolder = "http://www." + this.br.getHost() + aSubfolder;
                                decryptedLinks.add(this.createDownloadlink(aSubfolder));
                            }
                        }
                    }
                }
                if (folderID == null || foldername == null) {
                    /* Emergency exit */
                    break;
                }
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Possible empty folder, or plugin out of date for link: " + parameter);
            return null;
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private boolean folderNeedsPassword() {
        return br.containsHTML("jsCheckAndStoreFolderPassword");
    }

    /** 2019-08-29: New */
    private void crawlFolderNew() throws Exception {
        br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept", "text/html, */*; q=0.01");
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String currentDirID = null;
        if (this.parameter.matches(".+/(folder|minifolder)/\\d+.*")) {
            /* Old type without the new required ID --> Change to new type, access URL and find ID in html code */
            br.getPage(parameter.replaceAll("/(folder|minifolder)/", "/dir/"));
        } else if (this.parameter.matches(".+/(folder|minifolder)/.*")) {
            currentDirID = new Regex(parameter, "/(?:folder|minifolder)/([^/]+)").getMatch(0);
        }
        if (currentDirID == null) {
            currentDirID = br.getRegex("var currentDirId\\s*=\\s*'([^<>\"']+)';").getMatch(0);
        }
        if (StringUtils.isEmpty(currentDirID)) {
            return;
        }
        /*
         * 2019-08-30: Seems like there is no pagination at all and rthis request will always return ALL objects of one folder no matter how
         * many that are. Tested up to 300 objects.
         */
        br2.postPage("https://www." + this.getHost() + "/web/accountActions/changeDir", "dirId=" + currentDirID);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br2.toString());
        final String curdirName = (String) entries.get("curdirName");
        FilePackage fp = null;
        if (!StringUtils.isEmpty(curdirName)) {
            fp = FilePackage.getInstance();
            fp.setName(curdirName);
        }
        String subFolderPath = getAdoptedCloudFolderStructure();
        if (subFolderPath == null) {
            subFolderPath = curdirName;
        }
        entries = (LinkedHashMap<String, Object>) entries.get("info");
        final ArrayList<Object> dirs = (ArrayList<Object>) entries.get("dirs");
        final ArrayList<Object> files = (ArrayList<Object>) entries.get("files");
        for (final Object fileO : files) {
            entries = (LinkedHashMap<String, Object>) fileO;
            final String fileid = (String) entries.get("id");
            if (StringUtils.isEmpty(fileid)) {
                continue;
            }
            String filename = (String) entries.get("name");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            final String contentURL = "https://www." + this.getHost() + "/file/" + fileid + "/" + URLEncode.encodeURIComponent(filename) + ".html";
            final DownloadLink dl = createDownloadlink(contentURL);
            /*
             * 2019-08-30: This is actually not true - files inside folders crawler this way can also be offline but crawling them would
             * take very long so let's set them online here anyways.
             */
            dl.setAvailable(true);
            if (!StringUtils.isEmpty(filename)) {
                filename = Encoding.htmlDecode(filename);
                dl.setName(filename);
                /* Filenames are hard to parse via website --> By setting them here we can be sure to always have good filenames! */
                dl.setProperty("decrypterfilename", filename);
            }
            if (filesize > 0) {
                dl.setDownloadSize(filesize);
            }
            if (!StringUtils.isEmpty(subFolderPath)) {
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolderPath);
            }
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            decryptedLinks.add(dl);
        }
        for (final Object dirO : dirs) {
            entries = (LinkedHashMap<String, Object>) dirO;
            // final long foldersize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            final String folderid = (String) entries.get("id");
            String foldername = (String) entries.get("name");
            final String contentURL = "https://www." + this.getHost() + "/folder/" + folderid + "/" + URLEncode.encodeURIComponent(foldername) + ".html";
            final DownloadLink dl = createDownloadlink(contentURL);
            if (!StringUtils.isEmpty(foldername)) {
                foldername = Encoding.htmlDecode(foldername);
                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolderPath + "/" + foldername);
            }
            decryptedLinks.add(dl);
        }
    }

    private void parsePage(final String offset) throws Exception {
        br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept", "text/html, */*; q=0.01");
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String ran = Long.toString(new Random().nextLong()).substring(1, 11);
        br2.getPage(host + "/pageDownload1/folderContent.jsp?ajax=true&sId=" + sid + "&firstFileToShow=" + offset + "&rnd=" + ran);
        String[] filter = br2.getRegex("class=\"fnameCont\">(.*?)</td>").getColumn(0);
        if (filter == null || filter.length == 0) {
            filter = br2.getRegex("class=\"simpleTumbItem\">.*?</div>[\r\n\t ]+(<a.*?)</div>[\r\n\t ]+</div>").getColumn(0);
        }
        if (filter == null || filter.length == 0) {
            logger.warning("Couldn't filter 'folderContent'");
            if (decryptedLinks.size() > 0) {
                logger.info("Possible empty page or last page");
            } else {
                logger.warning("Possible error");
            }
            return;
        }
        if (filter != null && filter.length > 0) {
            for (final String entry : filter) {
                // sync folders share same uid but have ?sID=UID at the end, but this is done by JS from the main /folder/uid page...
                String subDir = new Regex(entry, "\"(https?://(www\\.)?4shared(\\-china)?\\.com/(dir|folder)/[^\"' ]+/[^\"' ]+(\\?sID=[a-zA-z0-9]{16})?)\"").getMatch(0);
                // prevent the UID from showing up in another url format structure
                if (subDir != null) {
                    if (subDir.contains("?sID=") || !new Regex(subDir, "\\.com/(folder|dir)/([^/]+)").getMatch(1).equals(uid)) {
                        decryptedLinks.add(createDownloadlink(subDir));
                    }
                } else {
                    final String dllink = new Regex(entry, "\"(http[^\"]+4shared(\\-china)?\\.com/(?!folder/|dir/)[^\"]+\\.html)").getMatch(0);
                    if (dllink == null) {
                        // logger.warning("Couldn't find dllink!");
                        continue;
                    }
                    final DownloadLink dlink = createDownloadlink(dllink);
                    if (pass != null && pass.length() != 0) {
                        dlink.setProperty("pass", pass);
                    }
                    String fileName = new Regex(entry, "title=\"(.*?)\"").getMatch(0);
                    if (fileName != null) {
                        dlink.setName(Encoding.htmlDecode(fileName));
                    }
                    dlink.setAvailable(true);
                    decryptedLinks.add(dlink);
                }
            }
        }
    }

    private boolean parseNextPage() throws Exception {
        String offset = br2.getRegex("id=\"pagerItemNext\" onclick=\"FolderActions.goToPage\\((\\d+)\\)").getMatch(0);
        if (offset != null) {
            parsePage(offset);
            parseNextPage();
            return true;
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}