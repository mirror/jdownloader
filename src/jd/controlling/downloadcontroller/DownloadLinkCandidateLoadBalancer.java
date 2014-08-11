package jd.controlling.downloadcontroller;

import java.util.Comparator;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.ProxyController;

import org.jdownloader.DomainInfo;

public class DownloadLinkCandidateLoadBalancer implements Comparator<AbstractProxySelectorImpl> {

    private final String     pluginHost;
    private final boolean    specialHandling;
    private final DomainInfo domainInfo;

    public DownloadLinkCandidateLoadBalancer(DownloadLinkCandidate candidate) {
        specialHandling = ProxyController.isSpecialPlugin(candidate.getCachedAccount().getPlugin());
        this.pluginHost = candidate.getCachedAccount().getPlugin().getHost();
        domainInfo = candidate.getLink().getDomainInfo();
    }

    public int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    @Override
    public int compare(final AbstractProxySelectorImpl proxy1, final AbstractProxySelectorImpl proxy2) {
        return compare(countActiveDownloads(proxy1), countActiveDownloads(proxy2));
    }

    public int countActiveDownloads(final AbstractProxySelectorImpl proxy) {
        int count = 0;
        if (proxy != null) {
            for (final SingleDownloadController controller : proxy.getSingleDownloadControllers()) {
                if (pluginHost.equals(controller.getDownloadLinkCandidate().getCachedAccount().getPlugin().getHost())) {
                    if (!specialHandling || domainInfo == controller.getDownloadLink().getDomainInfo()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

}
