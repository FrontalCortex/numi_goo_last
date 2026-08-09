const fs = require('fs');
const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');

const pathRegex = /<path[\s\S]*?\/>/g;
const paths = xml.match(pathRegex) || [];

paths.forEach((p, idx) => {
    const pdMatch = p.match(/pathData="([^"]+)"/);
    if (!pdMatch) return;
    
    const mMatch = pdMatch[1].match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (!mMatch) return;
    
    let sx = parseFloat(mMatch[1]);
    let sy = parseFloat(mMatch[2]);
    
    if (sx >= 810 && sx <= 860 && sy > 300) {
        console.log(`Path ${idx} starts at X=${sx}, Y=${sy}`);
    }
});
