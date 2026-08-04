const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

const oldFuncRegex = /private fun startIdleAnimation\(\) \{[\s\S]*?(?=private fun stopIdleAnimation\(\))/;

const newFunc = `private fun startIdleAnimation() {
        stopIdleAnimation()
        if (tapCount >= 4) return
        
        val durationMs: Long
        val transY: PropertyValuesHolder

        when (currentRarity) {
            ChestRarity.COMMON -> {
                durationMs = 2000L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)   // Bekleme
                val ty2 = Keyframe.ofFloat(0.30f, -35f) // Yukarı zıplama
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   // Yere düşüş
                val ty4 = Keyframe.ofFloat(0.38f, -10f) // Küçük sekme
                val ty5 = Keyframe.ofFloat(0.41f, 0f)   // Yere oturuş
                val ty6 = Keyframe.ofFloat(1f, 0f)      // Bekleme
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
            }
            ChestRarity.RARE -> {
                durationMs = 1500L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)   
                val ty2 = Keyframe.ofFloat(0.30f, -35f) 
                val ty3 = Keyframe.ofFloat(0.35f, 0f)   
                val ty4 = Keyframe.ofFloat(0.38f, -10f) 
                val ty5 = Keyframe.ofFloat(0.41f, 0f)   
                val ty6 = Keyframe.ofFloat(1f, 0f)      
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, ty3, ty4, ty5, ty6)
            }
            ChestRarity.EPIC -> {
                durationMs = 1000L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.29f, -70f) // Zıplama tepe (2x)
                val tyVib1 = Keyframe.ofFloat(0.31f, -62f) // Titreşim aşağı
                val tyVib2 = Keyframe.ofFloat(0.33f, -70f) // Titreşim yukarı
                val ty3 = Keyframe.ofFloat(0.36f, 0f)   // Yere düşüş
                val ty4 = Keyframe.ofFloat(0.39f, -20f) // Küçük sekme
                val ty5 = Keyframe.ofFloat(0.42f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, tyVib1, tyVib2, ty3, ty4, ty5, ty6)
            }
            ChestRarity.LEGENDARY -> {
                durationMs = 1000L
                val ty0 = Keyframe.ofFloat(0f, 0f)
                val ty1 = Keyframe.ofFloat(0.25f, 0f)
                val ty2 = Keyframe.ofFloat(0.28f, -105f) // Zıplama tepe (3x)
                val tyVib1 = Keyframe.ofFloat(0.295f, -90f) // Daha fazla titreşim
                val tyVib2 = Keyframe.ofFloat(0.31f, -110f)
                val tyVib3 = Keyframe.ofFloat(0.325f, -90f)
                val tyVib4 = Keyframe.ofFloat(0.34f, -105f)
                val ty3 = Keyframe.ofFloat(0.37f, 0f)   // Yere düşüş
                val ty4 = Keyframe.ofFloat(0.40f, -30f) // Küçük sekme
                val ty5 = Keyframe.ofFloat(0.43f, 0f)
                val ty6 = Keyframe.ofFloat(1f, 0f)
                transY = PropertyValuesHolder.ofKeyframe("translationY", ty0, ty1, ty2, tyVib1, tyVib2, tyVib3, tyVib4, ty3, ty4, ty5, ty6)
            }
        }

        idleAnim = ObjectAnimator.ofPropertyValuesHolder(binding.chestImage, transY).apply {
            duration = durationMs
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    `;

const newContent = content.replace(oldFuncRegex, newFunc);
fs.writeFileSync(filepath, newContent, 'utf-8');
