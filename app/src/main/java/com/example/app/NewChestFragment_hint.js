const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

// 1. Add hintHandler and hintRunnable
const classStartIdx = content.indexOf('class NewChestFragment : Fragment() {');
const addVars = `
    private val hintHandler = Handler(Looper.getMainLooper())
    private val hintRunnable = Runnable {
        if (isAdded && _binding != null) {
            binding.chestHintText.visibility = View.VISIBLE
            binding.chestHintText.alpha = 0f
            binding.chestHintText.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun resetHintTimer() {
        hintHandler.removeCallbacks(hintRunnable)
        if (_binding != null) {
            binding.chestHintText.visibility = View.INVISIBLE
        }
        hintHandler.postDelayed(hintRunnable, 5000)
    }
`;
content = content.replace(/(class NewChestFragment : Fragment\(\) \{[\s\S]*?)(?=    private var _binding)/, `$1${addVars}`);

// 2. Call resetHintTimer() in onViewCreated
content = content.replace(/(startIdleAnimation\(\)\s*)/, `$1\n        binding.chestHintText.visibility = View.INVISIBLE\n        resetHintTimer()\n`);

// 3. Call resetHintTimer() in onScreenTapped
content = content.replace(/(private fun onScreenTapped\(\) \{\s*)/, `$1resetHintTimer()\n        `);

// 4. Also need to cancel the timer in onDestroyView
content = content.replace(/(override fun onDestroyView\(\) \{\s*super\.onDestroyView\(\)\s*)/, `$1hintHandler.removeCallbacks(hintRunnable)\n        `);

fs.writeFileSync(filepath, content, 'utf-8');
