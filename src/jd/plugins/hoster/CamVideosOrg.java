package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

import org.appwork.utils.StringUtils;

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
        return "https://www." + this.getHost() + "/" + fuid + "/" + urlTitle + "/";
    }
}