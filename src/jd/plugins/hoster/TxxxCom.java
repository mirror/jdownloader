//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TxxxCom extends KernelVideoSharingComV2 {
    public TxxxCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "txxx.com", "tubecup.com", "videotxxx.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return KernelVideoSharingComV2.buildAnnotationUrlsDefaultVideosPattern(getPluginDomains());
    }

    @Override
    protected String generateContentURL(final String fuid, final String urlTitle) {
        if (StringUtils.isEmpty(fuid) || StringUtils.isEmpty(urlTitle)) {
            return null;
        }
        return "https://www." + this.getHost() + "/videos/" + fuid + "/" + urlTitle + "/";
    }

    @Override
    protected AvailableStatus requestFileInformation(final DownloadLink link, Account account, final boolean isDownload) throws Exception {
        dllink = null;
        prepBR(this.br);
        final String weakFilename = getWeakFilename(link);
        if (!link.isNameSet() && weakFilename != null) {
            /* Set this so that offline items have "nice" titles too. */
            link.setName(weakFilename);
        }
        if (account == null) {
            /* Was not called in download mode -> Try to grab any valid account */
            account = AccountController.getInstance().getValidAccount(this.getHost());
        }
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String lifetime = PluginJSonUtils.getJson(br, "lifetime");
        final Browser brAPI = br.cloneBrowser();
        final String param1 = this.getFUID(link).substring(0, 2) + "000000";
        getPage(brAPI, "https://" + this.getHost() + "/api/json/video/" + lifetime + "/" + param1 + "/16421000/" + this.getFUID(link) + ".json");
        if (brAPI.getHttpConnection().getResponseCode() != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(brAPI.toString(), TypeRef.HASHMAP);
        final Object videoO = root.get("video");
        if (videoO == null) {
            /*
             * E.g.
             * {"sql":"SELECT v.video_id, v.status_id, v.server_group_id sg_id, v.is_private, v.dir,v.title,vt.title_ru,vt.title_in,vt.title_de,vt.title_fr,vt.title_it,vt.title_jp,vt.title_es, v.description, v.post_date, v.duration, v.screen_main, v.rating, v.rating_amount, v.video_viewed, v.comments_count, v.start_pos,v.custom3,\n\tu.display_name user_display_name,u.avatar user_avatar,u.subscribers_count user_subscribers_count,u.user_id user_user_id,\n\tIFNULL(GROUP_CONCAT(DISTINCT CONCAT(c.dir,'|',c.category_id,'|',c.title,'|',c.category_group_id)),'') categories_data,\n\tIFNULL(GROUP_CONCAT(DISTINCT CONCAT(m.dir,'|',m.model_id,'|',m.title,'|',m.gender_id,'|',m.subscribers_count,'|',m.total_videos,'|',m.screenshot1)),'') models_data,\n\tIF(cs.custom10 = '',cs.title, cs.custom10)cs_title, IFNULL(cs.custom_file3,'')cs_img, IFNULL(cs.custom_file7,'')cs_banner_img,\n\tIFNULL(cs.dir,'')cs_dir, IFNULL(cs.content_source_id,'')cs_id, IFNULL(cs.subscribers_count,'')cs_subs, IFNULL(cs.url,'')cs_url, IFNULL(cs.total_videos, 0)cs_total_videos,\n\tIFNULL(GROUP_CONCAT(DISTINCT CONCAT(t.tag_dir,'|',t.tag_id,'|',t.tag)),'') tags_data, IFNULL(csg.title,'')csg_title, IFNULL(csg.dir,'')csg_dir, IFNULL(dvd.dvd_id,'')source_id, IFNULL(dvd.title,'')source_title, IFNULL(dvd.dir,'')source_dir\n\tFROM ktvs_videos v\n\tLEFT JOIN ktvs_videos_titles vt USING(video_id)\n\tLEFT JOIN ktvs_users u USING(user_id)\n\tLEFT JOIN ktvs_categories_videos cv USING(video_id)\n\tLEFT JOIN ktvs_categories c USING(category_id)\n\tLEFT JOIN ktvs_models_videos mv USING(video_id)\n\tLEFT JOIN ktvs_models m USING(model_id)\n\tLEFT JOIN ktvs_content_sources cs USING(content_source_id)\n\tLEFT JOIN ktvs_content_sources_groups csg ON cs.content_source_group_id=csg.content_source_group_id\n\tLEFT JOIN ktvs_dvds dvd USING(dvd_id)\n\tLEFT JOIN ktvs_tags_videos tv USING(video_id)\n\tLEFT JOIN ktvs_tags t USING(tag_id)\n\tWHERE v.status_id IN (1,5) AND v.post_date<='2021-08-09 14:35:21' AND v.video_id = 4731307\n\tGROUP BY v.video_id"
             * ,"error":1,"code":"video_not_found"}
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> video = (Map<String, Object>) videoO;
        String finalFilename = getFileTitle(link);
        if (!StringUtils.isEmpty(finalFilename)) {
            link.setFinalFileName(finalFilename + ".mp4");
        }
        /* Handling below eliminates the need of that. */
        // setSpecialFlags();
        final long is_private = JavaScriptEngineFactory.toLong(video.get("is_private"), 0);
        if (is_private == 1) {
            link.setProperty(PROPERTY_IS_PRIVATE_VIDEO, true);
            return AvailableStatus.TRUE;
        } else {
            link.setProperty(PROPERTY_IS_PRIVATE_VIDEO, false);
        }
        /* Only look for downloadurl if we need it! */
        if (isDownload || !this.enableFastLinkcheck()) {
            try {
                dllink = getDllink(this.br);
            } catch (final PluginException e) {
                if (this.isPrivateVideo(link) && e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                    logger.info("ERROR_FILE_NOT_FOUND in getDllink but we have a private video so it is not offline ...");
                } else {
                    throw e;
                }
            }
        }
        if (!StringUtils.isEmpty(this.dllink) && !dllink.contains(".m3u8") && !isDownload && !enableFastLinkcheck()) {
            URLConnectionAdapter con = null;
            try {
                // if you don't do this then referrer is fked for the download! -raztoki
                final Browser brc = this.br.cloneBrowser();
                brc.setAllowedResponseCodes(new int[] { 405 });
                // In case the link redirects to the finallink -
                // br.getHeaders().put("Accept-Encoding", "identity");
                con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(dllink));
                final String workaroundURL = getHttpServerErrorWorkaroundURL(con);
                if (workaroundURL != null) {
                    con.disconnect();
                    con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(workaroundURL));
                }
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setDownloadSize(con.getCompleteContentLength());
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    final String redirect_url = brc.getHttpConnection().getRequest().getUrl();
                    if (redirect_url != null) {
                        dllink = redirect_url;
                        logger.info("dllink: " + dllink);
                    }
                    if (StringUtils.isEmpty(finalFilename)) {
                        /* Fallback - attempt to find final filename */
                        final String filenameFromFinalDownloadurl = Plugin.getFileNameFromURL(con.getURL());
                        if (con.isContentDisposition()) {
                            logger.info("Using final filename from content disposition header");
                            finalFilename = Plugin.getFileNameFromHeader(con);
                            link.setFinalFileName(finalFilename);
                        } else if (!StringUtils.isEmpty(filenameFromFinalDownloadurl)) {
                            logger.info("Using final filename from inside final downloadurl");
                            finalFilename = filenameFromFinalDownloadurl;
                            link.setFinalFileName(finalFilename);
                        } else {
                            logger.info("Failed to find any final filename so far");
                        }
                    }
                } else {
                    try {
                        brc.followConnection(true);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    this.errorNoFile();
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (StringUtils.isEmpty(finalFilename)) {
            /* Last chance fallback */
            logger.info("Looking for last chance final filename --> Trying to use FUID as filename");
            if (this.getFUID(link) != null) {
                logger.info("Using fuid as filename");
                finalFilename = this.getFUID(link) + ".mp4";
                link.setFinalFileName(finalFilename);
            } else {
                logger.warning("Failed to find any filename!");
            }
        }
        return AvailableStatus.TRUE;
    }
    // @Override
    // protected boolean useAPIAvailablecheck() {
    // return true;
    // }
    //
    // @Override
    // protected boolean useAPIGetDllink() {
    // return true;
    // }
}