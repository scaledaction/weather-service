/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.scaledaction.core.akka

import scala.util.Try
import com.typesafe.config.Config
import com.scaledaction.core.config.{ AppConfig, HasAppConfig }
import akka.util.Timeout

/**
 * Application settings. First attempts to acquire from the deploy environment.
 * If not exists, then from -D java system properties, else a default config.
 *
 * Settings in the environment such as: SPARK_HA_MASTER=local[10] is picked up first.
 *
 * Settings from the command line in -D will override settings in the deploy environment.
 * For example: sbt -Dspark.master="local[12]" run
 *
 * If you have not yet used Typesafe Config before, you can pass in overrides like so:
 *
 * {{{
 *   new Settings(ConfigFactory.parseString("""
 *      spark.master = "some.ip"
 *   """))
 * }}}
 *
 * Any of these can also be overriden by your own application.conf.
 *
 * @param conf Optional config for test
 */
class HttpServerConfig(
  val host: String,
  val port: Int,
  val requestTimeout: Timeout,
  rootConfig: Config) extends AppConfig(rootConfig: Config) {

  override def toString(): String = s"host: ${host}, port: ${port}, requestTimeout: ${requestTimeout}"
}

trait HasHttpServerConfig extends HasAppConfig {

  def getHttpServerConfig: HttpServerConfig = getHttpServerConfig(rootConfig.getConfig("http"))

  def getHttpServerConfig(rootName: String): HttpServerConfig = getHttpServerConfig(rootConfig.getConfig(rootName))

  //http {
  //  host = "localhost"
  //  host = ${?HTTP_HOST}  
  //  port = 8081
  //  port = ${?HTTP_PORT}  
  //}

  //spray.can {
  //  server {
  //    # If a request hasn't been responded to after the time period set here
  //    # a `spray.http.Timedout` message will be sent to the timeout handler.
  //    # Set to `infinite` to completely disable request timeouts.
  //    request-timeout = 20 s    
  //}

  private def getHttpServerConfig(http: Config): HttpServerConfig = {

    val host = getRequiredValue("HTTP_HOST", (http, "host"), "localhost")

    val port = getRequiredValue("HTTP_PORT", (http, "port"), 8080)

    val spray = rootConfig.getConfig("spray")
    val timeout = stringToDuration(getRequiredValue("HTTP_TIMEOUT", (spray, "can.server.request-timeout"), "20 s"))

    new HttpServerConfig(host, port, timeout, http)
  }
}
