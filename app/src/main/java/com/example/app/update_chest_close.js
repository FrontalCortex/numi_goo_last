const fs = require('fs');

const chestPath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let chestContent = fs.readFileSync(chestPath, 'utf-8');

const regexClose = /private fun closeFragment\(\) \{[\s\S]*?val main = activity as\? MainActivity/;
const replacementClose = `private fun closeFragment() {
        parentFragmentManager.setFragmentResult("chest_closed", android.os.Bundle())
        val main = activity as? MainActivity`;

chestContent = chestContent.replace(regexClose, replacementClose);
fs.writeFileSync(chestPath, chestContent, 'utf-8');
