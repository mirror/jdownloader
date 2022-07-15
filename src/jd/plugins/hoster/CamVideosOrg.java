package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamVideosOrg extends KernelVideoSharingComV2 {
    public CamVideosOrg(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.camvideos.org/");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "camvideos.org" });
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
    protected boolean useEmbedWorkaround() {
        /**
         * 2022-04-12: A lot of videos are not embeddable but have already been embedded by other websites. </br>
         * Website shows error "You are not allowed to watch this video.". </br>
         * This workaround will fix that issue.
         */
        return true;
    }
}