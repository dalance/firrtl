package firrtl.passes
import firrtl.ir.Circuit
import firrtl.transforms.VerilogRename

@deprecated("Use transforms.VerilogRename, will be removed in 1.3", "1.2")
object VerilogRename extends Pass {
  override def run(c: Circuit): Circuit = new VerilogRename().run(c)
}
