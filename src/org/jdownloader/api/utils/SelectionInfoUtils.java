package org.jdownloader.api.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.settings.UrlDisplayType;

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

    public static Map<String, List<Long>> getURLs(final SelectionInfo<? extends AbstractPackageNode, ? extends AbstractNode> selectionInfo, final List<UrlDisplayType> urlDisplayTypes) {
        final List<? extends AbstractNode> children = selectionInfo.getChildren();
        final HashMap<String, List<Long>> ret = new HashMap<String, List<Long>>();
        if (children.size() > 0) {
            for (final AbstractNode node : children) {
                for (final UrlDisplayType urlDisplayType : urlDisplayTypes) {
                    final String url = LinkTreeUtils.getUrlByType(urlDisplayType, node);
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
