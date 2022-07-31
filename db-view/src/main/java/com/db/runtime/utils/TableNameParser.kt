/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */
package com.db.runtime.utils

import java.util.*
import java.util.regex.Pattern

/**
 * Ultra light, Ultra fast parser to extract table name out SQLs, supports oracle dialect SQLs as well.
 *
 * @author Nadeem Mohammad
 *
 *
 * Ref : https://github.com/mnadeem/sql-table-name-parser
 */
class TableNameParser(sql: String) {
    private val tables: MutableMap<String, String> = HashMap()
    private fun removeComments(sql: String): String {
        val sb = StringBuilder(sql)
        var nextCommentPosition = sb.indexOf(TOKEN_SINGLE_LINE_COMMENT)
        while (nextCommentPosition > -1) {
            val end = indexOfRegex(TOKEN_NEWLINE, sb.substring(nextCommentPosition))
            if (end == -1) {
                return sb.substring(0, nextCommentPosition)
            } else {
                sb.replace(nextCommentPosition, end + nextCommentPosition, "")
            }
            nextCommentPosition = sb.indexOf(TOKEN_SINGLE_LINE_COMMENT)
        }
        return sb.toString()
    }

    private fun indexOfRegex(regex: String, string: String): Int {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(string)
        return if (matcher.find()) matcher.start() else -1
    }

    private fun normalized(sql: String): String {
        var normalized = sql.trim { it <= ' ' }
            .replace(TOKEN_NEWLINE.toRegex(), SPACE).replace(TOKEN_COMMA.toRegex(), " , ")
            .replace("\\(".toRegex(), " ( ").replace("\\)".toRegex(), " ) ")
        if (normalized.endsWith(TOKEN_SEMI_COLON)) {
            normalized = normalized.substring(0, normalized.length - 1)
        }
        return normalized
    }

    private fun clean(normalized: String): String {
        val start = normalized.indexOf(TOKEN_ORACLE_HINT_START)
        var end = NO_INDEX
        if (start != NO_INDEX) {
            end = normalized.indexOf(TOKEN_ORACLE_HINT_END)
            if (end != NO_INDEX) {
                val firstHalf = normalized.substring(0, start)
                val secondHalf = normalized.substring(end + 2, normalized.length)
                return firstHalf.trim { it <= ' ' } + SPACE + secondHalf.trim { it <= ' ' }
            }
        }
        return normalized
    }

    private fun isOracleSpecialDelete(
        currentToken: String,
        tokens: Array<String>,
        index: Int
    ): Boolean {
        var index = index
        index++ // Point to next token
        if (TOKEN_DELETE == currentToken) {
            if (moreTokens(tokens, index)) {
                val nextToken = tokens[index++]
                if (KEYWORD_FROM != nextToken && TOKEN_ASTERICK != nextToken) {
                    return true
                }
            }
        }
        return false
    }

    private fun handleSpecialOracleSpecialDelete(
        currentToken: String,
        tokens: Array<String>,
        index: Int
    ) {
        val tableName = tokens[index + 1]
        considerInclusion(tableName)
    }

    private fun isCreateIndex(currentToken: String, tokens: Array<String>, index: Int): Boolean {
        var index = index
        index++ // Point to next token
        if (TOKEN_CREATE == currentToken.lowercase(Locale.getDefault()) && hasIthToken(
                tokens,
                index,
                3
            )
        ) {
            val nextToken = tokens[index++]
            if (TOKEN_INDEX == nextToken.lowercase(Locale.getDefault())) {
                return true
            }
        }
        return false
    }

    private fun handleCreateIndex(currentToken: String, tokens: Array<String>, index: Int) {
        val tableName = tokens[index + 4]
        considerInclusion(tableName)
    }

    private fun hasIthToken(tokens: Array<String>, currentIndex: Int, tokenNumber: Int): Boolean {
        return if (moreTokens(tokens, currentIndex) && tokens.size > currentIndex + tokenNumber) {
            true
        } else false
    }

    private fun shouldProcess(currentToken: String): Boolean {
        return concerned.contains(currentToken.lowercase(Locale.getDefault()))
    }

    private fun isFromToken(currentToken: String): Boolean {
        return KEYWORD_FROM == currentToken.lowercase(Locale.getDefault())
    }

    private fun processFromToken(tokens: Array<String>, index: Int) {
        var index = index
        val currentToken = tokens[index++]
        considerInclusion(currentToken)
        var nextToken: String? = null
        if (moreTokens(tokens, index)) {
            nextToken = tokens[index++]
        }
        if (shouldProcessMultipleTables(nextToken)) {
            processNonAliasedMultiTables(tokens, index, nextToken)
        } else {
            processAliasedMultiTables(tokens, index, currentToken)
        }
    }

    private fun processNonAliasedMultiTables(
        tokens: Array<String>,
        index: Int,
        nextToken: String?
    ) {
        var index = index
        var nextToken = nextToken
        while (nextToken == TOKEN_COMMA) {
            val currentToken = tokens[index++]
            considerInclusion(currentToken)
            nextToken = if (moreTokens(tokens, index)) {
                tokens[index++]
            } else {
                break
            }
        }
    }

    private fun processAliasedMultiTables(tokens: Array<String>, index: Int, currentToken: String) {
        var index = index
        var currentToken = currentToken
        var nextNextToken: String? = null
        if (moreTokens(tokens, index)) {
            nextNextToken = tokens[index++]
        }
        if (shouldProcessMultipleTables(nextNextToken)) {
            while (moreTokens(tokens, index) && nextNextToken == TOKEN_COMMA) {
                if (moreTokens(tokens, index)) {
                    currentToken = tokens[index++]
                }
                if (moreTokens(tokens, index)) {
                    index++
                }
                if (moreTokens(tokens, index)) {
                    nextNextToken = tokens[index++]
                }
                considerInclusion(currentToken)
            }
        }
    }

    private fun shouldProcessMultipleTables(nextToken: String?): Boolean {
        return nextToken != null && nextToken == TOKEN_COMMA
    }

    private fun moreTokens(tokens: Array<String>, index: Int): Boolean {
        return index < tokens.size
    }

    private fun considerInclusion(token: String) {
        if (!ignored.contains(token.lowercase(Locale.getDefault())) && !tables.containsKey(
                token.lowercase(
                    Locale.getDefault()
                )
            )
        ) {
            tables[token.lowercase(Locale.getDefault())] = token
        }
    }

    /**
     * @return table names extracted out of sql
     */
    fun tables(): Collection<String> {
        return HashSet(tables.values)
    }

    companion object {
        private const val NO_INDEX = -1
        private const val SPACE = " "
        private const val REGEX_SPACE = "\\s+"
        private const val TOKEN_ORACLE_HINT_START = "/*+"
        private const val TOKEN_ORACLE_HINT_END = "*/"
        private const val TOKEN_SINGLE_LINE_COMMENT = "--"
        private const val TOKEN_SEMI_COLON = ";"
        private const val TOKEN_PARAN_START = "("
        private const val TOKEN_COMMA = ","
        private const val TOKEN_SET = "set"
        private const val TOKEN_OF = "of"
        private const val TOKEN_DUAL = "dual"
        private const val TOKEN_DELETE = "delete"
        private const val TOKEN_CREATE = "create"
        private const val TOKEN_INDEX = "index"
        private const val TOKEN_ASTERICK = "*"
        private const val KEYWORD_JOIN = "join"
        private const val KEYWORD_INTO = "into"
        private const val KEYWORD_TABLE = "table"
        private const val KEYWORD_FROM = "from"
        private const val KEYWORD_USING = "using"
        private const val KEYWORD_UPDATE = "update"
        private val concerned =
            listOf(KEYWORD_TABLE, KEYWORD_INTO, KEYWORD_JOIN, KEYWORD_USING, KEYWORD_UPDATE)
        private val ignored = listOf(TOKEN_PARAN_START, TOKEN_SET, TOKEN_OF, TOKEN_DUAL)
        private const val TOKEN_NEWLINE = "\\r\\n|\\r|\\n|\\n\\r"
    }

    /**
     * Extracts table names out of SQL
     *
     * @param sql
     */
    init {
        val nocomments = removeComments(sql)
        val normalized = normalized(nocomments)
        val cleansed = clean(normalized)
        val tokens = cleansed.split(REGEX_SPACE).toTypedArray()
        var index = 0
        val firstToken = tokens[index]
        if (isOracleSpecialDelete(firstToken, tokens, index)) {
            handleSpecialOracleSpecialDelete(firstToken, tokens, index)
        } else if (isCreateIndex(firstToken, tokens, index)) {
            handleCreateIndex(firstToken, tokens, index)
        } else {
            while (moreTokens(tokens, index)) {
                val currentToken = tokens[index++]
                if (isFromToken(currentToken)) {
                    processFromToken(tokens, index)
                } else if (shouldProcess(currentToken)) {
                    var nextToken = tokens[index++]
                    considerInclusion(nextToken)
                    if (moreTokens(tokens, index)) {
                        nextToken = tokens[index++]
                    }
                }
            }
        }
    }
}
