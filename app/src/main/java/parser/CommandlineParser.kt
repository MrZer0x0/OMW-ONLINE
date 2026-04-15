/*
    Copyright (C) 2016 sandstranger
    Copyright (C) 2018 Ilya Zhuravlev

    This file is part of OpenMW-Android.

    OpenMW-Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenMW-Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenMW-Android.  If not, see <https://www.gnu.org/licenses/>.
*/

package parser

class CommandlineParser(data: String) {
    private val args = ArrayList<String>()
    val argv: Array<String>

    val argc: Int
        get() = args.size

    init {
        args.clear()
        args.add("openmw")
        if (data.isNotBlank()) {
            args.addAll(parseArgs(data))
        }
        argv = args.toTypedArray()
    }

    private fun parseArgs(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = '\u0000'

        fun flush() {
            if (current.isNotEmpty()) {
                result.add(current.toString())
                current.setLength(0)
            }
        }

        input.forEach { ch ->
            when {
                (ch == '"' || ch == ''') -> {
                    if (!inQuotes) {
                        inQuotes = true
                        quoteChar = ch
                    } else if (quoteChar == ch) {
                        inQuotes = false
                    } else {
                        current.append(ch)
                    }
                }
                ch.isWhitespace() && !inQuotes -> flush()
                else -> current.append(ch)
            }
        }
        flush()
        return result
    }
}
