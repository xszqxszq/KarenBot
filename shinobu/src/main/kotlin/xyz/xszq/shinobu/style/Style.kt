package xyz.xszq.shinobu.style

import org.jetbrains.skia.Color

data class Style(
    var width: Float ?= null,
    var minWidth: Float ?= null,
    var maxWidth: Float ?= null,

    var height: Float ?= null,
    var minHeight: Float ?= null,
    var maxHeight: Float ?= null,

    var backgroundColor: Int ?= null,
    var backgroundImage: String ?= null,
    var backgroundSize: BackgroundSize = BackgroundSize.AUTO,
    var backgroundPosition: BackgroundPosition = BackgroundPosition.TOP_LEFT,
    var backgroundOpacity: Float = 1.0f,

    var margin: Spacing = Spacing(),
    var padding: Spacing = Spacing(),

    var flexDirection: FlexDirection = FlexDirection.ROW,
    var flexWrap: FlexWrap = FlexWrap.NOWRAP,
    var justifyContent: JustifyContent = JustifyContent.FLEX_START,
    var alignItems: AlignItems = AlignItems.STRETCH,

    var textSize: Float = 14f,
    var minTextSize: Float ?= null,
    var textColor: Int = Color.BLACK,
    var fontFamilies: List<String>? = null,
    var fontWeight: Int = 400,
    var textStroke: TextStroke ?= null,
    var textShadow: TextShadow ?= null,
    var textAlign: TextAlign = TextAlign.LEFT,
    var whiteSpace: WhiteSpace = WhiteSpace.NORMAL,

    var opacity: Float = 1.0f,
    var objectFit: ObjectFit = ObjectFit.FILL,
    var maskImage: String ?= null,
) {
    fun deepCopy(): Style {
        return this.copy(
            margin = this.margin.copy(),
            padding = this.padding.copy(),
            textStroke = this.textStroke ?.copy(),
            textShadow = this.textShadow ?.copy()
        )
    }
}