package org.jdownloader.gui.notify;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorHighlightListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.controlling.reconnect.ReconnecterListener;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.notify.gui.Balloner;
import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class BubbleNotify implements UpdaterListener, ReconnecterListener {
    private static final BubbleNotify INSTANCE = new BubbleNotify();

    /**
     * get the only existing instance of BubbleNotify. This is a singleton
     * 
     * @return
     */
    public static BubbleNotify getInstance() {
        return BubbleNotify.INSTANCE;
    }

    private Balloner ballooner;
    private boolean  updatesNotified;

    /**
     * Create a new instance of BubbleNotify. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private BubbleNotify() {
        ballooner = new Balloner(null) {
            public JFrame getOwner() {
                if (JDGui.getInstance() == null) return null;
                return JDGui.getInstance().getMainFrame();
            }
        };
        GenericConfigEventListener<Object> update = new GenericConfigEventListener<Object>() {

            @Override
            public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                ballooner.setScreenID(CFG_BUBBLE.CFG.getScreenID());
                ballooner.setStartPoint(new Point(CFG_BUBBLE.CFG.getStartX(), CFG_BUBBLE.CFG.getStartY()), CFG_BUBBLE.CFG.getStartAnchor());
                ballooner.setEndPoint(new Point(CFG_BUBBLE.CFG.getEndX(), CFG_BUBBLE.CFG.getEndY()), CFG_BUBBLE.CFG.getEndAnchor());
                ballooner.setAnchorPoint(new Point(CFG_BUBBLE.CFG.getAnchorX(), CFG_BUBBLE.CFG.getAnchorY()), CFG_BUBBLE.CFG.getAnchor());

            }

            @Override
            public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
            }
        };

        CFG_BUBBLE.CFG._getStorageHandler().getEventSender().addListener(update);
        update.onConfigValueModified(null, null);
        initLinkCollectorListener();
        // if (ballooner != null) ballooner.add(new Notify(caption, text, NewTheme.I().getIcon("info", 32)));

        UpdateController.getInstance().getEventSender().addListener(this, true);

        Reconnecter.getInstance().getEventSender().addListener(this, true);
    }

    private void initLinkCollectorListener() {

        LinkCollector.getInstance().getEventsender().addListener(new LinkCollectorHighlightListener() {
            @Override
            public void onLinkCollectorContentAdded(final LinkCollectorEvent event) {

                if (!CFG_BUBBLE.CFG.isBubbleNotifyOnNewLinkgrabberPackageEnabled()) return;
                if (event.getParameters().length > 0 && event.getParameter(0) instanceof CrawledPackage) {
                    // new Package
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {

                            switch (((CrawledPackage) event.getParameter(0)).getType()) {
                            case NORMAL:

                                BasicNotify no = new BasicNotify(CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_PACKAGE_ENABLED, _GUI._.balloon_new_package(), _GUI._.balloon_new_package_msg(((CrawledPackage) event.getParameter(0)).getName()), NewTheme.I().getIcon(IconKey.ICON_PACKAGE_NEW, 32));
                                no.setActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER, null);
                                        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
                                    }
                                });
                                show(no);
                                break;

                            //
                            }
                        }

                    };

                }
            }

            @Override
            public void onHighLight(CrawledLink parameter) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (!CFG_BUBBLE.CFG.isBubbleNotifyOnNewLinkgrabberLinksEnabled()) return;

                        BasicNotify no = new BasicNotify(CFG_BUBBLE.BUBBLE_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED, _GUI._.balloon_new_links(), _GUI._.balloon_new_links_msg(LinkCollector.getInstance().getPackages().size(), LinkCollector.getInstance().getChildrenCount()), NewTheme.I().getIcon(IconKey.ICON_LINKGRABBER, 32));
                        no.setActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER, null);
                                JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
                            }
                        });
                        show(no);

                    }
                };
            }

            @Override
            public boolean isThisListenerEnabled() {
                return true;
            }
        });

    }

    private void show(final AbstractNotifyWindow no) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                ballooner.add(no);
            }
        };

    }

    @Override
    public void onReconnectSettingsUpdated(ReconnecterEvent event) {
    }

    @Override
    public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {

        if (UpdateController.getInstance().hasPendingUpdates() && !updatesNotified) {
            updatesNotified = true;
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (!CFG_BUBBLE.CFG.isBubbleNotifyOnUpdateAvailableEnabled()) return;

                    BasicNotify no = new BasicNotify(CFG_BUBBLE.BUBBLE_NOTIFY_ON_UPDATE_AVAILABLE_ENABLED, _GUI._.balloon_updates(), _GUI._.balloon_updates_msg(), NewTheme.I().getIcon("update", 32));
                    no.setActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            new UpdateAction(null).actionPerformed(e);
                        }
                    });
                    show(no);

                }
            };
        } else if (!UpdateController.getInstance().hasPendingUpdates()) {
            updatesNotified = false;
        }
    }

    @Override
    public void onBeforeReconnect(final ReconnecterEvent event) {
        if (!CFG_BUBBLE.CFG.isBubbleNotifyOnReconnectStartEnabled()) return;
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                BasicNotify no = new BasicNotify(CFG_BUBBLE.BUBBLE_NOTIFY_ON_RECONNECT_START_ENABLED, _GUI._.balloon_reconnect(), _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32));

                show(no);
            }
        };
    }

    @Override
    public void onAfterReconnect(final ReconnecterEvent event) {
        if (!CFG_BUBBLE.CFG.isBubbleNotifyOnReconnectEndEnabled()) return;
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (!(Boolean) event.getParameter()) {

                    BasicNotify no = new BasicNotify(CFG_BUBBLE.BUBBLE_NOTIFY_ON_RECONNECT_END_ENABLED, _GUI._.balloon_reconnect(), _GUI._.balloon_reconnect_end_msg_failed(IPController.getInstance().getIP()), NewTheme.I().getIcon("error", 32));

                    show(no);

                } else {

                    BasicNotify no = new BasicNotify(CFG_BUBBLE.BUBBLE_NOTIFY_ON_RECONNECT_END_ENABLED, _GUI._.balloon_reconnect(), _GUI._.balloon_reconnect_end_msg_failed(IPController.getInstance().getIP()), NewTheme.I().getIcon("ok", 32));

                    show(no);
                }

            }
        };
    }
}
