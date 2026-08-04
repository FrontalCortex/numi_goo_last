const fs = require('fs');
const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/MainActivity.kt";
let content = fs.readFileSync(filepath, 'utf-8');

content = content.replace("is AbacusPracticeFragment, is BlindingLessonFragment, is FeedbackFragment -> overlay", "is AbacusPracticeFragment, is BlindingLessonFragment, is FeedbackFragment, is NewChestFragment -> overlay");

fs.writeFileSync(filepath, content, 'utf-8');
