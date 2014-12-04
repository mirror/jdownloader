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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Script;
import net.sourceforge.htmlunit.corejs.javascript.tools.shell.Global;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.downloads.v2.DownloadLinkAPIStorableV2;
import org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl;
import org.jdownloader.api.downloads.v2.FilePackageAPIStorableV2;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;
import org.jdownloader.api.downloads.v2.PackageQueryStorable;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.IconKey;

public class EventScripterExtension extends AbstractExtension<EventScripterConfig, EventScripterTranslation> implements MenuExtenderHandler, DownloadWatchdogListener, GenericConfigEventListener<Object> {

    private Object                   lock = new Object();
    private EventScripterConfigPanel configPanel;
    private List<ScriptEntry>        entries;

    @Override
    public boolean isHeadlessRunnable() {
        return false;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public EventScripterExtension() throws StartException {
        setTitle("Event Caller");

    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_EVENT;
    }

    @Override
    protected void stop() throws StopException {
        DownloadWatchDog.getInstance().getEventSender().removeListener(this);
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().removeListener(this);
    }

    @Override
    protected void start() throws StartException {

        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        CFG_EVENT_CALLER.SCRIPTS.getEventSender().addListener(this);
        entries = getSettings().getScripts();
        configPanel = new EventScripterConfigPanel(this);

        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                for (ScriptEntry script : entries) {
                    if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_JDOWNLOADER_STARTED == script.getEventTrigger()) {
                        HashMap<String, Object> props = new HashMap<String, Object>();
                        runScript(script, props);

                    }
                }
            }
        });
    }

    @Override
    public String getDescription() {
        return _.description();
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
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {

        if (entries == null) {
            return;
        }
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_DOWNLOAD_CONTROLLER_START == script.getEventTrigger()) {
                try {
                    HashMap<String, Object> props = new HashMap<String, Object>();
                    props.put("link", getLinkInfo(downloadController.getDownloadLink()));
                    props.put("package", getPackageInfo(downloadController.getDownloadLink().getParentNode()));

                    runScript(script, props);
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }

    }

    public void runScript(ScriptEntry script, HashMap<String, Object> props) {
        new ScriptThread(script, props, getLogger()).start();

    }

    private FilePackageAPIStorableV2 getPackageInfo(FilePackage pkg) {

        PackageQueryStorable query = new PackageQueryStorable();
        query.setBytesLoaded(true);
        query.setBytesTotal(true);
        query.setComment(true);
        query.setEnabled(true);
        query.setEta(true);

        query.setFinished(true);

        query.setRunning(true);

        query.setSpeed(true);
        query.setStatus(true);
        query.setChildCount(true);
        query.setHosts(true);
        query.setSaveTo(true);

        return DownloadsAPIV2Impl.toStorable(query, pkg, this);
    }

    private DownloadLinkAPIStorableV2 getLinkInfo(DownloadLink downloadLink) {

        LinkQueryStorable query = new LinkQueryStorable();
        query.setBytesLoaded(true);
        query.setBytesTotal(true);
        query.setComment(true);
        query.setEnabled(true);
        query.setEta(true);
        query.setExtractionStatus(true);
        query.setFinished(true);
        query.setHost(true);
        query.setPriority(true);
        query.setRunning(true);
        query.setSkipped(true);
        query.setSpeed(true);
        query.setStatus(true);
        query.setUrl(true);
        return DownloadsAPIV2Impl.toStorable(query, downloadLink, this);
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {

        if (entries == null) {
            return;
        }
        for (ScriptEntry script : entries) {
            if (script.isEnabled() && StringUtils.isNotEmpty(script.getScript()) && EventTrigger.ON_DOWNLOAD_CONTROLLER_STOPPED == script.getEventTrigger()) {
                HashMap<String, Object> props = new HashMap<String, Object>();
                props.put("link", getLinkInfo(downloadController.getDownloadLink()));
                props.put("package", getPackageInfo(downloadController.getDownloadLink().getParentNode()));
                runScript(script, props);

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
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        entries = getSettings().getScripts();
    }

    public void save(final List<ScriptEntry> tableData) {
        new Thread() {
            @Override
            public void run() {
                getSettings().setScripts(new ArrayList<ScriptEntry>(tableData));
            }
        }.start();

    }

    public void runTest(EventTrigger eventTrigger, String name, String scriptSource) {

        ScriptEntry script = new ScriptEntry();
        script.setEventTrigger(eventTrigger);
        script.setScript(scriptSource);
        script.setName(name);
        runScript(script, eventTrigger.getTestProperties());
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

            Dialog.getInstance().showMessageDialog("Successful");
        } catch (Throwable e1) {
            Dialog.getInstance().showExceptionDialog("JavaScript Syntax Error", e1.getMessage(), e1);
        } finally {
            Context.exit();
        }
    }

    public void addScriptEntry(ScriptEntry newScript) {
        ArrayList<ScriptEntry> newEntries = entries == null ? new ArrayList<ScriptEntry>() : new ArrayList<ScriptEntry>(entries);
        newEntries.add(newScript);
        save(newEntries);
    }

    public void removeScriptEntries(List<ScriptEntry> entries2) {
        ArrayList<ScriptEntry> newEntries = entries == null ? new ArrayList<ScriptEntry>() : new ArrayList<ScriptEntry>(entries);
        newEntries.removeAll(entries2);
        save(newEntries);
    }
}