//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.jdownloader.extensions.eventscripter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ReconnecterEvent;
import jd.controlling.reconnect.ReconnecterListener;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Script;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.Subscriber;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerControllerListener;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.eventscripter.sandboxobjects.ArchiveSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.CrawlerJobSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.DownloadLinkSandBox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.DownloadlistSelectionSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.EventSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.FilePackageSandBox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.LinkgrabberSelectionSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.PackagizerLinkSandbox;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ExtractionListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.MenuManagerTrayIcon;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.MenuManagerDownloadTabBottomBar;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.gui.views.linkgrabber.bottombar.MenuManagerLinkgrabberTabBottombar;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;

public class EventScripterExtension extends AbstractExtension<EventScripterConfig, EventScripterTranslation> implements MenuExtenderHandler, DownloadWatchdogListener, GenericConfigEventListener<Object>, FileCreationListener, LinkCollectorListener, PackagizerControllerListener, ExtractionListener, ReconnecterListener, ChallengeResponseListener {
    private EventScripterConfigPanel          configPanel = null;
    private volatile List<ScriptEntry>        entries     = new ArrayList<ScriptEntry>();
    private final AtomicReference<Subscriber> subscriber  = new AtomicReference<Subscriber>(null);
    private IntervalController                intervalController;

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public EventScripterExtension() throws StartException {
        setTitle(T.title());
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_EVENT;
    }

    @Override
    protected void stop() throws StopException {
        ChallengeResponseController.getInstance().getEventSender().removeListener(this);
        Reconnecter.getInstance().getEventSender().removeListener(this);
        PackagizerController.getInstance().getEventSender().removeListener(this);
        DownloadWatchDog.getInstance().getEventSender().removeListener(this);
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().removeListener(this);
        FileCreationManager.getInstance().getEventSender().removeListener(this);
        LinkCollector.getInstance().getEventsender().removeListener(this);
        final Subscriber old = EventScripterExtension.this.subscriber.getAndSet(null);
        if (old != null) {
            RemoteAPIController.getInstance().getEventsapi().removeSubscriber(old);
        }
        if (!Application.isHeadless()) {
            MenuManagerTrayIcon.getInstance().unregisterExtender(this);
            MenuManagerMainToolbar.getInstance().unregisterExtender(this);
            MenuManagerMainmenu.getInstance().unregisterExtender(this);
            MenuManagerDownloadTabBottomBar.getInstance().unregisterExtender(this);
            MenuManagerDownloadTableContext.getInstance().unregisterExtender(this);
            MenuManagerLinkgrabberTabBottombar.getInstance().unregisterExtender(this);
            MenuManagerLinkgrabberTableContext.getInstance().unregisterExtender(this);
        }
        SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                final ExtractionExtension instance = ExtractionExtension.getInstance();
                if (instance != null) {
                    instance.getEventSender().removeListener(EventScripterExtension.this);
                }
            }
        });
    }

    @Override
    protected void start() throws StartException {
        ChallengeResponseController.getInstance().getEventSender().addListener(this);
        Reconnecter.getInstance().getEventSender().addListener(this);
        PackagizerController.getInstance().getEventSender().addListener(this);
        LinkCollector.getInstance().getEventsender().addListener(this);
        FileCreationManager.getInstance().getEventSender().addListener(this);
        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().registerExtender(this);
            MenuManagerMainmenu.getInstance().registerExtender(this);
            MenuManagerDownloadTabBottomBar.getInstance().registerExtender(this);
            MenuManagerDownloadTableContext.getInstance().registerExtender(this);
            MenuManagerLinkgrabberTabBottombar.getInstance().registerExtender(this);
            MenuManagerLinkgrabberTableContext.getInstance().registerExtender(this);
            MenuManagerTrayIcon.getInstance().registerExtender(this);
        }
        SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                final ExtractionExtension instance = ExtractionExtension.getInstance();
                if (instance != null) {
                    instance.getEventSender().addListener(EventScripterExtension.this);
                }
            }
        });
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().addListener(this);
        final List<ScriptEntry> loadedEntries = getSettings().getScripts();
        if (loadedEntries == null) {
            this.entries = new ArrayList<ScriptEntry>();
        } else {
            this.entries = new ArrayList<ScriptEntry>(loadedEntries);
        }
        if (!Application.isHeadless()) {
            configPanel = new EventScripterConfigPanel(this);
        }
        intervalController = new IntervalController(this);
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        setupRemoteAPIListener(entries);
                        for (ScriptEntry script : entries) {
                            if (script.isEnabled() && EventTrigger.ON_JDOWNLOADER_STARTED.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                                try {
                                    HashMap<String, Object> props = new HashMap<String, Object>();
                                    runScript(script, props);
                                } catch (Throwable e) {
                                    getLogger().log(e);
                                }
                            }
                        }
                    }
                }.start();
            }
        });
    }

    private void setupRemoteAPIListener(List<ScriptEntry> scriptEntries) {
        if (scriptEntries != null) {
            for (ScriptEntry script : entries) {
                if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_OUTGOING_REMOTE_API_EVENT == script.getEventTrigger()) {
                    final Subscriber subscriber = new Subscriber(new Pattern[] { Pattern.compile(".*") }, new Pattern[0]) {
                        @Override
                        public boolean isAlive() {
                            return super.isAlive() && EventScripterExtension.this.subscriber.get() == this;
                        }

                        @Override
                        public boolean isExpired() {
                            return false;
                        }

                        @Override
                        protected void push(final EventObject event) {
                            for (ScriptEntry script : entries) {
                                if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_OUTGOING_REMOTE_API_EVENT == script.getEventTrigger()) {
                                    try {
                                        HashMap<String, Object> props = new HashMap<String, Object>();
                                        props.put("event", new EventSandbox(event));
                                        // props.put("package", getPackageInfo(downloadController.getDownloadLink().getParentNode()));
                                        runScript(script, props);
                                    } catch (Throwable e) {
                                        getLogger().log(e);
                                    }
                                }
                            }
                        }

                        @Override
                        public void push(List<EventObject> events) {
                            if (events != null) {
                                for (EventObject event : events) {
                                    push(event);
                                }
                            }
                        }
                    };
                    final Subscriber old = EventScripterExtension.this.subscriber.getAndSet(subscriber);
                    if (old != null) {
                        RemoteAPIController.getInstance().getEventsapi().removeSubscriber(old);
                    }
                    RemoteAPIController.getInstance().getEventsapi().addSubscriber(subscriber);
                    return;
                }
            }
        }
        final Subscriber old = EventScripterExtension.this.subscriber.getAndSet(null);
        if (old != null) {
            RemoteAPIController.getInstance().getEventsapi().removeSubscriber(old);
        }
    }

    public List<ScriptEntry> getEntries() {
        return entries;
    }

    @Override
    public String getDescription() {
        return T.description();
    }

    @Override
    public AddonPanel<EventScripterExtension> getGUI() {
        // if you want an own t
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        if (manager instanceof MenuManagerMainmenu) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerMainmenuAction.class);
            return opt;
        } else if (manager instanceof MenuManagerDownloadTabBottomBar) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerMainmenuAction.class);
            return opt;
        } else if (manager instanceof MenuManagerDownloadTableContext) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerContextMenuAction.class);
            return opt;
        } else if (manager instanceof MenuManagerLinkgrabberTableContext) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerContextMenuAction.class);
            return opt;
        } else if (manager instanceof MenuManagerLinkgrabberTabBottombar) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerMainmenuAction.class);
            return opt;
        } else if (manager instanceof MenuManagerTrayIcon) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerMainmenuAction.class);
            return opt;
        } else if (manager instanceof MenuManagerMainToolbar) {
            OptionalContainer opt = new OptionalContainer(false);
            opt.add(GenericEventScriptTriggerToolbarAction.class);
            return opt;
        }
        return null;
    }

    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return configPanel;
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_DOWNLOADS_PAUSE.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_DOWNLOADS_RUNNING.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_DOWNLOADS_STOPPED.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_DOWNLOAD_CONTROLLER_START.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("link", new DownloadLinkSandBox(downloadController.getDownloadLink()));
                    props.put("package", new FilePackageSandBox(downloadController.getDownloadLink().getParentNode()));
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    public void runScript(ScriptEntry script, Map<String, Object> props) {
        final boolean isSynchronous = script.getEventTrigger().isSynchronous(script.getEventTriggerSettings());
        new ScriptThread(this, script, props, getLogger()) {
            @Override
            public boolean isSynchronous() {
                return isSynchronous;
            }
        }.start();
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && (EventTrigger.ON_DOWNLOAD_CONTROLLER_STOPPED.equals(script.getEventTrigger()) || EventTrigger.ON_PACKAGE_FINISHED.equals(script.getEventTrigger())) && StringUtils.isNotEmpty(script.getScript())) {
                final DownloadLink dlLink = downloadController.getDownloadLink();
                final FilePackage fp = dlLink.getParentNode();
                if (EventTrigger.ON_DOWNLOAD_CONTROLLER_STOPPED.equals(script.getEventTrigger())) {
                    try {
                        HashMap<String, Object> props = new HashMap<String, Object>();
                        props.put("link", new DownloadLinkSandBox(dlLink));
                        props.put("package", new FilePackageSandBox(fp));
                        runScript(script, props);
                    } catch (Throwable e) {
                        getLogger().log(e);
                    }
                }
                FilePackageSandBox pkg = null;
                if (EventTrigger.ON_PACKAGE_FINISHED.equals(script.getEventTrigger()) && (pkg = new FilePackageSandBox(fp)).isFinished()) {
                    try {
                        HashMap<String, Object> props = new HashMap<String, Object>();
                        props.put("link", new DownloadLinkSandBox(dlLink));
                        props.put("package", pkg);
                        runScript(script, props);
                    } catch (Throwable e) {
                        getLogger().log(e);
                    }
                }
            }
        }
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public synchronized void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        final List<ScriptEntry> entries = getSettings().getScripts();
        if (entries == null) {
            this.entries = new ArrayList<ScriptEntry>();
        } else {
            this.entries = new ArrayList<ScriptEntry>(entries);
        }
        setupRemoteAPIListener(entries);
        intervalController.update();
    }

    public void save(final List<ScriptEntry> tableData) {
        new Thread() {
            @Override
            public void run() {
                final ArrayList<ScriptEntry> entries = new ArrayList<ScriptEntry>(tableData);
                setupRemoteAPIListener(entries);
                getSettings().setScripts(entries);
            }
        }.start();
    }

    public void runTest(final EventTrigger eventTrigger, final String name, final String scriptSource) {
        new Thread("TestRun") {
            @Override
            public void run() {
                ScriptEntry script = new ScriptEntry();
                script.setEventTrigger(eventTrigger);
                script.setScript(scriptSource);
                script.setName(name);
                script.setEnabled(true);
                runScript(script, eventTrigger.getTestProperties());
            }
        }.start();
    }

    public void runTestCompile(EventTrigger eventTrigger, String script) {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_1_5);
        Global scope = new Global();
        scope.init(cx);
        try {
            // cx.compileString(source, sourceName, lineno, securityDomain)
            Script fcn = cx.compileString(script, "", 1, null);
            Dialog.getInstance().showMessageDialog(_GUI.T.lit_successfull());
        } catch (Throwable e1) {
            Dialog.getInstance().showExceptionDialog(T.syntax_error(), e1.getMessage(), e1);
        } finally {
            Context.exit();
        }
    }

    public synchronized void addScriptEntry(ScriptEntry newScript) {
        if (newScript != null) {
            final ArrayList<ScriptEntry> newEntries = new ArrayList<ScriptEntry>(entries);
            newEntries.add(newScript);
            save(newEntries);
        }
    }

    public synchronized void removeScriptEntries(List<ScriptEntry> entries2) {
        if (entries2 != null) {
            final ArrayList<ScriptEntry> newEntries = new ArrayList<ScriptEntry>(entries);
            newEntries.removeAll(entries2);
            save(newEntries);
        }
    }

    @Override
    public void onNewFile(Object caller, File[] fileList) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_NEW_FILE.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    String[] pathes = new String[fileList.length];
                    for (int i = 0; i < fileList.length; i++) {
                        pathes[i] = fileList[i].getAbsolutePath();
                    }
                    props.put("files", pathes);
                    props.put("caller", caller == null ? null : caller.getClass().getName());
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(final LinkCollectorEvent event, final CrawledLink link) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_NEW_LINK.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("link", new PackagizerLinkSandbox(link));
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink link) {
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_NEW_CRAWLER_JOB.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("job", new CrawlerJobSandbox(job));
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onPackagizerUpdate() {
    }

    @Override
    public void onPackagizerRunBeforeLinkcheck(CrawledLink link) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_PACKAGIZER.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("linkcheckDone", false);
                    props.put("link", new PackagizerLinkSandbox(link));
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onPackagizerRunAfterLinkcheck(CrawledLink link) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_PACKAGIZER.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("linkcheckDone", true);
                    props.put("link", new PackagizerLinkSandbox(link));
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onExtractionEvent(ExtractionEvent event) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.ON_GENERIC_EXTRACTION.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("archive", new ArchiveSandbox(event.getCaller().getArchive()));
                    props.put("event", event.getType().name());
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
        switch (event.getType()) {
        case FINISHED:
            for (ScriptEntry script : entries) {
                if (script.isEnabled() && EventTrigger.ON_ARCHIVE_EXTRACTED.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                    try {
                        HashMap<String, Object> props = new HashMap<String, Object>();
                        props.put("archive", new ArchiveSandbox(event.getCaller().getArchive()));
                        props.put("event", event.getType().name());
                        runScript(script, props);
                    } catch (Throwable e) {
                        getLogger().log(e);
                    }
                }
            }
            break;
        case ACTIVE_ITEM:
        case CLEANUP:
        case EXTRACTING:
        case EXTRACTION_FAILED:
        case EXTRACTION_FAILED_CRC:
        case FILE_NOT_FOUND:
        case NOT_ENOUGH_SPACE:
        case OPEN_ARCHIVE_SUCCESS:
        case PASSWORD_FOUND:
        case PASSWORD_NEEDED_TO_CONTINUE:
        case PASSWORT_CRACKING:
        case QUEUED:
        case START:
        case START_CRACK_PASSWORD:
        case START_EXTRACTION:
        }
    }

    public void refreshScripts() {
        if (intervalController != null) {
            intervalController.update();
        }
        if (configPanel != null) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    configPanel.refresh();
                }
            };
        }
    }

    public void triggerAction(String name, String iconKey, String shortCutString, EventTrigger downloadTableContextMenuButton, SelectionInfo selectionInfo) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && downloadTableContextMenuButton == script.getEventTrigger() && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("name", name);
                    props.put("icon", iconKey);
                    props.put("shortCutString", shortCutString);
                    props.put("menu", downloadTableContextMenuButton.name());
                    View view = MainTabbedPane.getInstance().getSelectedView();
                    if (view instanceof DownloadsView) {
                        if (downloadTableContextMenuButton == EventTrigger.DOWNLOAD_TABLE_CONTEXT_MENU_BUTTON) {
                            props.put("dlSelection", new DownloadlistSelectionSandbox(selectionInfo));
                        } else {
                            props.put("dlSelection", new DownloadlistSelectionSandbox(DownloadsTable.getInstance().getSelectionInfo()));
                        }
                    } else if (view instanceof LinkGrabberView) {
                        if (downloadTableContextMenuButton == EventTrigger.LINKGRABBER_TABLE_CONTEXT_MENU_BUTTON) {
                            props.put("lgSelection", new LinkgrabberSelectionSandbox(selectionInfo));
                        } else {
                            props.put("lgSelection", new LinkgrabberSelectionSandbox(LinkGrabberTable.getInstance().getSelectionInfo()));
                        }
                    }
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onAfterReconnect(ReconnecterEvent event) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.RECONNECT_AFTER.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("result", event.getResult() + "");
                    props.put("method", event.getPlugin().getClass().getSimpleName());
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onBeforeReconnect(ReconnecterEvent event) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.RECONNECT_BEFORE.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("method", event.getPlugin().getClass().getSimpleName());
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
    }

    @Override
    public void onJobDone(SolverJob<?> job) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.CAPTCHA_CHALLENGE_AFTER.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    final HashMap<String, Object> props = new HashMap<String, Object>();
                    final ArrayList<String> solver = new ArrayList<String>();
                    for (ChallengeSolver<?> s : job.getSolverList()) {
                        solver.add(s.getService().getID() + "." + s.getClass().getSimpleName());
                    }
                    props.put("hasPendingJobs", ChallengeResponseController.getInstance().hasPendingJobs());
                    props.put("solved", job.isSolved());
                    props.put("solver", solver.toArray(new String[] {}));
                    ResponseList<?> resp = job.getResponse();
                    props.put("result", resp == null ? null : resp.getValue());
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onNewJob(SolverJob<?> job) {
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && EventTrigger.CAPTCHA_CHALLENGE_BEFORE.equals(script.getEventTrigger()) && StringUtils.isNotEmpty(script.getScript())) {
                try {
                    final HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("hasPendingJobs", ChallengeResponseController.getInstance().hasPendingJobs());
                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onNewFolder(Object caller, File folder) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }
}