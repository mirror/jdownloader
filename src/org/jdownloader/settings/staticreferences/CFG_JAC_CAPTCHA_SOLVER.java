package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.captcha.v2.solver.jac.JACSolverConfig;

public class CFG_JAC_CAPTCHA_SOLVER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(JACSolverConfig.class);
    }

    // Static Mappings for interface org.jdownloader.captcha.v2.solver.jac.JACSolverConfig
    public static final JACSolverConfig                 CFG                        = JsonConfig.create(JACSolverConfig.class);
    public static final StorageHandler<JACSolverConfig> SH                         = (StorageHandler<JACSolverConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final BooleanKeyHandler               ENABLED                    = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                WAIT_FOR_MAP               = SH.getKeyHandler("WaitForMap", ObjectKeyHandler.class);

    /**
     * Do not Change me unless you know 100000% what this value is used for!
     **/
    public static final IntegerKeyHandler               DEFAULT_JACTRUST_THRESHOLD = SH.getKeyHandler("DefaultJACTrustThreshold", IntegerKeyHandler.class);

    public static final ObjectKeyHandler                JACTHRESHOLD               = SH.getKeyHandler("JACThreshold", ObjectKeyHandler.class);
}