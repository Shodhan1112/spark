/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.execution

import java.util.Locale

import scala.language.reflectiveCalls

import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst.plans.logical.{LogicalPlan, OneRowRelation}
import org.apache.spark.sql.test.SharedSQLContext

class QueryExecutionSuite extends SharedSQLContext {
  test("toString() exception/error handling") {
    val badRule = new SparkStrategy {
      var mode: String = ""
      override def apply(plan: LogicalPlan): Seq[SparkPlan] =
        mode.toLowerCase(Locale.ROOT) match {
          case "exception" => throw new AnalysisException(mode)
          case "error" => throw new Error(mode)
          case _ => Nil
        }
    }
    spark.experimental.extraStrategies = badRule :: Nil

    def qe: QueryExecution = new QueryExecution(spark, OneRowRelation)

    // Nothing!
    badRule.mode = ""
    assert(qe.toString.contains("OneRowRelation"))

    // Throw an AnalysisException - this should be captured.
    badRule.mode = "exception"
    assert(qe.toString.contains("org.apache.spark.sql.AnalysisException"))

    // Throw an Error - this should not be captured.
    badRule.mode = "error"
    val error = intercept[Error](qe.toString)
    assert(error.getMessage.contains("error"))
  }
}
