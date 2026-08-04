const fs = require('fs');
const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

content = content.replace(/override fun onDestroyView\(\) \{\s*starHandler\.removeCallbacks\(starSpawner\)\s*super\.onDestroyView\(\)\s*\}\s*private fun spawnStar\(\) \{[\s\S]*?\}\s*private fun onScreenTapped/, 'private fun onScreenTapped');

fs.writeFileSync(filepath, content, 'utf-8');
