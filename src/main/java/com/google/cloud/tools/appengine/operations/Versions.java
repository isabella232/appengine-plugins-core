/*
 * Copyright 2016 Google LLC.
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

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.configuration.VersionsListConfiguration;
import com.google.cloud.tools.appengine.configuration.VersionsSelectionConfiguration;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.appengine.operations.cloudsdk.internal.args.GcloudArgs;
import com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Run various gcloud versions commands. */
public class Versions {

  private final GcloudRunner runner;

  Versions(GcloudRunner runner) {
    this.runner = runner;
  }

  private void execute(List<String> arguments) throws AppEngineException {
    try {
      runner.run(arguments, null);
    } catch (ProcessHandlerException | IOException ex) {
      throw new AppEngineException(ex);
    }
  }

  /**
   * Starts serving a specific version or versions.
   *
   * @throws CloudSdkNotFoundException when the Cloud SDK is not installed where expected
   * @throws CloudSdkOutOfDateException when the installed Cloud SDK is too old
   */
  public void start(VersionsSelectionConfiguration configuration) throws AppEngineException {
    Preconditions.checkNotNull(configuration);
    Preconditions.checkNotNull(configuration.getVersions());
    Preconditions.checkArgument(configuration.getVersions().size() > 0);

    List<String> arguments = new ArrayList<>();
    arguments.add("app");
    arguments.add("versions");
    arguments.add("start");
    arguments.addAll(commonVersionSelectionArgs(configuration));

    execute(arguments);
  }

  /**
   * Stops serving a specific version or versions.
   *
   * @throws CloudSdkNotFoundException when the Cloud SDK is not installed where expected
   * @throws CloudSdkOutOfDateException when the installed Cloud SDK is too old
   */
  public void stop(VersionsSelectionConfiguration configuration) throws AppEngineException {
    Preconditions.checkNotNull(configuration);
    Preconditions.checkNotNull(configuration.getVersions());
    Preconditions.checkArgument(configuration.getVersions().size() > 0);

    List<String> arguments = new ArrayList<>();
    arguments.add("app");
    arguments.add("versions");
    arguments.add("stop");
    arguments.addAll(commonVersionSelectionArgs(configuration));

    execute(arguments);
  }

  /**
   * Deletes a specified version or versions.
   *
   * @throws CloudSdkNotFoundException when the Cloud SDK is not installed where expected
   * @throws CloudSdkOutOfDateException when the installed Cloud SDK is too old
   */
  public void delete(VersionsSelectionConfiguration configuration) throws AppEngineException {
    Preconditions.checkNotNull(configuration);
    Preconditions.checkNotNull(configuration.getVersions());
    Preconditions.checkArgument(configuration.getVersions().size() > 0);

    List<String> arguments = new ArrayList<>();
    arguments.add("app");
    arguments.add("versions");
    arguments.add("delete");
    arguments.addAll(commonVersionSelectionArgs(configuration));

    execute(arguments);
  }

  /**
   * Lists the versions for a service, or every version of every service if no service is specified.
   *
   * @throws CloudSdkNotFoundException when the Cloud SDK is not installed where expected
   * @throws CloudSdkOutOfDateException when the installed Cloud SDK is too old
   */
  public void list(VersionsListConfiguration configuration) throws AppEngineException {
    Preconditions.checkNotNull(configuration);

    List<String> arguments = new ArrayList<>();
    arguments.add("app");
    arguments.add("versions");
    arguments.add("list");
    arguments.addAll(GcloudArgs.get("service", configuration.getService()));
    arguments.addAll(GcloudArgs.get("hide-no-traffic", configuration.getHideNoTraffic()));
    arguments.addAll(GcloudArgs.get("project", configuration.getProjectId()));

    execute(arguments);
  }

  private static List<String> commonVersionSelectionArgs(
      VersionsSelectionConfiguration configuration) {
    List<String> arguments = new ArrayList<>();
    arguments.addAll(configuration.getVersions());
    arguments.addAll(GcloudArgs.get("service", configuration.getService()));
    arguments.addAll(GcloudArgs.get("project", configuration.getProjectId()));

    return arguments;
  }
}
