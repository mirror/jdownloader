package org.jdownloader.nativ;

import java.io.IOException;

public class NativeRuntime {

    private static NativeRuntime runtime = null;

    public static NativeRuntime getRuntime() {
        if (runtime == null) {
            runtime = new NativeRuntime();
        }
        return runtime;
    }

    private NativeRuntime() {

    }

    public NativeProcess exec(final String command) throws Exception {
        return new NativeProcess(command, "");
    }

    public NativeProcess exec(final String cmdarray[]) throws IOException {
        return null;// exec(cmdarray, null, null);
    }

}
