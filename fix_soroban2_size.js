const fs = require('fs');
const file = 'app/src/main/java/com/example/app/abacus/AbacusBeadRenderer.kt';
let code = fs.readFileSync(file, 'utf8');

// Fix buildSoroban2Drawable
code = code.replace(
    /val scale = targetW \/ 120f\s+val offsetY = 10f \* scale\s+val pillHeight = 72f \* scale\s+\/\/ Draw outer black pill\s+paint\.color = android\.graphics\.Color\.BLACK\s+val outerRect = android\.graphics\.RectF\(0f, offsetY, targetW\.toFloat\(\), offsetY \+ pillHeight\)\s+val outerRadius = pillHeight \/ 2f\s+canvas\.drawRoundRect\(outerRect, outerRadius, outerRadius, paint\)\s+\/\/ Draw inner gradient pill\s+val padX = 12f \* scale\s+val padY = 12f \* scale\s+val innerRect = android\.graphics\.RectF\(padX, offsetY \+ padY, targetW - padX, offsetY \+ pillHeight - padY\)\s+val innerRadius = innerRect\.height\(\) \/ 2f/,
    \al scaleX = targetW / 120f
        val scaleY = targetH / 72f
        val offsetY = 0f
        val pillHeight = targetH.toFloat()

        // Draw outer black pill
        paint.color = android.graphics.Color.BLACK
        val outerRect = android.graphics.RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
        val outerRx = 36f * scaleX
        val outerRy = 36f * scaleY
        canvas.drawRoundRect(outerRect, outerRx, outerRy, paint)

        // Draw inner gradient pill
        val padX = 12f * scaleX
        val padY = 12f * scaleY
        val innerRect = android.graphics.RectF(padX, padY, targetW - padX, targetH - padY)
        val innerRx = 24f * scaleX
        val innerRy = 24f * scaleY\
);

code = code.replace(
    /canvas\.drawRoundRect\(innerRect, innerRadius, innerRadius, paint\)/,
    \canvas.drawRoundRect(innerRect, innerRx, innerRy, paint)\
);

// Fix density for buildSoroban2Drawable
code = code.replace(
    /val bitmap = android\.graphics\.Bitmap\.createBitmap\(targetW, targetH, android\.graphics\.Bitmap\.Config\.ARGB_8888\)/,
    \al bitmap = android.graphics.Bitmap.createBitmap(targetW, targetH, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.density = context.resources.displayMetrics.densityDpi\
);

// Fix density for buildReplacedBitmapDrawable
code = code.replace(
    /val bitmap = Bitmap\.createBitmap\(targetW, targetH, Bitmap\.Config\.ARGB_8888\)/,
    \al bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        bitmap.density = context.resources.displayMetrics.densityDpi\
);

fs.writeFileSync(file, code);
