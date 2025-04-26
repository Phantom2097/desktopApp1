package ru.phantom2097

import com.google.gson.Gson
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tornadofx.*
import java.net.URLEncoder
import java.util.*

interface PaymentService {
//    @GET("/api/payment")
    suspend fun getPaymentAmount(): PaymentResponse
}

data class PaymentResponse(val amount: String)

class PaymentView : View("Платежное приложение"), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val amount = SimpleStringProperty("0")
    private val status = SimpleStringProperty("Не подключено")
    private val loading = SimpleBooleanProperty(false)
    private val errorUrl = SimpleStringProperty("")

    private val paymentService by lazy {
        Retrofit.Builder()
            .baseUrl("https://danil12121.github.io/Redirect/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PaymentService::class.java)
    }

    init {
        val app = FX.application as? MainApp
        val encodedData = app?.initialData

        if (!encodedData.isNullOrBlank()) {
            val url = app.initialData?.removePrefix("paymentapp://")
            val data = url?.substringAfter("data=")
            decodeInitialData(data)
        }
    }

    override val root = vbox {
        paddingAll = 25
        spacing = 15.0

        label("Платежная система") {
            style {
                fontSize = 26.px
                fontWeight = FontWeight.BOLD
            }
        }

        hbox(spacing = 15) {
            label("Статус:") { style { fontSize = 18.px } }
            label(status) {
                bind(status)
                style {
                    fontSize = 18.px
                    textFill = Color.DARKRED
                }
            }
        }

        hbox(spacing = 15) {
            label("Сумма платежа:") { style { fontSize = 18.px } }
            label(amount) {
                bind(amount)
                style {
                    fontSize = 18.px
                    textFill = Color.DARKBLUE
                    fontWeight = FontWeight.BOLD
                }
            }
        }

        hbox(spacing = 15) {
            button("Получить сумму платежа") {
                enableWhen(!loading)
                style {
                    fontSize = 16.px
                    paddingAll = 10
                    backgroundColor += Color.LIGHTGREEN
                }
                action {
                    fetchPaymentAmount()
                }
            }

            button("Перейти на сайт") {
                visibleWhen(errorUrl.isNotEmpty) // <-- Кнопка появляется только при ошибке
                enableWhen(errorUrl.isNotEmpty)
                style {
                    fontSize = 16.px
                    paddingAll = 10
                    backgroundColor += Color.LIGHTPINK
                }
                action {
                    errorUrl.value.takeIf { it.isNotBlank() }?.let { url ->
                        FX.application.hostServices.showDocument(url)
                    }
                }
            }
        }

        progressindicator {
            visibleWhen(loading)
        }
    }

    private fun decodeInitialData(encodedData: String?) {
        try {
            val decodedJson = String(Base64.getDecoder().decode(encodedData))
            val jsonObject = Gson().fromJson(decodedJson, Map::class.java)

            val receivedAmount = (jsonObject["amount"] as? Double)?.toInt()?.toString() ?: "Не указано"
            val receivedCurrency = jsonObject["currency"] as? String ?: "Не указана валюта"

            runLater {
                amount.value = "$receivedAmount $receivedCurrency"
                status.value = "Данные из ссылки получены"
            }
        } catch (e: Exception) {
            println("Ошибка расшифровки данных: ${e.message}")
            runLater {
                status.value = "Ошибка расшифровки данных"
            }
        }
    }


    private fun fetchPaymentAmount() {
        loading.value = true
        status.value = "Загрузка..."
        errorUrl.value = ""

        launch {
            try {
                val result = paymentService.getPaymentAmount().amount
                runLater {
                    amount.value = result
                    status.value = "Успешно загружено"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runLater {
                    status.value = "Ошибка: ${e.message}"
                    handlePaymentError(e)
                }
            } finally {
                runLater {
                    loading.value = false
                }
            }
        }
    }

    /**
     * Пока что обрабатывает ошибку получения данных, и перекидывает на сайт
     */
    private fun handlePaymentError(error: Exception) {
        try {
            // Шифрование данных об ошибке (упрощенный пример)
            val errorData = mapOf(
                "error" to error.message,
                "timestamp" to System.currentTimeMillis(),
                "amount" to amount.value,
            )

            val jsonData = Gson().toJson(errorData)
            val encryptedData = Base64.getEncoder().encodeToString(jsonData.toByteArray())

            // Открываем браузер с данными об ошибке
            val url = "https://serebrovskaya.github.io/ifAppNotFound/folder_for_pay/index_pay.html" +
                    "?data=${URLEncoder.encode(encryptedData, "UTF-8")}"

            errorUrl.value = url

        } catch (e: Exception) {
            println("Failed to handle error: ${e.message}")
        }
    }

    override fun onDock() {
        super.onDock()
        // Инициализация при необходимости
    }

    override fun onUndock() {
        super.onUndock()
        // Очистка ресурсов
        cancel()
    }
}

class MainApp : App(PaymentView::class) {
    var initialData: String? = null
    override fun start(stage: Stage) {
        if (parameters.raw.isNotEmpty()) {
            initialData = parameters.raw.first()
        }
        stage.width = 800.0
        stage.height = 600.0
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    launch<MainApp>(args)
}