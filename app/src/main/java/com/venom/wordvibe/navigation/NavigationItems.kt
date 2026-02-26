package com.venom.wordvibe.navigation

import com.venom.resources.R
import com.venom.ui.navigation.Screen

enum class NavigationItems(
    val screen: Screen,
    val icon: Int,
    val titleRes: Int,
    val showInBottomBar: Boolean = true
) {
    DIALOG(Screen.Dialog, R.drawable.icon_dialogs_activated, R.string.dialog_title, false),
    QUOTE(Screen.Quote, R.drawable.ic_quotes_fill, R.string.quote_title),
    PHRASE(Screen.Phrases, R.drawable.icon_dialog, R.string.phrase_title),
    WORDCARD(Screen.StackCard, R.drawable.icon_cards1, R.string.stackcard_title),
    QUIZ(Screen.Quiz, R.drawable.ic_question_fill, R.string.quiz_title),
    OCR(Screen.Ocr, R.drawable.icon_camera, R.string.ocr_title, false),
    TRANSLATE(Screen.Translation, R.drawable.icon_translate, R.string.translate_title);
}