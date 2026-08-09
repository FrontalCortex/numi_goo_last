const fs = require('fs');

const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');

const pathRegex = /<path[\s\S]*?\/>/g;
const paths = xml.match(pathRegex) || [];

let pathInfos = [];

paths.forEach((p, idx) => {
    if (idx === 0) return; // Main background
    
    // Top 4 backgrounds
    if (p.includes('269.2v290.9')) return;
    
    const pdMatch = p.match(/pathData="([^"]+)"/);
    if (!pdMatch) return;
    const pathData = pdMatch[1];
    
    // Simple center estimate based on all absolute coordinates in the path
    const coords = pathData.match(/-?\d+(?:\.\d+)?/g);
    if (!coords) return;
    
    let sumX = 0, sumY = 0, count = 0;
    
    // Just finding the first M is usually good enough to locate the path
    const mMatch = pathData.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (mMatch) {
        let sx = parseFloat(mMatch[1]);
        let sy = parseFloat(mMatch[2]);
        pathInfos.push({ idx, p, sx, sy });
    }
});

// Group paths into clusters based on distance
let clusters = [];

pathInfos.forEach(info => {
    // Top row avatars have sy < 300
    if (info.sy < 300) {
        // Belong to top 4 avatars
        return;
    }
    
    let added = false;
    for (let c of clusters) {
        if (Math.abs(c.cx - info.sx) < 60 && Math.abs(c.cy - info.sy) < 150) {
            c.paths.push(info);
            // Update cluster center average
            c.cx = (c.cx * (c.paths.length - 1) + info.sx) / c.paths.length;
            c.cy = (c.cy * (c.paths.length - 1) + info.sy) / c.paths.length;
            added = true;
            break;
        }
    }
    if (!added) {
        clusters.push({ cx: info.sx, cy: info.sy, paths: [info] });
    }
});

console.log('Found', clusters.length, 'clusters for the bottom 12 avatars');

// Sort clusters top-to-bottom, then left-to-right
clusters.sort((a, b) => {
    if (Math.abs(a.cy - b.cy) > 50) return a.cy - b.cy;
    return a.cx - b.cx;
});

clusters.forEach((c, i) => {
    console.log(`Avatar ${i+1}: Center=(${c.cx.toFixed(1)}, ${c.cy.toFixed(1)}), Paths=${c.paths.length}`);
});
