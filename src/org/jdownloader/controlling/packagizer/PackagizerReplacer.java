package org.jdownloader.controlling.packagizer;

import jd.controlling.linkcrawler.CrawledLink;

public interface PackagizerReplacer {

    public String getID();

    public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr);

}
