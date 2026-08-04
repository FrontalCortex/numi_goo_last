const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

const oldFuncRegex = /private fun startIdleAnimation\(\) \{[\s\S]*?(?=private fun stopIdleAnimation\(\))/;

const newFunc = `private fun startIdleAnimation() {
        stopIdleAnimation()
        if (tapCount >= 4) return
        
        val durationMs: Long
        val transY: PropertyValuesHolder
        val rot: PropertyValuesHolder

        when (currentRarity) {
            ChestRarity.COMMON -> {
                durationMs = 2000L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.30f, -35f)
                val ty3 = Keyframe.ofFloat(0.35f, 0f)
                val ty4 = Keyframe.ofFloat(0.38f, -10f)
                val ty5 = Keyframe.ofFloat(0.41f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1)
            }
            ChestRarity.RARE -> {
                durationMs = 1750L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)   
                val ty2 = Keyframe.ofFloat(0.30f, -35f) 
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   
                val ty4 = Keyframe.ofFloat(0.38f, -10f) 
                val ty5 = Keyframe.ofFloat(0.41f, 0f)   
                val ty6 = Keyframe.ofFloat(1f, 0f)      
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1)
            }
            ChestRarity.EPIC -> {
                durationMs = 2000L // Beklemeli (2 saniye döngü, 320ms zıplama)
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.30f, -70f) // Zıplama tepe
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   
                val ty4 = Keyframe.ofFloat(0.38f, -20f) 
                val ty5 = Keyframe.ofFloat(0.41f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(0.25f, 0f)
                val ry2 = Keyframe.ofFloat(0.28f, 5f)
                val ry3 = Keyframe.ofFloat(0.32f, -5f)
                val ry4 = Keyframe.ofFloat(0.35f, 0f)
                val ry5 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1, ry2, ry3, ry4, ry5)
            }
            ChestRarity.LEGENDARY -> {
                durationMs = 320L // Destansı ile tamamen aynı hız, fakat bekleme süresi yok (sürekli zıplıyor)
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.31f, -70f) // Zıplama tepe
                val ty2 = Keyframe.ofFloat(0.62f, 0f)   // Yere düşüş
                val ty3 = Keyframe.ofFloat(0.81f, -20f) // Küçük sekme
                val ty4 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4)
                
                val ry0 = Keyframe.ofFloat(0f, 0f)
                val ry1 = Keyframe.ofFloat(0.19f, 5f)
                val ry2 = Keyframe.ofFloat(0.44f, -5f)
                val ry3 = Keyframe.ofFloat(0.62f, 0f)
                val ry4 = Keyframe.ofFloat(1f, 0f)
                rot = PropertyValuesHolder.ofKeyframe("rotation", ry0, ry1, ry2, ry3, ry4)
            }
        }

        idleAnim = ObjectAnimator.ofPropertyValuesHolder(binding.chestImage, transY, rot).apply {
            duration = durationMs
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    `;

const newContent = content.replace(oldFuncRegex, newFunc);
fs.writeFileSync(filepath, newContent, 'utf-8');
