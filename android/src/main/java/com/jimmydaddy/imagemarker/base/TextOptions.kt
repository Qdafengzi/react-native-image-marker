package com.jimmydaddy.imagemarker.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.views.text.ReactFontManager
import com.jimmydaddy.imagemarker.base.Constants.DEFAULT_MARGIN
import kotlin.math.ceil

@Suppress("DEPRECATION")
data class TextOptions(val options: ReadableMap) {
  private var text: String? = options.getString("text")
  private var x: String?
  private var y: String?
  private var positionEnum: PositionEnum?
  private var style: TextStyle

  init {
    if (text == null) {
      throw MarkerError(ErrorCode.PARAMS_REQUIRED, "mark text is required")
    }
    val positionOptions =
      if (null != options.getMap("positionOptions")) options.getMap("positionOptions") else null
    x = if (positionOptions!!.hasKey("X")) Utils.handleDynamicToString(positionOptions.getDynamic("X")) else null
    y = if (positionOptions.hasKey("Y")) Utils.handleDynamicToString(positionOptions.getDynamic("Y")) else null
    positionEnum =
      if (null != positionOptions.getString("position")) PositionEnum.getPosition(
        positionOptions.getString("position")
      ) else null
    style = TextStyle(options.getMap("style"))
  }

  fun applyStyle(
    context: ReactApplicationContext,
    canvas: Canvas,
    maxWidth: Int,
    maxHeight: Int
  ) {
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.DEV_KERN_TEXT_FLAG)
    textPaint.isAntiAlias = true
    if (null != style.shadowLayerStyle) {
      textPaint.setShadowLayer(
        style.shadowLayerStyle!!.radius,
        style.shadowLayerStyle!!.dx,
        style.shadowLayerStyle!!.dy,
        style.shadowLayerStyle!!.color
      )
    }
    var typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    try {
      //设置字体失败时使用默认字体
      typeface = ReactFontManager.getInstance()
        .getTypeface(style.fontName!!, Typeface.NORMAL, context.assets)
    } catch (e: Exception) {
      textPaint.typeface = Typeface.DEFAULT
    }
    val textSize = TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      style.fontSize.toFloat(),
      context.resources.displayMetrics
    )
    textPaint.isAntiAlias = true
    textPaint.textSize = textSize
    textPaint.color = Color.parseColor(Utils.transRGBColor(style.color))
    textPaint.isUnderlineText = style.underline
    textPaint.textSkewX = style.skewX!!
    if (style.italic && style.bold) {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
    } else if (style.italic) {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    } else if (style.bold) {
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    textPaint.isStrikeThruText = style.strikeThrough
    textPaint.typeface = typeface
    textPaint.textAlign = style.textAlign
    // ALIGN_CENTER, ALIGN_NORMAL, ALIGN_OPPOSITE
    val textLayout: StaticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val builder =
        StaticLayout.Builder.obtain(text!!, 0, text!!.length, textPaint, canvas.width)
      builder.setAlignment(Layout.Alignment.ALIGN_NORMAL)
      builder.setLineSpacing(0.0f, 1.0f)
      builder.setIncludePad(false)
      builder.build()
    } else {
      StaticLayout(
        text,
        textPaint,
        canvas.width,
        Layout.Alignment.ALIGN_NORMAL,
        1.0f,
        0.0f,
        false
      )
    }

    val textHeight = textLayout.height
    var textWidth =0
    val count = textLayout.lineCount
    for (a in 0 until count) {
      textWidth = ceil(
        textWidth.toFloat().coerceAtLeast(textLayout.getLineWidth(a) + textLayout.getLineLeft(a)).toDouble()
      ).toInt()
    }
    val margin = DEFAULT_MARGIN
    var position = Position(margin, margin)
    if (positionEnum != null) {
      position = Position.getTextPosition(
        positionEnum,
        maxWidth,
        maxHeight,
        textWidth,
        textHeight
      )
    } else {
      if (null != x) {
        position.x = Utils.parseSpreadValue(x, maxWidth.toFloat())
      }
      if (null != y) {
        position.y = Utils.parseSpreadValue(y, maxHeight.toFloat())
      }
    }



    val x = position.x
    val y = position.y

    Log.d(
      "Marker", "角度：${style.rotate.toFloat()}\n" +
              "textWidth:${textWidth}\n" +
              "textHeight:${textHeight}\n" +
              "maxWidth:${maxWidth}\n" +
              "maxHeight:${maxHeight}\n" +
              "x:${x}\n" +
              "y:${y}\n" +
              "this x:${this.x}\n" +
              "this y:${this.y}\n" +
              "textSize:${textSize}\n" +
              ""
    )

    //文字宽度的一半
    val centerX: Float = x + (textWidth shr 1)
    val centerY: Float = y + (textHeight shr 1)

    canvas.save()

    canvas.rotate(style.rotate.toFloat(), centerX, centerY)


//    val textRectWithPosition = RectF(this.x?.toFloat()?:0f, this.y?.toFloat()?:0f , textWidth.toFloat(), textHeight.toFloat())
//    canvas.rotate(style.rotate.toFloat(), textRectWithPosition.centerX(), textRectWithPosition.centerY())

    // Draw text background
    if (null != style.textBackgroundStyle) {
      Log.d("Marker","textBackgroundStyle")

      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)
      paint.style = Paint.Style.FILL
      paint.color = style.textBackgroundStyle!!.color
      val bgInsets = style.textBackgroundStyle!!.toEdgeInsets(maxWidth, maxHeight)
      var bgRect = RectF(x - bgInsets.left, y - bgInsets.top, x + textWidth + bgInsets.right, y + textHeight + bgInsets.bottom)
      when (style.textBackgroundStyle!!.type) {
        "stretchX" -> {
          bgRect = RectF(0f, y - bgInsets.top, maxWidth.toFloat(),
            y + textHeight + bgInsets.bottom
          )
        }

        "stretchY" -> {
          bgRect = RectF(x - bgInsets.left, 0f,
            x + textWidth + bgInsets.right, maxHeight.toFloat())
        }
      }

      if (style.textBackgroundStyle!!.cornerRadius != null) {
        val path = Path()
        Log.d("Marker","cornerRadius")
        path.addRoundRect(bgRect, style.textBackgroundStyle!!.cornerRadius!!.radii(bgRect), Path.Direction.CW);

        canvas.drawPath(path, paint);
      } else {
        canvas.drawRect(bgRect, paint)
      }
    }

    Log.d("Marker","TextAlign:${textPaint.textAlign}")
    val textX = when(textPaint.textAlign) {
      Paint.Align.RIGHT -> x + textWidth
      Paint.Align.CENTER -> x + textWidth / 2
      Paint.Align.LEFT -> x
    }
    canvas.translate(textX, y)
    textLayout.draw(canvas)
    canvas.restore()
  }
}
