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

package org.apache.zeppelin.php;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.InterpreterResult.Type;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.apache.zeppelin.scheduler.Scheduler;
import org.apache.zeppelin.scheduler.SchedulerFactory;
import org.apache.zeppelin.interpreter.util.InterpreterOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 *
 */
public class PhpInterpreter extends Interpreter {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhpInterpreter.class);
  private final String shell = "php";
  private static final String TIMEOUT_PROPERTY = "shell.command.timeout.millisecs";
  private String DEFAULT_TIMEOUT_PROPERTY = "10000";
  ConcurrentHashMap<String, DefaultExecutor> executors;

  public PhpInterpreter(Properties property) {
    super(property);
  }

  @Override
  public void open() {
    LOGGER.info("Command timeout property: {}", getProperty(TIMEOUT_PROPERTY));
    executors = new ConcurrentHashMap<>();
  }

  @Override
  public void close() {
    for (String executorKey : executors.keySet()) {
      DefaultExecutor executor = executors.remove(executorKey);
      if (executor != null) {
        try {
          executor.getWatchdog().destroyProcess();
        } catch (Exception e){
          LOGGER.error("error destroying executor for paragraphId: " + executorKey, e);
        }
      }
    }
  }

  @Override
  public InterpreterResult interpret(String cmd, InterpreterContext contextInterpreter) {
    LOGGER.debug("Raw PHP input command '" + cmd + "'");

    OutputStream outStream = new ByteArrayOutputStream();

    CommandLine cmdLine = new CommandLine(this.shell);
    cmdLine.addArgument("-r");
    cmdLine.addArgument(cmd);

    DefaultExecutor executor = new DefaultExecutor();
    executor.setWatchdog(new ExecuteWatchdog(
      Long.valueOf(DEFAULT_TIMEOUT_PROPERTY)));
    executor.setStreamHandler(new PumpStreamHandler(outStream, contextInterpreter.out));
    try {
      int exitVal = executor.execute(cmdLine);
      LOGGER.debug(outStream.toString());
      return new InterpreterResult(Code.SUCCESS, outStream.toString());
    } catch (ExecuteException e) {
      LOGGER.debug("ExecuteException" + e.getMessage());
      return new InterpreterResult(Code.ERROR, e.getMessage());
    } catch (IOException e) {
      LOGGER.error("IOException: Can not run " + cmd, e);
      return new InterpreterResult(Code.ERROR, e.getMessage());
    }
    // DefaultExecutor executor = new DefaultExecutor();
    // executor.setStreamHandler(new PumpStreamHandler(
    //   outStream, contextInterpreter.out));
    // try {

    //   executor.setWatchdog(new ExecuteWatchdog(
    //       Long.valueOf(DEFAULT_TIMEOUT_PROPERTY)));

    //   executors.put(contextInterpreter.getParagraphId(), executor);
    //   // if (Boolean.valueOf(getProperty(DIRECTORY_USER_HOME))) {
    //   //   executor.setWorkingDirectory(new File(System.getProperty("user.home")));
    //   // }

    //   int exitVal = executor.execute(cmdLine);
    //   LOGGER.info("Paragraph " + contextInterpreter.getParagraphId()
    //     + " return with exit value: " + exitVal);
    //   return new InterpreterResult(Code.SUCCESS, outStream.toString());
    // } catch (ExecuteException e) {
    //   int exitValue = e.getExitValue();
    //   LOGGER.error("Can not run the following code " + cmd, e);
    //   String message = outStream.toString();
    //   // if (exitValue == 143) {
    //   //   code = Code.INCOMPLETE;
    //   //   message += "Paragraph received a SIGTERM\n";
    //   //   LOGGER.info("The paragraph " + contextInterpreter.getParagraphId()
    //   //     + " stopped executing: " + message);
    //   // }
    //   message += "ExitValue: " + exitValue;
    //   return new InterpreterResult(Code.ERROR, message);
    // } catch (IOException e) {
    //   LOGGER.error("Can not run " + cmd, e);
    //   return new InterpreterResult(Code.ERROR, e.getMessage());
    // } finally {
    //   executors.remove(contextInterpreter.getParagraphId());
    // }
  }

  @Override
  public void cancel(InterpreterContext context) {
  }

  @Override
  public FormType getFormType() {
    return FormType.NATIVE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override
  public List<InterpreterCompletion> completion(String buf, int cursor,
      InterpreterContext interpreterContext) {
    return new LinkedList<>();
  }

  @Override
  public Scheduler getScheduler() {
    return SchedulerFactory.singleton().createOrGetFIFOScheduler(
        PhpInterpreter.class.getName() + this.hashCode());
  }
}
