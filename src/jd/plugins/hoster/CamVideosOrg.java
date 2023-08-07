package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

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
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        return generateContentURLDefaultVideosPattern(host, fuid, urlTitle);
    }

    @Override
    protected boolean useEmbedWorkaround() {
        /**
         * 2022-04-12: A lot of videos are not embeddable but have already been embedded by other websites in the past so users keep adding
         * such URLs. </br>
         * Website shows error "You are not allowed to watch this video.". </br>
         * This workaround will fix that issue by trying to access the non-embed URL.
         */
        return true;
    }
    // @Override
    // protected String regexNormalTitleWebsite(final Browser br) {
    // return super.regexNormalTitleWebsite(br);
    // }

    @Override
    protected boolean preferTitleHTML() {
        /* 2023-08-07: Plenty of their URLs do not contain usable titles for filenames -> Prefer titles from HTML. */
        return true;
    }
}