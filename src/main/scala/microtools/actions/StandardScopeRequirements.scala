package microtools.actions

object StandardScopeRequirements {

  val read = ScopeRequirement.require("R")

  val write = ScopeRequirement.require("W")

  val self = ScopeRequirement.require("S")

}
