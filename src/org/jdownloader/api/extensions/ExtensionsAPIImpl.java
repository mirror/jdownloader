package org.jdownloader.api.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.UninstalledExtension;
import org.jdownloader.updatev2.UpdateController;

public class ExtensionsAPIImpl implements ExtensionsAPI {

    @Override
    public boolean isInstalled(String id) {

        if (!UpdateController.getInstance().isHandlerSet()) {
            throw new WTFException("UpdateHandler not set");
        }
        return UpdateController.getInstance().isExtensionInstalled(id);
    }

    @Override
    public void install(final String id) {

        new Thread("Install Extension") {
            public void run() {
                try {
                    Dialog.getInstance().showConfirmDialog(0, "Extension Installation requested", "Do you want to install the " + id + "-extension?");

                    UpdateController.getInstance().setGuiVisible(true);
                    UpdateController.getInstance().runExtensionInstallation(id);

                    while (true) {
                        Thread.sleep(500);
                        if (!UpdateController.getInstance().isRunning()) {
                            break;
                        }
                        UpdateController.getInstance().waitForUpdate();

                    }
                } catch (Exception e) {
                    Log.exception(e);
                } finally {

                }
            }
        }.start();

    }

    @Override
    public boolean isEnabled(String id) {
        LazyExtension lazy = ExtensionController.getInstance().getExtension(id);
        return lazy != null && lazy._isEnabled();
    }

    @Override
    public List<ExtensionAPIStorable> list(final ExtensionQueryStorable query) {
        try {
            final List<LazyExtension> installedExtensions = ExtensionController.getInstance().getExtensions();
            final List<UninstalledExtension> uninstalledExtensions = ExtensionController.getInstance().getUninstalledExtensions();
            final ArrayList<ExtensionAPIStorable> result = new ArrayList<ExtensionAPIStorable>(installedExtensions.size() + uninstalledExtensions.size());
            if (result.size() == 0) {
                return result;
            }
            final Pattern cPat = StringUtils.isEmpty(query.getPattern()) ? null : Pattern.compile(query.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            for (LazyExtension installedExtension : installedExtensions) {
                if (cPat == null || cPat.matcher(installedExtension.getClassname()).matches()) {
                    ExtensionAPIStorable extensionStorable = new ExtensionAPIStorable();
                    extensionStorable.setId(installedExtension.getClassname());
                    if (query.isName()) {
                        extensionStorable.setName(installedExtension.getName());
                    }
                    if (query.isEnabled()) {
                        extensionStorable.setEnabled(installedExtension._isEnabled());
                    }
                    if (query.isInstalled()) {
                        extensionStorable.setInstalled(true);
                    }
                    if (query.isName()) {
                        extensionStorable.setName(installedExtension.getName());
                    }
                    if (query.isDescription()) {
                        extensionStorable.setDescription(installedExtension.getDescription());
                    }
                    if (query.isConfigInterface()) {
                        extensionStorable.setDescription(installedExtension.getConfigInterface());
                    }
                    if (query.isIconKey()) {
                        extensionStorable.setName(installedExtension._getExtension().getIconKey());
                    }
                    result.add(extensionStorable);
                }
            }
            for (UninstalledExtension uninstalledExtension : uninstalledExtensions) {
                if (cPat == null || cPat.matcher(uninstalledExtension.getId()).matches()) {
                    ExtensionAPIStorable extensionStorable = new ExtensionAPIStorable();
                    extensionStorable.setId(uninstalledExtension.getId());
                    if (query.isName()) {
                        extensionStorable.setName(uninstalledExtension.getName());
                    }
                    if (query.isEnabled()) {
                        extensionStorable.setEnabled(false);
                    }
                    if (query.isInstalled()) {
                        extensionStorable.setInstalled(false);
                    }
                    if (query.isName()) {
                        extensionStorable.setName(uninstalledExtension.getName());
                    }
                    if (query.isDescription()) {
                        extensionStorable.setDescription(uninstalledExtension.getDescription());
                    }
                    if (query.isConfigInterface()) {
                        extensionStorable.setDescription(null);
                    }
                    if (query.isIconKey()) {
                        extensionStorable.setName(uninstalledExtension.getIconKey());
                    }
                    result.add(extensionStorable);
                }
            }
            return result;
        } catch (Exception e) {
            throw new WTFException();
        }
    }

    @Override
    public void setEnabled(String id, boolean b) {
        LazyExtension lazy = ExtensionController.getInstance().getExtension(id);
        ExtensionController.getInstance().setEnabled(lazy, b);
    }

}
