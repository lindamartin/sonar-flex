/*
 * SonarQube Flex Plugin
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.flex.cobertura;

import com.google.common.collect.Maps;
import static java.util.Locale.ENGLISH;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import static org.sonar.api.utils.ParsingUtils.parseNumber;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;
import org.sonar.plugins.flex.core.Flex;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.text.ParseException;
import java.util.Map;

public class CoberturaReportParser {

  private static final Logger LOG = LoggerFactory.getLogger(CoberturaReportParser.class);

  private CoberturaReportParser() {
  }

  /**
   * Parse a Cobertura xml report and create measures accordingly
   */
  public static void parseReport(File xmlFile, final SensorContext context, final FileSystem fileSystem) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        @Override
        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          rootCursor.advance();
          collectPackageMeasures(rootCursor.descendantElementCursor("package"), context, fileSystem);
        }
      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private static void collectPackageMeasures(SMInputCursor pack, SensorContext context, FileSystem fileSystem) throws XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      FilePredicates predicates = fileSystem.predicates();

      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {

        String fileName = entry.getKey().startsWith(File.separator) ? entry.getKey() : (File.separator + entry.getKey());
        InputFile inputFile = fileSystem.inputFile(predicates.and(
          predicates.matchesPathPattern("file:**" + fileName.replace(File.separator, "/")),
          predicates.hasType(InputFile.Type.MAIN),
          predicates.hasLanguage(Flex.KEY)));

        if (inputFile != null) {
          for (Measure measure : entry.getValue().createMeasures()) {
            context.saveMeasure(inputFile, measure);
          }
          // mxml files are not imported by the plugin because they are not supported
        } else if (!entry.getKey().endsWith(".mxml")){
          LOG.warn("Cannot save coverage result for file: {}, because resource not found.", entry.getKey());
        }
      }
    }
  }

  private static void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> builderByFilename) throws XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = clazz.getAttrValue("filename");

      // mxml files are not supported by the plugin
      CoverageMeasuresBuilder builder = builderByFilename.get(fileName);

      if (builder == null) {
        builder = CoverageMeasuresBuilder.create();
        builderByFilename.put(fileName, builder);
      }
      collectFileData(clazz, builder);
    }
  }

  private static void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      try {
        builder.setHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));
      } catch (ParseException e) {
        throw new XmlParserException(e);
      }

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }
}
