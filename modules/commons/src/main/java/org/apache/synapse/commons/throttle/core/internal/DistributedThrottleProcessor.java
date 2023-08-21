package org.apache.synapse.commons.throttle.core.internal;

import org.apache.synapse.commons.throttle.core.CallerConfiguration;
import org.apache.synapse.commons.throttle.core.CallerContext;
import org.apache.synapse.commons.throttle.core.RequestContext;
import org.apache.synapse.commons.throttle.core.ThrottleContext;

public interface DistributedThrottleProcessor {

    public boolean canAccessBasedOnUnitTime(CallerContext callerContext, CallerConfiguration configuration, ThrottleContext throttleContext, RequestContext requestContext);

    public boolean canAccessIfUnitTimeNotOver(CallerContext callerContext, CallerConfiguration configuration, ThrottleContext throttleContext, RequestContext requestContext);

    public boolean canAccessIfUnitTimeOver(CallerContext callerContext, CallerConfiguration configuration, ThrottleContext throttleContext, RequestContext requestContext);

    public void syncThrottleCounterParams(CallerContext callerContext, boolean incrementLocalCounter, RequestContext requestContext);

    public void syncThrottleWindowParams(CallerContext callerContext, boolean isInvocationFlow);

    public String getType();

    public boolean isEnable();
}
