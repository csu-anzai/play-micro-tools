package microtools.actions

object StandardScopeRequirements {

  def checkedRead(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("R")(block)

  def checkedWrite(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("W")(block)

  def checkedSelf(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("S")(block)

  val read  = checkedRead { case _  => true }
  val write = checkedWrite { case _ => true }
  val self  = checkedSelf { case _  => true }
}
