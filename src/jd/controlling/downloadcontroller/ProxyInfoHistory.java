package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;
import jd.controlling.proxy.ProxyInfo;
import jd.plugins.Account;

import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.WaitWhileWaitingSkipReasonIsSet;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;

public class ProxyInfoHistory {

    public static final class WaitingSkipReasonContainer {
        private final WaitingSkipReason         skipReason;
        private WaitWhileWaitingSkipReasonIsSet waitWhile = null;
        private final String                    host;
        private final ProxyInfo                 proxyInfo;

        private WaitingSkipReasonContainer(WaitingSkipReason skipReason, DownloadLinkCandidate candidate) {
            this.skipReason = skipReason;
            this.host = candidate.getCachedAccount().getPlugin().getHost();
            this.proxyInfo = candidate.getProxy();
        }

        public final void invalidate() {
            skipReason.invalidate();
        }

        public final String getHost() {
            return host;
        }

        public final WaitingSkipReason getWaitingSkipReason() {
            return skipReason;
        }

        public final ProxyInfo getProxyInfo() {
            return proxyInfo;
        }
    }

    private final Map<WaitingSkipReason.CAUSE, Map<ProxyInfo, List<WaitingSkipReasonContainer>>> causeMapping = new LinkedHashMap<WaitingSkipReason.CAUSE, Map<ProxyInfo, List<WaitingSkipReasonContainer>>>();

    private final Map<ProxyInfo, Map<String, Map<Account, List<WaitingSkipReasonContainer>>>>    history      = new LinkedHashMap<ProxyInfo, Map<String, Map<Account, List<WaitingSkipReasonContainer>>>>();

    private List<WaitingSkipReasonContainer> getInteralWaitingSkipReasonList(DownloadLinkCandidate candidate) {
        ProxyInfo proxy = candidate.getProxy();
        Map<String, Map<Account, List<WaitingSkipReasonContainer>>> map1 = history.get(proxy);
        if (map1 == null) {
            map1 = new HashMap<String, Map<Account, List<WaitingSkipReasonContainer>>>();
            history.put(proxy, map1);
        }
        String host = candidate.getCachedAccount().getPlugin().getHost();
        Map<Account, List<WaitingSkipReasonContainer>> map2 = map1.get(host);
        if (map2 == null) {
            map2 = new HashMap<Account, List<WaitingSkipReasonContainer>>();
            map1.put(host, map2);
        }
        Account acc = candidate.getCachedAccount().getAccount();
        List<WaitingSkipReasonContainer> ret = map2.get(acc);
        if (ret == null) {
            ret = new ArrayList<WaitingSkipReasonContainer>();
            map2.put(acc, ret);
        }
        return ret;
    }

    private void add(WaitingSkipReasonContainer waitingSkipReason, ProxyInfo proxyInfo) {
        CAUSE cause = waitingSkipReason.getWaitingSkipReason().getCause();
        Map<ProxyInfo, List<WaitingSkipReasonContainer>> map = causeMapping.get(cause);
        if (map == null) {
            map = new HashMap<ProxyInfo, List<WaitingSkipReasonContainer>>();
            causeMapping.put(cause, map);
        }
        List<WaitingSkipReasonContainer> list = map.get(proxyInfo);
        if (list == null) {
            list = new ArrayList<ProxyInfoHistory.WaitingSkipReasonContainer>();
            map.put(proxyInfo, list);
        }
        list.add(waitingSkipReason);
    }

    private void remove(WaitingSkipReasonContainer waitingSkipReason, ProxyInfo proxyInfo) {
        waitingSkipReason.invalidate();
        CAUSE cause = waitingSkipReason.getWaitingSkipReason().getCause();
        Map<ProxyInfo, List<WaitingSkipReasonContainer>> map = causeMapping.get(cause);
        if (map == null) return;
        List<WaitingSkipReasonContainer> list = map.get(proxyInfo);
        if (list == null) return;
        list.remove(waitingSkipReason);
        if (list.size() == 0) {
            map.remove(proxyInfo);
            if (map.size() == 0) {
                causeMapping.remove(cause);
            }
        }
    }

    public Set<ProxyInfo> list(WaitingSkipReason.CAUSE cause) {
        Map<ProxyInfo, List<WaitingSkipReasonContainer>> map = causeMapping.get(cause);
        if (map != null) return map.keySet();
        return null;
    }

    public List<WaitingSkipReasonContainer> list(WaitingSkipReason.CAUSE cause, ProxyInfo proxyInfo) {
        Map<ProxyInfo, List<WaitingSkipReasonContainer>> map = causeMapping.get(cause);
        if (map != null) {
            if (proxyInfo == null) {
                List<WaitingSkipReasonContainer> ret = new ArrayList<ProxyInfoHistory.WaitingSkipReasonContainer>();
                for (List<WaitingSkipReasonContainer> list : map.values()) {
                    ret.addAll(list);
                }
                return ret;
            }
            return map.get(proxyInfo);
        }
        return null;
    }

    public DownloadLinkCandidateResult getBlockingHistory(DownloadLinkCandidate candidate) {
        if (candidate.getProxy() == null) return null;
        List<WaitingSkipReasonContainer> list = cleanup(getInteralWaitingSkipReasonList(candidate), candidate.getProxy());
        if (list != null && list.size() > 0) {
            final WaitingSkipReason waitingSkipReason = list.get(0).getWaitingSkipReason();
            return new DownloadLinkCandidateResult(RESULT.PROXY_UNAVAILABLE) {
                @Override
                public long getRemainingTime() {
                    return waitingSkipReason.getTimeOutLeft();
                }
            };
        }
        return null;
    }

    private List<WaitingSkipReasonContainer> cleanup(List<WaitingSkipReasonContainer> list, ProxyInfo proxyInfo) {
        if (list == null) return list;
        Iterator<WaitingSkipReasonContainer> it = list.iterator();
        while (it.hasNext()) {
            WaitingSkipReasonContainer next = it.next();
            if (!next.skipReason.isValid() || next.skipReason.isConditionReached()) {
                remove(next, proxyInfo);
                it.remove();
            }
        }
        return list;
    }

    protected void validate() {
        Iterator<Entry<ProxyInfo, Map<String, Map<Account, List<WaitingSkipReasonContainer>>>>> it1 = history.entrySet().iterator();
        while (it1.hasNext()) {
            Entry<ProxyInfo, Map<String, Map<Account, List<WaitingSkipReasonContainer>>>> next1 = it1.next();
            Iterator<Entry<String, Map<Account, List<WaitingSkipReasonContainer>>>> it2 = next1.getValue().entrySet().iterator();
            while (it2.hasNext()) {
                Entry<String, Map<Account, List<WaitingSkipReasonContainer>>> next2 = it2.next();
                Iterator<Entry<Account, List<WaitingSkipReasonContainer>>> it3 = next2.getValue().entrySet().iterator();
                while (it3.hasNext()) {
                    Entry<Account, List<WaitingSkipReasonContainer>> next3 = it3.next();
                    Iterator<WaitingSkipReasonContainer> it4 = next3.getValue().iterator();
                    while (it4.hasNext()) {
                        WaitingSkipReasonContainer next4 = it4.next();
                        WaitingSkipReason skipReason = next4.skipReason;
                        if (skipReason.isConditionReached() || skipReason.isValid() == false) {
                            remove(next4, next1.getKey());
                            it4.remove();
                        }
                    }
                    if (next3.getValue().size() == 0) {
                        it3.remove();
                    }
                }
                if (next2.getValue().size() == 0) {
                    it2.remove();
                }
            }
            if (next1.getValue().size() == 0) {
                it1.remove();
            }
        }
    }

    public ConditionalSkipReason getConditionalSkipReason(DownloadLinkCandidate candidate) {
        if (candidate.getProxy() == null) return null;
        List<WaitingSkipReasonContainer> list = cleanup(getInteralWaitingSkipReasonList(candidate), candidate.getProxy());
        if (list.size() > 0) {
            WaitingSkipReasonContainer first = list.get(0);
            WaitWhileWaitingSkipReasonIsSet ret = first.waitWhile;
            if (ret == null || ret.isValid() == false || ret.isConditionReached()) {
                ret = new WaitWhileWaitingSkipReasonIsSet(first.skipReason, candidate.getLink());
                first.waitWhile = ret;
            }
            return ret;
        }
        return null;
    }

    public boolean putIntoHistory(DownloadLinkCandidate candidate, WaitingSkipReason waitingSkipReason) {
        if (candidate.getProxy() == null) throw new IllegalArgumentException("candidate.getProxy() == null");
        if (!(waitingSkipReason.getCause() == WaitingSkipReason.CAUSE.HOST_TEMP_UNAVAILABLE || waitingSkipReason.getCause() == WaitingSkipReason.CAUSE.IP_BLOCKED)) throw new IllegalArgumentException("putIntoHistory cannot be used with " + waitingSkipReason.getCause());
        ProxyInfo proxyInfo = candidate.getProxy();
        List<WaitingSkipReasonContainer> list = cleanup(getInteralWaitingSkipReasonList(candidate), proxyInfo);
        Iterator<WaitingSkipReasonContainer> it = list.iterator();
        boolean add = true;
        while (it.hasNext()) {
            WaitingSkipReasonContainer next = it.next();
            if (next.skipReason.getCause() == waitingSkipReason.getCause()) {
                if (next.skipReason.getTimeOutTimeStamp() < waitingSkipReason.getTimeOutTimeStamp()) {
                    add = false;
                } else {
                    remove(next, proxyInfo);
                    it.remove();
                }
            }
        }
        if (add) {
            WaitingSkipReasonContainer container = new WaitingSkipReasonContainer(waitingSkipReason, candidate);
            list.add(container);
            add(container, proxyInfo);
            Collections.sort(list, new Comparator<WaitingSkipReasonContainer>() {

                @Override
                public int compare(WaitingSkipReasonContainer o1, WaitingSkipReasonContainer o2) {
                    long x = o1.skipReason.getTimeOutTimeStamp();
                    long y = o2.skipReason.getTimeOutTimeStamp();
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                }
            });
            return true;
        } else {
            return false;
        }

    }
}
