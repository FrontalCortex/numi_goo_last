const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

const regex = /(private fun openChest\(\) \{\s*hintHandler\.removeCallbacks\(hintRunnable\)\s*)/;
const replacement = `$1binding.chestImage.setImageResource(currentRarity.openDrawableRes)\n        `;

content = content.replace(regex, replacement);

fs.writeFileSync(filepath, content, 'utf-8');
