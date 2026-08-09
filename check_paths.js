const fs = require('fs');
const xml = fs.readFileSync('C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/res/drawable/avatars_ic3.xml', 'utf8');
const paths = xml.match(/<path[\s\S]*?\/>/g) || [];
[0, 28, 50].forEach(idx => {
    const pd = paths[idx].match(/pathData="([^"]+)"/)[1];
    console.log('idx'+idx+':', pd);
});
