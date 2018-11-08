// See LICENSE for license details.

package firrtlTests.transforms

import firrtl.{ChirrtlForm, CircuitState}
import firrtl.transforms.{ClockSource, FindClockSources, GroupAnnotation, GroupComponents}
import firrtl.annotations._
import firrtlTests.AnnotationSpec


class FindClockSourcesSpec extends AnnotationSpec {
  def execute(input: String, checks: Seq[Annotation], notChecks: Seq[Annotation], annotations: Seq[Annotation]): Unit = {
    val cr = compile(CircuitState(parse(input), ChirrtlForm, annotations), Seq(new FindClockSources()))
    checks.foreach { c =>
      cr.annotations.toSeq should contain (c)
    }
    notChecks.foreach { c =>
      cr.annotations.toSeq shouldNot contain (c)
    }
  }
  "test" should "do stuff" in {
    val input =
      """circuit Test:
        |  module Test :
        |    input in : UInt<8>
        |    input clk: Clock
        |    output out : UInt<8>
        |    inst a1 of A
        |    a1.clk <= asClock(UInt(1))
        |    inst a2 of A
        |    a2.clk <= clk
        |    inst b1 of B
        |    b1.clkin <= UInt(1)
        |    inst b2 of B
        |    b2.clkin <= UInt(1)
        |    inst c1 of C
        |    c1.clk <= clk
        |    inst c2 of C
        |    c2.clk <= clk
        |    a1.in <= in
        |    a2.in <= a1.out
        |    b1.in <= a2.out
        |    b2.in <= b1.out
        |    c1.in <= b2.out
        |    c2.in <= c1.out
        |    out <= c2.out
        |  module A :
        |    input in: UInt<8>
        |    input clk: Clock
        |    output out: UInt<8>
        |    reg r : UInt<8>, clk
        |    r <= in
        |    out <= r
        |  module B :
        |    input in: UInt<8>
        |    input clkin: UInt<1>
        |    output out: UInt<8>
        |    reg r : UInt<8>, asClock(clkin)
        |    r <= in
        |    out <= r
        |  module C :
        |    input in: UInt<8>
        |    input clk: Clock
        |    output out: UInt<8>
        |    inst clkdiv of CLKDIV
        |    clkdiv.clk <= clk
        |    reg r : UInt<8>, clkdiv.clk_2
        |    r <= in
        |    out <= r
        |  extmodule CLKDIV:
        |    input clk: Clock
        |    output clk_2: Clock
        |""".stripMargin


    execute(input, Nil, Nil, Nil)
  }

  "All clock source types" should "be collected" in {
    val input =
      """circuit Test:
        |  module Test :
        |    input in : UInt<8>
        |    input clk: Clock
        |    output out0 : UInt<8>
        |    output out1 : UInt<8>
        |    output out2 : UInt<8>
        |    reg r0: UInt<8>, clk
        |    reg r1: UInt<8>, asClock(bits(in, 1, 0))
        |    inst clkdiv of CLKDIV
        |    clkdiv.clk <= clk
        |    reg r2: UInt<8>, clkdiv.clkOut
        |
        |    r0 <= in
        |    r1 <= in
        |    r2 <= in
        |
        |    out0 <= r0
        |    out1 <= r1
        |    out2 <= r2
        |  extmodule CLKDIV:
        |    input clk: Clock
        |    output clkOut: Clock
        |""".stripMargin

    val C = CircuitTarget("Test")
    val Test = C.module("Test")
    val out = Test.ref("out")
    val clockSources = Seq(
      ClockSource(Seq(Test.ref("out0")), Test.ref("clk"), None),
      ClockSource(Seq(Test.ref("out1")), Test, Some("asClock$0")),
      ClockSource(Seq(Test.ref("out2")), Test.instOf("clkdiv", "CLKDIV").ref("clkOut"), None)
    )

    execute(input, clockSources, Nil, Nil)
  }

  "Clock source search" should "not pass through registers" in {
    val input =
      """circuit Test:
        |  module Test :
        |    input in : UInt<8>
        |    input clk: Clock
        |    output out0 : UInt<8>
        |    reg r0: UInt<8>, clk
        |    reg r1: UInt<8>, asClock(bits(in, 1, 0))
        |
        |    r1 <= in
        |    r0 <= r1
        |    out0 <= r0
        |""".stripMargin

    val C = CircuitTarget("Test")
    val Test = C.module("Test")
    val out = Test.ref("out")
    val clockSources = Seq( ClockSource(Seq(Test.ref("out0")), Test.ref("clk"), None) )
    val notClockSources = Seq( ClockSource(Seq(Test.ref("out0")), Test, Some("asClock$0")) )

    execute(input, clockSources, notClockSources, Nil)
  }

  // Check bundled registers

}
