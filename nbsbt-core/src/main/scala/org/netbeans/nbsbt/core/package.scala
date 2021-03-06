/*
 * Copyright 2011 Typesafe Inc.
 *
 * This work is based on the original contribution of WeigleWilczek.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.netbeans.nbsbt

import java.util.Properties
import sbt.internal.BuildStructure
import sbt.{
  Configuration,
  Configurations,
  Extracted,
  EvaluateTask,
  File,
  Inc,
  Incomplete,
  Project,
  ProjectRef,
  Reference,
  Result,
  TaskKey,
  SettingKey,
  State,
  Task,
  Value
}

import sbt.internal.BuildStructure
import sbt.complete.Parser
import scalaz.{ Equal, NonEmptyList, Validation => ScalazValidation }
import scalaz.Scalaz._
import scalaz._
import scalaz.syntax.ValidationOps
import scalaz.syntax.ValidationOps._
import scalaz.syntax.validation._

package object core {

  val NewLine = System.getProperty("line.separator")

  val FileSep = System.getProperty("file.separator")

  implicit val fileEqual = new Equal[File] {
    def equal(file1: File, file2: File): Boolean = file1 == file2
  }

  def id[A](a: A): A = a

  def boolOpt(key: String): Parser[(String, Boolean)] = {
    import sbt.complete.DefaultParsers._
    (Space ~> key ~ ("=" ~> ("true" | "false"))) map { case (k, v) => k -> v.toBoolean }
  }

  def setting[A](key: SettingKey[A], state: State): Validation[A] =
    key get structure(state).data match {
      case Some(a) => Validation.success(a)
      case None => Validation.failureNel("Undefined setting '%s'!".format(key.key))
    }

  def evaluateTask[A](key: TaskKey[A], ref: ProjectRef, state: State): Validation[A] = {
    val extracted = Project.extract(state)
    val defaultCfg = EvaluateTask.extractedTaskConfig(extracted, extracted.structure, state)

    EvaluateTask(structure(state), key, state, ref, defaultCfg) match {
      case Some((_, Value(a))) => Validation.success(a)
      case Some((_, Inc(inc))) => Validation.failureNel("Error evaluating task '%s': %s".format(key.key, Incomplete.show(inc.tpe)))
      case None => Validation.failureNel("Undefined task '%s' for '%s'!".format(key.key, ref.project))
    }
  }

  def extracted(state: State): Extracted = Project.extract(state)

  def structure(state: State): BuildStructure = extracted(state).structure

  type Validation[A] = ScalazValidation[NonEmptyList[String], A]
}
