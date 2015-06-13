package mkorolyov.util

import scala.concurrent.ExecutionContext

trait ExecutionContextProvider {
  implicit val ec: ExecutionContext
}

trait GlobalExecutionContext extends ExecutionContextProvider {
  override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
}