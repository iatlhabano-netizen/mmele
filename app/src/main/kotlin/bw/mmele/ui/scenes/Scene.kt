package bw.mmele.ui.scenes

/**
 * Illustrated backdrops celebrating real Botswana destinations. Every scene is drawn in code
 * (no photos); each caption fact was checked against UNESCO / Botswana Tourism / NASA sources
 * (see docs/SCENES.md).
 */
enum class Scene(val title: String, val fact: String) {
    OKAVANGO(
        "Okavango Delta",
        "A UNESCO World Heritage Site since 2014 — the List's 1,000th — and a great inland delta that never reaches the sea.",
    ),
    CHOBE(
        "Chobe National Park",
        "Botswana's first national park, where huge elephant herds gather on the Chobe River in the dry season.",
    ),
    MAKGADIKGADI(
        "Makgadikgadi Pans",
        "One of the largest salt pans on Earth: the dry bed of an ancient lake, ringed by ancient baobabs.",
    ),
    KALAHARI(
        "Central Kalahari",
        "Established in 1961, the Central Kalahari Game Reserve covers 52,800 km² — larger than the Netherlands.",
    ),
    TSODILO(
        "Tsodilo Hills",
        "A UNESCO World Heritage Site with over 4,500 rock paintings in just 10 km² — the \"Louvre of the Desert\".",
    ),
}
