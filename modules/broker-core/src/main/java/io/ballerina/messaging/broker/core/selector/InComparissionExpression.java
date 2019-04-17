/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.ballerina.messaging.broker.core.selector;

import io.ballerina.messaging.broker.core.Metadata;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 *Implementation of a boolean expression.This class is doing a comparison operation between the left and list of element
 *values provided.if left expression contain or not contain the element list values and it evaluate as boolean value.
 */
public class InComparissionExpression implements BooleanExpression {

    private final Expression<Metadata> left;
    private final List elements;

    public InComparissionExpression(Expression left, List elements) {

        this.left = left;
        this.elements = elements;
    }

    @Override
    public boolean evaluate(Metadata metadata) {

        Collection t = null;
        Object rvalue = left.evaluate(metadata);
        if (elements.size() == 0) {
            return false;
        }
        if (elements.size() < 5) {
            t = elements;
        }
        if (elements.size() > 5) {
            t = new HashSet<Object>(elements);
        }
        final Collection inList = t;
        if (rvalue.getClass() != String.class) {
            return false;
        }
        return ((inList != null) && inList.contains(rvalue));
    }
}
