package org.jdownloader.api.utils;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.jdownloader.gui.views.SelectionInfo;

public class SelectionInfoUtils {

    public static void startOnlineStatusCheck(SelectionInfo<?, ?> selectionInfo) throws BadParameterException {
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
}
