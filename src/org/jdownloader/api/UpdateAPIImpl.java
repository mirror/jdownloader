package org.jdownloader.api;

import org.jdownloader.api.update.UpdateAPI;
import org.jdownloader.updatev2.ForcedRestartRequest;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.UpdateController;

public class UpdateAPIImpl implements UpdateAPI {

    @Override
    public void restartAndUpdate() {

        RestartController.getInstance().asyncRestart(new ForcedRestartRequest("-forceupdate"));
    }

    @Override
    public boolean isUpdateAvailable() {
        return UpdateController.getInstance().hasPendingUpdates();
    }

}
