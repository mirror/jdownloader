package org.jdownloader.tests;

import org.appwork.storage.tests.RemoteAPI1InterfaceValidator;
import org.appwork.storage.tests.StorableValidator;

public class IDETestRunnerStorableValidator {
    public static void main(String[] args) throws Exception {
        RemoteAPI1InterfaceValidator.main(args);
        StorableValidator.main(args);
    }
}
