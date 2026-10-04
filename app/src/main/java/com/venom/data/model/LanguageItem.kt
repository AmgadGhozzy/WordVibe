package com.venom.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LanguageItem(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val flag: String,
    val countryCode: String,
    val isDownloaded: Boolean = false,
    val downloadSizeMb: Float? = null,
    val isDownloading: Boolean = false
)

val LANGUAGES_LIST = listOf(
    LanguageItem(
        code = "en",
        englishName = "English",
        nativeName = "English",
        flag = "🇺🇸",
        countryCode = "US"
    ),
    LanguageItem(
        code = "ar",
        englishName = "Arabic",
        nativeName = "العربية",
        flag = "🇸🇦",
        countryCode = "SA"
    ),
    LanguageItem(
        code = "fr",
        englishName = "French",
        nativeName = "Français (France)",
        flag = "🇫🇷",
        countryCode = "FR"
    ),
    LanguageItem(
        code = "de",
        englishName = "German",
        nativeName = "Deutsch",
        flag = "🇩🇪",
        countryCode = "DE"
    ),
    LanguageItem(
        code = "es",
        englishName = "Spanish",
        nativeName = "Español (España)",
        flag = "🇪🇸",
        countryCode = "ES"
    ),
    LanguageItem(
        code = "zh",
        englishName = "Chinese",
        nativeName = "中文（简体）",
        flag = "🇨🇳",
        countryCode = "CN"
    ),
    LanguageItem(
        code = "pt",
        englishName = "Portuguese",
        nativeName = "Português (Brasil)",
        flag = "🇧🇷",
        countryCode = "BR"
    ),
    LanguageItem(
        code = "sw",
        englishName = "Swahili",
        nativeName = "Kiswahili",
        flag = "🇹🇿",
        countryCode = "TZ"
    ),
    LanguageItem(
        code = "cs",
        englishName = "Czech",
        nativeName = "Čeština",
        flag = "🇨🇿",
        countryCode = "CZ"
    ),
    LanguageItem(
        code = "hu",
        englishName = "Hungarian",
        nativeName = "Magyar",
        flag = "🇭🇺",
        countryCode = "HU"
    ),
    LanguageItem(
        code = "uk",
        englishName = "Ukrainian",
        nativeName = "Українська",
        flag = "🇺🇦",
        countryCode = "UA"
    ),
    LanguageItem(
        code = "tr",
        englishName = "Turkish",
        nativeName = "Türkçe",
        flag = "🇹🇷",
        countryCode = "TR"
    ),
    LanguageItem(
        code = "ja",
        englishName = "Japanese",
        nativeName = "日本語",
        flag = "🇯🇵",
        countryCode = "JP"
    ),
    LanguageItem(
        code = "fi",
        englishName = "Finnish",
        nativeName = "Suomi",
        flag = "🇫🇮",
        countryCode = "FI"
    ),
    LanguageItem(
        code = "sk",
        englishName = "Slovak",
        nativeName = "Slovenčina",
        flag = "🇸🇰",
        countryCode = "SK"
    ),
    LanguageItem(
        code = "he",
        englishName = "Hebrew",
        nativeName = "עברית",
        flag = "🇮🇱",
        countryCode = "IL"
    ),
    LanguageItem(
        code = "ms",
        englishName = "Malay",
        nativeName = "Bahasa Melayu (Malaysia)",
        flag = "🇲🇾",
        countryCode = "MY"
    ),
    LanguageItem(
        code = "hr",
        englishName = "Croatian",
        nativeName = "Hrvatski",
        flag = "🇭🇷",
        countryCode = "HR"
    ),
    LanguageItem(
        code = "vi",
        englishName = "Vietnamese",
        nativeName = "Tiếng Việt",
        flag = "🇻🇳",
        countryCode = "VN"
    ),
    LanguageItem(
        code = "ca",
        englishName = "Catalan",
        nativeName = "Català",
        flag = "🇪🇸",
        countryCode = "ES"
    ),
    LanguageItem(
        code = "th",
        englishName = "Thai",
        nativeName = "ไทย",
        flag = "🇹🇭",
        countryCode = "TH"
    ),
    LanguageItem(
        code = "pl",
        englishName = "Polish",
        nativeName = "Polski",
        flag = "🇵🇱",
        countryCode = "PL"
    ),
    LanguageItem(
        code = "sv",
        englishName = "Swedish",
        nativeName = "Svenska",
        flag = "🇸🇪",
        countryCode = "SE"
    ),
    LanguageItem(
        code = "id",
        englishName = "Indonesian",
        nativeName = "Indonesia",
        flag = "🇮🇩",
        countryCode = "ID"
    ),
    LanguageItem(
        code = "ro",
        englishName = "Romanian",
        nativeName = "Română",
        flag = "🇷🇴",
        countryCode = "RO"
    ),
    LanguageItem(
        code = "nl",
        englishName = "Dutch",
        nativeName = "Nederlands",
        flag = "🇳🇱",
        countryCode = "NL"
    ),
    LanguageItem(
        code = "ko",
        englishName = "Korean",
        nativeName = "한국어",
        flag = "🇰🇷",
        countryCode = "KR"
    ),
    LanguageItem(
        code = "el",
        englishName = "Greek",
        nativeName = "Ελληνικά",
        flag = "🇬🇷",
        countryCode = "GR"
    ),
    LanguageItem(
        code = "it",
        englishName = "Italian",
        nativeName = "Italiano",
        flag = "🇮🇹",
        countryCode = "IT"
    ),
    LanguageItem(
        code = "no",
        englishName = "Norwegian",
        nativeName = "Norsk",
        flag = "🇳🇴",
        countryCode = "NO"
    ),
    LanguageItem(
        code = "hi",
        englishName = "Hindi",
        nativeName = "हिन्दी",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "ru",
        englishName = "Russian",
        nativeName = "Русский",
        flag = "🇷🇺",
        countryCode = "RU"
    ),
    LanguageItem(
        code = "af",
        englishName = "Afrikaans",
        nativeName = "Afrikaans",
        flag = "🇿🇦",
        countryCode = "ZA"
    ),
    LanguageItem(
        code = "sq",
        englishName = "Albanian",
        nativeName = "Shqip",
        flag = "🇦🇱",
        countryCode = "AL"
    ),
    LanguageItem(
        code = "am",
        englishName = "Amharic",
        nativeName = "አማርኛ",
        flag = "🇪🇹",
        countryCode = "ET"
    ),
    LanguageItem(
        code = "hy",
        englishName = "Armenian",
        nativeName = "Հայերեն",
        flag = "🇦🇲",
        countryCode = "AM"
    ),
    LanguageItem(
        code = "my",
        englishName = "Burmese",
        nativeName = "ဗမာ",
        flag = "🇲🇲",
        countryCode = "MM"
    ),
    LanguageItem(
        code = "eu",
        englishName = "Basque",
        nativeName = "Euskara",
        flag = "🇪🇸",
        countryCode = "ES"
    ),
    LanguageItem(
        code = "bn",
        englishName = "Bengali",
        nativeName = "বাংলা",
        flag = "🇧🇩",
        countryCode = "BD"
    ),
    LanguageItem(
        code = "bg",
        englishName = "Bulgarian",
        nativeName = "Български",
        flag = "🇧🇬",
        countryCode = "BG"
    ),
    LanguageItem(
        code = "be",
        englishName = "Belarusian",
        nativeName = "Беларуская",
        flag = "🇧🇾",
        countryCode = "BY"
    ),
    LanguageItem(
        code = "da",
        englishName = "Danish",
        nativeName = "Dansk",
        flag = "🇩🇰",
        countryCode = "DK"
    ),
    LanguageItem(
        code = "et",
        englishName = "Estonian",
        nativeName = "Eesti",
        flag = "🇪🇪",
        countryCode = "EE"
    ),
    LanguageItem(
        code = "tl",
        englishName = "Filipino",
        nativeName = "Filipino",
        flag = "🇵🇭",
        countryCode = "PH"
    ),
    LanguageItem(
        code = "gl",
        englishName = "Galician",
        nativeName = "Galego",
        flag = "🇪🇸",
        countryCode = "ES"
    ),
    LanguageItem(
        code = "ka",
        englishName = "Georgian",
        nativeName = "ქართული",
        flag = "🇬🇪",
        countryCode = "GE"
    ),
    LanguageItem(
        code = "gu",
        englishName = "Gujarati",
        nativeName = "ગુજરાતી",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "is",
        englishName = "Icelandic",
        nativeName = "Íslenska",
        flag = "🇮🇸",
        countryCode = "IS"
    ),
    LanguageItem(
        code = "kn",
        englishName = "Kannada",
        nativeName = "ಕನ್ನಡ",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "kk",
        englishName = "Kazakh",
        nativeName = "Қазақ тілі",
        flag = "🇰🇿",
        countryCode = "KZ"
    ),
    LanguageItem(
        code = "km",
        englishName = "Khmer",
        nativeName = "ខ្មែរ",
        flag = "🇰🇭",
        countryCode = "KH"
    ),
    LanguageItem(
        code = "ky",
        englishName = "Kyrgyz",
        nativeName = "Кыргызча",
        flag = "🇰🇬",
        countryCode = "KG"
    ),
    LanguageItem(
        code = "lo",
        englishName = "Lao",
        nativeName = "ລາວ",
        flag = "🇱🇦",
        countryCode = "LA"
    ),
    LanguageItem(
        code = "lt",
        englishName = "Lithuanian",
        nativeName = "Lietuvių",
        flag = "🇱🇹",
        countryCode = "LT"
    ),
    LanguageItem(
        code = "lv",
        englishName = "Latvian",
        nativeName = "Latviešu",
        flag = "🇱🇻",
        countryCode = "LV"
    ),
    LanguageItem(
        code = "mk",
        englishName = "Macedonian",
        nativeName = "Македонски",
        flag = "🇲🇰",
        countryCode = "MK"
    ),
    LanguageItem(
        code = "ml",
        englishName = "Malayalam",
        nativeName = "മലയാളം",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "mr",
        englishName = "Marathi",
        nativeName = "मराठी",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "mn",
        englishName = "Mongolian",
        nativeName = "Монгол",
        flag = "🇲🇳",
        countryCode = "MN"
    ),
    LanguageItem(
        code = "ne",
        englishName = "Nepali",
        nativeName = "नेपाली",
        flag = "🇳🇵",
        countryCode = "NP"
    ),
    LanguageItem(
        code = "pa",
        englishName = "Punjabi",
        nativeName = "ਪੰਜਾਬੀ",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "fa",
        englishName = "Persian",
        nativeName = "فارسی",
        flag = "🇮🇷",
        countryCode = "IR"
    ),
    LanguageItem(
        code = "rm",
        englishName = "Romansh",
        nativeName = "Rumantsch",
        flag = "🇨🇭",
        countryCode = "CH"
    ),
    LanguageItem(
        code = "si",
        englishName = "Sinhala",
        nativeName = "සිංහල",
        flag = "🇱🇰",
        countryCode = "LK"
    ),
    LanguageItem(
        code = "sl",
        englishName = "Slovenian",
        nativeName = "Slovenščina",
        flag = "🇸🇮",
        countryCode = "SI"
    ),
    LanguageItem(
        code = "sr",
        englishName = "Serbian",
        nativeName = "Српски",
        flag = "🇷🇸",
        countryCode = "RS"
    ),
    LanguageItem(
        code = "ta",
        englishName = "Tamil",
        nativeName = "தமிழ்",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "te",
        englishName = "Telugu",
        nativeName = "తెలుగు",
        flag = "🇮🇳",
        countryCode = "IN"
    ),
    LanguageItem(
        code = "ur",
        englishName = "Urdu",
        nativeName = "اردو",
        flag = "🇵🇰",
        countryCode = "PK"
    ),
    LanguageItem(
        code = "zu",
        englishName = "Zulu",
        nativeName = "Zulu",
        flag = "🇿🇦",
        countryCode = "ZA"
    ),
    LanguageItem(
        code = "az",
        englishName = "Azerbaijani",
        nativeName = "Azərbaycan dili",
        flag = "🇦🇿",
        countryCode = "AZ"
    )
)