const fs = require('fs');
const file = 'app/src/main/res/layout/layout_tab1_bead_options.xml';
let content = fs.readFileSync(file, 'utf8');

// Find all rows (LinearLayouts with orientation="horizontal")
const rowRegex = /<LinearLayout[^>]*?android:orientation="horizontal"[\s\S]*?<\/LinearLayout>/g;
let match;
let rows = [];
while ((match = rowRegex.exec(content)) !== null) {
    let rawRow = match[0];
    let cardsInRow = rawRow.split('<com.google.android.material.card.MaterialCardView').length - 1;
    if (cardsInRow > 0) {
        rows.push(cardsInRow);
    }
}
console.log('Cards per row:', rows);
