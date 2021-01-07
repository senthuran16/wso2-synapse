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

package org.apache.synapse.api.rest;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.api.AbstractApiHandler;
import org.apache.synapse.api.ApiConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.API;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for receiving requests from various sources and dispatching
 * them to a suitable REST API for further processing. This is the main entry point for
 * mediating messages through APIs and Resources.
 */
public class RestRequestHandler extends AbstractApiHandler {

    private static final Log log = LogFactory.getLog(RestRequestHandler.class);

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
    public boolean process(MessageContext synCtx) {

        if (synCtx.isResponse()) {
            return dispatchToAPI(synCtx);
        }

        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        String protocol = msgCtx.getIncomingTransportName();
        if (!Constants.TRANSPORT_HTTP.equals(protocol) && !Constants.TRANSPORT_HTTPS.equals(protocol)) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid protocol for REST API mediation: " + protocol);
            }
            return false;
        }

        return dispatchToAPI(synCtx);
    }

//    @Override // TODO remove if finalized
//    protected boolean dispatchToAPI(MessageContext synCtx) {
//        Map<String, List<API>> inboundApiMappings = synCtx.getEnvironment().getSynapseConfiguration()
//                .getInboundApiMappings();
//        List<API> apiSet = inboundApiMappings.get(ApiConstants.DEFAULT_BINDING_ENDPOINT_NAME);
//        if (apiSet == null) {
//            return dispatchToAPI(Collections.<API>emptyList(), synCtx);
//        }
//        return dispatchToAPI(apiSet, synCtx);
//    }

    @Override
    protected boolean dispatchToAPI(MessageContext synCtx) {
        Map<String, Map<String, API>> inboundApiMappings = synCtx.getEnvironment().getSynapseConfiguration()
                .getInboundApiMappings();
        // TODO can we safely rely on dispatching to "default" in normal mode?
        //  - Observers when initting - Handlers when parsing
        // TODO if not, we can add to api table only if no bindings are returned out of 'createInboundApis'
        Map<String, API> apis = inboundApiMappings.get(ApiConstants.DEFAULT_BINDING_ENDPOINT_NAME);
        if (apis == null) {
            return dispatchToAPI(Collections.<API>emptyList(), synCtx);
        }
        return dispatchToAPI(apis.values(), synCtx);
    }
}
