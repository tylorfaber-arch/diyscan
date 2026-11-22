package dev.diyscan.obd

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * Torque-style PID formulas, e.g. "A*256+B", "(A-40)*1.8+32", "SIGNED(A)/2", "{B:3}".
 *
 * Variables: A..Z are reply bytes 0..25, AA..AZ are bytes 26..51.
 * Operators: + - * / ^ and parentheses. Bit access: {A:7} (bit 7 of A, 0 or 1).
 * Functions: SIGNED(x) (8-bit two's complement), ABS(x), INT(x), MIN(a,b), MAX(a,b).
 *
 * Evaluation returns NaN when the reply is too short for the formula.
 */
class Equation private constructor(val text: String, private val root: Node) {

    /** Highest byte index the formula reads, so callers know how much reply is needed. */
    val bytesNeeded: Int = root.maxByte() + 1

    fun eval(bytes: IntArray): Double = try { root.eval(bytes) } catch (e: IndexOutOfBoundsException) { Double.NaN }

    private sealed interface Node {
        fun eval(b: IntArray): Double
        fun maxByte(): Int
    }
    private class Num(val v: Double) : Node { override fun eval(b: IntArray) = v; override fun maxByte() = -1 }
    private class Var(val i: Int) : Node { override fun eval(b: IntArray) = b[i].toDouble(); override fun maxByte() = i }
    private class Bit(val i: Int, val bit: Int) : Node {
        override fun eval(b: IntArray) = ((b[i] shr bit) and 1).toDouble(); override fun maxByte() = i
    }
    private class Neg(val n: Node) : Node { override fun eval(b: IntArray) = -n.eval(b); override fun maxByte() = n.maxByte() }
    private class Bin(val op: Char, val l: Node, val r: Node) : Node {
        override fun eval(b: IntArray): Double {
            val x = l.eval(b); val y = r.eval(b)
            return when (op) { '+' -> x + y; '-' -> x - y; '*' -> x * y; '/' -> x / y; else -> x.pow(y) }
        }
        override fun maxByte() = maxOf(l.maxByte(), r.maxByte())
    }
    private class Fn(val name: String, val args: List<Node>) : Node {
        override fun eval(b: IntArray): Double {
            val v = args.map { it.eval(b) }
            return when (name) {
                "SIGNED" -> v[0].let { if (it > 127) it - 256 else it }
                "ABS" -> abs(v[0])
                "INT" -> floor(v[0])
                "MIN" -> minOf(v[0], v[1])
                "MAX" -> maxOf(v[0], v[1])
                else -> Double.NaN
            }
        }
        override fun maxByte() = args.maxOfOrNull { it.maxByte() } ?: -1
    }

    private class Parser(val s: String) {
        var p = 0
        fun ws() { while (p < s.length && s[p].isWhitespace()) p++ }
        fun peek(): Char? { ws(); return s.getOrNull(p) }
        fun eat(c: Char): Boolean { if (peek() == c) { p++; return true }; return false }
        fun expect(c: Char) { if (!eat(c)) throw IllegalArgumentException("Expected '$c' at ${p + 1}") }

        fun expr(): Node {
            var n = term()
            while (true) n = when { eat('+') -> Bin('+', n, term()); eat('-') -> Bin('-', n, term()); else -> return n }
        }
        fun term(): Node {
            var n = power()
            while (true) n = when { eat('*') -> Bin('*', n, power()); eat('/') -> Bin('/', n, power()); else -> return n }
        }
        fun power(): Node { val b = unary(); return if (eat('^')) Bin('^', b, power()) else b }
        fun unary(): Node = when { eat('-') -> Neg(unary()); eat('+') -> unary(); else -> atom() }

        fun atom(): Node {
            val c = peek() ?: throw IllegalArgumentException("Formula ended early")
            return when {
                c == '(' -> { p++; expr().also { expect(')') } }
                c == '{' -> { p++; val v = varIndex(ident()); expect(':'); val bit = number().toInt(); expect('}')
                    require(bit in 0..7) { "Bit must be 0-7" }; Bit(v, bit) }
                c.isDigit() || c == '.' -> Num(number())
                c.isLetter() -> {
                    val id = ident()
                    if (peek() == '(') {
                        p++
                        val args = mutableListOf(expr())
                        while (eat(',')) args += expr()
                        expect(')')
                        val n = id.uppercase()
                        require(n in setOf("SIGNED", "ABS", "INT", "MIN", "MAX")) { "Unknown function $id" }
                        require(args.size == if (n == "MIN" || n == "MAX") 2 else 1) { "Wrong argument count for $id" }
                        Fn(n, args)
                    } else Var(varIndex(id))
                }
                else -> throw IllegalArgumentException("Unexpected '$c' at ${p + 1}")
            }
        }
        fun ident(): String { ws(); val st = p; while (p < s.length && s[p].isLetter()) p++; return s.substring(st, p) }
        fun number(): Double {
            ws(); val st = p
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++
            return s.substring(st, p).toDoubleOrNull() ?: throw IllegalArgumentException("Bad number at ${st + 1}")
        }
        fun varIndex(id: String): Int {
            val u = id.uppercase()
            return when {
                u.length == 1 && u[0] in 'A'..'Z' -> u[0] - 'A'
                u.length == 2 && u[0] == 'A' && u[1] in 'A'..'Z' -> 26 + (u[1] - 'A')
                else -> throw IllegalArgumentException("Unknown variable $id")
            }
        }
    }

    companion object {
        /** Throws IllegalArgumentException with a readable message if the formula isn't supported. */
        fun parse(text: String): Equation {
            val ps = Parser(text.trim())
            val n = ps.expr()
            if (ps.peek() != null) throw IllegalArgumentException("Unexpected '${ps.peek()}' at ${ps.p + 1}")
            return Equation(text.trim(), n)
        }
    }
}
