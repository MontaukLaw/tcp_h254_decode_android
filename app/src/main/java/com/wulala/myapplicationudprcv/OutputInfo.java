package com.wulala.myapplicationudprcv;

public class OutputInfo {

    private long ts;
    private int index;
    private long outputTs;

    public OutputInfo(long ts, long outputTs, int index) {
        this.ts = ts;
        this.index = index;
        this.outputTs = outputTs;
    }

    public long getOutputTs() {
        return outputTs;
    }

    public void setOutputTs(long outputTs) {
        this.outputTs = outputTs;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "OutputInfo{" +
                "ts=" + ts +
                ", index=" + index +
                ", outputTs=" + outputTs +
                '}';
    }
}
