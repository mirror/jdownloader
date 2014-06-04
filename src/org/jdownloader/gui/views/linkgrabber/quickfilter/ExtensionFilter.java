package org.jdownloader.gui.views.linkgrabber.quickfilter;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.images.NewTheme;

public abstract class ExtensionFilter extends Filter {

    private final String                    description;
    private final ExtensionsFilterInterface filter;

    public ExtensionFilter(ExtensionsFilterInterface filter) {
        super(filter.getDesc(), NewTheme.I().getIcon(filter.getIconID(), 16));
        this.filter = filter;
        description = filter.getDesc();
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean isFiltered(CrawledLink link) {
        final ExtensionsFilterInterface extension = link.getLinkInfo().getExtension();
        return filter != null && filter.isSameExtensionGroup(extension);
    }

}
