package no.uio.microobject.test.basic

import io.kotest.core.spec.style.StringSpec
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.SimulationScenario
import no.uio.microobject.runtime.SimulatorObject
import no.uio.microobject.test.MicroObjectTest
import org.apache.commons.lang3.SystemUtils
import org.junit.Assume
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FMOScenTest : MicroObjectTest() {
    init {
        "creating scenario".config(enabled = !SystemUtils.IS_OS_MAC) {
            Assume.assumeTrue("FMUs not found",
                File("examples/Scen/Prey.fmu").exists()
                        && File("examples/Scen/Predator.fmu").exists()
                        && File("examples/Scen/PreyPredator.conf").exists())
            val preyMem = mutableMapOf<String, LiteralExpr>()
            val simPrey = SimulatorObject("examples/Scen/Prey.fmu", preyMem)
            simPrey.role = "prey"

            val predMem = mutableMapOf<String, LiteralExpr>()
            val simPred = SimulatorObject("examples/Scen/Predator.fmu", predMem)
            simPred.role = "predator"

            val simScen = SimulationScenario("examples/Scen/PreyPredator.conf")
            simScen.assign(simPrey)
            simScen.assign(simPred)
            assertTrue(simScen.check())
        }
    }
}