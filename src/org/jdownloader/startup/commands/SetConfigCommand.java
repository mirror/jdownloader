package org.jdownloader.startup.commands;

import jd.SecondLevelLaunch;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.config.InvalidValueException;

public class SetConfigCommand extends AbstractStartupCommand {

    public SetConfigCommand() {
        super("set");

    }

    @Override
    public void run(final String command, final String... parameters) {
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                if (parameters.length == 3) {
                    try {
                        RemoteAPIController.getInstance().getAdvancedConfigAPI().set(parameters[0], null, parameters[1], JSonStorage.restoreFromString(parameters[2], new TypeRef<Object>() {
                        }));
                        System.out.println("Set: " + parameters[0] + "." + parameters[1] + "=" + parameters[2]);
                    } catch (InvalidValueException e) {
                        System.out.println(Exceptions.getStackTrace(e));

                    }
                } else if (parameters.length == 4) {
                    try {
                        RemoteAPIController.getInstance().getAdvancedConfigAPI().set(parameters[0], parameters[1], parameters[2], JSonStorage.restoreFromString(parameters[3], new TypeRef<Object>() {
                        }));
                        System.out.println("Set: " + parameters[0] + "." + parameters[1] + "(" + parameters[2] + ")" + "=" + parameters[3]);
                    } catch (InvalidValueException e) {
                        System.out.println(Exceptions.getStackTrace(e));

                    }
                } else {
                    System.out.println("Set Config -set class [storageid] key value");
                }

            }
        });

    }

    @Override
    public String getDescription() {
        return "Set Config -set class key value";
    }

}
