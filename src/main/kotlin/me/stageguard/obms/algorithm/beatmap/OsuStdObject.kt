package me.stageguard.obms.algorithm.beatmap

import me.stageguard.obms.algorithm.pp.DifficultyAttributes
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

@Suppress("PrivatePropertyName")
class OsuStdObject constructor(
    h: HitObject,
    beatmap: Beatmap,
    radius: Double,
    scalingFactor: Double,
    ticks: MutableList<Double>,
    attributes: DifficultyAttributes,
    sliderState: SliderState,
) {

    private val LEGACY_LAST_TICK_OFFSET = 36.0

    var time by Delegates.notNull<Double>()
    lateinit var position: HitObjectPosition
    var stackHeight by Delegates.notNull<Double>()
    var kind: OsuStdObjectType = OsuStdObjectType.Hold

    val travelDist get() = when(val kind = kind) {
        is OsuStdObjectType.Slider -> kind.travelDist
        is OsuStdObjectType.Hold -> -1.0
        else -> 0.0
    }

    val endPosition get() = when(val kind = kind) {
        is OsuStdObjectType.Circle -> position
        is OsuStdObjectType.Slider -> kind.endPosition
        is OsuStdObjectType.Spinner -> position
        is OsuStdObjectType.Hold -> HitObjectPosition(-1.0, -1.0)
    }

    val endTime get() = when(val kind = kind) {
        is OsuStdObjectType.Circle -> time
        is OsuStdObjectType.Slider -> kind.endTime
        is OsuStdObjectType.Spinner -> kind.endTime
        is OsuStdObjectType.Hold -> -1.0
    }

    val lazyEndPosition get() = when(val kind = kind) {
        is OsuStdObjectType.Circle -> position
        is OsuStdObjectType.Slider -> kind.lazyEndPosition
        is OsuStdObjectType.Spinner -> position
        is OsuStdObjectType.Hold -> HitObjectPosition(-1.0, -1.0)
    }

    val isCircle get() = kind is OsuStdObjectType.Circle
    val isSlider get() = kind is OsuStdObjectType.Slider
    val isSpinner get() = kind is OsuStdObjectType.Spinner

    init {
        attributes.maxCombo ++
        val stackHeight = 0.0

        when(h.kind) {
            is HitObjectType.Circle -> {
                this.time = h.startTime
                this.position = h.pos
                this.kind = OsuStdObjectType.Circle
                this.stackHeight = stackHeight
            }
            is HitObjectType.Slider -> {
                var lazyEndPosition = h.pos
                var travelDist = 0.0

                sliderState.update(h.startTime)

                val approxFollowCircleRadius = radius * 3.0
                var tickDistance = 100.0 * beatmap.sliderMultiplier / beatmap.sliderTickRate

                if(beatmap.version >= 8) {
                    tickDistance /= min(1000.0, max(10.0, 100.0 / sliderState.speedMultiply)) / 100.0
                }

                println("sliderState.speedMultiply: ${sliderState.speedMultiply}")

                val duration = h.kind.repeatTimes.toDouble() * sliderState.beatLength * h.kind.pixelLength / (beatmap.sliderMultiplier * sliderState.speedMultiply) / 100
                val spanDuration = duration / h.kind.repeatTimes.toDouble()

                val curve = Curve.newCurve(h.kind.curvePoints, h.kind.pathType)

                val computeVertex = { time: Double ->
                    attributes.maxCombo ++

                    var progress = (time - h.startTime) / spanDuration

                    if (progress % 2.0 >= 1.0) {
                        progress = 1.0 - progress % 1.0
                    } else {
                        progress %= 1.0
                    }

                    val currDist = h.kind.pixelLength * progress
                    val currPos = curve.pointAtDistance(currDist)

                    val diff = currPos - lazyEndPosition
                    var dist = diff.length()

                    if (dist > approxFollowCircleRadius) {
                        dist -= approxFollowCircleRadius
                        lazyEndPosition += diff.normalize() * dist
                        travelDist += dist
                    }
                }

                var currentDistance = tickDistance
                val timeAdd = duration * (tickDistance / (h.kind.pixelLength * h.kind.repeatTimes.toDouble()))

                val target = h.kind.pixelLength - tickDistance / 8.0
                //ticks.reserve((target / tickDistance).toInt())
                println("Variable: h.kind.pixelLength: ${h.kind.pixelLength}, tickDistance: $tickDistance")

                if (currentDistance < target) {
                    for (index in 1..Int.MAX_VALUE) {
                        val time = h.startTime + timeAdd * index
                        computeVertex(time)
                        println("Combo: currentDistance < target + 1")
                        ticks.add(time)
                        currentDistance += tickDistance

                        if (currentDistance >= target) break
                    }
                }

                if(h.kind.repeatTimes > 1) {
                    println("RepeatTimes: ${h.kind.repeatTimes}")
                    for (rptIndex in 1 until h.kind.repeatTimes) {
                        val timeOffset = (duration / h.kind.repeatTimes.toDouble()) * rptIndex.toDouble()
                        computeVertex(h.startTime + timeOffset)
                        println("Combo: repeatTimes > 1 + 1")
                        if (rptIndex and 1 == 1) {
                            ticks.asReversed().forEach {
                                println("Combo: reservedList + 1")
                                computeVertex(it)
                            }
                        } else {
                            ticks.forEach{
                                println("Combo: normalList + 1")
                                computeVertex(it)
                            }
                        }
                    }
                }

                val finalSpanIndex = min(0, h.kind.repeatTimes - 1)
                val finalSpanStartTime = h.startTime + finalSpanIndex.toDouble() * spanDuration
                val finalSpanEndTime = max(
                    h.startTime + duration / 2.0,
                    finalSpanStartTime + spanDuration - LEGACY_LAST_TICK_OFFSET
                )

                computeVertex(finalSpanEndTime)
                ticks.clear()
                travelDist *= scalingFactor
                val endPos = curve.pointAtDistance(h.kind.pixelLength)

                this.time = h.startTime
                this.position = h.pos
                this.stackHeight = stackHeight
                this.kind = OsuStdObjectType.Slider(
                    endTime = finalSpanEndTime,
                    endPosition = endPos,
                    lazyEndPosition = lazyEndPosition,
                    travelDist = travelDist
                )
            }
            is HitObjectType.Spinner -> {
                this.time = h.startTime
                this.position = h.pos
                this.stackHeight = stackHeight
                this.kind = OsuStdObjectType.Spinner(h.kind.endTime)
            }
            is HitObjectType.Hold -> {
                this.stackHeight = -1.0 //represent a Hold object
            }
        }
    }


}

sealed class OsuStdObjectType {
    object Circle : OsuStdObjectType()
    class Slider(
        val endTime: Double, val endPosition: HitObjectPosition,
        val lazyEndPosition: HitObjectPosition, val travelDist: Double
    ) : OsuStdObjectType()
    class Spinner(val endTime: Double) : OsuStdObjectType()
    object Hold : OsuStdObjectType()
}