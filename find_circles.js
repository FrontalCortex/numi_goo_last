const fs = require('fs');
const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');

const pathRegex = /<path[\s\S]*?\/>/g;
const paths = xml.match(pathRegex) || [];

console.log('Finding circles...');
paths.forEach((p, idx) => {
    // Icons in the original image are inside circular backgrounds.
    // Circle paths typically have 4 cubic beziers (c or C).
    const pdMatch = p.match(/pathData="([^"]+)"/);
    if (!pdMatch) return;
    const pathData = pdMatch[1];
    
    // Check if it looks like a circle
    if ((pathData.includes('c0,2') || pathData.includes('c0,3') || pathData.includes('c0,4')) && pathData.split(/[cC]/).length === 5) {
        const mMatch = pathData.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
        if (mMatch) {
            console.log(`Circle at index ${idx}: X=${mMatch[1]}, Y=${mMatch[2]}`);
        }
    }
});
