const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

// The code looks like this:
// val rootHeight = binding.newChestRoot.height.toFloat()
// binding.chestOverlay.translationY = -rootHeight
// val overlayTy = android.animation.ObjectAnimator.ofFloat(binding.chestOverlay, "translationY", -rootHeight, 0f)
// val overlayAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestOverlay, "alpha", 0f, 1f)
// hideAnims.add(overlayTy)
// hideAnims.add(overlayAlpha)

// We want to remove the translationY part.
content = content.replace(
    /binding\.chestOverlay\.translationY = -rootHeight\s*val overlayTy = android\.animation\.ObjectAnimator\.ofFloat\(binding\.chestOverlay, "translationY", -rootHeight, 0f\)\s*/g,
    ''
);

content = content.replace(
    /hideAnims\.add\(overlayTy\)\s*/g,
    ''
);

content = content.replace(
    /val rootHeight = binding\.newChestRoot\.height\.toFloat\(\)\s*/g,
    ''
);

fs.writeFileSync(filepath, content, 'utf-8');
