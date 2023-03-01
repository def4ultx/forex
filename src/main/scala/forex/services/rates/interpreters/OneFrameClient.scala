package forex.services.rates.interpreters

import cats.effect.Sync
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.http4s.Uri
import org.http4s.client.Client
import forex.domain.Rate
import forex.services.rates.errors._
import org.http4s._
import cats.implicits._
import org.http4s.circe._
import scala.collection.mutable.{Map => MutableMap}

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class OneFrameClient[F[_] : Sync](token: String, endpoint: Uri, client: Client[F]) extends Algebra[F] {

  import forex.services.rates.interpreters.Protocol._

  var cache: MutableMap[Rate.Pair, Rate] = MutableMap.empty

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    def isNotExpired(t: OffsetDateTime): Boolean = t.plus(5, ChronoUnit.MINUTES).isAfter(OffsetDateTime.now)

    cache.get(pair) match {
      case Some(rate) if isNotExpired(rate.timestamp.value) =>
        val e: Either[Error, Rate] = Right(rate)
        e.pure[F]
      case _ =>
        getRate(pair)
    }
  }

  private def getRate(pair: Rate.Pair): F[Error Either Rate] = {
    val request = createRequest(pair)
    client.run(request).use(resp => {
      resp.asJsonDecode[List[GetRateResponse]].attempt.flatMap {
        case Right(rates) if rates.nonEmpty =>
          val r = rates.head.toDomain
          val e: Either[Error, Rate] = Right(r)
          cache.put(pair, r)
          e.pure[F]
        case Right(_) =>
          val r = OneFrameLookupFailed("invalid response from OneFrame")
          val e: Either[Error, Rate] = Left(r)
          e.pure[F]
        case Left(_) =>
          resp.asJsonDecode[ErrorResponse].map { x =>
            Left(OneFrameLookupFailed(x.error))
          }
      }
    })
  }

  private def createRequest(pair: Rate.Pair) = {
    val params = Query.fromMap(Map("pair" -> List(s"${pair.from}${pair.to}")))
    val uri = (endpoint / "rates").copy(query = params)
    Request[F](uri = uri, headers = Headers.of(Header("token", token)))
  }
}

object OneFrameClient {
  def apply[F[_] : Sync](token: String, endpoint: Uri, client: Client[F]): OneFrameClient[F] = new OneFrameClient(token, endpoint, client)
}
