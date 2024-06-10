package com.linecorp

import com.linecorp.bot.model.message.FlexMessage
import com.linecorp.bot.model.message.flex.component.Box
import com.linecorp.bot.model.message.flex.component.Text
import com.linecorp.bot.model.message.flex.container.Bubble
import com.linecorp.bot.model.message.flex.unit.FlexLayout

fun main() {
    FlexMessage(
        "aaaaaaaaa",
        Bubble {
            header = Box {
                contents = listOf(
                    Box {
                        layout = FlexLayout.BASELINE
                    },
                    Text {
                        text = "header"
                    }
                )
            }
        }
    )
    println("Hello World!")
}