const fs = require('fs');

const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');

const pathRegex = /<path[\s\S]*?\/>/g;
const paths = xml.match(pathRegex) || [];

function getBoundingBox(pathData) {
    const mMatch = pathData.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (!mMatch) return null;
    let startX = parseFloat(mMatch[1]);
    let startY = parseFloat(mMatch[2]);
    return { x: startX, y: startY };
}

let keepPaths = [];
let removedPaths = [];
let minRemovedX = Infinity, maxRemovedX = -Infinity;
let minRemovedY = Infinity, maxRemovedY = -Infinity;

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
    
    const center = getBoundingBox(pdMatch[1]);
    if (!center) {
        keepPaths.push(p);
        return;
    }
    
    let isRightIcon = center.x > 820 && center.y > 300;
    let isBottomIcon = center.y > 960;
    
    if (isRightIcon || isBottomIcon) {
        removedPaths.push(p);
        if (center.x < minRemovedX) minRemovedX = center.x;
        if (center.x > maxRemovedX) maxRemovedX = center.x;
        if (center.y < minRemovedY) minRemovedY = center.y;
        if (center.y > maxRemovedY) maxRemovedY = center.y;
    } else {
        keepPaths.push(p);
    }
});

console.log('Removed paths:', removedPaths.length);
console.log(`Removed X range: [${minRemovedX}, ${maxRemovedX}]`);
console.log(`Removed Y range: [${minRemovedY}, ${maxRemovedY}]`);

const head = xml.substring(0, xml.indexOf('<path'));
const tail = xml.substring(xml.lastIndexOf('/>') + 2);
const finalXml = head + paths.filter(p => !removedPaths.includes(p)).join('\n  ') + tail;

fs.writeFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', finalXml, 'utf8');
console.log('Overwritten avatars_ic2.xml');
