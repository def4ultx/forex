package forex.services.rates.interpreters

import forex.domain.{Currency, Price, Rate, Timestamp}
import java.time.OffsetDateTime
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object Protocol {

  implicit val currencyDecoder: Decoder[Currency] = Decoder[String].map(Currency.fromString)
  implicit val rateResponseDecoder: Decoder[GetRateResponse] = deriveDecoder[GetRateResponse]
  implicit val errorResponseDecoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]

  case class GetRateResponse(from: Currency,
                             to: Currency,
                             bid: BigDecimal,
                             ask: BigDecimal,
                             price: BigDecimal,
                             time_stamp: OffsetDateTime) {
    def toDomain: Rate = Rate(Rate.Pair(from, to), Price(price), Timestamp(time_stamp))
  }

  case class ErrorResponse(error: String)

}
