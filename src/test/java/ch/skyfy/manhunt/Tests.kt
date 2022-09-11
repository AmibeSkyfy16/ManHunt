package ch.skyfy.tinyeconomyrenewed

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.minecraft.util.math.random.Random
import kotlin.test.Test

class Tests {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testWithMultipleDelays() = runTest {



    }

    @Test
    fun test2(){
        val random = Random.create()
        for(i in 0..1000) {
            val test = random.nextDouble()
            println(test)
        }
    }

}