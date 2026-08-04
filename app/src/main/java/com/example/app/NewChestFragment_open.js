const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

// Add canClose variable
if (!content.includes('private var canClose = false')) {
    content = content.replace(/(private var currentRarity)/, `private var canClose = false\n    $1`);
}

// Update onScreenTapped to check canClose
content = content.replace(/4 -> \{\s*\/\/\s*Açıldıktan sonra kapat\s*closeFragment\(\)\s*\}/, 
`4 -> {
                // Açıldıktan sonra kapat
                if (canClose) {
                    closeFragment()
                }
            }`);

// Also fix the comments inside onScreenTapped if they use special chars
content = content.replace(/4 -> \{\s*\/\/\s*A\S+ld\S+ktan sonra kapat\s*closeFragment\(\)\s*\}/, 
`4 -> {
                // Açıldıktan sonra kapat
                if (canClose) {
                    closeFragment()
                }
            }`);

// Replace openChest function
const openChestRegex = /private fun openChest\(\) \{[\s\S]*?(?=private fun closeFragment\(\))/;

const newOpenChest = `private fun openChest() {
        hintHandler.removeCallbacks(hintRunnable)
        
        val viewsToHide = listOf(
            binding.chestImage, 
            binding.stepsContainer, 
            binding.chestRarityLabel, 
            binding.chestHintText, 
            binding.chestPlatform, 
            binding.starContainer
        )
        
        val hideAnims = mutableListOf<android.animation.Animator>()
        viewsToHide.forEach { view ->
            val ty = android.animation.ObjectAnimator.ofFloat(view, "translationY", view.translationY, view.translationY + 500f)
            val alpha = android.animation.ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f)
            hideAnims.add(ty)
            hideAnims.add(alpha)
        }

        binding.chestOverlay.visibility = android.view.View.VISIBLE
        binding.chestOverlay.alpha = 0f
        
        binding.chestOverlay.post {
            val rootHeight = binding.newChestRoot.height.toFloat()
            binding.chestOverlay.translationY = -rootHeight
            
            val overlayTy = android.animation.ObjectAnimator.ofFloat(binding.chestOverlay, "translationY", -rootHeight, 0f)
            val overlayAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestOverlay, "alpha", 0f, 1f)
            hideAnims.add(overlayTy)
            hideAnims.add(overlayAlpha)
            
            val phase1 = android.animation.AnimatorSet().apply {
                playTogether(hideAnims)
                duration = 700
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            
            phase1.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    showReward()
                }
            })
            
            phase1.start()
        }
    }

    private fun showReward() {
        binding.chestRewardCoin.visibility = android.view.View.VISIBLE
        binding.chestRewardText.visibility = android.view.View.VISIBLE
        
        val coinScaleX = android.animation.ObjectAnimator.ofFloat(binding.chestRewardCoin, "scaleX", 0f, 1f)
        val coinScaleY = android.animation.ObjectAnimator.ofFloat(binding.chestRewardCoin, "scaleY", 0f, 1f)
        val coinAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestRewardCoin, "alpha", 0f, 1f)
        val textAlpha = android.animation.ObjectAnimator.ofFloat(binding.chestRewardText, "alpha", 0f, 1f)
        
        val rewardAnim = android.animation.AnimatorSet().apply {
            playTogether(coinScaleX, coinScaleY, coinAlpha, textAlpha)
            duration = 500
            interpolator = android.view.animation.OvershootInterpolator(1.5f)
        }
        
        rewardAnim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                canClose = true
            }
        })
        
        rewardAnim.start()
    }

    `;

content = content.replace(openChestRegex, newOpenChest);
fs.writeFileSync(filepath, content, 'utf-8');
