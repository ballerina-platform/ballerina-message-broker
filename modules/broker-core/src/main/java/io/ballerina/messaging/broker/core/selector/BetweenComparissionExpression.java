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

/**
 * Implementation of a boolean expression.This class is doing a comparison operation between the left with other two
 * values provided.if left value is between other two values it evaluate to a boolean value.
 * please refer the ![jms-selector-guide](../docs/user/jms-selector-guide.md).
 */
public class BetweenComparissionExpression implements BooleanExpression {

    private final Expression<Metadata> left;
    private final Expression<Metadata> value1;
    private final Expression<Metadata> value2;

    public BetweenComparissionExpression(Expression left, Expression value1, Expression value2) {

        this.left = left;
        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public boolean evaluate(Metadata metadata) {

        Object leftValue = left.evaluate(metadata);
        Object firstValue = value1.evaluate(metadata);
        Object secondValue = value2.evaluate(metadata);

        if (leftValue instanceof Number && firstValue instanceof Number && secondValue instanceof Number) {
            Class lv = leftValue.getClass();
            if ((lv == Integer.class) || (lv == Long.class)) {
                if ((firstValue instanceof Long) && (secondValue instanceof Long)) {
                    Long value = ((Number) leftValue).longValue();
                    Long value1 = ((Number) firstValue).longValue();
                    Long value2 = ((Number) secondValue).longValue();

                    return ((leftValue == firstValue) || (value > value1)) &&
                            ((leftValue == secondValue) || (value < value2));
                }
            }

            Double value = ((Number) leftValue).doubleValue();
            Double value1 = ((Number) firstValue).doubleValue();
            Double value2 = ((Number) secondValue).doubleValue();

            return ((leftValue == firstValue) || (value > value1)) && ((leftValue == secondValue) || (value < value2));

        }
        return false;
    }
}
