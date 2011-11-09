package org.jdownloader.gui.views.linkgrabber;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class SearchField<PackageType extends AbstractPackageNode<ChildType, PackageType>, ChildType extends AbstractPackageChildrenNode<PackageType>> extends ExtTextField implements PackageControllerTableModelFilter<PackageType, ChildType> {
    /**
     * 
     */
    private static final long                              serialVersionUID = -8079363840549073686L;
    private static final int                               SIZE             = 20;
    private Image                                          img;
    private DelayedRunnable                                delayedFilter;
    private PackageControllerTable<PackageType, ChildType> table2Filter;
    private Pattern                                        filterPattern    = null;

    public SearchField(PackageControllerTable<PackageType, ChildType> table2Filter) {
        super();
        this.table2Filter = table2Filter;
        img = NewTheme.I().getImage("search", SIZE);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, 22, 0, 0)));
        setHelpText(_GUI._.SearchField_SearchField_helptext());
        delayedFilter = new DelayedRunnable(IOEQ.TIMINGQUEUE, 150l, 2000l) {

            @Override
            public void delayedrun() {
                updateFilter();
            }

        };
    }

    @Override
    protected void onChanged() {
        delayedFilter.run();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.drawImage(img, 3, 3, 3 + SIZE, 3 + SIZE, 0, 0, SIZE, SIZE, null);
        g2.setComposite(comp);
    }

    private synchronized void updateFilter() {
        String filterRegex = this.getText();
        boolean enabled = filterRegex.length() > 0;
        if (enabled) {
            try {
                filterPattern = LinkgrabberFilterRuleWrapper.createPattern(filterRegex, (JsonConfig.create(GeneralSettings.class).isFilterRegex()));
                table2Filter.getPackageControllerTableModel().addFilter(this);
            } catch (final Throwable e) {
                Log.exception(e);
            }
        } else {
            table2Filter.getPackageControllerTableModel().removeFilter(this);
        }
        table2Filter.getPackageControllerTableModel().recreateModel(true);
    }

    public boolean isFiltered(PackageType e) {
        return false;
    }

    public boolean isFiltered(ChildType v) {
        return !filterPattern.matcher(v.getName()).find();
    }

    public void reset() {
    }
}
