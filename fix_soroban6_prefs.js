const fs = require('fs');
const file = 'app/src/main/java/com/example/app/abacus/AbacusPreferences.kt';
let code = fs.readFileSync(file, 'utf8');

code = code.replace(
    /val SOROBAN6_COLOR_LABELS = arrayOf\("Gövde"\)/,
    \al SOROBAN6_COLOR_LABELS = arrayOf("Gövde", "Iþýk", "Gölge")\
);

code = code.replace(
    /private val SOROBAN6_COLOR_KEYS = arrayOf\("soroban6_color_0"\)/,
    \private val SOROBAN6_COLOR_KEYS = arrayOf("soroban6_color_0", "soroban6_color_1", "soroban6_color_2")\
);

code = code.replace(
    /private val SOROBAN6_DEFAULT_COLORS = intArrayOf\([\s\S]*?0xFF9AE2C6\.toInt\(\)\s*\)/,
    \private val SOROBAN6_DEFAULT_COLORS = intArrayOf(
        0xFF9AE2C6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )\
);

code = code.replace(
    /private val SOROBAN6_SEL_DEFAULT_COLORS = intArrayOf\([\s\S]*?0xFF9AE2C6\.toInt\(\)\s*\)/,
    \private val SOROBAN6_SEL_DEFAULT_COLORS = intArrayOf(
        0xFF9AE2C6.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt()
    )\
);

code = code.replace(
    /return IntArray\(1\) \{ i ->/,
    \eturn IntArray(3) { i ->\
);

code = code.replace(
    /for \(i in 0 until 1\) \{/,
    \or (i in 0 until 3) {\
);

fs.writeFileSync(file, code);
