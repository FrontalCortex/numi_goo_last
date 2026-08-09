const fs = require('fs');
const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');
const paths = xml.match(/<path[\s\S]*?\/>/g) || [];

let minX = 1000, maxX = 0;
// Top avatar 1 paths are roughly from index 5 to 15 (background is 1-4)
for(let i=5; i<15; i++) {
    const pd = paths[i].match(/pathData="([^"]+)"/)[1];
    const nums = pd.match(/-?\d+(?:\.\d+)?/g);
    if(nums) {
        // Just checking all absolute numbers to get a rough bound
        for(let j=0; j<nums.length; j+=2) {
            let x = parseFloat(nums[j]);
            // If x is an absolute coordinate in the first avatar's range
            if (x > 0 && x < 269) {
                if(x < minX) minX = x;
                if(x > maxX) maxX = x;
            }
        }
    }
}
console.log('Top avatar 1 X bounds:', minX, maxX, 'Width:', maxX - minX);
