package microtools.actions

import microtools.BusinessTry

abstract class StandardScopeRequirements {

  def checkedReadWithLogging(
      accessCheck: ScopeRequirement.AccessCheckWithLogging
  ): ScopeRequirement =
    ScopeRequirement.require("R", accessCheck)

  def checkedWriteWithLogging(
      accessCheck: ScopeRequirement.AccessCheckWithLogging
  ): ScopeRequirement =
    ScopeRequirement.require("W", accessCheck)

  def checkedSelfWithLogging(
      accessCheck: ScopeRequirement.AccessCheckWithLogging
  ): ScopeRequirement =
    ScopeRequirement.require("S", accessCheck)

  def checkedRead(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("R")(block)

  def checkedWrite(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("W")(block)

  def checkedSelf(block: ScopeRequirement.AccessCheck): ScopeRequirement =
    ScopeRequirement.require("S")(block)

  val read  = checkedRead { case _  => BusinessTry.success(true) }
  val write = checkedWrite { case _ => BusinessTry.success(true) }
  val self  = checkedSelf { case _  => BusinessTry.success(true) }
}

object StandardScopeRequirements extends StandardScopeRequirements
