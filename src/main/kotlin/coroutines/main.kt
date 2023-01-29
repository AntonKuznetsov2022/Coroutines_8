import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val gson = Gson()
private val BASE_URL = "http://127.0.0.1:9999"
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor(::println).apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

fun main() {
    CoroutineScope(EmptyCoroutineContext).launch {
        try {
            val posts = getPosts()
            val result = posts.map {
                async {
                    PostWithComments(
                        it, getAuthors(it.authorId),
                        getComments(it.id).map { CommentsWithAuthor(it, getAuthors(it.authorId)) }
                    )
                }
            }.awaitAll()

            println(result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    Thread.sleep(30_000L)
}

suspend fun <T> parseResponse(url: String, typeToken: TypeToken<T>): T {
    val response = makeRequest(url)
    return withContext(Dispatchers.Default) {
        gson.fromJson(requireNotNull(response.body).string(), typeToken.type)
    }
}

suspend fun makeRequest(url: String): Response =
    suspendCoroutine { continuation ->
        client.newCall(
            Request.Builder()
                .url(url)
                .build()
        )
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }


suspend fun getPosts(): List<Post> = parseResponse(
    "$BASE_URL/api/posts",
    object : TypeToken<List<Post>>() {},
)

suspend fun getComments(postId: Long): List<Comment> = parseResponse(
    "$BASE_URL/api/posts/$postId/comments",
    object : TypeToken<List<Comment>>() {},
)

suspend fun getAuthors(id: Long): Author = parseResponse(
    "$BASE_URL/api/authors/$id",
    object : TypeToken<Author>() {},
)
