/*
 * SonarLint Core - Implementation
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.core.plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.picocontainer.Startable;
import org.sonar.api.SonarPlugin;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Orchestrates the installation and loading of plugins
 */
public class DefaultPluginRepository implements Startable {
  private static final Logger LOG = Loggers.get(DefaultPluginRepository.class);

  private final PluginCopier installer;
  private final PluginLoader loader;

  private Map<String, SonarPlugin> pluginInstancesByKeys;
  private Map<String, PluginInfo> infosByKeys;

  public DefaultPluginRepository(PluginCopier installer, PluginLoader loader) {
    this.installer = installer;
    this.loader = loader;
  }

  @Override
  public void start() {
    infosByKeys = Maps.newHashMap(installer.installRemotes());
    pluginInstancesByKeys = Maps.newHashMap(loader.load(infosByKeys));

    logPlugins();
  }

  private void logPlugins() {
    if (infosByKeys.isEmpty()) {
      LOG.debug("No plugins loaded");
    } else {
      LOG.debug("Plugins:");
      for (PluginInfo p : infosByKeys.values()) {
        LOG.debug("  * {} {} ({})", p.getName(), p.getVersion(), p.getKey());
      }
    }
  }

  @Override
  public void stop() {
    // close plugin classloaders
    loader.unload(pluginInstancesByKeys.values());

    pluginInstancesByKeys.clear();
    infosByKeys.clear();
  }

  public Collection<PluginInfo> getPluginInfos() {
    return infosByKeys.values();
  }

  public PluginInfo getPluginInfo(String key) {
    PluginInfo info = infosByKeys.get(key);
    Preconditions.checkState(info != null, String.format("Plugin [%s] does not exist", key));
    return info;
  }

  public SonarPlugin getPluginInstance(String key) {
    SonarPlugin instance = pluginInstancesByKeys.get(key);
    Preconditions.checkState(instance != null, String.format("Plugin [%s] does not exist", key));
    return instance;
  }

  public boolean hasPlugin(String key) {
    return infosByKeys.containsKey(key);
  }
}
