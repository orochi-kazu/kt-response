package corp.orochi

sealed class Response<out A> {
  data class Error<out T>(val message: String) : Response<T>()
  data class Failure<out F : FailureCategory, out T>(val message: String, val status: F) : Response<T>()
  data class Success<out T>(val body: T) : Response<T>()

  inline infix fun <reified B> map(func: (A) -> B): Response<B> = when (this) {
    is Error -> Error(message)
    is Failure -> Failure(message, status)
    is Success -> try {
      Success(func(body))
    } catch (ex: Exception) {
      Error("$body -> ${B::class.java}:\n\n$ex")
    }
  }

  infix fun <B> flatMap(func: (A) -> Response<B>): Response<B> = map(func).flatten()

  val errorBody: ErrorBody
    get() = ErrorBody(
      when (this) {
        is Error -> message
        is Failure -> "$message ($status)"
        is Success -> "No error"
      }
    )

  companion object {
    fun <A> all(responses: List<Response<A>>): Response<List<A>> = try {
      Success(
        responses.map {
          it as? Success ?: throw UnwrapException(it)
        }.map { it.body }
      )
    } catch (ex: UnwrapException) {
      when (val r = ex.causeResponse) {
        is Failure<*> -> Failure(r.message, r.status)
        is Error<*> -> Error(r.message)
        else -> Error(ex.localizedMessage)
      }
    }

    fun <A> all(vararg responses: Response<A>) =
      all(responses.toList())

    class UnwrapException(val causeResponse: Response<*>) : Exception(causeResponse.errorBody.message)
  }
}

fun <A> Response<Response<A>>.flatten(): Response<A> = when (this) {
  is Error -> Error(message)
  is Failure -> Failure(message, status)
  is Success -> when (body) {
    is Error -> Error(body.message)
    is Failure -> Failure(body.message, body.status)
    is Success -> Success(body.body)
  }
}

typealias EmptyResponse = Response<Void?>
fun Response.Companion.emptySuccess(): EmptyResponse = Success(null)

interface FailureCategory

data class ErrorBody(val message: String)
