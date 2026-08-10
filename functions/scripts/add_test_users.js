const admin = require('firebase-admin');

// Initialize the app with application default credentials
admin.initializeApp();

const db = admin.firestore();

async function createTestUsers() {
    console.log("Starting to create 100 test users...");
    
    // We can use a batch to write them faster. A batch can hold up to 500 operations.
    const batch = db.batch();
    
    for (let i = 1; i <= 100; i++) {
        // Create a fake UID
        const fakeUid = `test_uid_kreyzmen_${i}`;
        
        // Generate a random 6-character hex string for the userId, plus AB or something similar
        const randomHex = Math.floor(Math.random() * 16777215).toString(16).toUpperCase().padStart(6, '0');
        const userId = `${randomHex}AB`;
        
        const userRef = db.collection('users').doc(fakeUid);
        
        const userData = {
            name: "KreyzMen",
            userId: userId,
            followersCount: 0,
            followingCount: 0,
            selectedAvatar: Math.floor(Math.random() * 12) + 1, // random avatar 1-12
            email: `kreyzmen${i}@test.com`,
            role: "STUDENT",
            totalTimeSpent: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        };
        
        batch.set(userRef, userData);
    }
    
    try {
        await batch.commit();
        console.log("Successfully added 100 test users!");
    } catch (error) {
        console.error("Error adding test users:", error);
    }
}

createTestUsers();
