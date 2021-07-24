package me.stageguard.obms.algorithm.pp

import me.stageguard.obms.algorithm.beatmap.Beatmap
import me.stageguard.obms.algorithm.beatmap.ModCombination
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class PPCalculator private constructor(
    val beatmap: Beatmap
) {
    private var attributes: Optional<DifficultyAttributes> = Optional.empty()
    private var mods: Int = 0
    private var combo: Optional<Int> = Optional.empty()
    private var acc: Optional<Double> = Optional.empty()

    private var n300: Optional<Int> = Optional.empty()
    private var n100: Optional<Int> = Optional.empty()
    private var n50: Optional<Int> = Optional.empty()
    private var nMisses: Int = 0
    private var passedObjects: Optional<Int> = Optional.empty()

    fun attributes(attr: Optional<DifficultyAttributes>) = this.also {
        attr.ifPresent { this.attributes = Optional.of(it) }
    }
    fun mods(mod: Int) = this.also { this.mods = mod }
    fun combo(cb: Int) = this.also { this.combo = Optional.of(cb) }
    fun n300(n : Int) = this.also { this.n300 = Optional.of(n) }
    fun n100(n: Int) = this.also { this.n100 = Optional.of(n) }
    fun n50(n: Int) = this.also { this.n50 = Optional.of(n) }
    fun misses(n: Int) = this.also { this.nMisses = n }
    fun passedObjects(n: Int) = this.also { this.passedObjects = Optional.of(n) }
    fun accuracy(acc: Double) = this.also {
        val nObjects = this.passedObjects.orElse(beatmap.hitObjects.size)

        val accuracy = acc / 100.0

        if (this.n100.isPresent && this.n50.isPresent) {
            var n100 = this.n100.orElse(0)
            var n50 = this.n50.orElse(0)

            val placedPoints = 2 * n100 + n50 + this.nMisses
            val missingObjects = nObjects - n100 - n50 - this.nMisses
            val missingPoints = max(0, round(6.0 * accuracy * nObjects.toDouble()).toInt() - placedPoints)

            var n300 = min(missingObjects, missingPoints / 6)
            n50 += missingObjects - n300

            this.n50.filter { this.n100.isEmpty }.ifPresent { originalN50 ->
                val difference = n50 - originalN50
                val n = min(n300, difference / 4)

                n300 -= n
                n100 += 5 * n
                n50 -= 4 * n
            }

            this.n300 = Optional.of(n300)
            this.n100 = Optional.of(n100)
            this.n50 = Optional.of(n50)

        } else {
            val misses = min(this.nMisses, nObjects)
            val targetTotal = round(accuracy * nObjects.toDouble() * 6.0).toInt()
            val delta = targetTotal - (nObjects - misses)

            var n300 = delta / 5
            var n100 = min(delta % 5, nObjects - n300 - misses)
            var n50 = nObjects - n300 - n100 - misses

            val n = min(n300, n50 / 4)
            n300 -= n
            n100 += 5 * n
            n50 -= 4 * n

            this.n300 = Optional.of(n300)
            this.n100 = Optional.of(n100)
            this.n50 = Optional.of(n50)
        }
        this.acc = try {
            Optional.of((6 * this.n300.get() + 2 * this.n100.get() + this.n50.get()).toDouble() / (6 * nObjects).toDouble())
        } catch (ex: Exception) {
            Optional.empty<Double>()
        }
    }

    private fun assertHitResults() {
        if(this.acc.isEmpty) {
            val nObjects = this.passedObjects.orElse(this.beatmap.hitObjects.size)

            val remaining = max(0,
                max(0,
                    max(0,
                        max(0,
                            max(0, nObjects - this.n300.orElse(0))
                        ) - this.n100.orElse(0)
                    ) - this.n50.orElse(0)
                ) - this.nMisses
            )

            if(remaining > 0) {
                if (this.n300.isEmpty) {
                    this.n300 = Optional.of(remaining)
                } else if (this.n100.isEmpty) {
                    this.n100 = Optional.of(remaining)
                }
                if (this.n50.isEmpty) {
                    this.n50 = Optional.of(remaining)
                } else {
                    this.n300 = Optional.of(this.n300.get() + remaining)
                }
            }

            val n300 = 0.run {
                if(this@PPCalculator.n300.isEmpty) this@PPCalculator.n300 = Optional.of(this)
                this@PPCalculator.n300.get()
            }

            val n100 = 0.run {
                if(this@PPCalculator.n100.isEmpty) this@PPCalculator.n100 = Optional.of(this)
                this@PPCalculator.n100.get()
            }

            val n50 = 0.run {
                if(this@PPCalculator.n50.isEmpty) this@PPCalculator.n50 = Optional.of(this)
                this@PPCalculator.n50.get()
            }

            val numerator = n300 * 6 + n100 * 2 + n50
            this.acc = Optional.of(numerator.toDouble() / nObjects.toDouble() / 6.0)
        }
    }

    fun calculate() {
        if(this.attributes.isEmpty) {
            this.attributes = Optional.of(beatmap.stars(ModCombination(this.mods), this.passedObjects))
        }
        assertHitResults()

    }

    companion object {
        fun of(beatmap: Beatmap) = PPCalculator(beatmap)
    }

}