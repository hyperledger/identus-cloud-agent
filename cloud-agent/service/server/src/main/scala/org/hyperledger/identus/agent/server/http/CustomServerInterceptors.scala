package org.hyperledger.identus.agent.server.http

import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.shared.models.StatusCode
import org.log4s.*
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.interceptor.*
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.FailureMessages
import sttp.tapir.server.interceptor.decodefailure.{DecodeFailureHandler, DefaultDecodeFailureHandler}
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.interceptor.reject.RejectHandler
import sttp.tapir.server.model.ValuedEndpointOutput

object CustomServerInterceptors {

  private val logger: Logger = getLogger
  private val endpointOutput = jsonBody[ErrorResponse]

  private def defectHandler(response: ErrorResponse, maybeCause: Option[Throwable] = None) = {
    val statusCode = sttp.model.StatusCode(response.status)
    // Log defect as 'error' when status code matches a server error (5xx). Log other defects as 'debug'.
    (statusCode, maybeCause) match
      case (sc, Some(cause)) if sc.isServerError => logger.error(cause)(endpointOutput.codec.encode(response))
      case (sc, None) if sc.isServerError        => logger.error(endpointOutput.codec.encode(response))
      case (_, Some(cause))                      => logger.debug(cause)(endpointOutput.codec.encode(response))
      case (_, None)                             => logger.debug(endpointOutput.codec.encode(response))
    Some(ValuedEndpointOutput(endpointOutput, response).prepend(sttp.tapir.statusCode, statusCode))
  }

  def exceptionHandler[F[_]]: ExceptionHandler[F] = ExceptionHandler.pure[F](ctx =>
    defectHandler(
      ErrorResponse(
        StatusCode.InternalServerError.code,
        s"error:InternalServerError",
        "Internal Server Error",
        Some(
          s"An unexpected error occurred when processing the request: " +
            s"path=['${ctx.request.showShort}']"
        )
      ),
      Some(ctx.e)
    )
  )

  def rejectHandler[F[_]]: RejectHandler[F] = RejectHandler.pure[F](resultFailure =>
    defectHandler(
      ErrorResponse(
        StatusCode.NotFound.code,
        s"error:ResourcePathNotFound",
        "Resource Path Not Found",
        Some(s"The requested resource path doesn't exist.")
      )
    )
  )

  def decodeFailureHandler: DecodeFailureHandler = (ctx: DecodeFailureContext) => {

    /** As per the Tapir Decode Failures documentation:
      *
      * <pre> an “endpoint doesn’t match” result is returned if the request method or path doesn’t match. The http
      * library should attempt to serve this request with the next endpoint. The path doesn’t match if a path segment is
      * missing, there’s a constant value mismatch or a decoding error (e.g. parsing a segment to an Int fails).</pre>
      *
      * This means that in some failure cases, the handler should instruct Tapir to try processing the request with the
      * next endpoint, and not return an error response straight to the caller. This is achieved by returning Some (stop
      * and report error) or None (try next endpoint) from the failure handler processing logic. Here we rely on
      * [[DefaultDecodeFailureHandler.respond]] to decide whether to stop or continue based on the nature of the
      * reported failure, and determine the error response status code.
      *
      * @see
      *   <a href="https://docs.oracle.com/en/java/">Tapir Decode failures handling</a>
      */
    DefaultDecodeFailureHandler.respond(ctx) match
      case Some((sc, _)) =>
        val details = FailureMessages.failureMessage(ctx)
        defectHandler(
          ErrorResponse(
            sc.code,
            s"error:RequestBodyDecodingFailure",
            "Request Body Decoding Failure",
            Some(
              s"An error occurred when decoding the request body: " +
                s"path=['${ctx.request.showShort}'], details=[$details]"
            )
          )
        )
      case None => None
  }
}
