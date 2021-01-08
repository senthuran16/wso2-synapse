package org.apache.synapse.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.rest.version.ContextVersionStrategy;
import org.apache.synapse.rest.version.DefaultStrategy;
import org.apache.synapse.rest.version.URLBasedVersionStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractApiHandler { // TODO Extend RESTRequestHandler and InboundApiHandler here
    private static final Log log = LogFactory.getLog(AbstractApiHandler.class);

    /**
     * Attempt to process the given message through one of the available APIs. This method
     * will first try to locate a suitable API for the given message by running it through
     * the API validation routines available. If a matching API is found it will dispatch
     * the message to the located API. If a matching API cannot be found, message will be
     * left intact so any other handlers (eg: main sequence) can pick it up later.
     *
     * @param synCtx MessageContext of the request to be processed
     * @return true if the message was dispatched to an API and false otherwise
     */
    public abstract boolean process(MessageContext synCtx);

    protected abstract boolean dispatchToAPI(MessageContext synCtx);

    protected boolean dispatchToAPI(Collection<API> apiSet, MessageContext synCtx) {
        //Since swapping elements are not possible with sets, Collection is converted to a List
        List<API> defaultStrategyApiSet = new ArrayList<API>(apiSet);
        API defaultAPI = null;
        if (null != synCtx.getProperty(RESTConstants.IS_PROMETHEUS_ENGAGED)) {
            API api = (API) synCtx.getProperty(RESTConstants.PROCESSED_API);
            if (identifyAPI(api, synCtx, defaultStrategyApiSet)) {
                return true;
            }
        } else {
            Object apiObject = synCtx.getProperty(RESTConstants.PROCESSED_API);
            if (apiObject != null) {
                API api = (API) synCtx.getProperty(RESTConstants.PROCESSED_API);
                if (identifyAPI(api, synCtx, defaultStrategyApiSet)) {
                    return true;
                }
            } else {
                for (API api : apiSet) {
                    if (identifyAPI(api, synCtx, defaultStrategyApiSet)) {
                        return true;
                    }
                }
            }
        }

        for (API api : defaultStrategyApiSet) {
            api.setLogSetterValue();
            if (api.canProcess(synCtx)) {
                if (log.isDebugEnabled()) {
                    log.debug("Located specific API: " + api.getName() + " for processing message");
                }
                apiProcess(synCtx, api);
                return true;
            }
        }

        if (defaultAPI != null && defaultAPI.canProcess(synCtx)) {
            defaultAPI.setLogSetterValue();
            apiProcess(synCtx, defaultAPI);
            return true;
        }

        return false;
    }

    protected void apiProcess(MessageContext synCtx, API api) {
        Integer statisticReportingIndex = 0;
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            statisticReportingIndex = OpenEventCollector
                    .reportEntryEvent(synCtx, api.getAPIName(), api.getAspectConfiguration(), ComponentType.API);
            api.process(synCtx);
            CloseEventCollector.tryEndFlow(synCtx, api.getAPIName(), ComponentType.API, statisticReportingIndex, true);
        } else {
            api.process(synCtx);
        }
    }

    //Process APIs which have context or url strategy
    protected void apiProcessNonDefaultStrategy(MessageContext synCtx, API api) {
        Integer statisticReportingIndex = 0;
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            statisticReportingIndex = OpenEventCollector
                    .reportEntryEvent(synCtx, api.getAPIName() + "_" + api.getVersion(), api.getAspectConfiguration(),
                            ComponentType.API);
            api.process(synCtx);
            CloseEventCollector.tryEndFlow(synCtx, api.getAPIName(), ComponentType.API, statisticReportingIndex, true);
        } else {
            api.process(synCtx);
        }
    }

    protected boolean identifyAPI(API api, MessageContext synCtx, List defaultStrategyApiSet) {
        API defaultAPI = null;
        api.setLogSetterValue();
        if ("/".equals(api.getContext())) {
            defaultAPI = api;
        } else if (api.getVersionStrategy().getClass().getName().equals(DefaultStrategy.class.getName())) {
            //APIs whose VersionStrategy is bound to an instance of DefaultStrategy, should be skipped and processed at
            // last.Otherwise they will be always chosen to process the request without matching the version.
            defaultStrategyApiSet.add(api);
        } else if (api.getVersionStrategy().getClass().getName().equals(ContextVersionStrategy.class.getName())
                || api.getVersionStrategy().getClass().getName().equals(URLBasedVersionStrategy.class.getName())) {
            api.setLogSetterValue();
            if (api.canProcess(synCtx)) {
                if (log.isDebugEnabled()) {
                    log.debug("Located specific API: " + api.getName() + " for processing message");
                }
                apiProcessNonDefaultStrategy(synCtx, api);
                return true;
            }
        } else if (api.canProcess(synCtx)) {
            if (log.isDebugEnabled()) {
                log.debug("Located specific API: " + api.getName() + " for processing message");
            }
            api.process(synCtx);
            return true;
        }
        return false;
    }
}
