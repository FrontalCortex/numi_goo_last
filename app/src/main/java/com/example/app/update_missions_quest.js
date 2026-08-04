const fs = require('fs');

const missionsPath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/MissionsFragment.kt";
let content = fs.readFileSync(missionsPath, 'utf-8');

const regex = /private fun onQuestClicked\([\s\S]*?(?=\n    private fun updateMissionsUI)/;

const replacement = `private fun onQuestClicked(quest: MissionQuestData) {
        val ctx = context ?: return
        val done = quest.progress >= quest.target
        if (!done || quest.isClaimed) return
        if (isVideoFlowOpen || !isAdded) return
        
        isVideoFlowOpen = true
        
        parentFragmentManager.setFragmentResultListener("chest_closed", viewLifecycleOwner) { _, _ ->
            MissionsProgressStore.markMissionRewardClaimed(requireContext(), quest.window, quest.missionId)
            isVideoFlowOpen = false
            updateMissionsUI()
            parentFragmentManager.clearFragmentResultListener("chest_closed")
        }

        val main = activity as? MainActivity
        if (main != null) {
            main.showAbacusOverlayFragment(NewChestFragment()) {}
        } else {
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainerID, NewChestFragment())
                .addToBackStack(null)
                .commit()
        }
    }
`;

content = content.replace(regex, replacement);
fs.writeFileSync(missionsPath, content, 'utf-8');
