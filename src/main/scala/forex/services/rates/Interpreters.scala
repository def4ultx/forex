package forex.services.rates

import cats.Applicative
import interpreters._
import cats.effect.ConcurrentEffect
import forex.config.OneFrameConfig
import org.http4s.Uri
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_] : Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_] : ConcurrentEffect](config: OneFrameConfig, client: Client[F]): Algebra[F] = {
    val uri = Uri.unsafeFromString(s"http://${config.host}:${config.port}")
    OneFrameClient[F](config.token, uri, client)
  }
}
