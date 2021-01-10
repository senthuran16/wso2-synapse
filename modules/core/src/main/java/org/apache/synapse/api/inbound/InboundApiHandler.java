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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.api.AbstractApiHandler;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.API;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * This class is responsible for receiving requests from various sources and dispatching
 * them to a suitable REST API for further processing. This is the main entry point for
 * mediating messages through APIs and Resources.
 */
public class InboundApiHandler extends AbstractApiHandler {

    private static final Log log = LogFactory.getLog(InboundApiHandler.class);

    public boolean process(MessageContext synCtx) { // TODO set endpoint to ctx, via carbon-apimgt
        if (synCtx.isResponse()) {
            return dispatchToAPI(synCtx);
        }
        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        String protocol = msgCtx.getIncomingTransportName();
        if (!Arrays.asList("ws", "wss", "http", "https").contains(protocol)) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid protocol for Inbound API mediation: " + protocol);
            }
            return false;
        }
        return dispatchToAPI(synCtx);
    }

    @Override
    protected boolean dispatchToAPI(MessageContext synCtx) {
        Object inboundEndpointName = synCtx.getProperty(SynapseConstants.INBOUND_ENDPOINT_NAME);
        if (inboundEndpointName == null) {
            return false;
        }
        Map<String, API> apis = synCtx.getEnvironment().getSynapseConfiguration()
                .getInboundApiMappings().get(inboundEndpointName.toString());
        if (apis == null) {
            return dispatchToAPI(Collections.<API>emptyList(), synCtx);
        }
        return dispatchToAPI(apis.values(), synCtx);
    }
}
