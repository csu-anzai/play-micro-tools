package microtools.actions

object StandardScopeRequirements {

  def read(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("R")(block)

  def write(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("W")(block)

  def self(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("S")(block)

  val readAny  = read { case _  => true }
  val writeAny = write { case _ => true }
}
