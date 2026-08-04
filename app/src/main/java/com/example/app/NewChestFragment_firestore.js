const fs = require('fs');

const filepath = "C:/Users/ASUS/AndroidStudioProjects/numi_goo_last/app/src/main/java/com/example/app/NewChestFragment.kt";
let content = fs.readFileSync(filepath, 'utf-8');

const regex = /private fun showReward\(\) \{[\s\S]*?(?=binding\.chestRewardText\.text = textValue)/;

const replacement = `private fun showReward() {
        var textValue = ""
        var coinDrawable = 0
        var textDrawable = 0
        var earnedGold = 0
        var earnedKey = 0

        val rand = (1..100).random()
        when (currentRarity) {
            ChestRarity.COMMON -> {
                val amount = (50..100).random()
                earnedGold = amount
                textValue = "$amount altın"
                coinDrawable = R.drawable.shop_coin_ic1
                textDrawable = R.drawable.gold_ic
            }
            ChestRarity.RARE -> {
                if (rand <= 50) {
                    val amount = (150..200).random()
                    earnedGold = amount
                    textValue = "$amount altın"
                    coinDrawable = R.drawable.shop_coin_ic1
                    textDrawable = R.drawable.gold_ic
                } else {
                    earnedKey = 1
                    textValue = "1"
                    coinDrawable = R.drawable.key
                    textDrawable = R.drawable.key
                }
            }
            ChestRarity.EPIC -> {
                if (rand <= 50) {
                    val amount = (300..400).random()
                    earnedGold = amount
                    textValue = "$amount altın"
                    coinDrawable = R.drawable.shop_coin_ic2
                    textDrawable = R.drawable.gold_ic
                } else {
                    earnedKey = 2
                    textValue = "2"
                    coinDrawable = R.drawable.key
                    textDrawable = R.drawable.key
                }
            }
            ChestRarity.LEGENDARY -> {
                if (rand <= 50) {
                    val amount = (1000..1500).random()
                    earnedGold = amount
                    textValue = "$amount altın"
                    coinDrawable = R.drawable.shop_coin_ic3
                    textDrawable = R.drawable.gold_ic
                } else {
                    earnedKey = 3
                    textValue = "3"
                    coinDrawable = R.drawable.key
                    textDrawable = R.drawable.key
                }
            }
        }
        
        val ctx = context
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (ctx != null && uid != null) {
            if (earnedGold > 0) {
                UserWalletFirestore.applyCurrencyDelta(ctx, uid, earnedGold)
            }
            if (earnedKey > 0) {
                UserWalletFirestore.applyKeyDelta(ctx, uid, earnedKey)
            }
        }

        `;

content = content.replace(regex, replacement);
fs.writeFileSync(filepath, content, 'utf-8');
