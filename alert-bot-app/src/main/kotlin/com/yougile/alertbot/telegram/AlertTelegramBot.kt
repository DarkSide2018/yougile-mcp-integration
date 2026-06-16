package com.yougile.alertbot.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.yougile.alertbot.model.Alert
import com.yougile.alertbot.model.AlertPriority
import com.yougile.alertbot.service.AlertProcessingService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AlertTelegramBot(
    @Value("\${telegram.bot.token}") private val botToken: String,
    private val alertProcessingService: AlertProcessingService
) {
    private lateinit var bot: Bot

    @PostConstruct
    fun init() {
        bot = bot {
            token = botToken

            dispatch {
                command("start") {
                    val chatId = message.chat.id
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = """
                            YouGile Alert Bot
                            
                            Send me any alert message and I'll:
                            1. Analyze it with AI
                            2. Find matching template from knowledge base
                            3. Create a task in YouGile
                            
                            Commands:
                            /testalert - Create a demo alert
                            /help - Show this message
                        """.trimIndent(),
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = KeyboardReplyMarkup(
                            keyboard = listOf(
                                listOf(KeyboardButton("/testalert"))
                            ),
                            resizeKeyboard = true
                        )
                    )
                }

                command("help") {
                    val chatId = message.chat.id
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = """
                            Send any alert text to create a YouGile task.
                            /testalert - Generate a demo alert
                        """.trimIndent()
                    )
                }

                command("testalert") {
                    val chatId = message.chat.id
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "Generating demo alert..."
                    )

                    val demoAlert = Alert(
                        text = "CRITICAL: CPU usage 94% on prod-db-01\n" +
                                "Service: database\n" +
                                "Severity: critical\n" +
                                "Time: ${java.time.Instant.now()}\n" +
                                "Response time exceeded 5s for 3 consecutive checks",
                        priority = AlertPriority.CRITICAL,
                        category = "infrastructure"
                    )

                    try {
                        val result = alertProcessingService.processAlert(demoAlert)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "Alert processed!\n\n$result",
                            parseMode = ParseMode.MARKDOWN
                        )
                    } catch (e: Exception) {
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "Error processing alert: ${e.message}"
                        )
                    }
                }

                message {
                    val text = message.text ?: return@message
                    if (text.startsWith("/")) return@message

                    val chatId = message.chat.id
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "Processing alert..."
                    )

                    try {
                        val alert = Alert(text = text)
                        val result = alertProcessingService.processAlert(alert)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "Task created!\n\n$result",
                            parseMode = ParseMode.MARKDOWN
                        )
                    } catch (e: Exception) {
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "Error: ${e.message}"
                        )
                    }
                }
            }
        }

        bot.startPolling()
    }
}
