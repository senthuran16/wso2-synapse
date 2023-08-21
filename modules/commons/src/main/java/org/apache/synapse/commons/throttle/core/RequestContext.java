package org.apache.synapse.commons.throttle.core;

public class RequestContext {
    private long requestTime;

    public RequestContext(long requestTime) {
        this.requestTime = System.currentTimeMillis();
    }

    public long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }
}
