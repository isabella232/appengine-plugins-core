/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.appengine.operations;

import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.appengine.operations.cloudsdk.internal.args.GcloudArgs;
import com.google.cloud.tools.appengine.operations.cloudsdk.internal.process.ProcessBuilderFactory;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandler;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class GcloudRunner {

  private static final Logger logger = Logger.getLogger(GcloudRunner.class.getName());

  private final CloudSdk sdk;
  @Nullable private final String metricsEnvironment;
  @Nullable private final String metricsEnvironmentVersion;
  @Nullable private final Path credentialFile;
  @Nullable private final List<Path> flagsFiles;
  @Nullable private final String outputFormat;
  @Nullable private final String showStructuredLogs;
  @Nullable private final String verbosity;
  private final ProcessBuilderFactory processBuilderFactory;
  private final ProcessHandler processHandler;

  GcloudRunner(
      CloudSdk sdk,
      @Nullable String metricsEnvironment,
      @Nullable String metricsEnvironmentVersion,
      @Nullable Path credentialFile,
      @Nullable List<Path> flagsFiles,
      @Nullable String outputFormat,
      @Nullable String showStructuredLogs,
      @Nullable String verbosity,
      ProcessBuilderFactory processBuilderFactory,
      ProcessHandler processHandler) {
    this.sdk = sdk;
    this.metricsEnvironment = metricsEnvironment;
    this.metricsEnvironmentVersion = metricsEnvironmentVersion;
    this.credentialFile = credentialFile;
    this.flagsFiles = flagsFiles;
    this.outputFormat = outputFormat;
    this.showStructuredLogs = showStructuredLogs;
    this.verbosity = verbosity;
    this.processBuilderFactory = processBuilderFactory;
    this.processHandler = processHandler;
  }

  /**
   * Launch an external process that runs gcloud.
   *
   * @param workingDirectory if null then the working directory of current Java process
   */
  void run(List<String> arguments, @Nullable Path workingDirectory)
      throws ProcessHandlerException, CloudSdkNotFoundException, CloudSdkOutOfDateException,
          CloudSdkVersionFileException, IOException {

    sdk.validateCloudSdk();

    List<String> command = new ArrayList<>();
    command.add(sdk.getGCloudPath().toAbsolutePath().toString());

    command.addAll(arguments);
    if (outputFormat != null) {
      command.addAll(GcloudArgs.get("format", outputFormat));
    }

    if (verbosity != null) {
      command.addAll(GcloudArgs.get("verbosity", verbosity));
    }

    if (credentialFile != null) {
      command.addAll(GcloudArgs.get("credential-file-override", credentialFile));
    }

    if (flagsFiles != null) {
      for (Path flagFile : flagsFiles) {
        command.addAll(GcloudArgs.get("flags-file", flagFile));
      }
    }

    logger.info("submitting command: " + Joiner.on(" ").join(command));

    ProcessBuilder processBuilder = processBuilderFactory.newProcessBuilder();
    processBuilder.command(command);
    if (workingDirectory != null) {
      processBuilder.directory(workingDirectory.toFile());
    }
    processBuilder.environment().putAll(getGcloudCommandEnvironment());
    Process process = processBuilder.start();
    processHandler.handleProcess(process);
  }

  @VisibleForTesting
  Map<String, String> getGcloudCommandEnvironment() {
    Map<String, String> environment = Maps.newHashMap();
    if (credentialFile != null) {
      environment.put("CLOUDSDK_APP_USE_GSUTIL", "0");
    }
    if (metricsEnvironment != null) {
      environment.put("CLOUDSDK_METRICS_ENVIRONMENT", metricsEnvironment);
    }
    if (metricsEnvironmentVersion != null) {
      environment.put("CLOUDSDK_METRICS_ENVIRONMENT_VERSION", metricsEnvironmentVersion);
    }
    if (showStructuredLogs != null) {
      environment.put("CLOUDSDK_CORE_SHOW_STRUCTURED_LOGS", showStructuredLogs);
    }
    // This is to ensure IDE credentials get correctly passed to the gcloud commands, in Windows.
    // It's a temporary workaround until a fix is released.
    // https://github.com/GoogleCloudPlatform/google-cloud-intellij/issues/985
    if (System.getProperty("os.name").contains("Windows")) {
      environment.put("CLOUDSDK_APP_NUM_FILE_UPLOAD_PROCESSES", "1");
    }

    environment.put("CLOUDSDK_CORE_DISABLE_PROMPTS", "1");

    return environment;
  }

  static class Factory {
    private final ProcessBuilderFactory processBuilderFactory;

    Factory() {
      this(new ProcessBuilderFactory());
    }

    Factory(ProcessBuilderFactory processBuilderFactory) {
      this.processBuilderFactory = processBuilderFactory;
    }

    GcloudRunner newRunner(
        CloudSdk sdk,
        @Nullable String metricsEnvironment,
        @Nullable String metricsEnvironmentVersion,
        @Nullable Path credentialFile,
        @Nullable List<Path> flagsFiles,
        @Nullable String outputFormat,
        @Nullable String showStructuredLogs,
        @Nullable String verbosity,
        ProcessHandler processHandler) {
      return new GcloudRunner(
          sdk,
          metricsEnvironment,
          metricsEnvironmentVersion,
          credentialFile,
          flagsFiles,
          outputFormat,
          showStructuredLogs,
          verbosity,
          processBuilderFactory,
          processHandler);
    }
  }
}
