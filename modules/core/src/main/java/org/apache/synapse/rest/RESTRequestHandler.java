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

package org.apache.synapse.rest;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.api.rest.RestRequestHandler;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.version.ContextVersionStrategy;
import org.apache.synapse.rest.version.DefaultStrategy;
import org.apache.synapse.rest.version.URLBasedVersionStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is responsible for receiving requests from various sources and dispatching
 * them to a suitable REST API for further processing. This is the main entry point for
 * mediating messages through APIs and Resources.
 */
@Deprecated
public class RESTRequestHandler {

    private RestRequestHandler restRequestHandler = new RestRequestHandler();

    private static final Log log = LogFactory.getLog(RESTRequestHandler.class);

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
        return restRequestHandler.process(synCtx);
    }
}
