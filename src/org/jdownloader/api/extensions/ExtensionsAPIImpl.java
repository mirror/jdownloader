package org.jdownloader.api.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
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
    public boolean install(final String id) {

        new Thread("Install Extension") {
            public void run() {
                try {
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
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                } finally {

                }
            }
        }.start();
        return true;
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
            if (installedExtensions.size() + uninstalledExtensions.size() == 0) {
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
                    if (query.isDescription()) {
                        extensionStorable.setDescription(installedExtension.getDescription());
                    }
                    if (query.isConfigInterface()) {
                        extensionStorable.setConfigInterface(installedExtension.getConfigInterface());
                    }
                    if (query.isIconKey()) {
                        extensionStorable.setIconKey(installedExtension.getIconPath());
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
                    if (query.isDescription()) {
                        extensionStorable.setDescription(uninstalledExtension.getDescription());
                    }
                    if (query.isConfigInterface()) {
                        extensionStorable.setConfigInterface(null);
                    }
                    if (query.isIconKey()) {
                        extensionStorable.setIconKey(uninstalledExtension.getIconKey());
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
    public boolean setEnabled(String id, boolean b) {
        LazyExtension lazy = ExtensionController.getInstance().getExtension(id);
        if (lazy == null) {
            return false;
        } else {
            ExtensionController.getInstance().setEnabled(lazy, b);
            return true;
        }
    }

}
