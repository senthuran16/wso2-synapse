package org.apache.synapse.commons.throttle.core;

public class RequestContext {

    public RequestContext(long requestTime) {
        this.requestTime = System.currentTimeMillis();
    }
    private long requestTime;

    public long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }
}
