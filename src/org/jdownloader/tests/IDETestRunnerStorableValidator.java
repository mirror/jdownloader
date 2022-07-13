package org.jdownloader.tests;

import org.appwork.storage.tests.RemoteAPI1InterfaceValidator;
import org.appwork.storage.tests.StorableValidatorTest;

public class IDETestRunnerStorableValidator {
    public static void main(String[] args) throws Exception {
        RemoteAPI1InterfaceValidator.main(args);
        StorableValidatorTest.main(args);
    }
}
