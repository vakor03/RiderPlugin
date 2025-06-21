package com.jetbrains.rider.plugins.myawesomeplugin1

import com.jetbrains.rider.settings.simple.SimpleOptionsPage

class SamplePage : SimpleOptionsPage(
    name = OptionPagesBundle.message("configurable.name.optionpages.options.title"),
    pageId = "SamplePage" // Must be in sync with SamplePage.PID
) {
    override fun getId(): String {
        return "SamplePage"
    }
}