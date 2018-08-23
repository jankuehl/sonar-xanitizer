/** 
 * SonarQube Xanitizer Plugin
 * Copyright 2012-2018 by RIGS IT GmbH, Switzerland, www.rigs-it.ch.
 * mailto: info@rigs-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Created on 23.08.2018
 *
 */
package com.rigsit.xanitizer.sqplugin;

import java.io.File;
import java.util.Optional;

import org.sonar.api.PropertyType;
import org.sonar.api.Plugin.Context;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.rigsit.xanitizer.sqplugin.util.PluginUtil;

/**
 * @author nwe
 *
 */
public class XanitizerProperties {

	public static final String XANITIZER_XML_REPORT_FILE = "sonar.xanitizer.xmlReportFile";
	private static final String REPORT_FILE_DEFAULT = "Xanitizer-Findings-List.xml";
	private static final String REPORT_FILE_NAME = "Xanitizer  Report File";
	private static final String REPORT_FILE_DESCRIPTION = "Path (absolut or relative) to the xml file with Xanitizer issues.</BR></BR>"
			+ " Generated by Xanitizer, either from the GUI "
			+ "'Reporting > Generate Findings List Report...', and choosing the XML output format,"
			+ " or by running headless, for example using ANT or Maven.";

	public static final String XANITIZER_IMPORT_ALL_FINDINGS = "sonar.xanitizer.importAllFindings";
	private static final String IMPORT_ALL_DEFAULT = "false";
	private static final String IMPORT_ALL_NAME = "Xanitizer Import All";
	private static final String IMPORT_ALL_DESCRIPTION = "By default, the Xanitizer plugin only creates an issue for a finding, when a corresponding source code position can be detected in"
			+ "the context of SonarQube. Additionally, findings from integrated tools, that have their own SonarQube plugin, like OWASP "
			+ "Dependency Check or SpotBugs, are ignored.</BR></BR>"
			+ "To enforce that SonarQube also contains issues for these findings so that you see the same numbers in SonarQube as in Xanitizer, "
			+ "set this option to 'true'.";

	private static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";
	private static final String JAVA_SUBCATEGORY = "Java";

	private static final Logger LOG = Loggers.get(XanitizerProperties.class);
	
	
	private XanitizerProperties() {
		// hide default constructor
	}
	

	public static void define(final Context context) {
		context.addExtension(PropertyDefinition.builder(XANITIZER_XML_REPORT_FILE)
				.name(REPORT_FILE_NAME).description(REPORT_FILE_DESCRIPTION)
				.category(EXTERNAL_ANALYZERS_CATEGORY).subCategory(JAVA_SUBCATEGORY)
				.onQualifiers(Qualifiers.PROJECT).defaultValue(REPORT_FILE_DEFAULT).build());

		context.addExtension(
				PropertyDefinition.builder(XANITIZER_IMPORT_ALL_FINDINGS).name(IMPORT_ALL_NAME)
						.description(IMPORT_ALL_DESCRIPTION).category(EXTERNAL_ANALYZERS_CATEGORY)
						.subCategory(JAVA_SUBCATEGORY).onQualifiers(Qualifiers.PROJECT)
						.type(PropertyType.BOOLEAN).defaultValue(IMPORT_ALL_DEFAULT).build());
	}

	public static boolean getImportAll(final SensorContext sensorContext) {
		final Configuration config = sensorContext.config();
		final Optional<Boolean> importAll = config.getBoolean(XANITIZER_IMPORT_ALL_FINDINGS);

		if (!importAll.isPresent()) {
			return false;
		}

		return importAll.get();
	}

	/**
	 * Extracts the XML report file from the SonarQube settings.
	 * 
	 * @param sensorContext
	 * @param settings
	 * @return
	 */
	public static File geReportFile(final SensorContext sensorContext) {
		final Configuration config = sensorContext.config();
		final Optional<String> reportFileSetting = config.get(XANITIZER_XML_REPORT_FILE);

		if (!reportFileSetting.isPresent() || reportFileSetting.get().isEmpty()) {
			LOG.warn("Xanitizer parameter '" + XANITIZER_XML_REPORT_FILE
					+ "' not specified in project settings. Skipping analysis.");
			return null;
		}

		final String reportFileString = reportFileSetting.get();

		File reportFile = new File(reportFileString);
		if (!reportFile.isAbsolute()) {

			reportFile = new File(sensorContext.fileSystem().baseDir(), reportFileString);

			// recursively check the files below the base dir, if they contain a
			// file with the same name
			if (!reportFile.isFile() && PluginUtil.isFileName(reportFileString)) {
				final File temp = PluginUtil.searchRecursivlyInDir(
						sensorContext.fileSystem().baseDir(), reportFileString);
				if (temp != null && temp.isFile()) {
					reportFile = temp;
				}
			}
		}

		if (!reportFile.isFile()) {
			LOG.warn(
					"Xanitizer XML report file '" + reportFile + "' not found. Skipping analysis.");
			return null;
		}

		return reportFile;
	}

}
