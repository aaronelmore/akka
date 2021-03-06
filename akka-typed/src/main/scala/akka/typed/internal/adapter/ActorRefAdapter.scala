/**
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal
package adapter

import akka.{ actor ⇒ a }
import akka.annotation.InternalApi
import akka.dispatch.sysmsg

/**
 * INTERNAL API
 */
@InternalApi private[typed] class ActorRefAdapter[-T](val untyped: a.InternalActorRef)
  extends ActorRef[T] with internal.ActorRefImpl[T] {

  override def path: a.ActorPath = untyped.path
  override def tell(msg: T): Unit = untyped ! msg
  override def isLocal: Boolean = untyped.isLocal
  override def sendSystem(signal: internal.SystemMessage): Unit =
    ActorRefAdapter.sendSystemMessage(untyped, signal)
}

private[typed] object ActorRefAdapter {
  def apply[T](untyped: a.ActorRef): ActorRef[T] = new ActorRefAdapter(untyped.asInstanceOf[a.InternalActorRef])

  def toUntyped[U](ref: ActorRef[U]): akka.actor.InternalActorRef =
    ref match {
      case adapter: ActorRefAdapter[_] ⇒ adapter.untyped
      case _ ⇒
        throw new UnsupportedOperationException("only adapted untyped ActorRefs permissible " +
          s"($ref of class ${ref.getClass.getName})")
    }

  def sendSystemMessage(untyped: akka.actor.InternalActorRef, signal: internal.SystemMessage): Unit =
    signal match {
      case internal.Create()    ⇒ throw new IllegalStateException("WAT? No, seriously.")
      case internal.Terminate() ⇒ untyped.stop()
      case internal.Watch(watchee, watcher) ⇒ untyped.sendSystemMessage(
        sysmsg.Watch(
          toUntyped(watchee),
          toUntyped(watcher)))
      case internal.Unwatch(watchee, watcher)          ⇒ untyped.sendSystemMessage(sysmsg.Unwatch(toUntyped(watchee), toUntyped(watcher)))
      case internal.DeathWatchNotification(ref, cause) ⇒ untyped.sendSystemMessage(sysmsg.DeathWatchNotification(toUntyped(ref), true, false))
      case internal.NoMessage                          ⇒ // just to suppress the warning
    }
}
