const fs = require('fs');

const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');

const pathRegex = /<path[\s\S]*?\/>/g;
const paths = xml.match(pathRegex) || [];

let removedCount = 0;
let keepPaths = [];
let removedPaths = [];

paths.forEach((p, idx) => {
    if (idx === 0) {
        keepPaths.push(p);
        return;
    }
    
    const pdMatch = p.match(/pathData="([^"]+)"/);
    if (!pdMatch) {
        keepPaths.push(p);
        return;
    }
    
    const mMatch = pdMatch[1].match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (!mMatch) {
        keepPaths.push(p);
        return;
    }
    
    let sx = parseFloat(mMatch[1]);
    let sy = parseFloat(mMatch[2]);
    
    // Right icons: X > 840, Y > 300
    let isRightIcon = sx > 840 && sy > 300;
    
    // Bottom icons: Y > 1000
    let isBottomIcon = sy > 1000;
    
    // Also, there are icons at the bottom right. Those will be caught by both or either.
    if (isRightIcon || isBottomIcon) {
        removedPaths.push(p);
        removedCount++;
    } else {
        keepPaths.push(p);
    }
});

console.log('Removed', removedCount, 'paths.');

const head = xml.substring(0, xml.indexOf('<path'));
const tail = xml.substring(xml.lastIndexOf('/>') + 2);
const finalXml = head + paths.filter(p => !removedPaths.includes(p)).join('\n  ') + tail;

fs.writeFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', finalXml, 'utf8');
console.log('Done');
