package org.jdownloader.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.controlling.DefaultDownloadLinkViewImpl;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.myjdownloader.client.bindings.UrlDisplayTypeStorable;
import org.jdownloader.settings.UrlDisplayEntry;
import org.jdownloader.settings.UrlDisplayType;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class SelectionInfoUtils {
    public static void startOnlineStatusCheck(SelectionInfo<? extends AbstractPackageNode, ? extends AbstractNode> selectionInfo) throws BadParameterException {
        final List<?> children = selectionInfo.getChildren();
        if (children.size() > 0) {
            final List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(children.size());
            for (Object l : children) {
                if (l instanceof CheckableLink) {
                    checkableLinks.add(((CheckableLink) l));
                }
            }
            final LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
            linkChecker.check(checkableLinks);
        }
    }

    public static List<UrlDisplayType> parse(UrlDisplayTypeStorable[] urlDisplayTypes) throws BadParameterException {
        final List<UrlDisplayType> ret = new ArrayList<UrlDisplayType>();
        if (CFG_GENERAL.CFG.isUseUrlOrderForMyJD()) {
            DefaultDownloadLinkViewImpl.DISPLAY_URL_TYPE.getClass();
            final UrlDisplayEntry[] newOrder = CFG_GENERAL.CFG.getUrlOrder();
            if (newOrder != null && newOrder.length > 0) {
                for (UrlDisplayEntry e : newOrder) {
                    try {
                        final UrlDisplayType type = UrlDisplayType.valueOf(e.getType());
                        if (!ret.contains(type)) {
                            ret.add(type);
                        }
                    } catch (Throwable e1) {
                    }
                }
            } else {
                ret.addAll(Arrays.asList(UrlDisplayType.values()));
            }
        } else {
            for (final UrlDisplayTypeStorable urlDisplayType : urlDisplayTypes) {
                try {
                    ret.add(UrlDisplayType.valueOf(urlDisplayType.name()));
                } catch (Exception e) {
                    throw new BadParameterException(e.getMessage());
                }
            }
        }
        return ret;
    }

    public static Map<String, List<Long>> getURLs(final SelectionInfo<? extends AbstractPackageNode, ? extends AbstractNode> selectionInfo, final List<UrlDisplayType> urlDisplayTypes) {
        final List<? extends AbstractNode> children = selectionInfo.getChildren();
        final HashMap<String, List<Long>> ret = new HashMap<String, List<Long>>();
        if (children.size() > 0) {
            final boolean copySingleRealURL = CFG_GENERAL.CFG.isCopySingleRealURL() && children.size() == 1;
            for (final AbstractNode node : children) {
                for (final UrlDisplayType urlDisplayType : urlDisplayTypes) {
                    String url = LinkTreeUtils.getUrlByType(urlDisplayType, node);
                    if (url == null && UrlDisplayType.CONTENT.equals(urlDisplayType) && copySingleRealURL) {
                        final Set<String> urls = LinkTreeUtils.getURLs(selectionInfo, false, true);
                        url = urls.size() == 1 ? urls.iterator().next() : null;
                    }
                    if (url != null) {
                        List<Long> list = ret.get(url);
                        if (list == null) {
                            list = new ArrayList<Long>();
                            ret.put(url, list);
                        }
                        list.add(node.getUniqueID().getID());
                        break;
                    }
                }
            }
        }
        return ret;
    }
}
