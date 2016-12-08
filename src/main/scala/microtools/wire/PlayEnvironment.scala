package microtools.wire

import akka.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}

import scala.concurrent.ExecutionContext

trait PlayEnvironment {
  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle
  def actorSystem: ActorSystem

  implicit def executionContext: ExecutionContext = actorSystem.dispatcher
}
