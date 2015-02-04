/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nutch.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;


/** Utility to create Hadoop {@link Configuration}s that include Nutch-specific
 * resources.  */
public class NutchConfiguration {
  public static final String UUID_KEY = "nutch.conf.uuid";
  
  private NutchConfiguration() {}                 // singleton
  
  /*
   * Configuration.hashCode() doesn't return values that
   * correspond to a unique set of parameters. This is a workaround
   * so that we can track instances of Configuration created by Nutch.
   */
  private static void setUUID(Configuration conf) {
    UUID uuid = UUID.randomUUID();
    conf.set(UUID_KEY, uuid.toString());
  }
  
  /**
   * Retrieve a Nutch UUID of this configuration object, or null
   * if the configuration was created elsewhere.
   * @param conf configuration instance
   * @return uuid or null
   */
  public static String getUUID(Configuration conf) {
    return conf.get(UUID_KEY);
  }

  /** Create a {@link Configuration} for Nutch. This will load the standard
   * Nutch resources, <code>nutch-default.xml</code> and
   * <code>nutch-site.xml</code> overrides.
   */
  public static Configuration create() {
    Configuration conf = new Configuration();
    List<URL> nutchConfigurationClasspathURLs = new ArrayList<URL>();

    // Collect classpath URLs from Hadoop's Configuration class CL
    URLClassLoader hadoopBundleConfigurationClassLoader = (URLClassLoader) conf.getClassLoader();
    for (URL hadoopBundleClasspathURL : hadoopBundleConfigurationClassLoader.getURLs()) {
      nutchConfigurationClasspathURLs.add(hadoopBundleClasspathURL);
    }

    // Append classpath URLs from current thread, which ostensibly include a Nutch job file
    URLClassLoader tccl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
    for (URL tcclClasspathURL : tccl.getURLs()) {
      nutchConfigurationClasspathURLs.add(tcclClasspathURL);
    }

    URLClassLoader nutchConfigurationClassLoader = new URLClassLoader(nutchConfigurationClasspathURLs.toArray(new URL[0]));
    // Reset the Configuration object's CL to the new one
    conf.setClassLoader(nutchConfigurationClassLoader);

    setUUID(conf);
    addNutchResources(conf);
    return conf;
  }
  
  /** Create a {@link Configuration} from supplied properties.
   * @param addNutchResources if true, then first <code>nutch-default.xml</code>,
   * and then <code>nutch-site.xml</code> will be loaded prior to applying the
   * properties. Otherwise these resources won't be used.
   * @param nutchProperties a set of properties to define (or override)
   */
  public static Configuration create(boolean addNutchResources, Properties nutchProperties) {
    Configuration conf = new Configuration();
    setUUID(conf);
    if (addNutchResources) {
      addNutchResources(conf);
    }
    for (Entry<Object, Object> e : nutchProperties.entrySet()) {
      conf.set(e.getKey().toString(), e.getValue().toString());
    }
    return conf;
  }

  /**
   * Add the standard Nutch resources to {@link Configuration}.
   * 
   * @param conf               Configuration object to which
   *                           configuration is to be added.
   */
  private static Configuration addNutchResources(Configuration conf) {
    conf.addResource("nutch-default.xml");
    conf.addResource("nutch-site.xml");
    return conf;
  }
}

