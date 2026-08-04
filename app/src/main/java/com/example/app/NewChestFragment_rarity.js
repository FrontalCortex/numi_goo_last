const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

const regex = /ChestRarity\.COMMON -> when \{[\s\S]*?(?=ChestRarity\.LEGENDARY -> ChestRarity\.LEGENDARY)/;

const replacement = `ChestRarity.COMMON -> when {
                rand <= 10 -> ChestRarity.EPIC   // %10
                rand <= 40 -> ChestRarity.RARE   // %30
                else -> ChestRarity.COMMON       // %60
            }
            ChestRarity.RARE -> when {
                rand <= 5 -> ChestRarity.LEGENDARY // %5
                rand <= 40 -> ChestRarity.EPIC      // %35
                else -> ChestRarity.RARE            // %60
            }
            ChestRarity.EPIC -> when {
                rand <= 20 -> ChestRarity.LEGENDARY // %20
                else -> ChestRarity.EPIC            // %80
            }
            `;

content = content.replace(regex, replacement);
fs.writeFileSync(filepath, content, 'utf-8');
