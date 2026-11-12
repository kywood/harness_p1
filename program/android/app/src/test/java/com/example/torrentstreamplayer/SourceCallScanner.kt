package com.example.torrentstreamplayer

/**
 * 소스 검사 테스트용 Kotlin 호출 인자 분리기. 문자열 리터럴의 글자는 버리고 문자열 템플릿(`$x`, `${expr}`)만
 * 코드로 남기므로, 리터럴 안의 `,`·`(`·`)`에 속지 않으면서 메시지에 끼운 값은 그대로 검사할 수 있다.
 */
internal object SourceCallScanner {
    /** [start]는 여는 괄호 바로 뒤. 짝이 맞는 `)`까지의 최상위 인자를 리터럴 글자를 뺀 코드 텍스트로 돌려준다. */
    fun arguments(source: String, start: Int): List<String> = scan(source, start).first

    /** [start]는 여는 `(`·`{`·`[` 바로 뒤. 짝이 맞는 닫는 괄호의 위치(없으면 소스 끝)를 돌려준다. 리터럴 안은 건너뛴다. */
    fun closingIndex(source: String, start: Int): Int = scan(source, start).second

    private fun scan(source: String, start: Int): Pair<List<String>, Int> {
        val args = mutableListOf(StringBuilder())
        var depth = 1
        var i = start
        while (i < source.length) {
            val c = source[i]
            when {
                c == '"' -> {
                    i = string(source, i, args.last())
                    continue
                }
                c == '\'' -> {
                    i = charLiteral(source, i)
                    continue
                }
                c == '(' || c == '[' || c == '{' -> depth++
                c == ')' || c == ']' || c == '}' -> {
                    depth--
                    if (depth == 0) break
                }
                c == ',' && depth == 1 -> {
                    args.add(StringBuilder())
                    i++
                    continue
                }
            }
            args.last().append(c)
            i++
        }
        // 끝의 쉼표(`f(a, b,)`)는 인자를 늘리지 않는다.
        val trimmed = args.map { it.toString().trim() }
        return (if (trimmed.size > 1 && trimmed.last().isEmpty()) trimmed.dropLast(1) else trimmed) to i
    }

    private const val DESCRIBE = "EngineFailureLog.describe("

    /** [code]에서 `EngineFailureLog.describe(…)` 호출 구간을 지운다. 그 안의 예외 참조는 원문을 남기지 않는 정상 사용이다. */
    fun withoutDescribeCalls(code: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < code.length) {
            val found = code.indexOf(DESCRIBE, i)
            if (found < 0) {
                out.append(code, i, code.length)
                break
            }
            out.append(code, i, found)
            var depth = 1
            var j = found + DESCRIBE.length
            while (j < code.length && depth > 0) {
                when (code[j]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                j++
            }
            i = j
        }
        return out.toString()
    }

    /** [start]는 여는 `"`. 리터럴 글자는 버리고 템플릿만 [out]에 붙인 뒤, 닫는 따옴표 다음 위치를 돌려준다. */
    private fun string(source: String, start: Int, out: StringBuilder): Int {
        val raw = source.startsWith("\"\"\"", start)
        var i = start + if (raw) 3 else 1
        while (i < source.length) {
            val c = source[i]
            when {
                raw && source.startsWith("\"\"\"", i) -> return i + 3
                !raw && c == '"' -> return i + 1
                !raw && c == '\\' -> i += 2
                c == '$' && source.getOrNull(i + 1) == '{' -> {
                    out.append("\${")
                    i = template(source, i + 2, out)
                    out.append('}')
                }
                c == '$' && source.getOrNull(i + 1)?.let { it.isLetter() || it == '_' } == true -> {
                    var end = i + 1
                    while (end < source.length && (source[end].isLetterOrDigit() || source[end] == '_')) end++
                    out.append(' ').append(source, i, end).append(' ')
                    i = end
                }
                else -> i++
            }
        }
        return i
    }

    /** [start]는 `${` 바로 뒤. 짝이 맞는 `}`까지의 식을 [out]에 붙이고 `}` 다음 위치를 돌려준다. */
    private fun template(source: String, start: Int, out: StringBuilder): Int {
        var depth = 1
        var i = start
        while (i < source.length) {
            val c = source[i]
            when (c) {
                '"' -> {
                    i = string(source, i, out)
                    continue
                }
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i + 1
                }
            }
            out.append(c)
            i++
        }
        return i
    }

    private fun charLiteral(source: String, start: Int): Int {
        var i = start + 1
        while (i < source.length && source[i] != '\'') i += if (source[i] == '\\') 2 else 1
        return i + 1
    }
}
