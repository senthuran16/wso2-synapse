package org.apache.synapse.commons.throttle.core.internal;

import org.apache.synapse.commons.throttle.core.CallerConfiguration;
import org.apache.synapse.commons.throttle.core.CallerContext;
import org.apache.synapse.commons.throttle.core.ThrottleContext;

public interface DistributedThrottleProcessor {

    public boolean canAccessBasedOnUnitTime(CallerContext callerContext, CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime);

    public boolean canAccessIfUnitTimeNotOver(CallerContext callerContext, CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime);

    public boolean canAccessIfUnitTimeOver(CallerContext callerContext, CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime);

    public void syncThrottleCounterParams(CallerContext callerContext, boolean incrementLocalCounter, long currentTime);

    public void syncThrottleWindowParams(CallerContext callerContext, boolean isInvocationFlow);

    public String getType();

    public boolean isEnable();
}
