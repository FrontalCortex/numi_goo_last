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

// Let's log the first 2 paths for each avatar
avatars.forEach(a => {
    console.log(`\nAvatar ${a.id}:`);
    for (let i = 0; i < Math.min(2, a.paths.length); i++) {
        let p = a.paths[i];
        let pd = p.match(/pathData="([^"]+)"/)[1];
        console.log(`Path ${i}: ${pd.substring(0, 80)}...`);
    }
});
