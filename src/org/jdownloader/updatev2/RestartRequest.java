package org.jdownloader.updatev2;

import org.appwork.shutdown.ShutdownRequest;

public interface RestartRequest extends ShutdownRequest {

    abstract public String[] getArguments();
}
