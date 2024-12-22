import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import tech.rocksavage.args.Conf


class ConfTest
  extends AnyFlatSpec {

  "Conf" should "have module name foo in verilog subcommand" in {
    val conf = new Conf(Seq("verilog", "--module", "foo"))
    conf.subcommand.get shouldBe conf.verilog

  }
}