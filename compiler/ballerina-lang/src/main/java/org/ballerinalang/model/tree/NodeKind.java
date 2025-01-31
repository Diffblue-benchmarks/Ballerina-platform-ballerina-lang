/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.model.tree;

/**
 * @since 0.94
 */
public enum NodeKind {

    ANNOTATION,
    ANNOTATION_ATTACHMENT,
    ANNOTATION_ATTRIBUTE,
    CATCH,
    COMPILATION_UNIT,
    DEPRECATED,
    DOCUMENTATION,
    MARKDOWN_DOCUMENTATION,
    ENDPOINT,
    FUNCTION,
    IDENTIFIER,
    IMPORT,
    PACKAGE,
    PACKAGE_DECLARATION,
    RECORD_LITERAL_KEY_VALUE,
    RESOURCE,
    SERVICE,
    TYPE_DEFINITION,
    TABLE,
    TABLE_COLUMN,
    VARIABLE,
    TUPLE_VARIABLE,
    RECORD_VARIABLE,
    ERROR_VARIABLE,
    WORKER,
    XMLNS,
    CHANNEL,
    WAIT_LITERAL_KEY_VALUE,

    /* Expressions */
    DOCUMENTATION_ATTRIBUTE,
    ARRAY_LITERAL_EXPR,
    TUPLE_LITERAL_EXPR,
    LIST_CONSTRUCTOR_EXPR,
    BINARY_EXPR,
    ELVIS_EXPR,
    GROUP_EXPR,
    TYPE_INIT_EXPR,
    FIELD_BASED_ACCESS_EXPR,
    INDEX_BASED_ACCESS_EXPR,
    INT_RANGE_EXPR,
    INVOCATION,
    LAMBDA,
    ARROW_EXPR,
    LITERAL,
    NUMERIC_LITERAL,
    CONSTANT,
    RECORD_LITERAL_EXPR,
    SIMPLE_VARIABLE_REF,
    CONSTANT_REF,
    TUPLE_VARIABLE_REF,
    RECORD_VARIABLE_REF,
    ERROR_VARIABLE_REF,
    STRING_TEMPLATE_LITERAL,
    TERNARY_EXPR,
    WAIT_EXPR,
    TRAP_EXPR,
    TYPEDESC_EXPRESSION,
    TYPE_CONVERSION_EXPR,
    IS_ASSIGNABLE_EXPR,
    UNARY_EXPR,
    REST_ARGS_EXPR,
    NAMED_ARGS_EXPR,
    XML_QNAME,
    XML_ATTRIBUTE,
    XML_ATTRIBUTE_ACCESS_EXPR,
    XML_QUOTED_STRING,
    XML_ELEMENT_LITERAL,
    XML_TEXT_LITERAL,
    XML_COMMENT_LITERAL,
    XML_PI_LITERAL,
    XML_SEQUENCE_LITERAL,
    STATEMENT_EXPRESSION,
    MATCH_EXPRESSION,
    MATCH_EXPRESSION_PATTERN_CLAUSE,
    CHECK_EXPR,
    CHECK_PANIC_EXPR,
    ERROR_CONSTRUCTOR,
    TYPE_TEST_EXPR,
    IS_LIKE,
    DOCUMENTATION_DESCRIPTION,
    DOCUMENTATION_PARAMETER,
    SERVICE_CONSTRUCTOR,

    /* streams/tables expressions */
    SELECT_EXPRESSION,
    TABLE_QUERY_EXPRESSION,

    /* Statements */
    ABORT,
    DONE,
    RETRY,
    ASSIGNMENT,
    COMPOUND_ASSIGNMENT,
    POST_INCREMENT,
    BLOCK,
    BREAK,
    NEXT,
    EXPRESSION_STATEMENT,
    FOREACH,
    FORK_JOIN,
    IF,
    MATCH,
    MATCH_TYPED_PATTERN_CLAUSE,
    MATCH_STATIC_PATTERN_CLAUSE,
    MATCH_STRUCTURED_PATTERN_CLAUSE,
    REPLY,
    RETURN,
    THROW,
    PANIC,
    TRANSACTION,
    TRANSFORM,
    TRY,
    TUPLE_DESTRUCTURE,
    RECORD_DESTRUCTURE,
    ERROR_DESTRUCTURE,
    VARIABLE_DEF,
    WHILE,
    LOCK,
    WORKER_RECEIVE,
    WORKER_SEND,
    WORKER_SYNC_SEND,
    WORKER_FLUSH,
    STREAM,
    SCOPE,
    COMPENSATE,
    CHANNEL_RECEIVE,
    CHANNEL_SEND,

    /* Types */
    ARRAY_TYPE,
    UNION_TYPE_NODE,
    FINITE_TYPE_NODE,
    TUPLE_TYPE_NODE,
    BUILT_IN_REF_TYPE,
    CONSTRAINED_TYPE,
    FUNCTION_TYPE,
    USER_DEFINED_TYPE,
    ENDPOINT_TYPE,
    VALUE_TYPE,
    RECORD_TYPE,
    OBJECT_TYPE,
    ERROR_TYPE,

    /* Clauses */
    ORDER_BY,
    ORDER_BY_VARIABLE,
    LIMIT,
    GROUP_BY,
    HAVING,
    SELECT_CLAUSE,
    WHERE,
    FUNCTION_CLAUSE,
    WINDOW_CLAUSE,
    STREAMING_INPUT,
    JOIN_STREAMING_INPUT,
    TABLE_QUERY,
    SET_ASSIGNMENT_CLAUSE,
    SET,
    STREAM_ACTION,
    PATTERN_STREAMING_EDGE_INPUT,
    PATTERN_STREAMING_INPUT,
    STREAMING_QUERY,
    QUERY,
    STREAMING_QUERY_DECLARATION,
    WITHIN,
    PATTERN_CLAUSE,
    OUTPUT_RATE_LIMIT,
    FOREVER,
}
