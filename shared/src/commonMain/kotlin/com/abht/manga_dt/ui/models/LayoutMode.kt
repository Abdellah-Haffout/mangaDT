package com.abht.manga_dt.ui.models

enum class LayoutMode {
    COMFORTABLE,
    COMPACT,
    LIST
}

enum class ReadingMode(val label: String) {
    WEBTOON("Webtoon"),
    RTL("Right to Left"),
    LTR("Left to Right"),
    VERTICAL_PAGED("Vertical Paged")
}

enum class ReaderBackground(val label: String) {
    BLACK("AMOLED Black"),
    DARK_GRAY("Dark Gray"),
    WHITE("White")
}

enum class ReaderScaleMode(val label: String) {
    FIT_WIDTH("Fit Width"),
    FIT_SCREEN("Fit Screen"),
    FIT_HEIGHT("Fit Height"),
    ORIGINAL("Original")
}

