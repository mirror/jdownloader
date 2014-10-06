package org.jdownloader.extensions.schedulerV2.actions;

public class SetChunksActionConfig implements IScheduleActionConfig {
    public SetChunksActionConfig() {
    }

    public int getChunks() {
        return chunks;
    }

    public void setChunks(int chunks) {
        this.chunks = chunks;
    }

    private int chunks = 0;

}
