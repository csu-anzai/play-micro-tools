package microtools.actions

object StandardScopeRequirements {

  val read = ScopeRequirement.require("read")

  val write = ScopeRequirement.require("write")

  val self = ScopeRequirement.require("self")

}
