/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.api.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.RESTRequestHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for receiving requests from various sources and dispatching
 * them to a suitable REST API for further processing. This is the main entry point for
 * mediating messages through APIs and Resources.
 */
public class InboundApiHandler extends RESTRequestHandler { // TODO introduce a common Interface 'RequestHandler' with #process() , which is implemented by both REST and InboundAPI handlers

    private static final Log log = LogFactory.getLog(InboundApiHandler.class);

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
    public boolean process(MessageContext synCtx) { // TODO set endpoint to ctx, via carbon-apimgt

        if (synCtx.isResponse()) {
            return dispatchToAPIs(synCtx);
        }

        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        String protocol = msgCtx.getIncomingTransportName();
        if (!"ws".equals(protocol) && !"wss".equals(protocol)) { // TODO allow http & https too // allowedProtocols[]
            if (log.isDebugEnabled()) {
                log.debug("Invalid protocol for Inbound API mediation: " + protocol);
            }
            return false;
        }

        return dispatchToAPIs(synCtx);
    }

    private boolean dispatchToAPIs(MessageContext synCtx) {
        Map<String, Map<String, API>> inboundApiMappings = synCtx.getEnvironment().getSynapseConfiguration()
                .getApiLevelInboundApiMappings();
        Map<String, Map<String, API>> implicitInboundApiMappings = synCtx.getEnvironment().getSynapseConfiguration()
                .getResourceLevelInboundApiMappings();

        Object inboundEndpointName = synCtx.getProperty("inbound.endpoint.name");
        if (inboundEndpointName == null || (inboundApiMappings.isEmpty() && implicitInboundApiMappings.isEmpty())) {
            return false; // Inbound API mapping and dispatching is not applicable.
        }

        String endpointName = inboundEndpointName.toString();

        List<API> apiList = new ArrayList<>();
        Map<String, API> mappedApis = inboundApiMappings.get(endpointName);
        if (mappedApis != null) {
            apiList.addAll(mappedApis.values());
        }
        Map<String, API> implicitlyMappedApis = implicitInboundApiMappings.get(endpointName);
        if (implicitlyMappedApis != null) {
            apiList.addAll(implicitlyMappedApis.values());
        }

        return dispatchToAPI(apiList, synCtx);

//        Map<String, List<API>> apiMappings = synCtx.getEnvironment().getSynapseConfiguration()
//                .getInboundAPIMappingMap();
//        Object inboundEndpointName = synCtx.getProperty("inbound.endpoint.name");
//        if (apiMappings != null && inboundEndpointName != null) {
//            String endpointName = inboundEndpointName.toString();
//            List<API> apiList = apiMappings.get(endpointName);
//            if (apiList != null) {
//                return dispatchToAPI(apiList, synCtx);
//            }
//        }
//        return false;
    }
}
