package com.example.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.util.AttributeSet
import android.util.LruCache
import android.view.View
import com.caverock.androidsvg.SVG
import kotlin.math.min

/**
 * [AvatarConfig]'i stilinin parçalarından ([AvatarArt]) çizen görünüm.
 *
 * SVG her değişiklikte bir kez çözülüp [Picture]'a kaydediliyor; onDraw yalnızca resmi
 * çiziyor. Aynı avatar (ör. seçenek ızgarasında kaydırınca geri gelen kutu) önbellekten gelir.
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var config: AvatarConfig = AvatarConfig.default(AvatarStyle.PERSONAS)
        set(value) {
            if (field == value && picture != null) return
            field = value
            rebuild()
        }

    /** Arkadaki renkli daire. Seçenek küçük resimlerinde kapalı. */
    var showBackground: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            rebuild()
        }

    /** Yüze yakın plan mı (göz ve ağız seçeneklerinin küçük resimleri), tamamı mı. */
    var closeUp: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            rebuild()
        }

    private var picture: Picture? = null

    init {
        rebuild()
    }

    private fun rebuild() {
        picture = render(context, config, showBackground, closeUp)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pic = picture ?: return
        val w = (width - paddingLeft - paddingRight).toFloat()
        val h = (height - paddingTop - paddingBottom).toFloat()
        if (w <= 0f || h <= 0f) return
        val scale = min(w / pic.width, h / pic.height)
        canvas.save()
        canvas.translate(paddingLeft + (w - pic.width * scale) / 2f, paddingTop + (h - pic.height * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.drawPicture(pic)
        canvas.restore()
    }

    companion object {
        /** Picture'lar her stilde 280 piksellik kareye kaydediliyor; ölçek çizerken veriliyor. */
        private const val PICTURE_SIZE = 280

        private val cache = LruCache<String, Picture>(120)

        private fun render(context: Context, config: AvatarConfig, circle: Boolean, closeUp: Boolean): Picture {
            val key = "${config.encode()}|$circle|$closeUp"
            cache.get(key)?.let { return it }
            val viewBox = if (closeUp) config.style.faceCloseUp else config.style.canvas
            val svg = SVG.getFromString(AvatarArt.buildSvg(context, config, circle, viewBox))
            svg.documentWidth = PICTURE_SIZE.toFloat()
            svg.documentHeight = PICTURE_SIZE.toFloat()
            val pic = svg.renderToPicture(PICTURE_SIZE, PICTURE_SIZE)
            cache.put(key, pic)
            return pic
        }

        /**
         * Başka bir kullanıcının avatarı (publicProfiles.avatarConfig) için hazır çizim; listelerdeki
         * harfli dairenin yerine konuyor. Avatar yoksa ya da okunamıyorsa null → harf gösterilir.
         */
        fun drawableFor(context: Context, encoded: String?, sizePx: Int): android.graphics.drawable.Drawable? {
            val config = AvatarConfig.decode(encoded) ?: return null
            return android.graphics.drawable.BitmapDrawable(context.resources, toBitmap(context, config, sizePx))
        }

        /** Alt bar ikonu gibi görünüm dışı yerler için. */
        fun toBitmap(context: Context, config: AvatarConfig, sizePx: Int, withBackground: Boolean = true): Bitmap {
            val pic = render(context, config, withBackground, closeUp = false)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.scale(sizePx / pic.width.toFloat(), sizePx / pic.height.toFloat())
            c.drawPicture(pic)
            return bmp
        }
    }
}
