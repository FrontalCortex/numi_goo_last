const https = require('https');

async function createUser(i) {
    const fakeUid = `test_uid_kreyzmen_${i}`;
    const randomHex = Math.floor(Math.random() * 16777215).toString(16).toUpperCase().padStart(6, '0');
    const userId = `${randomHex}AB`;
    const selectedAvatar = Math.floor(Math.random() * 12) + 1;
    
    const docData = {
        fields: {
            name: { stringValue: "KreyzMen" },
            userId: { stringValue: userId },
            followersCount: { integerValue: "0" },
            followingCount: { integerValue: "0" },
            selectedAvatar: { integerValue: selectedAvatar.toString() },
            email: { stringValue: `kreyzmen${i}@test.com` },
            role: { stringValue: "STUDENT" },
            totalTimeSpent: { integerValue: "0" }
        }
    };

    const postData = JSON.stringify(docData);
    
    const options = {
        hostname: 'firestore.googleapis.com',
        port: 443,
        path: `/v1/projects/numigo-new/databases/(default)/documents/users/${fakeUid}?currentDocument.exists=false`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData)
        }
    };
    
    // Actually we should use PATCH or createDocument
    // createDocument endpoint: POST /v1/projects/{projectId}/databases/{databaseId}/documents/{collectionId}?documentId={documentId}
    options.path = `/v1/projects/numigo-new/databases/(default)/documents/users?documentId=${fakeUid}`;

    return new Promise((resolve, reject) => {
        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        });
        
        req.on('error', (e) => reject(e));
        req.write(postData);
        req.end();
    });
}

async function run() {
    console.log("Creating 100 users...");
    const promises = [];
    for (let i = 1; i <= 100; i++) {
        promises.push(createUser(i));
        // small delay to not overwhelm
        await new Promise(r => setTimeout(r, 20));
    }
    await Promise.all(promises);
    console.log("Done adding 100 users!");
}

run();
