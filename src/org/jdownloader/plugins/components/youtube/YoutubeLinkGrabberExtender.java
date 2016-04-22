package org.jdownloader.plugins.components.youtube;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.JScrollMenu;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.plugins.hoster.YoutubeDashV2;

public class YoutubeLinkGrabberExtender {

    private JComponent                                    parent;
    private PluginView<CrawledLink>                       pv;
    private Collection<PluginView<CrawledLink>>           allPvs;
    private YoutubeDashV2                                 plg;
    private LogInterface                                  logger;
    private JScrollMenu                                   setVariants;
    private JScrollMenu                                   addVariants;
    // protected HashMap<String, AbstractVariant> map;
    protected HashMap<String, ArrayList<AbstractVariant>> listMapAdd;
    protected HashMap<String, ArrayList<AbstractVariant>> listMapAvailable;
    private HashMap<String, ArrayList<AbstractVariant>>   listMapSet;

    public YoutubeLinkGrabberExtender(YoutubeDashV2 plg, JComponent parent, PluginView<CrawledLink> pv, Collection<PluginView<CrawledLink>> allPvs) {
        this.plg = plg;
        this.parent = parent;
        this.pv = pv;
        this.allPvs = allPvs;
        logger = plg.getLogger();
    }

    public void run() {
        setVariants = new JScrollMenu(_GUI.T.YoutubeDashV2_extendLinkgrabberContextMenu_context_menu());
        setVariants.setIcon(DomainInfo.getInstance(getHost()).getFavIcon());
        setVariants.setEnabled(false);

        addVariants = new JScrollMenu(_GUI.T.YoutubeDashV2_extendLinkgrabberContextMenu_context_menu_add());

        addVariants.setIcon(new BadgeIcon(DomainInfo.getInstance(getHost()).getFavIcon(), new AbstractIcon(IconKey.ICON_ADD, 16), 4, 4));
        addVariants.setEnabled(false);
        new Thread("Collect Variants") {
            public void run() {
                buildVariantsMaps();

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        buildMenuInEDT();

                    }

                };

            };
        }.start();

        parent.add(setVariants);
        parent.add(addVariants);
    }

    private void buildMenuInEDT() {
        add(VariantGroup.VIDEO);
        // if (has3D()) {
        // add(VariantGroup.VIDEO_3D);
        // }
        add(VariantGroup.AUDIO);

        add(VariantGroup.IMAGE);

        add(VariantGroup.SUBTITLES);
        add(VariantGroup.DESCRIPTION);
    }

    private boolean has3D() {
        for (CrawledLink cl : pv.getChildren()) {
            if (cl.getDownloadLink().getBooleanProperty(YoutubeHelper.YT_3D, false)) {
                return true;
            }
        }
        return false;
    }

    private void add(final VariantGroup group) {
        ArrayList<AbstractVariant> optionsAdd = listMapAdd.get(group.name());
        ArrayList<AbstractVariant> optionsSet = listMapSet.get(group.name());

        // final Comparator<AbstractVariant> comp;
        // Collections.sort(list, comp = new Comparator<AbstractVariant>() {
        //
        // @Override
        // public int compare(AbstractVariant o1, AbstractVariant o2) {
        // return new Double(o2.getQualityRating()).compareTo(new Double(o1.getQualityRating()));
        // }
        // });
        setVariants.setEnabled(true);
        addVariants.setEnabled(true);

        addToAddMenu(group, optionsAdd);
        addToSetMenu(group, optionsSet);

    }

    protected void addToAddMenu(final VariantGroup group, ArrayList<AbstractVariant> list) {

        if (list == null || list.size() <= 1) {
            addVariants.add(new JMenuItem(new BasicAction() {
                {
                    setName(group.getLabel());
                }

                @Override
                public void actionPerformed(ActionEvent e) {

                    addAdditionalVariant(pv, group.name(), null);

                }

            }));

            return;
        }

        JMenu groupMenu = new JScrollMenu(group.getLabel());
        // if (listMap.size() == 1) {
        // groupMenu = addSubmenu;
        // } else {
        addVariants.add(groupMenu);
        // }
        groupMenu.add(new JMenuItem(new BasicAction() {
            {
                setName(_GUI.T.YoutubeDashV2_add_best(group.getLabel()));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                addAdditionalVariant(pv, "+" + group.name(), null);

            }

        }));

        for (final AbstractVariant v : list) {
            groupMenu.add(new JMenuItem(new BasicAction() {
                {
                    setName(v._getName(pv));
                    setTooltipText(v.createAdvancedName());
                }

                //
                @Override
                public void actionPerformed(ActionEvent e) {
                    String id;
                    try {
                        id = Base64.encodeToString(v.getStorableString().getBytes("UTF-8"), false);

                        addAdditionalVariant(pv, id, v);
                    } catch (UnsupportedEncodingException e2) {
                        logger.log(e2);
                    }

                }

            }));

        }
        switch (group) {
        case AUDIO:
        case IMAGE:
        case VIDEO:
            // case VIDEO_3D:
            groupMenu.add(new JMenuItem(new BasicAction() {
                {
                    setName(_GUI.T.YoutubeDashV2_add_worst(group.getLabel()));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    addAdditionalVariant(pv, "-" + group.name(), null);
                }

            }));
        }

    }

    protected void addToSetMenu(final VariantGroup group, ArrayList<AbstractVariant> list) {
        if (list == null || list.size() <= 1) {

            return;
        }

        JMenu groupMenu = new JScrollMenu(group.getLabel());
        if (listMapSet.size() == 1) {
            groupMenu = setVariants;
        } else {
            setVariants.add(groupMenu);
        }
        groupMenu.add(new JMenuItem(new BasicAction() {
            {
                setName(_GUI.T.YoutubeDashV2_add_best(group.getLabel()));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                for (CrawledLink cl : pv.getChildren()) {
                    List<AbstractVariant> variants = new ArrayList<AbstractVariant>();
                    for (LinkVariant v : plg.getVariantsByLink(cl.getDownloadLink())) {
                        if (v instanceof AbstractVariant) {
                            variants.add((AbstractVariant) v);
                        }
                    }
                    // Collections.sort(variants, comp);
                    for (AbstractVariant variant : variants) {
                        if (variant.getGroup() == group) {
                            LinkCollector.getInstance().setActiveVariantForLink(cl.getDownloadLink(), variant);

                            checkableLinks.add(cl);
                            break;
                        }

                    }

                }

                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.check(checkableLinks);
            }

        }));
        for (final AbstractVariant v : list) {
            groupMenu.add(new JMenuItem(new BasicAction() {
                {
                    setName(v._getName(pv));
                    setTooltipText(v.createAdvancedName());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                    for (CrawledLink cl : pv.getChildren()) {
                        for (LinkVariant variants : plg.getVariantsByLink(cl.getDownloadLink())) {
                            if (variants instanceof AbstractVariant) {
                                if (((AbstractVariant) variants).getTypeId().equals(v.getTypeId())) {
                                    LinkCollector.getInstance().setActiveVariantForLink(cl.getDownloadLink(), v);
                                    checkableLinks.add(cl);
                                }
                            }
                        }

                    }

                    LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                    linkChecker.check(checkableLinks);
                }

            }));

        }

        groupMenu.add(new JMenuItem(new BasicAction() {
            {
                setName(_GUI.T.YoutubeDashV2_add_worst(group.getLabel()));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                for (CrawledLink cl : pv.getChildren()) {
                    List<AbstractVariant> variants = new ArrayList<AbstractVariant>();
                    for (LinkVariant v : plg.getVariantsByLink(cl.getDownloadLink())) {
                        if (v instanceof AbstractVariant) {
                            variants.add((AbstractVariant) v);
                        }
                    }
                    // Collections.sort(variants, comp);
                    for (int i = variants.size() - 1; i >= 0; i--) {
                        AbstractVariant variant = variants.get(i);
                        if (variant.getGroup() == group) {
                            LinkCollector.getInstance().setActiveVariantForLink(cl.getDownloadLink(), variant);

                            checkableLinks.add(cl);
                            break;
                        }

                    }

                }

                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.check(checkableLinks);
            }

        }));
    }

    private void addAdditionalVariant(final PluginView<CrawledLink> pv, final String id, final AbstractVariant requested) {
        new Thread("Add Additional YoutubeLinks") {
            public void run() {
                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                HashSet<String> dupecheck = new HashSet<String>();
                main: for (CrawledLink cl : pv.getChildren()) {
                    String videoID = cl.getDownloadLink().getStringProperty(YoutubeHelper.YT_ID);
                    if (!dupecheck.add(videoID)) {
                        continue;
                    }
                    if (requested != null) {
                        CrawledPackage pkg = cl.getParentNode();

                        final boolean readL = pkg.getModifyLock().readLock();

                        ArrayList<CrawledLink> lst;
                        try {
                            lst = new ArrayList<CrawledLink>(pkg.getChildren());
                        } finally {
                            pkg.getModifyLock().readUnlock(readL);
                        }
                        // Search the package to find variants of the same videoID. If we find one, we do not have to go through the
                        // linkcrawler again
                        for (CrawledLink brother : lst) {
                            if (StringUtils.equals(brother.getDownloadLink().getStringProperty(YoutubeHelper.YT_ID, null), cl.getDownloadLink().getStringProperty(YoutubeHelper.YT_ID, null))) {
                                // link form the same videoID
                                List<LinkVariant> brotherVariants = plg.getVariantsByLink(brother.getDownloadLink());
                                if (brotherVariants != null) {
                                    for (LinkVariant brotherVariant : brotherVariants) {
                                        if (brotherVariant != null && brotherVariant instanceof AbstractVariant) {
                                            if (StringUtils.equals(((AbstractVariant) brotherVariant).getTypeId(), requested.getTypeId())) {
                                                CrawledLink newLink = LinkCollector.getInstance().addAdditional(brother, brotherVariant);
                                                if (newLink != null) {
                                                    // forward cache
                                                    checkableLinks.add(newLink);
                                                    continue main;
                                                } else {
                                                    Toolkit.getDefaultToolkit().beep();
                                                }
                                            }
                                        }
                                    }
                                }

                            }

                        }
                    }

                    String dummyUrl = "https://www.youtube.com/watch?v=" + videoID + "#variant=" + Encoding.urlEncode(id);

                    LinkCollectingJob job = new LinkCollectingJob(cl.getOriginLink().getOrigin() == null ? new LinkOriginDetails(LinkOrigin.ADD_LINKS_DIALOG) : cl.getOriginLink().getOrigin());
                    job.setText(dummyUrl);
                    job.setCustomSourceUrl(cl.getOriginLink().getURL());
                    job.setDeepAnalyse(false);
                    LinkCollector.getInstance().addCrawlerJob(job);

                }
                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.check(checkableLinks);
            }

        }.start();

    };

    private String getHost() {
        return plg.getHost();
    }

    private void buildVariantsMaps() {
        // contains all variants
        listMapAdd = new HashMap<String, ArrayList<AbstractVariant>>();
        // contains only available by grouping
        listMapSet = new HashMap<String, ArrayList<AbstractVariant>>();

        VariantBase[] variants = VariantBase.values();

        HashSet<String> dupeAdd = new HashSet<String>();
        HashSet<String> dupeSet = new HashSet<String>();

        for (VariantBase ytv : variants) {
            switch (ytv.getGroup()) {
            case AUDIO:
            case IMAGE:
            case DESCRIPTION:
                AbstractVariant lv = AbstractVariant.get(ytv);
                if (dupeAdd.add(lv.getTypeId())) {
                    ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
                    if (l == null) {
                        l = new ArrayList<AbstractVariant>();
                        listMapAdd.put(lv.getGroup().name(), l);
                    }
                    l.add(lv);
                }
                break;
            case VIDEO:
                lv = AbstractVariant.get(ytv);
                if (dupeAdd.add(lv.getTypeId())) {
                    ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
                    if (l == null) {
                        l = new ArrayList<AbstractVariant>();
                        listMapAdd.put(lv.getGroup().name(), l);
                    }
                    l.add(lv);
                }

                lv = AbstractVariant.get(ytv);
                // ((VideoVariant) lv).getGenericInfo().setThreeD(true);
                //
                // if (dupeAdd.add(lv.getTypeId())) {
                // ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
                // if (l == null) {
                // l = new ArrayList<AbstractVariant>();
                // listMapAdd.put(lv.getGroup().name(), l);
                // }
                // l.add(lv);
                // }

                break;
            // case VIDEO_3D:
            // lv = AbstractVariant.get(ytv);
            // if (dupeAdd.add(lv.getTypeId())) {
            // ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
            // if (l == null) {
            // l = new ArrayList<AbstractVariant>();
            // listMapAdd.put(lv.getGroup().name(), l);
            // }
            // l.add(lv);
            // }

            case SUBTITLES:
                // nothing
            }

        }
        // add generics
        for (CrawledLink cl : pv.getChildren()) {
            List<LinkVariant> v = plg.getVariantsByLink(cl.getDownloadLink());
            if (v != null) {
                for (LinkVariant lv : v) {
                    if (lv instanceof AbstractVariant) {
                        if (dupeAdd.add(((AbstractVariant) lv).getTypeId())) {
                            ArrayList<AbstractVariant> l = listMapAdd.get(((AbstractVariant) lv).getGroup().name());
                            if (l == null) {
                                l = new ArrayList<AbstractVariant>();
                                listMapAdd.put(((AbstractVariant) lv).getGroup().name(), l);
                            }
                            l.add((AbstractVariant) lv);
                        }

                        if (dupeSet.add(((AbstractVariant) lv).getTypeId())) {
                            ArrayList<AbstractVariant> l = listMapSet.get(((AbstractVariant) lv).getGroup().name());
                            if (l == null) {
                                l = new ArrayList<AbstractVariant>();
                                listMapSet.put(((AbstractVariant) lv).getGroup().name(), l);
                            }
                            l.add((AbstractVariant) lv);
                        }

                    }
                }
            }
        }

        Comparator<AbstractVariant> comp = new Comparator<AbstractVariant>() {

            @Override
            public int compare(AbstractVariant o1, AbstractVariant o2) {
                return new Double(o2.getQualityRating()).compareTo(new Double(o1.getQualityRating()));
            }
        };
        for (Entry<String, ArrayList<AbstractVariant>> es : listMapSet.entrySet()) {
            Collections.sort(es.getValue(), comp);
        }
        for (Entry<String, ArrayList<AbstractVariant>> es : listMapAdd.entrySet()) {
            Collections.sort(es.getValue(), comp);
        }
    }

}
