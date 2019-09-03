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

package org.apache.synapse.aspects.flow.statistics.tracing.store;

import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

import java.util.Objects;

public class SpanId {
    private int index;
    private String name;

    public SpanId(StatisticDataUnit statisticDataUnit) {
        this.index = statisticDataUnit.getCurrentIndex();
        this.name = statisticDataUnit.getComponentName();
    }

    public SpanId(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpanId spanId = (SpanId) o;
        return index == spanId.index &&
                name.equals(spanId.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, name);
    }
}
