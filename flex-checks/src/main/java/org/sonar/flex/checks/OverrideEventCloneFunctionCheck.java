/*
 * SonarQube Flex Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.flex.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.flex.FlexGrammar;
import org.sonar.flex.FlexKeyword;
import org.sonar.flex.checks.utils.Function;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(key = "S1470",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.CRITICAL)
public class OverrideEventCloneFunctionCheck extends SquidCheck<LexerlessGrammar> {

  private static final String EVENT_TYPE_NAME = "Event";

  @Override
  public void init() {
    subscribeTo(FlexGrammar.CLASS_DEF);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (!isExtendingEvent(astNode)) {
      return;
    }
    List<AstNode> classDirectives = astNode
      .getFirstChild(FlexGrammar.BLOCK)
      .getFirstChild(FlexGrammar.DIRECTIVES)
      .getChildren(FlexGrammar.DIRECTIVE);

    for (AstNode directive : classDirectives) {
      if (isOverridingFunction(directive) && isCloneFunction(directive)) {
        return;
      }
    }

    getContext().createLineViolation(this, "Make this class \"{0}\" override \"Event.clone()\" function.", astNode,
      astNode.getFirstChild(FlexGrammar.CLASS_NAME).getFirstChild(FlexGrammar.CLASS_IDENTIFIERS).getLastChild().getTokenValue());
  }

  private static boolean isCloneFunction(AstNode directive) {
    AstNode functionDef = directive
      .getFirstChild(FlexGrammar.ANNOTABLE_DIRECTIVE)
      .getFirstChild(FlexGrammar.FUNCTION_DEF);

    String functionName = Function.getName(functionDef);

    return "clone".equals(functionName) && EVENT_TYPE_NAME.equals(getResultType(functionDef));
  }

  private static String getResultType(AstNode functionDef) {
    AstNode resultType = functionDef
      .getFirstChild(FlexGrammar.FUNCTION_COMMON)
      .getFirstChild(FlexGrammar.FUNCTION_SIGNATURE)
      .getFirstChild(FlexGrammar.RESULT_TYPE);

    if (resultType != null && resultType.getFirstChild(FlexGrammar.TYPE_EXPR) != null) {
      return resultType.getFirstChild(FlexGrammar.TYPE_EXPR).getTokenValue();
    }
    return null;
  }


  private static boolean isExtendingEvent(AstNode classDef) {
    AstNode inheritenceNode = classDef.getFirstChild(FlexGrammar.INHERITENCE);

    if (inheritenceNode != null && inheritenceNode.getFirstChild(FlexKeyword.EXTENDS) != null) {
      AstNode qualifiedId = inheritenceNode.getFirstChild(FlexGrammar.TYPE_EXPR).getLastChild();
      if (qualifiedId.is(FlexGrammar.QUALIFIED_IDENTIFIER) && EVENT_TYPE_NAME.equals(qualifiedId.getTokenValue())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isOverridingFunction(AstNode directive) {
    return isFunctionWithAttributes(directive) && isOverriding(directive);
  }

  private static boolean isFunctionWithAttributes(AstNode directive) {
    return directive.getFirstChild(FlexGrammar.ANNOTABLE_DIRECTIVE) != null
      && directive.getFirstChild(FlexGrammar.ANNOTABLE_DIRECTIVE).getFirstChild().is(FlexGrammar.FUNCTION_DEF)
      && directive.getFirstChild(FlexGrammar.ATTRIBUTES) != null;
  }

  private static boolean isOverriding(AstNode directive) {
    for (AstNode attribute : directive.getFirstChild(FlexGrammar.ATTRIBUTES).getChildren()) {

      if (attribute.getFirstChild().is(FlexGrammar.ATTRIBUTE_EXPR)
        && attribute.getFirstChild().getNumberOfChildren() == 1
        && attribute.getFirstChild().getFirstChild(FlexGrammar.IDENTIFIER).getTokenValue().equals(FlexKeyword.OVERRIDE.getValue())) {
        return true;
      }
    }
    return false;
  }
}
