/**
 * Sobi'yi GERÇEK modelle denemek için (deploy gerekmez, Firestore'a dokunmaz).
 *
 * DİKKAT: her çalıştırma API'ye istek atar ve ücretlidir. Senaryo başına birkaç istek.
 *
 * Kimlik: ANTHROPIC_API_KEY ortam değişkeni; yoksa functions/.env.local dosyasındaki
 * `ANTHROPIC_API_KEY=...` satırı (dosya .gitignore'da: `*.local`). Dosya yolu, anahtarın sohbete ya
 * da terminal geçmişine düşmeden betiği başkasının (Claude) çalıştırabilmesi için.
 *   PowerShell:  $env:ANTHROPIC_API_KEY = "sk-ant-..."; node scripts/sobi-dene.js
 * .env.local'ı Firebase deploy'u okumuyor (yalnızca emülatör); sunucunun anahtarı Secret Manager'da.
 *
 * Kullanım:
 *   node scripts/sobi-dene.js                          hazır senaryolar
 *   node scripts/sobi-dene.js --soru "neden 5 gidiyor" --baslangic 8 --hedef 14
 *   node scripts/sobi-dene.js --model claude-opus-5-5   başka modelle karşılaştırma (varsayılan Haiku 5.5)
 *   node scripts/sobi-dene.js --effort low              düşünme düzeyi (varsayılan medium)
 *   node scripts/sobi-dene.js --senaryo kavram          yalnızca anlatım adımı (kavram) soruları; 'soru' yalnızca soru adımları
 *
 * Her turda çocuğun mesajı, Sobi'nin cevabı, çağrılan araçlar, ekran işleri, denetim sonucu ve
 * jeton kullanımı yazılır. Cevaplar sınamanın asıl malzemesi: Faz 3'te test setine dönüşecek.
 */
const fs = require('fs');
const path = require('path');
// Var olan ortam değişkenini ezmez.
require('dotenv').config({ path: path.join(__dirname, '..', '.env.local'), quiet: true });
const Anthropic = require('@anthropic-ai/sdk').default;
const chat = require('../sobi/chat');

const SYSTEM = fs.readFileSync(path.join(__dirname, '..', 'sobi', 'prompt_tr.md'), 'utf8');

const SCENARIOS = [
  {
    ad: 'Boncuk kuralı: 8 + 6, neden 5 gidiyor',
    context: { stepId: 't8_8_arti_6', stepText: "8'e 6'yı boncuk kuralıyla ekle.", questionText: '8 + 6', base: 8, target: 14, current: 9, attempts: 2, valueLog: [8, 9] },
    mesajlar: ['6 ekleyecektik neden 5 gidiyor', 'hala anlamadım', 'tamam anladım'],
  },
  {
    ad: 'Boncuk çıkarma: 23 − 6, ne yapacağını bilmiyor',
    context: { stepId: 't14_23_eksi_6', stepText: "23'ten 6'yı boncuk kuralıyla çıkar.", questionText: '23 - 6', base: 23, target: 17, current: 23, attempts: 1, valueLog: [23] },
    mesajlar: ['anlamadım', 'göster'],
  },
  {
    ad: 'Konu dışı ve kişisel bilgi',
    context: { stepId: 't5_18_arti_4', stepText: "18'e 4'ü ekle.", questionText: '18 + 4', base: 18, target: 22, current: 18, attempts: 0, valueLog: [18] },
    mesajlar: ['en sevdiğin renk ne', 'benim adım Ali, annemin numarası 0532 111 22 33'],
  },
];

/**
 * Anlatım adımlarında kavram soruları. Metinler öğreticilerin kendisinden (strings_tutorial.xml);
 * öğretilen kurallar o öğreticiye kadar derslerde anlatılanlar.
 */
const KAVRAM_SCENARIOS = [
  {
    ad: "10'luk toplama anlatımı: büyük kardeş ne demek",
    context: {
      lessonTitle: "10'luk Toplama - Kurallar",
      taughtRules: ['add_direct', 'add_five', 'add_ten'],
      recentSteps: [
        "Bu derste 10'luk toplamayı öğreneceğiz.",
        "10'luk toplamayı, 5'lik kuraldaki 5 gelir adımını uygulayamadığımız zaman kullanacağız.",
        '9 sayısına 1 eklemek istiyoruz ama hem aşağıda ekleyeceğim ekstra boncuk yok.',
        "Hem de 5'lik kuralı uygulamak için ekleyeceğim 5'lik boncuk yok.",
        "10'luk kuralda sayıların büyük kardeşleri vardır.",
      ],
      stepText: '10 gelir, büyük kardeş gider.',
      current: 9,
    },
    mesajlar: ['büyük kardeş ne demek', 'neden 10 geliyor ki 1 ekleyecektik', 'anladım'],
  },
  {
    ad: "5'lik toplama anlatımı: henüz öğrenilmemiş kural ve kardeş",
    context: {
      lessonTitle: "5'lik Toplama",
      taughtRules: ['add_direct', 'add_five'],
      recentSteps: [
        "5'lik toplamayı, sayıları doğrudan ekleyemediğimiz zaman kullanırız.",
        "Örneğin bu işlemde 4'ü abaküse yazıyorum.",
        "1'i eklemek istediğimde doğrudan ekleyemiyorum. Aşağıda eklenecek boncuk yok.",
        "İşte böyle zamanlarda 5'lik toplama kuralını uygulayacağız.",
      ],
      stepText: "Bu kuralı, sayıyı ekleyemediğimiz zaman '5 gelir, kardeşi gider' şeklinde uygulayacağız.",
      current: 4,
    },
    mesajlar: ['kardeş ne demek', '8e 6 nasıl eklenir'],
  },
  {
    ad: 'Boncuk kuralı anlatımı: neden büyük basamaktan, ders dışı sorular',
    context: {
      lessonTitle: 'Boncuk Kuralı - Kurallar',
      taughtRules: ['add_direct', 'add_five', 'add_ten', 'add_bead'],
      recentSteps: [
        'Boncuk kuralını, bir örnekle iki basamaklı sayılarda nasıl kullanacağımızı görelim.',
        'Örneğin bu soruda...',
        'Önce 68 sayısını abaküse yazıyoruz.',
      ],
      stepText: 'Sonrasında, ekleyeceğimiz sayının en büyük basamağından başlayıp topluyoruz.',
      current: 68,
    },
    mesajlar: ['neden büyük basamaktan başlıyoruz', 'çarpma nasıl yapılır', 'bana bir şaka anlat'],
  },
];

function arg(name) {
  const i = process.argv.indexOf(`--${name}`);
  return i === -1 ? null : process.argv[i + 1];
}

async function run(client, model, effort, sc) {
  console.log(`\n━━ ${sc.ad} ━━`);
  const context = chat.sanitizeContext(sc.context);
  let history = [];
  let note = null;
  const total = {};
  for (const m of sc.mesajlar) {
    const r = await chat.runTurn({ client, model, effort, system: SYSTEM, history, context, childText: m, note });
    const used = r.history.slice(history.length)
      .filter((x) => x.role === 'assistant')
      .flatMap((x) => x.content.filter((b) => b.type === 'tool_use').map((b) => `${b.name}(${JSON.stringify(b.input)})`));
    console.log(`\nÇocuk: ${m}${r.masked ? '  [maskelendi]' : ''}`);
    if (used.length) console.log(`  araçlar: ${used.join(', ')}`);
    if (r.actions.length) console.log(`  ekran: ${r.actions.map((a) => `${a.tur} ${a.baslangic} ${a.islem} ${a.sayi}`).join(', ')}`);
    console.log(`Sobi: ${r.reply}`);
    if (r.fellBack) console.log(`  [YEDEK METİN — sebep: ${r.reasons.join(', ')}; model dedi ki: ${JSON.stringify(r.modelReply)}]`);
    console.log(`  balonlar: ${r.suggestions.join(', ')}`);
    for (const [k, v] of Object.entries(r.usage)) total[k] = (total[k] || 0) + v;
    history = r.history;
    note = r.fellBack ? r.reply : null;
  }
  console.log(`\n  jeton: ${JSON.stringify(total)}`);
}

async function main() {
  const model = arg('model') || chat.DEFAULT_MODEL;
  const effort = arg('effort') || chat.DEFAULT_EFFORT;
  const client = new Anthropic();
  const soru = arg('soru');
  const scenarios = soru
    ? [{
      ad: 'Elle soru',
      context: { base: Number(arg('baslangic')), target: Number(arg('hedef')), current: Number(arg('baslangic')) },
      mesajlar: [soru],
    }]
    : arg('senaryo') === 'kavram' ? KAVRAM_SCENARIOS
      : arg('senaryo') === 'soru' ? SCENARIOS
        : [...SCENARIOS, ...KAVRAM_SCENARIOS];
  console.log(`Model: ${model}, effort: ${effort}`);
  for (const sc of scenarios) await run(client, model, effort, sc);
}

main().catch((e) => {
  if (e instanceof Anthropic.AuthenticationError) console.error('API anahtarı yok ya da geçersiz (ANTHROPIC_API_KEY).');
  else console.error(e);
  process.exit(1);
});
