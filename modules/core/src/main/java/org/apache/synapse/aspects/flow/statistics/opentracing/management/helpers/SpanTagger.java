/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.opentracing.management.helpers;

import io.opentracing.Span;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.opentracing.OpenTracingManagerHolder;
import org.apache.synapse.aspects.flow.statistics.opentracing.models.SpanWrapper;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.Map;

/**
 * Applies tags to Spans.
 */
public class SpanTagger {

    /**
     * Prevents Instantiation.
     */
    private SpanTagger() {}

    private static final String PROPERTY_TRACE_VALUE_KEY = "componentValue";
    private static final String PROPERTY_MEDIATOR = "PropertyMediator";

    /**
     * Sets tags to the span which is contained in the provided span wrapper, from information acquired from the
     * given basic statistic data unit.
     * @param spanWrapper               Span wrapper that contains the target span.
     * @param basicStatisticDataUnit    Basic statistic data unit from which, tag data will be acquired.
     * @param synCtx                    Synapse message context
     */
    public static void setSpanTags(SpanWrapper spanWrapper, BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        StatisticsLog statisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
            if (OpenTracingManagerHolder.isCollectingPayloads() || OpenTracingManagerHolder.isCollectingProperties()) {
                StatisticDataUnit endEventDataUnit = (StatisticDataUnit) basicStatisticDataUnit;

                if (OpenTracingManagerHolder.isCollectingPayloads()) {
                    statisticsLog.setAfterPayload(endEventDataUnit.getPayload());
                    span.setTag("beforePayload", statisticsLog.getBeforePayload());
                    span.setTag("afterPayload", statisticsLog.getAfterPayload());
                }

                if (OpenTracingManagerHolder.isCollectingProperties()) {
                    if (spanWrapper.getStatisticDataUnit().getContextPropertyMap() != null) {
                        span.setTag("beforeContextPropertyMap",
                                spanWrapper.getStatisticDataUnit().getContextPropertyMap().toString());
                    }
                    if (statisticsLog.getContextPropertyMap() != null) {
                        span.setTag("afterContextPropertyMap", statisticsLog.getContextPropertyMap().toString());
                    }

                    if (statisticsLog.getComponentId().contains(PROPERTY_MEDIATOR) && synCtx != null) {
                        String propertyName = statisticsLog.getComponentId().split(":")[2];
                        Map<String, Object> headersMap =
                                ( Map<String, Object>) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                        if (!StringUtils.isEmpty((String) synCtx.getProperty(propertyName))) {
                            // Set value for Synapse properties
                            span.setTag(PROPERTY_TRACE_VALUE_KEY, (String) synCtx.getProperty(propertyName));
                        } else if (!StringUtils.isEmpty((String) ((Axis2MessageContext) synCtx).getAxis2MessageContext().
                                getProperty(propertyName))) {
                            // Set value for Axis properties
                            span.setTag(PROPERTY_TRACE_VALUE_KEY, (String) ((Axis2MessageContext) synCtx).
                                getAxis2MessageContext().getProperty(propertyName));
                        } else if (!StringUtils.isEmpty((String) headersMap.get(propertyName))) {
                            // Set value for Transport properties
                            span.setTag(PROPERTY_TRACE_VALUE_KEY, (String) headersMap.get(propertyName));
                        }
                    }
                }
            }

            span.setTag("componentName", statisticsLog.getComponentName());
            span.setTag("componentType", statisticsLog.getComponentTypeToString());
            span.setTag("threadId", Thread.currentThread().getId());
            span.setTag("componentId", statisticsLog.getComponentId());
            span.setTag("hashcode", statisticsLog.getHashCode());
        }
    }
}
