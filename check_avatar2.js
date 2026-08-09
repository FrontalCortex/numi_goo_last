const fs = require('fs');
const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic2.xml', 'utf8');
const paths = xml.match(/<path[\s\S]*?\/>/g) || [];

// Avatar 2 was assigned paths based on distance to (235, 490)
paths.forEach((p, idx) => {
    if (idx === 0) return;
    const mMatch = p.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    if (!mMatch) return;
    let sx = parseFloat(mMatch[1]);
    let sy = parseFloat(mMatch[2]);
    let dist = Math.sqrt(Math.pow(sx - 235, 2) + Math.pow(sy - 490, 2));
    if (dist < 100) {
        // It's Avatar 2. Let's look for paths that look like a head.
        // A head might be drawn with a fillColor that looks like skin, e.g. #D1A881 or similar.
        const fillMatch = p.match(/fillColor="([^"]+)"/);
        const color = fillMatch ? fillMatch[1] : 'none';
        
        const pdMatch = p.match(/pathData="([^"]+)"/)[1];
        console.log(`Color: ${color}, path starts at ${sx}, ${sy}`);
        if (color === '#D1A881' || color === '#E6B58F' || color === '#F0C7A6') {
            console.log(`SKIN PATH: ${pdMatch.substring(0, 150)}`);
        }
    }
});
