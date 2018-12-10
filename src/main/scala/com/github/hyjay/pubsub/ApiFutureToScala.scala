package com.github.hyjay.pubsub

import java.util.concurrent.{ExecutionException, Executor}

import com.google.api.core.ApiFuture
import com.google.common.base.Preconditions
import com.google.common.base.Preconditions.checkState
import com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly
import com.google.common.util.concurrent.{FutureCallback, MoreExecutors}

import scala.concurrent.{Future, Promise}

private[pubsub] trait ApiFutureToScala {

  implicit class ApiFutureToScalaOps[T](af: ApiFuture[T]) {

    private def getDone(future: java.util.concurrent.Future[T]): T = {
      checkState(future.isDone, "Future was expected to be done: %s", future)
      getUninterruptibly(future)
    }

    private def addCallback(future: ApiFuture[T], callback: FutureCallback[_ >: T], executor: Executor): Unit = {
      Preconditions.checkNotNull(callback)
      val callbackListener = new Runnable() {
        override def run(): Unit = {
          try {
            callback.onSuccess(getDone(future))
          } catch {
            case e: ExecutionException =>
              callback.onFailure(e.getCause)
            case e: RuntimeException =>
              callback.onFailure(e)
            case e: Error =>
              callback.onFailure(e)
          }
        }
      }
      future.addListener(callbackListener, executor)
    }

    def toScala: Future[T] = {
      val p = Promise[T]()
      val cb = new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t
        def onSuccess(result: T): Unit    = p success result
      }
      addCallback(af, cb, MoreExecutors.directExecutor)
      p.future
    }
  }
}

private[pubsub] object ApiFutureToScala extends ApiFutureToScala
