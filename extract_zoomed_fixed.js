const fs = require('fs');

const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');
const paths = xml.match(/<path[\s\S]*?\/>/g) || [];

const avatars = [];
const xCenters = [105, 235, 370, 505, 645, 780];
const yCenters = [490, 780];

yCenters.forEach((yc, row) => {
    xCenters.forEach((xc, col) => {
        avatars.push({
            id: row * 6 + col + 1,
            cx: xc,
            cy: yc,
            paths: []
        });
    });
});

paths.forEach((p, idx) => {
    if (idx === 0) return;
    if (p.includes('269.2v290.9')) return;
    
    const mMatch = p.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (!mMatch) return;
    let sx = parseFloat(mMatch[1]);
    let sy = parseFloat(mMatch[2]);
    if (sy < 310) return;
    
    let closestAvatar = null;
    let minDist = Infinity;
    avatars.forEach(a => {
        let dx = a.cx - sx;
        let dy = a.cy - sy;
        let dist = Math.sqrt(dx*dx + dy*dy);
        if (dist < minDist) {
            minDist = dist;
            closestAvatar = a;
        }
    });
    if (closestAvatar) {
        closestAvatar.paths.push(p);
    }
});

avatars.forEach(a => {
    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    a.paths.forEach(p => {
        const pdMatch = p.match(/pathData="([^"]+)"/);
        if (pdMatch) {
            const pathData = pdMatch[1];
            const absCoords = pathData.match(/-?\d+(?:\.\d+)?/g);
            if (absCoords) {
                let mMatch = pathData.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
                if (mMatch) {
                    let cx = parseFloat(mMatch[1]);
                    let cy = parseFloat(mMatch[2]);
                    if (cx < minX) minX = cx;
                    if (cx > maxX) maxX = cx;
                    if (cy < minY) minY = cy;
                    if (cy > maxY) maxY = cy;
                }
            }
        }
    });
    
    a.bbox = { minX, maxX, minY, maxY };
});

avatars.forEach(a => {
    let width = a.bbox.maxX - a.bbox.minX;
    
    // Instead of raw bounding box center which can be skewed by asymmetric paths,
    // we use a combination of the theoretical grid center and the bounding box.
    // Actually, just the theoretical grid center X is the most geometrically stable!
    // The avatars are aligned to xCenters.
    let centerX = a.cx;
    
    // For Y, we want to zoom in on the face. The face is always at the top of the avatar (minY).
    // The face usually takes up the top ~60 units of the avatar.
    // Let's center the Y on minY + 45 (approx center of the face).
    let centerY = a.bbox.minY + 45;
    
    // SCALE FIX:
    // The previous scale of 2.5 meant the 200px viewport only covered 80 units of the SVG.
    // Since the avatars are ~84 units wide, the edges were cut off, making them look huge and off-center.
    // We change the scale to 1.8 so the 200px viewport covers 111 units, giving ample padding for the shoulders
    // and matching the aesthetic of the top 4 portraits perfectly.
    let scale = 1.8; 
    
    let tx = (100 - centerX * scale).toFixed(2);
    let ty = (100 - centerY * scale).toFixed(2);
    
    let out = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="200"
    android:viewportHeight="200">
    <group
        android:scaleX="${scale}"
        android:scaleY="${scale}"
        android:translateX="${tx}"
        android:translateY="${ty}">
        ${a.paths.join('\n        ')}
    </group>
</vector>`;

    fs.writeFileSync(`C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatar_custom_ic${a.id}.xml`, out, 'utf8');
});
console.log('Done generating correctly scaled avatars.');
