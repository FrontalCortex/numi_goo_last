const fs = require('fs');
const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');

const pathRegex = /<path[\s\S]*?\/>/g;
const paths = xml.match(pathRegex) || [];

paths.forEach((p, idx) => {
    const pdMatch = p.match(/pathData="([^"]+)"/);
    if (!pdMatch) return;
    
    const pathData = pdMatch[1];
    
    // Find absolute maximum X coordinate for this path
    const coords = pathData.match(/-?\d+(?:\.\d+)?/g);
    if (!coords) return;
    
    let maxX = -1000;
    // Assuming relative or absolute, this is rough.
    // Better to just find starting point M
    const mMatch = pathData.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (!mMatch) return;
    
    let sx = parseFloat(mMatch[1]);
    let sy = parseFloat(mMatch[2]);
    
    // Check if this path was deleted by my filter!
    let isRightIcon = sx > 820 && sy > 300;
    let isBottomIcon = sy > 1000;
    
    if (isRightIcon || isBottomIcon) {
        // Was it actually an avatar part?
        // Icons are strictly inside circles.
        // Circles have `c0,22.` or similar.
        // Let's log all deleted paths that don't look like icon circles or typical icon internals.
        // Actually, let's just log ALL deleted paths that have sx < 880 (since icons start at 882.5!)
        if (sx < 880) {
            console.log(`DELETED PATH ${idx}: sx=${sx}, sy=${sy}`);
        }
    }
});
