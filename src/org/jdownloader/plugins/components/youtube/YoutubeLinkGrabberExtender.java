package org.jdownloader.plugins.components.youtube;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.PluginForHost;

import org.appwork.swing.action.BasicAction;
import org.appwork.uio.UIOManager;
import org.appwork.utils.CounterMap;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.choosevariantdialog.YoutubeVariantSelectionDialogAddMulti;
import org.jdownloader.plugins.components.youtube.choosevariantdialog.YoutubeVariantSelectionDialogSetMulti;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.AudioVariant;
import org.jdownloader.plugins.components.youtube.variants.SubtitleVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VariantInfo;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;

public class YoutubeLinkGrabberExtender {

    private JComponent                          parent;
    private PluginView<CrawledLink>             pv;
    private Collection<PluginView<CrawledLink>> allPvs;
    private PluginForHost                       plg;

    private JMenuItem                           addVariants;

    // protected HashMap<String, AbstractVariant> map;

    private JMenu                               youtubeMenu;
    private HashMap<VariantGroup, JMenuItem>    groupMenuItems;
    private LogInterface                        logger;

    public YoutubeLinkGrabberExtender(PluginForHost plg, JComponent parent, PluginView<CrawledLink> pv, Collection<PluginView<CrawledLink>> allPvs) {
        this.plg = plg;
        this.parent = parent;
        this.pv = pv;
        this.allPvs = allPvs;
        logger = plg.getLogger();
    }

    private CounterMap<VariantGroup> groupCount;
    private JMenu                    setVariantMenu;

    public void run() {

        addVariants = new JMenuItem(new BasicAction() {
            {

                setSmallIcon(new AbstractIcon(IconKey.ICON_ADD, 18));
                setName(_GUI.T.youtube_add_variant());

            }

            @Override
            public void actionPerformed(final ActionEvent e) {

                addVariants();
            }

        });
        // addVariants.setIcon(new BadgeIcon(DomainInfo.getInstance(getHost()).getFavIcon(), new AbstractIcon(IconKey.ICON_ADD, 16), 4, 4));

        parent.add(youtubeMenu = new JMenu("youtube.com"));
        Icon icon = DomainInfo.getInstance("youtube.com").getFavIcon();

        youtubeMenu.setIcon(icon);

        youtubeMenu.add(setVariantMenu = new JMenu(_GUI.T.youtube_choose_variant()));
        setVariantMenu.setIcon(new AbstractIcon(IconKey.ICON_REFRESH, 18));
        groupMenuItems = new HashMap<VariantGroup, JMenuItem>();
        for (final VariantGroup g : VariantGroup.values()) {
            if (g == VariantGroup.DESCRIPTION) {
                continue;
            }
            JMenuItem menu;
            setVariantMenu.add(menu = new JMenuItem(new BasicAction() {
                {

                    setSmallIcon(g.getIcon(18));
                    setName(_GUI.T.youtube_choose_variant_group(g.getLabel()));

                }

                @Override
                public void actionPerformed(final ActionEvent e) {

                    setVariants(g);
                }

            }));
            groupMenuItems.put(g, menu);
        }

        youtubeMenu.add(addVariants);
        new Thread("Collect Variants") {

            public void run() {
                groupCount = new CounterMap<VariantGroup>();
                for (CrawledLink cl : pv.getChildren()) {
                    if (cl.getDownloadLink().getHost().equals("youtube.com")) {
                        try {
                            groupCount.increment(((AbstractVariant) cl.gethPlugin().getActiveVariantByLink(cl.getDownloadLink())).getGroup());

                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                }

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        int active = 0;
                        JMenuItem last = null;
                        for (VariantGroup g : VariantGroup.values()) {
                            if (g == VariantGroup.DESCRIPTION) {
                                continue;
                            }
                            if (groupCount.getInt(g) > 0) {
                                groupMenuItems.get(g).setEnabled(true);
                                last = groupMenuItems.get(g);
                                active++;
                            }

                            groupMenuItems.get(g).setText(_GUI.T.youtube_choose_variant_group_linkcount(g.getLabel(), groupCount.getInt(g)));

                        }
                        if (active == 1) {
                            youtubeMenu.remove(setVariantMenu);
                            youtubeMenu.remove(addVariants);
                            youtubeMenu.add(last);
                            youtubeMenu.add(addVariants);
                        }
                    }

                };

            };
        }.start();

    }

    protected void addVariants() {
        if (pv.getChildren().size() == 1) {
            ((YoutubeHostPluginInterface) pv.getPlugin()).showChangeOrAddVariantDialog(pv.getChildren().get(0), null);
            return;
        }
        ProgressGetter pg = new ProgressGetter() {

            private int done;

            protected void aggregate(CrawledLink cl, final CounterMap<String> matchingLinks, HashSet<String> dupeAdd, final ArrayList<VariantInfo> vs, List<VariantInfo> variants) {
                HashSet<String> dupe = new HashSet<String>();
                ;
                for (VariantInfo vi : variants) {

                    String id = new VariantIDStorable(vi.getVariant()).createUniqueID();
                    if (vi.getVariant() instanceof SubtitleVariant) {
                        id = vi.getVariant()._getUniqueId();
                    }
                    if (dupe.add(id)) {

                        matchingLinks.increment(id);
                    }
                    if (dupeAdd.add(id)) {
                        vs.add(vi);

                    }

                }
            }

            @Override
            public void run() throws Exception {

                final CounterMap<String> matchingLinks = new CounterMap<String>();
                HashSet<String> dupeAdd = new HashSet<String>();
                final ArrayList<VariantInfo> vs = new ArrayList<VariantInfo>();
                final HashSet<String> videoIDDupe = new HashSet<String>();
                done = 0;

                for (CrawledLink cl : pv.getChildren()) {

                    try {

                        YoutubeHelper helper;
                        final YoutubeClipData clipData = ClipDataCache.get(helper = new YoutubeHelper(new Browser(), LoggerFactory.getDefaultLogger()), cl.getDownloadLink());
                        if (!videoIDDupe.add(clipData.videoID)) {
                            continue;
                        }
                        List<VariantInfo> variants = clipData.findVariants();
                        helper.extendedDataLoading(variants);
                        aggregate(cl, matchingLinks, dupeAdd, vs, variants);
                        aggregate(cl, matchingLinks, dupeAdd, vs, clipData.findDescriptionVariant());

                        aggregate(cl, matchingLinks, dupeAdd, vs, clipData.findSubtitleVariants());

                    } finally {
                        done++;
                    }
                }

                new Thread("Add Youtube Variant") {
                    public void run() {
                        YoutubeVariantSelectionDialogAddMulti d;
                        try {

                            UIOManager.I().show(null, d = new YoutubeVariantSelectionDialogAddMulti(matchingLinks, videoIDDupe.size(), vs)).throwCloseExceptions();

                            boolean alternativesEnabled = d.isAutoAlternativesEnabled();
                            final List<LinkVariant> choosenVariant = d.getVariants();
                            java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);
                            for (LinkVariant lv : choosenVariant) {
                                HashSet<String> dupe = new HashSet<String>();
                                for (CrawledLink cl : pv.getChildren()) {

                                    if (!dupe.add(cl.getDownloadLink().getStringProperty(YoutubeHelper.YT_ID))) {
                                        continue;
                                    }

                                    AbstractVariant found = findBestVariant(cl, ((AbstractVariant) lv).getGroup(), ((AbstractVariant) lv), alternativesEnabled);
                                    if (found != null) {
                                        CrawledLink newLink = LinkCollector.getInstance().addAdditional(cl, found);
                                        if (newLink != null) {
                                            checkableLinks.add(newLink);
                                        }
                                    }
                                }
                            }
                            LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                            linkChecker.check(checkableLinks);
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }.start();

            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return (done * 100) / pv.getChildren().size();
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };
        ProgressDialog dialog = new ProgressDialog(pg, 0, _GUI.T.lit_please_wait(), _GUI.T.youtube_scan_variants(), new AbstractIcon(IconKey.ICON_WAIT, 32));
        UIOManager.I().show(null, dialog);
    }

    protected void setVariants(final VariantGroup g) {
        if (pv.getChildren().size() == 1) {
            ((YoutubeHostPluginInterface) pv.getPlugin()).showChangeOrAddVariantDialog(pv.getChildren().get(0), (AbstractVariant) pv.getPlugin().getActiveVariantByLink(pv.getChildren().get(0).getDownloadLink()));
            return;
        }
        ProgressGetter pg = new ProgressGetter() {

            private int done;

            @Override
            public void run() throws Exception {

                final CounterMap<String> matchingLinks = new CounterMap<String>();
                HashSet<String> dupeAdd = new HashSet<String>();
                final ArrayList<VariantInfo> vs = new ArrayList<VariantInfo>();
                HashSet<String> videoIDDupe = new HashSet<String>();
                done = 0;

                for (CrawledLink cl : pv.getChildren()) {

                    try {

                        LinkVariant activeVariant = cl.gethPlugin().getActiveVariantByLink(cl.getDownloadLink());
                        if (((AbstractVariant) activeVariant).getGroup() != g) {
                            continue;
                        }
                        YoutubeHelper helper;
                        final YoutubeClipData clipData = ClipDataCache.get(helper = new YoutubeHelper(new Browser(), LoggerFactory.getDefaultLogger()), cl.getDownloadLink());
                        if (!videoIDDupe.add(clipData.videoID)) {
                            continue;
                        }
                        List<VariantInfo> variants = clipData.findVariants();
                        helper.extendedDataLoading(variants);
                        aggregate(cl, g, matchingLinks, dupeAdd, vs, variants);
                        aggregate(cl, g, matchingLinks, dupeAdd, vs, clipData.findDescriptionVariant());

                        aggregate(cl, g, matchingLinks, dupeAdd, vs, clipData.findSubtitleVariants());

                    } finally {
                        done++;
                    }
                }

                new Thread("Choose Youtube Variant") {
                    public void run() {
                        YoutubeVariantSelectionDialogSetMulti d;
                        try {

                            UIOManager.I().show(null, d = new YoutubeVariantSelectionDialogSetMulti(matchingLinks, g, groupCount.getInt(g), vs)).throwCloseExceptions();

                            boolean alternativesEnabled = d.isAutoAlternativesEnabled();
                            final AbstractVariant choosenVariant = (AbstractVariant) d.getVariant();
                            HashSet<String> dupe = new HashSet<String>();
                            for (CrawledLink cl : pv.getChildren()) {

                                if (!dupe.add(cl.getDownloadLink().getStringProperty(YoutubeHelper.YT_ID))) {
                                    continue;
                                }
                                AbstractVariant found = findBestVariant(cl, g, choosenVariant, alternativesEnabled);
                                if (found != null) {
                                    LinkCollector.getInstance().setActiveVariantForLink(cl, found);
                                }
                            }

                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }.start();

            }

            protected void aggregate(CrawledLink cl, final VariantGroup g, final CounterMap<String> matchingLinks, HashSet<String> dupeAdd, final ArrayList<VariantInfo> vs, List<VariantInfo> variants) {
                HashSet<String> dupe = new HashSet<String>();
                ;
                for (VariantInfo vi : variants) {
                    if (vi.getVariant().getGroup() == g) {
                        String id = new VariantIDStorable(vi.getVariant()).createUniqueID();
                        if (g == VariantGroup.SUBTITLES) {
                            id = vi.getVariant()._getUniqueId();
                        }
                        if (dupe.add(id)) {

                            matchingLinks.increment(id);
                        }
                        if (dupeAdd.add(id)) {
                            vs.add(vi);

                        }
                    }
                }
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return (done * 100) / pv.getChildren().size();
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };
        ProgressDialog dialog = new ProgressDialog(pg, 0, _GUI.T.lit_please_wait(), _GUI.T.youtube_scan_variants(), new AbstractIcon(IconKey.ICON_WAIT, 32));
        UIOManager.I().show(null, dialog);

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

                                List<? extends LinkVariant> brotherVariants = plg.getVariantsByLink(brother.getDownloadLink());
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

                    LinkCollectingJob job = new LinkCollectingJob(cl.getOriginLink().getOrigin() == null ? LinkOrigin.ADD_LINKS_DIALOG.getLinkOriginDetails() : cl.getOriginLink().getOrigin());
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

    private AbstractVariant findBestVariant(final CrawledLink cl, VariantGroup g, final AbstractVariant choosenVariant, final boolean alternativesEnabled) throws Exception {

        final YoutubeHelper helper;
        final YoutubeClipData clipData = ClipDataCache.get(helper = new YoutubeHelper(new Browser(), LoggerFactory.getDefaultLogger()), cl.getDownloadLink());

        switch (g) {
        case DESCRIPTION:
            return null;
        case SUBTITLES:
            List<VariantInfo> subtitles = clipData.findSubtitleVariants();
            if (subtitles != null) {

                for (VariantInfo v : subtitles) {
                    if (StringUtils.equals(v.getVariant()._getUniqueId(), choosenVariant._getUniqueId())) {
                        return v.getVariant();
                    }
                }
                if (alternativesEnabled) {
                    Locale choosenLocale = ((SubtitleVariant) choosenVariant).getGenericInfo()._getLocale();
                    for (VariantInfo v : subtitles) {
                        Locale vLocale = ((SubtitleVariant) v.getVariant()).getGenericInfo()._getLocale();
                        if (StringUtils.equals(vLocale.getLanguage(), choosenLocale.getLanguage())) {
                            return v.getVariant();
                        }
                    }
                }

            }
            break;
        case AUDIO:
        case IMAGE:
        case VIDEO:
            List<VariantInfo> variants = clipData.findVariants();
            helper.extendedDataLoading(variants);
            // sorts the best matching variants first. (based on quality rating)
            Collections.sort(variants, new Comparator<VariantInfo>() {

                @Override
                public int compare(VariantInfo o1, VariantInfo o2) {
                    return o2.compareTo(o1);
                }
            });

            for (VariantInfo v : variants) {
                if (StringUtils.equals(v.getVariant()._getUniqueId(), choosenVariant._getUniqueId())) {
                    return v.getVariant();
                }
            }

            VariantInfo vCur = null;
            VariantInfo vLast = null;

            for (int i = 0; i < variants.size(); i++) {
                vCur = variants.get(i);
                int comCur = choosenVariant.compareTo(vCur.getVariant());
                if (comCur == 0) {

                    return vCur.getVariant();

                } else if (comCur > 0) {
                    if (vLast != null) {
                        return vLast.getVariant();
                    } else {
                        return vCur.getVariant();
                    }
                }

                vLast = vCur;
            }

            for (VariantInfo v : variants) {
                if (StringUtils.equals(v.getVariant().getTypeId(), choosenVariant.getTypeId())) {
                    return v.getVariant();
                }
            }

            if (alternativesEnabled) {

                for (VariantInfo v : variants) {
                    if (v.getVariant().getGroup() == choosenVariant.getGroup()) {
                        if (v.getVariant().getContainer() == choosenVariant.getContainer()) {
                            if (choosenVariant instanceof VideoVariant && v.getVariant() instanceof VideoVariant) {
                                if (((VideoVariant) v.getVariant()).getVideoCodec() == ((VideoVariant) choosenVariant).getVideoCodec()) {
                                    if (((VideoVariant) v.getVariant()).getAudioCodec() == ((VideoVariant) choosenVariant).getAudioCodec()) {
                                        return v.getVariant();
                                    }
                                }
                            } else if (choosenVariant instanceof AudioVariant && v.getVariant() instanceof AudioVariant) {
                                if (v.getVariant().getContainer() == choosenVariant.getContainer()) {
                                    if (((AudioVariant) v.getVariant()).getAudioCodec() == ((AudioVariant) choosenVariant).getAudioCodec()) {
                                        return v.getVariant();
                                    }
                                }

                            }
                        }

                    }

                }

                for (VariantInfo v : variants) {
                    if (v.getVariant().getGroup() == choosenVariant.getGroup()) {
                        if (v.getVariant().getContainer() == choosenVariant.getContainer()) {
                            return v.getVariant();
                        }
                    }

                }

            }

        }
        return null;
    };

    private String getHost() {
        return plg.getHost();
    }

    // private void buildVariantsMaps() {
    // // contains all variants
    // listMapAdd = new HashMap<String, ArrayList<AbstractVariant>>();
    // // contains only available by grouping
    // listMapSet = new HashMap<String, ArrayList<AbstractVariant>>();
    //
    // VariantBase[] variants = VariantBase.values();
    //
    // HashSet<String> dupeAdd = new HashSet<String>();
    // HashSet<String> dupeSet = new HashSet<String>();
    //
    // for (VariantBase ytv : variants) {
    // switch (ytv.getGroup()) {
    // case AUDIO:
    // case IMAGE:
    // case DESCRIPTION:
    // AbstractVariant lv = AbstractVariant.get(ytv);
    // if (dupeAdd.add(lv.getTypeId())) {
    // ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
    // if (l == null) {
    // l = new ArrayList<AbstractVariant>();
    // listMapAdd.put(lv.getGroup().name(), l);
    // }
    // l.add(lv);
    // }
    // break;
    // case VIDEO:
    // lv = AbstractVariant.get(ytv);
    // if (dupeAdd.add(lv.getTypeId())) {
    // ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
    // if (l == null) {
    // l = new ArrayList<AbstractVariant>();
    // listMapAdd.put(lv.getGroup().name(), l);
    // }
    // l.add(lv);
    // }
    //
    // lv = AbstractVariant.get(ytv);
    // // ((VideoVariant) lv).getGenericInfo().setThreeD(true);
    // //
    // // if (dupeAdd.add(lv.getTypeId())) {
    // // ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
    // // if (l == null) {
    // // l = new ArrayList<AbstractVariant>();
    // // listMapAdd.put(lv.getGroup().name(), l);
    // // }
    // // l.add(lv);
    // // }
    //
    // break;
    // // case VIDEO_3D:
    // // lv = AbstractVariant.get(ytv);
    // // if (dupeAdd.add(lv.getTypeId())) {
    // // ArrayList<AbstractVariant> l = listMapAdd.get(lv.getGroup().name());
    // // if (l == null) {
    // // l = new ArrayList<AbstractVariant>();
    // // listMapAdd.put(lv.getGroup().name(), l);
    // // }
    // // l.add(lv);
    // // }
    //
    // case SUBTITLES:
    // // nothing
    // }
    //
    // }
    // // add generics
    // for (CrawledLink cl : pv.getChildren()) {
    // List<? extends LinkVariant> v = plg.getVariantsByLink(cl.getDownloadLink());
    // if (v != null) {
    // for (LinkVariant lv : v) {
    // if (lv instanceof AbstractVariant) {
    // if (dupeAdd.add(((AbstractVariant) lv).getTypeId())) {
    // ArrayList<AbstractVariant> l = listMapAdd.get(((AbstractVariant) lv).getGroup().name());
    // if (l == null) {
    // l = new ArrayList<AbstractVariant>();
    // listMapAdd.put(((AbstractVariant) lv).getGroup().name(), l);
    // }
    // l.add((AbstractVariant) lv);
    // }
    //
    // if (dupeSet.add(((AbstractVariant) lv).getTypeId())) {
    // ArrayList<AbstractVariant> l = listMapSet.get(((AbstractVariant) lv).getGroup().name());
    // if (l == null) {
    // l = new ArrayList<AbstractVariant>();
    // listMapSet.put(((AbstractVariant) lv).getGroup().name(), l);
    // }
    // l.add((AbstractVariant) lv);
    // }
    //
    // }
    // }
    // }
    // }
    //
    // Comparator<AbstractVariant> comp = new Comparator<AbstractVariant>() {
    //
    // @Override
    // public int compare(AbstractVariant o1, AbstractVariant o2) {
    // return new Double(o2.getQualityRating()).compareTo(new Double(o1.getQualityRating()));
    // }
    // };
    // for (Entry<String, ArrayList<AbstractVariant>> es : listMapSet.entrySet()) {
    // Collections.sort(es.getValue(), comp);
    // }
    // for (Entry<String, ArrayList<AbstractVariant>> es : listMapAdd.entrySet()) {
    // Collections.sort(es.getValue(), comp);
    // }
    // }

}
