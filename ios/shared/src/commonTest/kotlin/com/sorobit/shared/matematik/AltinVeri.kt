package com.sorobit.shared.matematik

// OTOMATİK ÜRETİLDİ — elle düzenleme.
// Android kaynak dosyaları (app/.../MathOperationGenerator.kt ve GlobalValues.kt) JVM'de derlenip,
// rastgele çağrıları tohumlu kotlin.random.Random(tohum) kaynağına yönlendirilerek çalıştırıldı;
// aşağıdaki satırlar o ÖZGÜN Android kodunun çıktılarıdır. Biçim: tohum|durum|sonuç.
// `java.util.Random()` örnekleri gerçek java.util.Random(kaynak.nextLong()) ile kuruldu.
// AltinVeriTest, ortak kodun her platformda (JVM + Kotlin/Native) aynı çıktıyı verdiğini doğrular.

private val PARCA_0 = """
1|generateRelatedNumbers0(1,1)|(2 + 7)
1|generateRelatedNumbers0(2,2)|(2 + 7)
1|generateRelatedNumbers(1,1)|(2 + 2)
1|generateRelatedNumbers(2,2)|(23 + 66)
1|generateRelatedNumbers(3,2)|(243 + 55)
1|generateRelatedNumbers(2,3)|(23 + 766)
1|generateRelatedNumbers(4,4)|(2403 + 2016)
1|generateRelatedNumbers(2,1)|(23 + 5)
1|generateRelatedNumbers(1,2)|(2 + 50)
1|generateRelatedNumbersList(5,1,1)|[(2 + 2), (8 + 1), (7 + 1), (5 + 1), (3 + 5)]
1|generateRelatedNumbersList(12,1,1)|[(2 + 2), (8 + 1), (7 + 1), (5 + 1), (3 + 5), (6 + 1), (1 + 7), (4 + 5), (3 + 6), (2 + 2), (5 + 1), (8 + 1)]
1|generateRelatedNumbersList(6,2,2)|[(23 + 66), (78 + 21), (10 + 80), (72 + 26), (12 + 37), (71 + 21)]
1|generateRelatedNumbersList(0,2,2)|[]
1|generateRelatedNumbersList(-1,1,1)|[]
1|generateRelatedNumbersList(4,3,2)|[(243 + 55), (331 + 10), (307 + 11), (628 + 11)]
1|generateRelatedNumbers2(1,1)|(4 + 4)
1|generateRelatedNumbers2(2,2)|(42 + 23)
1|generateRelatedNumbers2(3,3)|(425 + 241)
1|generateRelatedNumbers2(2,3)|(42 + 523)
1|generateRelatedNumbers2(3,2)|(425 + 41)
1|generateRelatedNumbers2Blinding(5,1)|[4, 3, 2, 0, 0]
1|generateRelatedNumbers2Blinding(5,2)|[42, 43, 11, 2, 1]
1|generateRelatedNumbers2Blinding(8,3)|[425, 124, 420, 20, 10, 0, 0, 0]
1|generateRelatedNumbers2Blinding(0,2)|[]
1|generateRelatedNumbers2Blinding(3,0)|[4, 3, 2]
1|generateRelatedNumbers2Blinding(1,4)|[4254]
1|generateRelatedNumbers2Blinding(20,2)|[42, 43, 11, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateRandomMathOperation1()|(45 + 23)
1|generateMathOperation()|(19 + 1)
1|generateMathOperationList(0)|[]
1|generateMathOperationList(1)|[(67 + 5)]
1|generateMathOperationList(5)|[(67 + 5), (18 + 4), (38 + 2), (68 + 3), (69 + 1)]
1|generateMathOperationList(12)|[(67 + 5), (18 + 4), (38 + 2), (68 + 3), (69 + 1), (77 + 5), (39 + 4), (19 + 1), (38 + 3), (79 + 2), (18 + 2), (29 + 4)]
1|generateMathOperationList(-3)|[]
1|generateMathOperation2()|(46 + 4)
1|generateMathOperationWithDigits(1,1)|(5 + 5)
1|generateMathOperationWithDigits(2,2)|(56 + 54)
1|generateMathOperationWithDigits(3,2)|(567 + 54)
1|generateMathOperationWithDigits(3,3)|(567 + 543)
1|generateMathOperationWithDigits(2,3)|EXC:IndexOutOfBounds
1|generateMathOperation3()|(19 + 6)
1|generateMathOperationList3(0)|[]
1|generateMathOperationList3(1)|[(77 + 8)]
1|generateMathOperationList3(4)|[(77 + 8), (59 + 7), (63 + 9), (64 + 6)]
1|generateMathOperationList3(10)|[(77 + 8), (59 + 7), (63 + 9), (64 + 6), (29 + 7), (14 + 8), (29 + 6), (71 + 9), (17 + 8), (86 + 9)]
1|generateMathOperation4()|(44 + 7)
1|generateMathOperationList4(0)|[]
1|generateMathOperationList4(1)|[(97 + 8)]
1|generateMathOperationList4(4)|[(97 + 8), (49 + 7), (93 + 9), (44 + 6)]
1|generateMathOperationList4(10)|[(97 + 8), (49 + 7), (93 + 9), (44 + 6), (99 + 7), (44 + 8), (49 + 6), (91 + 9), (97 + 8), (96 + 9)]
1|generateMathOperationWithDigits2(1,1)|(4 + 8)
1|generateMathOperationWithDigits2(2,2)|(44 + 98)
1|generateMathOperationWithDigits2(3,2)|(445 + 85)
1|generateMathOperationWithDigits2(2,3)|(44 + 989)
1|generateMathOperationWithDigits2(3,3)|(445 + 895)
1|generateMathOperationWithDigits3(2,2)|(44 + 51)
1|generateMathOperationWithDigits3(3,3)|(445 + 173)
1|generateMathOperationWithDigits3(3,1)|(445 + 1)
1|generateMathOperationWithDigits3(1,2)|EXC:IndexOutOfBounds
1|generateMathOperationBeadRule()|(17 + 7)
1|generateMathOperationWithDigitsBeadRule(2,2)|(57 + 98)
1|generateMathOperationWithDigitsBeadRule(3,3)|(578 + 897)
1|generateMathOperationWithDigitsBeadRule(3,2)|(578 + 89)
1|generateMathOperationWithDigitsBeadRule(1,1)|(5 + 8)
1|irregularExtraction(1,1)|(4 - 2)
1|irregularExtraction(2,2)|(44 - 12)
1|irregularExtraction(3,2)|(445 - 21)
1|irregularExtraction(3,3)|(445 - 215)
1|irregularExtractionFiveRules(2,2)|(57 - 13)
1|irregularExtractionFiveRules(3,3)|(578 - 234)
1|irregularExtractionFiveRules(2,1)|(57 - 1)
1|irregularExtractionFiveRules(1,2)|EXC:IndexOutOfBounds
1|irregularExtractionFiveRulesMix(2,2)|(77 - 54)
1|irregularExtractionFiveRulesMix(3,3)|(771 - 411)
1|irregularExtractionFiveRulesMix(3,2)|(771 - 41)
1|irregularExtractionFiveRulesMix(1,1)|(7 - 1)
1|extractionGenerateMathOperationList(0)|[]
1|extractionGenerateMathOperationList(1)|[(70 - 1)]
1|extractionGenerateMathOperationList(9)|[(70 - 1), (71 - 2), (30 - 3), (63 - 4), (20 - 5), (80 - 6), (36 - 7), (86 - 8), (70 - 9)]
1|extractionGenerateMathOperationList(20)|[(70 - 1), (71 - 2), (30 - 3), (63 - 4), (20 - 5), (80 - 6), (36 - 7), (86 - 8), (70 - 9), (60 - 1), (21 - 2), (70 - 3), (90 - 4), (70 - 5), (30 - 6), (20 - 7), (87 - 8), (67 - 9), (90 - 1), (91 - 2)]
"""

private val PARCA_1 = """
1|extractionGenerateMathOperation()|(23 - 5)
1|extractionGenerateMathOperationTen()|(234 - 45)
1|extractionGenerateMathOperationTenWithOtherRules()|(264 - 15)
1|extractionGenerateMathOperationTenWithOtherRulesExtreme()|(564 - 15)
1|extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton()|(464 - 85)
1|extractionBeadRules()|(23 - 7)
1|extractionBeadRulesList(0)|[]
1|extractionBeadRulesList(1)|[(84 - 8)]
1|extractionBeadRulesList(4)|[(84 - 8), (94 - 7), (64 - 9), (21 - 6)]
1|extractionBeadRulesList(10)|[(84 - 8), (94 - 7), (64 - 9), (21 - 6), (72 - 6), (14 - 8), (74 - 9), (64 - 7), (24 - 8), (64 - 6)]
1|extractionBeadRulesThreeTwo()|(434 - 89)
1|extractionBeadRulesFourThree()|(4343 - 898)
1|multiplicationLessFive()|(10 x 5)
1|multiplicationLessFiveFull()|(56 x 7)
1|multiplicationFull()|(46 x 9)
1|multiplicationTwo()|(11 x 33)
1|multiplicationTwoFull()|(46 x 58)
1|multiplicationThreeFull()|(462 x 7)
1|multiplicationThreeTwoFive()|(104 x 43)
1|multiplicationThreeTwoFull()|(462 x 19)
1|generateSequence1(0)|[12]
1|generateSequence1(1)|[12]
1|generateSequence1(6)|[12, 25, 51, 11, 0, 0]
1|generateSequence1(15)|[12, 25, 51, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateSequence1Digits(0,2)|[]
1|generateSequence1Digits(1,1)|[1]
1|generateSequence1Digits(6,1)|[1, 1, 5, 1, 1, 0]
1|generateSequence1Digits(6,2)|[10, 21, 55, 12, 1, 0]
1|generateSequence1Digits(10,3)|[101, 115, 511, 111, 111, 50, 0, 0, 0, 0]
1|generateSequence1Digits(5,0)|[1, 1, 5, 1, 1]
1|generateSequence5RulesNew(0)|[12]
1|generateSequence5RulesNew(1)|[12]
1|generateSequence5RulesNew(6)|[12, 43, 21, 12, 11, 0]
1|generateSequence5RulesNew(15)|[12, 43, 21, 12, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateSequence10RulesEasyNew(0)|[13]
1|generateSequence10RulesEasyNew(1)|[13]
1|generateSequence10RulesEasyNew(6)|[13, 45, 22, 14, 2, 5]
1|generateSequence10RulesEasyNew(15)|[13, 45, 22, 14, 2, 5, 15, 35, 14, 15, 12, 6, 2, 15, 25]
1|generateSequence10RulesEasyOneCount(0)|[44]
1|generateSequence10RulesEasyOneCount(1)|[44]
1|generateSequence10RulesEasyOneCount(6)|[44, 4, 3, 6, 3, 2]
1|generateSequence10RulesEasyOneCount(15)|[44, 4, 3, 6, 3, 2, 4, 5, 4, 5, 2, 6, 5, 4, 4]
1|generateSequence10RulesEasyOldOneCount(0)|[44]
1|generateSequence10RulesEasyOldOneCount(1)|[44]
1|generateSequence10RulesEasyOldOneCount(6)|[44, 7, 9, 4, 6, 2]
1|generateSequence10RulesEasyOldOneCount(15)|[44, 7, 9, 4, 6, 2, 9, 4, 5, 3, 3, 5, 4, 5, 5]
1|generateSequence10RulesEasyOld(0)|[17]
1|generateSequence10RulesEasyOld(1)|[17]
1|generateSequence10RulesEasyOld(6)|[17, 48, 23, 17, 52, 18]
1|generateSequence10RulesEasyOld(15)|[17, 48, 23, 17, 52, 18, 12, 18, 21, 29, 21, 19, 3, 7, 13]
1|generateSequenceBeadRules(0)|[13]
1|generateSequenceBeadRules(1)|[13]
1|generateSequenceBeadRules(6)|[13, 24, 56, 4, 6, 25]
1|generateSequenceBeadRules(15)|[13, 24, 56, 4, 6, 25, 56, 14, 6, 53, 26, 14, 7, 13, 16]
1|generateSequenceExtraction(0)|[89]
1|generateSequenceExtraction(1)|[89]
1|generateSequenceExtraction(6)|[89, -23, -51, -15, 0, 0]
1|generateSequenceExtraction(15)|[89, -23, -51, -15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateSequenceExtractionThreeDigits(0)|[899]
1|generateSequenceExtractionThreeDigits(1)|[899]
1|generateSequenceExtractionThreeDigits(6)|[899, -152, -515, -111, -111, -10]
1|generateSequenceExtractionThreeDigits(15)|[899, -152, -515, -111, -111, -10, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateSequenceExtractionFiveRules(0)|[89]
1|generateSequenceExtractionFiveRules(1)|[89]
1|generateSequenceExtractionFiveRules(6)|[89, -42, -14, -22, -11, 0]
1|generateSequenceExtractionFiveRules(15)|[89, -42, -14, -22, -11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateSequenceExtractionFiveRulesThreeDigits(0)|[899]
1|generateSequenceExtractionFiveRulesThreeDigits(1)|[899]
1|generateSequenceExtractionFiveRulesThreeDigits(6)|[899, -421, -234, -122, -111, -11]
1|generateSequenceExtractionFiveRulesThreeDigits(15)|[899, -421, -234, -122, -111, -11, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1|generateSequenceExtractionTenRules(0)|[87]
1|generateSequenceExtractionTenRules(1)|[87]
1|generateSequenceExtractionTenRules(2)|[87, -58]
1|generateSequenceExtractionTenRules(3)|[87, -58, -19]
1|generateSequenceExtractionTenRules(4)|[87, -58, -19, -14]
1|generateSequenceExtractionTenRules(6)|EXC:NumberFormat
1|generateSequenceExtractionTenRules(15)|EXC:NumberFormat
1|generateSequenceExtractionTenRules(40)|EXC:NumberFormat
1|generateSequenceExtractionTenRulesThreeDigits(0)|[1997]
1|generateSequenceExtractionTenRulesThreeDigits(1)|[1997]
"""

private val PARCA_2 = """
1|generateSequenceExtractionTenRulesThreeDigits(2)|[1997, -628]
1|generateSequenceExtractionTenRulesThreeDigits(3)|[1997, -628, -241]
1|generateSequenceExtractionTenRulesThreeDigits(4)|[1997, -628, -241, -119]
1|generateSequenceExtractionTenRulesThreeDigits(6)|[1997, -628, -241, -119, -381, -519]
1|generateSequenceExtractionTenRulesThreeDigits(15)|EXC:NumberFormat
1|generateSequenceExtractionTenRulesThreeDigits(40)|EXC:NumberFormat
1|generateSequenceExtractionBeadRules(0)|[83]
1|generateSequenceExtractionBeadRules(1)|[83]
1|generateSequenceExtractionBeadRules(2)|[83, -26]
1|generateSequenceExtractionBeadRules(3)|[83, -26, -43]
1|generateSequenceExtractionBeadRules(4)|[83, -26, -43, -18]
1|generateSequenceExtractionBeadRules(6)|EXC:NumberFormat
1|generateSequenceExtractionBeadRules(15)|EXC:NumberFormat
1|generateSequenceExtractionBeadRules(40)|EXC:NumberFormat
1|generateSequenceExtractionBeadRulesThreeDigits(0)|[1993]
1|generateSequenceExtractionBeadRulesThreeDigits(1)|[1993]
1|generateSequenceExtractionBeadRulesThreeDigits(2)|[1993, -217]
1|generateSequenceExtractionBeadRulesThreeDigits(3)|[1993, -217, -112]
1|generateSequenceExtractionBeadRulesThreeDigits(4)|[1993, -217, -112, -159]
1|generateSequenceExtractionBeadRulesThreeDigits(6)|[1993, -217, -112, -159, -284, -116]
1|generateSequenceExtractionBeadRulesThreeDigits(15)|EXC:NumberFormat
1|generateSequenceExtractionBeadRulesThreeDigits(40)|EXC:NumberFormat
1|generalCollectionOneDigits(0)|[4]
1|generalCollectionOneDigits(1)|[4]
1|generalCollectionOneDigits(8)|[4, 4, 5, 1, 7, 9, 2, 4]
1|generalCollectionTwoDigits(0)|[46]
1|generalCollectionTwoDigits(1)|[46]
1|generalCollectionTwoDigits(8)|[46, 51, 79, 24, 91, 82, 43, 81]
1|generalCollectionThreeDigits(0)|[556]
1|generalCollectionThreeDigits(1)|[556]
1|generalCollectionThreeDigits(8)|[556, 197, 270, 151, 418, 140, 997, 283]
1|generalCollectionFourDigits(0)|[5856]
1|generalCollectionFourDigits(1)|[5856]
1|generalCollectionFourDigits(8)|[5856, 4971, 2015, 1418, 3407, 8708, 5325, 2279]
1|generateSequenceExtractionRace(0)|[56]
1|generateSequenceExtractionRace(1)|[56]
1|generateSequenceExtractionRace(8)|[56, -5, -1, -7, -9, -2, -4, -9]
1|generateSequenceExtractionRaceHard(0)|[562]
1|generateSequenceExtractionRaceHard(1)|[562]
1|generateSequenceExtractionRaceHard(8)|[562, -1, -7, -9, -2, -4, -9, -1]
1|generateSequenceExtractionRaceTwoDigits(0)|[562]
1|generateSequenceExtractionRaceTwoDigits(1)|[562]
1|generateSequenceExtractionRaceTwoDigits(8)|[562, -71, -29, -94, -81, -42, -83, -51]
1|generateSequenceExtractionRaceTwoDigitsHard(0)|[5628]
1|generateSequenceExtractionRaceTwoDigitsHard(1)|[5628]
1|generateSequenceExtractionRaceTwoDigitsHard(8)|[5628, -97, -42, -19, -28, -34, -18, -35]
1|generateSequenceExtractionRaceThreeDigits(0)|[5628]
1|generateSequenceExtractionRaceThreeDigits(1)|[5628]
1|generateSequenceExtractionRaceThreeDigits(8)|[5628, -297, -194, -428, -183, -935, -263, -287]
1|generateSequenceExtractionRaceThreeDigitsHard(0)|[56289]
1|generateSequenceExtractionRaceThreeDigitsHard(1)|[56289]
1|generateSequenceExtractionRaceThreeDigitsHard(8)|[56289, -429, -819, -342, -518, -393, -726, -428]
1|generateSequenceExtractionRaceFourDigits(0)|[56289]
1|generateSequenceExtractionRaceFourDigits(1)|[56289]
1|generateSequenceExtractionRaceFourDigits(8)|[56289, -9429, -4281, -5183, -6393, -2872, -7574, -9285]
1|G.generateRandomNumber(-1)|0
1|G.generateRandomNumber(0)|0
1|G.generateRandomNumber(1)|4
1|G.generateRandomNumber(2)|85
1|G.generateRandomNumber(3)|265
1|G.generateRandomNumber(5)|11965
1|G.randomNumberChangeToString(1)|8
1|G.randomNumberChangeToString(2)|94
1|G.randomNumberChangeToString(4)|2311
1|G.randomUniqueNumberStrings(1,5)|[8, 3, 7, 2, 5]
1|G.randomUniqueNumberStrings(1,9)|[8, 3, 7, 2, 5, 1, 9, 6, 4]
1|G.randomUniqueNumberStrings(1,12)|[9, 4, 2, 5, 1, 7, 8, 9, 6, 1, 8, 3]
1|G.randomUniqueNumberStrings(2,4)|[94, 66, 36, 31]
1|G.randomUniqueNumberStrings(1,0)|[]
1|G.randomUniqueNumberStrings(0,3)|EXC:NoSuchElement
1|G.randomUniqueNumberStrings(3,6)|[152, 672, 104, 564, 877, 116]
2|generateRelatedNumbers0(1,1)|(7 + 2)
2|generateRelatedNumbers0(2,2)|(7 + 2)
2|generateRelatedNumbers(1,1)|(7 + 1)
2|generateRelatedNumbers(2,2)|(76 + 13)
2|generateRelatedNumbers(3,2)|(756 + 10)
2|generateRelatedNumbers(2,3)|(76 + 913)
2|generateRelatedNumbers(4,4)|(7556 + 1240)
2|generateRelatedNumbers(2,1)|(76 + 3)
2|generateRelatedNumbers(1,2)|(7 + 60)
"""

private val PARCA_3 = """
2|generateRelatedNumbersList(5,1,1)|[(7 + 1), (8 + 1), (1 + 1), (3 + 6), (5 + 3)]
2|generateRelatedNumbersList(12,1,1)|[(7 + 1), (8 + 1), (1 + 1), (3 + 6), (5 + 3), (4 + 5), (6 + 3), (2 + 2), (5 + 4), (8 + 1), (8 + 1), (1 + 2)]
2|generateRelatedNumbersList(6,2,2)|[(76 + 13), (12 + 65), (81 + 16), (28 + 21), (46 + 53), (77 + 11)]
2|generateRelatedNumbersList(0,2,2)|[]
2|generateRelatedNumbersList(-1,1,1)|[]
2|generateRelatedNumbersList(4,3,2)|[(756 + 10), (750 + 30), (602 + 12), (662 + 25)]
2|generateRelatedNumbers2(1,1)|(5 + 1)
2|generateRelatedNumbers2(2,2)|(54 + 23)
2|generateRelatedNumbers2(3,3)|(544 + 231)
2|generateRelatedNumbers2(2,3)|(54 + 123)
2|generateRelatedNumbers2(3,2)|(544 + 31)
2|generateRelatedNumbers2Blinding(5,1)|[4, 1, 1, 3, 0]
2|generateRelatedNumbers2Blinding(5,2)|[44, 12, 42, 1, 0]
2|generateRelatedNumbers2Blinding(8,3)|[444, 332, 111, 112, 0, 0, 0, 0]
2|generateRelatedNumbers2Blinding(0,2)|[]
2|generateRelatedNumbers2Blinding(3,0)|[4, 1, 1]
2|generateRelatedNumbers2Blinding(1,4)|[4442]
2|generateRelatedNumbers2Blinding(20,2)|[44, 12, 42, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateRandomMathOperation1()|(84 + 11)
2|generateMathOperation()|(15 + 5)
2|generateMathOperationList(0)|[]
2|generateMathOperationList(1)|[(69 + 4)]
2|generateMathOperationList(5)|[(69 + 4), (69 + 1), (69 + 3), (29 + 2), (68 + 5)]
2|generateMathOperationList(12)|[(69 + 4), (69 + 1), (69 + 3), (29 + 2), (68 + 5), (29 + 2), (29 + 1), (27 + 4), (17 + 3), (75 + 5), (56 + 4), (89 + 1)]
2|generateMathOperationList(-3)|[]
2|generateMathOperation2()|(99 + 5)
2|generateMathOperationWithDigits(1,1)|(9 + 1)
2|generateMathOperationWithDigits(2,2)|(99 + 53)
2|generateMathOperationWithDigits(3,2)|(995 + 34)
2|generateMathOperationWithDigits(3,3)|(995 + 345)
2|generateMathOperationWithDigits(2,3)|EXC:IndexOutOfBounds
2|generateMathOperation3()|(12 + 9)
2|generateMathOperationList3(0)|[]
2|generateMathOperationList3(1)|[(39 + 8)]
2|generateMathOperationList3(4)|[(39 + 8), (13 + 7), (69 + 6), (29 + 9)]
2|generateMathOperationList3(10)|[(39 + 8), (13 + 7), (69 + 6), (29 + 9), (51 + 9), (57 + 8), (69 + 6), (24 + 7), (67 + 9), (58 + 7)]
2|generateMathOperation4()|(97 + 9)
2|generateMathOperationList4(0)|[]
2|generateMathOperationList4(1)|[(49 + 8)]
2|generateMathOperationList4(4)|[(49 + 8), (93 + 7), (49 + 6), (99 + 9)]
2|generateMathOperationList4(10)|[(49 + 8), (93 + 7), (49 + 6), (99 + 9), (91 + 9), (47 + 8), (99 + 6), (94 + 7), (47 + 9), (48 + 7)]
2|generateMathOperationWithDigits2(1,1)|(8 + 7)
2|generateMathOperationWithDigits2(2,2)|(87 + 98)
2|generateMathOperationWithDigits2(3,2)|(876 + 89)
2|generateMathOperationWithDigits2(2,3)|(87 + 998)
2|generateMathOperationWithDigits2(3,3)|(876 + 989)
2|generateMathOperationWithDigits3(2,2)|(87 + 93)
2|generateMathOperationWithDigits3(3,3)|(876 + 159)
2|generateMathOperationWithDigits3(3,1)|(876 + 1)
2|generateMathOperationWithDigits3(1,2)|EXC:IndexOutOfBounds
2|generateMathOperationBeadRule()|(66 + 6)
2|generateMathOperationWithDigitsBeadRule(2,2)|(86 + 96)
2|generateMathOperationWithDigitsBeadRule(3,3)|(868 + 669)
2|generateMathOperationWithDigitsBeadRule(3,2)|(868 + 66)
2|generateMathOperationWithDigitsBeadRule(1,1)|(8 + 7)
2|irregularExtraction(1,1)|(6 - 1)
2|irregularExtraction(2,2)|(87 - 75)
2|irregularExtraction(3,2)|(876 - 66)
2|irregularExtraction(3,3)|(876 - 661)
2|irregularExtractionFiveRules(2,2)|(86 - 42)
2|irregularExtractionFiveRules(3,3)|(868 - 424)
2|irregularExtractionFiveRules(2,1)|(86 - 4)
2|irregularExtractionFiveRules(1,2)|EXC:IndexOutOfBounds
2|irregularExtractionFiveRulesMix(2,2)|(59 - 34)
2|irregularExtractionFiveRulesMix(3,3)|(596 - 564)
2|irregularExtractionFiveRulesMix(3,2)|(596 - 56)
2|irregularExtractionFiveRulesMix(1,1)|(5 - 3)
2|extractionGenerateMathOperationList(0)|[]
2|extractionGenerateMathOperationList(1)|[(30 - 1)]
2|extractionGenerateMathOperationList(9)|[(30 - 1), (11 - 2), (82 - 3), (30 - 4), (61 - 5), (45 - 6), (30 - 7), (20 - 8), (63 - 9)]
2|extractionGenerateMathOperationList(20)|[(30 - 1), (11 - 2), (82 - 3), (30 - 4), (61 - 5), (45 - 6), (30 - 7), (20 - 8), (63 - 9), (90 - 1), (31 - 2), (31 - 3), (42 - 4), (32 - 5), (25 - 6), (20 - 7), (61 - 8), (17 - 9), (90 - 1), (90 - 2)]
2|extractionGenerateMathOperation()|(86 - 9)
2|extractionGenerateMathOperationTen()|(865 - 96)
2|extractionGenerateMathOperationTenWithOtherRules()|(895 - 99)
2|extractionGenerateMathOperationTenWithOtherRulesExtreme()|(305 - 9)
2|extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton()|(895 - 46)
2|extractionBeadRules()|(82 - 7)
2|extractionBeadRulesList(0)|[]
2|extractionBeadRulesList(1)|[(13 - 8)]
2|extractionBeadRulesList(4)|[(13 - 8), (14 - 7), (92 - 6), (64 - 9)]
"""

private val PARCA_4 = """
2|extractionBeadRulesList(10)|[(13 - 8), (14 - 7), (92 - 6), (64 - 9), (74 - 7), (41 - 6), (64 - 9), (94 - 8), (72 - 6), (93 - 8)]
2|extractionBeadRulesThreeTwo()|(824 - 68)
2|extractionBeadRulesFourThree()|(8241 - 696)
2|multiplicationLessFive()|(53 x 5)
2|multiplicationLessFiveFull()|(99 x 5)
2|multiplicationFull()|(89 x 9)
2|multiplicationTwo()|(44 x 42)
2|multiplicationTwoFull()|(89 x 67)
2|multiplicationThreeFull()|(890 x 2)
2|multiplicationThreeTwoFive()|(532 x 35)
2|multiplicationThreeTwoFull()|(890 x 61)
2|generateSequence1(0)|[21]
2|generateSequence1(1)|[21]
2|generateSequence1(6)|[21, 55, 12, 11, 0, 0]
2|generateSequence1(15)|[21, 55, 12, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateSequence1Digits(0,2)|[]
2|generateSequence1Digits(1,1)|[2]
2|generateSequence1Digits(6,1)|[2, 1, 5, 1, 0, 0]
2|generateSequence1Digits(6,2)|[20, 55, 12, 11, 1, 0]
2|generateSequence1Digits(10,3)|[202, 155, 111, 521, 10, 0, 0, 0, 0, 0]
2|generateSequence1Digits(5,0)|[2, 1, 5, 1, 0]
2|generateSequence5RulesNew(0)|[21]
2|generateSequence5RulesNew(1)|[21]
2|generateSequence5RulesNew(6)|[21, 41, 14, 11, 11, 1]
2|generateSequence5RulesNew(15)|[21, 41, 14, 11, 11, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateSequence10RulesEasyNew(0)|[42]
2|generateSequence10RulesEasyNew(1)|[42]
2|generateSequence10RulesEasyNew(6)|[42, 45, 15, 13, 45, 23]
2|generateSequence10RulesEasyNew(15)|[42, 45, 15, 13, 45, 23, 16, 1, 17, 43, 28, 12, 22, 46, 22]
2|generateSequence10RulesEasyOneCount(0)|[87]
2|generateSequence10RulesEasyOneCount(1)|[87]
2|generateSequence10RulesEasyOneCount(6)|[87, 5, 7, 2, 4, 5]
2|generateSequence10RulesEasyOneCount(15)|[87, 5, 7, 2, 4, 5, 9, 1, 7, 3, 8, 3, 4, 5, 3]
2|generateSequence10RulesEasyOldOneCount(0)|[87]
2|generateSequence10RulesEasyOldOneCount(1)|[87]
2|generateSequence10RulesEasyOldOneCount(6)|[87, 9, 9, 5, 2, 8]
2|generateSequence10RulesEasyOldOneCount(15)|[87, 9, 9, 5, 2, 8, 9, 6, 5, 1, 9, 1, 9, 7, 9]
2|generateSequence10RulesEasyOld(0)|[23]
2|generateSequence10RulesEasyOld(1)|[23]
2|generateSequence10RulesEasyOld(6)|[23, 49, 18, 6, 9, 12]
2|generateSequence10RulesEasyOld(15)|[23, 49, 18, 6, 9, 12, 19, 39, 15, 1, 9, 21, 39, 27, 19]
2|generateSequenceBeadRules(0)|[22]
2|generateSequenceBeadRules(1)|[22]
2|generateSequenceBeadRules(6)|[22, 53, 19, 2, 8, 12]
2|generateSequenceBeadRules(15)|[22, 53, 19, 2, 8, 12, 17, 52, 19, 24, 56, 12, 7, 13, 56]
2|generateSequenceExtraction(0)|[98]
2|generateSequenceExtraction(1)|[98]
2|generateSequenceExtraction(6)|[98, -51, -35, -11, -1, 0]
2|generateSequenceExtraction(15)|[98, -51, -35, -11, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateSequenceExtractionThreeDigits(0)|[989]
2|generateSequenceExtractionThreeDigits(1)|[989]
2|generateSequenceExtractionThreeDigits(6)|[989, -155, -113, -511, -110, -100]
2|generateSequenceExtractionThreeDigits(15)|[989, -155, -113, -511, -110, -100, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateSequenceExtractionFiveRules(0)|[98]
2|generateSequenceExtractionFiveRules(1)|[98]
2|generateSequenceExtractionFiveRules(6)|[98, -24, -43, -11, -10, -10]
2|generateSequenceExtractionFiveRules(15)|[98, -24, -43, -11, -10, -10, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateSequenceExtractionFiveRulesThreeDigits(0)|[989]
2|generateSequenceExtractionFiveRulesThreeDigits(1)|[989]
2|generateSequenceExtractionFiveRulesThreeDigits(6)|[989, -142, -433, -213, -101, -100]
2|generateSequenceExtractionFiveRulesThreeDigits(15)|[989, -142, -433, -213, -101, -100, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2|generateSequenceExtractionTenRules(0)|[96]
2|generateSequenceExtractionTenRules(1)|[96]
2|generateSequenceExtractionTenRules(2)|[96, -67]
2|generateSequenceExtractionTenRules(3)|[96, -67, -13]
2|generateSequenceExtractionTenRules(4)|[96, -67, -13, -17]
2|generateSequenceExtractionTenRules(6)|EXC:NumberFormat
2|generateSequenceExtractionTenRules(15)|EXC:NumberFormat
2|generateSequenceExtractionTenRules(40)|EXC:NumberFormat
2|generateSequenceExtractionTenRulesThreeDigits(0)|[4895]
2|generateSequenceExtractionTenRulesThreeDigits(1)|[4895]
2|generateSequenceExtractionTenRulesThreeDigits(2)|[4895, -369]
2|generateSequenceExtractionTenRulesThreeDigits(3)|[4895, -369, -318]
2|generateSequenceExtractionTenRulesThreeDigits(4)|[4895, -369, -318, -139]
2|generateSequenceExtractionTenRulesThreeDigits(6)|[4895, -369, -318, -139, -933, -128]
2|generateSequenceExtractionTenRulesThreeDigits(15)|[4895, -369, -318, -139, -933, -128, -179, -415, -215, -111, -929, -145, -615, -123, -157]
2|generateSequenceExtractionTenRulesThreeDigits(40)|[4895, -369, -318, -139, -933, -128, -179, -415, -215, -111, -929, -145, -615, -123, -157, -111, -339, -218, -328, -439, -219, -418, -529, -158, -332, -614, -448, -225, -122, -129, -112, -213, -327, -138, -286, -258, -115, -615, -215, -415]
2|generateSequenceExtractionBeadRules(0)|[92]
2|generateSequenceExtractionBeadRules(1)|[92]
2|generateSequenceExtractionBeadRules(2)|[92, -26]
"""

private val PARCA_5 = """
2|generateSequenceExtractionBeadRules(3)|[92, -26, -14]
2|generateSequenceExtractionBeadRules(4)|[92, -26, -14, -16]
2|generateSequenceExtractionBeadRules(6)|[92, -26, -14, -16, -23, -18]
2|generateSequenceExtractionBeadRules(15)|EXC:NumberFormat
2|generateSequenceExtractionBeadRules(40)|EXC:NumberFormat
2|generateSequenceExtractionBeadRulesThreeDigits(0)|[4891]
2|generateSequenceExtractionBeadRulesThreeDigits(1)|[4891]
2|generateSequenceExtractionBeadRulesThreeDigits(2)|[4891, -126]
2|generateSequenceExtractionBeadRulesThreeDigits(3)|[4891, -126, -153]
2|generateSequenceExtractionBeadRulesThreeDigits(4)|[4891, -126, -153, -516]
2|generateSequenceExtractionBeadRulesThreeDigits(6)|[4891, -126, -153, -516, -924, -117]
2|generateSequenceExtractionBeadRulesThreeDigits(15)|[4891, -126, -153, -516, -924, -117, -142, -217, -122, -316, -124, -116, -614, -138, -114]
2|generateSequenceExtractionBeadRulesThreeDigits(40)|[4891, -126, -153, -516, -924, -117, -142, -217, -122, -316, -124, -116, -614, -138, -114, -147, -331, -519, -132, -616, -426, -615, -527, -116, -114, -226, -114, -126, -133, -115, -171, -114, -227, -513, -116, -113, -225, -427, -116, -114]
2|generalCollectionOneDigits(0)|[8]
2|generalCollectionOneDigits(1)|[8]
2|generalCollectionOneDigits(8)|[8, 7, 6, 6, 9, 3, 1, 6]
2|generalCollectionTwoDigits(0)|[89]
2|generalCollectionTwoDigits(1)|[89]
2|generalCollectionTwoDigits(8)|[89, 66, 93, 16, 32, 43, 19, 73]
2|generalCollectionThreeDigits(0)|[699]
2|generalCollectionThreeDigits(1)|[699]
2|generalCollectionThreeDigits(8)|[699, 619, 151, 226, 146, 339, 254, 807]
2|generalCollectionFourDigits(0)|[6799]
2|generalCollectionFourDigits(1)|[6799]
2|generalCollectionFourDigits(8)|[6799, 6197, 3152, 3346, 1395, 1490, 7857, 9931]
2|generateSequenceExtractionRace(0)|[99]
2|generateSequenceExtractionRace(1)|[99]
2|generateSequenceExtractionRace(8)|[99, -6, -6, -9, -3, -1, -6, -3]
2|generateSequenceExtractionRaceHard(0)|[990]
2|generateSequenceExtractionRaceHard(1)|[990]
2|generateSequenceExtractionRaceHard(8)|[990, -6, -9, -3, -1, -6, -3, -2]
2|generateSequenceExtractionRaceTwoDigits(0)|[990]
2|generateSequenceExtractionRaceTwoDigits(1)|[990]
2|generateSequenceExtractionRaceTwoDigits(8)|[990, -96, -13, -36, -42, -13, -79, -73]
2|generateSequenceExtractionRaceTwoDigitsHard(0)|[9907]
2|generateSequenceExtractionRaceTwoDigitsHard(1)|[9907]
2|generateSequenceExtractionRaceTwoDigitsHard(8)|[9907, -39, -61, -23, -34, -91, -37, -77]
2|generateSequenceExtractionRaceThreeDigits(0)|[9907]
2|generateSequenceExtractionRaceThreeDigits(1)|[9907]
2|generateSequenceExtractionRaceThreeDigits(8)|[9907, -139, -236, -134, -379, -277, -871, -512]
2|generateSequenceExtractionRaceThreeDigitsHard(0)|[99071]
2|generateSequenceExtractionRaceThreeDigitsHard(1)|[99071]
2|generateSequenceExtractionRaceThreeDigitsHard(8)|[99071, -613, -423, -913, -737, -127, -287, -251]
2|generateSequenceExtractionRaceFourDigits(0)|[99071]
2|generateSequenceExtractionRaceFourDigits(1)|[99071]
2|generateSequenceExtractionRaceFourDigits(8)|[99071, -3613, -1342, -7379, -7127, -5128, -6742, -2913]
2|G.generateRandomNumber(-1)|0
2|G.generateRandomNumber(0)|0
2|G.generateRandomNumber(1)|8
2|G.generateRandomNumber(2)|89
2|G.generateRandomNumber(3)|629
2|G.generateRandomNumber(5)|19529
2|G.randomNumberChangeToString(1)|5
2|G.randomNumberChangeToString(2)|13
2|G.randomNumberChangeToString(4)|8094
2|G.randomUniqueNumberStrings(1,5)|[5, 9, 1, 4, 7]
2|G.randomUniqueNumberStrings(1,9)|[5, 9, 1, 4, 7, 6, 2, 3, 8]
2|G.randomUniqueNumberStrings(1,12)|[3, 1, 3, 6, 5, 4, 4, 9, 2, 2, 7, 8]
2|G.randomUniqueNumberStrings(2,4)|[13, 67, 46, 52]
2|G.randomUniqueNumberStrings(1,0)|[]
2|G.randomUniqueNumberStrings(0,3)|EXC:NoSuchElement
2|G.randomUniqueNumberStrings(3,6)|[340, 581, 836, 211, 546, 723]
3|generateRelatedNumbers0(1,1)|(5 + 4)
3|generateRelatedNumbers0(2,2)|(5 + 4)
3|generateRelatedNumbers(1,1)|(5 + 3)
3|generateRelatedNumbers(2,2)|(56 + 43)
3|generateRelatedNumbers(3,2)|(576 + 13)
3|generateRelatedNumbers(2,3)|(56 + 243)
3|generateRelatedNumbers(4,4)|(5706 + 4111)
3|generateRelatedNumbers(2,1)|(56 + 2)
3|generateRelatedNumbers(1,2)|(5 + 80)
3|generateRelatedNumbersList(5,1,1)|[(5 + 3), (8 + 1), (4 + 5), (6 + 2), (2 + 2)]
3|generateRelatedNumbersList(12,1,1)|[(5 + 3), (8 + 1), (4 + 5), (6 + 2), (2 + 2), (7 + 2), (1 + 1), (3 + 6), (2 + 2), (3 + 6), (6 + 3), (1 + 2)]
3|generateRelatedNumbersList(6,2,2)|[(56 + 43), (44 + 55), (61 + 18), (41 + 56), (58 + 40), (83 + 16)]
3|generateRelatedNumbersList(0,2,2)|[]
3|generateRelatedNumbersList(-1,1,1)|[]
3|generateRelatedNumbersList(4,3,2)|[(576 + 13), (637 + 52), (853 + 30), (582 + 17)]
3|generateRelatedNumbers2(1,1)|(4 + 2)
3|generateRelatedNumbers2(2,2)|(43 + 22)
3|generateRelatedNumbers2(3,3)|(437 + 341)
"""

private val PARCA_6 = """
3|generateRelatedNumbers2(2,3)|(43 + 722)
3|generateRelatedNumbers2(3,2)|(437 + 41)
3|generateRelatedNumbers2Blinding(5,1)|[4, 4, 1, 0, 0]
3|generateRelatedNumbers2Blinding(5,2)|[43, 44, 11, 1, 0]
3|generateRelatedNumbers2Blinding(8,3)|[437, 242, 310, 10, 0, 0, 0, 0]
3|generateRelatedNumbers2Blinding(0,2)|[]
3|generateRelatedNumbers2Blinding(3,0)|[4, 4, 1]
3|generateRelatedNumbers2Blinding(1,4)|[4373]
3|generateRelatedNumbers2Blinding(20,2)|[43, 44, 11, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateRandomMathOperation1()|(14 + 64)
3|generateMathOperation()|(59 + 1)
3|generateMathOperationList(0)|[]
3|generateMathOperationList(1)|[(39 + 5)]
3|generateMathOperationList(5)|[(39 + 5), (59 + 4), (18 + 2), (27 + 3), (19 + 1)]
3|generateMathOperationList(12)|[(39 + 5), (59 + 4), (18 + 2), (27 + 3), (19 + 1), (16 + 5), (79 + 4), (39 + 2), (87 + 3), (29 + 1), (18 + 2), (17 + 3)]
3|generateMathOperationList(-3)|[]
3|generateMathOperation2()|(95 + 5)
3|generateMathOperationWithDigits(1,1)|(5 + 5)
3|generateMathOperationWithDigits(2,2)|(55 + 55)
3|generateMathOperationWithDigits(3,2)|(558 + 55)
3|generateMathOperationWithDigits(3,3)|(558 + 553)
3|generateMathOperationWithDigits(2,3)|EXC:IndexOutOfBounds
3|generateMathOperation3()|(53 + 8)
3|generateMathOperationList3(0)|[]
3|generateMathOperationList3(1)|[(53 + 9)]
3|generateMathOperationList3(4)|[(53 + 9), (69 + 7), (89 + 6), (27 + 8)]
3|generateMathOperationList3(10)|[(53 + 9), (69 + 7), (89 + 6), (27 + 8), (28 + 7), (64 + 6), (38 + 9), (72 + 8), (24 + 6), (72 + 8)]
3|generateMathOperation4()|(97 + 9)
3|generateMathOperationList4(0)|[]
3|generateMathOperationList4(1)|[(93 + 9)]
3|generateMathOperationList4(4)|[(93 + 9), (99 + 7), (99 + 6), (47 + 8)]
3|generateMathOperationList4(10)|[(93 + 9), (99 + 7), (99 + 6), (47 + 8), (98 + 7), (94 + 6), (48 + 9), (92 + 8), (44 + 6), (92 + 8)]
3|generateMathOperationWithDigits2(1,1)|(1 + 9)
3|generateMathOperationWithDigits2(2,2)|(17 + 99)
3|generateMathOperationWithDigits2(3,2)|(178 + 98)
3|generateMathOperationWithDigits2(2,3)|(17 + 998)
3|generateMathOperationWithDigits2(3,3)|(178 + 988)
3|generateMathOperationWithDigits3(2,2)|(17 + 84)
3|generateMathOperationWithDigits3(3,3)|(178 + 137)
3|generateMathOperationWithDigits3(3,1)|(178 + 1)
3|generateMathOperationWithDigits3(1,2)|EXC:IndexOutOfBounds
3|generateMathOperationBeadRule()|(37 + 6)
3|generateMathOperationWithDigitsBeadRule(2,2)|(77 + 99)
3|generateMathOperationWithDigitsBeadRule(3,3)|(778 + 978)
3|generateMathOperationWithDigitsBeadRule(3,2)|(778 + 97)
3|generateMathOperationWithDigitsBeadRule(1,1)|(7 + 8)
3|irregularExtraction(1,1)|(8 - 5)
3|irregularExtraction(2,2)|(17 - 16)
3|irregularExtraction(3,2)|(178 - 17)
3|irregularExtraction(3,3)|(178 - 173)
3|irregularExtractionFiveRules(2,2)|(77 - 33)
3|irregularExtractionFiveRules(3,3)|(778 - 344)
3|irregularExtractionFiveRules(2,1)|(77 - 3)
3|irregularExtractionFiveRules(1,2)|EXC:IndexOutOfBounds
3|irregularExtractionFiveRulesMix(2,2)|(89 - 87)
3|irregularExtractionFiveRulesMix(3,3)|(893 - 461)
3|irregularExtractionFiveRulesMix(3,2)|(893 - 46)
3|irregularExtractionFiveRulesMix(1,1)|(8 - 3)
3|extractionGenerateMathOperationList(0)|[]
3|extractionGenerateMathOperationList(1)|[(70 - 1)]
3|extractionGenerateMathOperationList(9)|[(70 - 1), (81 - 2), (71 - 3), (93 - 4), (42 - 5), (75 - 6), (21 - 7), (65 - 8), (25 - 9)]
3|extractionGenerateMathOperationList(20)|[(70 - 1), (81 - 2), (71 - 3), (93 - 4), (42 - 5), (75 - 6), (21 - 7), (65 - 8), (25 - 9), (90 - 1), (91 - 2), (82 - 3), (13 - 4), (80 - 5), (25 - 6), (21 - 7), (30 - 8), (43 - 9), (60 - 1), (11 - 2)]
3|extractionGenerateMathOperation()|(66 - 8)
3|extractionGenerateMathOperationTen()|(667 - 78)
3|extractionGenerateMathOperationTenWithOtherRules()|(657 - 58)
3|extractionGenerateMathOperationTenWithOtherRulesExtreme()|(730 - 35)
3|extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton()|(157 - 18)
3|extractionBeadRules()|(63 - 7)
3|extractionBeadRulesList(0)|[]
3|extractionBeadRulesList(1)|[(84 - 9)]
3|extractionBeadRulesList(4)|[(84 - 9), (73 - 7), (94 - 6), (44 - 8)]
3|extractionBeadRulesList(10)|[(84 - 9), (73 - 7), (94 - 6), (44 - 8), (73 - 7), (64 - 8), (21 - 6), (94 - 9), (94 - 8), (22 - 7)]
3|extractionBeadRulesThreeTwo()|(134 - 87)
3|extractionBeadRulesFourThree()|(1344 - 798)
3|multiplicationLessFive()|(13 x 5)
3|multiplicationLessFiveFull()|(55 x 8)
3|multiplicationFull()|(15 x 9)
3|multiplicationTwo()|(30 x 31)
3|multiplicationTwoFull()|(15 x 86)
3|multiplicationThreeFull()|(153 x 8)
"""

private val PARCA_7 = """
3|multiplicationThreeTwoFive()|(131 x 24)
3|multiplicationThreeTwoFull()|(153 x 10)
3|generateSequence1(0)|[22]
3|generateSequence1(1)|[22]
3|generateSequence1(6)|[22, 55, 11, 11, 0, 0]
3|generateSequence1(15)|[22, 55, 11, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateSequence1Digits(0,2)|[]
3|generateSequence1Digits(1,1)|[2]
3|generateSequence1Digits(6,1)|[2, 5, 1, 1, 0, 0]
3|generateSequence1Digits(6,2)|[20, 51, 12, 15, 1, 0]
3|generateSequence1Digits(10,3)|[201, 522, 155, 111, 10, 0, 0, 0, 0, 0]
3|generateSequence1Digits(5,0)|[2, 5, 1, 1, 0]
3|generateSequence5RulesNew(0)|[22]
3|generateSequence5RulesNew(1)|[22]
3|generateSequence5RulesNew(6)|[22, 44, 12, 11, 10, 0]
3|generateSequence5RulesNew(15)|[22, 44, 12, 11, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateSequence10RulesEasyNew(0)|[33]
3|generateSequence10RulesEasyNew(1)|[33]
3|generateSequence10RulesEasyNew(6)|[33, 43, 15, 5, 4, 14]
3|generateSequence10RulesEasyNew(15)|[33, 43, 15, 5, 4, 14, 21, 35, 19, 11, 24, 41, 25, 7, 3]
3|generateSequence10RulesEasyOneCount(0)|[17]
3|generateSequence10RulesEasyOneCount(1)|[17]
3|generateSequence10RulesEasyOneCount(6)|[17, 3, 5, 5, 2, 5]
3|generateSequence10RulesEasyOneCount(15)|[17, 3, 5, 5, 2, 5, 4, 6, 5, 6, 5, 2, 5, 7, 5]
3|generateSequence10RulesEasyOldOneCount(0)|[17]
3|generateSequence10RulesEasyOldOneCount(1)|[17]
3|generateSequence10RulesEasyOldOneCount(6)|[17, 3, 5, 5, 2, 8]
3|generateSequence10RulesEasyOldOneCount(15)|[17, 3, 5, 5, 2, 8, 2, 9, 9, 2, 3, 5, 1, 4, 5]
3|generateSequence10RulesEasyOld(0)|[17]
3|generateSequence10RulesEasyOld(1)|[17]
3|generateSequence10RulesEasyOld(6)|[17, 48, 15, 14, 6, 14]
3|generateSequence10RulesEasyOld(15)|[17, 48, 15, 14, 6, 14, 28, 38, 19, 6, 24, 46, 13, 17, 11]
3|generateSequenceBeadRules(0)|[23]
3|generateSequenceBeadRules(1)|[23]
3|generateSequenceBeadRules(6)|[23, 55, 16, 4, 6, 13]
3|generateSequenceBeadRules(15)|[23, 55, 16, 4, 6, 13, 56, 14, 16, 55, 26, 14, 6, 14, 16]
3|generateSequenceExtraction(0)|[99]
3|generateSequenceExtraction(1)|[99]
3|generateSequenceExtraction(6)|[99, -55, -22, -11, -11, 0]
3|generateSequenceExtraction(15)|[99, -55, -22, -11, -11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateSequenceExtractionThreeDigits(0)|[999]
3|generateSequenceExtractionThreeDigits(1)|[999]
3|generateSequenceExtractionThreeDigits(6)|[999, -523, -255, -111, -110, 0]
3|generateSequenceExtractionThreeDigits(15)|[999, -523, -255, -111, -110, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateSequenceExtractionFiveRules(0)|[99]
3|generateSequenceExtractionFiveRules(1)|[99]
3|generateSequenceExtractionFiveRules(6)|[99, -22, -43, -21, -11, -1]
3|generateSequenceExtractionFiveRules(15)|[99, -22, -43, -21, -11, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateSequenceExtractionFiveRulesThreeDigits(0)|[999]
3|generateSequenceExtractionFiveRulesThreeDigits(1)|[999]
3|generateSequenceExtractionFiveRulesThreeDigits(6)|[999, -212, -343, -211, -112, -111]
3|generateSequenceExtractionFiveRulesThreeDigits(15)|[999, -212, -343, -211, -112, -111, -10, 0, 0, 0, 0, 0, 0, 0, 0]
3|generateSequenceExtractionTenRules(0)|[97]
3|generateSequenceExtractionTenRules(1)|[97]
3|generateSequenceExtractionTenRules(2)|[97, -68]
3|generateSequenceExtractionTenRules(3)|[97, -68, -15]
3|generateSequenceExtractionTenRules(4)|[97, -68, -15, -15]
3|generateSequenceExtractionTenRules(6)|EXC:NumberFormat
3|generateSequenceExtractionTenRules(15)|EXC:NumberFormat
3|generateSequenceExtractionTenRules(40)|EXC:NumberFormat
3|generateSequenceExtractionTenRulesThreeDigits(0)|[3998]
3|generateSequenceExtractionTenRulesThreeDigits(1)|[3998]
3|generateSequenceExtractionTenRulesThreeDigits(2)|[3998, -259]
3|generateSequenceExtractionTenRulesThreeDigits(3)|[3998, -259, -322]
3|generateSequenceExtractionTenRulesThreeDigits(4)|[3998, -259, -322, -119]
3|generateSequenceExtractionTenRulesThreeDigits(6)|[3998, -259, -322, -119, -159, -113]
3|generateSequenceExtractionTenRulesThreeDigits(15)|[3998, -259, -322, -119, -159, -113, -219, -588, -117, -165, -129, -369, -329, -117, -119]
3|generateSequenceExtractionTenRulesThreeDigits(40)|EXC:NumberFormat
3|generateSequenceExtractionBeadRules(0)|[93]
3|generateSequenceExtractionBeadRules(1)|[93]
3|generateSequenceExtractionBeadRules(2)|[93, -26]
3|generateSequenceExtractionBeadRules(3)|[93, -26, -14]
3|generateSequenceExtractionBeadRules(4)|[93, -26, -14, -46]
3|generateSequenceExtractionBeadRules(6)|[93, -26, -14, -46, -33, -12]
3|generateSequenceExtractionBeadRules(15)|[93, -26, -14, -46, -33, -12, -14, -37, -25, -19, -26, -45, -16, -17, -23]
3|generateSequenceExtractionBeadRules(40)|[93, -26, -14, -46, -33, -12, -14, -37, -25, -19, -26, -45, -16, -17, -23, -11, -16, -13, -93, -28, -16, -13, -16, -54, -17, -13, -46, -12, -74, -26, -94, -26, -14, -26, -14, -26, -44, -17, -15, -18]
3|generateSequenceExtractionBeadRulesThreeDigits(0)|[3994]
3|generateSequenceExtractionBeadRulesThreeDigits(1)|[3994]
3|generateSequenceExtractionBeadRulesThreeDigits(2)|[3994, -129]
3|generateSequenceExtractionBeadRulesThreeDigits(3)|[3994, -129, -252]
"""

private val PARCA_8 = """
3|generateSequenceExtractionBeadRulesThreeDigits(4)|[3994, -129, -252, -518]
3|generateSequenceExtractionBeadRulesThreeDigits(6)|[3994, -129, -252, -518, -223, -216]
3|generateSequenceExtractionBeadRulesThreeDigits(15)|[3994, -129, -252, -518, -223, -216, -542, -119, -114, -216, -152, -218, -121, -116, -514]
3|generateSequenceExtractionBeadRulesThreeDigits(40)|[3994, -129, -252, -518, -223, -216, -542, -119, -114, -216, -152, -218, -121, -116, -514, -418, -112, -517, -376, -215, -227, -216, -213, -124, -118, -126, -114, -126, -244, -547, -125, -116, -217, -164, -116, -213, -146, -124, -215, -121]
3|generalCollectionOneDigits(0)|[1]
3|generalCollectionOneDigits(1)|[1]
3|generalCollectionOneDigits(8)|[1, 7, 8, 1, 2, 5, 8, 4]
3|generalCollectionTwoDigits(0)|[15]
3|generalCollectionTwoDigits(1)|[15]
3|generalCollectionTwoDigits(8)|[15, 81, 25, 84, 32, 44, 62, 48]
3|generalCollectionThreeDigits(0)|[805]
3|generalCollectionThreeDigits(1)|[805]
3|generalCollectionThreeDigits(8)|[805, 109, 817, 237, 651, 898, 382, 420]
3|generalCollectionFourDigits(0)|[8605]
3|generalCollectionFourDigits(1)|[8605]
3|generalCollectionFourDigits(8)|[8605, 4093, 4713, 8551, 2981, 8282, 7725, 1652]
3|generateSequenceExtractionRace(0)|[55]
3|generateSequenceExtractionRace(1)|[55]
3|generateSequenceExtractionRace(8)|[55, -8, -1, -2, -5, -8, -4, -3]
3|generateSequenceExtractionRaceHard(0)|[553]
3|generateSequenceExtractionRaceHard(1)|[553]
3|generateSequenceExtractionRaceHard(8)|[553, -1, -2, -5, -8, -4, -3, -2]
3|generateSequenceExtractionRaceTwoDigits(0)|[553]
3|generateSequenceExtractionRaceTwoDigits(1)|[553]
3|generateSequenceExtractionRaceTwoDigits(8)|[553, -21, -85, -34, -42, -64, -42, -38]
3|generateSequenceExtractionRaceTwoDigitsHard(0)|[5536]
3|generateSequenceExtractionRaceTwoDigitsHard(1)|[5536]
3|generateSequenceExtractionRaceTwoDigitsHard(8)|[5536, -52, -48, -23, -44, -26, -84, -93]
3|generateSequenceExtractionRaceThreeDigits(0)|[5536]
3|generateSequenceExtractionRaceThreeDigits(1)|[5536]
3|generateSequenceExtractionRaceThreeDigits(8)|[5536, -852, -234, -644, -842, -393, -452, -186]
3|generateSequenceExtractionRaceThreeDigitsHard(0)|[55360]
3|generateSequenceExtractionRaceThreeDigitsHard(1)|[55360]
3|generateSequenceExtractionRaceThreeDigitsHard(8)|[55360, -485, -423, -264, -384, -239, -645, -118]
3|generateSequenceExtractionRaceFourDigits(0)|[55360]
3|generateSequenceExtractionRaceFourDigits(1)|[55360]
3|generateSequenceExtractionRaceFourDigits(8)|[55360, -3485, -6442, -3842, -5239, -1864, -4771, -7166]
3|G.generateRandomNumber(-1)|0
3|G.generateRandomNumber(0)|0
3|G.generateRandomNumber(1)|1
3|G.generateRandomNumber(2)|10
3|G.generateRandomNumber(3)|100
3|G.generateRandomNumber(5)|17200
3|G.randomNumberChangeToString(1)|5
3|G.randomNumberChangeToString(2)|44
3|G.randomNumberChangeToString(4)|5534
3|G.randomUniqueNumberStrings(1,5)|[5, 4, 2, 3, 8]
3|G.randomUniqueNumberStrings(1,9)|[5, 4, 2, 3, 8, 7, 9, 6, 1]
3|G.randomUniqueNumberStrings(1,12)|[2, 4, 9, 1, 4, 2, 6, 8, 3, 7, 5, 3]
3|G.randomUniqueNumberStrings(2,4)|[44, 11, 48, 63]
3|G.randomUniqueNumberStrings(1,0)|[]
3|G.randomUniqueNumberStrings(0,3)|EXC:NoSuchElement
3|G.randomUniqueNumberStrings(3,6)|[478, 119, 858, 718, 988, 385]
"""

val ALTIN_VERI: List<String> = listOf(PARCA_0, PARCA_1, PARCA_2, PARCA_3, PARCA_4, PARCA_5, PARCA_6, PARCA_7, PARCA_8).flatMap { it.lines() }.filter { it.isNotBlank() }
