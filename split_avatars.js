const fs = require('fs');

/**
 * ic3 has TWO formats:
 *
 * Format A (avatar 1, 3):
 *   M50.19,232.91 a60.01,60.01 0,1,0 84.86,-84.86 a60.01,60.01 0,1,0 -84.86,84.86 z
 *   Two half-arcs forming a full circle. Start point = a point ON the circle edge.
 *   Arc1 endpoint = (sx+dx1, sy+dy1). The circle center = midpoint of start & arc1-end.
 *   center = (sx + dx1/2, sy + dy1/2)
 *
 * Format B (avatar 2, 4, 5, 6):
 *   M238.14,248.97 a60.01,60.01 57.94,1,0 26.88,-116.97 a60.01,60.01 57.94,1,0 -26.88,116.97 z
 *   Same structure as A, formula is identical.
 *
 *   OR:
 *   M92.62,369.01 m-60.01,0 a60.01,60.01 0,1,1 120.01,0 a60.01,60.01 0,1,1 -120.01,0 z
 *   Here M sets a reference, then m-60.01,0 moves to the left edge.
 *   Center = M coordinate = (92.62, 369.01)
 */
function parseIc3Center(pathData) {
    // Check for Format B with m-offset: has "m-" after the first M
    if (/M[\d., -]+m-/.test(pathData) || /M[\d., -]+m-/.test(pathData)) {
        // M(cx,cy) m(-r,0) a...
        const mMatch = pathData.match(/M\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
        return { cx: parseFloat(mMatch[1]), cy: parseFloat(mMatch[2]) };
    }

    // Format A/B: M(sx,sy) a... dx,dy → center = (sx+dx/2, sy+dy/2)
    const mMatch = pathData.match(/M\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    const sx = parseFloat(mMatch[1]);
    const sy = parseFloat(mMatch[2]);

    // Arc format: a rx,ry xrot laf sf dx dy  OR  a rx,ry xrot,laf sf dx,dy etc.
    // The endpoint (dx,dy) is always the LAST two numbers of the arc params before the next command.
    // Strategy: grab everything after 'a' up to the next letter, then take last two numbers.
    const aSegment = pathData.match(/a([^a-zA-Z]+)/i)[1];
    const allNums = aSegment.trim().match(/-?\d+(?:\.\d+)?/g).map(Number);
    // arc params: rx, ry, x-rotation, large-arc-flag, sweep-flag, dx, dy = 7 values
    const dx = allNums[5];
    const dy = allNums[6];
    return { cx: sx + dx / 2, cy: sy + dy / 2 };
}

/**
 * ic4 format: m(rightX,centerY) c0,kR ... -r,r ...  C ...,rightX,centerY
 *   The "C" (absolute cubic) near the end: "C168.2,171.5 204.02,207.32 204.02,251.51"
 *   The last coordinate of C is the endpoint = (rightX, centerY) again (closes the circle).
 *   Radius r is the x-displacement from right point to center:
 *   m(204.02,251.51) c0,44.19 -35.82,80.01 -80.01,80.01  → r=80.01 (last x of first c)
 *   center = (rightX - r, centerY)
 *
 *   CORRECT extraction: find "-r,r " pattern (the endpoint of the first bezier segment)
 */
function parseIc4Center(pathData) {
    const mMatch = pathData.match(/[Mm]\s*(-?\d+(?:\.\d+)?)[, ]\s*(-?\d+(?:\.\d+)?)/);
    const rightX = parseFloat(mMatch[1]);
    const centerY = parseFloat(mMatch[2]);

    // c0,kR  cp2x,cp2y  -r,+r → the third coordinate pair of the 'c' segment = (-r, +r)
    // Match: c0,\d+ \S+,\S+ (-\d+\.?\d*),(+\d+\.?\d*)
    const cMatch = pathData.match(/c0,[\d.]+\s+[\d.,-]+\s+(-[\d.]+),([\d.]+)/);
    const r = parseFloat(cMatch[1].replace('-', ''));  // r is positive
    return { cx: rightX - r, cy: centerY };
}

function generateFromFile(inputFile, startIndex, parseCenter, scale) {
    const xml = fs.readFileSync(inputFile, 'utf8');
    const paths = xml.match(/<path[\s\S]*?\/>/g) || [];
    const isIc4 = scale !== 1.0;

    const bgIndexes = [];
    paths.forEach((p, i) => {
        if (!isIc4 && (p.includes('a60.') || p.includes('a59.') || p.includes('a61.'))) bgIndexes.push(i);
        if (isIc4  && (p.includes('c0,44.') || p.includes('c0,45.'))) bgIndexes.push(i);
    });

    bgIndexes.forEach((bgIdx, i) => {
        const nextIdx = i < bgIndexes.length - 1 ? bgIndexes[i + 1] : paths.length;
        const avatarPaths = paths.slice(bgIdx, nextIdx);

        const pd = paths[bgIdx].match(/pathData="([^"]+)"/)[1];
        const { cx, cy } = parseCenter(pd);

        // Android VG transform: point_screen = point * scale + translate
        // We want point_screen = 100 for the center.
        const tx = (100 - cx * scale).toFixed(2);
        const ty = (100 - cy * scale).toFixed(2);

        let groupAttrs = `android:translateX="${tx}"\n        android:translateY="${ty}"`;
        if (scale !== 1.0) {
            groupAttrs = `android:scaleX="${scale}"\n        android:scaleY="${scale}"\n        android:translateX="${tx}"\n        android:translateY="${ty}"`;
        }

        const out = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="200"
    android:viewportHeight="200">

    <group
        ${groupAttrs}>
        ${avatarPaths.join('\n        ')}
    </group>
</vector>`;

        const fname = `C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatar_ic${startIndex + i}.xml`;
        fs.writeFileSync(fname, out, 'utf8');
        console.log(`avatar_ic${startIndex+i}: center=(${cx.toFixed(2)},${cy.toFixed(2)}) tx=${tx} ty=${ty}`);
    });
}

generateFromFile(
    'C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic3.xml',
    1, parseIc3Center, 1.0
);
generateFromFile(
    'C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic4.xml',
    7, parseIc4Center, 0.75
);
