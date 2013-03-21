package org.jdownloader.startup.commands;

import jd.SecondLevelLaunch;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;

public abstract class AbstractExtensionStartupCommand extends AbstractStartupCommand {

    public AbstractExtensionStartupCommand(String... commands) {
        super(commands);

    }

    @Override
    public final void run(final String command, final String... parameters) {
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                for (AbstractExtension ae : ExtensionController.getInstance().getEnabledExtensions()) {
                    try {
                        ae.handleCommand(command, parameters);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }

            }
        });

    }

}
