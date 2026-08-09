const fs = require('fs');

function printBgPaths(file, label) {
    const xml = fs.readFileSync(file, 'utf8');
    const paths = xml.match(/<path[\s\S]*?\/>/g) || [];
    const bgIdx = [];
    paths.forEach((p, i) => {
        if (p.includes('a60.') || p.includes('a59.') || p.includes('a61.') || p.includes('c0,44.') || p.includes('c0,45.')) bgIdx.push(i);
    });
    bgIdx.forEach((idx, i) => {
        const pd = paths[idx].match(/pathData="([^"]+)"/)[1];
        console.log(label + ' Avatar' + (i+1) + '['+idx+']: ' + pd.substring(0, 150));
    });
}

printBgPaths('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic3.xml', 'ic3');
printBgPaths('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic4.xml', 'ic4');
