/** Tüm sezonlar için seed — ALL_SEASONS=1 ayarlar ve ana scripti çalıştırır. */
process.env.ALL_SEASONS = '1';
require('./seed-lesson-leaderboard.js');
