package com.venom.data.mock

import com.venom.domain.model.CefrLevel
import com.venom.domain.model.RelatedWords
import com.venom.domain.model.WordFamily
import com.venom.domain.model.WordMaster

object MockWordData {

    // 1. A2 - Journey (The Essential Movement)
    val journeyWord = WordMaster(
        id = 105,
        wordEn = "journey",
        pos = "noun",
        cefrLevel = CefrLevel.A2,
        fromOxford = 1,
        rank = 850,
        frequency = 1,
        difficultyScore = 2,
        phoneticUs = "ˈdʒɜːrni",
        phoneticUk = "ˈdʒɜːni",
        phoneticAr = "جِرْنِي",
        translit = "Riḥla",
        syllabify = "jour•ney",
        definitionEn = "An act of traveling from one place to another, especially when they are far apart.",
        definitionAr = "رحلة أو انتقال من مكان لآخر، وغالباً ما تكون لمسافة طويلة أو ذات معنى معنوي.",
        primarySense = "Movement",
        semanticTags = listOf("#Travel", "#Adventure", "#Life", "#Movement"),
        usageNote = "A 'journey' focuses on the movement and experience of traveling. A 'trip' is usually shorter and focuses on the destination. 'Journey' can also be metaphorical (e.g., life journey).",
        register = "Neutral",
        category = "Travel & Movement",
        mnemonicAr = "كلمة (Journey) قريبة من (جرني).. تخيل حد بيـ (جرّك) في رحلة طويلة غصب عنك.",
        wordFamily = WordFamily(noun = "journey", verb = "journey"),
        synonyms = listOf("trip", "voyage", "expedition", "travel"),
        antonyms = listOf("immobility", "stay"),
        examples = mapOf(
            CefrLevel.A1 to "The journey is very long.",
            CefrLevel.A2 to "We went on a journey to the mountains.",
            CefrLevel.B1 to "Her journey to become a doctor was not easy.",
            CefrLevel.B2 to "Make sure you pack enough water for the journey.",
            CefrLevel.C1 to "The novel depicts a spiritual journey of self-discovery.",
            CefrLevel.C2 to "His political journey culminated in his appointment as Prime Minister."
        ),
        collocations = listOf("long journey", "safe journey", "spiritual journey"),
        relatedWords = RelatedWords(
            english = listOf("travel", "destination", "ticket"),
            arabic = listOf("سفر", "وجهة", "تذكرة")
        ),
        arabicAr = "رحلة",
        frenchFr = "voyage",
        germanDe = "Reise",
        spanishEs = "viaje",
        chineseZh = "旅程 [lǚchéng]",
        russianRu = "путешествие",
        portuguesePt = "jornada",
        japaneseJa = "旅 [tabi]",
        italianIt = "viaggio",
        turkishTr = "yolculuk"
    )

    // 2. B1 - Curious (The Cognitive Trigger)
    val curiousWord = WordMaster(
        id = 2140,
        wordEn = "curious",
        pos = "adjective",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = 1800,
        frequency = 3,
        difficultyScore = 4,
        phoneticUs = "ˈkjʊriəs",
        phoneticUk = "ˈkjʊəriəs",
        phoneticAr = "كْيُورِيَسْ",
        translit = "Fuḍūlī",
        syllabify = "cu•ri•ous",
        definitionEn = "Eager to know or learn something; implies a strong desire for knowledge.",
        definitionAr = "فضولي؛ لديه رغبة قوية في المعرفة أو تعلم شيء جديد.",
        primarySense = "Quality",
        semanticTags = listOf("#Learning", "#Intelligence", "#Personality"),
        usageNote = "Mainly means 'eager to learn' (positive). Can sometimes mean 'strange' or 'unusual' (e.g., a curious noise). Context determines the meaning.",
        register = "Neutral",
        category = "Mind & Thinking",
        mnemonicAr = "الشخص الـ (Curious) بيحب يعرف كل حاجة عشان يـ (Cure) أو يعالج جهله.",
        wordFamily = WordFamily(noun = "curiosity", adj = "curious", adv = "curiously"),
        synonyms = listOf("inquisitive", "interested", "eager"),
        antonyms = listOf("indifferent", "uninterested", "bored"),
        examples = mapOf(
            CefrLevel.A1 to "The cat is curious.",
            CefrLevel.A2 to "I am curious about your new house.",
            CefrLevel.B1 to "Children are naturally curious about the world.",
            CefrLevel.B2 to "I was curious to see what would happen next.",
            CefrLevel.C1 to "She has a curious habit of singing while she works.",
            CefrLevel.C2 to "It represents a curious anomaly in the otherwise consistent data."
        ),
        collocations = listOf("curious mind", "intellectually curious", "curious about"),
        relatedWords = RelatedWords(
            english = listOf("wonder", "inquiry", "interest"),
            arabic = listOf("تساؤل", "تحقيق", "اهتمام")
        ),
        arabicAr = "فضولي",
        frenchFr = "curieux",
        germanDe = "neugierig",
        spanishEs = "curioso",
        chineseZh = "好奇 [hàoqí]",
        russianRu = "любопытный",
        portuguesePt = "curioso",
        japaneseJa = "好奇心が強い [kōkishin ga tsuyoi]",
        italianIt = "curioso",
        turkishTr = "meraklı"
    )

    // 3. B2 - Evaluate (The Business/Academic Engine)
    val evaluateWord = WordMaster(
        id = 3520,
        wordEn = "evaluate",
        pos = "verb",
        cefrLevel = CefrLevel.B2,
        fromOxford = 1,
        rank = 2500,
        frequency = 3,
        difficultyScore = 6,
        phoneticUs = "ɪˈvæljueɪt",
        phoneticUk = null,
        phoneticAr = "إِڤَالْيُوِيتْ",
        translit = "Yuqayyim",
        syllabify = "e•val•u•ate",
        definitionEn = "To form an idea of the amount, number, or value of something; assess.",
        definitionAr = "تقييم شيء ما من حيث القيمة أو الجودة أو الأهمية.",
        primarySense = "Measure",
        semanticTags = listOf("#Analysis", "#Education", "#Business", "#Judgment"),
        usageNote = "Common in academic and business contexts. Synonymous with 'assess', but often implies a more calculated calculation of value (linking to the root 'value').",
        register = "Formal",
        category = "Work & Business",
        mnemonicAr = "كلمة (Evaluate) في نصها كلمة (Value) يعني قيمة.. إنت بتعمل إيه؟ بتحدد القيمة.",
        wordFamily = WordFamily(noun = "evaluation", verb = "evaluate", adj = "evaluative"),
        synonyms = listOf("assess", "judge", "appraise", "estimate"),
        antonyms = listOf("ignore", "neglect"),
        examples = mapOf(
            CefrLevel.A1 to "Teachers evaluate our work.",
            CefrLevel.A2 to "Please evaluate this product.",
            CefrLevel.B1 to "The doctor needs to evaluate the patient's condition.",
            CefrLevel.B2 to "We must evaluate the risks before investing money.",
            CefrLevel.C1 to "The committee met to evaluate the effectiveness of the new policy.",
            CefrLevel.C2 to "Scholars continue to evaluate the historical significance of the discovery."
        ),
        collocations = listOf("evaluate performance", "carefully evaluate", "reevaluate"),
        relatedWords = RelatedWords(
            english = listOf("score", "criteria", "judgment"),
            arabic = listOf("درجة", "معايير", "حكم")
        ),
        arabicAr = "يقيّم",
        frenchFr = "évaluer",
        germanDe = "bewerten",
        spanishEs = "evaluar",
        chineseZh = "评价 [píngjià]",
        russianRu = "оценивать",
        portuguesePt = "avaliar",
        japaneseJa = "評価する [hyōka suru]",
        italianIt = "valutare",
        turkishTr = "değerlendirmek"
    )

    // 4. C1 - Resilient (The Emotional Shield)
    val resilientWord = WordMaster(
        id = 5890,
        wordEn = "resilient",
        pos = "adjective",
        cefrLevel = CefrLevel.C1,
        fromOxford = 1,
        rank = 4200,
        frequency = 5,
        difficultyScore = 7,
        phoneticUs = "rɪˈzɪliənt",
        phoneticUk = null,
        phoneticAr = "رِزِلْيَنْتْ",
        translit = "Marin",
        syllabify = "re•sil•ient",
        definitionEn = "Able to withstand or recover quickly from difficult conditions.",
        definitionAr = "القدرة على التعافي بسرعة من الصعوبات؛ مرن وقوي.",
        primarySense = "Quality",
        semanticTags = listOf("#Strength", "#MentalHealth", "#Personality", "#Recovery"),
        usageNote = "Can be used for people (psychological strength) or objects/materials (returning to original shape after bending).",
        register = "Neutral",
        category = "Emotions & Feelings",
        mnemonicAr = "الشخص الـ (Resilient) عامل زي السوستة (Silent but strong).. تضغط عليه يرجع تاني أقوى.",
        wordFamily = WordFamily(noun = "resilience", adj = "resilient", adv = "resiliently"),
        synonyms = listOf("strong", "tough", "adaptable", "flexible"),
        antonyms = listOf("fragile", "weak", "vulnerable"),
        examples = mapOf(
            CefrLevel.A1 to "The ball is rubber and strong.",
            CefrLevel.A2 to "She is strong and gets better quickly.",
            CefrLevel.B1 to "Rubber is a resilient material.",
            CefrLevel.B2 to "Children are often more resilient than adults.",
            CefrLevel.C1 to "The local economy proved remarkably resilient during the crisis.",
            CefrLevel.C2 to "Building a climate-resilient infrastructure is a priority for the government."
        ),
        collocations = listOf("resilient economy", "highly resilient", "resilient spirit"),
        relatedWords = RelatedWords(
            english = listOf("rebound", "survivor", "grit"),
            arabic = listOf("ارتداد", "ناجي", "ثبات")
        ),
        arabicAr = "صامد",
        frenchFr = "résilient",
        germanDe = "belastbar",
        spanishEs = "resiliente",
        chineseZh = "有弹性的 [yǒu tánxìng de]",
        russianRu = "устойчивый",
        portuguesePt = "resiliente",
        japaneseJa = "回復力のある [kaifukuryoku no aru]",
        italianIt = "resiliente",
        turkishTr = "esnek"
    )

    // 5. C2 - Fluctuate (The Sophisticated Change)
    val fluctuateWord = WordMaster(
        id = 6500,
        wordEn = "fluctuate",
        pos = "verb",
        cefrLevel = CefrLevel.C2,
        fromOxford = 1,
        rank = 5800,
        frequency = 5,
        difficultyScore = 9,
        phoneticUs = "ˈflʌktʃueɪt",
        phoneticUk = null,
        phoneticAr = "فْلَكْشُوِيتْ",
        translit = "Yataqallab",
        syllabify = "fluc•tu•ate",
        definitionEn = "To rise and fall irregularly in number or amount.",
        definitionAr = "التأرجح أو التذبذب ارتفاعاً وانخفاضاً بشكل غير منتظم.",
        primarySense = "Change",
        semanticTags = listOf("#Economics", "#Math", "#Data", "#Statistics"),
        usageNote = "Commonly used with stock markets, temperatures, and currency exchange rates. Not typically used for physical movement of a person.",
        register = "Academic",
        category = "Numbers, Time & Math",
        mnemonicAr = "كلمة (Fluctuate) أولها (Flu) زي الأنفلونزا.. حرارة جسمك بتطلع وتنزل ومش ثابتة.",
        wordFamily = WordFamily(noun = "fluctuation", verb = "fluctuate", adj = "fluctuating"),
        synonyms = listOf("vary", "oscillate", "waver", "swing"),
        antonyms = listOf("stabilize", "steady", "remain"),
        examples = mapOf(
            CefrLevel.A1 to "The price goes up and down.",
            CefrLevel.A2 to "The weather changes every day.",
            CefrLevel.B1 to "Fruit prices often change in different seasons.",
            CefrLevel.B2 to "My mood tends to fluctuate when I am tired.",
            CefrLevel.C1 to "Oil prices fluctuate according to global demand.",
            CefrLevel.C2 to "The data revealed that metabolic rates fluctuate significantly during sleep cycles."
        ),
        collocations = listOf("fluctuating prices", "widely fluctuate", "seasonal fluctuations"),
        relatedWords = RelatedWords(
            english = listOf("graph", "volatility", "unstable"),
            arabic = listOf("رسم بياني", "تقلب", "غير مستقر")
        ),
        arabicAr = "يتقلب",
        frenchFr = "fluctuer",
        germanDe = "schwanken",
        spanishEs = "fluctuar",
        chineseZh = "波动 [bōdòng]",
        russianRu = "колебаться",
        portuguesePt = "flutuar",
        japaneseJa = "変動する [hendō suru]",
        italianIt = "fluttuare",
        turkishTr = "dalgalanmak"
    )

    // 6. A1 - Success (The Universal Goal)
    val successWord = WordMaster(
        id = 120,
        wordEn = "success",
        pos = "noun",
        cefrLevel = CefrLevel.A1,
        fromOxford = 1,
        rank = 320,
        frequency = 1,
        difficultyScore = 1,
        phoneticUs = "səkˈses",
        phoneticUk = null,
        phoneticAr = "سَكْسَسْ",
        translit = "Najāḥ",
        syllabify = "suc•cess",
        definitionEn = "The accomplishment of an aim or purpose.",
        definitionAr = "النجاح وتحقيق الأهداف المرجوة.",
        primarySense = "State",
        semanticTags = listOf("#Goals", "#Winning", "#Life", "#Business"),
        usageNote = "Note the family: Success (noun), Succeed (verb). Don't say 'I successed'—say 'I succeeded'.",
        register = "Neutral",
        category = "General / Common",
        mnemonicAr = "كلمة (Success) بتبدأ بحرف الـ (S).. افتكر إن السلّم (Success) هو طريقك للقمة.",
        wordFamily = WordFamily(
            noun = "success",
            verb = "succeed",
            adj = "successful",
            adv = "successfully"
        ),
        synonyms = listOf("victory", "triumph", "achievement"),
        antonyms = listOf("failure", "defeat", "loss"),
        examples = mapOf(
            CefrLevel.A1 to "I want success.",
            CefrLevel.A2 to "The party was a big success.",
            CefrLevel.B1 to "Hard work is the key to success.",
            CefrLevel.B2 to "She achieved success in her career very early.",
            CefrLevel.C1 to "The campaign was deemed a moderate success by analysts.",
            CefrLevel.C2 to "Measuring success purely by financial gain is often shortsighted."
        ),
        collocations = listOf("great success", "achieve success", "key to success"),
        relatedWords = RelatedWords(
            english = listOf("win", "fame", "wealth", "growth"),
            arabic = listOf("فوز", "شهرة", "ثروة", "نمو")
        ),
        arabicAr = "نجاح",
        frenchFr = "succès",
        germanDe = "Erfolg",
        spanishEs = "éxito",
        chineseZh = "成功 [chénggōng]",
        russianRu = "успех",
        portuguesePt = "sucesso",
        japaneseJa = "成功 [seikō]",
        italianIt = "successo",
        turkishTr = "başarı"
    )

    // 7. B1 - Decision (The Logical Step)
    val decisionWord = WordMaster(
        id = 980,
        wordEn = "decision",
        pos = "noun",
        cefrLevel = CefrLevel.B1,
        fromOxford = 1,
        rank = 1200,
        frequency = 3,
        difficultyScore = 3,
        phoneticUs = "dɪˈsɪʒn",
        phoneticUk = null,
        phoneticAr = "دِسِجَنْ",
        translit = "Qarār",
        syllabify = "de•ci•sion",
        definitionEn = "A conclusion or resolution reached after consideration.",
        definitionAr = "القرار أو النتيجة النهائية التي يتم الوصول إليها بعد تفكير.",
        primarySense = "Concept",
        semanticTags = listOf("#Logic", "#Choice", "#Leadership", "#Mind"),
        usageNote = "You 'make' a decision, you don't 'do' a decision. Often followed by 'to' (decision to go) or 'on' (decision on the matter).",
        register = "Neutral",
        category = "Mind & Thinking",
        mnemonicAr = "القرار (Decision) محتاج دقة (Precision)..",
        wordFamily = WordFamily(
            noun = "decision",
            verb = "decide",
            adj = "decisive",
            adv = "decisively"
        ),
        synonyms = listOf("choice", "judgment", "resolution"),
        antonyms = listOf("indecision", "hesitation"),
        examples = mapOf(
            CefrLevel.A1 to "It is a good decision.",
            CefrLevel.A2 to "I made a decision to buy the car.",
            CefrLevel.B1 to "Making the right decision can be difficult.",
            CefrLevel.B2 to "This is a final decision and cannot be changed.",
            CefrLevel.C1 to "The court's decision will impact future laws.",
            CefrLevel.C2 to "The CEO's abrupt decision had ramifications across the entire industry."
        ),
        collocations = listOf("make a decision", "final decision", "tough decision"),
        relatedWords = RelatedWords(
            english = listOf("option", "selection", "vote"),
            arabic = listOf("خيار", "اختيار", "تصويت")
        ),
        arabicAr = "قرار",
        frenchFr = "décision",
        germanDe = "Entscheidung",
        spanishEs = "decisión",
        chineseZh = "决定 [juédìng]",
        russianRu = "решение",
        portuguesePt = "decisão",
        japaneseJa = "決定 [kettei]",
        italianIt = "decisione",
        turkishTr = "karar"
    )

    // 8. B2 - Analyze (The Technical Process)
    val analyzeWord = WordMaster(
        id = 3600,
        wordEn = "analyze",
        pos = "verb",
        cefrLevel = CefrLevel.B2,
        fromOxford = 1,
        rank = 2100,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "ˈænəlaɪz",
        phoneticUk = null,
        phoneticAr = "أَنَالايْزْ",
        translit = "Yuḥallil",
        syllabify = "an•a•lyze",
        definitionEn = "To examine methodically and in detail the structure of something.",
        definitionAr = "تحليل وفحص الشيء بدقة وتفصيل لفهمه.",
        primarySense = "Process",
        semanticTags = listOf("#Research", "#Science", "#Data", "#Study"),
        usageNote = "In British English, it is spelled 'analyse'. It is often followed by objects like 'data', 'samples', or 'situations'.",
        register = "Academic",
        category = "Actions & Processes",
        mnemonicAr = "كلمة (Analyze) بتبدأ بـ (Ana).. أنا اللي لازم أحلل (Analyze) الموضوع ده بنفسي.",
        wordFamily = WordFamily(noun = "analysis", verb = "analyze", adj = "analytical", adv = "analytically"),
        synonyms = listOf("examine", "inspect", "investigate", "study"),
        antonyms = listOf("ignore", "synthesize", "overlook"),
        examples = mapOf(
            CefrLevel.A1 to "We look at the problem.",
            CefrLevel.A2 to "Please examine this paper.",
            CefrLevel.B1 to "The doctor needs to analyze the blood test.",
            CefrLevel.B2 to "Computers can analyze data much faster than humans.",
            CefrLevel.C1 to "We need to carefully analyze the underlying causes of the conflict.",
            CefrLevel.C2 to "The software is designed to analyze complex genomic sequences in real-time."
        ),
        collocations = listOf("analyze data", "carefully analyze", "statistically analyze"),
        relatedWords = RelatedWords(
            english = listOf("logic", "researcher", "pattern"),
            arabic = listOf("منطق", "باحث", "نمط")
        ),
        arabicAr = "يحلل",
        frenchFr = "analyser",
        germanDe = "analysieren",
        spanishEs = "analizar",
        chineseZh = "分析 [fēnxī]",
        russianRu = "анализировать",
        portuguesePt = "analisar",
        japaneseJa = "分析する [bunseki suru]",
        italianIt = "analizzare",
        turkishTr = "analiz etmek"
    )

    // 9. C1 - Advocate (The Social Voice)
    val advocateWord = WordMaster(
        id = 5120,
        wordEn = "advocate",
        pos = "verb",
        cefrLevel = CefrLevel.C1,
        fromOxford = 1,
        rank = 4100,
        frequency = 5,
        difficultyScore = 8,
        phoneticUs = "ˈædvəkeɪt",
        phoneticUk = null,
        phoneticAr = "أَدْفُوكَيتْ",
        translit = "Yu’ayyid",
        syllabify = "ad•vo•cate",
        definitionEn = "To publicly recommend or support a particular cause or policy.",
        definitionAr = "تأييد أو دعم قضية معينة بشكل علني.",
        primarySense = "Communication",
        semanticTags = listOf("#Politics", "#Law", "#Social", "#Support"),
        usageNote = "As a verb, pronounced /keɪt/. As a noun (a person who supports), pronounced /kət/. 'Advocate for' is a common structure.",
        register = "Formal",
        category = "Social Life & Communication",
        mnemonicAr = "كلمة (Advocate) بتفكرنا بكلمة (Advise).. هو شخص بينصح وبيدافع عن حقوق الناس.",
        wordFamily = WordFamily(noun = "advocacy", verb = "advocate"),
        synonyms = listOf("support", "recommend", "champion", "defend"),
        antonyms = listOf("oppose", "criticize", "reject"),
        examples = mapOf(
            CefrLevel.A1 to "He speaks for his friend.",
            CefrLevel.A2 to "The teacher supports good rules.",
            CefrLevel.B1 to "Doctors advocate healthy eating.",
            CefrLevel.B2 to "They advocate for better working conditions.",
            CefrLevel.C1 to "The organization advocates for the abolition of the death penalty.",
            CefrLevel.C2 to "Several economists advocate a complete restructuring of the tax system."
        ),
        collocations = listOf("publicly advocate", "advocate for change", "strong advocate"),
        relatedWords = RelatedWords(
            english = listOf("rights", "justice", "activist"),
            arabic = listOf("حقوق", "عدالة", "ناشط")
        ),
        arabicAr = "يؤيد",
        frenchFr = "préconiser",
        germanDe = "befürworten",
        spanishEs = "abogar",
        chineseZh = "提倡 [tíchàng]",
        russianRu = "защищать",
        portuguesePt = "advogar",
        japaneseJa = "提唱する [teishō suru]",
        italianIt = "sostenere",
        turkishTr = "savunmak"
    )

    // 10. C2 - Ambition (The Internal Drive)
    val ambitionWord = WordMaster(
        id = 6100,
        wordEn = "ambition",
        pos = "noun",
        cefrLevel = CefrLevel.C2,
        fromOxford = 1,
        rank = 3800,
        frequency = 5,
        difficultyScore = 7,
        phoneticUs = "æmˈbɪʃn",
        phoneticUk = null,
        phoneticAr = "أَمْبِيشَنْ",
        translit = "Ṭumūḥ",
        syllabify = "am•bi•tion",
        definitionEn = "A strong desire to do or to achieve something, typically requiring determination.",
        definitionAr = "طموح ورغبة قوية في تحقيق شيء ما.",
        primarySense = "Emotion",
        semanticTags = listOf("#Dreams", "#Success", "#Work", "#Personality"),
        usageNote = "Ambition is the noun; Ambitious is the adjective. Having ambition is positive, but 'blind ambition' implies being too greedy for success.",
        register = "Neutral",
        category = "Emotions & Feelings",
        mnemonicAr = "كلمة (Ambition) فيها (Mission).. الطموح هو المهمة الأساسية ليك في الحياة.",
        wordFamily = WordFamily(noun = "ambition", adj = "ambitious", adv = "ambitiously"),
        synonyms = listOf("drive", "goal", "aspiration", "desire"),
        antonyms = listOf("apathy", "laziness", "indifference"),
        examples = mapOf(
            CefrLevel.A1 to "I have a big dream.",
            CefrLevel.A2 to "His plan is to be rich.",
            CefrLevel.B1 to "Her ambition is to become a doctor.",
            CefrLevel.B2 to "She has a lot of ambition for her career.",
            CefrLevel.C1 to "The organization lacks the ambition to grow.",
            CefrLevel.C2 to "The government was criticized for its lack of political ambition."
        ),
        collocations = listOf("lifelong ambition", "political ambition", "burning ambition"),
        relatedWords = RelatedWords(
            english = listOf("vision", "target", "motivation"),
            arabic = listOf("رؤية", "هدف", "تحفيز")
        ),
        arabicAr = "طموح",
        frenchFr = "ambition",
        germanDe = "Ehrgeiz",
        spanishEs = "ambición",
        chineseZh = "抱负 [bàofù]",
        russianRu = "амбиция",
        portuguesePt = "ambição",
        japaneseJa = "野望 [yabō]",
        italianIt = "ambizione",
        turkishTr = "hırs / amaç"
    )

    /**
     * Complete mock list for testing swipe packs and navigation.
     */
    val mockWordList: List<WordMaster> = listOf(
        journeyWord,
        curiousWord,
        evaluateWord,
        resilientWord,
        fluctuateWord,
        decisionWord,
        ambitionWord,
        successWord,
        analyzeWord,
        advocateWord
    )
}