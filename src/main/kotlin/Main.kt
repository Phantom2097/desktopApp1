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
import retrofit2.http.GET
import tornadofx.*
import java.net.URLDecoder
import java.util.*

interface PaymentService {
    @GET("/payment.json")
    suspend fun getPaymentAmount(): PaymentResponse
}

data class PaymentResponse(val amount: Int, val currency: String)

class PaymentView : View("Платежное приложение"), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private var rawData: String? = null
    private val amount = SimpleStringProperty("0")
    private val currency = SimpleStringProperty("RUB")
    private val status = SimpleStringProperty("Ожидание данных...")
    private val loading = SimpleBooleanProperty(false)
    private val websiteUrl = SimpleStringProperty("https://serebrovskaya.github.io/ifAppNotFound/")

    private val paymentService by lazy {
        Retrofit.Builder()
            .baseUrl("https://danil12121.github.io/Redirect/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PaymentService::class.java)
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
            label("Сумма:") { style { fontSize = 18.px } }
            label(amount) {
                bind(amount)
                style {
                    fontSize = 18.px
                    textFill = Color.DARKBLUE
                    fontWeight = FontWeight.BOLD
                }
            }
            label(currency) {
                bind(currency)
                style {
                    fontSize = 18.px
                    textFill = Color.DARKGREEN
                }
            }
        }

        button("Перейти на сайт") {
            style {
                fontSize = 16.px
                paddingAll = 10
                backgroundColor += Color.LIGHTPINK
            }
            action {
                openWebsite()
            }
        }

        progressindicator {
            visibleWhen(loading)
        }
    }

    init {
        // Автоматическая обработка входящих данных при запуске
        val app = FX.application as? MainApp
        app?.initialData?.let { url ->
            rawData = extractDataFromUrl(url)
            processIncomingData()
        } ?: fetchPaymentData() // Если данных нет, загружаем с сервера
    }

    private fun extractDataFromUrl(url: String): String? {
        return try {
            val cleanUrl = url.removePrefix("paymentapp://")
            val dataParam = cleanUrl.substringAfter("data=").substringBefore("&")
            URLDecoder.decode(dataParam, "UTF-8")
        } catch (e: Exception) {
            null
        }
    }

    private fun processIncomingData() {
        try {
            rawData?.let { data ->
                val jsonString = Base64.getDecoder().decode(data).toString(Charsets.UTF_8)
                val paymentData = Gson().fromJson(jsonString, Map::class.java)

                amount.set(paymentData["amount"]?.toString() ?: "0")
                currency.set(paymentData["currency"]?.toString() ?: "RUB")
                status.set("Данные получены")

                // Формируем URL для перехода на сайт с исходными данными
                websiteUrl.set("https://serebrovskaya.github.io/ifAppNotFound/folder_for_pay/index_pay.html?data=$rawData")
            } ?: run {
                status.set("Ошибка: неверный формат данных")
                websiteUrl.set("https://serebrovskaya.github.io/ifAppNotFound/")
            }
        } catch (e: Exception) {
            println("Ошибка обработки данных: ${e.message}")
            status.set("Ошибка обработки данных")
            websiteUrl.set("https://serebrovskaya.github.io/ifAppNotFound/")
            fetchPaymentData()
        }
    }

    private fun fetchPaymentData() {
        loading.value = true
        status.value = "Загрузка данных..."
        websiteUrl.set("https://serebrovskaya.github.io/ifAppNotFound/")

        launch {
            try {
                val response = paymentService.getPaymentAmount()
                runLater {
                    amount.value = response.amount.toString()
                    currency.value = response.currency
                    status.value = "Данные успешно загружены"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runLater {
                    status.value = "Ошибка: ${e.message}"
                }
            } finally {
                runLater {
                    loading.value = false
                }
            }
        }
    }

    private fun openWebsite() {
        FX.application.hostServices.showDocument(websiteUrl.value)
    }

    override fun onUndock() {
        cancel()
    }
}

class MainApp : App(PaymentView::class) {
    var initialData: String? = null

    override fun start(stage: Stage) {
        stage.width = 800.0
        stage.height = 600.0

        // Обработка аргументов командной строки
        if (parameters.raw.isNotEmpty()) {
            initialData = parameters.raw[0]
        }

        super.start(stage)
    }
}

fun main(args: Array<String>) {
    System.setProperty("prism.allowhidpi", "false")
    launch<MainApp>(args)
}