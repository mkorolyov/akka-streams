package mkorolyov.db

import mkorolyov.util.GlobalExecutionContext

trait DbKernel {
  val currencyRepo: CurrencyRepository
}

trait MongoDbKernel extends DbKernel {
  val currencyRepo = new MongoCurrencyRespository with GlobalExecutionContext {}
}
