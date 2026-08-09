const fs = require('fs');

const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');
const paths = xml.match(/<path[\s\S]*?\/>/g) || [];

const avatars = [];
const xCenters = [105, 235, 370, 505, 645, 780];
// Updated Y centers to better separate the two rows
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
    
    if (sy < 310) return; // Top 4 avatars
    
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
            const parts = pathData.split(/(?=[a-zA-Z])/);
            parts.forEach(part => {
                const cmd = part[0];
                const nums = part.substring(1).match(/-?\d+(?:\.\d+)?/g);
                if (nums && (cmd === 'M' || cmd === 'm' || cmd === 'C' || cmd === 'c' || cmd === 'L' || cmd === 'l')) {
                    // It's a rough bounding box if we mix absolute and relative, 
                    // but usually the first M is absolute and dominates, 
                    // or everything is absolute. 
                    // Let's just use sx, sy for the bounding box calculation instead of true bezier bounds,
                    // it's good enough for centering a face.
                }
            });
            // Better: just use the absolute coordinates found in the path
            const absCoords = pathData.match(/-?\d+(?:\.\d+)?/g);
            if (absCoords) {
                // If it's all absolute, this works well. If it's relative, it might overshoot, but sx, sy are accurate.
                // Since avatars are well-separated, let's just use the 'sx' and 'sy' of each path to find the bounds.
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
    let width = maxX - minX;
    let height = maxY - minY;
    console.log(`Avatar ${a.id} bounds: X[${minX.toFixed(1)}, ${maxX.toFixed(1)}], Y[${minY.toFixed(1)}, ${maxY.toFixed(1)}], W=${width.toFixed(1)}, H=${height.toFixed(1)}, Paths=${a.paths.length}`);
});

avatars.forEach(a => {
    let width = a.bbox.maxX - a.bbox.minX;
    let height = a.bbox.maxY - a.bbox.minY;
    
    let centerX = a.bbox.minX + width / 2;
    // The head is at the top. Let's center on Y = minY + 50
    let centerY = a.bbox.minY + 50;
    
    // Scale it up. 2.5 seems good for a face zoom on these full-body figures.
    let scale = 2.5; 
    
    let tx = (100 - centerX * scale).toFixed(2);
    let ty = (100 - centerY * scale).toFixed(2);
    
    let out = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="200"
    android:viewportHeight="200">
    <!-- CenterX: ${centerX.toFixed(2)}, CenterY: ${centerY.toFixed(2)} -->
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
console.log('Done generating zoomed avatars.');
