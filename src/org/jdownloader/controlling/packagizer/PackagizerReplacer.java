package org.jdownloader.controlling.packagizer;

import org.jdownloader.controlling.packagizer.PackagizerController.REPLACEVARIABLE;

import jd.controlling.linkcrawler.CrawledLink;

public interface PackagizerReplacer {

    public String getID();

    public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr);

}
