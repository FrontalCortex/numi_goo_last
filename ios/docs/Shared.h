#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SharedAbacusBead, SharedAbacusBeadController, SharedAbacusBeadMetrics, SharedAbacusBeadMetricsMoveDistancesPx, SharedAbacusBeadMetricsRodMeasurement, SharedAbacusBeadRenderer, SharedAbacusBeadRendererBeadRenderKind, SharedAbacusBeadRendererSoroban2Geometry, SharedAbacusCupRepository, SharedAbacusCustomizationFirestore, SharedAbacusCustomizationRules, SharedAbacusCustomizationRulesPlaceResult, SharedAbacusCustomizationSnapshot, SharedAbacusCustomizationUiLogic, SharedAbacusCustomizationUiLogicBeadCard, SharedAbacusCustomizationUiLogicBeadCardState, SharedAbacusCustomizationUiLogicBeadPaint, SharedAbacusCustomizationUiLogicColorRow, SharedAbacusCustomizationUiLogicFrameCard, SharedAbacusCustomizationUiLogicFrameCardState, SharedAbacusCustomizationUiLogicFrameLayer, SharedAbacusCustomizationUiLogicFramePaint, SharedAbacusCustomizationUiLogicPillGeometry, SharedAbacusCustomizationUiLogicPlaceOutcome, SharedAbacusCustomizationUiLogicPurchaseCheck, SharedAbacusCustomizationUiLogicPurchasePanelRules, SharedAbacusCustomizationUiLogicSlotBead, SharedAbacusCustomizationUiLogicSlotPanelState, SharedAbacusFrameRenderer, SharedAbacusFrameRendererRoundRectLayer, SharedAbacusLessonOutcome, SharedAbacusLessonRules, SharedAbacusLessonSessionControlResult, SharedAbacusLessonSessionControlResultAnswered, SharedAbacusLessonSessionControlResultIgnored, SharedAbacusLessonSessionControlResultOffline, SharedAbacusOperation, SharedAbacusPracticeLogic, SharedAbacusPracticeLogicKontrolResult, SharedAbacusPreferences, SharedAbacusPreferencesBeadColorSpec, SharedAbacusPreferencesBeadType, SharedAbacusPreferencesFrameType, SharedAbacusPreviewMetrics, SharedAbacusSoundPlayer, SharedAbacusTouchAction, SharedAbacusUiSettings, SharedAbakusBoncukKoprusu, SharedAbakusDersAkisi, SharedAbakusDersBitisi, SharedAbakusDersGirdisi, SharedAbakusDersKoprusu, SharedAbakusKontrolSonucu, SharedAbakusKontrolSonucuCompanion, SharedAbakusSoruListesi, SharedAccountDeletionRules, SharedAccountSettingsRules, SharedAdRules, SharedAdRulesAdSkipPolicyCompanion, SharedAgeConsentManager, SharedAgeConsentManagerAdRequestTags, SharedAgeConsentManagerAgeTreatment, SharedAnimatedQuest, SharedAppRatingManagerCompanion, SharedAuthRules, SharedAuthRulesGoogleLoginFailureAction, SharedAuthRulesPendingRegistration, SharedAuthRulesStudentEmailFlow, SharedBadgeAnimMode, SharedBadgeDetailLogic, SharedBadgeDetailLogicBadgeDetailUiState, SharedBadgeDetailLogicBadgeRenderConfig, SharedBadgeFragmentLogic, SharedBadgeFragmentLogicCelebrationQueueItem, SharedBadgeFragmentLogicLevelUpAnimConfig, SharedBadgeFragmentLogicLevelUpCelebrationPlan, SharedBadgeFragmentLogicPieceCountBadge, SharedBadgeFragmentLogicProgressUi, SharedBadgeIncrementTransactionResult, SharedBadgeKind, SharedBadgeKindCompanion, SharedBadgeLevelTone, SharedBadgeLevelUpPayload, SharedBadgePieceLeaderboardSync, SharedBadgePrecalcHelper, SharedBadgeProgressEngine, SharedBadgeProgressFirestore, SharedBadgeProgressRepository, SharedBadgeProgressWindow, SharedBadgeState, SharedBeadAnimationCompanion, SharedBeadAnimationPlan, SharedBeadData, SharedBeadMoveDistances, SharedBeadPurchaseFirestore, SharedBeadsBehavior, SharedBellekIciGunlukSoruDeposu, SharedBillingCatalog, SharedBlindingAdditionCupRepository, SharedBlindingExtractionCupRepository, SharedBlindingImpactCupRepository, SharedBlindingLessonFlags, SharedBlindingLessonOutcome, SharedBlindingLessonRules, SharedBlindingLessonRulesResultFalseAction, SharedBlindingOperation, SharedBlindingOperationMath, SharedBlindingOperationOther, SharedBlindingOperationSequence, SharedBlindingSequencePlaybackStep, SharedBlindingSequencePlaybackTimer, SharedBlindingWrongAnswerNext, SharedChestCrystalPolicy, SharedChestFragmentLogic, SharedChestFragmentLogicCupIconChange, SharedChestFragmentLogicMapProgressResult, SharedChestFragmentLogicPendingLessonProgressAnimation, SharedChestFragmentLogicSuccessRateEvent, SharedChestFragmentLogicSuccessRateEventItemFirstFinish, SharedChestFragmentLogicSuccessRateEventPass, SharedChestIcons, SharedChestRarity, SharedChestRarityCompanion, SharedChestResultLogic, SharedChestResultLogicReplayResult, SharedChestResultLogic_, SharedChestRewardDisplay, SharedChestRewardOutcome, SharedChestRewardType, SharedChestStepIndicator, SharedChestTapAction, SharedChestTapResult, SharedChestTypeProgressHelper, SharedChestTypeProgressHelper_, SharedCrystalBreakLogic, SharedCrystalRarityBehavior, SharedCrystalRarityTier, SharedCrystalTapAction, SharedCrystalTapResult, SharedCrystalTickResult, SharedCupBadgeSyncResult, SharedCupDeltaPrecalc, SharedCupDeltaTarget, SharedCupFetchResult, SharedCupFragmentLogic, SharedCupHistoryLogic, SharedCupHistoryLogicRangeTab, SharedCupHistoryRepository, SharedCupIconChange, SharedCupLessonLaunch, SharedCupMathOperation, SharedCupPathCard, SharedCupPathKind, SharedCupPathLogic, SharedCupPathRewardRepository, SharedCupPathRewardRepositoryCupPathState, SharedCupPathRewardRepositoryMilestone, SharedCupPathRewardRepositoryMilestoneStatus, SharedCupPieceRow, SharedCupRuleEngine, SharedCupRuleEngineBlindingMultiplicationConfig, SharedCupRuleEngineCupLessonItemOverride, SharedCupRuleEngineCupRange, SharedCupRuleEngineDigitConfig, SharedCupRuleEngineMultiplicationConfig, SharedCupScoreRepositorySpec, SharedCupScoreService, SharedCupScoreServiceCupScoreResult, SharedCupTier, SharedCupUpdateResult, SharedDailyQuestionBrokenHeartAction, SharedDailyQuestionBrokenHeartStore, SharedDailyQuestionBrokenHeartStoreCompanion, SharedDailyQuestionCardClickAction, SharedDailyQuestionCardLogic, SharedDailyQuestionCardProgressAnimStore, SharedDailyQuestionCardProgressAnimStoreCompanion, SharedDailyQuestionCardUiState, SharedDailyQuestionChallenge, SharedDailyQuestionChallengeClickAction, SharedDailyQuestionPeriod, SharedDailyQuestionPoolBuilder, SharedDailyQuestionPoolBuilderCompanion, SharedDailyQuestionProgressPlan, SharedDailyQuestionRepository, SharedDailyQuestionSlot, SharedDailyQuestionSource, SharedEditProfileRules, SharedEnergyManagerCompanion, SharedExtractionCupRepository, SharedFcmRules, SharedFcmRulesIncoming, SharedFeedbackGate, SharedFeedbackRules, SharedFirstTutorialShownStoreCompanion, SharedFirstTutorialShownStoreResolution, SharedFishingStreakPeriod, SharedFollowRules, SharedFollowRulesFriendSearchResult, SharedFrameData, SharedFramePurchaseFirestore, SharedFriendSearchRules, SharedGlobalLessonData, SharedGlobalLessonDataChestLessonRef, SharedGlobalLessonDataFirestoreLoadStatus, SharedGlobalLessonDataItemWrite, SharedGlobalLessonDataLeaderboardSubmit, SharedGlobalLessonDataLegacyMigrationPlan, SharedGlobalValues, SharedGorevEkraniDurumu, SharedGuidePanelData, SharedGunlukSoruBaslatma, SharedGunlukSoruCubukGorunumu, SharedGunlukSoruCubukModu, SharedImpactCupRepository, SharedJavaRandom, SharedKorDersAdimi, SharedKorDersAdimiCompanion, SharedKorDersOturumu, SharedKotlinArray<T>, SharedKotlinByteArray, SharedKotlinByteIterator, SharedKotlinEnum<E>, SharedKotlinEnumCompanion, SharedKotlinException, SharedKotlinFloatArray, SharedKotlinFloatIterator, SharedKotlinIntArray, SharedKotlinIntIterator, SharedKotlinNothing, SharedKotlinPair<__covariant A, __covariant B>, SharedKotlinRandom, SharedKotlinRandomDefault, SharedKotlinThrowable, SharedKotlinTriple<__covariant A, __covariant B, __covariant C>, SharedKotlinUnit, SharedKotlinx_datetimeDayOfWeek, SharedKotlinx_datetimeDayOfWeekNames, SharedKotlinx_datetimeDayOfWeekNamesCompanion, SharedKotlinx_datetimeFixedOffsetTimeZone, SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion, SharedKotlinx_datetimeInstant, SharedKotlinx_datetimeInstantCompanion, SharedKotlinx_datetimeLocalDate, SharedKotlinx_datetimeLocalDateCompanion, SharedKotlinx_datetimeLocalDateTime, SharedKotlinx_datetimeLocalDateTimeCompanion, SharedKotlinx_datetimeLocalTime, SharedKotlinx_datetimeLocalTimeCompanion, SharedKotlinx_datetimeMonth, SharedKotlinx_datetimeMonthNames, SharedKotlinx_datetimeMonthNamesCompanion, SharedKotlinx_datetimePadding, SharedKotlinx_datetimeTimeZone, SharedKotlinx_datetimeTimeZoneCompanion, SharedKotlinx_datetimeUtcOffset, SharedKotlinx_datetimeUtcOffsetCompanion, SharedKotlinx_serialization_coreSerialKind, SharedKotlinx_serialization_coreSerializersModule, SharedKupaEkranKoprusu, SharedKupaGecmisNoktasi, SharedKupaGrafikNoktasi, SharedKupaGrafikVerisi, SharedLessonAdapterLogic, SharedLessonAdapterLogicActionButtonDecision, SharedLessonAdapterLogicCardState, SharedLessonAdapterLogicChestCardStars, SharedLessonAdapterLogicChestStarSlot, SharedLessonAdapterLogicChestStarsRow, SharedLessonAdapterLogicLessonBottomSheetState, SharedLessonAdapterLogicLessonStartGlobals, SharedLessonAdapterLogicLessonStartTarget, SharedLessonAdapterLogicPanelState, SharedLessonAdapterLogicProgressAnimationAction, SharedLessonAdapterLogicProgressAnimationDecision, SharedLessonAdapterLogicRaceLessonBottomSheetState, SharedLessonAdapterLogicRaceRowStatus, SharedLessonAdapterLogicStepSegments, SharedLessonDataManager, SharedLessonEndMascotDraw, SharedLessonEndMascotLogic, SharedLessonItem, SharedLessonItemCompanion, SharedLessonLeaderboardRepository, SharedLessonLeaderboardRepositoryLeaderboardEntry, SharedLessonManager, SharedLessonOperations, SharedLessonProgressMerge, SharedLessonResultFalseLogic, SharedLessonResultFalseLogicFailRecord, SharedLessonResultLogic, SharedLessonResultLogicClaimBranch, SharedLessonResultLogicClaimDecision, SharedLessonSuccessRateRepository, SharedLessonSuccessRateRepositoryStateRefs, SharedLessonSuccessRateRepositoryStepWrite, SharedLiderHamKayit, SharedLiderSatirVerisi, SharedLoginFlowTexts, SharedMapFragmentLogic, SharedMapFragmentLogicStickyHeader, SharedMapLayoutLogic, SharedMarathonGuideLogic, SharedMarathonGuideStoreCompanion, SharedMatematikDailyQuestionGenerators, SharedMathOperation, SharedMathOperationGenerator, SharedMathOperation_, SharedMedalPieceRow, SharedMessageReport, SharedMessageReportCompanion, SharedMissionCountdownLabel, SharedMissionCounterType, SharedMissionProgressOverlay, SharedMissionQuestCatalog, SharedMissionQuestData, SharedMissionQuestDefinition, SharedMissionQuestState, SharedMissionUiLogic, SharedMissionWindow, SharedMissionsProgressStore, SharedMissionsProgressStoreCompanion, SharedMissionsProgressStoreSnapshot, SharedNewChestLogic, SharedNewChestLogicCloseRoute, SharedOtpRules, SharedOtpRulesWrongAttemptResult, SharedOyunLessonItemCompanion, SharedOyunLessonItemData, SharedPartSelectionLogic, SharedPartState, SharedPendingUploadMeta, SharedPlanStatus, SharedProfileBadgeRenderConfig, SharedProfileBadgeSlotUi, SharedProfileCupCard, SharedProfileFragmentLogic, SharedQuestionMessage, SharedQuestionMessageCompanion, SharedRarityTheme, SharedRastgeleKaynagi, SharedRatingPromptDecision, SharedRecordLogic, SharedRecordLogicLeaderboardSections, SharedRecordLogicRowColors, SharedRecordLogicSeasonCountdown, SharedRecordTimeUtils, SharedRestrictedUser, SharedRestrictedUserCompanion, SharedRestrictedUserRestrictionUpdate, SharedRozetEkranVerisi, SharedRozetKutuVerisi, SharedRozetParcaVerisi, SharedScoreBarColorBlend, SharedSeasonClock, SharedSeasonLeaderboardRewardPayloads, SharedServerEnergy, SharedServerEnergyBuyWithKeysResult, SharedServerRewards, SharedServerRewardsChestOutcome, SharedServerRewardsCrystalOutcome, SharedSessionDeviceRules, SharedSessionDeviceRulesDecision, SharedSezonVerisi, SharedShopFragmentLogic, SharedShopFragmentLogicEnergySectionState, SharedShopFragmentLogicPackCard, SharedShopFragmentLogicProductKind, SharedSignupFlowRules, SharedSignupFlowRulesStep, SharedSonucEkraniKoprusu, SharedSplashRules, SharedSplashRulesRoute, SharedStudentQuestion, SharedStudentQuestionCompanion, SharedSurum, SharedTeacherLoginRules, SharedTimeTrackerCompanion, SharedTimeTrackerCompanionSyncWrite, SharedTrustedClockCompanion, SharedTrustedClockRules, SharedTrustedClockRulesAnchor, SharedUploadLessonDataLogic, SharedUploadLessonDataLogicYuklenenDers<T>, SharedUserAskQuestionRestriction, SharedUserBadgeProgress, SharedUserIdRules, SharedUserInfoRules, SharedUserWallet, SharedUserWalletFirestore, SharedWalletLoadResult, SharedWalletReason;

@protocol SharedAbacusBeadHost, SharedAbacusPrefsStore, SharedDailyQuestionChallengeCache, SharedDailyQuestionGenerators, SharedEnergyPlatform, SharedKotlinAnnotation, SharedKotlinAppendable, SharedKotlinComparable, SharedKotlinIterator, SharedKotlinKAnnotatedElement, SharedKotlinKClass, SharedKotlinKClassifier, SharedKotlinKDeclarationContainer, SharedKotlinx_datetimeDateTimeFormat, SharedKotlinx_datetimeDateTimeFormatBuilder, SharedKotlinx_datetimeDateTimeFormatBuilderWithDate, SharedKotlinx_datetimeDateTimeFormatBuilderWithDateTime, SharedKotlinx_datetimeDateTimeFormatBuilderWithTime, SharedKotlinx_datetimeDateTimeFormatBuilderWithUtcOffset, SharedKotlinx_serialization_coreCompositeDecoder, SharedKotlinx_serialization_coreCompositeEncoder, SharedKotlinx_serialization_coreDecoder, SharedKotlinx_serialization_coreDeserializationStrategy, SharedKotlinx_serialization_coreEncoder, SharedKotlinx_serialization_coreKSerializer, SharedKotlinx_serialization_coreSerialDescriptor, SharedKotlinx_serialization_coreSerializationStrategy, SharedKotlinx_serialization_coreSerializersModuleCollector, SharedLessonManagerLessonAdapterBridge, SharedLessonManagerRaceAdapterBridge, SharedLessonProgressSink, SharedMissionCloudGateway, SharedOyunKeyValueStore, SharedOyunLessonItem, SharedServerRewardsChestCaller, SharedTimeTrackerPlatform;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface SharedBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SharedBase (SharedBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SharedMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SharedMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSharedKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SharedNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface SharedByte : SharedNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SharedUByte : SharedNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SharedShort : SharedNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SharedUShort : SharedNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SharedInt : SharedNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SharedUInt : SharedNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SharedLong : SharedNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SharedULong : SharedNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SharedFloat : SharedNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SharedDouble : SharedNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SharedBoolean : SharedNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Surum")))
@interface SharedSurum : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)surum __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSurum *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t ASAMA __attribute__((swift_name("ASAMA")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBead")))
@interface SharedAbacusBead : SharedBase
- (instancetype)initWithRod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex __attribute__((swift_name("init(rod:isTop:beadIndex:)"))) __attribute__((objc_designated_initializer));
@property (readonly) int32_t beadIndex __attribute__((swift_name("beadIndex")));
@property int32_t bottomMargin __attribute__((swift_name("bottomMargin")));
@property (readonly) NSString *idName __attribute__((swift_name("idName")));
@property (readonly) BOOL isTop __attribute__((swift_name("isTop")));
@property void (^ _Nullable onChanged)(SharedAbacusBead *) __attribute__((swift_name("onChanged")));
@property (readonly) int32_t rod __attribute__((swift_name("rod")));
@property float translationY __attribute__((swift_name("translationY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadController")))
@interface SharedAbacusBeadController : SharedBase
- (instancetype)initWithHost:(id<SharedAbacusBeadHost>)host touchSlopPx:(int32_t)touchSlopPx bottomStepPx:(float)bottomStepPx topStepPx:(float)topStepPx animationDurationMs:(int64_t)animationDurationMs __attribute__((swift_name("init(host:touchSlopPx:bottomStepPx:topStepPx:animationDurationMs:)"))) __attribute__((objc_designated_initializer));
- (void)animateGuideBeadBeadId:(NSString *)beadId __attribute__((swift_name("animateGuideBead(beadId:)")));
- (void)applyInternalStateFromValueValue:(int32_t)value applyAppearance:(BOOL)applyAppearance __attribute__((swift_name("applyInternalStateFromValue(value:applyAppearance:)")));
- (int64_t)computeDurationMsVelocityAbsPxPerMs:(float)velocityAbsPxPerMs moveDistancePx:(float)moveDistancePx __attribute__((swift_name("computeDurationMs(velocityAbsPxPerMs:moveDistancePx:)")));
- (BOOL)computeMovementDistancesFromLayoutDynamic:(SharedAbacusBeadMetricsMoveDistancesPx * _Nullable)dynamic force:(BOOL)force __attribute__((swift_name("computeMovementDistancesFromLayout(dynamic:force:)")));
- (int32_t)countConsecutiveRaisedBottomBeadsRod:(int32_t)rod __attribute__((swift_name("countConsecutiveRaisedBottomBeads(rod:)")));
- (void)forceRecaptureInitialPositions __attribute__((swift_name("forceRecaptureInitialPositions()")));
- (int32_t)getBottomCountRod:(int32_t)rod __attribute__((swift_name("getBottomCount(rod:)")));
- (float)getBottomMoveDistancePx __attribute__((swift_name("getBottomMoveDistancePx()")));
- (int32_t)getCurrentValue __attribute__((swift_name("getCurrentValue()")));
- (float)getTopMoveDistancePx __attribute__((swift_name("getTopMoveDistancePx()")));
- (BOOL)isBeadAnimationInProgress __attribute__((swift_name("isBeadAnimationInProgress()")));
- (BOOL)isBottomBeadPhysicallyRaisedRod:(int32_t)rod beadIndex:(int32_t)beadIndex __attribute__((swift_name("isBottomBeadPhysicallyRaised(rod:beadIndex:)")));
- (BOOL)isResetInProgress __attribute__((swift_name("isResetInProgress()")));
- (BOOL)isTopBeadDownRod:(int32_t)rod __attribute__((swift_name("isTopBeadDown(rod:)")));
- (BOOL)isTopDownLogicalRod:(int32_t)rod __attribute__((swift_name("isTopDownLogical(rod:)")));
- (BOOL)onBottomBeadTouchRod:(int32_t)rod beadNumber:(int32_t)beadNumber action:(SharedAbacusTouchAction *)action rawY:(float)rawY eventTime:(int64_t)eventTime __attribute__((swift_name("onBottomBeadTouch(rod:beadNumber:action:rawY:eventTime:)")));
- (BOOL)onTopBeadTouchRod:(int32_t)rod action:(SharedAbacusTouchAction *)action rawY:(float)rawY eventTime:(int64_t)eventTime __attribute__((swift_name("onTopBeadTouch(rod:action:rawY:eventTime:)")));
- (void)refreshAll __attribute__((swift_name("refreshAll()")));
- (void)resetAnimate:(BOOL)animate __attribute__((swift_name("reset(animate:)")));
- (void)setBeadMarginOffsetsBottomOffsetPx:(float)bottomOffsetPx topOffsetPx:(float)topOffsetPx __attribute__((swift_name("setBeadMarginOffsets(bottomOffsetPx:topOffsetPx:)")));
- (void)setEnabledEnabled:(BOOL)enabled __attribute__((swift_name("setEnabled(enabled:)")));
- (void)setExternalBeadAnimationInProgressChecker:(SharedBoolean *(^)(void))checker __attribute__((swift_name("setExternalBeadAnimationInProgress(checker:)")));
- (void)setup __attribute__((swift_name("setup()")));
- (void)syncStateFromUiApplyAppearance:(BOOL)applyAppearance __attribute__((swift_name("syncStateFromUi(applyAppearance:)")));
@property (readonly) SharedKotlinArray<SharedKotlinArray<SharedAbacusBead *> *> *bottomBeads __attribute__((swift_name("bottomBeads")));
@property void (^ _Nullable onValueChanged)(void) __attribute__((swift_name("onValueChanged")));
@property (readonly) SharedKotlinArray<SharedAbacusBead *> *topBeads __attribute__((swift_name("topBeads")));
@end

__attribute__((swift_name("AbacusBeadHost")))
@protocol SharedAbacusBeadHost
@required
- (void)animateTranslationYBead:(SharedAbacusBead *)bead targetTranslationY:(float)targetTranslationY durationMs:(int64_t)durationMs endAction:(void (^)(void))endAction __attribute__((swift_name("animateTranslationY(bead:targetTranslationY:durationMs:endAction:)")));
- (void)cancelAnimationBead:(SharedAbacusBead *)bead __attribute__((swift_name("cancelAnimation(bead:)")));
- (void)playBeadClickActivating:(BOOL)activating __attribute__((swift_name("playBeadClick(activating:)")));
- (void)postDelayedDelayMs:(int64_t)delayMs action:(void (^)(void))action __attribute__((swift_name("postDelayed(delayMs:action:)")));
- (void)updateBeadAppearanceBead:(SharedAbacusBead *)bead isSelected:(BOOL)isSelected __attribute__((swift_name("updateBeadAppearance(bead:isSelected:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadMetrics")))
@interface SharedAbacusBeadMetrics : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusBeadMetrics __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusBeadMetrics *shared __attribute__((swift_name("shared")));
- (float)bottomStepPxDensity:(float)density __attribute__((swift_name("bottomStepPx(density:)")));
- (int32_t)bottomStepPxIntDensity:(float)density __attribute__((swift_name("bottomStepPxInt(density:)")));
- (SharedAbacusBeadMetricsMoveDistancesPx * _Nullable)fromBarrierDistancesRods:(NSArray<SharedAbacusBeadMetricsRodMeasurement *> *)rods ratio:(float)ratio __attribute__((swift_name("fromBarrierDistances(rods:ratio:)")));
- (float)topStepPxDensity:(float)density __attribute__((swift_name("topStepPx(density:)")));
- (int32_t)topStepPxIntDensity:(float)density __attribute__((swift_name("topStepPxInt(density:)")));
@property (readonly) float BOTTOM_STEP_DP __attribute__((swift_name("BOTTOM_STEP_DP")));
@property (readonly) float TOP_STEP_DP __attribute__((swift_name("TOP_STEP_DP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadMetrics.MoveDistancesPx")))
@interface SharedAbacusBeadMetricsMoveDistancesPx : SharedBase
- (instancetype)initWithBottomPx:(float)bottomPx topPx:(float)topPx __attribute__((swift_name("init(bottomPx:topPx:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusBeadMetricsMoveDistancesPx *)doCopyBottomPx:(float)bottomPx topPx:(float)topPx __attribute__((swift_name("doCopy(bottomPx:topPx:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float bottomPx __attribute__((swift_name("bottomPx")));
@property (readonly) float topPx __attribute__((swift_name("topPx")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadMetrics.RodMeasurement")))
@interface SharedAbacusBeadMetricsRodMeasurement : SharedBase
- (instancetype)initWithTopBeadY:(int32_t)topBeadY topBeadHeight:(int32_t)topBeadHeight bottom1Y:(int32_t)bottom1Y barrierY:(int32_t)barrierY barrierHeight:(int32_t)barrierHeight __attribute__((swift_name("init(topBeadY:topBeadHeight:bottom1Y:barrierY:barrierHeight:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusBeadMetricsRodMeasurement *)doCopyTopBeadY:(int32_t)topBeadY topBeadHeight:(int32_t)topBeadHeight bottom1Y:(int32_t)bottom1Y barrierY:(int32_t)barrierY barrierHeight:(int32_t)barrierHeight __attribute__((swift_name("doCopy(topBeadY:topBeadHeight:bottom1Y:barrierY:barrierHeight:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t barrierHeight __attribute__((swift_name("barrierHeight")));
@property (readonly) int32_t barrierY __attribute__((swift_name("barrierY")));
@property (readonly) int32_t bottom1Y __attribute__((swift_name("bottom1Y")));
@property (readonly) int32_t topBeadHeight __attribute__((swift_name("topBeadHeight")));
@property (readonly) int32_t topBeadY __attribute__((swift_name("topBeadY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadRenderer")))
@interface SharedAbacusBeadRenderer : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusBeadRenderer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusBeadRenderer *shared __attribute__((swift_name("shared")));
- (int32_t)HSVToColorHsv:(SharedKotlinFloatArray *)hsv __attribute__((swift_name("HSVToColor(hsv:)")));
- (NSString * _Nullable)beadResNameType:(SharedAbacusPreferencesBeadType *)type selected:(BOOL)selected __attribute__((swift_name("beadResName(type:selected:)")));
- (int32_t)bitmapCacheKeySeed:(int32_t)seed newColors:(SharedKotlinIntArray *)newColors __attribute__((swift_name("bitmapCacheKey(seed:newColors:)")));
- (void)colorToHSVColor:(int32_t)color hsv:(SharedKotlinFloatArray *)hsv __attribute__((swift_name("colorToHSV(color:hsv:)")));
- (int32_t)getHueColor:(int32_t)color __attribute__((swift_name("getHue(color:)")));
- (int32_t)getLightness100Color:(int32_t)color __attribute__((swift_name("getLightness100(color:)")));
- (int32_t)makeColorHueDegrees:(int32_t)hueDegrees lightness100:(int32_t)lightness100 __attribute__((swift_name("makeColor(hueDegrees:lightness100:)")));
- (NSDictionary<SharedInt *, SharedInt *> * _Nullable)originalColorMapType:(SharedAbacusPreferencesBeadType *)type selected:(BOOL)selected __attribute__((swift_name("originalColorMap(type:selected:)")));
- (SharedAbacusBeadRendererBeadRenderKind *)renderKindType:(SharedAbacusPreferencesBeadType *)type __attribute__((swift_name("renderKind(type:)")));
- (void)replaceBeadPixelsPixels:(SharedKotlinIntArray *)pixels targetW:(int32_t)targetW targetH:(int32_t)targetH originalMap:(NSDictionary<SharedInt *, SharedInt *> *)originalMap newColors:(SharedKotlinIntArray *)newColors __attribute__((swift_name("replaceBeadPixels(pixels:targetW:targetH:originalMap:newColors:)")));
- (int32_t)replacedBitmapTargetHeightTargetW:(int32_t)targetW intrinsicWidth:(int32_t)intrinsicWidth intrinsicHeight:(int32_t)intrinsicHeight __attribute__((swift_name("replacedBitmapTargetHeight(targetW:intrinsicWidth:intrinsicHeight:)")));
- (int32_t)replacedBitmapTargetWidthDensity:(float)density __attribute__((swift_name("replacedBitmapTargetWidth(density:)")));
- (int32_t)shiftHueBaseColor:(int32_t)baseColor hueDegrees:(int32_t)hueDegrees minSaturation:(float)minSaturation __attribute__((swift_name("shiftHue(baseColor:hueDegrees:minSaturation:)")));
- (int32_t)soroban2CacheSeedSelected:(BOOL)selected __attribute__((swift_name("soroban2CacheSeed(selected:)")));
- (SharedAbacusBeadRendererSoroban2Geometry *)soroban2GeometryDensity:(float)density __attribute__((swift_name("soroban2Geometry(density:)")));
- (SharedKotlinArray<NSString *> *)sorobanLayerResNamesSelected:(BOOL)selected __attribute__((swift_name("sorobanLayerResNames(selected:)")));
- (NSSet<SharedAbacusPreferencesBeadType *> *)usedBeadTypesStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid __attribute__((swift_name("usedBeadTypes(store:uid:)")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL2_ORIGINAL_MAP __attribute__((swift_name("ANIMAL2_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL2_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL2_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL3_ORIGINAL_MAP __attribute__((swift_name("ANIMAL3_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL3_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL3_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL4_ORIGINAL_MAP __attribute__((swift_name("ANIMAL4_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL4_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL4_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL5_ORIGINAL_MAP __attribute__((swift_name("ANIMAL5_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL5_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL5_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL6_ORIGINAL_MAP __attribute__((swift_name("ANIMAL6_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL6_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL6_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL7_ORIGINAL_MAP __attribute__((swift_name("ANIMAL7_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL7_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL7_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL8_ORIGINAL_MAP __attribute__((swift_name("ANIMAL8_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL8_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL8_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL9_ORIGINAL_MAP __attribute__((swift_name("ANIMAL9_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL9_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL9_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *ANIMAL_SEL_ORIGINAL_MAP __attribute__((swift_name("ANIMAL_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL10_ORIGINAL_MAP __attribute__((swift_name("BALL10_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL10_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL10_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL11_ORIGINAL_MAP __attribute__((swift_name("BALL11_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL11_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL11_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL12_ORIGINAL_MAP __attribute__((swift_name("BALL12_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL12_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL12_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL13_ORIGINAL_MAP __attribute__((swift_name("BALL13_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL13_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL13_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL14_ORIGINAL_MAP __attribute__((swift_name("BALL14_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL14_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL14_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL1_ORIGINAL_MAP __attribute__((swift_name("BALL1_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL1_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL1_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL3_ORIGINAL_MAP __attribute__((swift_name("BALL3_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL3_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL3_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL4_ORIGINAL_MAP __attribute__((swift_name("BALL4_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL4_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL4_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL5_ORIGINAL_MAP __attribute__((swift_name("BALL5_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL5_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL5_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL6_ORIGINAL_MAP __attribute__((swift_name("BALL6_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL6_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL6_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL7_ORIGINAL_MAP __attribute__((swift_name("BALL7_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL7_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL7_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL8_ORIGINAL_MAP __attribute__((swift_name("BALL8_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL8_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL8_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL9_ORIGINAL_MAP __attribute__((swift_name("BALL9_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BALL9_SEL_ORIGINAL_MAP __attribute__((swift_name("BALL9_SEL_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BOWLING_ORIGINAL_MAP __attribute__((swift_name("BOWLING_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *BOWLING_SEL_ORIGINAL_MAP __attribute__((swift_name("BOWLING_SEL_ORIGINAL_MAP")));
@property (readonly) int32_t COLOR_TOLERANCE __attribute__((swift_name("COLOR_TOLERANCE")));
@property (readonly) float FIXED_SATURATION __attribute__((swift_name("FIXED_SATURATION")));
@property (readonly) int32_t MAX_BITMAP_CACHE_ENTRIES __attribute__((swift_name("MAX_BITMAP_CACHE_ENTRIES")));
@property (readonly) int32_t RABBIT_RENDER_DP __attribute__((swift_name("RABBIT_RENDER_DP")));
@property (readonly) SharedKotlinFloatArray *SOROBAN2_GRADIENT_STOPS __attribute__((swift_name("SOROBAN2_GRADIENT_STOPS")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *SOROBAN6_ORIGINAL_MAP __attribute__((swift_name("SOROBAN6_ORIGINAL_MAP")));
@property (readonly) NSDictionary<SharedInt *, SharedInt *> *SOROBAN6_SEL_ORIGINAL_MAP __attribute__((swift_name("SOROBAN6_SEL_ORIGINAL_MAP")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SharedKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SharedKotlinEnum<E> : SharedBase <SharedKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadRenderer.BeadRenderKind")))
@interface SharedAbacusBeadRendererBeadRenderKind : SharedKotlinEnum<SharedAbacusBeadRendererBeadRenderKind *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAbacusBeadRendererBeadRenderKind *tintedLayers __attribute__((swift_name("tintedLayers")));
@property (class, readonly) SharedAbacusBeadRendererBeadRenderKind *replacedBitmap __attribute__((swift_name("replacedBitmap")));
@property (class, readonly) SharedAbacusBeadRendererBeadRenderKind *gradientPill __attribute__((swift_name("gradientPill")));
+ (SharedKotlinArray<SharedAbacusBeadRendererBeadRenderKind *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAbacusBeadRendererBeadRenderKind *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusBeadRenderer.Soroban2Geometry")))
@interface SharedAbacusBeadRendererSoroban2Geometry : SharedBase
- (instancetype)initWithTargetW:(int32_t)targetW targetH:(int32_t)targetH outerRx:(float)outerRx outerRy:(float)outerRy padX:(float)padX padY:(float)padY innerRx:(float)innerRx innerRy:(float)innerRy __attribute__((swift_name("init(targetW:targetH:outerRx:outerRy:padX:padY:innerRx:innerRy:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusBeadRendererSoroban2Geometry *)doCopyTargetW:(int32_t)targetW targetH:(int32_t)targetH outerRx:(float)outerRx outerRy:(float)outerRy padX:(float)padX padY:(float)padY innerRx:(float)innerRx innerRy:(float)innerRy __attribute__((swift_name("doCopy(targetW:targetH:outerRx:outerRy:padX:padY:innerRx:innerRy:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float innerRx __attribute__((swift_name("innerRx")));
@property (readonly) float innerRy __attribute__((swift_name("innerRy")));
@property (readonly) float outerRx __attribute__((swift_name("outerRx")));
@property (readonly) float outerRy __attribute__((swift_name("outerRy")));
@property (readonly) float padX __attribute__((swift_name("padX")));
@property (readonly) float padY __attribute__((swift_name("padY")));
@property (readonly) int32_t targetH __attribute__((swift_name("targetH")));
@property (readonly) int32_t targetW __attribute__((swift_name("targetW")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationFirestore")))
@interface SharedAbacusCustomizationFirestore : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusCustomizationFirestore __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusCustomizationFirestore *shared __attribute__((swift_name("shared")));
- (void)applySnapshotToLocalPreferencesStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid snapshot:(SharedAbacusCustomizationSnapshot *)snapshot __attribute__((swift_name("applySnapshotToLocalPreferences(store:uid:snapshot:)")));
- (SharedAbacusCustomizationSnapshot *)buildSnapshotFromLocalPreferencesStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid __attribute__((swift_name("buildSnapshotFromLocalPreferences(store:uid:)")));
- (NSDictionary<NSString *, id> *)customizationDataSnapshot:(SharedAbacusCustomizationSnapshot *)snapshot __attribute__((swift_name("customizationData(snapshot:)")));
- (NSString *)docPathUid:(NSString *)uid __attribute__((swift_name("docPath(uid:)")));
- (BOOL)isHydratedMarkerUid:(NSString * _Nullable)markerUid uid:(NSString *)uid __attribute__((swift_name("isHydrated(markerUid:uid:)")));
- (NSDictionary<NSString *, id> *)previewBeadTypeDataType:(SharedAbacusPreferencesBeadType *)type __attribute__((swift_name("previewBeadTypeData(type:)")));
- (SharedAbacusCustomizationSnapshot *)snapshotFromDocumentExists:(BOOL)exists data:(NSDictionary<NSString *, id> * _Nullable)data __attribute__((swift_name("snapshotFromDocument(exists:data:)")));
@property (readonly) NSString *COLLECTION_ABACUS_CUSTOMIZATION __attribute__((swift_name("COLLECTION_ABACUS_CUSTOMIZATION")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *DOC_STATE __attribute__((swift_name("DOC_STATE")));
@property (readonly) NSString *FIELD_BEAD_COLORS __attribute__((swift_name("FIELD_BEAD_COLORS")));
@property (readonly) NSString *FIELD_BEAD_TYPE __attribute__((swift_name("FIELD_BEAD_TYPE")));
@property (readonly) NSString *FIELD_FRAME_COLORS __attribute__((swift_name("FIELD_FRAME_COLORS")));
@property (readonly) NSString *FIELD_FRAME_TYPE __attribute__((swift_name("FIELD_FRAME_TYPE")));
@property (readonly) NSString *FIELD_PREVIEW_BEAD_TYPE __attribute__((swift_name("FIELD_PREVIEW_BEAD_TYPE")));
@property (readonly) NSString *FIELD_SLOTS __attribute__((swift_name("FIELD_SLOTS")));
@property (readonly) NSString *FIELD_UPDATED_AT __attribute__((swift_name("FIELD_UPDATED_AT")));
@property (readonly) NSString *HYDRATION_PREFS __attribute__((swift_name("HYDRATION_PREFS")));
@property (readonly) NSString *KEY_HYDRATED_UID __attribute__((swift_name("KEY_HYDRATED_UID")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationRules")))
@interface SharedAbacusCustomizationRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusCustomizationRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusCustomizationRules *shared __attribute__((swift_name("shared")));
- (int32_t)availableCountOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads store:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("availableCount(ownedBeads:store:uid:beadType:)")));
- (NSString *)beadImageResNameBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("beadImageResName(beadType:)")));
- (NSString *)colorFeatureCurrencyIconResNameBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("colorFeatureCurrencyIconResName(beadType:)")));
- (int32_t)countAfterPurchaseCurrentCount:(int32_t)currentCount __attribute__((swift_name("countAfterPurchase(currentCount:)")));
- (NSString *)currencyIconResNameBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("currencyIconResName(beadType:)")));
- (int32_t)getBeadColorFeaturePriceBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("getBeadColorFeaturePrice(beadType:)")));
- (int32_t)getBeadPriceBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("getBeadPrice(beadType:)")));
- (int32_t)getFramePriceFrameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("getFramePrice(frameType:)")));
- (BOOL)hasEnoughBeadType:(SharedAbacusPreferencesBeadType *)beadType cachedKeys:(int32_t)cachedKeys cachedCurrency:(int32_t)cachedCurrency __attribute__((swift_name("hasEnough(beadType:cachedKeys:cachedCurrency:)")));
- (BOOL)isBeadColorFeatureActiveOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("isBeadColorFeatureActive(ownedBeads:beadType:)")));
- (BOOL)isBeadOwnedOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("isBeadOwned(ownedBeads:beadType:)")));
- (BOOL)isFrameColorUnlockedOwnedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames currentFrameType:(SharedAbacusPreferencesFrameType *)currentFrameType __attribute__((swift_name("isFrameColorUnlocked(ownedFrames:currentFrameType:)")));
- (BOOL)isFrameSelectableOwnedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames frameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("isFrameSelectable(ownedFrames:frameType:)")));
- (BOOL)isKeyCurrencyBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("isKeyCurrency(beadType:)")));
- (BOOL)isStockMaxedOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("isStockMaxed(ownedBeads:beadType:)")));
- (NSString *)notEnoughMessageBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("notEnoughMessage(beadType:)")));
- (int32_t)ownedCountOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("ownedCount(ownedBeads:beadType:)")));
- (SharedAbacusCustomizationRulesPlaceResult *)placeBeadOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads store:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid beadType:(SharedAbacusPreferencesBeadType *)beadType rod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex __attribute__((swift_name("placeBead(ownedBeads:store:uid:beadType:rod:isTop:beadIndex:)")));
- (int32_t)placedCountStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("placedCount(store:uid:beadType:)")));
- (NSDictionary<NSString *, SharedBeadData *> *)withBeadColorFeatureOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("withBeadColorFeature(ownedBeads:beadType:)")));
- (NSDictionary<NSString *, SharedBeadData *> *)withBeadCountOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType newCount:(int32_t)newCount __attribute__((swift_name("withBeadCount(ownedBeads:beadType:newCount:)")));
- (NSDictionary<NSString *, SharedFrameData *> *)withFrameColorFeatureOwnedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames frameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("withFrameColorFeature(ownedFrames:frameType:)")));
- (NSDictionary<NSString *, SharedFrameData *> *)withFrameOwnedOwnedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames frameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("withFrameOwned(ownedFrames:frameType:)")));
@property (readonly) int32_t BEAD_PACK_SIZE __attribute__((swift_name("BEAD_PACK_SIZE")));
@property (readonly) int32_t COLOR_FEATURE_KEY_PRICE __attribute__((swift_name("COLOR_FEATURE_KEY_PRICE")));
@property (readonly) int32_t COLOR_FEATURE_PRICE __attribute__((swift_name("COLOR_FEATURE_PRICE")));
@property (readonly) NSString *ICON_GOLD __attribute__((swift_name("ICON_GOLD")));
@property (readonly) NSString *ICON_KEY __attribute__((swift_name("ICON_KEY")));
@property (readonly) int32_t MAX_BEAD_COUNT __attribute__((swift_name("MAX_BEAD_COUNT")));
@property (readonly) NSString *MESSAGE_ERROR __attribute__((swift_name("MESSAGE_ERROR")));
@property (readonly) NSString *MESSAGE_NOT_ENOUGH_GOLD __attribute__((swift_name("MESSAGE_NOT_ENOUGH_GOLD")));
@property (readonly) NSString *MESSAGE_NOT_ENOUGH_KEYS __attribute__((swift_name("MESSAGE_NOT_ENOUGH_KEYS")));
@property (readonly) NSString *MESSAGE_NO_BEADS_LEFT __attribute__((swift_name("MESSAGE_NO_BEADS_LEFT")));
@property (readonly) NSString *MESSAGE_OPERATION_FAILED __attribute__((swift_name("MESSAGE_OPERATION_FAILED")));
@property (readonly) NSString *MESSAGE_OUT_OF_STOCK __attribute__((swift_name("MESSAGE_OUT_OF_STOCK")));
@property (readonly) NSString *MESSAGE_SAVE_ERROR __attribute__((swift_name("MESSAGE_SAVE_ERROR")));
@property (readonly) NSString *MESSAGE_SAVE_ERROR_RETRY __attribute__((swift_name("MESSAGE_SAVE_ERROR_RETRY")));
@property (readonly) NSArray<SharedKotlinPair<SharedAbacusPreferencesBeadType *, NSString *> *> *PAID_BEAD_INFO __attribute__((swift_name("PAID_BEAD_INFO")));
@property (readonly) int32_t SELECTED_STROKE_COLOR __attribute__((swift_name("SELECTED_STROKE_COLOR")));
@property (readonly) int32_t SELECTED_STROKE_WIDTH __attribute__((swift_name("SELECTED_STROKE_WIDTH")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationRules.PlaceResult")))
@interface SharedAbacusCustomizationRulesPlaceResult : SharedKotlinEnum<SharedAbacusCustomizationRulesPlaceResult *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAbacusCustomizationRulesPlaceResult *sameTypeIgnored __attribute__((swift_name("sameTypeIgnored")));
@property (class, readonly) SharedAbacusCustomizationRulesPlaceResult *noBeadsLeft __attribute__((swift_name("noBeadsLeft")));
@property (class, readonly) SharedAbacusCustomizationRulesPlaceResult *placed __attribute__((swift_name("placed")));
+ (SharedKotlinArray<SharedAbacusCustomizationRulesPlaceResult *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAbacusCustomizationRulesPlaceResult *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationSnapshot")))
@interface SharedAbacusCustomizationSnapshot : SharedBase
- (instancetype)initWithBeadType:(SharedAbacusPreferencesBeadType *)beadType frameType:(SharedAbacusPreferencesFrameType *)frameType slots:(NSDictionary<NSString *, SharedAbacusPreferencesBeadType *> *)slots beadColors:(NSDictionary<NSString *, SharedKotlinIntArray *> *)beadColors frameColors:(NSDictionary<NSString *, SharedInt *> *)frameColors previewBeadType:(SharedAbacusPreferencesBeadType * _Nullable)previewBeadType __attribute__((swift_name("init(beadType:frameType:slots:beadColors:frameColors:previewBeadType:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusPreferencesBeadType *)beadTypeForSlotRod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex __attribute__((swift_name("beadTypeForSlot(rod:isTop:beadIndex:)")));
- (SharedKotlinIntArray * _Nullable)colorsForType:(SharedAbacusPreferencesBeadType *)type selected:(BOOL)selected __attribute__((swift_name("colorsFor(type:selected:)")));
- (SharedAbacusCustomizationSnapshot *)doCopyBeadType:(SharedAbacusPreferencesBeadType *)beadType frameType:(SharedAbacusPreferencesFrameType *)frameType slots:(NSDictionary<NSString *, SharedAbacusPreferencesBeadType *> *)slots beadColors:(NSDictionary<NSString *, SharedKotlinIntArray *> *)beadColors frameColors:(NSDictionary<NSString *, SharedInt *> *)frameColors previewBeadType:(SharedAbacusPreferencesBeadType * _Nullable)previewBeadType __attribute__((swift_name("doCopy(beadType:frameType:slots:beadColors:frameColors:previewBeadType:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (SharedKotlinIntArray * _Nullable)frameColorArray __attribute__((swift_name("frameColorArray()")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, SharedKotlinIntArray *> *beadColors __attribute__((swift_name("beadColors")));
@property (readonly) SharedAbacusPreferencesBeadType *beadType __attribute__((swift_name("beadType")));
@property (readonly) NSDictionary<NSString *, SharedInt *> *frameColors __attribute__((swift_name("frameColors")));
@property (readonly) SharedAbacusPreferencesFrameType *frameType __attribute__((swift_name("frameType")));
@property (readonly) SharedAbacusPreferencesBeadType * _Nullable previewBeadType __attribute__((swift_name("previewBeadType")));
@property (readonly) NSDictionary<NSString *, SharedAbacusPreferencesBeadType *> *slots __attribute__((swift_name("slots")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic")))
@interface SharedAbacusCustomizationUiLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusCustomizationUiLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusCustomizationUiLogic *shared __attribute__((swift_name("shared")));
- (NSArray<SharedAbacusCustomizationUiLogicBeadCardState *> *)beadCardStatesStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid ownedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads __attribute__((swift_name("beadCardStates(store:uid:ownedBeads:)")));
- (NSArray<SharedAbacusCustomizationUiLogicColorRow *> *)beadColorRowsStore:(id<SharedAbacusPrefsStore>)store beadType:(SharedAbacusPreferencesBeadType *)beadType selected:(BOOL)selected __attribute__((swift_name("beadColorRows(store:beadType:selected:)")));
- (int32_t)beadCountAfterPurchaseOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType fromSlotPanel:(BOOL)fromSlotPanel __attribute__((swift_name("beadCountAfterPurchase(ownedBeads:beadType:fromSlotPanel:)")));
- (SharedAbacusCustomizationUiLogicBeadPaint *)beadPaintStore:(id<SharedAbacusPrefsStore>)store beadType:(SharedAbacusPreferencesBeadType *)beadType selected:(BOOL)selected __attribute__((swift_name("beadPaint(store:beadType:selected:)")));
- (SharedAbacusCustomizationUiLogicPurchaseCheck *)checkBeadColorFeaturePurchaseBeadType:(SharedAbacusPreferencesBeadType *)beadType cachedKeys:(int32_t)cachedKeys cachedCurrency:(int32_t)cachedCurrency __attribute__((swift_name("checkBeadColorFeaturePurchase(beadType:cachedKeys:cachedCurrency:)")));
- (SharedAbacusCustomizationUiLogicPurchaseCheck *)checkBeadPurchaseBeadType:(SharedAbacusPreferencesBeadType *)beadType cachedKeys:(int32_t)cachedKeys cachedCurrency:(int32_t)cachedCurrency __attribute__((swift_name("checkBeadPurchase(beadType:cachedKeys:cachedCurrency:)")));
- (SharedAbacusCustomizationUiLogicPurchaseCheck *)checkGoldPurchasePrice:(int32_t)price cachedCurrency:(int32_t)cachedCurrency __attribute__((swift_name("checkGoldPurchase(price:cachedCurrency:)")));
- (SharedAbacusCustomizationUiLogicPurchaseCheck *)checkSlotPanelPurchaseOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType cachedKeys:(int32_t)cachedKeys cachedCurrency:(int32_t)cachedCurrency __attribute__((swift_name("checkSlotPanelPurchase(ownedBeads:beadType:cachedKeys:cachedCurrency:)")));
- (int32_t)colorFromSlidersHue:(int32_t)hue lightness:(int32_t)lightness __attribute__((swift_name("colorFromSliders(hue:lightness:)")));
- (NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *)frameBgInsetLayersWoodColor:(int32_t)woodColor __attribute__((swift_name("frameBgInsetLayers(woodColor:)")));
- (NSArray<SharedAbacusCustomizationUiLogicFrameCardState *> *)frameCardStatesStore:(id<SharedAbacusPrefsStore>)store ownedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames __attribute__((swift_name("frameCardStates(store:ownedFrames:)")));
- (NSString * _Nullable)frameColorFeatureImageResNameFrameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("frameColorFeatureImageResName(frameType:)")));
- (NSArray<SharedAbacusCustomizationUiLogicColorRow *> *)frameColorRowsStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("frameColorRows(store:)")));
- (SharedAbacusCustomizationUiLogicFramePaint *)framePaintStore:(id<SharedAbacusPrefsStore>)store frameType:(SharedAbacusPreferencesFrameType * _Nullable)frameType __attribute__((swift_name("framePaint(store:frameType:)")));
- (BOOL)isBeadColorLockedOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("isBeadColorLocked(ownedBeads:beadType:)")));
- (BOOL)isFrameColorLockedStore:(id<SharedAbacusPrefsStore>)store ownedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames __attribute__((swift_name("isFrameColorLocked(store:ownedFrames:)")));
- (NSArray<SharedInt *> *)lightnessGradientColorsHue:(int32_t)hue __attribute__((swift_name("lightnessGradientColors(hue:)")));
- (BOOL)onBeadCardClickedStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid ownedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("onBeadCardClicked(store:uid:ownedBeads:beadType:)")));
- (BOOL)onFrameCardClickedStore:(id<SharedAbacusPrefsStore>)store ownedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames frameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("onFrameCardClicked(store:ownedFrames:frameType:)")));
- (NSDictionary<NSString *, SharedFrameData *> *)onFramePurchasedStore:(id<SharedAbacusPrefsStore>)store ownedFrames:(NSDictionary<NSString *, SharedFrameData *> *)ownedFrames frameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("onFramePurchased(store:ownedFrames:frameType:)")));
- (SharedAbacusCustomizationUiLogicPillGeometry *)pillGeometry __attribute__((swift_name("pillGeometry()")));
- (SharedAbacusCustomizationUiLogicPlaceOutcome *)placeBeadInSlotOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads store:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid beadType:(SharedAbacusPreferencesBeadType *)beadType rod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex __attribute__((swift_name("placeBeadInSlot(ownedBeads:store:uid:beadType:rod:isTop:beadIndex:)")));
- (NSArray<SharedInt *> *)rainbowGradientColors __attribute__((swift_name("rainbowGradientColors()")));
- (void)recolorBeadPixelsStore:(id<SharedAbacusPrefsStore>)store beadType:(SharedAbacusPreferencesBeadType *)beadType selected:(BOOL)selected pixels:(SharedKotlinIntArray *)pixels width:(int32_t)width height:(int32_t)height __attribute__((swift_name("recolorBeadPixels(store:beadType:selected:pixels:width:height:)")));
- (void)recolorFramePixelsStore:(id<SharedAbacusPrefsStore>)store frameType:(SharedAbacusPreferencesFrameType *)frameType pixels:(SharedKotlinIntArray *)pixels __attribute__((swift_name("recolorFramePixels(store:frameType:pixels:)")));
- (int32_t)setBeadColorFromSlidersStore:(id<SharedAbacusPrefsStore>)store beadType:(SharedAbacusPreferencesBeadType *)beadType selected:(BOOL)selected index:(int32_t)index hue:(int32_t)hue lightness:(int32_t)lightness __attribute__((swift_name("setBeadColorFromSliders(store:beadType:selected:index:hue:lightness:)")));
- (int32_t)setFrameColorFromSlidersStore:(id<SharedAbacusPrefsStore>)store index:(int32_t)index hue:(int32_t)hue lightness:(int32_t)lightness __attribute__((swift_name("setFrameColorFromSliders(store:index:hue:lightness:)")));
- (NSArray<SharedAbacusCustomizationUiLogicSlotBead *> *)slotBeadsStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid __attribute__((swift_name("slotBeads(store:uid:)")));
- (SharedAbacusCustomizationUiLogicSlotPanelState *)slotPanelStateOwnedBeads:(NSDictionary<NSString *, SharedBeadData *> *)ownedBeads store:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("slotPanelState(ownedBeads:store:uid:beadType:)")));
- (int32_t)tab2PathCountBeadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("tab2PathCount(beadType:)")));
@property (readonly) NSArray<SharedAbacusCustomizationUiLogicBeadCard *> *BEAD_CARDS __attribute__((swift_name("BEAD_CARDS")));
@property (readonly) int32_t BEAD_CARDS_PER_ROW __attribute__((swift_name("BEAD_CARDS_PER_ROW")));
@property (readonly) SharedAbacusCustomizationUiLogicPurchasePanelRules *BEAD_COLOR_FEATURE_PANEL __attribute__((swift_name("BEAD_COLOR_FEATURE_PANEL")));
@property (readonly) SharedAbacusCustomizationUiLogicPurchasePanelRules *BEAD_PURCHASE_PANEL __attribute__((swift_name("BEAD_PURCHASE_PANEL")));
@property (readonly) int64_t BEAD_REFRESH_DEBOUNCE_MS __attribute__((swift_name("BEAD_REFRESH_DEBOUNCE_MS")));
@property (readonly) int32_t BUY_BUTTON_MAXED_COLOR __attribute__((swift_name("BUY_BUTTON_MAXED_COLOR")));
@property (readonly) int32_t CARD_BACKGROUND_COLOR __attribute__((swift_name("CARD_BACKGROUND_COLOR")));
@property (readonly) int64_t FIRESTORE_SYNC_DEBOUNCE_MS __attribute__((swift_name("FIRESTORE_SYNC_DEBOUNCE_MS")));
@property (readonly) NSArray<SharedAbacusCustomizationUiLogicFrameCard *> *FRAME_CARDS __attribute__((swift_name("FRAME_CARDS")));
@property (readonly) int32_t FRAME_CARDS_PER_ROW __attribute__((swift_name("FRAME_CARDS_PER_ROW")));
@property (readonly) NSString *FRAME_KIND_CANVAS_LAYERS __attribute__((swift_name("FRAME_KIND_CANVAS_LAYERS")));
@property (readonly) NSString *FRAME_KIND_INSET_LAYERS __attribute__((swift_name("FRAME_KIND_INSET_LAYERS")));
@property (readonly) NSString *FRAME_KIND_REPLACED_BITMAP __attribute__((swift_name("FRAME_KIND_REPLACED_BITMAP")));
@property (readonly) SharedAbacusCustomizationUiLogicPurchasePanelRules *FRAME_PURCHASE_PANEL __attribute__((swift_name("FRAME_PURCHASE_PANEL")));
@property (readonly) int32_t HUE_MAX __attribute__((swift_name("HUE_MAX")));
@property (readonly) NSString *LABEL_HUE __attribute__((swift_name("LABEL_HUE")));
@property (readonly) NSString *LABEL_LIGHTNESS __attribute__((swift_name("LABEL_LIGHTNESS")));
@property (readonly) NSString *LABEL_NORMAL __attribute__((swift_name("LABEL_NORMAL")));
@property (readonly) NSString *LABEL_SELECTED __attribute__((swift_name("LABEL_SELECTED")));
@property (readonly) int32_t LIGHTNESS_MAX __attribute__((swift_name("LIGHTNESS_MAX")));
@property (readonly) int32_t LOCK_OVERLAY_COLOR __attribute__((swift_name("LOCK_OVERLAY_COLOR")));
@property (readonly) float PURCHASE_PANEL_WIDTH_RATIO __attribute__((swift_name("PURCHASE_PANEL_WIDTH_RATIO")));
@property (readonly) SharedAbacusCustomizationUiLogicPurchasePanelRules *SLOT_PANEL_PURCHASE __attribute__((swift_name("SLOT_PANEL_PURCHASE")));
@property (readonly) float SLOT_PANEL_WIDTH_RATIO __attribute__((swift_name("SLOT_PANEL_WIDTH_RATIO")));
@property (readonly) int32_t TAB_BEAD_COLOR __attribute__((swift_name("TAB_BEAD_COLOR")));
@property (readonly) int32_t TAB_BEAD_TYPE __attribute__((swift_name("TAB_BEAD_TYPE")));
@property (readonly) int32_t TAB_FRAME_COLOR __attribute__((swift_name("TAB_FRAME_COLOR")));
@property (readonly) int32_t TAB_FRAME_TYPE __attribute__((swift_name("TAB_FRAME_TYPE")));
@property (readonly) NSArray<NSString *> *TAB_ICON_RES_NAMES __attribute__((swift_name("TAB_ICON_RES_NAMES")));
@property (readonly) float TAB_SELECTED_ALPHA __attribute__((swift_name("TAB_SELECTED_ALPHA")));
@property (readonly) float TAB_UNSELECTED_ALPHA __attribute__((swift_name("TAB_UNSELECTED_ALPHA")));
@property (readonly) NSString *TITLE_BEAD_TYPE __attribute__((swift_name("TITLE_BEAD_TYPE")));
@property (readonly) NSString *TITLE_FRAME_TYPE __attribute__((swift_name("TITLE_FRAME_TYPE")));
@property (readonly) NSString *TITLE_SLOT_PANEL __attribute__((swift_name("TITLE_SLOT_PANEL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.BeadCard")))
@interface SharedAbacusCustomizationUiLogicBeadCard : SharedBase
- (instancetype)initWithBeadType:(SharedAbacusPreferencesBeadType *)beadType imageResName:(NSString *)imageResName label:(NSString *)label imageWidthDp:(int32_t)imageWidthDp imageHeightDp:(int32_t)imageHeightDp labelMarginTopDp:(int32_t)labelMarginTopDp __attribute__((swift_name("init(beadType:imageResName:label:imageWidthDp:imageHeightDp:labelMarginTopDp:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicBeadCard *)doCopyBeadType:(SharedAbacusPreferencesBeadType *)beadType imageResName:(NSString *)imageResName label:(NSString *)label imageWidthDp:(int32_t)imageWidthDp imageHeightDp:(int32_t)imageHeightDp labelMarginTopDp:(int32_t)labelMarginTopDp __attribute__((swift_name("doCopy(beadType:imageResName:label:imageWidthDp:imageHeightDp:labelMarginTopDp:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAbacusPreferencesBeadType *beadType __attribute__((swift_name("beadType")));
@property (readonly) int32_t imageHeightDp __attribute__((swift_name("imageHeightDp")));
@property (readonly) NSString *imageResName __attribute__((swift_name("imageResName")));
@property (readonly) int32_t imageWidthDp __attribute__((swift_name("imageWidthDp")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t labelMarginTopDp __attribute__((swift_name("labelMarginTopDp")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.BeadCardState")))
@interface SharedAbacusCustomizationUiLogicBeadCardState : SharedBase
- (instancetype)initWithCard:(SharedAbacusCustomizationUiLogicBeadCard *)card selected:(BOOL)selected locked:(BOOL)locked price:(int32_t)price currencyIconResName:(NSString *)currencyIconResName __attribute__((swift_name("init(card:selected:locked:price:currencyIconResName:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicBeadCardState *)doCopyCard:(SharedAbacusCustomizationUiLogicBeadCard *)card selected:(BOOL)selected locked:(BOOL)locked price:(int32_t)price currencyIconResName:(NSString *)currencyIconResName __attribute__((swift_name("doCopy(card:selected:locked:price:currencyIconResName:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAbacusCustomizationUiLogicBeadCard *card __attribute__((swift_name("card")));
@property (readonly) NSString *currencyIconResName __attribute__((swift_name("currencyIconResName")));
@property (readonly) BOOL locked __attribute__((swift_name("locked")));
@property (readonly) int32_t price __attribute__((swift_name("price")));
@property (readonly) BOOL selected __attribute__((swift_name("selected")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.BeadPaint")))
@interface SharedAbacusCustomizationUiLogicBeadPaint : SharedBase
- (instancetype)initWithBeadType:(SharedAbacusPreferencesBeadType *)beadType selected:(BOOL)selected kind:(NSString *)kind resNames:(NSArray<NSString *> *)resNames colors:(NSArray<SharedInt *> *)colors isDefaultColors:(BOOL)isDefaultColors cacheKey:(NSString *)cacheKey __attribute__((swift_name("init(beadType:selected:kind:resNames:colors:isDefaultColors:cacheKey:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicBeadPaint *)doCopyBeadType:(SharedAbacusPreferencesBeadType *)beadType selected:(BOOL)selected kind:(NSString *)kind resNames:(NSArray<NSString *> *)resNames colors:(NSArray<SharedInt *> *)colors isDefaultColors:(BOOL)isDefaultColors cacheKey:(NSString *)cacheKey __attribute__((swift_name("doCopy(beadType:selected:kind:resNames:colors:isDefaultColors:cacheKey:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAbacusPreferencesBeadType *beadType __attribute__((swift_name("beadType")));
@property (readonly) NSString *cacheKey __attribute__((swift_name("cacheKey")));
@property (readonly) NSArray<SharedInt *> *colors __attribute__((swift_name("colors")));
@property (readonly) BOOL isDefaultColors __attribute__((swift_name("isDefaultColors")));
@property (readonly) NSString *kind __attribute__((swift_name("kind")));
@property (readonly) NSArray<NSString *> *resNames __attribute__((swift_name("resNames")));
@property (readonly) BOOL selected __attribute__((swift_name("selected")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.ColorRow")))
@interface SharedAbacusCustomizationUiLogicColorRow : SharedBase
- (instancetype)initWithIndex:(int32_t)index label:(NSString *)label color:(int32_t)color hue:(int32_t)hue lightness:(int32_t)lightness __attribute__((swift_name("init(index:label:color:hue:lightness:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicColorRow *)doCopyIndex:(int32_t)index label:(NSString *)label color:(int32_t)color hue:(int32_t)hue lightness:(int32_t)lightness __attribute__((swift_name("doCopy(index:label:color:hue:lightness:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t color __attribute__((swift_name("color")));
@property (readonly) int32_t hue __attribute__((swift_name("hue")));
@property (readonly) int32_t index __attribute__((swift_name("index")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t lightness __attribute__((swift_name("lightness")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.FrameCard")))
@interface SharedAbacusCustomizationUiLogicFrameCard : SharedBase
- (instancetype)initWithFrameType:(SharedAbacusPreferencesFrameType *)frameType imageResName:(NSString *)imageResName label:(NSString *)label price:(int32_t)price purchaseImageResName:(NSString * _Nullable)purchaseImageResName __attribute__((swift_name("init(frameType:imageResName:label:price:purchaseImageResName:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicFrameCard *)doCopyFrameType:(SharedAbacusPreferencesFrameType *)frameType imageResName:(NSString *)imageResName label:(NSString *)label price:(int32_t)price purchaseImageResName:(NSString * _Nullable)purchaseImageResName __attribute__((swift_name("doCopy(frameType:imageResName:label:price:purchaseImageResName:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAbacusPreferencesFrameType *frameType __attribute__((swift_name("frameType")));
@property (readonly) NSString *imageResName __attribute__((swift_name("imageResName")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t price __attribute__((swift_name("price")));
@property (readonly) NSString * _Nullable purchaseImageResName __attribute__((swift_name("purchaseImageResName")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.FrameCardState")))
@interface SharedAbacusCustomizationUiLogicFrameCardState : SharedBase
- (instancetype)initWithCard:(SharedAbacusCustomizationUiLogicFrameCard *)card selected:(BOOL)selected locked:(BOOL)locked __attribute__((swift_name("init(card:selected:locked:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicFrameCardState *)doCopyCard:(SharedAbacusCustomizationUiLogicFrameCard *)card selected:(BOOL)selected locked:(BOOL)locked __attribute__((swift_name("doCopy(card:selected:locked:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAbacusCustomizationUiLogicFrameCard *card __attribute__((swift_name("card")));
@property (readonly) BOOL locked __attribute__((swift_name("locked")));
@property (readonly) BOOL selected __attribute__((swift_name("selected")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.FrameLayer")))
@interface SharedAbacusCustomizationUiLogicFrameLayer : SharedBase
- (instancetype)initWithLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom radius:(float)radius color:(int32_t)color isGradient:(BOOL)isGradient __attribute__((swift_name("init(left:top:right:bottom:radius:color:isGradient:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicFrameLayer *)doCopyLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom radius:(float)radius color:(int32_t)color isGradient:(BOOL)isGradient __attribute__((swift_name("doCopy(left:top:right:bottom:radius:color:isGradient:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float bottom __attribute__((swift_name("bottom")));
@property (readonly) int32_t color __attribute__((swift_name("color")));
@property (readonly) BOOL isGradient __attribute__((swift_name("isGradient")));
@property (readonly) float left __attribute__((swift_name("left")));
@property (readonly) float radius __attribute__((swift_name("radius")));
@property (readonly) float right __attribute__((swift_name("right")));
@property (readonly) float top __attribute__((swift_name("top")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.FramePaint")))
@interface SharedAbacusCustomizationUiLogicFramePaint : SharedBase
- (instancetype)initWithFrameType:(SharedAbacusPreferencesFrameType *)frameType kind:(NSString *)kind resName:(NSString * _Nullable)resName colors:(NSArray<SharedInt *> *)colors isDefaultColors:(BOOL)isDefaultColors insetLayers:(NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *)insetLayers canvasLayers:(NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *)canvasLayers gradientStops:(NSArray<SharedFloat *> *)gradientStops canvasSize:(int32_t)canvasSize cacheKey:(NSString *)cacheKey __attribute__((swift_name("init(frameType:kind:resName:colors:isDefaultColors:insetLayers:canvasLayers:gradientStops:canvasSize:cacheKey:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicFramePaint *)doCopyFrameType:(SharedAbacusPreferencesFrameType *)frameType kind:(NSString *)kind resName:(NSString * _Nullable)resName colors:(NSArray<SharedInt *> *)colors isDefaultColors:(BOOL)isDefaultColors insetLayers:(NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *)insetLayers canvasLayers:(NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *)canvasLayers gradientStops:(NSArray<SharedFloat *> *)gradientStops canvasSize:(int32_t)canvasSize cacheKey:(NSString *)cacheKey __attribute__((swift_name("doCopy(frameType:kind:resName:colors:isDefaultColors:insetLayers:canvasLayers:gradientStops:canvasSize:cacheKey:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *cacheKey __attribute__((swift_name("cacheKey")));
@property (readonly) NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *canvasLayers __attribute__((swift_name("canvasLayers")));
@property (readonly) int32_t canvasSize __attribute__((swift_name("canvasSize")));
@property (readonly) NSArray<SharedInt *> *colors __attribute__((swift_name("colors")));
@property (readonly) SharedAbacusPreferencesFrameType *frameType __attribute__((swift_name("frameType")));
@property (readonly) NSArray<SharedFloat *> *gradientStops __attribute__((swift_name("gradientStops")));
@property (readonly) NSArray<SharedAbacusCustomizationUiLogicFrameLayer *> *insetLayers __attribute__((swift_name("insetLayers")));
@property (readonly) BOOL isDefaultColors __attribute__((swift_name("isDefaultColors")));
@property (readonly) NSString *kind __attribute__((swift_name("kind")));
@property (readonly) NSString * _Nullable resName __attribute__((swift_name("resName")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.PillGeometry")))
@interface SharedAbacusCustomizationUiLogicPillGeometry : SharedBase
- (instancetype)initWithWidth:(float)width height:(float)height outerRadiusX:(float)outerRadiusX outerRadiusY:(float)outerRadiusY padX:(float)padX padY:(float)padY innerRadiusX:(float)innerRadiusX innerRadiusY:(float)innerRadiusY stops:(NSArray<SharedFloat *> *)stops __attribute__((swift_name("init(width:height:outerRadiusX:outerRadiusY:padX:padY:innerRadiusX:innerRadiusY:stops:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicPillGeometry *)doCopyWidth:(float)width height:(float)height outerRadiusX:(float)outerRadiusX outerRadiusY:(float)outerRadiusY padX:(float)padX padY:(float)padY innerRadiusX:(float)innerRadiusX innerRadiusY:(float)innerRadiusY stops:(NSArray<SharedFloat *> *)stops __attribute__((swift_name("doCopy(width:height:outerRadiusX:outerRadiusY:padX:padY:innerRadiusX:innerRadiusY:stops:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float height __attribute__((swift_name("height")));
@property (readonly) float innerRadiusX __attribute__((swift_name("innerRadiusX")));
@property (readonly) float innerRadiusY __attribute__((swift_name("innerRadiusY")));
@property (readonly) float outerRadiusX __attribute__((swift_name("outerRadiusX")));
@property (readonly) float outerRadiusY __attribute__((swift_name("outerRadiusY")));
@property (readonly) float padX __attribute__((swift_name("padX")));
@property (readonly) float padY __attribute__((swift_name("padY")));
@property (readonly) NSArray<SharedFloat *> *stops __attribute__((swift_name("stops")));
@property (readonly) float width __attribute__((swift_name("width")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.PlaceOutcome")))
@interface SharedAbacusCustomizationUiLogicPlaceOutcome : SharedBase
- (instancetype)initWithPlaced:(BOOL)placed message:(NSString * _Nullable)message __attribute__((swift_name("init(placed:message:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicPlaceOutcome *)doCopyPlaced:(BOOL)placed message:(NSString * _Nullable)message __attribute__((swift_name("doCopy(placed:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
@property (readonly) BOOL placed __attribute__((swift_name("placed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.PurchaseCheck")))
@interface SharedAbacusCustomizationUiLogicPurchaseCheck : SharedBase
- (instancetype)initWithAllowed:(BOOL)allowed message:(NSString * _Nullable)message price:(int32_t)price isKeyCurrency:(BOOL)isKeyCurrency __attribute__((swift_name("init(allowed:message:price:isKeyCurrency:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicPurchaseCheck *)doCopyAllowed:(BOOL)allowed message:(NSString * _Nullable)message price:(int32_t)price isKeyCurrency:(BOOL)isKeyCurrency __attribute__((swift_name("doCopy(allowed:message:price:isKeyCurrency:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL allowed __attribute__((swift_name("allowed")));
@property (readonly) BOOL isKeyCurrency __attribute__((swift_name("isKeyCurrency")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
@property (readonly) int32_t price __attribute__((swift_name("price")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.PurchasePanelRules")))
@interface SharedAbacusCustomizationUiLogicPurchasePanelRules : SharedBase
- (instancetype)initWithWalletFailureMessage:(NSString *)walletFailureMessage saveFailureMessage:(NSString *)saveFailureMessage rollbackOnSaveFailure:(BOOL)rollbackOnSaveFailure __attribute__((swift_name("init(walletFailureMessage:saveFailureMessage:rollbackOnSaveFailure:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicPurchasePanelRules *)doCopyWalletFailureMessage:(NSString *)walletFailureMessage saveFailureMessage:(NSString *)saveFailureMessage rollbackOnSaveFailure:(BOOL)rollbackOnSaveFailure __attribute__((swift_name("doCopy(walletFailureMessage:saveFailureMessage:rollbackOnSaveFailure:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL rollbackOnSaveFailure __attribute__((swift_name("rollbackOnSaveFailure")));
@property (readonly) NSString *saveFailureMessage __attribute__((swift_name("saveFailureMessage")));
@property (readonly) NSString *walletFailureMessage __attribute__((swift_name("walletFailureMessage")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.SlotBead")))
@interface SharedAbacusCustomizationUiLogicSlotBead : SharedBase
- (instancetype)initWithRod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("init(rod:isTop:beadIndex:beadType:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicSlotBead *)doCopyRod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex beadType:(SharedAbacusPreferencesBeadType *)beadType __attribute__((swift_name("doCopy(rod:isTop:beadIndex:beadType:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t beadIndex __attribute__((swift_name("beadIndex")));
@property (readonly) SharedAbacusPreferencesBeadType *beadType __attribute__((swift_name("beadType")));
@property (readonly) BOOL isTop __attribute__((swift_name("isTop")));
@property (readonly) int32_t rod __attribute__((swift_name("rod")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCustomizationUiLogic.SlotPanelState")))
@interface SharedAbacusCustomizationUiLogicSlotPanelState : SharedBase
- (instancetype)initWithAvailableCount:(int32_t)availableCount inventoryText:(NSString *)inventoryText price:(int32_t)price currencyIconResName:(NSString *)currencyIconResName buyButtonMaxed:(BOOL)buyButtonMaxed __attribute__((swift_name("init(availableCount:inventoryText:price:currencyIconResName:buyButtonMaxed:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusCustomizationUiLogicSlotPanelState *)doCopyAvailableCount:(int32_t)availableCount inventoryText:(NSString *)inventoryText price:(int32_t)price currencyIconResName:(NSString *)currencyIconResName buyButtonMaxed:(BOOL)buyButtonMaxed __attribute__((swift_name("doCopy(availableCount:inventoryText:price:currencyIconResName:buyButtonMaxed:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t availableCount __attribute__((swift_name("availableCount")));
@property (readonly) BOOL buyButtonMaxed __attribute__((swift_name("buyButtonMaxed")));
@property (readonly) NSString *currencyIconResName __attribute__((swift_name("currencyIconResName")));
@property (readonly) NSString *inventoryText __attribute__((swift_name("inventoryText")));
@property (readonly) int32_t price __attribute__((swift_name("price")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusFrameRenderer")))
@interface SharedAbacusFrameRenderer : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusFrameRenderer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusFrameRenderer *shared __attribute__((swift_name("shared")));
- (NSString * _Nullable)frameResNameType:(SharedAbacusPreferencesFrameType *)type __attribute__((swift_name("frameResName(type:)")));
- (int32_t)lerpA:(int32_t)a b:(int32_t)b t:(float)t __attribute__((swift_name("lerp(a:b:t:)")));
- (SharedKotlinIntArray *)doNewColorsForWoodNew:(int32_t)woodNew accentNew:(int32_t)accentNew __attribute__((swift_name("doNewColorsFor(woodNew:accentNew:)")));
- (void)replaceFramePixelsPixels:(SharedKotlinIntArray *)pixels origColors:(SharedKotlinIntArray *)origColors newColors:(SharedKotlinIntArray *)newColors __attribute__((swift_name("replaceFramePixels(pixels:origColors:newColors:)")));
- (SharedKotlinIntArray *)resolveFrameColorsStore:(id<SharedAbacusPrefsStore>)store type:(SharedAbacusPreferencesFrameType *)type colorsOverride:(SharedKotlinIntArray * _Nullable)colorsOverride __attribute__((swift_name("resolveFrameColors(store:type:colorsOverride:)")));
- (SharedAbacusPreferencesFrameType *)resolveFrameTypeStore:(id<SharedAbacusPrefsStore>)store frameTypeOverride:(SharedAbacusPreferencesFrameType * _Nullable)frameTypeOverride __attribute__((swift_name("resolveFrameType(store:frameTypeOverride:)")));
@property (readonly) SharedKotlinIntArray *ALL_ORIG __attribute__((swift_name("ALL_ORIG")));
@property (readonly) SharedKotlinIntArray *ALL_ORIG_BG3 __attribute__((swift_name("ALL_ORIG_BG3")));
@property (readonly) SharedKotlinFloatArray *BG4_GRADIENT_STOPS __attribute__((swift_name("BG4_GRADIENT_STOPS")));
@property (readonly) NSArray<SharedAbacusFrameRendererRoundRectLayer *> *BG4_LAYERS __attribute__((swift_name("BG4_LAYERS")));
@property (readonly) SharedKotlinIntArray *EDGES __attribute__((swift_name("EDGES")));
@property (readonly) int32_t ORIG_BLACK __attribute__((swift_name("ORIG_BLACK")));
@property (readonly) int32_t ORIG_DARK __attribute__((swift_name("ORIG_DARK")));
@property (readonly) int32_t ORIG_HEART __attribute__((swift_name("ORIG_HEART")));
@property (readonly) int32_t ORIG_STAR __attribute__((swift_name("ORIG_STAR")));
@property (readonly) int32_t ORIG_WOOD __attribute__((swift_name("ORIG_WOOD")));
@property (readonly) int32_t RENDER_SIZE __attribute__((swift_name("RENDER_SIZE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusFrameRenderer.RoundRectLayer")))
@interface SharedAbacusFrameRendererRoundRectLayer : SharedBase
- (instancetype)initWithLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom radius:(float)radius color:(SharedInt * _Nullable)color __attribute__((swift_name("init(left:top:right:bottom:radius:color:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusFrameRendererRoundRectLayer *)doCopyLeft:(float)left top:(float)top right:(float)right bottom:(float)bottom radius:(float)radius color:(SharedInt * _Nullable)color __attribute__((swift_name("doCopy(left:top:right:bottom:radius:color:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float bottom __attribute__((swift_name("bottom")));
@property (readonly) SharedInt * _Nullable color __attribute__((swift_name("color")));
@property (readonly) float left __attribute__((swift_name("left")));
@property (readonly) float radius __attribute__((swift_name("radius")));
@property (readonly) float right __attribute__((swift_name("right")));
@property (readonly) float top __attribute__((swift_name("top")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusLessonOutcome")))
@interface SharedAbacusLessonOutcome : SharedKotlinEnum<SharedAbacusLessonOutcome *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAbacusLessonOutcome *lessonResult __attribute__((swift_name("lessonResult")));
@property (class, readonly) SharedAbacusLessonOutcome *lessonResultFalse __attribute__((swift_name("lessonResultFalse")));
@property (class, readonly) SharedAbacusLessonOutcome *chestResult __attribute__((swift_name("chestResult")));
@property (class, readonly) SharedAbacusLessonOutcome *chestFailure __attribute__((swift_name("chestFailure")));
+ (SharedKotlinArray<SharedAbacusLessonOutcome *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAbacusLessonOutcome *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusLessonRules")))
@interface SharedAbacusLessonRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusLessonRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusLessonRules *shared __attribute__((swift_name("shared")));
- (int64_t)beadAnimationDurationMsLessonType:(int32_t)lessonType __attribute__((swift_name("beadAnimationDurationMs(lessonType:)")));
- (SharedKotlinPair<SharedFloat *, SharedInt *> *)calculateChestScoreSuccessRate:(float)successRate timeStr:(NSString *)timeStr worstCupTime:(int32_t)worstCupTime __attribute__((swift_name("calculateChestScore(successRate:timeStr:worstCupTime:)")));
- (int32_t)dersPuaniSuccessRate:(float)successRate __attribute__((swift_name("dersPuani(successRate:)")));
- (NSString *)formatTimerSeconds:(int32_t)seconds __attribute__((swift_name("formatTimer(seconds:)")));
- (BOOL)isLessonSuccessRateTypeLessonType:(int32_t)lessonType __attribute__((swift_name("isLessonSuccessRateType(lessonType:)")));
- (NSString *)multiplicationLayoutForDigitDigit:(int32_t)digit __attribute__((swift_name("multiplicationLayoutForDigit(digit:)")));
- (NSArray<NSString *> *)optimizeBeadIdsForReverseBeadIds:(NSArray<NSString *> *)beadIds __attribute__((swift_name("optimizeBeadIdsForReverse(beadIds:)")));
- (int32_t)parseTimeToSecondsValue:(NSString *)value __attribute__((swift_name("parseTimeToSeconds(value:)")));
- (float)progressEndWeightCurrentIndex:(int32_t)currentIndex operationCount:(int32_t)operationCount __attribute__((swift_name("progressEndWeight(currentIndex:operationCount:)")));
- (int32_t)resolveWorstCupTimeFallbackFromCurrent:(SharedInt * _Nullable)fromCurrent fromTemplate:(SharedInt * _Nullable)fromTemplate __attribute__((swift_name("resolveWorstCupTimeFallback(fromCurrent:fromTemplate:)")));
- (BOOL)shouldPlayPerfectAnimationCurrentIndex:(int32_t)currentIndex operationCount:(int32_t)operationCount correctAnswer:(int32_t)correctAnswer totalQuestions:(int32_t)totalQuestions __attribute__((swift_name("shouldPlayPerfectAnimation(currentIndex:operationCount:correctAnswer:totalQuestions:)")));
- (NSString *)splitTextEquallyText:(NSString *)text __attribute__((swift_name("splitTextEqually(text:)")));
- (float)successRateCorrectAnswer:(int32_t)correctAnswer totalQuestions:(int32_t)totalQuestions __attribute__((swift_name("successRate(correctAnswer:totalQuestions:)")));
@property (readonly) int32_t CHEST_MIN_TOTAL_SCORE __attribute__((swift_name("CHEST_MIN_TOTAL_SCORE")));
@property (readonly) int64_t CONTROL_DEBOUNCE_MS __attribute__((swift_name("CONTROL_DEBOUNCE_MS")));
@property (readonly) int64_t CONTROL_REENABLE_DELAY_CORRECT_MS __attribute__((swift_name("CONTROL_REENABLE_DELAY_CORRECT_MS")));
@property (readonly) int64_t CONTROL_REENABLE_DELAY_INCORRECT_MS __attribute__((swift_name("CONTROL_REENABLE_DELAY_INCORRECT_MS")));
@property (readonly) float LESSON_PASS_MIN_SUCCESS_RATE __attribute__((swift_name("LESSON_PASS_MIN_SUCCESS_RATE")));
@property (readonly) int64_t PROGRESS_ANIMATION_MS __attribute__((swift_name("PROGRESS_ANIMATION_MS")));
@property (readonly) int32_t PROGRESS_COLOR_DARK_RED __attribute__((swift_name("PROGRESS_COLOR_DARK_RED")));
@property (readonly) int32_t PROGRESS_COLOR_GREEN __attribute__((swift_name("PROGRESS_COLOR_GREEN")));
@property (readonly) int32_t PROGRESS_COLOR_YELLOW __attribute__((swift_name("PROGRESS_COLOR_YELLOW")));
@property (readonly) NSString *SOUND_CORRECT __attribute__((swift_name("SOUND_CORRECT")));
@property (readonly) float SOUND_CORRECT_VOLUME __attribute__((swift_name("SOUND_CORRECT_VOLUME")));
@property (readonly) NSString *SOUND_INCORRECT __attribute__((swift_name("SOUND_INCORRECT")));
@property (readonly) int32_t TYPE_CHEST __attribute__((swift_name("TYPE_CHEST")));
@property (readonly) int32_t WORST_CUP_TIME_FALLBACK __attribute__((swift_name("WORST_CUP_TIME_FALLBACK")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusLessonSession")))
@interface SharedAbacusLessonSession : SharedBase
- (instancetype)initWithOperations:(NSArray<SharedAbacusOperation *> *)operations lessonType:(int32_t)lessonType __attribute__((swift_name("init(operations:lessonType:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusOperation * _Nullable)currentOperation __attribute__((swift_name("currentOperation()")));
- (float)currentSuccessRate __attribute__((swift_name("currentSuccessRate()")));
- (int32_t)dersPuani __attribute__((swift_name("dersPuani()")));
- (BOOL)onContinue __attribute__((swift_name("onContinue()")));
- (SharedAbacusLessonSessionControlResult *)onControlPressedNowMs:(int64_t)nowMs isOnline:(BOOL)isOnline abacusValue:(int32_t)abacusValue __attribute__((swift_name("onControlPressed(nowMs:isOnline:abacusValue:)")));
- (NSString *)onTimerTick __attribute__((swift_name("onTimerTick()")));
- (SharedAbacusLessonOutcome *)outcomeWorstCupTime:(int32_t)worstCupTime __attribute__((swift_name("outcome(worstCupTime:)")));
- (BOOL)start __attribute__((swift_name("start()")));
- (BOOL)stepAnswerAlgorithmAbacusValue:(int32_t)abacusValue __attribute__((swift_name("stepAnswerAlgorithm(abacusValue:)")));
- (void)stopTimer __attribute__((swift_name("stopTimer()")));
@property (readonly) int32_t answerNumber __attribute__((swift_name("answerNumber")));
@property (readonly) int32_t controlNumber __attribute__((swift_name("controlNumber")));
@property (readonly) int32_t correctAnswer __attribute__((swift_name("correctAnswer")));
@property (readonly) NSString *correctAnswerLabel __attribute__((swift_name("correctAnswerLabel")));
@property (readonly) NSString *correctAnswerText __attribute__((swift_name("correctAnswerText")));
@property (readonly) int32_t currentIndex __attribute__((swift_name("currentIndex")));
@property (readonly) NSString *currentTime __attribute__((swift_name("currentTime")));
@property (readonly) BOOL isTimerStarted __attribute__((swift_name("isTimerStarted")));
@property (readonly) BOOL isTimerVisible __attribute__((swift_name("isTimerVisible")));
@property (readonly) BOOL lessonStarted __attribute__((swift_name("lessonStarted")));
@property (readonly) int32_t lessonType __attribute__((swift_name("lessonType")));
@property (readonly) BOOL needsStartButton __attribute__((swift_name("needsStartButton")));
@property (readonly) NSArray<SharedAbacusOperation *> *operations __attribute__((swift_name("operations")));
@property (readonly) int32_t seconds __attribute__((swift_name("seconds")));
@property (readonly) int32_t totalQuestions __attribute__((swift_name("totalQuestions")));
@end

__attribute__((swift_name("AbacusLessonSession.ControlResult")))
@interface SharedAbacusLessonSessionControlResult : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusLessonSession.ControlResultAnswered")))
@interface SharedAbacusLessonSessionControlResultAnswered : SharedAbacusLessonSessionControlResult
- (instancetype)initWithIsCorrect:(BOOL)isCorrect timerStopped:(BOOL)timerStopped progressEndWeight:(float)progressEndWeight playPerfectAnimation:(BOOL)playPerfectAnimation __attribute__((swift_name("init(isCorrect:timerStopped:progressEndWeight:playPerfectAnimation:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusLessonSessionControlResultAnswered *)doCopyIsCorrect:(BOOL)isCorrect timerStopped:(BOOL)timerStopped progressEndWeight:(float)progressEndWeight playPerfectAnimation:(BOOL)playPerfectAnimation __attribute__((swift_name("doCopy(isCorrect:timerStopped:progressEndWeight:playPerfectAnimation:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isCorrect __attribute__((swift_name("isCorrect")));
@property (readonly) BOOL playPerfectAnimation __attribute__((swift_name("playPerfectAnimation")));
@property (readonly) float progressEndWeight __attribute__((swift_name("progressEndWeight")));
@property (readonly) BOOL timerStopped __attribute__((swift_name("timerStopped")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusLessonSession.ControlResultIgnored")))
@interface SharedAbacusLessonSessionControlResultIgnored : SharedAbacusLessonSessionControlResult
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)ignored __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusLessonSessionControlResultIgnored *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusLessonSession.ControlResultOffline")))
@interface SharedAbacusLessonSessionControlResultOffline : SharedAbacusLessonSessionControlResult
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)offline __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusLessonSessionControlResultOffline *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusOperation")))
@interface SharedAbacusOperation : SharedBase
- (instancetype)initWithFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("init(firstNumber:operator:secondNumber:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusOperation *)doCopyFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("doCopy(firstNumber:operator:secondNumber:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable firstNumber __attribute__((swift_name("firstNumber")));
@property (readonly, getter=operator) NSString * _Nullable operator_ __attribute__((swift_name("operator_")));
@property (readonly) SharedInt * _Nullable secondNumber __attribute__((swift_name("secondNumber")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPracticeLogic")))
@interface SharedAbacusPracticeLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusPracticeLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusPracticeLogic *shared __attribute__((swift_name("shared")));
- (SharedAbacusPracticeLogicKontrolResult *)kontrolFirstText:(NSString * _Nullable)firstText secondText:(NSString * _Nullable)secondText selectedOperator:(NSString *)selectedOperator abacusValue:(int32_t)abacusValue __attribute__((swift_name("kontrol(firstText:secondText:selectedOperator:abacusValue:)")));
@property (readonly) NSString *DEFAULT_OPERATOR __attribute__((swift_name("DEFAULT_OPERATOR")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPracticeLogic.KontrolResult")))
@interface SharedAbacusPracticeLogicKontrolResult : SharedBase
- (instancetype)initWithIsCorrect:(BOOL)isCorrect expected:(int64_t)expected abacusValue:(int64_t)abacusValue __attribute__((swift_name("init(isCorrect:expected:abacusValue:)"))) __attribute__((objc_designated_initializer));
- (SharedAbacusPracticeLogicKontrolResult *)doCopyIsCorrect:(BOOL)isCorrect expected:(int64_t)expected abacusValue:(int64_t)abacusValue __attribute__((swift_name("doCopy(isCorrect:expected:abacusValue:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t abacusValue __attribute__((swift_name("abacusValue")));
@property (readonly) NSString *correctAnswerLabel __attribute__((swift_name("correctAnswerLabel")));
@property (readonly) NSString *correctAnswerText __attribute__((swift_name("correctAnswerText")));
@property (readonly) int64_t expected __attribute__((swift_name("expected")));
@property (readonly) BOOL isCorrect __attribute__((swift_name("isCorrect")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPreferences")))
@interface SharedAbacusPreferences : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusPreferences __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusPreferences *shared __attribute__((swift_name("shared")));
- (SharedAbacusPreferencesBeadColorSpec *)beadColorSpecType:(SharedAbacusPreferencesBeadType *)type __attribute__((swift_name("beadColorSpec(type:)")));
- (SharedAbacusPreferencesBeadType * _Nullable)beadTypeFromNameName:(NSString * _Nullable)name __attribute__((swift_name("beadTypeFromName(name:)")));
- (SharedAbacusPreferencesFrameType * _Nullable)frameTypeFromNameName:(NSString * _Nullable)name __attribute__((swift_name("frameTypeFromName(name:)")));
- (SharedKotlinIntArray *)getBeadColorsStore:(id<SharedAbacusPrefsStore>)store type:(SharedAbacusPreferencesBeadType *)type selected:(BOOL)selected __attribute__((swift_name("getBeadColors(store:type:selected:)")));
- (SharedAbacusPreferencesBeadType *)getBeadTypeStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid __attribute__((swift_name("getBeadType(store:uid:)")));
- (SharedAbacusPreferencesBeadType *)getBeadTypeForSlotStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid rod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex __attribute__((swift_name("getBeadTypeForSlot(store:uid:rod:isTop:beadIndex:)")));
- (NSString *)getBeadTypeKeyUid:(NSString * _Nullable)uid __attribute__((swift_name("getBeadTypeKey(uid:)")));
- (int32_t)getFrameBg2HeartColorStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameBg2HeartColor(store:)")));
- (int32_t)getFrameBg2WoodColorStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameBg2WoodColor(store:)")));
- (int32_t)getFrameBg3StarColorStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameBg3StarColor(store:)")));
- (int32_t)getFrameBg3WoodColorStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameBg3WoodColor(store:)")));
- (SharedKotlinIntArray *)getFrameBg4ColorsStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameBg4Colors(store:)")));
- (int32_t)getFrameBgWoodColorStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameBgWoodColor(store:)")));
- (SharedAbacusPreferencesFrameType *)getFrameTypeStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("getFrameType(store:)")));
- (SharedKotlinIntArray *)getRabbitColorsStore:(id<SharedAbacusPrefsStore>)store selected:(BOOL)selected __attribute__((swift_name("getRabbitColors(store:selected:)")));
- (NSString *)getUidPrefixUid:(NSString * _Nullable)uid __attribute__((swift_name("getUidPrefix(uid:)")));
- (NSString *)prefsFileNameUid:(NSString * _Nullable)uid __attribute__((swift_name("prefsFileName(uid:)")));
- (void)resetAllBeadSlotsStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid __attribute__((swift_name("resetAllBeadSlots(store:uid:)")));
- (void)resetAllFrameColorsStore:(id<SharedAbacusPrefsStore>)store __attribute__((swift_name("resetAllFrameColors(store:)")));
- (void)resetBeadColorsStore:(id<SharedAbacusPrefsStore>)store type:(SharedAbacusPreferencesBeadType *)type selected:(BOOL)selected __attribute__((swift_name("resetBeadColors(store:type:selected:)")));
- (void)resetRabbitColorsStore:(id<SharedAbacusPrefsStore>)store selected:(BOOL)selected __attribute__((swift_name("resetRabbitColors(store:selected:)")));
- (void)setBeadColorStore:(id<SharedAbacusPrefsStore>)store type:(SharedAbacusPreferencesBeadType *)type selected:(BOOL)selected colorIndex:(int32_t)colorIndex color:(int32_t)color __attribute__((swift_name("setBeadColor(store:type:selected:colorIndex:color:)")));
- (void)setBeadTypeStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid type:(SharedAbacusPreferencesBeadType *)type __attribute__((swift_name("setBeadType(store:uid:type:)")));
- (void)setBeadTypeForSlotStore:(id<SharedAbacusPrefsStore>)store uid:(NSString * _Nullable)uid rod:(int32_t)rod isTop:(BOOL)isTop beadIndex:(int32_t)beadIndex type:(SharedAbacusPreferencesBeadType *)type __attribute__((swift_name("setBeadTypeForSlot(store:uid:rod:isTop:beadIndex:type:)")));
- (void)setFrameBg2HeartColorStore:(id<SharedAbacusPrefsStore>)store color:(int32_t)color __attribute__((swift_name("setFrameBg2HeartColor(store:color:)")));
- (void)setFrameBg2WoodColorStore:(id<SharedAbacusPrefsStore>)store color:(int32_t)color __attribute__((swift_name("setFrameBg2WoodColor(store:color:)")));
- (void)setFrameBg3StarColorStore:(id<SharedAbacusPrefsStore>)store color:(int32_t)color __attribute__((swift_name("setFrameBg3StarColor(store:color:)")));
- (void)setFrameBg3WoodColorStore:(id<SharedAbacusPrefsStore>)store color:(int32_t)color __attribute__((swift_name("setFrameBg3WoodColor(store:color:)")));
- (void)setFrameBg4ColorStore:(id<SharedAbacusPrefsStore>)store index:(int32_t)index color:(int32_t)color __attribute__((swift_name("setFrameBg4Color(store:index:color:)")));
- (void)setFrameBgWoodColorStore:(id<SharedAbacusPrefsStore>)store color:(int32_t)color __attribute__((swift_name("setFrameBgWoodColor(store:color:)")));
- (void)setFrameTypeStore:(id<SharedAbacusPrefsStore>)store type:(SharedAbacusPreferencesFrameType *)type __attribute__((swift_name("setFrameType(store:type:)")));
- (void)setRabbitColorStore:(id<SharedAbacusPrefsStore>)store selected:(BOOL)selected colorIndex:(int32_t)colorIndex color:(int32_t)color __attribute__((swift_name("setRabbitColor(store:selected:colorIndex:color:)")));
- (NSString *)slotKeyBottomUid:(NSString * _Nullable)uid rod:(int32_t)rod beadIndex:(int32_t)beadIndex __attribute__((swift_name("slotKeyBottom(uid:rod:beadIndex:)")));
- (NSString *)slotKeyTopUid:(NSString * _Nullable)uid rod:(int32_t)rod __attribute__((swift_name("slotKeyTop(uid:rod:)")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL2_COLOR_LABELS __attribute__((swift_name("ANIMAL2_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL2_DEFAULT_COLORS __attribute__((swift_name("ANIMAL2_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL2_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL2_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL3_COLOR_KEYS __attribute__((swift_name("ANIMAL3_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL3_COLOR_LABELS __attribute__((swift_name("ANIMAL3_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL3_DEFAULT_COLORS __attribute__((swift_name("ANIMAL3_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL3_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL3_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL4_COLOR_KEYS __attribute__((swift_name("ANIMAL4_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL4_COLOR_LABELS __attribute__((swift_name("ANIMAL4_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL4_DEFAULT_COLORS __attribute__((swift_name("ANIMAL4_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL4_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL4_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL5_COLOR_KEYS __attribute__((swift_name("ANIMAL5_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL5_COLOR_LABELS __attribute__((swift_name("ANIMAL5_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL5_DEFAULT_COLORS __attribute__((swift_name("ANIMAL5_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL5_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL5_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL6_COLOR_KEYS __attribute__((swift_name("ANIMAL6_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL6_COLOR_LABELS __attribute__((swift_name("ANIMAL6_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL6_DEFAULT_COLORS __attribute__((swift_name("ANIMAL6_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL6_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL6_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL7_COLOR_KEYS __attribute__((swift_name("ANIMAL7_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL7_COLOR_LABELS __attribute__((swift_name("ANIMAL7_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL7_DEFAULT_COLORS __attribute__((swift_name("ANIMAL7_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL7_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL7_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL8_COLOR_KEYS __attribute__((swift_name("ANIMAL8_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL8_COLOR_LABELS __attribute__((swift_name("ANIMAL8_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL8_DEFAULT_COLORS __attribute__((swift_name("ANIMAL8_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL8_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL8_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL9_COLOR_KEYS __attribute__((swift_name("ANIMAL9_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL9_COLOR_LABELS __attribute__((swift_name("ANIMAL9_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL9_DEFAULT_COLORS __attribute__((swift_name("ANIMAL9_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL9_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL9_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *ANIMAL_COLOR_LABELS __attribute__((swift_name("ANIMAL_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *ANIMAL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *ANIMAL_SEL_DEFAULT_COLORS __attribute__((swift_name("ANIMAL_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL10_COLOR_KEYS __attribute__((swift_name("BALL10_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL10_COLOR_LABELS __attribute__((swift_name("BALL10_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL10_DEFAULT_COLORS __attribute__((swift_name("BALL10_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL10_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL10_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL11_COLOR_KEYS __attribute__((swift_name("BALL11_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL11_COLOR_LABELS __attribute__((swift_name("BALL11_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL11_DEFAULT_COLORS __attribute__((swift_name("BALL11_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL11_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL11_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL12_COLOR_KEYS __attribute__((swift_name("BALL12_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL12_COLOR_LABELS __attribute__((swift_name("BALL12_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL12_DEFAULT_COLORS __attribute__((swift_name("BALL12_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL12_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL12_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL13_COLOR_KEYS __attribute__((swift_name("BALL13_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL13_COLOR_LABELS __attribute__((swift_name("BALL13_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL13_DEFAULT_COLORS __attribute__((swift_name("BALL13_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL13_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL13_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL14_COLOR_KEYS __attribute__((swift_name("BALL14_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL14_COLOR_LABELS __attribute__((swift_name("BALL14_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL14_DEFAULT_COLORS __attribute__((swift_name("BALL14_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL14_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL14_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL1_COLOR_KEYS __attribute__((swift_name("BALL1_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL1_COLOR_LABELS __attribute__((swift_name("BALL1_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL1_DEFAULT_COLORS __attribute__((swift_name("BALL1_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL1_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL1_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL3_COLOR_KEYS __attribute__((swift_name("BALL3_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL3_COLOR_LABELS __attribute__((swift_name("BALL3_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL3_DEFAULT_COLORS __attribute__((swift_name("BALL3_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL3_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL3_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL4_COLOR_KEYS __attribute__((swift_name("BALL4_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL4_COLOR_LABELS __attribute__((swift_name("BALL4_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL4_DEFAULT_COLORS __attribute__((swift_name("BALL4_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL4_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL4_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL5_COLOR_KEYS __attribute__((swift_name("BALL5_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL5_COLOR_LABELS __attribute__((swift_name("BALL5_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL5_DEFAULT_COLORS __attribute__((swift_name("BALL5_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL5_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL5_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL6_COLOR_KEYS __attribute__((swift_name("BALL6_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL6_COLOR_LABELS __attribute__((swift_name("BALL6_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL6_DEFAULT_COLORS __attribute__((swift_name("BALL6_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL6_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL6_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL7_COLOR_KEYS __attribute__((swift_name("BALL7_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL7_COLOR_LABELS __attribute__((swift_name("BALL7_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL7_DEFAULT_COLORS __attribute__((swift_name("BALL7_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL7_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL7_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL8_COLOR_KEYS __attribute__((swift_name("BALL8_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL8_COLOR_LABELS __attribute__((swift_name("BALL8_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL8_DEFAULT_COLORS __attribute__((swift_name("BALL8_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL8_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL8_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL9_COLOR_KEYS __attribute__((swift_name("BALL9_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BALL9_COLOR_LABELS __attribute__((swift_name("BALL9_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BALL9_DEFAULT_COLORS __attribute__((swift_name("BALL9_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BALL9_SEL_DEFAULT_COLORS __attribute__((swift_name("BALL9_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *BOWLING_COLOR_KEYS __attribute__((swift_name("BOWLING_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *BOWLING_COLOR_LABELS __attribute__((swift_name("BOWLING_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *BOWLING_DEFAULT_COLORS __attribute__((swift_name("BOWLING_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *BOWLING_SEL_DEFAULT_COLORS __attribute__((swift_name("BOWLING_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *FRAME_BG4_DEFAULT_COLORS __attribute__((swift_name("FRAME_BG4_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *FRAME_BG4_KEYS __attribute__((swift_name("FRAME_BG4_KEYS")));
@property (readonly) int32_t FRAME_DEFAULT_HEART __attribute__((swift_name("FRAME_DEFAULT_HEART")));
@property (readonly) int32_t FRAME_DEFAULT_STAR __attribute__((swift_name("FRAME_DEFAULT_STAR")));
@property (readonly) int32_t FRAME_DEFAULT_WOOD __attribute__((swift_name("FRAME_DEFAULT_WOOD")));
@property (readonly) NSString *KEY_BEAD_TYPE __attribute__((swift_name("KEY_BEAD_TYPE")));
@property (readonly) NSString *KEY_FRAME_BG2_HEART __attribute__((swift_name("KEY_FRAME_BG2_HEART")));
@property (readonly) NSString *KEY_FRAME_BG2_WOOD __attribute__((swift_name("KEY_FRAME_BG2_WOOD")));
@property (readonly) NSString *KEY_FRAME_BG3_STAR __attribute__((swift_name("KEY_FRAME_BG3_STAR")));
@property (readonly) NSString *KEY_FRAME_BG3_WOOD __attribute__((swift_name("KEY_FRAME_BG3_WOOD")));
@property (readonly) NSString *KEY_FRAME_BG4_C1 __attribute__((swift_name("KEY_FRAME_BG4_C1")));
@property (readonly) NSString *KEY_FRAME_BG4_C2 __attribute__((swift_name("KEY_FRAME_BG4_C2")));
@property (readonly) NSString *KEY_FRAME_BG4_C3 __attribute__((swift_name("KEY_FRAME_BG4_C3")));
@property (readonly) NSString *KEY_FRAME_BG4_C4 __attribute__((swift_name("KEY_FRAME_BG4_C4")));
@property (readonly) NSString *KEY_FRAME_BG4_C5 __attribute__((swift_name("KEY_FRAME_BG4_C5")));
@property (readonly) NSString *KEY_FRAME_BG_WOOD __attribute__((swift_name("KEY_FRAME_BG_WOOD")));
@property (readonly) NSString *KEY_FRAME_TYPE __attribute__((swift_name("KEY_FRAME_TYPE")));
@property (readonly) NSString *PREFIX_ANIMAL __attribute__((swift_name("PREFIX_ANIMAL")));
@property (readonly) NSString *PREFIX_ANIMAL2 __attribute__((swift_name("PREFIX_ANIMAL2")));
@property (readonly) NSString *PREFIX_ANIMAL2_SEL __attribute__((swift_name("PREFIX_ANIMAL2_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL3 __attribute__((swift_name("PREFIX_ANIMAL3")));
@property (readonly) NSString *PREFIX_ANIMAL3_SEL __attribute__((swift_name("PREFIX_ANIMAL3_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL4 __attribute__((swift_name("PREFIX_ANIMAL4")));
@property (readonly) NSString *PREFIX_ANIMAL4_SEL __attribute__((swift_name("PREFIX_ANIMAL4_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL5 __attribute__((swift_name("PREFIX_ANIMAL5")));
@property (readonly) NSString *PREFIX_ANIMAL5_SEL __attribute__((swift_name("PREFIX_ANIMAL5_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL6 __attribute__((swift_name("PREFIX_ANIMAL6")));
@property (readonly) NSString *PREFIX_ANIMAL6_SEL __attribute__((swift_name("PREFIX_ANIMAL6_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL7 __attribute__((swift_name("PREFIX_ANIMAL7")));
@property (readonly) NSString *PREFIX_ANIMAL7_SEL __attribute__((swift_name("PREFIX_ANIMAL7_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL8 __attribute__((swift_name("PREFIX_ANIMAL8")));
@property (readonly) NSString *PREFIX_ANIMAL8_SEL __attribute__((swift_name("PREFIX_ANIMAL8_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL9 __attribute__((swift_name("PREFIX_ANIMAL9")));
@property (readonly) NSString *PREFIX_ANIMAL9_SEL __attribute__((swift_name("PREFIX_ANIMAL9_SEL")));
@property (readonly) NSString *PREFIX_ANIMAL_SEL __attribute__((swift_name("PREFIX_ANIMAL_SEL")));
@property (readonly) NSString *PREFIX_BALL1 __attribute__((swift_name("PREFIX_BALL1")));
@property (readonly) NSString *PREFIX_BALL10 __attribute__((swift_name("PREFIX_BALL10")));
@property (readonly) NSString *PREFIX_BALL10_SEL __attribute__((swift_name("PREFIX_BALL10_SEL")));
@property (readonly) NSString *PREFIX_BALL11 __attribute__((swift_name("PREFIX_BALL11")));
@property (readonly) NSString *PREFIX_BALL11_SEL __attribute__((swift_name("PREFIX_BALL11_SEL")));
@property (readonly) NSString *PREFIX_BALL12 __attribute__((swift_name("PREFIX_BALL12")));
@property (readonly) NSString *PREFIX_BALL12_SEL __attribute__((swift_name("PREFIX_BALL12_SEL")));
@property (readonly) NSString *PREFIX_BALL13 __attribute__((swift_name("PREFIX_BALL13")));
@property (readonly) NSString *PREFIX_BALL13_SEL __attribute__((swift_name("PREFIX_BALL13_SEL")));
@property (readonly) NSString *PREFIX_BALL14 __attribute__((swift_name("PREFIX_BALL14")));
@property (readonly) NSString *PREFIX_BALL14_SEL __attribute__((swift_name("PREFIX_BALL14_SEL")));
@property (readonly) NSString *PREFIX_BALL1_SEL __attribute__((swift_name("PREFIX_BALL1_SEL")));
@property (readonly) NSString *PREFIX_BALL3 __attribute__((swift_name("PREFIX_BALL3")));
@property (readonly) NSString *PREFIX_BALL3_SEL __attribute__((swift_name("PREFIX_BALL3_SEL")));
@property (readonly) NSString *PREFIX_BALL4 __attribute__((swift_name("PREFIX_BALL4")));
@property (readonly) NSString *PREFIX_BALL4_SEL __attribute__((swift_name("PREFIX_BALL4_SEL")));
@property (readonly) NSString *PREFIX_BALL5 __attribute__((swift_name("PREFIX_BALL5")));
@property (readonly) NSString *PREFIX_BALL5_SEL __attribute__((swift_name("PREFIX_BALL5_SEL")));
@property (readonly) NSString *PREFIX_BALL6 __attribute__((swift_name("PREFIX_BALL6")));
@property (readonly) NSString *PREFIX_BALL6_SEL __attribute__((swift_name("PREFIX_BALL6_SEL")));
@property (readonly) NSString *PREFIX_BALL7 __attribute__((swift_name("PREFIX_BALL7")));
@property (readonly) NSString *PREFIX_BALL7_SEL __attribute__((swift_name("PREFIX_BALL7_SEL")));
@property (readonly) NSString *PREFIX_BALL8 __attribute__((swift_name("PREFIX_BALL8")));
@property (readonly) NSString *PREFIX_BALL8_SEL __attribute__((swift_name("PREFIX_BALL8_SEL")));
@property (readonly) NSString *PREFIX_BALL9 __attribute__((swift_name("PREFIX_BALL9")));
@property (readonly) NSString *PREFIX_BALL9_SEL __attribute__((swift_name("PREFIX_BALL9_SEL")));
@property (readonly) NSString *PREFIX_BOWLING __attribute__((swift_name("PREFIX_BOWLING")));
@property (readonly) NSString *PREFIX_BOWLING_SEL __attribute__((swift_name("PREFIX_BOWLING_SEL")));
@property (readonly) NSString *PREFIX_RABBIT __attribute__((swift_name("PREFIX_RABBIT")));
@property (readonly) NSString *PREFIX_RABBIT_SEL __attribute__((swift_name("PREFIX_RABBIT_SEL")));
@property (readonly) NSString *PREFIX_SOROBAN __attribute__((swift_name("PREFIX_SOROBAN")));
@property (readonly) NSString *PREFIX_SOROBAN_SEL __attribute__((swift_name("PREFIX_SOROBAN_SEL")));
@property (readonly) NSString *PREF_NAME __attribute__((swift_name("PREF_NAME")));
@property (readonly) SharedKotlinArray<NSString *> *RABBIT_COLOR_LABELS __attribute__((swift_name("RABBIT_COLOR_LABELS")));
@property (readonly) SharedAbacusPreferencesBeadColorSpec *RABBIT_COLOR_SPEC __attribute__((swift_name("RABBIT_COLOR_SPEC")));
@property (readonly) SharedKotlinIntArray *RABBIT_DEFAULT_COLORS __attribute__((swift_name("RABBIT_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *RABBIT_SEL_DEFAULT_COLORS __attribute__((swift_name("RABBIT_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *SOROBAN2_COLOR_KEYS __attribute__((swift_name("SOROBAN2_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *SOROBAN2_COLOR_LABELS __attribute__((swift_name("SOROBAN2_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *SOROBAN2_DEFAULT_COLORS __attribute__((swift_name("SOROBAN2_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *SOROBAN2_SEL_DEFAULT_COLORS __attribute__((swift_name("SOROBAN2_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *SOROBAN6_COLOR_KEYS __attribute__((swift_name("SOROBAN6_COLOR_KEYS")));
@property (readonly) SharedKotlinArray<NSString *> *SOROBAN6_COLOR_LABELS __attribute__((swift_name("SOROBAN6_COLOR_LABELS")));
@property (readonly) SharedKotlinIntArray *SOROBAN6_DEFAULT_COLORS __attribute__((swift_name("SOROBAN6_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *SOROBAN6_SEL_DEFAULT_COLORS __attribute__((swift_name("SOROBAN6_SEL_DEFAULT_COLORS")));
@property (readonly) SharedKotlinIntArray *SOROBAN_DEFAULT_COLORS __attribute__((swift_name("SOROBAN_DEFAULT_COLORS")));
@property (readonly) SharedKotlinArray<NSString *> *SOROBAN_PATH_LABELS __attribute__((swift_name("SOROBAN_PATH_LABELS")));
@property (readonly) SharedKotlinIntArray *SOROBAN_SEL_DEFAULT_COLORS __attribute__((swift_name("SOROBAN_SEL_DEFAULT_COLORS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPreferences.BeadColorSpec")))
@interface SharedAbacusPreferencesBeadColorSpec : SharedBase
- (instancetype)initWithPrefix:(NSString *)prefix selPrefix:(NSString *)selPrefix defaults:(SharedKotlinIntArray *)defaults selDefaults:(SharedKotlinIntArray *)selDefaults labels:(SharedKotlinArray<NSString *> *)labels onekli:(BOOL)onekli colorKeys:(SharedKotlinArray<NSString *> *)colorKeys __attribute__((swift_name("init(prefix:selPrefix:defaults:selDefaults:labels:onekli:colorKeys:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinIntArray *)defaultsForSelected:(BOOL)selected __attribute__((swift_name("defaultsFor(selected:)")));
- (NSString *)keySelected:(BOOL)selected index:(int32_t)index __attribute__((swift_name("key(selected:index:)")));
@property (readonly) SharedKotlinArray<NSString *> *colorKeys __attribute__((swift_name("colorKeys")));
@property (readonly) SharedKotlinIntArray *defaults __attribute__((swift_name("defaults")));
@property (readonly) SharedKotlinArray<NSString *> *labels __attribute__((swift_name("labels")));
@property (readonly) BOOL onekli __attribute__((swift_name("onekli")));
@property (readonly) NSString *prefix __attribute__((swift_name("prefix")));
@property (readonly) SharedKotlinIntArray *selDefaults __attribute__((swift_name("selDefaults")));
@property (readonly) NSString *selPrefix __attribute__((swift_name("selPrefix")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPreferences.BeadType")))
@interface SharedAbacusPreferencesBeadType : SharedKotlinEnum<SharedAbacusPreferencesBeadType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAbacusPreferencesBeadType *soroban __attribute__((swift_name("soroban")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal __attribute__((swift_name("animal")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal2 __attribute__((swift_name("animal2")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal3 __attribute__((swift_name("animal3")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal4 __attribute__((swift_name("animal4")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal5 __attribute__((swift_name("animal5")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal6 __attribute__((swift_name("animal6")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal7 __attribute__((swift_name("animal7")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal8 __attribute__((swift_name("animal8")));
@property (class, readonly) SharedAbacusPreferencesBeadType *animal9 __attribute__((swift_name("animal9")));
@property (class, readonly) SharedAbacusPreferencesBeadType *bowling __attribute__((swift_name("bowling")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball1 __attribute__((swift_name("ball1")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball3 __attribute__((swift_name("ball3")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball4 __attribute__((swift_name("ball4")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball5 __attribute__((swift_name("ball5")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball6 __attribute__((swift_name("ball6")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball7 __attribute__((swift_name("ball7")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball8 __attribute__((swift_name("ball8")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball9 __attribute__((swift_name("ball9")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball10 __attribute__((swift_name("ball10")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball11 __attribute__((swift_name("ball11")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball12 __attribute__((swift_name("ball12")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball13 __attribute__((swift_name("ball13")));
@property (class, readonly) SharedAbacusPreferencesBeadType *ball14 __attribute__((swift_name("ball14")));
@property (class, readonly) SharedAbacusPreferencesBeadType *soroban2 __attribute__((swift_name("soroban2")));
@property (class, readonly) SharedAbacusPreferencesBeadType *soroban6 __attribute__((swift_name("soroban6")));
+ (SharedKotlinArray<SharedAbacusPreferencesBeadType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAbacusPreferencesBeadType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPreferences.FrameType")))
@interface SharedAbacusPreferencesFrameType : SharedKotlinEnum<SharedAbacusPreferencesFrameType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAbacusPreferencesFrameType *frameBg __attribute__((swift_name("frameBg")));
@property (class, readonly) SharedAbacusPreferencesFrameType *frameBg2 __attribute__((swift_name("frameBg2")));
@property (class, readonly) SharedAbacusPreferencesFrameType *frameBg3 __attribute__((swift_name("frameBg3")));
@property (class, readonly) SharedAbacusPreferencesFrameType *frameBg4 __attribute__((swift_name("frameBg4")));
+ (SharedKotlinArray<SharedAbacusPreferencesFrameType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAbacusPreferencesFrameType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("AbacusPrefsStore")))
@protocol SharedAbacusPrefsStore
@required
- (int32_t)getIntKey:(NSString *)key defaultValue:(int32_t)defaultValue __attribute__((swift_name("getInt(key:defaultValue:)")));
- (NSString * _Nullable)getStringKey:(NSString *)key defaultValue:(NSString * _Nullable)defaultValue __attribute__((swift_name("getString(key:defaultValue:)")));
- (void)putIntKey:(NSString *)key value:(int32_t)value __attribute__((swift_name("putInt(key:value:)")));
- (void)putStringKey:(NSString *)key value:(NSString *)value __attribute__((swift_name("putString(key:value:)")));
- (void)removeKey:(NSString *)key __attribute__((swift_name("remove(key:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusPreviewMetrics")))
@interface SharedAbacusPreviewMetrics : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusPreviewMetrics __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusPreviewMetrics *shared __attribute__((swift_name("shared")));
- (int32_t)barrierTopDpBarrierHeightDp:(int32_t)barrierHeightDp __attribute__((swift_name("barrierTopDp(barrierHeightDp:)")));
- (int32_t)bottomBeadTopDpIndex:(int32_t)index __attribute__((swift_name("bottomBeadTopDp(index:)")));
- (int32_t)topBeadTopDp __attribute__((swift_name("topBeadTopDp()")));
@property (readonly) int32_t BARRIER_HEIGHT_DP __attribute__((swift_name("BARRIER_HEIGHT_DP")));
@property (readonly) int32_t BARRIER_INNER_COLOR __attribute__((swift_name("BARRIER_INNER_COLOR")));
@property (readonly) int32_t BARRIER_INNER_INSET_DP __attribute__((swift_name("BARRIER_INNER_INSET_DP")));
@property (readonly) int32_t BARRIER_MARGIN_BOTTOM_DP __attribute__((swift_name("BARRIER_MARGIN_BOTTOM_DP")));
@property (readonly) int32_t BARRIER_OUTER_COLOR __attribute__((swift_name("BARRIER_OUTER_COLOR")));
@property (readonly) int32_t BEAD_SIZE_DP __attribute__((swift_name("BEAD_SIZE_DP")));
@property (readonly) int32_t BOTTOM_BEADS_PER_ROD __attribute__((swift_name("BOTTOM_BEADS_PER_ROD")));
@property (readonly) NSArray<SharedInt *> *BOTTOM_BEAD_MARGIN_BOTTOM_DP __attribute__((swift_name("BOTTOM_BEAD_MARGIN_BOTTOM_DP")));
@property (readonly) int32_t FRAME_PADDING_HORIZONTAL_DP __attribute__((swift_name("FRAME_PADDING_HORIZONTAL_DP")));
@property (readonly) int32_t FRAME_PADDING_VERTICAL_DP __attribute__((swift_name("FRAME_PADDING_VERTICAL_DP")));
@property (readonly) int32_t ROD_COUNT __attribute__((swift_name("ROD_COUNT")));
@property (readonly) int32_t ROD_HEIGHT_DP __attribute__((swift_name("ROD_HEIGHT_DP")));
@property (readonly) int32_t ROD_LINE_COLOR __attribute__((swift_name("ROD_LINE_COLOR")));
@property (readonly) int32_t ROD_LINE_WIDTH_DP __attribute__((swift_name("ROD_LINE_WIDTH_DP")));
@property (readonly) int32_t ROD_WIDTH_DP __attribute__((swift_name("ROD_WIDTH_DP")));
@property (readonly) int32_t SLOT_PANEL_BARRIER_HEIGHT_DP __attribute__((swift_name("SLOT_PANEL_BARRIER_HEIGHT_DP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusSoundPlayer")))
@interface SharedAbacusSoundPlayer : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusSoundPlayer __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusSoundPlayer *shared __attribute__((swift_name("shared")));
- (float)playbackRateActivating:(BOOL)activating __attribute__((swift_name("playbackRate(activating:)")));
- (float)playbackVolumeForIndex:(int32_t)index __attribute__((swift_name("playbackVolumeFor(index:)")));
- (int32_t)selectedIndexStoredChoice:(SharedInt * _Nullable)storedChoice __attribute__((swift_name("selectedIndex(storedChoice:)")));
- (NSString *)soundResForIndex:(int32_t)index __attribute__((swift_name("soundResFor(index:)")));
@property (readonly) float ACTIVATE_RATE __attribute__((swift_name("ACTIVATE_RATE")));
@property (readonly) float DEACTIVATE_RATE __attribute__((swift_name("DEACTIVATE_RATE")));
@property (readonly) int32_t DEFAULT_SOUND_INDEX __attribute__((swift_name("DEFAULT_SOUND_INDEX")));
@property (readonly) NSString *KEY_ABACUS_SOUND_CHOICE __attribute__((swift_name("KEY_ABACUS_SOUND_CHOICE")));
@property (readonly) NSString *KEY_SOUND_ENABLED __attribute__((swift_name("KEY_SOUND_ENABLED")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) int32_t SOUND_COUNT __attribute__((swift_name("SOUND_COUNT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusTouchAction")))
@interface SharedAbacusTouchAction : SharedKotlinEnum<SharedAbacusTouchAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAbacusTouchAction *down __attribute__((swift_name("down")));
@property (class, readonly) SharedAbacusTouchAction *move __attribute__((swift_name("move")));
@property (class, readonly) SharedAbacusTouchAction *up __attribute__((swift_name("up")));
@property (class, readonly) SharedAbacusTouchAction *cancel __attribute__((swift_name("cancel")));
@property (class, readonly) SharedAbacusTouchAction *pointerUp __attribute__((swift_name("pointerUp")));
@property (class, readonly) SharedAbacusTouchAction *outside __attribute__((swift_name("outside")));
@property (class, readonly) SharedAbacusTouchAction *other __attribute__((swift_name("other")));
+ (SharedKotlinArray<SharedAbacusTouchAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAbacusTouchAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusUiSettings")))
@interface SharedAbacusUiSettings : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abacusUiSettings __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusUiSettings *shared __attribute__((swift_name("shared")));
- (float)baseBottomMarginDpIdName:(NSString *)idName __attribute__((swift_name("baseBottomMarginDp(idName:)")));
- (int32_t)beadBottomMarginPxIdName:(NSString *)idName marginBottomDp:(float)marginBottomDp density:(float)density __attribute__((swift_name("beadBottomMarginPx(idName:marginBottomDp:density:)")));
- (int32_t)beadTopMarginPxMarginTopDp:(float)marginTopDp density:(float)density __attribute__((swift_name("beadTopMarginPx(marginTopDp:density:)")));
- (int32_t)progressFromMarginDpMarginDp:(float)marginDp max:(int32_t)max __attribute__((swift_name("progressFromMarginDp(marginDp:max:)")));
- (int32_t)progressFromScaleScale:(float)scale __attribute__((swift_name("progressFromScale(scale:)")));
- (float)scaleFromProgressProgress:(int32_t)progress __attribute__((swift_name("scaleFromProgress(progress:)")));
@property (readonly) float DEFAULT_MARGIN_DP __attribute__((swift_name("DEFAULT_MARGIN_DP")));
@property (readonly) float DEFAULT_SCALE __attribute__((swift_name("DEFAULT_SCALE")));
@property (readonly) NSString *KEY_ABACUS_MARGIN_BOTTOM_DP __attribute__((swift_name("KEY_ABACUS_MARGIN_BOTTOM_DP")));
@property (readonly) NSString *KEY_ABACUS_SCALE_X __attribute__((swift_name("KEY_ABACUS_SCALE_X")));
@property (readonly) NSString *KEY_ABACUS_SCALE_Y __attribute__((swift_name("KEY_ABACUS_SCALE_Y")));
@property (readonly) NSString *KEY_BEAD_MARGIN_BOTTOM_DP __attribute__((swift_name("KEY_BEAD_MARGIN_BOTTOM_DP")));
@property (readonly) NSString *KEY_BEAD_MARGIN_TOP_DP __attribute__((swift_name("KEY_BEAD_MARGIN_TOP_DP")));
@property (readonly) NSString *KEY_BEAD_SCALE_X __attribute__((swift_name("KEY_BEAD_SCALE_X")));
@property (readonly) NSString *KEY_BEAD_SCALE_Y __attribute__((swift_name("KEY_BEAD_SCALE_Y")));
@property (readonly) int32_t MAX_PROGRESS_ABACUS_MARGIN_BOTTOM __attribute__((swift_name("MAX_PROGRESS_ABACUS_MARGIN_BOTTOM")));
@property (readonly) int32_t MAX_PROGRESS_BEAD_MARGIN_BOTTOM __attribute__((swift_name("MAX_PROGRESS_BEAD_MARGIN_BOTTOM")));
@property (readonly) int32_t MAX_PROGRESS_BEAD_MARGIN_TOP __attribute__((swift_name("MAX_PROGRESS_BEAD_MARGIN_TOP")));
@property (readonly) int32_t MAX_PROGRESS_SCALE __attribute__((swift_name("MAX_PROGRESS_SCALE")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusBoncukKoprusu")))
@interface SharedAbakusBoncukKoprusu : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abakusBoncukKoprusu __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbakusBoncukKoprusu *shared __attribute__((swift_name("shared")));
- (SharedAbacusBead *)altBoncukDenetleyici:(SharedAbacusBeadController *)denetleyici rod:(int32_t)rod beadIndex:(int32_t)beadIndex __attribute__((swift_name("altBoncuk(denetleyici:rod:beadIndex:)")));
- (void)degeriYerlestirDenetleyici:(SharedAbacusBeadController *)denetleyici deger:(int32_t)deger __attribute__((swift_name("degeriYerlestir(denetleyici:deger:)")));
- (SharedAbacusBeadController *)denetleyiciOlusturHost:(id<SharedAbacusBeadHost>)host yogunluk:(float)yogunluk animasyonSuresiMs:(int64_t)animasyonSuresiMs __attribute__((swift_name("denetleyiciOlustur(host:yogunluk:animasyonSuresiMs:)")));
- (BOOL)dokunmaDenetleyici:(SharedAbacusBeadController *)denetleyici boncuk:(SharedAbacusBead *)boncuk eylemKodu:(int32_t)eylemKodu rawY:(float)rawY eventTimeMs:(int64_t)eventTimeMs __attribute__((swift_name("dokunma(denetleyici:boncuk:eylemKodu:rawY:eventTimeMs:)")));
- (SharedAbacusTouchAction *)eylemKod:(int32_t)kod __attribute__((swift_name("eylem(kod:)")));
- (BOOL)mesafeleriOlcDenetleyici:(SharedAbacusBeadController *)denetleyici topBeadY:(int32_t)topBeadY topBeadHeight:(int32_t)topBeadHeight bottom1Y:(int32_t)bottom1Y barrierY:(int32_t)barrierY barrierHeight:(int32_t)barrierHeight __attribute__((swift_name("mesafeleriOlc(denetleyici:topBeadY:topBeadHeight:bottom1Y:barrierY:barrierHeight:)")));
- (SharedAbacusBead *)ustBoncukDenetleyici:(SharedAbacusBeadController *)denetleyici rod:(int32_t)rod __attribute__((swift_name("ustBoncuk(denetleyici:rod:)")));
- (int32_t)yuvaKoduBoncuk:(SharedAbacusBead *)boncuk __attribute__((swift_name("yuvaKodu(boncuk:)")));
@property (readonly) int32_t EYLEM_CANCEL __attribute__((swift_name("EYLEM_CANCEL")));
@property (readonly) int32_t EYLEM_DOWN __attribute__((swift_name("EYLEM_DOWN")));
@property (readonly) int32_t EYLEM_MOVE __attribute__((swift_name("EYLEM_MOVE")));
@property (readonly) int32_t EYLEM_UP __attribute__((swift_name("EYLEM_UP")));
@property (readonly) float TOUCH_SLOP_DP __attribute__((swift_name("TOUCH_SLOP_DP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadData")))
@interface SharedBeadData : SharedBase
- (instancetype)initWithCount:(int32_t)count colorFeatureActive:(BOOL)colorFeatureActive __attribute__((swift_name("init(count:colorFeatureActive:)"))) __attribute__((objc_designated_initializer));
- (SharedBeadData *)doCopyCount:(int32_t)count colorFeatureActive:(BOOL)colorFeatureActive __attribute__((swift_name("doCopy(count:colorFeatureActive:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL colorFeatureActive __attribute__((swift_name("colorFeatureActive")));
@property (readonly) int32_t count __attribute__((swift_name("count")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadPurchaseFirestore")))
@interface SharedBeadPurchaseFirestore : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)beadPurchaseFirestore __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBeadPurchaseFirestore *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)beadCountDataCount:(int32_t)count __attribute__((swift_name("beadCountData(count:)")));
- (SharedBeadData *)beadDataFromDocumentData:(NSDictionary<NSString *, id> *)data __attribute__((swift_name("beadDataFromDocument(data:)")));
- (NSString *)collectionPathUid:(NSString *)uid __attribute__((swift_name("collectionPath(uid:)")));
- (NSDictionary<NSString *, id> *)colorFeatureActiveData __attribute__((swift_name("colorFeatureActiveData()")));
- (NSString *)docPathUid:(NSString *)uid beadId:(NSString *)beadId __attribute__((swift_name("docPath(uid:beadId:)")));
- (NSDictionary<NSString *, SharedBeadData *> *)ownedBeadsFromDocumentsDocs:(NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)docs __attribute__((swift_name("ownedBeadsFromDocuments(docs:)")));
@property (readonly) NSString *COLLECTION_HAVE_BEAD __attribute__((swift_name("COLLECTION_HAVE_BEAD")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *FIELD_COLOR_FEATURE __attribute__((swift_name("FIELD_COLOR_FEATURE")));
@property (readonly) NSString *FIELD_COUNT __attribute__((swift_name("FIELD_COUNT")));
@property (readonly) NSString *FIELD_PURCHASED_AT __attribute__((swift_name("FIELD_PURCHASED_AT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BellekAbacusPrefsStore")))
@interface SharedBellekAbacusPrefsStore : SharedBase <SharedAbacusPrefsStore>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (int32_t)getIntKey:(NSString *)key defaultValue:(int32_t)defaultValue __attribute__((swift_name("getInt(key:defaultValue:)")));
- (NSString * _Nullable)getStringKey:(NSString *)key defaultValue:(NSString * _Nullable)defaultValue __attribute__((swift_name("getString(key:defaultValue:)")));
- (void)putIntKey:(NSString *)key value:(int32_t)value __attribute__((swift_name("putInt(key:value:)")));
- (void)putStringKey:(NSString *)key value:(NSString *)value __attribute__((swift_name("putString(key:value:)")));
- (void)removeKey:(NSString *)key __attribute__((swift_name("remove(key:)")));
@property (readonly) SharedMutableDictionary<NSString *, id> *degerler __attribute__((swift_name("degerler")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingLessonFlags")))
@interface SharedBlindingLessonFlags : SharedBase
- (instancetype)initWithType:(int32_t)type isBlinding:(SharedBoolean * _Nullable)isBlinding isMultiplication:(SharedBoolean * _Nullable)isMultiplication blindingMultiplication:(SharedBoolean * _Nullable)blindingMultiplication isExtraction:(SharedBoolean * _Nullable)isExtraction raceBusyLevel:(SharedInt * _Nullable)raceBusyLevel timePeriod:(SharedLong * _Nullable)timePeriod cupWinDelta:(SharedInt * _Nullable)cupWinDelta cupLossDelta:(SharedInt * _Nullable)cupLossDelta __attribute__((swift_name("init(type:isBlinding:isMultiplication:blindingMultiplication:isExtraction:raceBusyLevel:timePeriod:cupWinDelta:cupLossDelta:)"))) __attribute__((objc_designated_initializer));
- (SharedBlindingLessonFlags *)doCopyType:(int32_t)type isBlinding:(SharedBoolean * _Nullable)isBlinding isMultiplication:(SharedBoolean * _Nullable)isMultiplication blindingMultiplication:(SharedBoolean * _Nullable)blindingMultiplication isExtraction:(SharedBoolean * _Nullable)isExtraction raceBusyLevel:(SharedInt * _Nullable)raceBusyLevel timePeriod:(SharedLong * _Nullable)timePeriod cupWinDelta:(SharedInt * _Nullable)cupWinDelta cupLossDelta:(SharedInt * _Nullable)cupLossDelta __attribute__((swift_name("doCopy(type:isBlinding:isMultiplication:blindingMultiplication:isExtraction:raceBusyLevel:timePeriod:cupWinDelta:cupLossDelta:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedBoolean * _Nullable blindingMultiplication __attribute__((swift_name("blindingMultiplication")));
@property (readonly) SharedInt * _Nullable cupLossDelta __attribute__((swift_name("cupLossDelta")));
@property (readonly) SharedInt * _Nullable cupWinDelta __attribute__((swift_name("cupWinDelta")));
@property (readonly) SharedBoolean * _Nullable isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) SharedBoolean * _Nullable isExtraction __attribute__((swift_name("isExtraction")));
@property (readonly) SharedBoolean * _Nullable isMultiplication __attribute__((swift_name("isMultiplication")));
@property (readonly) SharedInt * _Nullable raceBusyLevel __attribute__((swift_name("raceBusyLevel")));
@property (readonly) SharedLong * _Nullable timePeriod __attribute__((swift_name("timePeriod")));
@property (readonly) int32_t type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingLessonOutcome")))
@interface SharedBlindingLessonOutcome : SharedKotlinEnum<SharedBlindingLessonOutcome *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedBlindingLessonOutcome *dailyQuestionComplete __attribute__((swift_name("dailyQuestionComplete")));
@property (class, readonly) SharedBlindingLessonOutcome *close __attribute__((swift_name("close")));
@property (class, readonly) SharedBlindingLessonOutcome *chestResult __attribute__((swift_name("chestResult")));
@property (class, readonly) SharedBlindingLessonOutcome *chestFailure __attribute__((swift_name("chestFailure")));
@property (class, readonly) SharedBlindingLessonOutcome *lessonResult __attribute__((swift_name("lessonResult")));
@property (class, readonly) SharedBlindingLessonOutcome *lessonResultFalse __attribute__((swift_name("lessonResultFalse")));
+ (SharedKotlinArray<SharedBlindingLessonOutcome *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBlindingLessonOutcome *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingLessonRules")))
@interface SharedBlindingLessonRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)blindingLessonRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBlindingLessonRules *shared __attribute__((swift_name("shared")));
- (int32_t)cupDeltaFlags:(SharedBlindingLessonFlags *)flags isCorrect:(BOOL)isCorrect __attribute__((swift_name("cupDelta(flags:isCorrect:)")));
- (SharedCupDeltaTarget *)cupDeltaTargetFlags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("cupDeltaTarget(flags:)")));
- (NSString *)cupModeNameFlags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("cupModeName(flags:)")));
- (SharedBlindingLessonOutcome *)finishLessonAfterLastQuestionIsDailyQuestionMode:(BOOL)isDailyQuestionMode globalPartId:(int32_t)globalPartId flags:(SharedBlindingLessonFlags *)flags correctAnswer:(int32_t)correctAnswer totalQuestions:(int32_t)totalQuestions toplamPuan:(int32_t)toplamPuan __attribute__((swift_name("finishLessonAfterLastQuestion(isDailyQuestionMode:globalPartId:flags:correctAnswer:totalQuestions:toplamPuan:)")));
- (BOOL)isDailyQuestionSessionPeriodValidSessionPeriodKey:(NSString * _Nullable)sessionPeriodKey currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("isDailyQuestionSessionPeriodValid(sessionPeriodKey:currentPeriodKey:)")));
- (BOOL)isHintHiddenGlobalPartId:(int32_t)globalPartId __attribute__((swift_name("isHintHidden(globalPartId:)")));
- (BOOL)isLessonSuccessRateScopeIsDailyQuestionMode:(BOOL)isDailyQuestionMode globalPartId:(int32_t)globalPartId lessonType:(int32_t)lessonType __attribute__((swift_name("isLessonSuccessRateScope(isDailyQuestionMode:globalPartId:lessonType:)")));
- (BOOL)isProgressBarHiddenGlobalPartId:(int32_t)globalPartId __attribute__((swift_name("isProgressBarHidden(globalPartId:)")));
- (BOOL)isRacePanelLessonFlags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("isRacePanelLesson(flags:)")));
- (BOOL)isTimerVisibleLessonType:(int32_t)lessonType globalPartId:(int32_t)globalPartId __attribute__((swift_name("isTimerVisible(lessonType:globalPartId:)")));
- (SharedBlindingLessonRulesResultFalseAction *)lessonResultFalseActionGlobalPartId:(int32_t)globalPartId flags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("lessonResultFalseAction(globalPartId:flags:)")));
- (BOOL)raceLessonPassedCorrectAnswer:(int32_t)correctAnswer totalQuestions:(int32_t)totalQuestions __attribute__((swift_name("raceLessonPassed(correctAnswer:totalQuestions:)")));
- (BOOL)shouldResetAbacusAfterAnswerFlags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("shouldResetAbacusAfterAnswer(flags:)")));
- (BOOL)shouldStartTimerLessonType:(int32_t)lessonType isTimerStarted:(BOOL)isTimerStarted __attribute__((swift_name("shouldStartTimer(lessonType:isTimerStarted:)")));
- (BOOL)shouldSubmitCupQuitResultGlobalPartId:(int32_t)globalPartId lessonStarted:(BOOL)lessonStarted cupResultSubmitted:(BOOL)cupResultSubmitted __attribute__((swift_name("shouldSubmitCupQuitResult(globalPartId:lessonStarted:cupResultSubmitted:)")));
- (BOOL)usesRulesTablePickerGlobalPartId:(int32_t)globalPartId isDailyQuestionMode:(BOOL)isDailyQuestionMode __attribute__((swift_name("usesRulesTablePicker(globalPartId:isDailyQuestionMode:)")));
- (SharedBlindingWrongAnswerNext *)wrongAnswerNextGlobalPartId:(int32_t)globalPartId isDailyQuestionMode:(BOOL)isDailyQuestionMode flags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("wrongAnswerNext(globalPartId:isDailyQuestionMode:flags:)")));
@property (readonly) int32_t CUP_PART_ID __attribute__((swift_name("CUP_PART_ID")));
@property (readonly) int32_t DEFAULT_CUP_LOSS_DELTA __attribute__((swift_name("DEFAULT_CUP_LOSS_DELTA")));
@property (readonly) int32_t DEFAULT_CUP_WIN_DELTA __attribute__((swift_name("DEFAULT_CUP_WIN_DELTA")));
@property (readonly) int64_t DEFAULT_TIME_PERIOD_MS __attribute__((swift_name("DEFAULT_TIME_PERIOD_MS")));
@property (readonly) float PART_4_5_PASS_MIN_SUCCESS_RATE __attribute__((swift_name("PART_4_5_PASS_MIN_SUCCESS_RATE")));
@property (readonly) int64_t SEQUENCE_BLANK_MS __attribute__((swift_name("SEQUENCE_BLANK_MS")));
@property (readonly) int64_t SEQUENCE_RESTART_DELAY_MS __attribute__((swift_name("SEQUENCE_RESTART_DELAY_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingLessonRules.ResultFalseAction")))
@interface SharedBlindingLessonRulesResultFalseAction : SharedKotlinEnum<SharedBlindingLessonRulesResultFalseAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedBlindingLessonRulesResultFalseAction *cupUseEnergyAndClose __attribute__((swift_name("cupUseEnergyAndClose")));
@property (class, readonly) SharedBlindingLessonRulesResultFalseAction *close __attribute__((swift_name("close")));
@property (class, readonly) SharedBlindingLessonRulesResultFalseAction *showResultFalse __attribute__((swift_name("showResultFalse")));
+ (SharedKotlinArray<SharedBlindingLessonRulesResultFalseAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBlindingLessonRulesResultFalseAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingLessonSession")))
@interface SharedBlindingLessonSession : SharedBase
- (instancetype)initWithOperations:(NSArray<SharedBlindingOperation *> *)operations flags:(SharedBlindingLessonFlags *)flags __attribute__((swift_name("init(operations:flags:)"))) __attribute__((objc_designated_initializer));
- (BOOL)claimCupRaceLog __attribute__((swift_name("claimCupRaceLog()")));
- (BOOL)claimCupResult __attribute__((swift_name("claimCupResult()")));
- (BOOL)claimLessonFinish __attribute__((swift_name("claimLessonFinish()")));
- (void)clearControlNumber __attribute__((swift_name("clearControlNumber()")));
- (BOOL)stepAnswerAlgorithmInputText:(NSString *)inputText abacusValue:(int32_t)abacusValue currentSequenceIndex:(int32_t)currentSequenceIndex __attribute__((swift_name("stepAnswerAlgorithm(inputText:abacusValue:currentSequenceIndex:)")));
@property (readonly) int32_t answerNumber __attribute__((swift_name("answerNumber")));
@property (readonly) int32_t controlNumber __attribute__((swift_name("controlNumber")));
@property int32_t correctAnswer __attribute__((swift_name("correctAnswer")));
@property (readonly) NSString *correctAnswerLabel __attribute__((swift_name("correctAnswerLabel")));
@property (readonly) NSString *correctAnswerText __attribute__((swift_name("correctAnswerText")));
@property (readonly) BOOL cupResultLogged __attribute__((swift_name("cupResultLogged")));
@property (readonly) BOOL cupResultSubmitted __attribute__((swift_name("cupResultSubmitted")));
@property int32_t currentIndex __attribute__((swift_name("currentIndex")));
@property (readonly) SharedBlindingLessonFlags *flags __attribute__((swift_name("flags")));
@property (readonly) BOOL lessonFinishStarted __attribute__((swift_name("lessonFinishStarted")));
@property (readonly) NSArray<SharedBlindingOperation *> *operations __attribute__((swift_name("operations")));
@property (readonly) int32_t totalQuestions __attribute__((swift_name("totalQuestions")));
@end

__attribute__((swift_name("BlindingOperation")))
@interface SharedBlindingOperation : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingOperation.Math")))
@interface SharedBlindingOperationMath : SharedBlindingOperation
- (instancetype)initWithFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("init(firstNumber:operator:secondNumber:)"))) __attribute__((objc_designated_initializer));
- (SharedBlindingOperationMath *)doCopyFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("doCopy(firstNumber:operator:secondNumber:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable firstNumber __attribute__((swift_name("firstNumber")));
@property (readonly, getter=operator) NSString * _Nullable operator_ __attribute__((swift_name("operator_")));
@property (readonly) SharedInt * _Nullable secondNumber __attribute__((swift_name("secondNumber")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingOperation.Other")))
@interface SharedBlindingOperationOther : SharedBlindingOperation
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)other __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBlindingOperationOther *shared __attribute__((swift_name("shared")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingOperation.Sequence")))
@interface SharedBlindingOperationSequence : SharedBlindingOperation
- (instancetype)initWithNumbers:(NSArray<SharedInt *> *)numbers __attribute__((swift_name("init(numbers:)"))) __attribute__((objc_designated_initializer));
- (SharedBlindingOperationSequence *)doCopyNumbers:(NSArray<SharedInt *> *)numbers __attribute__((swift_name("doCopy(numbers:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedInt *> *numbers __attribute__((swift_name("numbers")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingSequencePlayback")))
@interface SharedBlindingSequencePlayback : SharedBase
- (instancetype)initWithTimePeriod:(SharedLong * _Nullable)timePeriod __attribute__((swift_name("init(timePeriod:)"))) __attribute__((objc_designated_initializer));
- (SharedBlindingSequencePlaybackStep *)onSequenceRevealStep __attribute__((swift_name("onSequenceRevealStep()")));
- (SharedBlindingSequencePlaybackStep *)onShowNextNumberStep __attribute__((swift_name("onShowNextNumberStep()")));
- (SharedBlindingSequencePlaybackStep * _Nullable)skip __attribute__((swift_name("skip()")));
- (SharedBlindingSequencePlaybackStep *)startSequence:(NSArray<SharedInt *> *)sequence __attribute__((swift_name("start(sequence:)")));
- (void)stop __attribute__((swift_name("stop()")));
@property (readonly) NSArray<SharedInt *> *currentSequence __attribute__((swift_name("currentSequence")));
@property (readonly) int32_t currentSequenceIndex __attribute__((swift_name("currentSequenceIndex")));
@property (readonly) BOOL isBlankPending __attribute__((swift_name("isBlankPending")));
@property (readonly) BOOL isShowingSequence __attribute__((swift_name("isShowingSequence")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingSequencePlayback.Step")))
@interface SharedBlindingSequencePlaybackStep : SharedBase
- (instancetype)initWithDisplay:(NSString * _Nullable)display schedule:(SharedBlindingSequencePlaybackTimer * _Nullable)schedule delayMs:(int64_t)delayMs finished:(BOOL)finished disableControl:(BOOL)disableControl __attribute__((swift_name("init(display:schedule:delayMs:finished:disableControl:)"))) __attribute__((objc_designated_initializer));
- (SharedBlindingSequencePlaybackStep *)doCopyDisplay:(NSString * _Nullable)display schedule:(SharedBlindingSequencePlaybackTimer * _Nullable)schedule delayMs:(int64_t)delayMs finished:(BOOL)finished disableControl:(BOOL)disableControl __attribute__((swift_name("doCopy(display:schedule:delayMs:finished:disableControl:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t delayMs __attribute__((swift_name("delayMs")));
@property (readonly) BOOL disableControl __attribute__((swift_name("disableControl")));
@property (readonly) NSString * _Nullable display __attribute__((swift_name("display")));
@property (readonly) BOOL finished __attribute__((swift_name("finished")));
@property (readonly) SharedBlindingSequencePlaybackTimer * _Nullable schedule __attribute__((swift_name("schedule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingSequencePlayback.Timer")))
@interface SharedBlindingSequencePlaybackTimer : SharedKotlinEnum<SharedBlindingSequencePlaybackTimer *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedBlindingSequencePlaybackTimer *showNextNumber __attribute__((swift_name("showNextNumber")));
@property (class, readonly) SharedBlindingSequencePlaybackTimer *sequenceReveal __attribute__((swift_name("sequenceReveal")));
+ (SharedKotlinArray<SharedBlindingSequencePlaybackTimer *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBlindingSequencePlaybackTimer *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingWrongAnswerNext")))
@interface SharedBlindingWrongAnswerNext : SharedKotlinEnum<SharedBlindingWrongAnswerNext *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedBlindingWrongAnswerNext *cupResultFalse __attribute__((swift_name("cupResultFalse")));
@property (class, readonly) SharedBlindingWrongAnswerNext *dailyQuestionPanel __attribute__((swift_name("dailyQuestionPanel")));
@property (class, readonly) SharedBlindingWrongAnswerNext *raceQuestionPanel __attribute__((swift_name("raceQuestionPanel")));
@property (class, readonly) SharedBlindingWrongAnswerNext *nextQuestion __attribute__((swift_name("nextQuestion")));
+ (SharedKotlinArray<SharedBlindingWrongAnswerNext *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBlindingWrongAnswerNext *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupDeltaTarget")))
@interface SharedCupDeltaTarget : SharedKotlinEnum<SharedCupDeltaTarget *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCupDeltaTarget *pendingBlindingImpactCupDelta __attribute__((swift_name("pendingBlindingImpactCupDelta")));
@property (class, readonly) SharedCupDeltaTarget *pendingImpactCupDelta __attribute__((swift_name("pendingImpactCupDelta")));
@property (class, readonly) SharedCupDeltaTarget *pendingBlindingExtractionCupDelta __attribute__((swift_name("pendingBlindingExtractionCupDelta")));
@property (class, readonly) SharedCupDeltaTarget *pendingExtractionCupDelta __attribute__((swift_name("pendingExtractionCupDelta")));
@property (class, readonly) SharedCupDeltaTarget *pendingBlindingCupDelta __attribute__((swift_name("pendingBlindingCupDelta")));
@property (class, readonly) SharedCupDeltaTarget *pendingCupDelta __attribute__((swift_name("pendingCupDelta")));
+ (SharedKotlinArray<SharedCupDeltaTarget *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCupDeltaTarget *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FrameData")))
@interface SharedFrameData : SharedBase
- (instancetype)initWithOwned:(BOOL)owned colorFeatureActive:(BOOL)colorFeatureActive __attribute__((swift_name("init(owned:colorFeatureActive:)"))) __attribute__((objc_designated_initializer));
- (SharedFrameData *)doCopyOwned:(BOOL)owned colorFeatureActive:(BOOL)colorFeatureActive __attribute__((swift_name("doCopy(owned:colorFeatureActive:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL colorFeatureActive __attribute__((swift_name("colorFeatureActive")));
@property (readonly) BOOL owned __attribute__((swift_name("owned")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FramePurchaseFirestore")))
@interface SharedFramePurchaseFirestore : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)framePurchaseFirestore __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFramePurchaseFirestore *shared __attribute__((swift_name("shared")));
- (NSString *)collectionPathUid:(NSString *)uid __attribute__((swift_name("collectionPath(uid:)")));
- (NSDictionary<NSString *, id> *)colorFeatureActiveData __attribute__((swift_name("colorFeatureActiveData()")));
- (NSString *)docPathUid:(NSString *)uid frameType:(SharedAbacusPreferencesFrameType *)frameType __attribute__((swift_name("docPath(uid:frameType:)")));
- (SharedFrameData *)frameDataFromDocumentData:(NSDictionary<NSString *, id> *)data __attribute__((swift_name("frameDataFromDocument(data:)")));
- (NSDictionary<NSString *, SharedFrameData *> *)ownedFramesFromDocumentsDocs:(NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)docs __attribute__((swift_name("ownedFramesFromDocuments(docs:)")));
@property (readonly) NSString *COLLECTION_HAVE_FRAME __attribute__((swift_name("COLLECTION_HAVE_FRAME")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *FIELD_COLOR_FEATURE __attribute__((swift_name("FIELD_COLOR_FEATURE")));
@property (readonly) NSString *FIELD_PURCHASED_AT __attribute__((swift_name("FIELD_PURCHASED_AT")));
@end

__attribute__((swift_name("MissionCloudGateway")))
@protocol SharedMissionCloudGateway
@required
- (NSString * _Nullable)currentUid __attribute__((swift_name("currentUid()")));
- (void)fetchStateUid:(NSString *)uid onSuccess:(void (^)(NSDictionary<NSString *, id> * _Nullable))onSuccess onFailure:(void (^)(void))onFailure __attribute__((swift_name("fetchState(uid:onSuccess:onFailure:)")));
- (NSDictionary<NSString *, id> * _Nullable)fetchStateBlockingUid:(NSString *)uid __attribute__((swift_name("fetchStateBlocking(uid:)")));
- (void (^)(void))listenStateUid:(NSString *)uid onSnapshot:(void (^)(NSDictionary<NSString *, id> * _Nullable))onSnapshot __attribute__((swift_name("listenState(uid:onSnapshot:)")));
- (void)setStateUid:(NSString *)uid payload:(NSDictionary<NSString *, id> *)payload __attribute__((swift_name("setState(uid:payload:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BellekIciGorevBulutu")))
@interface SharedBellekIciGorevBulutu : SharedBase <SharedMissionCloudGateway>
- (instancetype)initWithUid:(NSString * _Nullable)uid baslangicBelgesi:(NSDictionary<NSString *, id> * _Nullable)baslangicBelgesi __attribute__((swift_name("init(uid:baslangicBelgesi:)"))) __attribute__((objc_designated_initializer));
- (NSString * _Nullable)currentUid __attribute__((swift_name("currentUid()")));
- (void)fetchStateUid:(NSString *)uid onSuccess:(void (^)(NSDictionary<NSString *, id> * _Nullable))onSuccess onFailure:(void (^)(void))onFailure __attribute__((swift_name("fetchState(uid:onSuccess:onFailure:)")));
- (NSDictionary<NSString *, id> * _Nullable)fetchStateBlockingUid:(NSString *)uid __attribute__((swift_name("fetchStateBlocking(uid:)")));
- (void (^)(void))listenStateUid:(NSString *)uid onSnapshot:(void (^)(NSDictionary<NSString *, id> * _Nullable))onSnapshot __attribute__((swift_name("listenState(uid:onSnapshot:)")));
- (void)setStateUid:(NSString *)uid payload:(NSDictionary<NSString *, id> *)payload __attribute__((swift_name("setState(uid:payload:)")));
@property (readonly) NSDictionary<NSString *, id> * _Nullable belge __attribute__((swift_name("belge")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BellekIciGunlukSoruDeposu")))
@interface SharedBellekIciGunlukSoruDeposu : SharedBase
- (instancetype)initWithUid:(NSString * _Nullable)uid builder:(SharedDailyQuestionPoolBuilder *)builder ornekBasariOrani:(SharedInt * _Nullable)ornekBasariOrani nowMs:(SharedLong *(^)(void))nowMs __attribute__((swift_name("init(uid:builder:ornekBasariOrani:nowMs:)"))) __attribute__((objc_designated_initializer));
- (BOOL)clearPendingDiamondContinuePeriodKey:(NSString *)periodKey __attribute__((swift_name("clearPendingDiamondContinue(periodKey:)")));
- (int32_t)incrementSolvedCountPeriodKey:(NSString *)periodKey __attribute__((swift_name("incrementSolvedCount(periodKey:)")));
- (SharedDailyQuestionCardUiState *)loadChallengeForCardSources:(NSArray<SharedDailyQuestionSource *> *)sources __attribute__((swift_name("loadChallengeForCard(sources:)")));
- (SharedDailyQuestionChallenge * _Nullable)loadOrCreateChallengeSources:(NSArray<SharedDailyQuestionSource *> *)sources __attribute__((swift_name("loadOrCreateChallenge(sources:)")));
- (BOOL)markPendingDiamondContinuePeriodKey:(NSString *)periodKey slotIndex:(int32_t)slotIndex __attribute__((swift_name("markPendingDiamondContinue(periodKey:slotIndex:)")));
- (BOOL)markRewardClaimedPeriodKey:(NSString *)periodKey __attribute__((swift_name("markRewardClaimed(periodKey:)")));
@property (readonly) NSDictionary<NSString *, id> * _Nullable belge __attribute__((swift_name("belge")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GorevEkraniDurumu")))
@interface SharedGorevEkraniDurumu : SharedBase
- (instancetype)initWithWeekly:(NSArray<SharedMissionQuestData *> *)weekly daily:(NSArray<SharedMissionQuestData *> *)daily weeklyCountdown:(SharedMissionCountdownLabel *)weeklyCountdown dailyCountdown:(SharedMissionCountdownLabel *)dailyCountdown __attribute__((swift_name("init(weekly:daily:weeklyCountdown:dailyCountdown:)"))) __attribute__((objc_designated_initializer));
- (SharedGorevEkraniDurumu *)doCopyWeekly:(NSArray<SharedMissionQuestData *> *)weekly daily:(NSArray<SharedMissionQuestData *> *)daily weeklyCountdown:(SharedMissionCountdownLabel *)weeklyCountdown dailyCountdown:(SharedMissionCountdownLabel *)dailyCountdown __attribute__((swift_name("doCopy(weekly:daily:weeklyCountdown:dailyCountdown:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedMissionQuestData *> *daily __attribute__((swift_name("daily")));
@property (readonly) SharedMissionCountdownLabel *dailyCountdown __attribute__((swift_name("dailyCountdown")));
@property (readonly) NSArray<SharedMissionQuestData *> *weekly __attribute__((swift_name("weekly")));
@property (readonly) SharedMissionCountdownLabel *weeklyCountdown __attribute__((swift_name("weeklyCountdown")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GunlukSoruBaslatma")))
@interface SharedGunlukSoruBaslatma : SharedBase
- (instancetype)initWithSequence:(NSArray<SharedInt *> *)sequence mathFirst:(SharedInt * _Nullable)mathFirst mathOperator:(NSString * _Nullable)mathOperator mathSecond:(SharedInt * _Nullable)mathSecond periodKey:(NSString *)periodKey slotIndex:(int32_t)slotIndex displayIntervalMs:(SharedLong * _Nullable)displayIntervalMs partId:(int32_t)partId __attribute__((swift_name("init(sequence:mathFirst:mathOperator:mathSecond:periodKey:slotIndex:displayIntervalMs:partId:)"))) __attribute__((objc_designated_initializer));
- (SharedGunlukSoruBaslatma *)doCopySequence:(NSArray<SharedInt *> *)sequence mathFirst:(SharedInt * _Nullable)mathFirst mathOperator:(NSString * _Nullable)mathOperator mathSecond:(SharedInt * _Nullable)mathSecond periodKey:(NSString *)periodKey slotIndex:(int32_t)slotIndex displayIntervalMs:(SharedLong * _Nullable)displayIntervalMs partId:(int32_t)partId __attribute__((swift_name("doCopy(sequence:mathFirst:mathOperator:mathSecond:periodKey:slotIndex:displayIntervalMs:partId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable displayIntervalMs __attribute__((swift_name("displayIntervalMs")));
@property (readonly) SharedInt * _Nullable mathFirst __attribute__((swift_name("mathFirst")));
@property (readonly) NSString * _Nullable mathOperator __attribute__((swift_name("mathOperator")));
@property (readonly) SharedInt * _Nullable mathSecond __attribute__((swift_name("mathSecond")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) NSString *periodKey __attribute__((swift_name("periodKey")));
@property (readonly) NSArray<SharedInt *> *sequence __attribute__((swift_name("sequence")));
@property (readonly) int32_t slotIndex __attribute__((swift_name("slotIndex")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GunlukSoruCubukGorunumu")))
@interface SharedGunlukSoruCubukGorunumu : SharedBase
- (instancetype)initWithMode:(SharedGunlukSoruCubukModu *)mode trackDrawable:(NSString *)trackDrawable overlay:(SharedMissionProgressOverlay * _Nullable)overlay labelResKey:(NSString *)labelResKey labelCurrent:(int32_t)labelCurrent labelTotal:(int32_t)labelTotal labelColor:(NSString *)labelColor __attribute__((swift_name("init(mode:trackDrawable:overlay:labelResKey:labelCurrent:labelTotal:labelColor:)"))) __attribute__((objc_designated_initializer));
- (SharedGunlukSoruCubukGorunumu *)doCopyMode:(SharedGunlukSoruCubukModu *)mode trackDrawable:(NSString *)trackDrawable overlay:(SharedMissionProgressOverlay * _Nullable)overlay labelResKey:(NSString *)labelResKey labelCurrent:(int32_t)labelCurrent labelTotal:(int32_t)labelTotal labelColor:(NSString *)labelColor __attribute__((swift_name("doCopy(mode:trackDrawable:overlay:labelResKey:labelCurrent:labelTotal:labelColor:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *labelColor __attribute__((swift_name("labelColor")));
@property (readonly) int32_t labelCurrent __attribute__((swift_name("labelCurrent")));
@property (readonly) NSString *labelResKey __attribute__((swift_name("labelResKey")));
@property (readonly) int32_t labelTotal __attribute__((swift_name("labelTotal")));
@property (readonly) SharedGunlukSoruCubukModu *mode __attribute__((swift_name("mode")));
@property (readonly) SharedMissionProgressOverlay * _Nullable overlay __attribute__((swift_name("overlay")));
@property (readonly) NSString *trackDrawable __attribute__((swift_name("trackDrawable")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GunlukSoruCubukModu")))
@interface SharedGunlukSoruCubukModu : SharedKotlinEnum<SharedGunlukSoruCubukModu *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedGunlukSoruCubukModu *claimed __attribute__((swift_name("claimed")));
@property (class, readonly) SharedGunlukSoruCubukModu *claimReady __attribute__((swift_name("claimReady")));
@property (class, readonly) SharedGunlukSoruCubukModu *progress __attribute__((swift_name("progress")));
+ (SharedKotlinArray<SharedGunlukSoruCubukModu *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedGunlukSoruCubukModu *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KorDersAdimi")))
@interface SharedKorDersAdimi : SharedBase
@property (class, readonly, getter=companion) SharedKorDersAdimiCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL bitti __attribute__((swift_name("bitti")));
@property (readonly) int64_t gecikmeMs __attribute__((swift_name("gecikmeMs")));
@property (readonly) BOOL kontrolKapat __attribute__((swift_name("kontrolKapat")));
@property (readonly) NSString * _Nullable metin __attribute__((swift_name("metin")));
@property (readonly) int32_t zamanlayici __attribute__((swift_name("zamanlayici")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KorDersAdimi.Companion")))
@interface SharedKorDersAdimiCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKorDersAdimiCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t ZAMANLAYICI_SAYI_GOSTER __attribute__((swift_name("ZAMANLAYICI_SAYI_GOSTER")));
@property (readonly) int32_t ZAMANLAYICI_SONRAKI_SAYI __attribute__((swift_name("ZAMANLAYICI_SONRAKI_SAYI")));
@property (readonly) int32_t ZAMANLAYICI_YOK __attribute__((swift_name("ZAMANLAYICI_YOK")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KorDersKurucu")))
@interface SharedKorDersKurucu : SharedBase
- (instancetype)initWithGlobalPartId:(int32_t)globalPartId lessonType:(int32_t)lessonType isDailyQuestionMode:(BOOL)isDailyQuestionMode mapFragmentStepIndex:(int32_t)mapFragmentStepIndex __attribute__((swift_name("init(globalPartId:lessonType:isDailyQuestionMode:mapFragmentStepIndex:)"))) __attribute__((objc_designated_initializer));
- (void)carpmaAyarlaDeger:(BOOL)deger __attribute__((swift_name("carpmaAyarla(deger:)")));
- (void)cikarmaAyarlaDeger:(BOOL)deger __attribute__((swift_name("cikarmaAyarla(deger:)")));
- (void)diziBaslat __attribute__((swift_name("diziBaslat()")));
- (void)diziyeEkleSayi:(int32_t)sayi __attribute__((swift_name("diziyeEkle(sayi:)")));
- (void)enKotuKupaSuresiAyarlaDeger:(int32_t)deger __attribute__((swift_name("enKotuKupaSuresiAyarla(deger:)")));
- (void)islemEkleBirinci:(int32_t)birinci islec:(NSString *)islec ikinci:(int32_t)ikinci __attribute__((swift_name("islemEkle(birinci:islec:ikinci:)")));
- (void)korAyarlaDeger:(BOOL)deger __attribute__((swift_name("korAyarla(deger:)")));
- (void)korCarpmaAyarlaDeger:(BOOL)deger __attribute__((swift_name("korCarpmaAyarla(deger:)")));
- (void)kupaKayipAyarlaDeger:(int32_t)deger __attribute__((swift_name("kupaKayipAyarla(deger:)")));
- (void)kupaKazancAyarlaDeger:(int32_t)deger __attribute__((swift_name("kupaKazancAyarla(deger:)")));
- (SharedKorDersOturumu *)kur __attribute__((swift_name("kur()")));
- (void)sayiSuresiAyarlaMs:(int64_t)ms __attribute__((swift_name("sayiSuresiAyarla(ms:)")));
- (void)tablodanYukleBaslangicAdimi:(int32_t)baslangicAdimi bitisAdimi:(int32_t)bitisAdimi bitisAdimiVar:(BOOL)bitisAdimiVar adimSayisi:(int32_t)adimSayisi adimBitti:(BOOL)adimBitti __attribute__((swift_name("tablodanYukle(baslangicAdimi:bitisAdimi:bitisAdimiVar:adimSayisi:adimBitti:)")));
- (void)yarisYogunluguAyarlaDeger:(int32_t)deger __attribute__((swift_name("yarisYogunluguAyarla(deger:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KorDersOturumu")))
@interface SharedKorDersOturumu : SharedBase
- (float)basariOrani __attribute__((swift_name("basariOrani()")));
- (BOOL)basariOraniKapsaminda __attribute__((swift_name("basariOraniKapsaminda()")));
- (NSString * _Nullable)birinciSayiMetni __attribute__((swift_name("birinciSayiMetni()")));
- (NSString *)bitisSureMetni:(NSString *)sureMetni __attribute__((swift_name("bitis(sureMetni:)")));
- (BOOL)dersBitisiHakkiAl __attribute__((swift_name("dersBitisiHakkiAl()")));
- (int32_t)dersPuani __attribute__((swift_name("dersPuani()")));
- (SharedKorDersAdimi * _Nullable)diziAtla __attribute__((swift_name("diziAtla()")));
- (SharedKorDersAdimi * _Nullable)diziBaslat __attribute__((swift_name("diziBaslat()")));
- (void)diziDurdur __attribute__((swift_name("diziDurdur()")));
- (SharedKorDersAdimi *)diziYenidenBaslat __attribute__((swift_name("diziYenidenBaslat()")));
- (BOOL)dogruIslendi __attribute__((swift_name("dogruIslendi()")));
- (NSString * _Nullable)ikinciSayiMetni __attribute__((swift_name("ikinciSayiMetni()")));
- (float)ilerlemeBitisAgirligi __attribute__((swift_name("ilerlemeBitisAgirligi()")));
- (NSString *)ipucuMetniIpucu:(NSString *)ipucu __attribute__((swift_name("ipucuMetni(ipucu:)")));
- (NSString * _Nullable)islecMetni __attribute__((swift_name("islecMetni()")));
- (BOOL)kontrolEtGirdi:(NSString *)girdi abakusDegeri:(int32_t)abakusDegeri __attribute__((swift_name("kontrolEt(girdi:abakusDegeri:)")));
- (void)kontrolSifirla __attribute__((swift_name("kontrolSifirla()")));
- (BOOL)kupaCikisCezasiVerilmeliDersBasladi:(BOOL)dersBasladi __attribute__((swift_name("kupaCikisCezasiVerilmeli(dersBasladi:)")));
- (NSString *)kupaDeltaHedefi __attribute__((swift_name("kupaDeltaHedefi()")));
- (int32_t)kupaDeltasiDogru:(BOOL)dogru __attribute__((swift_name("kupaDeltasi(dogru:)")));
- (NSString *)kupaModuAdi __attribute__((swift_name("kupaModuAdi()")));
- (BOOL)kupaSonucuHakkiAl __attribute__((swift_name("kupaSonucuHakkiAl()")));
- (BOOL)kupaYarisLoguHakkiAl __attribute__((swift_name("kupaYarisLoguHakkiAl()")));
- (SharedKorDersAdimi *)sayiGosterAdimi __attribute__((swift_name("sayiGosterAdimi()")));
- (SharedKorDersAdimi *)sonrakiSayiAdimi __attribute__((swift_name("sonrakiSayiAdimi()")));
- (BOOL)sonrakiSoru __attribute__((swift_name("sonrakiSoru()")));
- (BOOL)sureBaslatilmaliSureBasladi:(BOOL)sureBasladi __attribute__((swift_name("sureBaslatilmali(sureBasladi:)")));
- (NSString *)sureMetniSaniye:(int32_t)saniye __attribute__((swift_name("sureMetni(saniye:)")));
- (NSString * _Nullable)tekSayiMetni __attribute__((swift_name("tekSayiMetni()")));
- (NSString *)yanlisSonrasi __attribute__((swift_name("yanlisSonrasi()")));
- (NSString *)yanlisSonucEylemi __attribute__((swift_name("yanlisSonucEylemi()")));
@property (readonly) BOOL abakusleCevap __attribute__((swift_name("abakusleCevap")));
@property (readonly) BOOL adimAtlaGizli __attribute__((swift_name("adimAtlaGizli")));
@property (readonly) int64_t boncukAnimasyonMs __attribute__((swift_name("boncukAnimasyonMs")));
@property (readonly) float carpan __attribute__((swift_name("carpan")));
@property (readonly) BOOL carpmaGosterimi __attribute__((swift_name("carpmaGosterimi")));
@property (readonly) BOOL diziGosteriliyor __attribute__((swift_name("diziGosteriliyor")));
@property (readonly) BOOL diziVar __attribute__((swift_name("diziVar")));
@property (readonly) NSString *dogruCevapEtiketi __attribute__((swift_name("dogruCevapEtiketi")));
@property (readonly) int32_t dogruSayisi __attribute__((swift_name("dogruSayisi")));
@property (readonly) int32_t enKotuKupaSuresi __attribute__((swift_name("enKotuKupaSuresi")));
@property (readonly) int32_t globalPartId __attribute__((swift_name("globalPartId")));
@property (readonly) BOOL ilerlemeGizli __attribute__((swift_name("ilerlemeGizli")));
@property (readonly) BOOL ipucuGizli __attribute__((swift_name("ipucuGizli")));
@property (readonly) BOOL isDailyQuestionMode __attribute__((swift_name("isDailyQuestionMode")));
@property (readonly) BOOL klavyeyleCevap __attribute__((swift_name("klavyeyleCevap")));
@property (readonly) BOOL kupaModu __attribute__((swift_name("kupaModu")));
@property (readonly) NSString *seninCevabinMetni __attribute__((swift_name("seninCevabinMetni")));
@property (readonly) BOOL sifirlaGizli __attribute__((swift_name("sifirlaGizli")));
@property (readonly) int32_t soruPaneliKupaSuresi __attribute__((swift_name("soruPaneliKupaSuresi")));
@property (readonly) int32_t soruSayisi __attribute__((swift_name("soruSayisi")));
@property (readonly) int32_t suankiIndeks __attribute__((swift_name("suankiIndeks")));
@property (readonly) BOOL sureGorunur __attribute__((swift_name("sureGorunur")));
@property (readonly) int32_t toplamPuan __attribute__((swift_name("toplamPuan")));
@property (readonly) BOOL yarisDersi __attribute__((swift_name("yarisDersi")));
@property (readonly) int32_t yuklenenDersAdimi __attribute__((swift_name("yuklenenDersAdimi")));
@end

__attribute__((swift_name("OyunLessonItem")))
@protocol SharedOyunLessonItem
@required
@property (readonly) SharedInt * _Nullable cupPoint1 __attribute__((swift_name("cupPoint1")));
@property (readonly) SharedInt * _Nullable cupPoint2 __attribute__((swift_name("cupPoint2")));
@property (readonly) SharedBoolean * _Nullable isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) SharedInt * _Nullable mapFragmentIndex __attribute__((swift_name("mapFragmentIndex")));
@property (readonly) SharedInt * _Nullable record __attribute__((swift_name("record")));
@property (readonly) NSString *stepCupIcon __attribute__((swift_name("stepCupIcon")));
@property (readonly) BOOL stepIsFinish __attribute__((swift_name("stepIsFinish")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString * _Nullable titleUnit __attribute__((swift_name("titleUnit")));
@property (readonly) int32_t type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonItemOyunGorunumu")))
@interface SharedLessonItemOyunGorunumu : SharedBase <SharedOyunLessonItem>
- (instancetype)initWithItem:(SharedLessonItem *)item __attribute__((swift_name("init(item:)"))) __attribute__((objc_designated_initializer));
@property (readonly) SharedInt * _Nullable cupPoint1 __attribute__((swift_name("cupPoint1")));
@property (readonly) SharedInt * _Nullable cupPoint2 __attribute__((swift_name("cupPoint2")));
@property (readonly) SharedBoolean * _Nullable isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) SharedLessonItem *item __attribute__((swift_name("item")));
@property (readonly) SharedInt * _Nullable mapFragmentIndex __attribute__((swift_name("mapFragmentIndex")));
@property (readonly) SharedInt * _Nullable record __attribute__((swift_name("record")));
@property (readonly) NSString *stepCupIcon __attribute__((swift_name("stepCupIcon")));
@property (readonly) BOOL stepIsFinish __attribute__((swift_name("stepIsFinish")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString * _Nullable titleUnit __attribute__((swift_name("titleUnit")));
@property (readonly) int32_t type __attribute__((swift_name("type")));
@end

__attribute__((swift_name("DailyQuestionGenerators")))
@protocol SharedDailyQuestionGenerators
@required
- (NSArray<SharedInt *> *)generateRelatedNumbers2BlindingCount:(int32_t)count digitsOne:(int32_t)digitsOne __attribute__((swift_name("generateRelatedNumbers2Blinding(count:digitsOne:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyNewCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyNew(count:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyOldCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyOld(count:)")));
- (NSArray<SharedInt *> *)generateSequence1DigitsCount:(int32_t)count digitsOne:(int32_t)digitsOne __attribute__((swift_name("generateSequence1Digits(count:digitsOne:)")));
- (NSArray<SharedInt *> *)generateSequenceBeadRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceBeadRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionCount:(int32_t)count __attribute__((swift_name("generateSequenceExtraction(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionBeadRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionBeadRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionBeadRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionBeadRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionFiveRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionFiveRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionFiveRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionFiveRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionTenRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionTenRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionTenRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionTenRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionThreeDigits(count:)")));
- (SharedMathOperation *)multiplicationFull __attribute__((swift_name("multiplicationFull()")));
- (SharedMathOperation *)multiplicationLessFive __attribute__((swift_name("multiplicationLessFive()")));
- (SharedMathOperation *)multiplicationLessFiveFull __attribute__((swift_name("multiplicationLessFiveFull()")));
- (SharedMathOperation *)multiplicationThreeFull __attribute__((swift_name("multiplicationThreeFull()")));
- (SharedMathOperation *)multiplicationThreeTwoFive __attribute__((swift_name("multiplicationThreeTwoFive()")));
- (SharedMathOperation *)multiplicationThreeTwoFull __attribute__((swift_name("multiplicationThreeTwoFull()")));
- (SharedMathOperation *)multiplicationTwo __attribute__((swift_name("multiplicationTwo()")));
- (SharedMathOperation *)multiplicationTwoFull __attribute__((swift_name("multiplicationTwoFull()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MatematikDailyQuestionGenerators")))
@interface SharedMatematikDailyQuestionGenerators : SharedBase <SharedDailyQuestionGenerators>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)matematikDailyQuestionGenerators __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMatematikDailyQuestionGenerators *shared __attribute__((swift_name("shared")));
- (NSArray<SharedInt *> *)generateRelatedNumbers2BlindingCount:(int32_t)count digitsOne:(int32_t)digitsOne __attribute__((swift_name("generateRelatedNumbers2Blinding(count:digitsOne:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyNewCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyNew(count:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyOldCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyOld(count:)")));
- (NSArray<SharedInt *> *)generateSequence1DigitsCount:(int32_t)count digitsOne:(int32_t)digitsOne __attribute__((swift_name("generateSequence1Digits(count:digitsOne:)")));
- (NSArray<SharedInt *> *)generateSequenceBeadRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceBeadRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionCount:(int32_t)count __attribute__((swift_name("generateSequenceExtraction(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionBeadRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionBeadRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionBeadRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionBeadRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionFiveRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionFiveRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionFiveRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionFiveRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionTenRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionTenRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionTenRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionTenRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionThreeDigits(count:)")));
- (SharedMathOperation *)multiplicationFull __attribute__((swift_name("multiplicationFull()")));
- (SharedMathOperation *)multiplicationLessFive __attribute__((swift_name("multiplicationLessFive()")));
- (SharedMathOperation *)multiplicationLessFiveFull __attribute__((swift_name("multiplicationLessFiveFull()")));
- (SharedMathOperation *)multiplicationThreeFull __attribute__((swift_name("multiplicationThreeFull()")));
- (SharedMathOperation *)multiplicationThreeTwoFive __attribute__((swift_name("multiplicationThreeTwoFive()")));
- (SharedMathOperation *)multiplicationThreeTwoFull __attribute__((swift_name("multiplicationThreeTwoFull()")));
- (SharedMathOperation *)multiplicationTwo __attribute__((swift_name("multiplicationTwo()")));
- (SharedMathOperation *)multiplicationTwoFull __attribute__((swift_name("multiplicationTwoFull()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SonucEkraniKoprusu")))
@interface SharedSonucEkraniKoprusu : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sonucEkraniKoprusu __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSonucEkraniKoprusu *shared __attribute__((swift_name("shared")));
- (NSArray<NSString *> *)animasyonlar __attribute__((swift_name("animasyonlar()")));
- (float)basariOraniDogru:(int32_t)dogru toplam:(int32_t)toplam __attribute__((swift_name("basariOrani(dogru:toplam:)")));
- (NSString *)basarisizlikAciklamasiSandikBasarisizligi:(BOOL)sandikBasarisizligi __attribute__((swift_name("basarisizlikAciklamasi(sandikBasarisizligi:)")));
- (float)cubukOraniPuan:(int32_t)puan puanTavani:(int32_t)puanTavani __attribute__((swift_name("cubukOrani(puan:puanTavani:)")));
- (SharedScoreBarColorBlend *)cubukRengiPuan:(int32_t)puan oncekiPuan:(int32_t)oncekiPuan puanTavani:(int32_t)puanTavani __attribute__((swift_name("cubukRengi(puan:oncekiPuan:puanTavani:)")));
- (int32_t)dersPuaniBasariOrani:(float)basariOrani __attribute__((swift_name("dersPuani(basariOrani:)")));
- (int32_t)ikiYildizEsigiMapFragmentStepIndex:(int32_t)mapFragmentStepIndex puanTavani:(int32_t)puanTavani __attribute__((swift_name("ikiYildizEsigi(mapFragmentStepIndex:puanTavani:)")));
- (int32_t)puanTavaniMapFragmentStepIndex:(int32_t)mapFragmentStepIndex __attribute__((swift_name("puanTavani(mapFragmentStepIndex:)")));
- (NSString *)rastgeleAnimasyon __attribute__((swift_name("rastgeleAnimasyon()")));
- (NSString *)sureMetniSaniye:(int32_t)saniye __attribute__((swift_name("sureMetni(saniye:)")));
- (int32_t)sureSaniyeSureMetni:(NSString *)sureMetni __attribute__((swift_name("sureSaniye(sureMetni:)")));
- (int32_t)yildizEsigiSira:(int32_t)sira ikiYildizEsigi:(int32_t)ikiYildizEsigi puanTavani:(int32_t)puanTavani __attribute__((swift_name("yildizEsigi(sira:ikiYildizEsigi:puanTavani:)")));
- (float)yildizOraniEsik:(int32_t)esik puanTavani:(int32_t)puanTavani __attribute__((swift_name("yildizOrani(esik:puanTavani:)")));
- (float)yildizSesHiziKademe:(int32_t)kademe __attribute__((swift_name("yildizSesHizi(kademe:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestFragmentLogic")))
@interface SharedChestFragmentLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestFragmentLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestFragmentLogic *shared __attribute__((swift_name("shared")));
- (SharedChestFragmentLogicCupIconChange *)changeCupIconLessonItem:(SharedLessonItem *)lessonItem globalPartId:(int32_t)globalPartId __attribute__((swift_name("changeCupIcon(lessonItem:globalPartId:)")));
- (BOOL)isLastChestOfCupPathPartLessonAfterClaim:(SharedLessonItem * _Nullable)lessonAfterClaim globalPartId:(int32_t)globalPartId lessonItems:(NSArray<SharedLessonItem *> *)lessonItems __attribute__((swift_name("isLastChestOfCupPathPart(lessonAfterClaim:globalPartId:lessonItems:)")));
- (BOOL)isLessonTypeReturnItem:(SharedLessonItem * _Nullable)item __attribute__((swift_name("isLessonTypeReturn(item:)")));
- (NSString *)lessonProgressKeyItem:(SharedLessonItem *)item fallbackIndex:(int32_t)fallbackIndex __attribute__((swift_name("lessonProgressKey(item:fallbackIndex:)")));
- (BOOL)shouldIncrementDartProgressLessonBeforeClaim:(SharedLessonItem * _Nullable)lessonBeforeClaim lessonSuccessRate:(float)lessonSuccessRate __attribute__((swift_name("shouldIncrementDartProgress(lessonBeforeClaim:lessonSuccessRate:)")));
- (BOOL)shouldIncrementTornadoGlobalPartId:(int32_t)globalPartId __attribute__((swift_name("shouldIncrementTornado(globalPartId:)")));
- (BOOL)shouldIncrementVolcanoGlobalPartId:(int32_t)globalPartId __attribute__((swift_name("shouldIncrementVolcano(globalPartId:)")));
- (BOOL)shouldMarkJustFinishedChestForRatingLessonBeforeClaim:(SharedLessonItem * _Nullable)lessonBeforeClaim __attribute__((swift_name("shouldMarkJustFinishedChestForRating(lessonBeforeClaim:)")));
- (BOOL)shouldRedirectToLoginForFirstTutorialCurrentTutorialNumber:(int32_t)currentTutorialNumber isLoggedIn:(BOOL)isLoggedIn __attribute__((swift_name("shouldRedirectToLoginForFirstTutorial(currentTutorialNumber:isLoggedIn:)")));
- (SharedInt * _Nullable)unlockNextRaceItemSkippingCompletedCompletedIndex:(int32_t)completedIndex season:(int32_t)season __attribute__((swift_name("unlockNextRaceItemSkippingCompleted(completedIndex:season:)")));
- (SharedChestFragmentLogicMapProgressResult *)updateMapProgressMapFragmentStepIndex:(int32_t)mapFragmentStepIndex recordScore:(int32_t)recordScore lessonSuccessRate:(float)lessonSuccessRate icon:(NSString *)icon season:(int32_t)season questionElapsedMs:(SharedLong * _Nullable)questionElapsedMs __attribute__((swift_name("updateMapProgress(mapFragmentStepIndex:recordScore:lessonSuccessRate:icon:season:questionElapsedMs:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestFragmentLogic.CupIconChange")))
@interface SharedChestFragmentLogicCupIconChange : SharedBase
- (instancetype)initWithIcon:(NSString *)icon pendingChestStarGainAmount:(int32_t)pendingChestStarGainAmount __attribute__((swift_name("init(icon:pendingChestStarGainAmount:)"))) __attribute__((objc_designated_initializer));
- (SharedChestFragmentLogicCupIconChange *)doCopyIcon:(NSString *)icon pendingChestStarGainAmount:(int32_t)pendingChestStarGainAmount __attribute__((swift_name("doCopy(icon:pendingChestStarGainAmount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *icon __attribute__((swift_name("icon")));
@property (readonly) int32_t pendingChestStarGainAmount __attribute__((swift_name("pendingChestStarGainAmount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestFragmentLogic.MapProgressResult")))
@interface SharedChestFragmentLogicMapProgressResult : SharedBase
- (instancetype)initWithShouldIncrementStepFinishMission:(BOOL)shouldIncrementStepFinishMission shouldIncrementStepCountMission:(BOOL)shouldIncrementStepCountMission shouldIncrementPerfectStepCountMission:(BOOL)shouldIncrementPerfectStepCountMission successRateEvents:(NSArray<SharedChestFragmentLogicSuccessRateEvent *> *)successRateEvents pendingAnimation:(SharedChestFragmentLogicPendingLessonProgressAnimation * _Nullable)pendingAnimation __attribute__((swift_name("init(shouldIncrementStepFinishMission:shouldIncrementStepCountMission:shouldIncrementPerfectStepCountMission:successRateEvents:pendingAnimation:)"))) __attribute__((objc_designated_initializer));
- (SharedChestFragmentLogicMapProgressResult *)doCopyShouldIncrementStepFinishMission:(BOOL)shouldIncrementStepFinishMission shouldIncrementStepCountMission:(BOOL)shouldIncrementStepCountMission shouldIncrementPerfectStepCountMission:(BOOL)shouldIncrementPerfectStepCountMission successRateEvents:(NSArray<SharedChestFragmentLogicSuccessRateEvent *> *)successRateEvents pendingAnimation:(SharedChestFragmentLogicPendingLessonProgressAnimation * _Nullable)pendingAnimation __attribute__((swift_name("doCopy(shouldIncrementStepFinishMission:shouldIncrementStepCountMission:shouldIncrementPerfectStepCountMission:successRateEvents:pendingAnimation:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL incrementRocketDailyLessons __attribute__((swift_name("incrementRocketDailyLessons")));
@property (readonly) SharedChestFragmentLogicPendingLessonProgressAnimation * _Nullable pendingAnimation __attribute__((swift_name("pendingAnimation")));
@property (readonly) BOOL shouldIncrementPerfectStepCountMission __attribute__((swift_name("shouldIncrementPerfectStepCountMission")));
@property (readonly) BOOL shouldIncrementStepCountMission __attribute__((swift_name("shouldIncrementStepCountMission")));
@property (readonly) BOOL shouldIncrementStepFinishMission __attribute__((swift_name("shouldIncrementStepFinishMission")));
@property (readonly) NSArray<SharedChestFragmentLogicSuccessRateEvent *> *successRateEvents __attribute__((swift_name("successRateEvents")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestFragmentLogic.PendingLessonProgressAnimation")))
@interface SharedChestFragmentLogicPendingLessonProgressAnimation : SharedBase
- (instancetype)initWithKey:(NSString *)key fromFilledSegments:(int32_t)fromFilledSegments toFilledSegments:(int32_t)toFilledSegments __attribute__((swift_name("init(key:fromFilledSegments:toFilledSegments:)"))) __attribute__((objc_designated_initializer));
- (SharedChestFragmentLogicPendingLessonProgressAnimation *)doCopyKey:(NSString *)key fromFilledSegments:(int32_t)fromFilledSegments toFilledSegments:(int32_t)toFilledSegments __attribute__((swift_name("doCopy(key:fromFilledSegments:toFilledSegments:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t fromFilledSegments __attribute__((swift_name("fromFilledSegments")));
@property (readonly) NSString *key __attribute__((swift_name("key")));
@property (readonly) int32_t toFilledSegments __attribute__((swift_name("toFilledSegments")));
@end

__attribute__((swift_name("ChestFragmentLogic.SuccessRateEvent")))
@interface SharedChestFragmentLogicSuccessRateEvent : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestFragmentLogic.SuccessRateEventItemFirstFinish")))
@interface SharedChestFragmentLogicSuccessRateEventItemFirstFinish : SharedChestFragmentLogicSuccessRateEvent
- (instancetype)initWithPartId:(int32_t)partId position:(int32_t)position chestStars:(SharedInt * _Nullable)chestStars __attribute__((swift_name("init(partId:position:chestStars:)"))) __attribute__((objc_designated_initializer));
- (SharedChestFragmentLogicSuccessRateEventItemFirstFinish *)doCopyPartId:(int32_t)partId position:(int32_t)position chestStars:(SharedInt * _Nullable)chestStars __attribute__((swift_name("doCopy(partId:position:chestStars:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable chestStars __attribute__((swift_name("chestStars")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) int32_t position __attribute__((swift_name("position")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestFragmentLogic.SuccessRateEventPass")))
@interface SharedChestFragmentLogicSuccessRateEventPass : SharedChestFragmentLogicSuccessRateEvent
- (instancetype)initWithPartId:(int32_t)partId position:(int32_t)position step:(int32_t)step elapsedMs:(SharedLong * _Nullable)elapsedMs __attribute__((swift_name("init(partId:position:step:elapsedMs:)"))) __attribute__((objc_designated_initializer));
- (SharedChestFragmentLogicSuccessRateEventPass *)doCopyPartId:(int32_t)partId position:(int32_t)position step:(int32_t)step elapsedMs:(SharedLong * _Nullable)elapsedMs __attribute__((swift_name("doCopy(partId:position:step:elapsedMs:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable elapsedMs __attribute__((swift_name("elapsedMs")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) int32_t position __attribute__((swift_name("position")));
@property (readonly) int32_t step __attribute__((swift_name("step")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestIcons")))
@interface SharedChestIcons : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestIcons __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestIcons *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *CHEST_STARS_TIER0 __attribute__((swift_name("CHEST_STARS_TIER0")));
@property (readonly) NSString *CHEST_STARS_TIER1 __attribute__((swift_name("CHEST_STARS_TIER1")));
@property (readonly) NSString *CHEST_STARS_TIER2 __attribute__((swift_name("CHEST_STARS_TIER2")));
@property (readonly) NSString *CHEST_STARS_TIER3 __attribute__((swift_name("CHEST_STARS_TIER3")));
@property (readonly) NSString *CUP_IC __attribute__((swift_name("CUP_IC")));
@property (readonly) NSString *CUP_IC2 __attribute__((swift_name("CUP_IC2")));
@property (readonly) NSString *CUP_IC3 __attribute__((swift_name("CUP_IC3")));
@property (readonly) NSString *STAR_OFF_IC __attribute__((swift_name("STAR_OFF_IC")));
@property (readonly) NSString *STAR_ON_IC __attribute__((swift_name("STAR_ON_IC")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestResultLogic")))
@interface SharedChestResultLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestResultLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestResultLogic *shared __attribute__((swift_name("shared")));
- (NSString *)formatSecondsSeconds:(int32_t)seconds __attribute__((swift_name("formatSeconds(seconds:)")));
- (SharedInt * _Nullable)parseTimeToSecondsValue:(NSString *)value __attribute__((swift_name("parseTimeToSeconds(value:)")));
- (BOOL)recordLessonItem:(SharedLessonItem *)lessonItem toplamPuan:(int32_t)toplamPuan season:(int32_t)season __attribute__((swift_name("record(lessonItem:toplamPuan:season:)")));
- (SharedChestResultLogicReplayResult *)replayFinishedChestIdx:(int32_t)idx toplamPuan:(int32_t)toplamPuan season:(int32_t)season __attribute__((swift_name("replayFinishedChest(idx:toplamPuan:season:)")));
- (int32_t)resolveCupPoint2ThresholdMapFragmentStepIndex:(int32_t)mapFragmentStepIndex scoreCap:(int32_t)scoreCap globalPartId:(int32_t)globalPartId __attribute__((swift_name("resolveCupPoint2Threshold(mapFragmentStepIndex:scoreCap:globalPartId:)")));
- (int32_t)resolveScoreCapMapFragmentStepIndex:(int32_t)mapFragmentStepIndex globalPartId:(int32_t)globalPartId __attribute__((swift_name("resolveScoreCap(mapFragmentStepIndex:globalPartId:)")));
- (float)scoreProgressAnimatedScore:(int32_t)animatedScore scoreCap:(int32_t)scoreCap __attribute__((swift_name("scoreProgress(animatedScore:scoreCap:)")));
- (BOOL)shouldIncrementTornadoOnReplayGlobalPartId:(int32_t)globalPartId __attribute__((swift_name("shouldIncrementTornadoOnReplay(globalPartId:)")));
- (BOOL)shouldIncrementVolcanoOnReplayGlobalPartId:(int32_t)globalPartId __attribute__((swift_name("shouldIncrementVolcanoOnReplay(globalPartId:)")));
- (BOOL)shouldSkipChestLessonItem:(SharedLessonItem *)lessonItem __attribute__((swift_name("shouldSkipChest(lessonItem:)")));
- (NSArray<SharedInt *> *)starTiersCrossedPreviousScore:(int32_t)previousScore currentScore:(int32_t)currentScore cupPoint2Threshold:(int32_t)cupPoint2Threshold scoreCap:(int32_t)scoreCap __attribute__((swift_name("starTiersCrossed(previousScore:currentScore:cupPoint2Threshold:scoreCap:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestResultLogic.ReplayResult")))
@interface SharedChestResultLogicReplayResult : SharedBase
- (instancetype)initWithUpdated:(SharedLessonItem * _Nullable)updated gainedStars:(int32_t)gainedStars shouldIncrementKarate:(BOOL)shouldIncrementKarate justFinishedChestForRating:(BOOL)justFinishedChestForRating __attribute__((swift_name("init(updated:gainedStars:shouldIncrementKarate:justFinishedChestForRating:)"))) __attribute__((objc_designated_initializer));
- (SharedChestResultLogicReplayResult *)doCopyUpdated:(SharedLessonItem * _Nullable)updated gainedStars:(int32_t)gainedStars shouldIncrementKarate:(BOOL)shouldIncrementKarate justFinishedChestForRating:(BOOL)justFinishedChestForRating __attribute__((swift_name("doCopy(updated:gainedStars:shouldIncrementKarate:justFinishedChestForRating:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t gainedStars __attribute__((swift_name("gainedStars")));
@property (readonly) BOOL justFinishedChestForRating __attribute__((swift_name("justFinishedChestForRating")));
@property (readonly) BOOL shouldIncrementKarate __attribute__((swift_name("shouldIncrementKarate")));
@property (readonly) SharedLessonItem * _Nullable updated __attribute__((swift_name("updated")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestTypeProgressHelper")))
@interface SharedChestTypeProgressHelper : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestTypeProgressHelper __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestTypeProgressHelper *shared __attribute__((swift_name("shared")));
- (NSString *)resolvedChestIconItem:(SharedLessonItem *)item recordValue:(int32_t)recordValue globalPartId:(int32_t)globalPartId __attribute__((swift_name("resolvedChestIcon(item:recordValue:globalPartId:)")));
- (BOOL)shouldIncrementKarateForFirstThreeStarsItem:(SharedLessonItem *)item recordScoreThisRun:(int32_t)recordScoreThisRun globalPartId:(int32_t)globalPartId __attribute__((swift_name("shouldIncrementKarateForFirstThreeStars(item:recordScoreThisRun:globalPartId:)")));
- (int32_t)starCountForChestIconIconResId:(NSString *)iconResId __attribute__((swift_name("starCountForChestIcon(iconResId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalLessonData")))
@interface SharedGlobalLessonData : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)globalLessonData __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedGlobalLessonData *shared __attribute__((swift_name("shared")));
- (void)applyDefaultLessonItemsPartId:(int32_t)partId tutorial1Pending:(BOOL)tutorial1Pending saveRemote:(BOOL)saveRemote __attribute__((swift_name("applyDefaultLessonItems(partId:tutorial1Pending:saveRemote:)")));
- (void)applyLoadStatusStatus:(SharedGlobalLessonDataFirestoreLoadStatus *)status partId:(int32_t)partId tutorial1Pending:(BOOL)tutorial1Pending __attribute__((swift_name("applyLoadStatus(status:partId:tutorial1Pending:)")));
- (SharedLessonItem *)applyProgressFieldsTemplate:(SharedLessonItem *)template_ data:(NSDictionary<NSString *, id> *)data stepCupIconOku:(NSString * _Nullable (^)(id _Nullable))stepCupIconOku __attribute__((swift_name("applyProgressFields(template:data:stepCupIconOku:)")));
- (BOOL)applyTeacherUnlocksToMemory __attribute__((swift_name("applyTeacherUnlocksToMemory()")));
- (SharedGlobalLessonDataLeaderboardSubmit * _Nullable)backfillLeaderboardSubmitPartId:(int32_t)partId chestIndex:(int32_t)chestIndex items:(NSArray<SharedLessonItem *> * _Nullable)items season:(int32_t)season __attribute__((swift_name("backfillLeaderboardSubmit(partId:chestIndex:items:season:)")));
- (NSArray<SharedLessonItem *> *)buildDefaultLessonItemsForPartPartId:(int32_t)partId tutorial1Pending:(BOOL)tutorial1Pending __attribute__((swift_name("buildDefaultLessonItemsForPart(partId:tutorial1Pending:)")));
- (int32_t)calculateGlobalCompletionPercentageItemsByPart:(NSDictionary<SharedInt *, id> *)itemsByPart __attribute__((swift_name("calculateGlobalCompletionPercentage(itemsByPart:)")));
- (NSArray<NSString *> *)clearCurrentUserLessonDataUid:(NSString * _Nullable)uid __attribute__((swift_name("clearCurrentUserLessonData(uid:)")));
- (NSArray<SharedLessonItem *> *)createLessonItemsPartId:(int32_t)partId __attribute__((swift_name("createLessonItems(partId:)")));
- (NSArray<SharedLessonItem *> *)createLessonItemsInTurkishPartId:(int32_t)partId __attribute__((swift_name("createLessonItemsInTurkish(partId:)")));
- (NSArray<SharedGlobalLessonDataChestLessonRef *> *)getFinishedChestItemsAcrossPartsCloudItemsByPart:(NSDictionary<SharedInt *, id> *)cloudItemsByPart __attribute__((swift_name("getFinishedChestItemsAcrossParts(cloudItemsByPart:)")));
- (NSString *)getKeyPartId:(int32_t)partId __attribute__((swift_name("getKey(partId:)")));
- (SharedLessonItem * _Nullable)getLessonItemPosition:(int32_t)position __attribute__((swift_name("getLessonItem(position:)")));
- (SharedLessonItem * _Nullable)getLessonItemForPartPartId:(int32_t)partId index:(int32_t)index cloudItems:(NSArray<SharedLessonItem *> * _Nullable)cloudItems __attribute__((swift_name("getLessonItemForPart(partId:index:cloudItems:)")));
- (void)initializePartId:(int32_t)partId __attribute__((swift_name("initialize(partId:)")));
- (NSString *)itemDocumentPathUid:(NSString *)uid partId:(int32_t)partId stableId:(NSString *)stableId __attribute__((swift_name("itemDocumentPath(uid:partId:stableId:)")));
- (NSString *)itemsCollectionPathUid:(NSString *)uid partId:(int32_t)partId __attribute__((swift_name("itemsCollectionPath(uid:partId:)")));
- (SharedGlobalLessonDataLeaderboardSubmit * _Nullable)leaderboardSubmitForPartId:(int32_t)partId position:(int32_t)position newItem:(SharedLessonItem *)newItem season:(int32_t)season __attribute__((swift_name("leaderboardSubmitFor(partId:position:newItem:season:)")));
- (NSString *)legacyItemDocumentPathUid:(NSString *)uid partId:(int32_t)partId position:(int32_t)position __attribute__((swift_name("legacyItemDocumentPath(uid:partId:position:)")));
- (SharedGlobalLessonDataLegacyMigrationPlan * _Nullable)legacyMigrationPlanDocs:(NSDictionary<NSString *, id> *)docs merged:(NSArray<SharedLessonItem *> *)merged __attribute__((swift_name("legacyMigrationPlan(docs:merged:)")));
- (NSArray<SharedLessonItem *> * _Nullable)lessonItemsFromFirestoreDocsPartId:(int32_t)partId docs:(NSDictionary<NSString *, id> *)docs __attribute__((swift_name("lessonItemsFromFirestoreDocs(partId:docs:)")));
- (NSString *)lessonPrefsNameUid:(NSString * _Nullable)uid __attribute__((swift_name("lessonPrefsName(uid:)")));
- (void)loadFromParsedCacheItems:(NSArray<SharedLessonItem *> *)items partId:(int32_t)partId __attribute__((swift_name("loadFromParsedCache(items:partId:)")));
- (NSArray<SharedLessonItem *> *)loadLessonItemsForPartPartId:(int32_t)partId cloudItems:(NSArray<SharedLessonItem *> * _Nullable)cloudItems __attribute__((swift_name("loadLessonItemsForPart(partId:cloudItems:)")));
- (NSString *)localizeLessonTextTurkish:(NSString *)turkish __attribute__((swift_name("localizeLessonText(turkish:)")));
- (NSArray<SharedLessonItem *> *)mergeTemplateWithDocsPartId:(int32_t)partId docs:(NSDictionary<NSString *, id> *)docs __attribute__((swift_name("mergeTemplateWithDocs(partId:docs:)")));
- (id _Nullable)migrateLegacyRecordFieldRoot:(id _Nullable)root __attribute__((swift_name("migrateLegacyRecordField(root:)")));
- (id _Nullable)migrateRecordValueRecordElement:(id _Nullable)recordElement __attribute__((swift_name("migrateRecordValue(recordElement:)")));
- (SharedGlobalLessonDataFirestoreLoadStatus *)onLoadFromFirestorePartId:(int32_t)partId items:(NSArray<SharedLessonItem *> * _Nullable)items error:(BOOL)error __attribute__((swift_name("onLoadFromFirestore(partId:items:error:)")));
- (BOOL)onRealtimeSnapshotPartId:(int32_t)partId docs:(NSDictionary<NSString *, id> *)docs season:(int32_t)season __attribute__((swift_name("onRealtimeSnapshot(partId:docs:season:)")));
- (BOOL)prepareRealtimeSyncPartId:(int32_t)partId uid:(NSString *)uid __attribute__((swift_name("prepareRealtimeSync(partId:uid:)")));
- (NSDictionary<NSString *, id> *)progressFieldsOfItem:(SharedLessonItem *)item stepCupIconYaz:(id _Nullable (^)(NSString *))stepCupIconYaz __attribute__((swift_name("progressFieldsOf(item:stepCupIconYaz:)")));
- (SharedInt * _Nullable)resolveFirstChestLessonIndexPartId:(int32_t)partId cloudItems:(NSArray<SharedLessonItem *> * _Nullable)cloudItems error:(BOOL)error __attribute__((swift_name("resolveFirstChestLessonIndex(partId:cloudItems:error:)")));
- (void)saveToPreferencesSaveRemote:(BOOL)saveRemote __attribute__((swift_name("saveToPreferences(saveRemote:)")));
- (NSArray<NSDictionary<NSString *, id> *> * _Nullable)seedItemsForPartPartId:(int32_t)partId tutorial1Pending:(BOOL)tutorial1Pending __attribute__((swift_name("seedItemsForPart(partId:tutorial1Pending:)")));
- (NSArray<NSString *> *)seedStableIdsForPartPartId:(int32_t)partId __attribute__((swift_name("seedStableIdsForPart(partId:)")));
- (NSString * _Nullable)stableIdAtPartId:(int32_t)partId position:(int32_t)position __attribute__((swift_name("stableIdAt(partId:position:)")));
- (SharedLessonItem *)stepCupIconKayittanTuretItem:(SharedLessonItem *)item globalPartId:(int32_t)globalPartId __attribute__((swift_name("stepCupIconKayittanTuret(item:globalPartId:)")));
- (NSString * _Nullable)stepCupIconVarsayilanOkuRaw:(id _Nullable)raw __attribute__((swift_name("stepCupIconVarsayilanOku(raw:)")));
- (NSArray<SharedGlobalLessonDataItemWrite *> *)teacherUnlockPlanPartId:(int32_t)partId cloudItems:(NSArray<SharedLessonItem *> * _Nullable)cloudItems tutorial1Pending:(BOOL)tutorial1Pending __attribute__((swift_name("teacherUnlockPlan(partId:cloudItems:tutorial1Pending:)")));
- (SharedInt * _Nullable)templateFirstChestLessonIndexOrNullPartId:(int32_t)partId __attribute__((swift_name("templateFirstChestLessonIndexOrNull(partId:)")));
- (int32_t)totalValidItemsForCompletion __attribute__((swift_name("totalValidItemsForCompletion()")));
- (SharedLessonItem * _Nullable)turkishItemPartId:(int32_t)partId stableId:(NSString *)stableId __attribute__((swift_name("turkishItem(partId:stableId:)")));
- (BOOL)updateLessonItemPosition:(int32_t)position newItem:(SharedLessonItem *)newItem season:(int32_t)season __attribute__((swift_name("updateLessonItem(position:newItem:season:)")));
- (NSMutableArray<SharedLessonItem *> *)withDerivedUnlocksPartId:(int32_t)partId items:(NSArray<SharedLessonItem *> *)items __attribute__((swift_name("withDerivedUnlocks(partId:items:)")));
- (NSArray<SharedLessonItem *> *)withTeacherUnlocksPartId:(int32_t)partId items:(NSArray<SharedLessonItem *> *)items __attribute__((swift_name("withTeacherUnlocks(partId:items:)")));
@property (readonly) NSString *APP_PREFS_NAME __attribute__((swift_name("APP_PREFS_NAME")));
@property (readonly) int64_t AUTH_WAIT_TIMEOUT_MS __attribute__((swift_name("AUTH_WAIT_TIMEOUT_MS")));
@property (readonly) NSString *FIELD_TEACHER_LESSONS_UNLOCKED __attribute__((swift_name("FIELD_TEACHER_LESSONS_UNLOCKED")));
@property (readonly) NSString *FIRESTORE_LESSON_ITEMS __attribute__((swift_name("FIRESTORE_LESSON_ITEMS")));
@property (readonly) NSString *FIRESTORE_LESSON_PROGRESS __attribute__((swift_name("FIRESTORE_LESSON_PROGRESS")));
@property (readonly) NSString *FIRESTORE_USERS __attribute__((swift_name("FIRESTORE_USERS")));
@property (readonly) NSString *KEY_LESSON_ITEMS __attribute__((swift_name("KEY_LESSON_ITEMS")));
@property (readonly) NSString *KEY_TUTORIAL1_LOGIN_FLOW_PENDING __attribute__((swift_name("KEY_TUTORIAL1_LOGIN_FLOW_PENDING")));
@property (readonly) NSSet<SharedInt *> *LEADERBOARD_SUPPORTED_PARTS __attribute__((swift_name("LEADERBOARD_SUPPORTED_PARTS")));
@property (readonly) NSString *PREFS_PREFIX __attribute__((swift_name("PREFS_PREFIX")));
@property (readonly) NSArray<SharedInt *> *SEED_PART_IDS __attribute__((swift_name("SEED_PART_IDS")));
@property int32_t globalPartId __attribute__((swift_name("globalPartId")));
@property (readonly) NSArray<SharedLessonItem *> *lessonItems __attribute__((swift_name("lessonItems")));
@property id<SharedLessonProgressSink> _Nullable sink __attribute__((swift_name("sink")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalLessonData.ChestLessonRef")))
@interface SharedGlobalLessonDataChestLessonRef : SharedBase
- (instancetype)initWithPartId:(int32_t)partId index:(int32_t)index item:(SharedLessonItem *)item __attribute__((swift_name("init(partId:index:item:)"))) __attribute__((objc_designated_initializer));
- (SharedGlobalLessonDataChestLessonRef *)doCopyPartId:(int32_t)partId index:(int32_t)index item:(SharedLessonItem *)item __attribute__((swift_name("doCopy(partId:index:item:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t index __attribute__((swift_name("index")));
@property (readonly) SharedLessonItem *item __attribute__((swift_name("item")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalLessonData.FirestoreLoadStatus")))
@interface SharedGlobalLessonDataFirestoreLoadStatus : SharedKotlinEnum<SharedGlobalLessonDataFirestoreLoadStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedGlobalLessonDataFirestoreLoadStatus *loaded __attribute__((swift_name("loaded")));
@property (class, readonly) SharedGlobalLessonDataFirestoreLoadStatus *notFound __attribute__((swift_name("notFound")));
@property (class, readonly) SharedGlobalLessonDataFirestoreLoadStatus *error __attribute__((swift_name("error")));
+ (SharedKotlinArray<SharedGlobalLessonDataFirestoreLoadStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedGlobalLessonDataFirestoreLoadStatus *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalLessonData.ItemWrite")))
@interface SharedGlobalLessonDataItemWrite : SharedBase
- (instancetype)initWithStableId:(NSString *)stableId fields:(NSDictionary<NSString *, id> *)fields __attribute__((swift_name("init(stableId:fields:)"))) __attribute__((objc_designated_initializer));
- (SharedGlobalLessonDataItemWrite *)doCopyStableId:(NSString *)stableId fields:(NSDictionary<NSString *, id> *)fields __attribute__((swift_name("doCopy(stableId:fields:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, id> *fields __attribute__((swift_name("fields")));
@property (readonly) NSString *stableId __attribute__((swift_name("stableId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalLessonData.LeaderboardSubmit")))
@interface SharedGlobalLessonDataLeaderboardSubmit : SharedBase
- (instancetype)initWithPartId:(int32_t)partId lessonKey:(NSString *)lessonKey score:(int32_t)score season:(int32_t)season titleUnit:(NSString * _Nullable)titleUnit __attribute__((swift_name("init(partId:lessonKey:score:season:titleUnit:)"))) __attribute__((objc_designated_initializer));
- (SharedGlobalLessonDataLeaderboardSubmit *)doCopyPartId:(int32_t)partId lessonKey:(NSString *)lessonKey score:(int32_t)score season:(int32_t)season titleUnit:(NSString * _Nullable)titleUnit __attribute__((swift_name("doCopy(partId:lessonKey:score:season:titleUnit:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *lessonKey __attribute__((swift_name("lessonKey")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) int32_t score __attribute__((swift_name("score")));
@property (readonly) int32_t season __attribute__((swift_name("season")));
@property (readonly) NSString * _Nullable titleUnit __attribute__((swift_name("titleUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalLessonData.LegacyMigrationPlan")))
@interface SharedGlobalLessonDataLegacyMigrationPlan : SharedBase
- (instancetype)initWithWrites:(NSArray<SharedGlobalLessonDataItemWrite *> *)writes legacyDocIds:(NSArray<NSString *> *)legacyDocIds __attribute__((swift_name("init(writes:legacyDocIds:)"))) __attribute__((objc_designated_initializer));
- (SharedGlobalLessonDataLegacyMigrationPlan *)doCopyWrites:(NSArray<SharedGlobalLessonDataItemWrite *> *)writes legacyDocIds:(NSArray<NSString *> *)legacyDocIds __attribute__((swift_name("doCopy(writes:legacyDocIds:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *legacyDocIds __attribute__((swift_name("legacyDocIds")));
@property (readonly) NSArray<SharedGlobalLessonDataItemWrite *> *writes __attribute__((swift_name("writes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GuidePanelData")))
@interface SharedGuidePanelData : SharedBase
- (instancetype)initWithEmote:(NSString *)emote text:(NSString *)text __attribute__((swift_name("init(emote:text:)"))) __attribute__((objc_designated_initializer));
- (SharedGuidePanelData *)doCopyEmote:(NSString *)emote text:(NSString *)text __attribute__((swift_name("doCopy(emote:text:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *emote __attribute__((swift_name("emote")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic")))
@interface SharedLessonAdapterLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonAdapterLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonAdapterLogic *shared __attribute__((swift_name("shared")));
- (SharedLessonAdapterLogicActionButtonDecision *)actionButtonDecisionItem:(SharedLessonItem *)item isInfiniteEnergy:(BOOL)isInfiniteEnergy hasEnoughEnergy:(BOOL)hasEnoughEnergy globalPartId:(int32_t)globalPartId __attribute__((swift_name("actionButtonDecision(item:isInfiniteEnergy:hasEnoughEnergy:globalPartId:)")));
- (NSString *)cardIconNameItem:(SharedLessonItem *)item globalPartId:(int32_t)globalPartId __attribute__((swift_name("cardIconName(item:globalPartId:)")));
- (SharedLessonAdapterLogicCardState *)cardStateItem:(SharedLessonItem *)item goldAnimationPlayed:(BOOL)goldAnimationPlayed __attribute__((swift_name("cardState(item:goldAnimationPlayed:)")));
- (SharedLessonAdapterLogicChestCardStars *)chestCardStarsItem:(SharedLessonItem *)item goldDone:(BOOL)goldDone __attribute__((swift_name("chestCardStars(item:goldDone:)")));
- (int32_t)chestStarCountItem:(SharedLessonItem *)item __attribute__((swift_name("chestStarCount(item:)")));
- (SharedLessonAdapterLogicChestStarsRow *)chestStarsForBindItem:(SharedLessonItem *)item __attribute__((swift_name("chestStarsForBind(item:)")));
- (SharedLessonAdapterLogicChestStarsRow *)chestStarsRowTierResId:(NSString *)tierResId isCompleted:(BOOL)isCompleted __attribute__((swift_name("chestStarsRow(tierResId:isCompleted:)")));
- (SharedLessonAdapterLogicLessonStartTarget *)continueWithLessonTargetItem:(SharedLessonItem *)item __attribute__((swift_name("continueWithLessonTarget(item:)")));
- (NSString *)formatRaceTimePeriodDescriptionTimePeriodMs:(SharedLong * _Nullable)timePeriodMs __attribute__((swift_name("formatRaceTimePeriodDescription(timePeriodMs:)")));
- (BOOL)isKeyUnlockableItem:(SharedLessonItem *)item globalPartId:(int32_t)globalPartId __attribute__((swift_name("isKeyUnlockable(item:globalPartId:)")));
- (int32_t)itemViewTypeItems:(NSArray<SharedLessonItem *> *)items position:(int32_t)position __attribute__((swift_name("itemViewType(items:position:)")));
- (SharedLessonAdapterLogicLessonBottomSheetState *)lessonBottomSheetStateItem:(SharedLessonItem *)item globalPartId:(int32_t)globalPartId __attribute__((swift_name("lessonBottomSheetState(item:globalPartId:)")));
- (NSString *)lessonCardColorItem:(SharedLessonItem *)item __attribute__((swift_name("lessonCardColor(item:)")));
- (NSString *)lessonProgressKeyItem:(SharedLessonItem *)item adapterPosition:(int32_t)adapterPosition __attribute__((swift_name("lessonProgressKey(item:adapterPosition:)")));
- (SharedLessonAdapterLogicLessonStartGlobals *)lessonStartGlobalsItem:(SharedLessonItem *)item __attribute__((swift_name("lessonStartGlobals(item:)")));
- (NSString *)normalizedCupIconStepCupIcon:(NSString *)stepCupIcon __attribute__((swift_name("normalizedCupIcon(stepCupIcon:)")));
- (int32_t)partIdAfterRacePanelCloseItem:(SharedLessonItem *)item isFromPartSelection:(BOOL)isFromPartSelection currentGlobalPartId:(int32_t)currentGlobalPartId __attribute__((swift_name("partIdAfterRacePanelClose(item:isFromPartSelection:currentGlobalPartId:)")));
- (SharedLessonAdapterLogicProgressAnimationDecision *)progressAnimationDecisionItem:(SharedLessonItem *)item goldAnimationPlayed:(BOOL)goldAnimationPlayed pendingFromFilled:(SharedInt * _Nullable)pendingFromFilled canConsumePending:(BOOL)canConsumePending lastSeenFilled:(SharedInt * _Nullable)lastSeenFilled __attribute__((swift_name("progressAnimationDecision(item:goldAnimationPlayed:pendingFromFilled:canConsumePending:lastSeenFilled:)")));
- (NSString *)raceCardColorItem:(SharedLessonItem *)item __attribute__((swift_name("raceCardColor(item:)")));
- (BOOL)raceFlagClickAllowedItem:(SharedLessonItem *)item __attribute__((swift_name("raceFlagClickAllowed(item:)")));
- (SharedLessonAdapterLogicRaceLessonBottomSheetState *)raceLessonBottomSheetStateRaceItem:(SharedLessonItem *)raceItem __attribute__((swift_name("raceLessonBottomSheetState(raceItem:)")));
- (SharedLessonAdapterLogicRaceRowStatus *)raceRowStatusItem:(SharedLessonItem *)item __attribute__((swift_name("raceRowStatus(item:)")));
- (SharedLessonAdapterLogicLessonStartGlobals *)raceStartGlobalsRaceItem:(SharedLessonItem *)raceItem current:(SharedLessonAdapterLogicLessonStartGlobals *)current __attribute__((swift_name("raceStartGlobals(raceItem:current:)")));
- (NSArray<SharedLessonItem *> *)refreshedRacePanelItemsCurrentTitle:(NSString *)currentTitle lessonItems:(NSArray<SharedLessonItem *> *)lessonItems __attribute__((swift_name("refreshedRacePanelItems(currentTitle:lessonItems:)")));
- (BOOL)shouldStartBreathingAnimationItem:(SharedLessonItem *)item goldAnimationPlayed:(BOOL)goldAnimationPlayed __attribute__((swift_name("shouldStartBreathingAnimation(item:goldAnimationPlayed:)")));
- (SharedLessonAdapterLogicStepSegments *)stepSegmentsItem:(SharedLessonItem *)item __attribute__((swift_name("stepSegments(item:)")));
- (SharedLessonItem *)unlockedWithKeyItem:(SharedLessonItem *)item __attribute__((swift_name("unlockedWithKey(item:)")));
@property (readonly) NSString *ICON_BOOK __attribute__((swift_name("ICON_BOOK")));
@property (readonly) NSString *ICON_PODIUM __attribute__((swift_name("ICON_PODIUM")));
@property (readonly) int32_t LESSON_ENERGY_COST __attribute__((swift_name("LESSON_ENERGY_COST")));
@property (readonly) int32_t LESSON_UNLOCK_KEY_COST __attribute__((swift_name("LESSON_UNLOCK_KEY_COST")));
@property (readonly) int32_t RACE_FAST_FORWARD_KEY_COST __attribute__((swift_name("RACE_FAST_FORWARD_KEY_COST")));
@property (readonly) NSSet<SharedInt *> *RECORD_HIDDEN_PARTS __attribute__((swift_name("RECORD_HIDDEN_PARTS")));
@property (readonly) float SEGMENT_GAP_ANGLE __attribute__((swift_name("SEGMENT_GAP_ANGLE")));
@property (readonly) NSString *STRING_KEY_INSUFFICIENT_KEYS __attribute__((swift_name("STRING_KEY_INSUFFICIENT_KEYS")));
@property (readonly) NSString *STRING_KEY_RACE_UNLOCK __attribute__((swift_name("STRING_KEY_RACE_UNLOCK")));
@property (readonly) NSString *TEXT_CONTINUE __attribute__((swift_name("TEXT_CONTINUE")));
@property (readonly) NSString *TEXT_KIND_CHEST __attribute__((swift_name("TEXT_KIND_CHEST")));
@property (readonly) NSString *TEXT_KIND_LESSON __attribute__((swift_name("TEXT_KIND_LESSON")));
@property (readonly) NSString *TEXT_LESSON_DONE __attribute__((swift_name("TEXT_LESSON_DONE")));
@property (readonly) NSString *TEXT_LOCKED __attribute__((swift_name("TEXT_LOCKED")));
@property (readonly) NSString *TEXT_LOCKED_DESCRIPTION __attribute__((swift_name("TEXT_LOCKED_DESCRIPTION")));
@property (readonly) NSString *TEXT_RACE_START __attribute__((swift_name("TEXT_RACE_START")));
@property (readonly) NSString *TEXT_RETRY __attribute__((swift_name("TEXT_RETRY")));
@property (readonly) NSString *TEXT_REVIEW __attribute__((swift_name("TEXT_REVIEW")));
@property (readonly) NSString *TEXT_START __attribute__((swift_name("TEXT_START")));
@property (readonly) NSString *TEXT_UNLOCK_OFFER_DESCRIPTION __attribute__((swift_name("TEXT_UNLOCK_OFFER_DESCRIPTION")));
@property (readonly) NSString *TEXT_UNLOCK_WITH_KEY __attribute__((swift_name("TEXT_UNLOCK_WITH_KEY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.ActionButtonDecision")))
@interface SharedLessonAdapterLogicActionButtonDecision : SharedKotlinEnum<SharedLessonAdapterLogicActionButtonDecision *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonAdapterLogicActionButtonDecision *lockedCollapse __attribute__((swift_name("lockedCollapse")));
@property (class, readonly) SharedLessonAdapterLogicActionButtonDecision *notEnoughEnergy __attribute__((swift_name("notEnoughEnergy")));
@property (class, readonly) SharedLessonAdapterLogicActionButtonDecision *useEnergyAndStart __attribute__((swift_name("useEnergyAndStart")));
@property (class, readonly) SharedLessonAdapterLogicActionButtonDecision *startWithoutEnergy __attribute__((swift_name("startWithoutEnergy")));
@property (class, readonly) SharedLessonAdapterLogicActionButtonDecision *unlockWithKey __attribute__((swift_name("unlockWithKey")));
+ (SharedKotlinArray<SharedLessonAdapterLogicActionButtonDecision *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonAdapterLogicActionButtonDecision *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.CardState")))
@interface SharedLessonAdapterLogicCardState : SharedKotlinEnum<SharedLessonAdapterLogicCardState *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonAdapterLogicCardState *locked __attribute__((swift_name("locked")));
@property (class, readonly) SharedLessonAdapterLogicCardState *open __attribute__((swift_name("open")));
@property (class, readonly) SharedLessonAdapterLogicCardState *done __attribute__((swift_name("done")));
+ (SharedKotlinArray<SharedLessonAdapterLogicCardState *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonAdapterLogicCardState *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.ChestCardStars")))
@interface SharedLessonAdapterLogicChestCardStars : SharedBase
- (instancetype)initWithEarned:(int32_t)earned unlocked:(BOOL)unlocked done:(BOOL)done __attribute__((swift_name("init(earned:unlocked:done:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicChestCardStars *)doCopyEarned:(int32_t)earned unlocked:(BOOL)unlocked done:(BOOL)done __attribute__((swift_name("doCopy(earned:unlocked:done:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL done __attribute__((swift_name("done")));
@property (readonly) int32_t earned __attribute__((swift_name("earned")));
@property (readonly) float emptyStarAlpha __attribute__((swift_name("emptyStarAlpha")));
@property (readonly) BOOL unlocked __attribute__((swift_name("unlocked")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.ChestStarSlot")))
@interface SharedLessonAdapterLogicChestStarSlot : SharedKotlinEnum<SharedLessonAdapterLogicChestStarSlot *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonAdapterLogicChestStarSlot *yellowon __attribute__((swift_name("yellowon")));
@property (class, readonly) SharedLessonAdapterLogicChestStarSlot *lightgrayon __attribute__((swift_name("lightgrayon")));
@property (class, readonly) SharedLessonAdapterLogicChestStarSlot *off __attribute__((swift_name("off")));
+ (SharedKotlinArray<SharedLessonAdapterLogicChestStarSlot *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonAdapterLogicChestStarSlot *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.ChestStarsRow")))
@interface SharedLessonAdapterLogicChestStarsRow : SharedBase
- (instancetype)initWithThreeStarMode:(BOOL)threeStarMode slots:(NSArray<SharedLessonAdapterLogicChestStarSlot *> *)slots __attribute__((swift_name("init(threeStarMode:slots:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicChestStarsRow *)doCopyThreeStarMode:(BOOL)threeStarMode slots:(NSArray<SharedLessonAdapterLogicChestStarSlot *> *)slots __attribute__((swift_name("doCopy(threeStarMode:slots:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedLessonAdapterLogicChestStarSlot *> *slots __attribute__((swift_name("slots")));
@property (readonly) BOOL threeStarMode __attribute__((swift_name("threeStarMode")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.LessonBottomSheetState")))
@interface SharedLessonAdapterLogicLessonBottomSheetState : SharedBase
- (instancetype)initWithTitle:(NSString *)title description:(NSString *)description actionText:(NSString *)actionText actionEnabled:(BOOL)actionEnabled locked:(BOOL)locked againTutorialVisible:(BOOL)againTutorialVisible recordText:(NSString * _Nullable)recordText recordLayoutGone:(BOOL)recordLayoutGone recordClickable:(BOOL)recordClickable fireAnimVisible:(BOOL)fireAnimVisible panelState:(SharedLessonAdapterLogicPanelState *)panelState kindText:(NSString *)kindText iconName:(NSString *)iconName stepRowVisible:(BOOL)stepRowVisible doneSteps:(int32_t)doneSteps stepCount:(int32_t)stepCount keyUnlockable:(BOOL)keyUnlockable __attribute__((swift_name("init(title:description:actionText:actionEnabled:locked:againTutorialVisible:recordText:recordLayoutGone:recordClickable:fireAnimVisible:panelState:kindText:iconName:stepRowVisible:doneSteps:stepCount:keyUnlockable:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicLessonBottomSheetState *)doCopyTitle:(NSString *)title description:(NSString *)description actionText:(NSString *)actionText actionEnabled:(BOOL)actionEnabled locked:(BOOL)locked againTutorialVisible:(BOOL)againTutorialVisible recordText:(NSString * _Nullable)recordText recordLayoutGone:(BOOL)recordLayoutGone recordClickable:(BOOL)recordClickable fireAnimVisible:(BOOL)fireAnimVisible panelState:(SharedLessonAdapterLogicPanelState *)panelState kindText:(NSString *)kindText iconName:(NSString *)iconName stepRowVisible:(BOOL)stepRowVisible doneSteps:(int32_t)doneSteps stepCount:(int32_t)stepCount keyUnlockable:(BOOL)keyUnlockable __attribute__((swift_name("doCopy(title:description:actionText:actionEnabled:locked:againTutorialVisible:recordText:recordLayoutGone:recordClickable:fireAnimVisible:panelState:kindText:iconName:stepRowVisible:doneSteps:stepCount:keyUnlockable:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL actionEnabled __attribute__((swift_name("actionEnabled")));
@property (readonly) NSString *actionText __attribute__((swift_name("actionText")));
@property (readonly) BOOL againTutorialVisible __attribute__((swift_name("againTutorialVisible")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t doneSteps __attribute__((swift_name("doneSteps")));
@property (readonly) BOOL fireAnimVisible __attribute__((swift_name("fireAnimVisible")));
@property (readonly) NSString *iconName __attribute__((swift_name("iconName")));
@property (readonly) BOOL keyUnlockable __attribute__((swift_name("keyUnlockable")));
@property (readonly) NSString *kindText __attribute__((swift_name("kindText")));
@property (readonly) BOOL locked __attribute__((swift_name("locked")));
@property (readonly) SharedLessonAdapterLogicPanelState *panelState __attribute__((swift_name("panelState")));
@property (readonly) BOOL recordClickable __attribute__((swift_name("recordClickable")));
@property (readonly) BOOL recordLayoutGone __attribute__((swift_name("recordLayoutGone")));
@property (readonly) NSString * _Nullable recordText __attribute__((swift_name("recordText")));
@property (readonly) int32_t stepCount __attribute__((swift_name("stepCount")));
@property (readonly) BOOL stepRowVisible __attribute__((swift_name("stepRowVisible")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.LessonStartGlobals")))
@interface SharedLessonAdapterLogicLessonStartGlobals : SharedBase
- (instancetype)initWithMapFragmentStepIndex:(int32_t)mapFragmentStepIndex lessonStep:(int32_t)lessonStep __attribute__((swift_name("init(mapFragmentStepIndex:lessonStep:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicLessonStartGlobals *)doCopyMapFragmentStepIndex:(int32_t)mapFragmentStepIndex lessonStep:(int32_t)lessonStep __attribute__((swift_name("doCopy(mapFragmentStepIndex:lessonStep:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t lessonStep __attribute__((swift_name("lessonStep")));
@property (readonly) int32_t mapFragmentStepIndex __attribute__((swift_name("mapFragmentStepIndex")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.LessonStartTarget")))
@interface SharedLessonAdapterLogicLessonStartTarget : SharedKotlinEnum<SharedLessonAdapterLogicLessonStartTarget *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonAdapterLogicLessonStartTarget *blindingLesson __attribute__((swift_name("blindingLesson")));
@property (class, readonly) SharedLessonAdapterLogicLessonStartTarget *abacus __attribute__((swift_name("abacus")));
@property (class, readonly) SharedLessonAdapterLogicLessonStartTarget *tutorial __attribute__((swift_name("tutorial")));
+ (SharedKotlinArray<SharedLessonAdapterLogicLessonStartTarget *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonAdapterLogicLessonStartTarget *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.PanelState")))
@interface SharedLessonAdapterLogicPanelState : SharedKotlinEnum<SharedLessonAdapterLogicPanelState *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonAdapterLogicPanelState *open __attribute__((swift_name("open")));
@property (class, readonly) SharedLessonAdapterLogicPanelState *done __attribute__((swift_name("done")));
@property (class, readonly) SharedLessonAdapterLogicPanelState *locked __attribute__((swift_name("locked")));
+ (SharedKotlinArray<SharedLessonAdapterLogicPanelState *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonAdapterLogicPanelState *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.ProgressAnimationAction")))
@interface SharedLessonAdapterLogicProgressAnimationAction : SharedKotlinEnum<SharedLessonAdapterLogicProgressAnimationAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonAdapterLogicProgressAnimationAction *applyPersistentGold __attribute__((swift_name("applyPersistentGold")));
@property (class, readonly) SharedLessonAdapterLogicProgressAnimationAction *animateIncrease __attribute__((swift_name("animateIncrease")));
@property (class, readonly) SharedLessonAdapterLogicProgressAnimationAction *showPrevious __attribute__((swift_name("showPrevious")));
@property (class, readonly) SharedLessonAdapterLogicProgressAnimationAction *showTarget __attribute__((swift_name("showTarget")));
@property (class, readonly) SharedLessonAdapterLogicProgressAnimationAction *showTargetAndPlayGold __attribute__((swift_name("showTargetAndPlayGold")));
+ (SharedKotlinArray<SharedLessonAdapterLogicProgressAnimationAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonAdapterLogicProgressAnimationAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.ProgressAnimationDecision")))
@interface SharedLessonAdapterLogicProgressAnimationDecision : SharedBase
- (instancetype)initWithAction:(SharedLessonAdapterLogicProgressAnimationAction *)action segmentCount:(int32_t)segmentCount previousFilled:(int32_t)previousFilled targetFilled:(int32_t)targetFilled durationMs:(int64_t)durationMs playGoldAfterIncrease:(BOOL)playGoldAfterIncrease removePending:(BOOL)removePending lastSeenFilled:(int32_t)lastSeenFilled persistFinalGold:(BOOL)persistFinalGold __attribute__((swift_name("init(action:segmentCount:previousFilled:targetFilled:durationMs:playGoldAfterIncrease:removePending:lastSeenFilled:persistFinalGold:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicProgressAnimationDecision *)doCopyAction:(SharedLessonAdapterLogicProgressAnimationAction *)action segmentCount:(int32_t)segmentCount previousFilled:(int32_t)previousFilled targetFilled:(int32_t)targetFilled durationMs:(int64_t)durationMs playGoldAfterIncrease:(BOOL)playGoldAfterIncrease removePending:(BOOL)removePending lastSeenFilled:(int32_t)lastSeenFilled persistFinalGold:(BOOL)persistFinalGold __attribute__((swift_name("doCopy(action:segmentCount:previousFilled:targetFilled:durationMs:playGoldAfterIncrease:removePending:lastSeenFilled:persistFinalGold:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLessonAdapterLogicProgressAnimationAction *action __attribute__((swift_name("action")));
@property (readonly) int64_t durationMs __attribute__((swift_name("durationMs")));
@property (readonly) int32_t lastSeenFilled __attribute__((swift_name("lastSeenFilled")));
@property (readonly) BOOL persistFinalGold __attribute__((swift_name("persistFinalGold")));
@property (readonly) BOOL playGoldAfterIncrease __attribute__((swift_name("playGoldAfterIncrease")));
@property (readonly) int32_t previousFilled __attribute__((swift_name("previousFilled")));
@property (readonly) BOOL removePending __attribute__((swift_name("removePending")));
@property (readonly) int32_t segmentCount __attribute__((swift_name("segmentCount")));
@property (readonly) int32_t targetFilled __attribute__((swift_name("targetFilled")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.RaceLessonBottomSheetState")))
@interface SharedLessonAdapterLogicRaceLessonBottomSheetState : SharedBase
- (instancetype)initWithTitle:(NSString *)title description:(NSString *)description isLockedRace:(BOOL)isLockedRace actionText:(NSString *)actionText actionAllCaps:(BOOL)actionAllCaps actionIcon:(NSString * _Nullable)actionIcon __attribute__((swift_name("init(title:description:isLockedRace:actionText:actionAllCaps:actionIcon:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicRaceLessonBottomSheetState *)doCopyTitle:(NSString *)title description:(NSString *)description isLockedRace:(BOOL)isLockedRace actionText:(NSString *)actionText actionAllCaps:(BOOL)actionAllCaps actionIcon:(NSString * _Nullable)actionIcon __attribute__((swift_name("doCopy(title:description:isLockedRace:actionText:actionAllCaps:actionIcon:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL actionAllCaps __attribute__((swift_name("actionAllCaps")));
@property (readonly) NSString * _Nullable actionIcon __attribute__((swift_name("actionIcon")));
@property (readonly) NSString *actionText __attribute__((swift_name("actionText")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) BOOL isLockedRace __attribute__((swift_name("isLockedRace")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.RaceRowStatus")))
@interface SharedLessonAdapterLogicRaceRowStatus : SharedBase
- (instancetype)initWithStatusText:(NSString *)statusText lockVisible:(BOOL)lockVisible statusEnabled:(BOOL)statusEnabled __attribute__((swift_name("init(statusText:lockVisible:statusEnabled:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicRaceRowStatus *)doCopyStatusText:(NSString *)statusText lockVisible:(BOOL)lockVisible statusEnabled:(BOOL)statusEnabled __attribute__((swift_name("doCopy(statusText:lockVisible:statusEnabled:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL lockVisible __attribute__((swift_name("lockVisible")));
@property (readonly) BOOL statusEnabled __attribute__((swift_name("statusEnabled")));
@property (readonly) NSString *statusText __attribute__((swift_name("statusText")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonAdapterLogic.StepSegments")))
@interface SharedLessonAdapterLogicStepSegments : SharedBase
- (instancetype)initWithSegmentCount:(int32_t)segmentCount completedSegments:(int32_t)completedSegments __attribute__((swift_name("init(segmentCount:completedSegments:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonAdapterLogicStepSegments *)doCopySegmentCount:(int32_t)segmentCount completedSegments:(int32_t)completedSegments __attribute__((swift_name("doCopy(segmentCount:completedSegments:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t completedSegments __attribute__((swift_name("completedSegments")));
@property (readonly) int32_t segmentCount __attribute__((swift_name("segmentCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonDataManager")))
@interface SharedLessonDataManager : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonDataManager __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonDataManager *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *KEY_LESSON_ITEMS __attribute__((swift_name("KEY_LESSON_ITEMS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonEndMascotDraw")))
@interface SharedLessonEndMascotDraw : SharedBase
- (instancetype)initWithEmote:(NSString *)emote bag:(NSString *)bag last:(NSString *)last __attribute__((swift_name("init(emote:bag:last:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonEndMascotDraw *)doCopyEmote:(NSString *)emote bag:(NSString *)bag last:(NSString *)last __attribute__((swift_name("doCopy(emote:bag:last:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *bag __attribute__((swift_name("bag")));
@property (readonly) NSString *emote __attribute__((swift_name("emote")));
@property (readonly) NSString *last __attribute__((swift_name("last")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonEndMascotLogic")))
@interface SharedLessonEndMascotLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonEndMascotLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonEndMascotLogic *shared __attribute__((swift_name("shared")));
- (SharedLessonEndMascotDraw *)nextSuccessStoredBag:(NSString * _Nullable)storedBag storedLast:(NSString * _Nullable)storedLast __attribute__((swift_name("nextSuccess(storedBag:storedLast:)")));
- (SharedLessonEndMascotDraw *)nextSuccessWithStoredBag:(NSString * _Nullable)storedBag storedLast:(NSString * _Nullable)storedLast random:(SharedKotlinRandom *)random __attribute__((swift_name("nextSuccessWith(storedBag:storedLast:random:)")));
@property (readonly) int64_t ENTER_DELAY_MS __attribute__((swift_name("ENTER_DELAY_MS")));
@property (readonly) NSString *FAILURE __attribute__((swift_name("FAILURE")));
@property (readonly) NSArray<NSString *> *SUCCESS __attribute__((swift_name("SUCCESS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonItem")))
@interface SharedLessonItem : SharedBase
- (instancetype)initWithStableId:(NSString *)stableId id:(SharedInt * _Nullable)id type:(int32_t)type title:(NSString *)title partId:(SharedInt * _Nullable)partId offset:(int32_t)offset isCompleted:(BOOL)isCompleted progressBarLevel:(int32_t)progressBarLevel stepCount:(int32_t)stepCount currentStep:(int32_t)currentStep fragment:(NSString * _Nullable)fragment color:(NSString * _Nullable)color LESSON_ID:(SharedInt * _Nullable)LESSON_ID isBlinding:(SharedBoolean * _Nullable)isBlinding isExtraction:(SharedBoolean * _Nullable)isExtraction isMultiplication:(SharedBoolean * _Nullable)isMultiplication stepCompletionStatus:(NSArray<SharedBoolean *> *)stepCompletionStatus finishStepNumber:(SharedInt * _Nullable)finishStepNumber startStepNumber:(SharedInt * _Nullable)startStepNumber mapFragmentIndex:(SharedInt * _Nullable)mapFragmentIndex stepIsFinish:(BOOL)stepIsFinish finalGoldVisualUnlocked:(BOOL)finalGoldVisualUnlocked tutorialNumber:(int32_t)tutorialNumber tutorialIsFinish:(BOOL)tutorialIsFinish lessonHint:(NSString * _Nullable)lessonHint stepCupIcon:(NSString *)stepCupIcon cupPoint1:(SharedInt * _Nullable)cupPoint1 cupPoint2:(SharedInt * _Nullable)cupPoint2 worstCupTime:(SharedInt * _Nullable)worstCupTime sectionTitle:(NSString * _Nullable)sectionTitle backPart:(SharedBoolean * _Nullable)backPart sectionDescription:(NSString * _Nullable)sectionDescription timePeriod:(SharedLong * _Nullable)timePeriod blindingMultiplication:(SharedBoolean * _Nullable)blindingMultiplication racePartId:(SharedInt * _Nullable)racePartId backRaceId:(SharedInt * _Nullable)backRaceId raceBusyLevel:(SharedInt * _Nullable)raceBusyLevel cupDifficultyLevel:(SharedInt * _Nullable)cupDifficultyLevel cupWinDelta:(SharedInt * _Nullable)cupWinDelta cupLossDelta:(SharedInt * _Nullable)cupLossDelta record:(SharedInt * _Nullable)record leaderboardSeasonBest:(SharedInt * _Nullable)leaderboardSeasonBest leaderboardSeasonId:(SharedInt * _Nullable)leaderboardSeasonId abacusGuideNumber:(SharedInt * _Nullable)abacusGuideNumber titleUnit:(NSString * _Nullable)titleUnit raceTitle:(NSString * _Nullable)raceTitle cupDigitSize:(SharedInt * _Nullable)cupDigitSize cupNumberCount:(SharedInt * _Nullable)cupNumberCount __attribute__((swift_name("init(stableId:id:type:title:partId:offset:isCompleted:progressBarLevel:stepCount:currentStep:fragment:color:LESSON_ID:isBlinding:isExtraction:isMultiplication:stepCompletionStatus:finishStepNumber:startStepNumber:mapFragmentIndex:stepIsFinish:finalGoldVisualUnlocked:tutorialNumber:tutorialIsFinish:lessonHint:stepCupIcon:cupPoint1:cupPoint2:worstCupTime:sectionTitle:backPart:sectionDescription:timePeriod:blindingMultiplication:racePartId:backRaceId:raceBusyLevel:cupDifficultyLevel:cupWinDelta:cupLossDelta:record:leaderboardSeasonBest:leaderboardSeasonId:abacusGuideNumber:titleUnit:raceTitle:cupDigitSize:cupNumberCount:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedLessonItemCompanion *companion __attribute__((swift_name("companion")));
- (SharedLessonItem *)doCopyStableId:(NSString *)stableId id:(SharedInt * _Nullable)id type:(int32_t)type title:(NSString *)title partId:(SharedInt * _Nullable)partId offset:(int32_t)offset isCompleted:(BOOL)isCompleted progressBarLevel:(int32_t)progressBarLevel stepCount:(int32_t)stepCount currentStep:(int32_t)currentStep fragment:(NSString * _Nullable)fragment color:(NSString * _Nullable)color LESSON_ID:(SharedInt * _Nullable)LESSON_ID isBlinding:(SharedBoolean * _Nullable)isBlinding isExtraction:(SharedBoolean * _Nullable)isExtraction isMultiplication:(SharedBoolean * _Nullable)isMultiplication stepCompletionStatus:(NSArray<SharedBoolean *> *)stepCompletionStatus finishStepNumber:(SharedInt * _Nullable)finishStepNumber startStepNumber:(SharedInt * _Nullable)startStepNumber mapFragmentIndex:(SharedInt * _Nullable)mapFragmentIndex stepIsFinish:(BOOL)stepIsFinish finalGoldVisualUnlocked:(BOOL)finalGoldVisualUnlocked tutorialNumber:(int32_t)tutorialNumber tutorialIsFinish:(BOOL)tutorialIsFinish lessonHint:(NSString * _Nullable)lessonHint stepCupIcon:(NSString *)stepCupIcon cupPoint1:(SharedInt * _Nullable)cupPoint1 cupPoint2:(SharedInt * _Nullable)cupPoint2 worstCupTime:(SharedInt * _Nullable)worstCupTime sectionTitle:(NSString * _Nullable)sectionTitle backPart:(SharedBoolean * _Nullable)backPart sectionDescription:(NSString * _Nullable)sectionDescription timePeriod:(SharedLong * _Nullable)timePeriod blindingMultiplication:(SharedBoolean * _Nullable)blindingMultiplication racePartId:(SharedInt * _Nullable)racePartId backRaceId:(SharedInt * _Nullable)backRaceId raceBusyLevel:(SharedInt * _Nullable)raceBusyLevel cupDifficultyLevel:(SharedInt * _Nullable)cupDifficultyLevel cupWinDelta:(SharedInt * _Nullable)cupWinDelta cupLossDelta:(SharedInt * _Nullable)cupLossDelta record:(SharedInt * _Nullable)record leaderboardSeasonBest:(SharedInt * _Nullable)leaderboardSeasonBest leaderboardSeasonId:(SharedInt * _Nullable)leaderboardSeasonId abacusGuideNumber:(SharedInt * _Nullable)abacusGuideNumber titleUnit:(NSString * _Nullable)titleUnit raceTitle:(NSString * _Nullable)raceTitle cupDigitSize:(SharedInt * _Nullable)cupDigitSize cupNumberCount:(SharedInt * _Nullable)cupNumberCount __attribute__((swift_name("doCopy(stableId:id:type:title:partId:offset:isCompleted:progressBarLevel:stepCount:currentStep:fragment:color:LESSON_ID:isBlinding:isExtraction:isMultiplication:stepCompletionStatus:finishStepNumber:startStepNumber:mapFragmentIndex:stepIsFinish:finalGoldVisualUnlocked:tutorialNumber:tutorialIsFinish:lessonHint:stepCupIcon:cupPoint1:cupPoint2:worstCupTime:sectionTitle:backPart:sectionDescription:timePeriod:blindingMultiplication:racePartId:backRaceId:raceBusyLevel:cupDifficultyLevel:cupWinDelta:cupLossDelta:record:leaderboardSeasonBest:leaderboardSeasonId:abacusGuideNumber:titleUnit:raceTitle:cupDigitSize:cupNumberCount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedInt * _Nullable)leaderboardSubmitScoreSeason:(int32_t)season __attribute__((swift_name("leaderboardSubmitScore(season:)")));
- (int32_t)minLessonOperationsId __attribute__((swift_name("minLessonOperationsId()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property SharedInt * _Nullable LESSON_ID __attribute__((swift_name("LESSON_ID")));
@property SharedInt * _Nullable abacusGuideNumber __attribute__((swift_name("abacusGuideNumber")));
@property SharedBoolean * _Nullable backPart __attribute__((swift_name("backPart")));
@property SharedInt * _Nullable backRaceId __attribute__((swift_name("backRaceId")));
@property SharedBoolean * _Nullable blindingMultiplication __attribute__((swift_name("blindingMultiplication")));
@property (readonly) NSString * _Nullable color __attribute__((swift_name("color")));
@property SharedInt * _Nullable cupDifficultyLevel __attribute__((swift_name("cupDifficultyLevel")));
@property SharedInt * _Nullable cupDigitSize __attribute__((swift_name("cupDigitSize")));
@property SharedInt * _Nullable cupLossDelta __attribute__((swift_name("cupLossDelta")));
@property SharedInt * _Nullable cupNumberCount __attribute__((swift_name("cupNumberCount")));
@property SharedInt * _Nullable cupPoint1 __attribute__((swift_name("cupPoint1")));
@property SharedInt * _Nullable cupPoint2 __attribute__((swift_name("cupPoint2")));
@property SharedInt * _Nullable cupWinDelta __attribute__((swift_name("cupWinDelta")));
@property int32_t currentStep __attribute__((swift_name("currentStep")));
@property BOOL finalGoldVisualUnlocked __attribute__((swift_name("finalGoldVisualUnlocked")));
@property SharedInt * _Nullable finishStepNumber __attribute__((swift_name("finishStepNumber")));
@property NSString * _Nullable fragment __attribute__((swift_name("fragment")));
@property (readonly) SharedInt * _Nullable id __attribute__((swift_name("id")));
@property SharedBoolean * _Nullable isBlinding __attribute__((swift_name("isBlinding")));
@property BOOL isCompleted __attribute__((swift_name("isCompleted")));
@property SharedBoolean * _Nullable isExtraction __attribute__((swift_name("isExtraction")));
@property SharedBoolean * _Nullable isMultiplication __attribute__((swift_name("isMultiplication")));
@property SharedInt * _Nullable leaderboardSeasonBest __attribute__((swift_name("leaderboardSeasonBest")));
@property SharedInt * _Nullable leaderboardSeasonId __attribute__((swift_name("leaderboardSeasonId")));
@property NSString * _Nullable lessonHint __attribute__((swift_name("lessonHint")));
@property SharedInt * _Nullable mapFragmentIndex __attribute__((swift_name("mapFragmentIndex")));
@property int32_t offset __attribute__((swift_name("offset")));
@property SharedInt * _Nullable partId __attribute__((swift_name("partId")));
@property int32_t progressBarLevel __attribute__((swift_name("progressBarLevel")));
@property SharedInt * _Nullable raceBusyLevel __attribute__((swift_name("raceBusyLevel")));
@property SharedInt * _Nullable racePartId __attribute__((swift_name("racePartId")));
@property NSString * _Nullable raceTitle __attribute__((swift_name("raceTitle")));
@property SharedInt * _Nullable record __attribute__((swift_name("record")));
@property NSString * _Nullable sectionDescription __attribute__((swift_name("sectionDescription")));
@property NSString * _Nullable sectionTitle __attribute__((swift_name("sectionTitle")));
@property (readonly) NSString *stableId __attribute__((swift_name("stableId")));
@property SharedInt * _Nullable startStepNumber __attribute__((swift_name("startStepNumber")));
@property NSArray<SharedBoolean *> *stepCompletionStatus __attribute__((swift_name("stepCompletionStatus")));
@property int32_t stepCount __attribute__((swift_name("stepCount")));
@property NSString *stepCupIcon __attribute__((swift_name("stepCupIcon")));
@property BOOL stepIsFinish __attribute__((swift_name("stepIsFinish")));
@property SharedLong * _Nullable timePeriod __attribute__((swift_name("timePeriod")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property NSString * _Nullable titleUnit __attribute__((swift_name("titleUnit")));
@property BOOL tutorialIsFinish __attribute__((swift_name("tutorialIsFinish")));
@property int32_t tutorialNumber __attribute__((swift_name("tutorialNumber")));
@property (readonly) int32_t type __attribute__((swift_name("type")));
@property SharedInt * _Nullable worstCupTime __attribute__((swift_name("worstCupTime")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonItem.Companion")))
@interface SharedLessonItemCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonItemCompanion *shared __attribute__((swift_name("shared")));
- (SharedLessonItem *)mergeChestRunItem:(SharedLessonItem *)item runScore:(int32_t)runScore currentSeason:(int32_t)currentSeason __attribute__((swift_name("mergeChestRun(item:runScore:currentSeason:)")));
@property (readonly) int32_t TYPE_CHEST __attribute__((swift_name("TYPE_CHEST")));
@property (readonly) int32_t TYPE_HEADER __attribute__((swift_name("TYPE_HEADER")));
@property (readonly) int32_t TYPE_LESSON __attribute__((swift_name("TYPE_LESSON")));
@property (readonly) int32_t TYPE_RACE __attribute__((swift_name("TYPE_RACE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonManager")))
@interface SharedLessonManager : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonManager __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonManager *shared __attribute__((swift_name("shared")));
- (void)ensureInitializedPartId:(int32_t)partId initialize:(void (^)(SharedInt *, SharedKotlinUnit *(^)(void)))initialize __attribute__((swift_name("ensureInitialized(partId:initialize:)")));
- (SharedLessonItem * _Nullable)getLessonItemPosition:(int32_t)position __attribute__((swift_name("getLessonItem(position:)")));
- (void)refreshLessonsFromGlobalData __attribute__((swift_name("refreshLessonsFromGlobalData()")));
- (void)setAdapterAdapter:(id<SharedLessonManagerLessonAdapterBridge> _Nullable)adapter __attribute__((swift_name("setAdapter(adapter:)")));
- (void)setRaceAdapterRaceAdapter:(id<SharedLessonManagerRaceAdapterBridge> _Nullable)raceAdapter __attribute__((swift_name("setRaceAdapter(raceAdapter:)")));
- (void)updateLessonItemPosition:(int32_t)position item:(SharedLessonItem *)item season:(int32_t)season __attribute__((swift_name("updateLessonItem(position:item:season:)")));
- (void)updateRaceItemPosition:(int32_t)position item:(SharedLessonItem *)item season:(int32_t)season __attribute__((swift_name("updateRaceItem(position:item:season:)")));
@end

__attribute__((swift_name("LessonManagerLessonAdapterBridge")))
@protocol SharedLessonManagerLessonAdapterBridge
@required
- (BOOL)isRacePanelOpen __attribute__((swift_name("isRacePanelOpen()")));
- (void)updateItemsNewItems:(NSArray<SharedLessonItem *> *)newItems __attribute__((swift_name("updateItems(newItems:)")));
- (void)updateLessonItemPosition:(int32_t)position newItem:(SharedLessonItem *)newItem __attribute__((swift_name("updateLessonItem(position:newItem:)")));
@end

__attribute__((swift_name("LessonManagerRaceAdapterBridge")))
@protocol SharedLessonManagerRaceAdapterBridge
@required
- (void)updateRaceItemPosition:(int32_t)position item:(SharedLessonItem *)item __attribute__((swift_name("updateRaceItem(position:item:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonProgressMerge")))
@interface SharedLessonProgressMerge : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonProgressMerge __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonProgressMerge *shared __attribute__((swift_name("shared")));
- (NSMutableArray<SharedLessonItem *> *)mergeListsPreferMoreProgressLocal:(NSArray<SharedLessonItem *> *)local remote:(NSArray<SharedLessonItem *> *)remote season:(int32_t)season globalPartId:(int32_t)globalPartId __attribute__((swift_name("mergeListsPreferMoreProgress(local:remote:season:globalPartId:)")));
- (BOOL)sameProgressStateA:(NSArray<SharedLessonItem *> *)a b:(NSArray<SharedLessonItem *> *)b __attribute__((swift_name("sameProgressState(a:b:)")));
@end

__attribute__((swift_name("LessonProgressSink")))
@protocol SharedLessonProgressSink
@required
- (void)migrateLegacyItemKeysPartId:(int32_t)partId stableIds:(NSArray<NSString *> *)stableIds itemsFields:(NSArray<NSDictionary<NSString *, id> *> *)itemsFields legacyDocIds:(NSArray<NSString *> *)legacyDocIds __attribute__((swift_name("migrateLegacyItemKeys(partId:stableIds:itemsFields:legacyDocIds:)")));
- (void)submitBestIfNeededPartId:(int32_t)partId lessonKey:(NSString *)lessonKey score:(int32_t)score season:(int32_t)season titleUnit:(NSString * _Nullable)titleUnit __attribute__((swift_name("submitBestIfNeeded(partId:lessonKey:score:season:titleUnit:)")));
- (void)writeAllItemsPartId:(int32_t)partId stableIds:(NSArray<NSString *> *)stableIds itemsFields:(NSArray<NSDictionary<NSString *, id> *> *)itemsFields __attribute__((swift_name("writeAllItems(partId:stableIds:itemsFields:)")));
- (void)writeSingleItemPartId:(int32_t)partId stableId:(NSString *)stableId fields:(NSDictionary<NSString *, id> *)fields __attribute__((swift_name("writeSingleItem(partId:stableId:fields:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonResultFalseLogic")))
@interface SharedLessonResultFalseLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonResultFalseLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonResultFalseLogic *shared __attribute__((swift_name("shared")));
- (SharedLessonResultFalseLogicFailRecord * _Nullable)failRecordForItem:(SharedLessonItem * _Nullable)item mapFragmentStepIndex:(int32_t)mapFragmentStepIndex globalPartId:(int32_t)globalPartId successRate:(SharedFloat * _Nullable)successRate __attribute__((swift_name("failRecordFor(item:mapFragmentStepIndex:globalPartId:successRate:)")));
- (BOOL)isLessonTypeReturnItem:(SharedLessonItem * _Nullable)item __attribute__((swift_name("isLessonTypeReturn(item:)")));
- (NSString *)resultTitleCommentIsChestFailure:(BOOL)isChestFailure __attribute__((swift_name("resultTitleComment(isChestFailure:)")));
- (BOOL)shouldRedirectToLoginCurrentTutorialNumber:(int32_t)currentTutorialNumber isLoggedIn:(BOOL)isLoggedIn __attribute__((swift_name("shouldRedirectToLogin(currentTutorialNumber:isLoggedIn:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonResultFalseLogic.FailRecord")))
@interface SharedLessonResultFalseLogicFailRecord : SharedBase
- (instancetype)initWithPartId:(int32_t)partId position:(int32_t)position step:(int32_t)step answerSuccessRatePercent:(SharedFloat * _Nullable)answerSuccessRatePercent __attribute__((swift_name("init(partId:position:step:answerSuccessRatePercent:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonResultFalseLogicFailRecord *)doCopyPartId:(int32_t)partId position:(int32_t)position step:(int32_t)step answerSuccessRatePercent:(SharedFloat * _Nullable)answerSuccessRatePercent __attribute__((swift_name("doCopy(partId:position:step:answerSuccessRatePercent:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedFloat * _Nullable answerSuccessRatePercent __attribute__((swift_name("answerSuccessRatePercent")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) int32_t position __attribute__((swift_name("position")));
@property (readonly) int32_t step __attribute__((swift_name("step")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonResultLogic")))
@interface SharedLessonResultLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonResultLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonResultLogic *shared __attribute__((swift_name("shared")));
- (int32_t)lessonScoreSuccessRate:(float)successRate __attribute__((swift_name("lessonScore(successRate:)")));
- (SharedLessonResultLogicClaimDecision *)onClaimLessonItem:(SharedLessonItem * _Nullable)lessonItem __attribute__((swift_name("onClaim(lessonItem:)")));
- (float)successRateCorrectAnswers:(int32_t)correctAnswers totalQuestions:(int32_t)totalQuestions __attribute__((swift_name("successRate(correctAnswers:totalQuestions:)")));
- (NSString *)successRateTextSuccessRate:(float)successRate __attribute__((swift_name("successRateText(successRate:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonResultLogic.ClaimBranch")))
@interface SharedLessonResultLogicClaimBranch : SharedKotlinEnum<SharedLessonResultLogicClaimBranch *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedLessonResultLogicClaimBranch *skipToMap __attribute__((swift_name("skipToMap")));
@property (class, readonly) SharedLessonResultLogicClaimBranch *goToChest __attribute__((swift_name("goToChest")));
+ (SharedKotlinArray<SharedLessonResultLogicClaimBranch *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedLessonResultLogicClaimBranch *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonResultLogic.ClaimDecision")))
@interface SharedLessonResultLogicClaimDecision : SharedBase
- (instancetype)initWithBranch:(SharedLessonResultLogicClaimBranch *)branch recordItemReplay:(BOOL)recordItemReplay isLessonTypeReturn:(BOOL)isLessonTypeReturn __attribute__((swift_name("init(branch:recordItemReplay:isLessonTypeReturn:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonResultLogicClaimDecision *)doCopyBranch:(SharedLessonResultLogicClaimBranch *)branch recordItemReplay:(BOOL)recordItemReplay isLessonTypeReturn:(BOOL)isLessonTypeReturn __attribute__((swift_name("doCopy(branch:recordItemReplay:isLessonTypeReturn:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLessonResultLogicClaimBranch *branch __attribute__((swift_name("branch")));
@property (readonly) BOOL isLessonTypeReturn __attribute__((swift_name("isLessonTypeReturn")));
@property (readonly) BOOL recordItemReplay __attribute__((swift_name("recordItemReplay")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonSuccessRateRepository")))
@interface SharedLessonSuccessRateRepository : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonSuccessRateRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonSuccessRateRepository *shared __attribute__((swift_name("shared")));
- (SharedFloat * _Nullable)answerSuccessRateForAnalyticsPartId:(int32_t)partId answerSuccessRatePercent:(SharedFloat * _Nullable)answerSuccessRatePercent __attribute__((swift_name("answerSuccessRateForAnalytics(partId:answerSuccessRatePercent:)")));
- (BOOL)isTrackedPartPartId:(int32_t)partId __attribute__((swift_name("isTrackedPart(partId:)")));
- (NSString *)legacyUserStepStatePathUid:(NSString *)uid partId:(int32_t)partId position:(int32_t)position step:(int32_t)step __attribute__((swift_name("legacyUserStepStatePath(uid:partId:position:step:)")));
- (SharedKotlinPair<NSDictionary<NSString *, id> *, SharedBoolean *> *)pickUserSnapTarget:(NSDictionary<NSString *, id> * _Nullable)target legacy:(NSDictionary<NSString *, id> * _Nullable)legacy __attribute__((swift_name("pickUserSnap(target:legacy:)")));
- (SharedLessonSuccessRateRepositoryStepWrite * _Nullable)recordFailTarget:(NSDictionary<NSString *, id> * _Nullable)target legacy:(NSDictionary<NSString *, id> * _Nullable)legacy __attribute__((swift_name("recordFail(target:legacy:)")));
- (SharedLessonSuccessRateRepositoryStepWrite * _Nullable)recordPassTarget:(NSDictionary<NSString *, id> * _Nullable)target legacy:(NSDictionary<NSString *, id> * _Nullable)legacy __attribute__((swift_name("recordPass(target:legacy:)")));
- (BOOL)shouldLogEventPartId:(int32_t)partId isLoggedIn:(BOOL)isLoggedIn __attribute__((swift_name("shouldLogEvent(partId:isLoggedIn:)")));
- (NSString * _Nullable)stableIdOfPartId:(int32_t)partId position:(int32_t)position __attribute__((swift_name("stableIdOf(partId:position:)")));
- (NSString *)stateCollectionPathUid:(NSString *)uid __attribute__((swift_name("stateCollectionPath(uid:)")));
- (SharedLessonSuccessRateRepositoryStateRefs *)stateRefsUid:(NSString *)uid partId:(int32_t)partId position:(int32_t)position step:(int32_t)step __attribute__((swift_name("stateRefs(uid:partId:position:step:)")));
- (NSString *)userStepStatePathUid:(NSString *)uid partId:(int32_t)partId stableId:(NSString *)stableId step:(int32_t)step __attribute__((swift_name("userStepStatePath(uid:partId:stableId:step:)")));
@property (readonly) NSString *FIELD_ATTEMPTED __attribute__((swift_name("FIELD_ATTEMPTED")));
@property (readonly) NSString *FIELD_FAIL_STREAK __attribute__((swift_name("FIELD_FAIL_STREAK")));
@property (readonly) NSString *FIELD_PASSED __attribute__((swift_name("FIELD_PASSED")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonSuccessRateRepository.StateRefs")))
@interface SharedLessonSuccessRateRepositoryStateRefs : SharedBase
- (instancetype)initWithTargetPath:(NSString *)targetPath legacyPath:(NSString * _Nullable)legacyPath stableId:(NSString * _Nullable)stableId __attribute__((swift_name("init(targetPath:legacyPath:stableId:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonSuccessRateRepositoryStateRefs *)doCopyTargetPath:(NSString *)targetPath legacyPath:(NSString * _Nullable)legacyPath stableId:(NSString * _Nullable)stableId __attribute__((swift_name("doCopy(targetPath:legacyPath:stableId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable legacyPath __attribute__((swift_name("legacyPath")));
@property (readonly) NSString * _Nullable stableId __attribute__((swift_name("stableId")));
@property (readonly) NSString *targetPath __attribute__((swift_name("targetPath")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonSuccessRateRepository.StepWrite")))
@interface SharedLessonSuccessRateRepositoryStepWrite : SharedBase
- (instancetype)initWithUserState:(NSDictionary<NSString *, id> *)userState failStreak:(int64_t)failStreak deleteLegacy:(BOOL)deleteLegacy __attribute__((swift_name("init(userState:failStreak:deleteLegacy:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonSuccessRateRepositoryStepWrite *)doCopyUserState:(NSDictionary<NSString *, id> *)userState failStreak:(int64_t)failStreak deleteLegacy:(BOOL)deleteLegacy __attribute__((swift_name("doCopy(userState:failStreak:deleteLegacy:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL deleteLegacy __attribute__((swift_name("deleteLegacy")));
@property (readonly) int64_t failStreak __attribute__((swift_name("failStreak")));
@property (readonly) NSDictionary<NSString *, id> *userState __attribute__((swift_name("userState")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFragmentLogic")))
@interface SharedMapFragmentLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mapFragmentLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapFragmentLogic *shared __attribute__((swift_name("shared")));
- (int32_t)resolveLessonOperationsIdRequestedId:(int32_t)requestedId minLessonId:(int32_t)minLessonId operationsEmpty:(SharedBoolean *(^)(SharedInt *))operationsEmpty __attribute__((swift_name("resolveLessonOperationsId(requestedId:minLessonId:operationsEmpty:)")));
- (SharedMapFragmentLogicStickyHeader * _Nullable)stickyHeaderForItems:(NSArray<SharedLessonItem *> *)items firstVisible:(int32_t)firstVisible __attribute__((swift_name("stickyHeaderFor(items:firstVisible:)")));
@property (readonly) int64_t GUIDE_BOTTOM_SHEET_DELAY_MS __attribute__((swift_name("GUIDE_BOTTOM_SHEET_DELAY_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapFragmentLogic.StickyHeader")))
@interface SharedMapFragmentLogicStickyHeader : SharedBase
- (instancetype)initWithHeaderTitle:(NSString *)headerTitle sectionUnit:(NSString *)sectionUnit color:(NSString * _Nullable)color __attribute__((swift_name("init(headerTitle:sectionUnit:color:)"))) __attribute__((objc_designated_initializer));
- (SharedMapFragmentLogicStickyHeader *)doCopyHeaderTitle:(NSString *)headerTitle sectionUnit:(NSString *)sectionUnit color:(NSString * _Nullable)color __attribute__((swift_name("doCopy(headerTitle:sectionUnit:color:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable color __attribute__((swift_name("color")));
@property (readonly) NSString *headerTitle __attribute__((swift_name("headerTitle")));
@property (readonly) NSString *sectionUnit __attribute__((swift_name("sectionUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MapLayoutLogic")))
@interface SharedMapLayoutLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mapLayoutLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMapLayoutLogic *shared __attribute__((swift_name("shared")));
- (float)lessonHorizontalShiftType:(int32_t)type offset:(int32_t)offset innerWidth:(float)innerWidth maxOffset:(float)maxOffset lessonCardSize:(float)lessonCardSize lessonItemHorizontalPaddingTotal:(float)lessonItemHorizontalPaddingTotal edgeSafetyMargin:(float)edgeSafetyMargin __attribute__((swift_name("lessonHorizontalShift(type:offset:innerWidth:maxOffset:lessonCardSize:lessonItemHorizontalPaddingTotal:edgeSafetyMargin:)")));
- (float)lessonHorizontalShiftDefaultItem:(SharedLessonItem *)item innerWidth:(float)innerWidth __attribute__((swift_name("lessonHorizontalShiftDefault(item:innerWidth:)")));
@property (readonly) float LESSON_CARD_SIZE __attribute__((swift_name("LESSON_CARD_SIZE")));
@property (readonly) float LESSON_ITEM_PADDING __attribute__((swift_name("LESSON_ITEM_PADDING")));
@property (readonly) float LESSON_OFFSET_EDGE_MARGIN __attribute__((swift_name("LESSON_OFFSET_EDGE_MARGIN")));
@property (readonly) float MAX_LESSON_OFFSET __attribute__((swift_name("MAX_LESSON_OFFSET")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarathonGuideLogic")))
@interface SharedMarathonGuideLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)marathonGuideLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMarathonGuideLogic *shared __attribute__((swift_name("shared")));
- (NSString * _Nullable)eligibilityRejectReasonPartId:(int32_t)partId lessonIndex:(int32_t)lessonIndex item:(SharedLessonItem *)item __attribute__((swift_name("eligibilityRejectReason(partId:lessonIndex:item:)")));
- (SharedInt * _Nullable)firstChestLessonIndexForPartPartId:(int32_t)partId __attribute__((swift_name("firstChestLessonIndexForPart(partId:)")));
- (int32_t)firstMarathonLessonIndex __attribute__((swift_name("firstMarathonLessonIndex()")));
- (BOOL)shouldSchedulePartId:(int32_t)partId lessonIndex:(int32_t)lessonIndex item:(SharedLessonItem * _Nullable)item __attribute__((swift_name("shouldSchedule(partId:lessonIndex:item:)")));
@property (readonly) int32_t GUIDE_PART_ID __attribute__((swift_name("GUIDE_PART_ID")));
@property (readonly) NSString *KEY_PENDING __attribute__((swift_name("KEY_PENDING")));
@property (readonly) NSString *KEY_SHOWN __attribute__((swift_name("KEY_SHOWN")));
@property (readonly) NSString *LEGACY_SHOWN __attribute__((swift_name("LEGACY_SHOWN")));
@property (readonly) NSString *PREFS __attribute__((swift_name("PREFS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PartSelectionLogic")))
@interface SharedPartSelectionLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)partSelectionLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPartSelectionLogic *shared __attribute__((swift_name("shared")));
- (BOOL)hasProPlanPlan:(NSString *)plan teacherApproved:(BOOL)teacherApproved __attribute__((swift_name("hasProPlan(plan:teacherApproved:)")));
- (BOOL)lastChestCompleteItems:(NSArray<SharedLessonItem *> *)items __attribute__((swift_name("lastChestComplete(items:)")));
- (NSString *)marathonGateMessagePrerequisitePart:(int32_t)prerequisitePart __attribute__((swift_name("marathonGateMessage(prerequisitePart:)")));
- (BOOL)opensRacePanelPartId:(int32_t)partId __attribute__((swift_name("opensRacePanel(partId:)")));
- (NSString *)partAnimationNamePartId:(int32_t)partId __attribute__((swift_name("partAnimationName(partId:)")));
- (NSString *)partCardColorState:(SharedPartState *)state __attribute__((swift_name("partCardColor(state:)")));
- (NSString *)partLabelColorState:(SharedPartState *)state __attribute__((swift_name("partLabelColor(state:)")));
- (SharedPartState *)partStatePartId:(int32_t)partId hasProPlan:(BOOL)hasProPlan part1ChestComplete:(BOOL)part1ChestComplete part2ChestComplete:(BOOL)part2ChestComplete part3ChestComplete:(BOOL)part3ChestComplete teacherApproved:(BOOL)teacherApproved __attribute__((swift_name("partState(partId:hasProPlan:part1ChestComplete:part2ChestComplete:part3ChestComplete:teacherApproved:)")));
- (NSArray<SharedPartState *> *)partStatesPartIds:(NSArray<SharedInt *> *)partIds hasProPlan:(BOOL)hasProPlan part1ChestComplete:(BOOL)part1ChestComplete part2ChestComplete:(BOOL)part2ChestComplete part3ChestComplete:(BOOL)part3ChestComplete teacherApproved:(BOOL)teacherApproved __attribute__((swift_name("partStates(partIds:hasProPlan:part1ChestComplete:part2ChestComplete:part3ChestComplete:teacherApproved:)")));
- (NSString *)partTitlePartId:(int32_t)partId __attribute__((swift_name("partTitle(partId:)")));
- (NSString *)racePanelTitlePartId:(int32_t)partId __attribute__((swift_name("racePanelTitle(partId:)")));
@property (readonly) NSArray<SharedInt *> *ADDITION_PARTS __attribute__((swift_name("ADDITION_PARTS")));
@property (readonly) NSString *CATEGORY_TITLE_ADDITION __attribute__((swift_name("CATEGORY_TITLE_ADDITION")));
@property (readonly) NSString *CATEGORY_TITLE_MULTIPLICATION __attribute__((swift_name("CATEGORY_TITLE_MULTIPLICATION")));
@property (readonly) NSString *CATEGORY_TITLE_SUBTRACTION __attribute__((swift_name("CATEGORY_TITLE_SUBTRACTION")));
@property (readonly) NSArray<SharedInt *> *MULTIPLICATION_PARTS __attribute__((swift_name("MULTIPLICATION_PARTS")));
@property (readonly) NSArray<SharedInt *> *SUBTRACTION_PARTS __attribute__((swift_name("SUBTRACTION_PARTS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PartState")))
@interface SharedPartState : SharedBase
- (instancetype)initWithPartId:(int32_t)partId visualActive:(BOOL)visualActive functionalActive:(BOOL)functionalActive inactiveMessage:(NSString *)inactiveMessage __attribute__((swift_name("init(partId:visualActive:functionalActive:inactiveMessage:)"))) __attribute__((objc_designated_initializer));
- (SharedPartState *)doCopyPartId:(int32_t)partId visualActive:(BOOL)visualActive functionalActive:(BOOL)functionalActive inactiveMessage:(NSString *)inactiveMessage __attribute__((swift_name("doCopy(partId:visualActive:functionalActive:inactiveMessage:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL functionalActive __attribute__((swift_name("functionalActive")));
@property (readonly) NSString *inactiveMessage __attribute__((swift_name("inactiveMessage")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) BOOL visualActive __attribute__((swift_name("visualActive")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusDersAkisi")))
@interface SharedAbakusDersAkisi : SharedBase
- (BOOL)baslat __attribute__((swift_name("baslat()")));
- (SharedAbakusDersBitisi *)bitisWorstCupTime:(int32_t)worstCupTime __attribute__((swift_name("bitis(worstCupTime:)")));
- (BOOL)devam __attribute__((swift_name("devam()")));
- (NSString * _Nullable)ikinciSayiMetni __attribute__((swift_name("ikinciSayiMetni()")));
- (NSString * _Nullable)ilkSayiMetni __attribute__((swift_name("ilkSayiMetni()")));
- (NSString * _Nullable)islecMetni __attribute__((swift_name("islecMetni()")));
- (SharedAbakusKontrolSonucu *)kontrolNowMs:(int64_t)nowMs isOnline:(BOOL)isOnline abacusValue:(int32_t)abacusValue __attribute__((swift_name("kontrol(nowMs:isOnline:abacusValue:)")));
- (NSString *)sureAdimi __attribute__((swift_name("sureAdimi()")));
- (void)sureyiDurdur __attribute__((swift_name("sureyiDurdur()")));
@property (readonly) BOOL baslaDugmesiGerekli __attribute__((swift_name("baslaDugmesiGerekli")));
@property (readonly) BOOL basladi __attribute__((swift_name("basladi")));
@property (readonly) int32_t dogruSayisi __attribute__((swift_name("dogruSayisi")));
@property (readonly) int32_t soruSayisi __attribute__((swift_name("soruSayisi")));
@property (readonly) int32_t suankiIndeks __attribute__((swift_name("suankiIndeks")));
@property (readonly) BOOL sureCalisiyor __attribute__((swift_name("sureCalisiyor")));
@property (readonly) BOOL sureGorunur __attribute__((swift_name("sureGorunur")));
@property (readonly) NSString *sureMetni __attribute__((swift_name("sureMetni")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusDersBitisi")))
@interface SharedAbakusDersBitisi : SharedBase
- (instancetype)initWithYonlendirme:(int32_t)yonlendirme correctAnswers:(int32_t)correctAnswers totalQuestions:(int32_t)totalQuestions successRate:(float)successRate dersPuani:(int32_t)dersPuani time:(NSString *)time seconds:(int32_t)seconds worstCupTime:(int32_t)worstCupTime carpan:(float)carpan toplamPuan:(int32_t)toplamPuan __attribute__((swift_name("init(yonlendirme:correctAnswers:totalQuestions:successRate:dersPuani:time:seconds:worstCupTime:carpan:toplamPuan:)"))) __attribute__((objc_designated_initializer));
- (SharedAbakusDersBitisi *)doCopyYonlendirme:(int32_t)yonlendirme correctAnswers:(int32_t)correctAnswers totalQuestions:(int32_t)totalQuestions successRate:(float)successRate dersPuani:(int32_t)dersPuani time:(NSString *)time seconds:(int32_t)seconds worstCupTime:(int32_t)worstCupTime carpan:(float)carpan toplamPuan:(int32_t)toplamPuan __attribute__((swift_name("doCopy(yonlendirme:correctAnswers:totalQuestions:successRate:dersPuani:time:seconds:worstCupTime:carpan:toplamPuan:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float carpan __attribute__((swift_name("carpan")));
@property (readonly) int32_t correctAnswers __attribute__((swift_name("correctAnswers")));
@property (readonly) int32_t dersPuani __attribute__((swift_name("dersPuani")));
@property (readonly) int32_t seconds __attribute__((swift_name("seconds")));
@property (readonly) float successRate __attribute__((swift_name("successRate")));
@property (readonly) NSString *time __attribute__((swift_name("time")));
@property (readonly) int32_t toplamPuan __attribute__((swift_name("toplamPuan")));
@property (readonly) int32_t totalQuestions __attribute__((swift_name("totalQuestions")));
@property (readonly) int32_t worstCupTime __attribute__((swift_name("worstCupTime")));
@property (readonly) int32_t yonlendirme __attribute__((swift_name("yonlendirme")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusDersGirdisi")))
@interface SharedAbakusDersGirdisi : SharedBase
- (instancetype)initWithLessonType:(int32_t)lessonType isBlinding:(BOOL)isBlinding lessonOperationsId:(int32_t)lessonOperationsId lessonStep:(int32_t)lessonStep worstCupTime:(int32_t)worstCupTime lessonHint:(NSString *)lessonHint title:(NSString *)title __attribute__((swift_name("init(lessonType:isBlinding:lessonOperationsId:lessonStep:worstCupTime:lessonHint:title:)"))) __attribute__((objc_designated_initializer));
- (SharedAbakusDersGirdisi *)doCopyLessonType:(int32_t)lessonType isBlinding:(BOOL)isBlinding lessonOperationsId:(int32_t)lessonOperationsId lessonStep:(int32_t)lessonStep worstCupTime:(int32_t)worstCupTime lessonHint:(NSString *)lessonHint title:(NSString *)title __attribute__((swift_name("doCopy(lessonType:isBlinding:lessonOperationsId:lessonStep:worstCupTime:lessonHint:title:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) NSString *lessonHint __attribute__((swift_name("lessonHint")));
@property (readonly) int32_t lessonOperationsId __attribute__((swift_name("lessonOperationsId")));
@property (readonly) int32_t lessonStep __attribute__((swift_name("lessonStep")));
@property (readonly) int32_t lessonType __attribute__((swift_name("lessonType")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) int32_t worstCupTime __attribute__((swift_name("worstCupTime")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusDersKoprusu")))
@interface SharedAbakusDersKoprusu : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)abakusDersKoprusu __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbakusDersKoprusu *shared __attribute__((swift_name("shared")));
- (SharedAbakusDersAkisi * _Nullable)akisDersKimligindenLessonOperationsId:(int32_t)lessonOperationsId lessonType:(int32_t)lessonType __attribute__((swift_name("akisDersKimliginden(lessonOperationsId:lessonType:)")));
- (SharedAbakusDersAkisi * _Nullable)akisSorulardanSorular:(SharedAbakusSoruListesi *)sorular lessonType:(int32_t)lessonType __attribute__((swift_name("akisSorulardan(sorular:lessonType:)")));
- (BOOL)cevaplanabilirSoru:(SharedAbacusOperation *)soru __attribute__((swift_name("cevaplanabilir(soru:)")));
- (SharedAbakusDersGirdisi * _Nullable)dersGirdisiLessonItem:(SharedLessonItem *)lessonItem partId:(int32_t)partId mapIndex:(int32_t)mapIndex __attribute__((swift_name("dersGirdisi(lessonItem:partId:mapIndex:)")));
- (NSString *)ipucuMetniLessonHint:(NSString *)lessonHint __attribute__((swift_name("ipucuMetni(lessonHint:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusKontrolSonucu")))
@interface SharedAbakusKontrolSonucu : SharedBase
- (instancetype)initWithDurum:(int32_t)durum dogru:(BOOL)dogru sureDurdu:(BOOL)sureDurdu ilerlemeHedefi:(float)ilerlemeHedefi konfeti:(BOOL)konfeti cevapMetni:(NSString *)cevapMetni dogruCevapMetni:(NSString *)dogruCevapMetni __attribute__((swift_name("init(durum:dogru:sureDurdu:ilerlemeHedefi:konfeti:cevapMetni:dogruCevapMetni:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAbakusKontrolSonucuCompanion *companion __attribute__((swift_name("companion")));
- (SharedAbakusKontrolSonucu *)doCopyDurum:(int32_t)durum dogru:(BOOL)dogru sureDurdu:(BOOL)sureDurdu ilerlemeHedefi:(float)ilerlemeHedefi konfeti:(BOOL)konfeti cevapMetni:(NSString *)cevapMetni dogruCevapMetni:(NSString *)dogruCevapMetni __attribute__((swift_name("doCopy(durum:dogru:sureDurdu:ilerlemeHedefi:konfeti:cevapMetni:dogruCevapMetni:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *cevapMetni __attribute__((swift_name("cevapMetni")));
@property (readonly) BOOL dogru __attribute__((swift_name("dogru")));
@property (readonly) NSString *dogruCevapMetni __attribute__((swift_name("dogruCevapMetni")));
@property (readonly) int32_t durum __attribute__((swift_name("durum")));
@property (readonly) float ilerlemeHedefi __attribute__((swift_name("ilerlemeHedefi")));
@property (readonly) BOOL konfeti __attribute__((swift_name("konfeti")));
@property (readonly) BOOL sureDurdu __attribute__((swift_name("sureDurdu")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusKontrolSonucu.Companion")))
@interface SharedAbakusKontrolSonucuCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbakusKontrolSonucuCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t CEVAPLANDI __attribute__((swift_name("CEVAPLANDI")));
@property (readonly) int32_t CEVRIMDISI __attribute__((swift_name("CEVRIMDISI")));
@property (readonly) int32_t YOK_SAYILDI __attribute__((swift_name("YOK_SAYILDI")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbakusSoruListesi")))
@interface SharedAbakusSoruListesi : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)ekleIlkSayi:(int32_t)ilkSayi islec:(NSString *)islec ikinciSayi:(int32_t)ikinciSayi __attribute__((swift_name("ekle(ilkSayi:islec:ikinciSayi:)")));
- (void)ekleTekSayiSayi:(int32_t)sayi __attribute__((swift_name("ekleTekSayi(sayi:)")));
@property (readonly) int32_t adet __attribute__((swift_name("adet")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonOperations")))
@interface SharedLessonOperations : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonOperations __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonOperations *shared __attribute__((swift_name("shared")));
- (NSArray<SharedMathOperation_ *> *)getLessonOperationsLessonId:(int32_t)lessonId __attribute__((swift_name("getLessonOperations(lessonId:)")));
- (NSArray<id> *)getLessonOperationsBlindingLessonId:(int32_t)lessonId __attribute__((swift_name("getLessonOperationsBlinding(lessonId:)")));
- (int32_t)resolveLessonOperationsBlindingIdRequestedId:(int32_t)requestedId minLessonId:(int32_t)minLessonId __attribute__((swift_name("resolveLessonOperationsBlindingId(requestedId:minLessonId:)")));
- (int32_t)resolveLessonOperationsIdRequestedId:(int32_t)requestedId minLessonId:(int32_t)minLessonId __attribute__((swift_name("resolveLessonOperationsId(requestedId:minLessonId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UploadLessonDataLogic")))
@interface SharedUploadLessonDataLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)uploadLessonDataLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUploadLessonDataLogic *shared __attribute__((swift_name("shared")));
- (SharedUploadLessonDataLogicYuklenenDers<SharedMathOperation_ *> *)abacusUploadLessonDataBundleOperations:(NSArray<SharedMathOperation_ *> *)bundleOperations lessonItem:(SharedLessonItem *)lessonItem __attribute__((swift_name("abacusUploadLessonData(bundleOperations:lessonItem:)")));
- (SharedUploadLessonDataLogicYuklenenDers<id> *)blindingUploadLessonDataBundleOperations:(NSArray<id> *)bundleOperations lessonItem:(SharedLessonItem *)lessonItem globalPartId:(int32_t)globalPartId __attribute__((swift_name("blindingUploadLessonData(bundleOperations:lessonItem:globalPartId:)")));
- (int32_t)requestedLessonIdLessonItem:(SharedLessonItem *)lessonItem __attribute__((swift_name("requestedLessonId(lessonItem:)")));
- (NSArray<SharedMathOperation_ *> *)tutorialFinishedOperationsLessonStep:(int32_t)lessonStep __attribute__((swift_name("tutorialFinishedOperations(lessonStep:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UploadLessonDataLogicYuklenenDers")))
@interface SharedUploadLessonDataLogicYuklenenDers<T> : SharedBase
- (instancetype)initWithOperations:(NSArray<id> *)operations lessonStep:(SharedInt * _Nullable)lessonStep resolvedLessonId:(SharedInt * _Nullable)resolvedLessonId __attribute__((swift_name("init(operations:lessonStep:resolvedLessonId:)"))) __attribute__((objc_designated_initializer));
- (SharedUploadLessonDataLogicYuklenenDers<T> *)doCopyOperations:(NSArray<id> *)operations lessonStep:(SharedInt * _Nullable)lessonStep resolvedLessonId:(SharedInt * _Nullable)resolvedLessonId __attribute__((swift_name("doCopy(operations:lessonStep:resolvedLessonId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable lessonStep __attribute__((swift_name("lessonStep")));
@property (readonly) NSArray<id> *operations __attribute__((swift_name("operations")));
@property (readonly) SharedInt * _Nullable resolvedLessonId __attribute__((swift_name("resolvedLessonId")));
@end

__attribute__((swift_name("CupScoreRepositorySpec")))
@interface SharedCupScoreRepositorySpec : SharedBase
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer));
- (SharedCupUpdateResult *)computeUpdatedCupScoreCurrentFieldValue:(id _Nullable)currentFieldValue delta:(int32_t)delta __attribute__((swift_name("computeUpdatedCupScore(currentFieldValue:delta:)")));
- (int32_t)fetchFailureCupScore __attribute__((swift_name("fetchFailureCupScore()")));
- (NSString *)progressDocumentPathUid:(NSString *)uid __attribute__((swift_name("progressDocumentPath(uid:)")));
- (SharedCupFetchResult *)resolveFetchedCupScoreRawFieldValue:(id _Nullable)rawFieldValue __attribute__((swift_name("resolveFetchedCupScore(rawFieldValue:)")));
@property (readonly) NSString *COLLECTION __attribute__((swift_name("COLLECTION")));
@property (readonly) int32_t DEFAULT_CUP_SCORE __attribute__((swift_name("DEFAULT_CUP_SCORE")));
@property (readonly) NSString *DOCUMENT __attribute__((swift_name("DOCUMENT")));
@property (readonly) NSString *FIELD __attribute__((swift_name("FIELD")));
@property (readonly) NSString *SUBCOLLECTION __attribute__((swift_name("SUBCOLLECTION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AbacusCupRepository")))
@interface SharedAbacusCupRepository : SharedCupScoreRepositorySpec
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)abacusCupRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAbacusCupRepository *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t CUP_STEP __attribute__((swift_name("CUP_STEP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgePrecalcHelper")))
@interface SharedBadgePrecalcHelper : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgePrecalcHelper __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgePrecalcHelper *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)buildCupSubmitPayloadDelta:(int32_t)delta isMultiplication:(SharedBoolean * _Nullable)isMultiplication isExtraction:(SharedBoolean * _Nullable)isExtraction isBlinding:(SharedBoolean * _Nullable)isBlinding cupDifficultyLevel:(SharedInt * _Nullable)cupDifficultyLevel dayId:(NSString *)dayId __attribute__((swift_name("buildCupSubmitPayload(delta:isMultiplication:isExtraction:isBlinding:cupDifficultyLevel:dayId:)")));
- (SharedCupDeltaPrecalc *)precalculateDelta:(int32_t)delta currentLessonOldCupScore:(SharedInt * _Nullable)currentLessonOldCupScore isMultiplication:(SharedBoolean * _Nullable)isMultiplication isExtraction:(SharedBoolean * _Nullable)isExtraction isBlinding:(SharedBoolean * _Nullable)isBlinding __attribute__((swift_name("precalculate(delta:currentLessonOldCupScore:isMultiplication:isExtraction:isBlinding:)")));
- (SharedCupPathKind *)resolveCupPathKindIsMultiplication:(SharedBoolean * _Nullable)isMultiplication isExtraction:(SharedBoolean * _Nullable)isExtraction isBlinding:(SharedBoolean * _Nullable)isBlinding __attribute__((swift_name("resolveCupPathKind(isMultiplication:isExtraction:isBlinding:)")));
- (SharedInt * _Nullable)resolvePendingDeltaPendingCupDelta:(SharedInt * _Nullable)pendingCupDelta pendingBlindingCupDelta:(SharedInt * _Nullable)pendingBlindingCupDelta pendingExtractionCupDelta:(SharedInt * _Nullable)pendingExtractionCupDelta pendingBlindingExtractionCupDelta:(SharedInt * _Nullable)pendingBlindingExtractionCupDelta pendingImpactCupDelta:(SharedInt * _Nullable)pendingImpactCupDelta pendingBlindingImpactCupDelta:(SharedInt * _Nullable)pendingBlindingImpactCupDelta __attribute__((swift_name("resolvePendingDelta(pendingCupDelta:pendingBlindingCupDelta:pendingExtractionCupDelta:pendingBlindingExtractionCupDelta:pendingImpactCupDelta:pendingBlindingImpactCupDelta:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingAdditionCupRepository")))
@interface SharedBlindingAdditionCupRepository : SharedCupScoreRepositorySpec
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)blindingAdditionCupRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBlindingAdditionCupRepository *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t CUP_STEP __attribute__((swift_name("CUP_STEP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingExtractionCupRepository")))
@interface SharedBlindingExtractionCupRepository : SharedCupScoreRepositorySpec
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)blindingExtractionCupRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBlindingExtractionCupRepository *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t CUP_STEP __attribute__((swift_name("CUP_STEP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BlindingImpactCupRepository")))
@interface SharedBlindingImpactCupRepository : SharedCupScoreRepositorySpec
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)blindingImpactCupRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBlindingImpactCupRepository *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupDeltaPrecalc")))
@interface SharedCupDeltaPrecalc : SharedBase
- (instancetype)initWithKind:(SharedCupPathKind *)kind delta:(int32_t)delta oldScore:(int32_t)oldScore newScore:(int32_t)newScore payloadsLocally:(NSArray<SharedBadgeLevelUpPayload *> *)payloadsLocally __attribute__((swift_name("init(kind:delta:oldScore:newScore:payloadsLocally:)"))) __attribute__((objc_designated_initializer));
- (SharedCupDeltaPrecalc *)doCopyKind:(SharedCupPathKind *)kind delta:(int32_t)delta oldScore:(int32_t)oldScore newScore:(int32_t)newScore payloadsLocally:(NSArray<SharedBadgeLevelUpPayload *> *)payloadsLocally __attribute__((swift_name("doCopy(kind:delta:oldScore:newScore:payloadsLocally:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t delta __attribute__((swift_name("delta")));
@property (readonly) SharedCupPathKind *kind __attribute__((swift_name("kind")));
@property (readonly, getter=doNewScore) int32_t newScore __attribute__((swift_name("newScore")));
@property (readonly) int32_t oldScore __attribute__((swift_name("oldScore")));
@property (readonly) NSArray<SharedBadgeLevelUpPayload *> *payloadsLocally __attribute__((swift_name("payloadsLocally")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupFetchResult")))
@interface SharedCupFetchResult : SharedBase
- (instancetype)initWithScore:(int32_t)score shouldSeed:(BOOL)shouldSeed __attribute__((swift_name("init(score:shouldSeed:)"))) __attribute__((objc_designated_initializer));
- (SharedCupFetchResult *)doCopyScore:(int32_t)score shouldSeed:(BOOL)shouldSeed __attribute__((swift_name("doCopy(score:shouldSeed:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t score __attribute__((swift_name("score")));
@property (readonly) BOOL shouldSeed __attribute__((swift_name("shouldSeed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupFragmentLogic")))
@interface SharedCupFragmentLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupFragmentLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupFragmentLogic *shared __attribute__((swift_name("shared")));
- (BOOL)isLastStepStepCount:(int32_t)stepCount currentStep:(int32_t)currentStep __attribute__((swift_name("isLastStep(stepCount:currentStep:)")));
- (NSArray<SharedBoolean *> *)doNewStepCompletionStatusStepCount:(int32_t)stepCount currentStep:(int32_t)currentStep __attribute__((swift_name("doNewStepCompletionStatus(stepCount:currentStep:)")));
- (NSString *)pickMotivationTier:(SharedCupTier *)tier random:(SharedKotlinRandom *)random __attribute__((swift_name("pickMotivation(tier:random:)")));
- (SharedCupTier *)resolveCupTierToplamPuan:(int32_t)toplamPuan cupPoint1:(SharedInt * _Nullable)cupPoint1 cupPoint2:(SharedInt * _Nullable)cupPoint2 __attribute__((swift_name("resolveCupTier(toplamPuan:cupPoint1:cupPoint2:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupHistoryLogic")))
@interface SharedCupHistoryLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupHistoryLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupHistoryLogic *shared __attribute__((swift_name("shared")));
- (NSArray<SharedKotlinPair<NSString *, SharedInt *> *> *)bucketPoints:(NSArray<SharedKotlinPair<NSString *, SharedInt *> *> *)points tab:(SharedCupHistoryLogicRangeTab *)tab __attribute__((swift_name("bucket(points:tab:)")));
- (int32_t)currentScoreFromFieldRawFieldValue:(id _Nullable)rawFieldValue __attribute__((swift_name("currentScoreFromField(rawFieldValue:)")));
- (NSString *)daysAgoDays:(int32_t)days nowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("daysAgo(days:nowMs:timeZone:)")));
- (NSString *)formatLabelTab:(SharedCupHistoryLogicRangeTab *)tab dateStr:(NSString *)dateStr __attribute__((swift_name("formatLabel(tab:dateStr:)")));
- (NSArray<SharedKotlinPair<NSString *, SharedInt *> *> *)groupLastPerKeyPoints:(NSArray<SharedKotlinPair<NSString *, SharedInt *> *> *)points keyFn:(NSString *(^)(NSString *))keyFn __attribute__((swift_name("groupLastPerKey(points:keyFn:)")));
- (BOOL)isSinglePointBucketed:(NSArray<SharedKotlinPair<NSString *, SharedInt *> *> *)bucketed __attribute__((swift_name("isSinglePoint(bucketed:)")));
- (SharedKotlinPair<SharedInt *, SharedInt *> *)niceAxisRangeValues:(NSArray<SharedInt *> *)values __attribute__((swift_name("niceAxisRange(values:)")));
- (NSString * _Nullable)sinceDateIdForTabTab:(SharedCupHistoryLogicRangeTab *)tab nowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("sinceDateIdForTab(tab:nowMs:timeZone:)")));
- (SharedInt * _Nullable)windowDaysForTabTab:(SharedCupHistoryLogicRangeTab *)tab __attribute__((swift_name("windowDaysForTab(tab:)")));
- (int32_t)xAxisLabelCountLabelCount:(int32_t)labelCount __attribute__((swift_name("xAxisLabelCount(labelCount:)")));
@property (readonly) int32_t DEFAULT_CURRENT_SCORE __attribute__((swift_name("DEFAULT_CURRENT_SCORE")));
@property (readonly) NSArray<NSString *> *TAB_LABELS __attribute__((swift_name("TAB_LABELS")));
@property (readonly) NSArray<NSString *> *TR_SHORT_MONTHS __attribute__((swift_name("TR_SHORT_MONTHS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupHistoryLogic.RangeTab")))
@interface SharedCupHistoryLogicRangeTab : SharedKotlinEnum<SharedCupHistoryLogicRangeTab *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCupHistoryLogicRangeTab *weekly __attribute__((swift_name("weekly")));
@property (class, readonly) SharedCupHistoryLogicRangeTab *monthly __attribute__((swift_name("monthly")));
@property (class, readonly) SharedCupHistoryLogicRangeTab *yearly __attribute__((swift_name("yearly")));
@property (class, readonly) SharedCupHistoryLogicRangeTab *all __attribute__((swift_name("all")));
+ (SharedKotlinArray<SharedCupHistoryLogicRangeTab *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCupHistoryLogicRangeTab *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupHistoryRepository")))
@interface SharedCupHistoryRepository : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupHistoryRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupHistoryRepository *shared __attribute__((swift_name("shared")));
- (NSString *)historyCollectionPathUid:(NSString *)uid __attribute__((swift_name("historyCollectionPath(uid:)")));
- (NSString *)historyDocumentPathUid:(NSString *)uid dateId:(NSString *)dateId __attribute__((swift_name("historyDocumentPath(uid:dateId:)")));
- (NSArray<SharedKotlinPair<NSString *, SharedInt *> *> *)mapSnapshotDocuments:(NSArray<SharedKotlinPair<NSString *, NSDictionary<NSString *, id> *> *> *)documents field:(NSString *)field __attribute__((swift_name("mapSnapshot(documents:field:)")));
- (NSDictionary<NSString *, id> *)recordSnapshotField:(NSString *)field value:(int32_t)value __attribute__((swift_name("recordSnapshot(field:value:)")));
- (NSString *)todayIdNowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("todayId(nowMs:timeZone:)")));
@property (readonly) NSString *COLLECTION __attribute__((swift_name("COLLECTION")));
@property (readonly) NSString *SUBCOLLECTION __attribute__((swift_name("SUBCOLLECTION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupLessonLaunch")))
@interface SharedCupLessonLaunch : SharedBase
- (instancetype)initWithCardNumber:(int32_t)cardNumber cupScore:(int32_t)cupScore timePeriod:(int64_t)timePeriod cupDigitSize:(int32_t)cupDigitSize cupNumberCount:(int32_t)cupNumberCount cupDifficultyLevel:(int32_t)cupDifficultyLevel cupWinDelta:(int32_t)cupWinDelta cupLossDelta:(int32_t)cupLossDelta isBlinding:(BOOL)isBlinding isExtraction:(BOOL)isExtraction isMultiplication:(BOOL)isMultiplication numbers:(NSArray<SharedInt *> *)numbers multiplicationFirst:(int32_t)multiplicationFirst multiplicationSecond:(int32_t)multiplicationSecond __attribute__((swift_name("init(cardNumber:cupScore:timePeriod:cupDigitSize:cupNumberCount:cupDifficultyLevel:cupWinDelta:cupLossDelta:isBlinding:isExtraction:isMultiplication:numbers:multiplicationFirst:multiplicationSecond:)"))) __attribute__((objc_designated_initializer));
- (SharedCupLessonLaunch *)doCopyCardNumber:(int32_t)cardNumber cupScore:(int32_t)cupScore timePeriod:(int64_t)timePeriod cupDigitSize:(int32_t)cupDigitSize cupNumberCount:(int32_t)cupNumberCount cupDifficultyLevel:(int32_t)cupDifficultyLevel cupWinDelta:(int32_t)cupWinDelta cupLossDelta:(int32_t)cupLossDelta isBlinding:(BOOL)isBlinding isExtraction:(BOOL)isExtraction isMultiplication:(BOOL)isMultiplication numbers:(NSArray<SharedInt *> *)numbers multiplicationFirst:(int32_t)multiplicationFirst multiplicationSecond:(int32_t)multiplicationSecond __attribute__((swift_name("doCopy(cardNumber:cupScore:timePeriod:cupDigitSize:cupNumberCount:cupDifficultyLevel:cupWinDelta:cupLossDelta:isBlinding:isExtraction:isMultiplication:numbers:multiplicationFirst:multiplicationSecond:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t cardNumber __attribute__((swift_name("cardNumber")));
@property (readonly) int32_t cupDifficultyLevel __attribute__((swift_name("cupDifficultyLevel")));
@property (readonly) int32_t cupDigitSize __attribute__((swift_name("cupDigitSize")));
@property (readonly) int32_t cupLossDelta __attribute__((swift_name("cupLossDelta")));
@property (readonly) int32_t cupNumberCount __attribute__((swift_name("cupNumberCount")));
@property (readonly) int32_t cupScore __attribute__((swift_name("cupScore")));
@property (readonly) int32_t cupWinDelta __attribute__((swift_name("cupWinDelta")));
@property (readonly) BOOL isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) BOOL isExtraction __attribute__((swift_name("isExtraction")));
@property (readonly) BOOL isMultiplication __attribute__((swift_name("isMultiplication")));
@property (readonly) int32_t multiplicationFirst __attribute__((swift_name("multiplicationFirst")));
@property (readonly) int32_t multiplicationSecond __attribute__((swift_name("multiplicationSecond")));
@property (readonly) NSArray<SharedInt *> *numbers __attribute__((swift_name("numbers")));
@property (readonly) int64_t timePeriod __attribute__((swift_name("timePeriod")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupMathOperation")))
@interface SharedCupMathOperation : SharedBase
- (instancetype)initWithFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("init(firstNumber:operator:secondNumber:)"))) __attribute__((objc_designated_initializer));
- (SharedCupMathOperation *)doCopyFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("doCopy(firstNumber:operator:secondNumber:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable firstNumber __attribute__((swift_name("firstNumber")));
@property (readonly, getter=operator) NSString * _Nullable operator_ __attribute__((swift_name("operator_")));
@property (readonly) SharedInt * _Nullable secondNumber __attribute__((swift_name("secondNumber")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathCard")))
@interface SharedCupPathCard : SharedBase
- (instancetype)initWithCardNumber:(int32_t)cardNumber title:(NSString *)title profileTitle:(NSString *)profileTitle showForbiddenIcon:(BOOL)showForbiddenIcon animFile:(NSString *)animFile activeColorRes:(NSString *)activeColorRes field:(NSString *)field unlockPartId:(int32_t)unlockPartId isBlinding:(BOOL)isBlinding isExtraction:(BOOL)isExtraction isMultiplication:(BOOL)isMultiplication kind:(SharedCupPathKind *)kind __attribute__((swift_name("init(cardNumber:title:profileTitle:showForbiddenIcon:animFile:activeColorRes:field:unlockPartId:isBlinding:isExtraction:isMultiplication:kind:)"))) __attribute__((objc_designated_initializer));
- (SharedCupPathCard *)doCopyCardNumber:(int32_t)cardNumber title:(NSString *)title profileTitle:(NSString *)profileTitle showForbiddenIcon:(BOOL)showForbiddenIcon animFile:(NSString *)animFile activeColorRes:(NSString *)activeColorRes field:(NSString *)field unlockPartId:(int32_t)unlockPartId isBlinding:(BOOL)isBlinding isExtraction:(BOOL)isExtraction isMultiplication:(BOOL)isMultiplication kind:(SharedCupPathKind *)kind __attribute__((swift_name("doCopy(cardNumber:title:profileTitle:showForbiddenIcon:animFile:activeColorRes:field:unlockPartId:isBlinding:isExtraction:isMultiplication:kind:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *activeColorRes __attribute__((swift_name("activeColorRes")));
@property (readonly) NSString *animFile __attribute__((swift_name("animFile")));
@property (readonly) int32_t cardNumber __attribute__((swift_name("cardNumber")));
@property (readonly) NSString *field __attribute__((swift_name("field")));
@property (readonly) BOOL isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) BOOL isExtraction __attribute__((swift_name("isExtraction")));
@property (readonly) BOOL isMultiplication __attribute__((swift_name("isMultiplication")));
@property (readonly) SharedCupPathKind *kind __attribute__((swift_name("kind")));
@property (readonly) NSString *profileTitle __attribute__((swift_name("profileTitle")));
@property (readonly) BOOL showForbiddenIcon __attribute__((swift_name("showForbiddenIcon")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) int32_t unlockPartId __attribute__((swift_name("unlockPartId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathKind")))
@interface SharedCupPathKind : SharedKotlinEnum<SharedCupPathKind *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCupPathKind *blindingImpact __attribute__((swift_name("blindingImpact")));
@property (class, readonly) SharedCupPathKind *impact __attribute__((swift_name("impact")));
@property (class, readonly) SharedCupPathKind *blindingExtraction __attribute__((swift_name("blindingExtraction")));
@property (class, readonly) SharedCupPathKind *extraction __attribute__((swift_name("extraction")));
@property (class, readonly) SharedCupPathKind *blindingAddition __attribute__((swift_name("blindingAddition")));
@property (class, readonly) SharedCupPathKind *addition __attribute__((swift_name("addition")));
+ (SharedKotlinArray<SharedCupPathKind *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCupPathKind *> *entries __attribute__((swift_name("entries")));
@property (readonly) SharedBadgeAnimMode *badgeMode __attribute__((swift_name("badgeMode")));
@property (readonly) SharedCupScoreRepositorySpec *repository __attribute__((swift_name("repository")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathLogic")))
@interface SharedCupPathLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupPathLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupPathLogic *shared __attribute__((swift_name("shared")));
- (NSArray<SharedInt *> *)applyExtractionNegationNumbers:(NSArray<SharedInt *> *)numbers __attribute__((swift_name("applyExtractionNegation(numbers:)")));
- (SharedCupLessonLaunch *)buildCupLessonCardNumber:(int32_t)cardNumber cupScore:(int32_t)cupScore difficultyLevel:(int32_t)difficultyLevel forcedDigitSize:(int32_t)forcedDigitSize __attribute__((swift_name("buildCupLesson(cardNumber:cupScore:difficultyLevel:forcedDigitSize:)")));
- (BOOL)canStartLessonIsInfiniteEnergy:(BOOL)isInfiniteEnergy currentEnergy:(int32_t)currentEnergy __attribute__((swift_name("canStartLesson(isInfiniteEnergy:currentEnergy:)")));
- (SharedCupPathCard *)cardCardNumber:(int32_t)cardNumber __attribute__((swift_name("card(cardNumber:)")));
- (int32_t)deltaColorRgbDelta:(int32_t)delta __attribute__((swift_name("deltaColorRgb(delta:)")));
- (NSString *)deltaTextDelta:(int32_t)delta __attribute__((swift_name("deltaText(delta:)")));
- (int32_t)difficultyPercentProgress:(int32_t)progress __attribute__((swift_name("difficultyPercent(progress:)")));
- (NSString *)difficultyTitleIsMultiplication:(BOOL)isMultiplication __attribute__((swift_name("difficultyTitle(isMultiplication:)")));
- (NSString *)digitOptionLabelDigits:(int32_t)digits __attribute__((swift_name("digitOptionLabel(digits:)")));
- (NSArray<SharedInt *> *)digitOptionsCupScore:(int32_t)cupScore isBlinding:(BOOL)isBlinding isMultiplication:(BOOL)isMultiplication __attribute__((swift_name("digitOptions(cupScore:isBlinding:isMultiplication:)")));
- (BOOL)isCardActiveLessonItemTypes:(NSArray<SharedInt *> *)lessonItemTypes stepIsFinish:(NSArray<SharedBoolean *> *)stepIsFinish __attribute__((swift_name("isCardActive(lessonItemTypes:stepIsFinish:)")));
- (BOOL)isCardActiveLessonItemTypes:(NSArray<SharedInt *> *)lessonItemTypes stepIsFinish:(NSArray<SharedBoolean *> *)stepIsFinish isApprovedTeacher:(BOOL)isApprovedTeacher __attribute__((swift_name("isCardActive(lessonItemTypes:stepIsFinish:isApprovedTeacher:)")));
- (NSString *)lockedCardHintCardNumber:(int32_t)cardNumber __attribute__((swift_name("lockedCardHint(cardNumber:)")));
- (int32_t)scoreAfterLessonOldScore:(int32_t)oldScore delta:(int32_t)delta __attribute__((swift_name("scoreAfterLesson(oldScore:delta:)")));
- (int32_t)scoreAnimationEndUpdatedScore:(int32_t)updatedScore __attribute__((swift_name("scoreAnimationEnd(updatedScore:)")));
- (int32_t)scoreAnimationStartUpdatedScore:(int32_t)updatedScore delta:(int32_t)delta __attribute__((swift_name("scoreAnimationStart(updatedScore:delta:)")));
- (BOOL)shouldAnimateScoreDelta:(int32_t)delta __attribute__((swift_name("shouldAnimateScore(delta:)")));
- (int32_t)shownScoreScore:(int32_t)score __attribute__((swift_name("shownScore(score:)")));
- (BOOL)showsDifficultySliderIsMultiplication:(BOOL)isMultiplication __attribute__((swift_name("showsDifficultySlider(isMultiplication:)")));
@property (readonly) NSArray<SharedCupPathCard *> *CARDS __attribute__((swift_name("CARDS")));
@property (readonly) int64_t CARD_CLICK_DEBOUNCE_MS __attribute__((swift_name("CARD_CLICK_DEBOUNCE_MS")));
@property (readonly) NSArray<NSString *> *CARD_UNLOCK_REQUIREMENTS __attribute__((swift_name("CARD_UNLOCK_REQUIREMENTS")));
@property (readonly) NSString *DIFFICULTY_MESSAGE __attribute__((swift_name("DIFFICULTY_MESSAGE")));
@property (readonly) int32_t DIFFICULTY_SEEKBAR_MAX __attribute__((swift_name("DIFFICULTY_SEEKBAR_MAX")));
@property (readonly) NSString *DIGIT_LABEL __attribute__((swift_name("DIGIT_LABEL")));
@property (readonly) NSString *DIGIT_RANDOM_LABEL __attribute__((swift_name("DIGIT_RANDOM_LABEL")));
@property (readonly) int32_t LESSON_ITEM_TYPE_CHEST __attribute__((swift_name("LESSON_ITEM_TYPE_CHEST")));
@property (readonly) NSString *LOCKED_CARD_COLOR_RES __attribute__((swift_name("LOCKED_CARD_COLOR_RES")));
@property (readonly) NSString *LOCKED_CONTENT_COLOR_RES __attribute__((swift_name("LOCKED_CONTENT_COLOR_RES")));
@property (readonly) int32_t MULTIPLICATION_LOSS_DELTA __attribute__((swift_name("MULTIPLICATION_LOSS_DELTA")));
@property (readonly) int32_t MULTIPLICATION_NUMBER_COUNT __attribute__((swift_name("MULTIPLICATION_NUMBER_COUNT")));
@property (readonly) int32_t MULTIPLICATION_WIN_DELTA __attribute__((swift_name("MULTIPLICATION_WIN_DELTA")));
@property (readonly) int64_t PANEL_REOPEN_GUARD_MS __attribute__((swift_name("PANEL_REOPEN_GUARD_MS")));
@property (readonly) NSString *PANEL_TITLE __attribute__((swift_name("PANEL_TITLE")));
@property (readonly) int64_t SCORE_ANIMATION_MS __attribute__((swift_name("SCORE_ANIMATION_MS")));
@property (readonly) NSString *START_LABEL __attribute__((swift_name("START_LABEL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathRewardRepository")))
@interface SharedCupPathRewardRepository : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupPathRewardRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupPathRewardRepository *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, SharedCupPathRewardRepositoryCupPathState *> *)buildStatesCupDoc:(NSDictionary<NSString *, id> * _Nullable)cupDoc ledgerDoc:(NSDictionary<NSString *, id> * _Nullable)ledgerDoc __attribute__((swift_name("buildStates(cupDoc:ledgerDoc:)")));
- (SharedCupPathRewardRepositoryCupPathState *)defaultStateCupField:(NSString *)cupField __attribute__((swift_name("defaultState(cupField:)")));
- (NSString *)ledgerDocumentPathUid:(NSString *)uid __attribute__((swift_name("ledgerDocumentPath(uid:)")));
- (NSString *)lockedMilestoneTextCupValue:(int32_t)cupValue __attribute__((swift_name("lockedMilestoneText(cupValue:)")));
- (SharedChestRarity *)rarityOfMilestone:(int32_t)milestone __attribute__((swift_name("rarityOf(milestone:)")));
- (NSString *)titleOfCupField:(NSString *)cupField __attribute__((swift_name("titleOf(cupField:)")));
@property (readonly) NSString *COL_CUP __attribute__((swift_name("COL_CUP")));
@property (readonly) NSString *COL_LEDGER __attribute__((swift_name("COL_LEDGER")));
@property (readonly) NSString *COL_USERS __attribute__((swift_name("COL_USERS")));
@property (readonly) NSString *DOC_CUP __attribute__((swift_name("DOC_CUP")));
@property (readonly) NSString *DOC_LEDGER __attribute__((swift_name("DOC_LEDGER")));
@property (readonly) NSArray<NSString *> *FIELDS __attribute__((swift_name("FIELDS")));
@property (readonly) NSString *FIELD_ADDITION __attribute__((swift_name("FIELD_ADDITION")));
@property (readonly) NSString *FIELD_BLINDING_ADDITION __attribute__((swift_name("FIELD_BLINDING_ADDITION")));
@property (readonly) NSString *FIELD_BLINDING_EXTRACTION __attribute__((swift_name("FIELD_BLINDING_EXTRACTION")));
@property (readonly) NSString *FIELD_BLINDING_IMPACT __attribute__((swift_name("FIELD_BLINDING_IMPACT")));
@property (readonly) NSString *FIELD_EXTRACTION __attribute__((swift_name("FIELD_EXTRACTION")));
@property (readonly) NSString *FIELD_IMPACT __attribute__((swift_name("FIELD_IMPACT")));
@property (readonly) int32_t FUTURE_MILESTONES __attribute__((swift_name("FUTURE_MILESTONES")));
@property (readonly) NSString *F_CLAIMED __attribute__((swift_name("F_CLAIMED")));
@property (readonly) NSString *F_LAST_CLAIMED __attribute__((swift_name("F_LAST_CLAIMED")));
@property (readonly) int32_t MAX __attribute__((swift_name("MAX")));
@property (readonly) int32_t MAX_MILESTONES __attribute__((swift_name("MAX_MILESTONES")));
@property (readonly) int32_t START __attribute__((swift_name("START")));
@property (readonly) int32_t STEP __attribute__((swift_name("STEP")));
@property (readonly) NSString *TEXT_ALREADY_CLAIMED __attribute__((swift_name("TEXT_ALREADY_CLAIMED")));
@property (readonly) NSString *TEXT_CHEST_READY __attribute__((swift_name("TEXT_CHEST_READY")));
@property (readonly) NSString *TEXT_CLAIMED_CAPS __attribute__((swift_name("TEXT_CLAIMED_CAPS")));
@property (readonly) NSString *TEXT_FINISHED __attribute__((swift_name("TEXT_FINISHED")));
@property (readonly) NSString *TEXT_READY_CAPS __attribute__((swift_name("TEXT_READY_CAPS")));
@property (readonly) NSString *TEXT_TITLE_FALLBACK __attribute__((swift_name("TEXT_TITLE_FALLBACK")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathRewardRepository.CupPathState")))
@interface SharedCupPathRewardRepositoryCupPathState : SharedBase
- (instancetype)initWithCupField:(NSString *)cupField cupScore:(int32_t)cupScore lastClaimed:(int32_t)lastClaimed claimedAbove:(NSSet<SharedInt *> *)claimedAbove __attribute__((swift_name("init(cupField:cupScore:lastClaimed:claimedAbove:)"))) __attribute__((objc_designated_initializer));
- (SharedCupPathRewardRepositoryCupPathState *)doCopyCupField:(NSString *)cupField cupScore:(int32_t)cupScore lastClaimed:(int32_t)lastClaimed claimedAbove:(NSSet<SharedInt *> *)claimedAbove __attribute__((swift_name("doCopy(cupField:cupScore:lastClaimed:claimedAbove:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isClaimedMilestone:(int32_t)milestone __attribute__((swift_name("isClaimed(milestone:)")));
- (NSArray<SharedCupPathRewardRepositoryMilestone *> *)milestonesFuture:(int32_t)future __attribute__((swift_name("milestones(future:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL claimable __attribute__((swift_name("claimable")));
@property (readonly) NSSet<SharedInt *> *claimedAbove __attribute__((swift_name("claimedAbove")));
@property (readonly) NSString *cupField __attribute__((swift_name("cupField")));
@property (readonly) int32_t cupScore __attribute__((swift_name("cupScore")));
@property (readonly) BOOL finished __attribute__((swift_name("finished")));
@property (readonly) float fraction __attribute__((swift_name("fraction")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t lastClaimed __attribute__((swift_name("lastClaimed")));
@property (readonly) int32_t milestoneStart __attribute__((swift_name("milestoneStart")));
@property (readonly) int32_t nextMilestone __attribute__((swift_name("nextMilestone")));
@property (readonly) int32_t pendingChests __attribute__((swift_name("pendingChests")));
@property (readonly) BOOL tapOpensChest __attribute__((swift_name("tapOpensChest")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathRewardRepository.Milestone")))
@interface SharedCupPathRewardRepositoryMilestone : SharedBase
- (instancetype)initWithCupValue:(int32_t)cupValue status:(SharedCupPathRewardRepositoryMilestoneStatus *)status __attribute__((swift_name("init(cupValue:status:)"))) __attribute__((objc_designated_initializer));
- (SharedCupPathRewardRepositoryMilestone *)doCopyCupValue:(int32_t)cupValue status:(SharedCupPathRewardRepositoryMilestoneStatus *)status __attribute__((swift_name("doCopy(cupValue:status:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t cupValue __attribute__((swift_name("cupValue")));
@property (readonly) SharedCupPathRewardRepositoryMilestoneStatus *status __attribute__((swift_name("status")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPathRewardRepository.MilestoneStatus")))
@interface SharedCupPathRewardRepositoryMilestoneStatus : SharedKotlinEnum<SharedCupPathRewardRepositoryMilestoneStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCupPathRewardRepositoryMilestoneStatus *claimed __attribute__((swift_name("claimed")));
@property (class, readonly) SharedCupPathRewardRepositoryMilestoneStatus *claimable __attribute__((swift_name("claimable")));
@property (class, readonly) SharedCupPathRewardRepositoryMilestoneStatus *locked __attribute__((swift_name("locked")));
+ (SharedKotlinArray<SharedCupPathRewardRepositoryMilestoneStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCupPathRewardRepositoryMilestoneStatus *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupRuleEngine")))
@interface SharedCupRuleEngine : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupRuleEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupRuleEngine *shared __attribute__((swift_name("shared")));
- (SharedCupRuleEngineCupLessonItemOverride *)buildLessonItemCupScore:(int32_t)cupScore difficultyLevel:(int32_t)difficultyLevel isBlinding:(BOOL)isBlinding isExtraction:(BOOL)isExtraction forcedDigitSize:(SharedInt * _Nullable)forcedDigitSize random:(SharedKotlinRandom *)random __attribute__((swift_name("buildLessonItem(cupScore:difficultyLevel:isBlinding:isExtraction:forcedDigitSize:random:)")));
- (NSArray<SharedCupRuleEngineDigitConfig *> *)configsForCupScore:(int32_t)cupScore isBlinding:(BOOL)isBlinding __attribute__((swift_name("configsFor(cupScore:isBlinding:)")));
- (SharedCupMathOperation *)generateMultiplicationQuestionCupScore:(int32_t)cupScore isBlinding:(BOOL)isBlinding random:(SharedKotlinRandom *)random __attribute__((swift_name("generateMultiplicationQuestion(cupScore:isBlinding:random:)")));
- (NSArray<SharedInt *> *)getAvailableDigitsCupScore:(int32_t)cupScore isBlinding:(BOOL)isBlinding __attribute__((swift_name("getAvailableDigits(cupScore:isBlinding:)")));
- (SharedKotlinPair<SharedInt *, SharedInt *> *)multiplicationDigitsForCupScore:(int32_t)cupScore isBlinding:(BOOL)isBlinding __attribute__((swift_name("multiplicationDigitsFor(cupScore:isBlinding:)")));
- (SharedCupRuleEngineDigitConfig *)resolveCupScore:(int32_t)cupScore ranges:(NSArray<SharedCupRuleEngineCupRange *> *)ranges forcedDigitSize:(SharedInt * _Nullable)forcedDigitSize random:(SharedKotlinRandom *)random __attribute__((swift_name("resolve(cupScore:ranges:forcedDigitSize:random:)")));
- (NSArray<SharedCupRuleEngineDigitConfig *> *)resolveConfigsCupScore:(int32_t)cupScore ranges:(NSArray<SharedCupRuleEngineCupRange *> *)ranges __attribute__((swift_name("resolveConfigs(cupScore:ranges:)")));
- (SharedKotlinPair<SharedInt *, SharedLong *> *)selectValuesConfig:(SharedCupRuleEngineDigitConfig *)config difficultyLevel:(int32_t)difficultyLevel random:(SharedKotlinRandom *)random __attribute__((swift_name("selectValues(config:difficultyLevel:random:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupRuleEngine.BlindingMultiplicationConfig")))
@interface SharedCupRuleEngineBlindingMultiplicationConfig : SharedBase
- (instancetype)initWithMinCup:(int32_t)minCup maxCup:(int32_t)maxCup firstDigitCount:(int32_t)firstDigitCount firstAllowedDigits:(NSArray<SharedInt *> *)firstAllowedDigits secondDigitCount:(int32_t)secondDigitCount secondAllowedDigits:(NSArray<SharedInt *> *)secondAllowedDigits __attribute__((swift_name("init(minCup:maxCup:firstDigitCount:firstAllowedDigits:secondDigitCount:secondAllowedDigits:)"))) __attribute__((objc_designated_initializer));
- (SharedCupRuleEngineBlindingMultiplicationConfig *)doCopyMinCup:(int32_t)minCup maxCup:(int32_t)maxCup firstDigitCount:(int32_t)firstDigitCount firstAllowedDigits:(NSArray<SharedInt *> *)firstAllowedDigits secondDigitCount:(int32_t)secondDigitCount secondAllowedDigits:(NSArray<SharedInt *> *)secondAllowedDigits __attribute__((swift_name("doCopy(minCup:maxCup:firstDigitCount:firstAllowedDigits:secondDigitCount:secondAllowedDigits:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedInt *> *firstAllowedDigits __attribute__((swift_name("firstAllowedDigits")));
@property (readonly) int32_t firstDigitCount __attribute__((swift_name("firstDigitCount")));
@property (readonly) int32_t maxCup __attribute__((swift_name("maxCup")));
@property (readonly) int32_t minCup __attribute__((swift_name("minCup")));
@property (readonly) NSArray<SharedInt *> *secondAllowedDigits __attribute__((swift_name("secondAllowedDigits")));
@property (readonly) int32_t secondDigitCount __attribute__((swift_name("secondDigitCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupRuleEngine.CupLessonItemOverride")))
@interface SharedCupRuleEngineCupLessonItemOverride : SharedBase
- (instancetype)initWithTimePeriod:(int64_t)timePeriod cupDigitSize:(int32_t)cupDigitSize cupNumberCount:(int32_t)cupNumberCount cupDifficultyLevel:(int32_t)cupDifficultyLevel cupWinDelta:(int32_t)cupWinDelta cupLossDelta:(int32_t)cupLossDelta isBlinding:(SharedBoolean * _Nullable)isBlinding isExtraction:(SharedBoolean * _Nullable)isExtraction __attribute__((swift_name("init(timePeriod:cupDigitSize:cupNumberCount:cupDifficultyLevel:cupWinDelta:cupLossDelta:isBlinding:isExtraction:)"))) __attribute__((objc_designated_initializer));
- (SharedCupRuleEngineCupLessonItemOverride *)doCopyTimePeriod:(int64_t)timePeriod cupDigitSize:(int32_t)cupDigitSize cupNumberCount:(int32_t)cupNumberCount cupDifficultyLevel:(int32_t)cupDifficultyLevel cupWinDelta:(int32_t)cupWinDelta cupLossDelta:(int32_t)cupLossDelta isBlinding:(SharedBoolean * _Nullable)isBlinding isExtraction:(SharedBoolean * _Nullable)isExtraction __attribute__((swift_name("doCopy(timePeriod:cupDigitSize:cupNumberCount:cupDifficultyLevel:cupWinDelta:cupLossDelta:isBlinding:isExtraction:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t cupDifficultyLevel __attribute__((swift_name("cupDifficultyLevel")));
@property (readonly) int32_t cupDigitSize __attribute__((swift_name("cupDigitSize")));
@property (readonly) int32_t cupLossDelta __attribute__((swift_name("cupLossDelta")));
@property (readonly) int32_t cupNumberCount __attribute__((swift_name("cupNumberCount")));
@property (readonly) int32_t cupWinDelta __attribute__((swift_name("cupWinDelta")));
@property (readonly) SharedBoolean * _Nullable isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) SharedBoolean * _Nullable isExtraction __attribute__((swift_name("isExtraction")));
@property (readonly) int64_t timePeriod __attribute__((swift_name("timePeriod")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupRuleEngine.CupRange")))
@interface SharedCupRuleEngineCupRange : SharedBase
- (instancetype)initWithMinCup:(int32_t)minCup maxCup:(int32_t)maxCup configs:(NSArray<SharedCupRuleEngineDigitConfig *> *)configs __attribute__((swift_name("init(minCup:maxCup:configs:)"))) __attribute__((objc_designated_initializer));
- (SharedCupRuleEngineCupRange *)doCopyMinCup:(int32_t)minCup maxCup:(int32_t)maxCup configs:(NSArray<SharedCupRuleEngineDigitConfig *> *)configs __attribute__((swift_name("doCopy(minCup:maxCup:configs:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedCupRuleEngineDigitConfig *> *configs __attribute__((swift_name("configs")));
@property (readonly) int32_t maxCup __attribute__((swift_name("maxCup")));
@property (readonly) int32_t minCup __attribute__((swift_name("minCup")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupRuleEngine.DigitConfig")))
@interface SharedCupRuleEngineDigitConfig : SharedBase
- (instancetype)initWithDigits:(int32_t)digits counts:(NSArray<SharedInt *> *)counts intervals:(NSArray<SharedLong *> *)intervals __attribute__((swift_name("init(digits:counts:intervals:)"))) __attribute__((objc_designated_initializer));
- (SharedCupRuleEngineDigitConfig *)doCopyDigits:(int32_t)digits counts:(NSArray<SharedInt *> *)counts intervals:(NSArray<SharedLong *> *)intervals __attribute__((swift_name("doCopy(digits:counts:intervals:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedInt *> *counts __attribute__((swift_name("counts")));
@property (readonly) int32_t digits __attribute__((swift_name("digits")));
@property (readonly) NSArray<SharedLong *> *intervals __attribute__((swift_name("intervals")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupRuleEngine.MultiplicationConfig")))
@interface SharedCupRuleEngineMultiplicationConfig : SharedBase
- (instancetype)initWithMinCup:(int32_t)minCup maxCup:(int32_t)maxCup firstDigitCount:(int32_t)firstDigitCount firstAllowedDigits:(NSArray<SharedInt *> *)firstAllowedDigits secondDigitCount:(int32_t)secondDigitCount secondAllowedDigits:(NSArray<SharedInt *> *)secondAllowedDigits __attribute__((swift_name("init(minCup:maxCup:firstDigitCount:firstAllowedDigits:secondDigitCount:secondAllowedDigits:)"))) __attribute__((objc_designated_initializer));
- (SharedCupRuleEngineMultiplicationConfig *)doCopyMinCup:(int32_t)minCup maxCup:(int32_t)maxCup firstDigitCount:(int32_t)firstDigitCount firstAllowedDigits:(NSArray<SharedInt *> *)firstAllowedDigits secondDigitCount:(int32_t)secondDigitCount secondAllowedDigits:(NSArray<SharedInt *> *)secondAllowedDigits __attribute__((swift_name("doCopy(minCup:maxCup:firstDigitCount:firstAllowedDigits:secondDigitCount:secondAllowedDigits:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedInt *> *firstAllowedDigits __attribute__((swift_name("firstAllowedDigits")));
@property (readonly) int32_t firstDigitCount __attribute__((swift_name("firstDigitCount")));
@property (readonly) int32_t maxCup __attribute__((swift_name("maxCup")));
@property (readonly) int32_t minCup __attribute__((swift_name("minCup")));
@property (readonly) NSArray<SharedInt *> *secondAllowedDigits __attribute__((swift_name("secondAllowedDigits")));
@property (readonly) int32_t secondDigitCount __attribute__((swift_name("secondDigitCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupScoreService")))
@interface SharedCupScoreService : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)cupScoreService __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCupScoreService *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)buildSubmitPayloadCupField:(NSString *)cupField won:(BOOL)won difficultyLevel:(int32_t)difficultyLevel isMultiplication:(BOOL)isMultiplication dayId:(NSString *)dayId __attribute__((swift_name("buildSubmitPayload(cupField:won:difficultyLevel:isMultiplication:dayId:)")));
- (NSString *)cupFieldForIsMultiplication:(SharedBoolean * _Nullable)isMultiplication isExtraction:(SharedBoolean * _Nullable)isExtraction isBlinding:(SharedBoolean * _Nullable)isBlinding __attribute__((swift_name("cupFieldFor(isMultiplication:isExtraction:isBlinding:)")));
- (SharedCupScoreServiceCupScoreResult * _Nullable)parseResultResultData:(id _Nullable)resultData __attribute__((swift_name("parseResult(resultData:)")));
@property (readonly) NSString *CF_SUBMIT_CUP_RESULT __attribute__((swift_name("CF_SUBMIT_CUP_RESULT")));
@property (readonly) int32_t MAX_DIFFICULTY __attribute__((swift_name("MAX_DIFFICULTY")));
@property (readonly) int32_t MIN_DIFFICULTY __attribute__((swift_name("MIN_DIFFICULTY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupScoreService.CupScoreResult")))
@interface SharedCupScoreServiceCupScoreResult : SharedBase
- (instancetype)initWithOldScore:(int32_t)oldScore newScore:(int32_t)newScore __attribute__((swift_name("init(oldScore:newScore:)"))) __attribute__((objc_designated_initializer));
- (SharedCupScoreServiceCupScoreResult *)doCopyOldScore:(int32_t)oldScore newScore:(int32_t)newScore __attribute__((swift_name("doCopy(oldScore:newScore:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly, getter=doNewScore) int32_t newScore __attribute__((swift_name("newScore")));
@property (readonly) int32_t oldScore __attribute__((swift_name("oldScore")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupTier")))
@interface SharedCupTier : SharedBase
- (instancetype)initWithIcon:(NSString *)icon cupName:(NSString *)cupName cupMotivation:(NSArray<NSString *> *)cupMotivation __attribute__((swift_name("init(icon:cupName:cupMotivation:)"))) __attribute__((objc_designated_initializer));
- (SharedCupTier *)doCopyIcon:(NSString *)icon cupName:(NSString *)cupName cupMotivation:(NSArray<NSString *> *)cupMotivation __attribute__((swift_name("doCopy(icon:cupName:cupMotivation:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *cupMotivation __attribute__((swift_name("cupMotivation")));
@property (readonly) NSString *cupName __attribute__((swift_name("cupName")));
@property (readonly) NSString *icon __attribute__((swift_name("icon")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupUpdateResult")))
@interface SharedCupUpdateResult : SharedBase
- (instancetype)initWithOldScore:(int32_t)oldScore newScore:(int32_t)newScore progressUpdate:(NSDictionary<NSString *, id> *)progressUpdate historyUpdate:(NSDictionary<NSString *, id> *)historyUpdate __attribute__((swift_name("init(oldScore:newScore:progressUpdate:historyUpdate:)"))) __attribute__((objc_designated_initializer));
- (SharedCupUpdateResult *)doCopyOldScore:(int32_t)oldScore newScore:(int32_t)newScore progressUpdate:(NSDictionary<NSString *, id> *)progressUpdate historyUpdate:(NSDictionary<NSString *, id> *)historyUpdate __attribute__((swift_name("doCopy(oldScore:newScore:progressUpdate:historyUpdate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, id> *historyUpdate __attribute__((swift_name("historyUpdate")));
@property (readonly, getter=doNewScore) int32_t newScore __attribute__((swift_name("newScore")));
@property (readonly) int32_t oldScore __attribute__((swift_name("oldScore")));
@property (readonly) NSDictionary<NSString *, id> *progressUpdate __attribute__((swift_name("progressUpdate")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ExtractionCupRepository")))
@interface SharedExtractionCupRepository : SharedCupScoreRepositorySpec
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)extractionCupRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedExtractionCupRepository *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t CUP_STEP __attribute__((swift_name("CUP_STEP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ImpactCupRepository")))
@interface SharedImpactCupRepository : SharedCupScoreRepositorySpec
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithFIELD:(NSString *)FIELD __attribute__((swift_name("init(FIELD:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)impactCupRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedImpactCupRepository *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t CUP_STEP __attribute__((swift_name("CUP_STEP")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KupaEkranKoprusu")))
@interface SharedKupaEkranKoprusu : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)kupaEkranKoprusu __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKupaEkranKoprusu *shared __attribute__((swift_name("shared")));
- (NSString *)gecmisBaslangicKimligiTabIndex:(int32_t)tabIndex __attribute__((swift_name("gecmisBaslangicKimligi(tabIndex:)")));
- (SharedKupaGrafikVerisi *)gecmisGrafigiTabIndex:(int32_t)tabIndex noktalar:(NSArray<SharedKupaGecmisNoktasi *> *)noktalar __attribute__((swift_name("gecmisGrafigi(tabIndex:noktalar:)")));
- (int32_t)gecmisPencereGunuTabIndex:(int32_t)tabIndex __attribute__((swift_name("gecmisPencereGunu(tabIndex:)")));
- (NSArray<NSString *> *)gecmisSekmeBasliklari __attribute__((swift_name("gecmisSekmeBasliklari()")));
- (int32_t)gecmisVarsayilanSkor __attribute__((swift_name("gecmisVarsayilanSkor()")));
- (int32_t)guncelSezon __attribute__((swift_name("guncelSezon()")));
- (NSArray<SharedRozetKutuVerisi *> *)kisiselRekorlar __attribute__((swift_name("kisiselRekorlar()")));
- (NSArray<SharedLiderSatirVerisi *> *)liderSatirlariKayitlar:(NSArray<SharedLiderHamKayit *> *)kayitlar currentUid:(NSString *)currentUid __attribute__((swift_name("liderSatirlari(kayitlar:currentUid:)")));
- (NSString *)liderlikHataMetniYerelIleti:(NSString *)yerelIleti yetkiHatasi:(BOOL)yetkiHatasi __attribute__((swift_name("liderlikHataMetni(yerelIleti:yetkiHatasi:)")));
- (int32_t)liderlikSorguLimiti __attribute__((swift_name("liderlikSorguLimiti()")));
- (NSString *)liderlikTahtaKimligiPartId:(int32_t)partId lessonKey:(NSString *)lessonKey season:(int32_t)season __attribute__((swift_name("liderlikTahtaKimligi(partId:lessonKey:season:)")));
- (void)rozetDurumunuYukleDoc:(NSDictionary<NSString *, id> * _Nullable)doc __attribute__((swift_name("rozetDurumunuYukle(doc:)")));
- (SharedRozetEkranVerisi *)rozetEkraniModeName:(NSString *)modeName __attribute__((swift_name("rozetEkrani(modeName:)")));
- (int32_t)rozetSatirBoyu __attribute__((swift_name("rozetSatirBoyu()")));
- (NSArray<SharedRozetKutuVerisi *> *)rozetler __attribute__((swift_name("rozetler()")));
- (int64_t)sezonSayacAraligiMs __attribute__((swift_name("sezonSayacAraligiMs()")));
- (SharedSezonVerisi *)sezonVerisiBoundSeason:(int32_t)boundSeason __attribute__((swift_name("sezonVerisi(boundSeason:)")));
- (int64_t)sezonYoklamaAraligiMs __attribute__((swift_name("sezonYoklamaAraligiMs()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KupaGecmisNoktasi")))
@interface SharedKupaGecmisNoktasi : SharedBase
- (instancetype)initWithDateId:(NSString *)dateId value:(int32_t)value __attribute__((swift_name("init(dateId:value:)"))) __attribute__((objc_designated_initializer));
- (SharedKupaGecmisNoktasi *)doCopyDateId:(NSString *)dateId value:(int32_t)value __attribute__((swift_name("doCopy(dateId:value:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *dateId __attribute__((swift_name("dateId")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KupaGrafikNoktasi")))
@interface SharedKupaGrafikNoktasi : SharedBase
- (instancetype)initWithLabel:(NSString *)label value:(int32_t)value __attribute__((swift_name("init(label:value:)"))) __attribute__((objc_designated_initializer));
- (SharedKupaGrafikNoktasi *)doCopyLabel:(NSString *)label value:(int32_t)value __attribute__((swift_name("doCopy(label:value:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KupaGrafikVerisi")))
@interface SharedKupaGrafikVerisi : SharedBase
- (instancetype)initWithIsEmpty:(BOOL)isEmpty isSinglePoint:(BOOL)isSinglePoint points:(NSArray<SharedKupaGrafikNoktasi *> *)points axisMin:(int32_t)axisMin axisMax:(int32_t)axisMax xAxisLabelCount:(int32_t)xAxisLabelCount __attribute__((swift_name("init(isEmpty:isSinglePoint:points:axisMin:axisMax:xAxisLabelCount:)"))) __attribute__((objc_designated_initializer));
- (SharedKupaGrafikVerisi *)doCopyIsEmpty:(BOOL)isEmpty isSinglePoint:(BOOL)isSinglePoint points:(NSArray<SharedKupaGrafikNoktasi *> *)points axisMin:(int32_t)axisMin axisMax:(int32_t)axisMax xAxisLabelCount:(int32_t)xAxisLabelCount __attribute__((swift_name("doCopy(isEmpty:isSinglePoint:points:axisMin:axisMax:xAxisLabelCount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t axisMax __attribute__((swift_name("axisMax")));
@property (readonly) int32_t axisMin __attribute__((swift_name("axisMin")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) BOOL isSinglePoint __attribute__((swift_name("isSinglePoint")));
@property (readonly) NSArray<SharedKupaGrafikNoktasi *> *points __attribute__((swift_name("points")));
@property (readonly) int32_t xAxisLabelCount __attribute__((swift_name("xAxisLabelCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LiderHamKayit")))
@interface SharedLiderHamKayit : SharedBase
- (instancetype)initWithUserId:(NSString *)userId recordScore:(int32_t)recordScore displayName:(NSString *)displayName recordLabel:(NSString *)recordLabel avatarConfig:(NSString *)avatarConfig __attribute__((swift_name("init(userId:recordScore:displayName:recordLabel:avatarConfig:)"))) __attribute__((objc_designated_initializer));
- (SharedLiderHamKayit *)doCopyUserId:(NSString *)userId recordScore:(int32_t)recordScore displayName:(NSString *)displayName recordLabel:(NSString *)recordLabel avatarConfig:(NSString *)avatarConfig __attribute__((swift_name("doCopy(userId:recordScore:displayName:recordLabel:avatarConfig:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *avatarConfig __attribute__((swift_name("avatarConfig")));
@property (readonly) NSString *displayName __attribute__((swift_name("displayName")));
@property (readonly) NSString *recordLabel __attribute__((swift_name("recordLabel")));
@property (readonly) int32_t recordScore __attribute__((swift_name("recordScore")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LiderSatirVerisi")))
@interface SharedLiderSatirVerisi : SharedBase
- (instancetype)initWithUserId:(NSString *)userId displayRank:(int32_t)displayRank rewardRank:(int32_t)rewardRank displayName:(NSString *)displayName recordLabel:(NSString *)recordLabel avatarConfig:(NSString *)avatarConfig hasAvatar:(BOOL)hasAvatar isCurrentUser:(BOOL)isCurrentUser rankBadgeDrawable:(NSString *)rankBadgeDrawable cardColorRes:(NSString *)cardColorRes nameColorRes:(NSString *)nameColorRes timeColorRes:(NSString *)timeColorRes __attribute__((swift_name("init(userId:displayRank:rewardRank:displayName:recordLabel:avatarConfig:hasAvatar:isCurrentUser:rankBadgeDrawable:cardColorRes:nameColorRes:timeColorRes:)"))) __attribute__((objc_designated_initializer));
- (SharedLiderSatirVerisi *)doCopyUserId:(NSString *)userId displayRank:(int32_t)displayRank rewardRank:(int32_t)rewardRank displayName:(NSString *)displayName recordLabel:(NSString *)recordLabel avatarConfig:(NSString *)avatarConfig hasAvatar:(BOOL)hasAvatar isCurrentUser:(BOOL)isCurrentUser rankBadgeDrawable:(NSString *)rankBadgeDrawable cardColorRes:(NSString *)cardColorRes nameColorRes:(NSString *)nameColorRes timeColorRes:(NSString *)timeColorRes __attribute__((swift_name("doCopy(userId:displayRank:rewardRank:displayName:recordLabel:avatarConfig:hasAvatar:isCurrentUser:rankBadgeDrawable:cardColorRes:nameColorRes:timeColorRes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *avatarConfig __attribute__((swift_name("avatarConfig")));
@property (readonly) NSString *cardColorRes __attribute__((swift_name("cardColorRes")));
@property (readonly) NSString *displayName __attribute__((swift_name("displayName")));
@property (readonly) int32_t displayRank __attribute__((swift_name("displayRank")));
@property (readonly) BOOL hasAvatar __attribute__((swift_name("hasAvatar")));
@property (readonly) BOOL isCurrentUser __attribute__((swift_name("isCurrentUser")));
@property (readonly) NSString *nameColorRes __attribute__((swift_name("nameColorRes")));
@property (readonly) NSString *rankBadgeDrawable __attribute__((swift_name("rankBadgeDrawable")));
@property (readonly) NSString *recordLabel __attribute__((swift_name("recordLabel")));
@property (readonly) int32_t rewardRank __attribute__((swift_name("rewardRank")));
@property (readonly) NSString *timeColorRes __attribute__((swift_name("timeColorRes")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RozetEkranVerisi")))
@interface SharedRozetEkranVerisi : SharedBase
- (instancetype)initWithModeName:(NSString *)modeName unlocked:(BOOL)unlocked rewardAmountText:(NSString *)rewardAmountText showRewardAmount:(BOOL)showRewardAmount detailText:(NSString *)detailText toneName:(NSString *)toneName baseAsset:(NSString *)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale withCircleFrame:(BOOL)withCircleFrame showProgress:(BOOL)showProgress progressText:(NSString *)progressText progressPercent:(int32_t)progressPercent pieceCountText:(NSString *)pieceCountText pieceIsCup:(BOOL)pieceIsCup pieceRows:(NSArray<SharedRozetParcaVerisi *> *)pieceRows pieceAsset:(NSString *)pieceAsset replayOnTap:(BOOL)replayOnTap __attribute__((swift_name("init(modeName:unlocked:rewardAmountText:showRewardAmount:detailText:toneName:baseAsset:topAsset:topHoldFrame:topScale:withCircleFrame:showProgress:progressText:progressPercent:pieceCountText:pieceIsCup:pieceRows:pieceAsset:replayOnTap:)"))) __attribute__((objc_designated_initializer));
- (SharedRozetEkranVerisi *)doCopyModeName:(NSString *)modeName unlocked:(BOOL)unlocked rewardAmountText:(NSString *)rewardAmountText showRewardAmount:(BOOL)showRewardAmount detailText:(NSString *)detailText toneName:(NSString *)toneName baseAsset:(NSString *)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale withCircleFrame:(BOOL)withCircleFrame showProgress:(BOOL)showProgress progressText:(NSString *)progressText progressPercent:(int32_t)progressPercent pieceCountText:(NSString *)pieceCountText pieceIsCup:(BOOL)pieceIsCup pieceRows:(NSArray<SharedRozetParcaVerisi *> *)pieceRows pieceAsset:(NSString *)pieceAsset replayOnTap:(BOOL)replayOnTap __attribute__((swift_name("doCopy(modeName:unlocked:rewardAmountText:showRewardAmount:detailText:toneName:baseAsset:topAsset:topHoldFrame:topScale:withCircleFrame:showProgress:progressText:progressPercent:pieceCountText:pieceIsCup:pieceRows:pieceAsset:replayOnTap:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *baseAsset __attribute__((swift_name("baseAsset")));
@property (readonly) NSString *detailText __attribute__((swift_name("detailText")));
@property (readonly) NSString *modeName __attribute__((swift_name("modeName")));
@property (readonly) NSString *pieceAsset __attribute__((swift_name("pieceAsset")));
@property (readonly) NSString *pieceCountText __attribute__((swift_name("pieceCountText")));
@property (readonly) BOOL pieceIsCup __attribute__((swift_name("pieceIsCup")));
@property (readonly) NSArray<SharedRozetParcaVerisi *> *pieceRows __attribute__((swift_name("pieceRows")));
@property (readonly) int32_t progressPercent __attribute__((swift_name("progressPercent")));
@property (readonly) NSString *progressText __attribute__((swift_name("progressText")));
@property (readonly) BOOL replayOnTap __attribute__((swift_name("replayOnTap")));
@property (readonly) NSString *rewardAmountText __attribute__((swift_name("rewardAmountText")));
@property (readonly) BOOL showProgress __attribute__((swift_name("showProgress")));
@property (readonly) BOOL showRewardAmount __attribute__((swift_name("showRewardAmount")));
@property (readonly) NSString *toneName __attribute__((swift_name("toneName")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@property (readonly) BOOL unlocked __attribute__((swift_name("unlocked")));
@property (readonly) BOOL withCircleFrame __attribute__((swift_name("withCircleFrame")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RozetKutuVerisi")))
@interface SharedRozetKutuVerisi : SharedBase
- (instancetype)initWithModeName:(NSString *)modeName title:(NSString *)title subtext:(NSString *)subtext valueText:(NSString *)valueText showCount:(BOOL)showCount unlocked:(BOOL)unlocked toneName:(NSString *)toneName baseAsset:(NSString *)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale withCircleFrame:(BOOL)withCircleFrame __attribute__((swift_name("init(modeName:title:subtext:valueText:showCount:unlocked:toneName:baseAsset:topAsset:topHoldFrame:topScale:withCircleFrame:)"))) __attribute__((objc_designated_initializer));
- (SharedRozetKutuVerisi *)doCopyModeName:(NSString *)modeName title:(NSString *)title subtext:(NSString *)subtext valueText:(NSString *)valueText showCount:(BOOL)showCount unlocked:(BOOL)unlocked toneName:(NSString *)toneName baseAsset:(NSString *)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale withCircleFrame:(BOOL)withCircleFrame __attribute__((swift_name("doCopy(modeName:title:subtext:valueText:showCount:unlocked:toneName:baseAsset:topAsset:topHoldFrame:topScale:withCircleFrame:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *baseAsset __attribute__((swift_name("baseAsset")));
@property (readonly) NSString *modeName __attribute__((swift_name("modeName")));
@property (readonly) BOOL showCount __attribute__((swift_name("showCount")));
@property (readonly) NSString *subtext __attribute__((swift_name("subtext")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString *toneName __attribute__((swift_name("toneName")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@property (readonly) BOOL unlocked __attribute__((swift_name("unlocked")));
@property (readonly) NSString *valueText __attribute__((swift_name("valueText")));
@property (readonly) BOOL withCircleFrame __attribute__((swift_name("withCircleFrame")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RozetParcaVerisi")))
@interface SharedRozetParcaVerisi : SharedBase
- (instancetype)initWithTitleUnit:(NSString *)titleUnit rankText:(NSString *)rankText seasonText:(NSString *)seasonText __attribute__((swift_name("init(titleUnit:rankText:seasonText:)"))) __attribute__((objc_designated_initializer));
- (SharedRozetParcaVerisi *)doCopyTitleUnit:(NSString *)titleUnit rankText:(NSString *)rankText seasonText:(NSString *)seasonText __attribute__((swift_name("doCopy(titleUnit:rankText:seasonText:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *rankText __attribute__((swift_name("rankText")));
@property (readonly) NSString *seasonText __attribute__((swift_name("seasonText")));
@property (readonly) NSString *titleUnit __attribute__((swift_name("titleUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SezonVerisi")))
@interface SharedSezonVerisi : SharedBase
- (instancetype)initWithSeason:(int32_t)season seasonLabel:(NSString *)seasonLabel countdownText:(NSString *)countdownText remainingMs:(int64_t)remainingMs __attribute__((swift_name("init(season:seasonLabel:countdownText:remainingMs:)"))) __attribute__((objc_designated_initializer));
- (SharedSezonVerisi *)doCopySeason:(int32_t)season seasonLabel:(NSString *)seasonLabel countdownText:(NSString *)countdownText remainingMs:(int64_t)remainingMs __attribute__((swift_name("doCopy(season:seasonLabel:countdownText:remainingMs:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *countdownText __attribute__((swift_name("countdownText")));
@property (readonly) int64_t remainingMs __attribute__((swift_name("remainingMs")));
@property (readonly) int32_t season __attribute__((swift_name("season")));
@property (readonly) NSString *seasonLabel __attribute__((swift_name("seasonLabel")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeAnimMode")))
@interface SharedBadgeAnimMode : SharedKotlinEnum<SharedBadgeAnimMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedBadgeAnimMode *dailyOnly __attribute__((swift_name("dailyOnly")));
@property (class, readonly) SharedBadgeAnimMode *rocketWithBase __attribute__((swift_name("rocketWithBase")));
@property (class, readonly) SharedBadgeAnimMode *bowlingWithBase __attribute__((swift_name("bowlingWithBase")));
@property (class, readonly) SharedBadgeAnimMode *golfWithBase __attribute__((swift_name("golfWithBase")));
@property (class, readonly) SharedBadgeAnimMode *fishingWithBase __attribute__((swift_name("fishingWithBase")));
@property (class, readonly) SharedBadgeAnimMode *cupGoogle __attribute__((swift_name("cupGoogle")));
@property (class, readonly) SharedBadgeAnimMode *karate __attribute__((swift_name("karate")));
@property (class, readonly) SharedBadgeAnimMode *goldMedal __attribute__((swift_name("goldMedal")));
@property (class, readonly) SharedBadgeAnimMode *silverMedal __attribute__((swift_name("silverMedal")));
@property (class, readonly) SharedBadgeAnimMode *bronzeMedal __attribute__((swift_name("bronzeMedal")));
@property (class, readonly) SharedBadgeAnimMode *tornado __attribute__((swift_name("tornado")));
@property (class, readonly) SharedBadgeAnimMode *volcano __attribute__((swift_name("volcano")));
@property (class, readonly) SharedBadgeAnimMode *dino __attribute__((swift_name("dino")));
@property (class, readonly) SharedBadgeAnimMode *crocodile __attribute__((swift_name("crocodile")));
@property (class, readonly) SharedBadgeAnimMode *goat __attribute__((swift_name("goat")));
@property (class, readonly) SharedBadgeAnimMode *eagle __attribute__((swift_name("eagle")));
@property (class, readonly) SharedBadgeAnimMode *fly __attribute__((swift_name("fly")));
@property (class, readonly) SharedBadgeAnimMode *turtle __attribute__((swift_name("turtle")));
+ (SharedKotlinArray<SharedBadgeAnimMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBadgeAnimMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeDetailLogic")))
@interface SharedBadgeDetailLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgeDetailLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgeDetailLogic *shared __attribute__((swift_name("shared")));
- (NSArray<NSArray<SharedBadgeDetailLogicBadgeDetailUiState *> *> *)awardBadgeRows __attribute__((swift_name("awardBadgeRows()")));
- (NSArray<SharedBadgeDetailLogicBadgeDetailUiState *> *)awardBadges __attribute__((swift_name("awardBadges()")));
- (float)frameToProgressFrame:(float)frame startFrame:(float)startFrame endFrame:(float)endFrame __attribute__((swift_name("frameToProgress(frame:startFrame:endFrame:)")));
- (SharedBadgeDetailLogicBadgeRenderConfig *)getRenderConfigKind:(SharedBadgeKind *)kind __attribute__((swift_name("getRenderConfig(kind:)")));
- (int32_t)levelSortScoreTone:(SharedBadgeLevelTone * _Nullable)tone __attribute__((swift_name("levelSortScore(tone:)")));
- (NSArray<SharedBadgeDetailLogicBadgeDetailUiState *> *)personalRecordBadges __attribute__((swift_name("personalRecordBadges()")));
- (SharedBadgeDetailLogicBadgeDetailUiState *)toDetailUiStateState:(SharedBadgeState *)state __attribute__((swift_name("toDetailUiState(state:)")));
@property (readonly) int32_t AWARDS_PER_ROW __attribute__((swift_name("AWARDS_PER_ROW")));
@property (readonly) NSArray<SharedBadgeKind *> *awardKinds __attribute__((swift_name("awardKinds")));
@property (readonly) NSArray<SharedBadgeKind *> *personalRecordKinds __attribute__((swift_name("personalRecordKinds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeDetailLogic.BadgeDetailUiState")))
@interface SharedBadgeDetailLogicBadgeDetailUiState : SharedBase
- (instancetype)initWithTitle:(NSString *)title subtext:(NSString *)subtext progressCount:(int32_t)progressCount showCount:(BOOL)showCount targetMode:(SharedBadgeAnimMode *)targetMode unlocked:(BOOL)unlocked levelTone:(SharedBadgeLevelTone * _Nullable)levelTone baseAsset:(NSString * _Nullable)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topBackgroundRes:(NSString * _Nullable)topBackgroundRes __attribute__((swift_name("init(title:subtext:progressCount:showCount:targetMode:unlocked:levelTone:baseAsset:topAsset:topHoldFrame:topScale:topBackgroundRes:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeDetailLogicBadgeDetailUiState *)doCopyTitle:(NSString *)title subtext:(NSString *)subtext progressCount:(int32_t)progressCount showCount:(BOOL)showCount targetMode:(SharedBadgeAnimMode *)targetMode unlocked:(BOOL)unlocked levelTone:(SharedBadgeLevelTone * _Nullable)levelTone baseAsset:(NSString * _Nullable)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topBackgroundRes:(NSString * _Nullable)topBackgroundRes __attribute__((swift_name("doCopy(title:subtext:progressCount:showCount:targetMode:unlocked:levelTone:baseAsset:topAsset:topHoldFrame:topScale:topBackgroundRes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable baseAsset __attribute__((swift_name("baseAsset")));
@property (readonly) SharedBadgeLevelTone * _Nullable levelTone __attribute__((swift_name("levelTone")));
@property (readonly) int32_t progressCount __attribute__((swift_name("progressCount")));
@property (readonly) BOOL showCount __attribute__((swift_name("showCount")));
@property (readonly) NSString *subtext __attribute__((swift_name("subtext")));
@property (readonly) SharedBadgeAnimMode *targetMode __attribute__((swift_name("targetMode")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) NSString * _Nullable topBackgroundRes __attribute__((swift_name("topBackgroundRes")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@property (readonly) BOOL unlocked __attribute__((swift_name("unlocked")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeDetailLogic.BadgeRenderConfig")))
@interface SharedBadgeDetailLogicBadgeRenderConfig : SharedBase
- (instancetype)initWithBaseAsset:(NSString * _Nullable)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topBackgroundRes:(NSString * _Nullable)topBackgroundRes __attribute__((swift_name("init(baseAsset:topAsset:topHoldFrame:topScale:topBackgroundRes:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeDetailLogicBadgeRenderConfig *)doCopyBaseAsset:(NSString * _Nullable)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topBackgroundRes:(NSString * _Nullable)topBackgroundRes __attribute__((swift_name("doCopy(baseAsset:topAsset:topHoldFrame:topScale:topBackgroundRes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable baseAsset __attribute__((swift_name("baseAsset")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) NSString * _Nullable topBackgroundRes __attribute__((swift_name("topBackgroundRes")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeFragmentLogic")))
@interface SharedBadgeFragmentLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgeFragmentLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgeFragmentLogic *shared __attribute__((swift_name("shared")));
- (NSString *)badgeDetailTextMode:(SharedBadgeAnimMode *)mode unlocked:(BOOL)unlocked celebrateTarget:(SharedInt * _Nullable)celebrateTarget __attribute__((swift_name("badgeDetailText(mode:unlocked:celebrateTarget:)")));
- (int32_t)celebrationAnimatedCurrentFromProgress:(int32_t)fromProgress reachedTarget:(int32_t)reachedTarget animatedFraction:(float)animatedFraction __attribute__((swift_name("celebrationAnimatedCurrent(fromProgress:reachedTarget:animatedFraction:)")));
- (SharedBadgeFragmentLogicProgressUi *)celebrationProgressUiFraction:(float)fraction current:(int32_t)current target:(int32_t)target __attribute__((swift_name("celebrationProgressUi(fraction:current:target:)")));
- (NSArray<SharedCupPieceRow *> *)cupPieceRowsProgress:(SharedUserBadgeProgress *)progress pieceSeasonFilter:(SharedInt * _Nullable)pieceSeasonFilter __attribute__((swift_name("cupPieceRows(progress:pieceSeasonFilter:)")));
- (SharedInt * _Nullable)defaultRewardAmountForModeMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("defaultRewardAmountForMode(mode:)")));
- (NSString *)formatUnlockedDetailTextMode:(SharedBadgeAnimMode *)mode template:(NSString *)template_ celebrateTarget:(SharedInt * _Nullable)celebrateTarget __attribute__((swift_name("formatUnlockedDetailText(mode:template:celebrateTarget:)")));
- (float)frameToProgressTargetFrame:(float)targetFrame startFrame:(float)startFrame endFrame:(float)endFrame __attribute__((swift_name("frameToProgress(targetFrame:startFrame:endFrame:)")));
- (BOOL)hasNextCelebrationInQueueQueueSize:(SharedInt * _Nullable)queueSize index:(int32_t)index __attribute__((swift_name("hasNextCelebrationInQueue(queueSize:index:)")));
- (SharedBadgeFragmentLogicLevelUpAnimConfig *)levelUpAnimConfigMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("levelUpAnimConfig(mode:)")));
- (SharedBadgeFragmentLogicLevelUpCelebrationPlan *)levelUpCelebrationPlanMode:(SharedBadgeAnimMode *)mode fromProgress:(int32_t)fromProgress toProgress:(int32_t)toProgress reachedTarget:(int32_t)reachedTarget __attribute__((swift_name("levelUpCelebrationPlan(mode:fromProgress:toProgress:reachedTarget:)")));
- (NSString *)lockedDetailTextMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("lockedDetailText(mode:)")));
- (SharedBadgeFragmentLogicProgressUi *)lockedProgressUiLockedProgressCurrent:(int32_t)lockedProgressCurrent lockedProgressTarget:(int32_t)lockedProgressTarget __attribute__((swift_name("lockedProgressUi(lockedProgressCurrent:lockedProgressTarget:)")));
- (NSString * _Nullable)medalPieceAssetMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("medalPieceAsset(mode:)")));
- (NSArray<SharedMedalPieceRow *> *)medalPieceRowsMode:(SharedBadgeAnimMode *)mode progress:(SharedUserBadgeProgress *)progress pieceSeasonFilter:(SharedInt * _Nullable)pieceSeasonFilter __attribute__((swift_name("medalPieceRows(mode:progress:pieceSeasonFilter:)")));
- (SharedBadgeFragmentLogicCelebrationQueueItem *)parseQueueItemPayloads:(NSArray<NSString *> *)payloads index:(int32_t)index __attribute__((swift_name("parseQueueItem(payloads:index:)")));
- (SharedBadgeFragmentLogicPieceCountBadge * _Nullable)pieceCountBadgeMode:(SharedBadgeAnimMode *)mode unlocked:(BOOL)unlocked progress:(SharedUserBadgeProgress *)progress isCelebrationFlow:(BOOL)isCelebrationFlow seasonLbPieceFilter:(SharedInt * _Nullable)seasonLbPieceFilter __attribute__((swift_name("pieceCountBadge(mode:unlocked:progress:isCelebrationFlow:seasonLbPieceFilter:)")));
- (NSString *)piecePanelKeyMode:(SharedBadgeAnimMode *)mode isCup:(BOOL)isCup pieceSeasonFilter:(SharedInt * _Nullable)pieceSeasonFilter __attribute__((swift_name("piecePanelKey(mode:isCup:pieceSeasonFilter:)")));
- (int32_t)resolveShownValueFallbackMode:(SharedBadgeAnimMode *)mode progress:(int32_t)progress __attribute__((swift_name("resolveShownValueFallback(mode:progress:)")));
- (BOOL)shouldShowProgressForModeMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("shouldShowProgressForMode(mode:)")));
- (NSString *)unlockedDetailTemplateMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("unlockedDetailTemplate(mode:)")));
@property (readonly) int64_t CELEBRATION_COUNT_DURATION_MS __attribute__((swift_name("CELEBRATION_COUNT_DURATION_MS")));
@property (readonly) float CUP_GOOGLE_HOLD_FRAME __attribute__((swift_name("CUP_GOOGLE_HOLD_FRAME")));
@property (readonly) NSString *CUP_PIECE_ASSET __attribute__((swift_name("CUP_PIECE_ASSET")));
@property (readonly) float KARATE_HOLD_FRAME __attribute__((swift_name("KARATE_HOLD_FRAME")));
@property (readonly) float KARATE_REPLAY_START_FRAME __attribute__((swift_name("KARATE_REPLAY_START_FRAME")));
@property (readonly) float TOP_HOLD_FRAME_BOWLING __attribute__((swift_name("TOP_HOLD_FRAME_BOWLING")));
@property (readonly) float TOP_HOLD_FRAME_DART __attribute__((swift_name("TOP_HOLD_FRAME_DART")));
@property (readonly) float TOP_HOLD_FRAME_FISHING __attribute__((swift_name("TOP_HOLD_FRAME_FISHING")));
@property (readonly) float TOP_HOLD_FRAME_GOLF __attribute__((swift_name("TOP_HOLD_FRAME_GOLF")));
@property (readonly) float TOP_HOLD_FRAME_ROCKET __attribute__((swift_name("TOP_HOLD_FRAME_ROCKET")));
@property (readonly) float TOP_HOLD_FRAME_TORNADO __attribute__((swift_name("TOP_HOLD_FRAME_TORNADO")));
@property (readonly) float TOP_HOLD_FRAME_VOLCANO __attribute__((swift_name("TOP_HOLD_FRAME_VOLCANO")));
@property (readonly) NSDictionary<SharedBadgeAnimMode *, NSString *> *lockedDetailTextOverrides __attribute__((swift_name("lockedDetailTextOverrides")));
@property (readonly) NSSet<SharedBadgeAnimMode *> *pieceModes __attribute__((swift_name("pieceModes")));
@property (readonly) NSDictionary<SharedBadgeAnimMode *, NSString *> *unlockedDetailTextOverrides __attribute__((swift_name("unlockedDetailTextOverrides")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeFragmentLogic.CelebrationQueueItem")))
@interface SharedBadgeFragmentLogicCelebrationQueueItem : SharedBase
- (instancetype)initWithMode:(SharedBadgeAnimMode *)mode from:(int32_t)from to:(int32_t)to target:(int32_t)target __attribute__((swift_name("init(mode:from:to:target:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeFragmentLogicCelebrationQueueItem *)doCopyMode:(SharedBadgeAnimMode *)mode from:(int32_t)from to:(int32_t)to target:(int32_t)target __attribute__((swift_name("doCopy(mode:from:to:target:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t from __attribute__((swift_name("from")));
@property (readonly) SharedBadgeAnimMode *mode __attribute__((swift_name("mode")));
@property (readonly) int32_t target __attribute__((swift_name("target")));
@property (readonly) int32_t to __attribute__((swift_name("to")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeFragmentLogic.LevelUpAnimConfig")))
@interface SharedBadgeFragmentLogicLevelUpAnimConfig : SharedBase
- (instancetype)initWithTopAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topSpeed:(float)topSpeed withCircleFrame:(BOOL)withCircleFrame topRepeatCount:(int32_t)topRepeatCount growFromZero:(BOOL)growFromZero __attribute__((swift_name("init(topAsset:topHoldFrame:topScale:topSpeed:withCircleFrame:topRepeatCount:growFromZero:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeFragmentLogicLevelUpAnimConfig *)doCopyTopAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topSpeed:(float)topSpeed withCircleFrame:(BOOL)withCircleFrame topRepeatCount:(int32_t)topRepeatCount growFromZero:(BOOL)growFromZero __attribute__((swift_name("doCopy(topAsset:topHoldFrame:topScale:topSpeed:withCircleFrame:topRepeatCount:growFromZero:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL growFromZero __attribute__((swift_name("growFromZero")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) int32_t topRepeatCount __attribute__((swift_name("topRepeatCount")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@property (readonly) float topSpeed __attribute__((swift_name("topSpeed")));
@property (readonly) BOOL withCircleFrame __attribute__((swift_name("withCircleFrame")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeFragmentLogic.LevelUpCelebrationPlan")))
@interface SharedBadgeFragmentLogicLevelUpCelebrationPlan : SharedBase
- (instancetype)initWithOldShownValue:(int32_t)oldShownValue newShownValue:(int32_t)newShownValue lockedProgressCurrent:(int32_t)lockedProgressCurrent lockedProgressTarget:(int32_t)lockedProgressTarget previousTone:(SharedBadgeLevelTone * _Nullable)previousTone newTone:(SharedBadgeLevelTone * _Nullable)newTone __attribute__((swift_name("init(oldShownValue:newShownValue:lockedProgressCurrent:lockedProgressTarget:previousTone:newTone:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeFragmentLogicLevelUpCelebrationPlan *)doCopyOldShownValue:(int32_t)oldShownValue newShownValue:(int32_t)newShownValue lockedProgressCurrent:(int32_t)lockedProgressCurrent lockedProgressTarget:(int32_t)lockedProgressTarget previousTone:(SharedBadgeLevelTone * _Nullable)previousTone newTone:(SharedBadgeLevelTone * _Nullable)newTone __attribute__((swift_name("doCopy(oldShownValue:newShownValue:lockedProgressCurrent:lockedProgressTarget:previousTone:newTone:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t lockedProgressCurrent __attribute__((swift_name("lockedProgressCurrent")));
@property (readonly) int32_t lockedProgressTarget __attribute__((swift_name("lockedProgressTarget")));
@property (readonly, getter=doNewShownValue) int32_t newShownValue __attribute__((swift_name("newShownValue")));
@property (readonly, getter=doNewTone) SharedBadgeLevelTone * _Nullable newTone __attribute__((swift_name("newTone")));
@property (readonly) int32_t oldShownValue __attribute__((swift_name("oldShownValue")));
@property (readonly) SharedBadgeLevelTone * _Nullable previousTone __attribute__((swift_name("previousTone")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeFragmentLogic.PieceCountBadge")))
@interface SharedBadgeFragmentLogicPieceCountBadge : SharedBase
- (instancetype)initWithCount:(int32_t)count isCup:(BOOL)isCup __attribute__((swift_name("init(count:isCup:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeFragmentLogicPieceCountBadge *)doCopyCount:(int32_t)count isCup:(BOOL)isCup __attribute__((swift_name("doCopy(count:isCup:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t count __attribute__((swift_name("count")));
@property (readonly) BOOL isCup __attribute__((swift_name("isCup")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeFragmentLogic.ProgressUi")))
@interface SharedBadgeFragmentLogicProgressUi : SharedBase
- (instancetype)initWithText:(NSString *)text percent:(int32_t)percent __attribute__((swift_name("init(text:percent:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeFragmentLogicProgressUi *)doCopyText:(NSString *)text percent:(int32_t)percent __attribute__((swift_name("doCopy(text:percent:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t percent __attribute__((swift_name("percent")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeIncrementTransactionResult")))
@interface SharedBadgeIncrementTransactionResult : SharedBase
- (instancetype)initWithUpdateMap:(NSDictionary<NSString *, id> *)updateMap beforeDart:(int32_t)beforeDart afterDart:(int32_t)afterDart beforeBowling:(int32_t)beforeBowling afterBowling:(int32_t)afterBowling beforeKarate:(int32_t)beforeKarate afterKarate:(int32_t)afterKarate beforeRocket:(int32_t)beforeRocket afterRocket:(int32_t)afterRocket daily:(int32_t)daily dayId:(NSString *)dayId beforeGolf:(int32_t)beforeGolf afterGolf:(int32_t)afterGolf beforeFishingTier:(int32_t)beforeFishingTier afterFishingTier:(int32_t)afterFishingTier afterFishingStreak:(int32_t)afterFishingStreak afterFishingStreakPeriod:(NSString *)afterFishingStreakPeriod beforeTornado:(int32_t)beforeTornado afterTornado:(int32_t)afterTornado beforeVolcano:(int32_t)beforeVolcano afterVolcano:(int32_t)afterVolcano __attribute__((swift_name("init(updateMap:beforeDart:afterDart:beforeBowling:afterBowling:beforeKarate:afterKarate:beforeRocket:afterRocket:daily:dayId:beforeGolf:afterGolf:beforeFishingTier:afterFishingTier:afterFishingStreak:afterFishingStreakPeriod:beforeTornado:afterTornado:beforeVolcano:afterVolcano:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeIncrementTransactionResult *)doCopyUpdateMap:(NSDictionary<NSString *, id> *)updateMap beforeDart:(int32_t)beforeDart afterDart:(int32_t)afterDart beforeBowling:(int32_t)beforeBowling afterBowling:(int32_t)afterBowling beforeKarate:(int32_t)beforeKarate afterKarate:(int32_t)afterKarate beforeRocket:(int32_t)beforeRocket afterRocket:(int32_t)afterRocket daily:(int32_t)daily dayId:(NSString *)dayId beforeGolf:(int32_t)beforeGolf afterGolf:(int32_t)afterGolf beforeFishingTier:(int32_t)beforeFishingTier afterFishingTier:(int32_t)afterFishingTier afterFishingStreak:(int32_t)afterFishingStreak afterFishingStreakPeriod:(NSString *)afterFishingStreakPeriod beforeTornado:(int32_t)beforeTornado afterTornado:(int32_t)afterTornado beforeVolcano:(int32_t)beforeVolcano afterVolcano:(int32_t)afterVolcano __attribute__((swift_name("doCopy(updateMap:beforeDart:afterDart:beforeBowling:afterBowling:beforeKarate:afterKarate:beforeRocket:afterRocket:daily:dayId:beforeGolf:afterGolf:beforeFishingTier:afterFishingTier:afterFishingStreak:afterFishingStreakPeriod:beforeTornado:afterTornado:beforeVolcano:afterVolcano:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t afterBowling __attribute__((swift_name("afterBowling")));
@property (readonly) int32_t afterDart __attribute__((swift_name("afterDart")));
@property (readonly) int32_t afterFishingStreak __attribute__((swift_name("afterFishingStreak")));
@property (readonly) NSString *afterFishingStreakPeriod __attribute__((swift_name("afterFishingStreakPeriod")));
@property (readonly) int32_t afterFishingTier __attribute__((swift_name("afterFishingTier")));
@property (readonly) int32_t afterGolf __attribute__((swift_name("afterGolf")));
@property (readonly) int32_t afterKarate __attribute__((swift_name("afterKarate")));
@property (readonly) int32_t afterRocket __attribute__((swift_name("afterRocket")));
@property (readonly) int32_t afterTornado __attribute__((swift_name("afterTornado")));
@property (readonly) int32_t afterVolcano __attribute__((swift_name("afterVolcano")));
@property (readonly) int32_t beforeBowling __attribute__((swift_name("beforeBowling")));
@property (readonly) int32_t beforeDart __attribute__((swift_name("beforeDart")));
@property (readonly) int32_t beforeFishingTier __attribute__((swift_name("beforeFishingTier")));
@property (readonly) int32_t beforeGolf __attribute__((swift_name("beforeGolf")));
@property (readonly) int32_t beforeKarate __attribute__((swift_name("beforeKarate")));
@property (readonly) int32_t beforeRocket __attribute__((swift_name("beforeRocket")));
@property (readonly) int32_t beforeTornado __attribute__((swift_name("beforeTornado")));
@property (readonly) int32_t beforeVolcano __attribute__((swift_name("beforeVolcano")));
@property (readonly) int32_t daily __attribute__((swift_name("daily")));
@property (readonly) NSString *dayId __attribute__((swift_name("dayId")));
@property (readonly) NSDictionary<NSString *, id> *updateMap __attribute__((swift_name("updateMap")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeKind")))
@interface SharedBadgeKind : SharedKotlinEnum<SharedBadgeKind *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedBadgeKindCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedBadgeKind *cup __attribute__((swift_name("cup")));
@property (class, readonly) SharedBadgeKind *gold __attribute__((swift_name("gold")));
@property (class, readonly) SharedBadgeKind *silver __attribute__((swift_name("silver")));
@property (class, readonly) SharedBadgeKind *bronze __attribute__((swift_name("bronze")));
@property (class, readonly) SharedBadgeKind *karate __attribute__((swift_name("karate")));
@property (class, readonly) SharedBadgeKind *bowling __attribute__((swift_name("bowling")));
@property (class, readonly) SharedBadgeKind *dart __attribute__((swift_name("dart")));
@property (class, readonly) SharedBadgeKind *fishing __attribute__((swift_name("fishing")));
@property (class, readonly) SharedBadgeKind *golf __attribute__((swift_name("golf")));
@property (class, readonly) SharedBadgeKind *rocket __attribute__((swift_name("rocket")));
@property (class, readonly) SharedBadgeKind *tornado __attribute__((swift_name("tornado")));
@property (class, readonly) SharedBadgeKind *volcano __attribute__((swift_name("volcano")));
@property (class, readonly) SharedBadgeKind *dino __attribute__((swift_name("dino")));
@property (class, readonly) SharedBadgeKind *crocodile __attribute__((swift_name("crocodile")));
@property (class, readonly) SharedBadgeKind *goat __attribute__((swift_name("goat")));
@property (class, readonly) SharedBadgeKind *eagle __attribute__((swift_name("eagle")));
@property (class, readonly) SharedBadgeKind *fly __attribute__((swift_name("fly")));
@property (class, readonly) SharedBadgeKind *turtle __attribute__((swift_name("turtle")));
+ (SharedKotlinArray<SharedBadgeKind *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBadgeKind *> *entries __attribute__((swift_name("entries")));
@property (readonly) SharedBadgeAnimMode *mode __attribute__((swift_name("mode")));
@property (readonly) NSString *subtext __attribute__((swift_name("subtext")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeKind.Companion")))
@interface SharedBadgeKindCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgeKindCompanion *shared __attribute__((swift_name("shared")));
- (SharedBadgeKind * _Nullable)fromModeMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("fromMode(mode:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeLevelTone")))
@interface SharedBadgeLevelTone : SharedKotlinEnum<SharedBadgeLevelTone *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedBadgeLevelTone *bronze __attribute__((swift_name("bronze")));
@property (class, readonly) SharedBadgeLevelTone *silver __attribute__((swift_name("silver")));
@property (class, readonly) SharedBadgeLevelTone *original __attribute__((swift_name("original")));
@property (class, readonly) SharedBadgeLevelTone *red __attribute__((swift_name("red")));
@property (class, readonly) SharedBadgeLevelTone *purple __attribute__((swift_name("purple")));
+ (SharedKotlinArray<SharedBadgeLevelTone *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBadgeLevelTone *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeLevelUpPayload")))
@interface SharedBadgeLevelUpPayload : SharedBase
- (instancetype)initWithMode:(SharedBadgeAnimMode *)mode fromProgress:(int32_t)fromProgress toProgress:(int32_t)toProgress reachedTarget:(int32_t)reachedTarget __attribute__((swift_name("init(mode:fromProgress:toProgress:reachedTarget:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeLevelUpPayload *)doCopyMode:(SharedBadgeAnimMode *)mode fromProgress:(int32_t)fromProgress toProgress:(int32_t)toProgress reachedTarget:(int32_t)reachedTarget __attribute__((swift_name("doCopy(mode:fromProgress:toProgress:reachedTarget:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t fromProgress __attribute__((swift_name("fromProgress")));
@property (readonly) SharedBadgeAnimMode *mode __attribute__((swift_name("mode")));
@property (readonly) int32_t reachedTarget __attribute__((swift_name("reachedTarget")));
@property (readonly) int32_t toProgress __attribute__((swift_name("toProgress")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgePieceLeaderboardSync")))
@interface SharedBadgePieceLeaderboardSync : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgePieceLeaderboardSync __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgePieceLeaderboardSync *shared __attribute__((swift_name("shared")));
- (NSArray<SharedCupPieceRow *> *)parseCupPieceListValue:(id _Nullable)value __attribute__((swift_name("parseCupPieceList(value:)")));
- (NSArray<SharedMedalPieceRow *> *)parseMedalPieceListValue:(id _Nullable)value __attribute__((swift_name("parseMedalPieceList(value:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeProgressEngine")))
@interface SharedBadgeProgressEngine : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgeProgressEngine __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgeProgressEngine *shared __attribute__((swift_name("shared")));
- (SharedBadgeProgressWindow *)bowlingProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("bowlingProgressWindow(tierProgress:)")));
- (NSArray<SharedBadgeState *> *)calculateProgress:(SharedUserBadgeProgress *)progress __attribute__((swift_name("calculate(progress:)")));
- (int32_t)crocodileNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("crocodileNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)crocodileProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("crocodileProgressWindow(tierProgress:)")));
- (int32_t)dinoNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("dinoNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)dinoProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("dinoProgressWindow(tierProgress:)")));
- (int32_t)eagleNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("eagleNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)eagleProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("eagleProgressWindow(tierProgress:)")));
- (int32_t)effectiveFishingStreakProgress:(SharedUserBadgeProgress *)progress currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("effectiveFishingStreak(progress:currentPeriodKey:)")));
- (int32_t)fishingNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("fishingNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)fishingProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("fishingProgressWindow(tierProgress:)")));
- (int32_t)flyNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("flyNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)flyProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("flyProgressWindow(tierProgress:)")));
- (int32_t)goatNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("goatNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)goatProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("goatProgressWindow(tierProgress:)")));
- (SharedKotlinPair<NSArray<SharedInt *> *, SharedInt *> * _Nullable)levelSpecMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("levelSpec(mode:)")));
- (SharedKotlinPair<NSArray<SharedInt *> *, SharedInt *> * _Nullable)levelSpecKind:(SharedBadgeKind *)kind __attribute__((swift_name("levelSpec(kind:)")));
- (int32_t)rocketNextThresholdRawProgress:(int32_t)rawProgress __attribute__((swift_name("rocketNextThreshold(rawProgress:)")));
- (SharedBadgeProgressWindow *)rocketProgressWindowRawProgress:(int32_t)rawProgress __attribute__((swift_name("rocketProgressWindow(rawProgress:)")));
- (int32_t)tornadoNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("tornadoNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)tornadoProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("tornadoProgressWindow(tierProgress:)")));
- (int32_t)turtleNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("turtleNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)turtleProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("turtleProgressWindow(tierProgress:)")));
- (int32_t)volcanoNextThresholdTierProgress:(int32_t)tierProgress __attribute__((swift_name("volcanoNextThreshold(tierProgress:)")));
- (SharedBadgeProgressWindow *)volcanoProgressWindowTierProgress:(int32_t)tierProgress __attribute__((swift_name("volcanoProgressWindow(tierProgress:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeProgressFirestore")))
@interface SharedBadgeProgressFirestore : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgeProgressFirestore __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgeProgressFirestore *shared __attribute__((swift_name("shared")));
- (NSString *)badgeStateDocumentPathUid:(NSString *)uid __attribute__((swift_name("badgeStateDocumentPath(uid:)")));
- (SharedCupBadgeSyncResult *)computeCupBadgeProgressSyncField:(NSString *)field snapshot:(NSDictionary<NSString *, id> * _Nullable)snapshot newCupScore:(int32_t)newCupScore __attribute__((swift_name("computeCupBadgeProgressSync(field:snapshot:newCupScore:)")));
- (SharedKotlinPair<SharedInt *, NSString *> *)computeFishingStreakAfterSolveStoredStreak:(int32_t)storedStreak lastStreakPeriodKey:(NSString *)lastStreakPeriodKey currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("computeFishingStreakAfterSolve(storedStreak:lastStreakPeriodKey:currentPeriodKey:)")));
- (SharedBadgeIncrementTransactionResult *)computeIncrementBadgeProgressTransactionSnapshot:(NSDictionary<NSString *, id> * _Nullable)snapshot incrementDart:(BOOL)incrementDart incrementBowlingBy:(int32_t)incrementBowlingBy incrementKarate:(BOOL)incrementKarate incrementRocketDailyLessons:(BOOL)incrementRocketDailyLessons incrementGolf:(BOOL)incrementGolf incrementFishing:(BOOL)incrementFishing incrementTornado:(BOOL)incrementTornado incrementVolcano:(BOOL)incrementVolcano dailyQuestionPeriodKey:(NSString * _Nullable)dailyQuestionPeriodKey today:(NSString *)today currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("computeIncrementBadgeProgressTransaction(snapshot:incrementDart:incrementBowlingBy:incrementKarate:incrementRocketDailyLessons:incrementGolf:incrementFishing:incrementTornado:incrementVolcano:dailyQuestionPeriodKey:today:currentPeriodKey:)")));
- (NSString * _Nullable)cupBadgeProgressFieldMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("cupBadgeProgressField(mode:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)finishIncrementBadgeProgressResult:(SharedBadgeIncrementTransactionResult *)result incrementDart:(BOOL)incrementDart incrementBowlingBy:(int32_t)incrementBowlingBy incrementKarate:(BOOL)incrementKarate incrementGolf:(BOOL)incrementGolf incrementFishing:(BOOL)incrementFishing incrementTornado:(BOOL)incrementTornado incrementVolcano:(BOOL)incrementVolcano __attribute__((swift_name("finishIncrementBadgeProgress(result:incrementDart:incrementBowlingBy:incrementKarate:incrementGolf:incrementFishing:incrementTornado:incrementVolcano:)")));
- (SharedUserBadgeProgress *)migrateLegacyFishingFieldsDoc:(NSDictionary<NSString *, id> * _Nullable)doc progress:(SharedUserBadgeProgress *)progress currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("migrateLegacyFishingFields(doc:progress:currentPeriodKey:)")));
- (NSString *)payloadToQueueItemPayload:(SharedBadgeLevelUpPayload *)payload __attribute__((swift_name("payloadToQueueItem(payload:)")));
- (SharedInt * _Nullable)pendingLeaderboardRewardSeasonFromDocDoc:(NSDictionary<NSString *, id> * _Nullable)doc __attribute__((swift_name("pendingLeaderboardRewardSeasonFromDoc(doc:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveCrocodileLevelUpChainBeforeCrocodile:(int32_t)beforeCrocodile afterCrocodile:(int32_t)afterCrocodile __attribute__((swift_name("resolveCrocodileLevelUpChain(beforeCrocodile:afterCrocodile:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveCupBadgeLevelUpChainMode:(SharedBadgeAnimMode *)mode before:(int32_t)before after:(int32_t)after __attribute__((swift_name("resolveCupBadgeLevelUpChain(mode:before:after:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveDinoLevelUpChainBeforeDino:(int32_t)beforeDino afterDino:(int32_t)afterDino __attribute__((swift_name("resolveDinoLevelUpChain(beforeDino:afterDino:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveEagleLevelUpChainBeforeEagle:(int32_t)beforeEagle afterEagle:(int32_t)afterEagle __attribute__((swift_name("resolveEagleLevelUpChain(beforeEagle:afterEagle:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveFishingLevelUpChainBeforeTier:(int32_t)beforeTier afterTier:(int32_t)afterTier __attribute__((swift_name("resolveFishingLevelUpChain(beforeTier:afterTier:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveFlyLevelUpChainBeforeFly:(int32_t)beforeFly afterFly:(int32_t)afterFly __attribute__((swift_name("resolveFlyLevelUpChain(beforeFly:afterFly:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveGoatLevelUpChainBeforeGoat:(int32_t)beforeGoat afterGoat:(int32_t)afterGoat __attribute__((swift_name("resolveGoatLevelUpChain(beforeGoat:afterGoat:)")));
- (SharedBadgeLevelUpPayload * _Nullable)resolveKarateFirstUnlockPayloadBefore:(int32_t)before after:(int32_t)after __attribute__((swift_name("resolveKarateFirstUnlockPayload(before:after:)")));
- (SharedBadgeLevelUpPayload * _Nullable)resolveLevelUpPayloadMode:(SharedBadgeAnimMode *)mode before:(int32_t)before after:(int32_t)after levels:(NSArray<SharedInt *> * _Nullable)levels __attribute__((swift_name("resolveLevelUpPayload(mode:before:after:levels:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveRocketLevelUpChainBeforeRocket:(int32_t)beforeRocket afterRocket:(int32_t)afterRocket __attribute__((swift_name("resolveRocketLevelUpChain(beforeRocket:afterRocket:)")));
- (NSArray<SharedBadgeLevelUpPayload *> *)resolveTurtleLevelUpChainBeforeTurtle:(int32_t)beforeTurtle afterTurtle:(int32_t)afterTurtle __attribute__((swift_name("resolveTurtleLevelUpChain(beforeTurtle:afterTurtle:)")));
- (SharedUserBadgeProgress *)userBadgeProgressFromStateSnapshotDoc:(NSDictionary<NSString *, id> * _Nullable)doc currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("userBadgeProgressFromStateSnapshot(doc:currentPeriodKey:)")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *DOCUMENT_STATE __attribute__((swift_name("DOCUMENT_STATE")));
@property (readonly) NSString *F_BRONZE_MEDAL_PIECE __attribute__((swift_name("F_BRONZE_MEDAL_PIECE")));
@property (readonly) NSString *F_CUP_PIECE __attribute__((swift_name("F_CUP_PIECE")));
@property (readonly) NSString *F_GOLD_MEDAL_PIECE __attribute__((swift_name("F_GOLD_MEDAL_PIECE")));
@property (readonly) NSString *F_PENDING_LEADERBOARD_REWARD_SEASON __attribute__((swift_name("F_PENDING_LEADERBOARD_REWARD_SEASON")));
@property (readonly) NSString *F_SILVER_MEDAL_PIECE __attribute__((swift_name("F_SILVER_MEDAL_PIECE")));
@property (readonly) NSString *F_USER_BOWLING_PROGRESS __attribute__((swift_name("F_USER_BOWLING_PROGRESS")));
@property (readonly) NSString *F_USER_CROCODILE_PROGRESS __attribute__((swift_name("F_USER_CROCODILE_PROGRESS")));
@property (readonly) NSString *F_USER_DART_PROGRESS __attribute__((swift_name("F_USER_DART_PROGRESS")));
@property (readonly) NSString *F_USER_DINO_PROGRESS __attribute__((swift_name("F_USER_DINO_PROGRESS")));
@property (readonly) NSString *F_USER_EAGLE_PROGRESS __attribute__((swift_name("F_USER_EAGLE_PROGRESS")));
@property (readonly) NSString *F_USER_FISHING_PROGRESS __attribute__((swift_name("F_USER_FISHING_PROGRESS")));
@property (readonly) NSString *F_USER_FISHING_STREAK __attribute__((swift_name("F_USER_FISHING_STREAK")));
@property (readonly) NSString *F_USER_FISHING_STREAK_PERIOD_KEY __attribute__((swift_name("F_USER_FISHING_STREAK_PERIOD_KEY")));
@property (readonly) NSString *F_USER_FLY_PROGRESS __attribute__((swift_name("F_USER_FLY_PROGRESS")));
@property (readonly) NSString *F_USER_GOAT_PROGRESS __attribute__((swift_name("F_USER_GOAT_PROGRESS")));
@property (readonly) NSString *F_USER_GOLF_PROGRESS __attribute__((swift_name("F_USER_GOLF_PROGRESS")));
@property (readonly) NSString *F_USER_KARATE_PROGRESS __attribute__((swift_name("F_USER_KARATE_PROGRESS")));
@property (readonly) NSString *F_USER_ROCKET_DAILY_DAY_ID __attribute__((swift_name("F_USER_ROCKET_DAILY_DAY_ID")));
@property (readonly) NSString *F_USER_ROCKET_DAILY_LESSONS __attribute__((swift_name("F_USER_ROCKET_DAILY_LESSONS")));
@property (readonly) NSString *F_USER_ROCKET_PROGRESS __attribute__((swift_name("F_USER_ROCKET_PROGRESS")));
@property (readonly) NSString *F_USER_TORNADO_PROGRESS __attribute__((swift_name("F_USER_TORNADO_PROGRESS")));
@property (readonly) NSString *F_USER_TURTLE_PROGRESS __attribute__((swift_name("F_USER_TURTLE_PROGRESS")));
@property (readonly) NSString *F_USER_VOLCANO_PROGRESS __attribute__((swift_name("F_USER_VOLCANO_PROGRESS")));
@property (readonly) NSString *SUBCOLLECTION_BADGE_PROGRESS __attribute__((swift_name("SUBCOLLECTION_BADGE_PROGRESS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeProgressRepository")))
@interface SharedBadgeProgressRepository : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)badgeProgressRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBadgeProgressRepository *shared __attribute__((swift_name("shared")));
- (NSString * _Nullable)formatLeveledTierRatioMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("formatLeveledTierRatio(mode:)")));
- (SharedKotlinPair<NSArray<SharedInt *> *, SharedInt *> * _Nullable)getLevelSpecByModeMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("getLevelSpecByMode(mode:)")));
- (SharedInt * _Nullable)getProgressValueByModeMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("getProgressValueByMode(mode:)")));
- (SharedBadgeProgressWindow * _Nullable)getProgressWindowByModeMode:(SharedBadgeAnimMode *)mode today:(NSString *)today currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("getProgressWindowByMode(mode:today:currentPeriodKey:)")));
- (SharedBadgeState * _Nullable)getStateKind:(SharedBadgeKind *)kind __attribute__((swift_name("getState(kind:)")));
- (SharedBadgeState * _Nullable)getStateByModeMode:(SharedBadgeAnimMode *)mode __attribute__((swift_name("getStateByMode(mode:)")));
- (NSArray<SharedBadgeState *> *)getStates __attribute__((swift_name("getStates()")));
- (SharedUserBadgeProgress *)getUserBadgeProgress __attribute__((swift_name("getUserBadgeProgress()")));
- (SharedInt * _Nullable)resolveShownValueByModeMode:(SharedBadgeAnimMode *)mode progress:(int32_t)progress __attribute__((swift_name("resolveShownValueByMode(mode:progress:)")));
- (SharedBadgeLevelTone * _Nullable)resolveToneByModeMode:(SharedBadgeAnimMode *)mode progress:(int32_t)progress __attribute__((swift_name("resolveToneByMode(mode:progress:)")));
- (void)updateProgress:(SharedUserBadgeProgress *)progress __attribute__((swift_name("update(progress:)")));
- (void)updateBowlingProgressNewProgress:(int32_t)newProgress __attribute__((swift_name("updateBowlingProgress(newProgress:)")));
- (void)updateDartProgressNewProgress:(int32_t)newProgress __attribute__((swift_name("updateDartProgress(newProgress:)")));
- (void)updateFishingSyncUserFishingProgress:(int32_t)userFishingProgress userFishingStreak:(int32_t)userFishingStreak userFishingStreakPeriodKey:(NSString *)userFishingStreakPeriodKey __attribute__((swift_name("updateFishingSync(userFishingProgress:userFishingStreak:userFishingStreakPeriodKey:)")));
- (void)updateGolfProgressNewProgress:(int32_t)newProgress __attribute__((swift_name("updateGolfProgress(newProgress:)")));
- (void)updateKarateProgressNewProgress:(int32_t)newProgress __attribute__((swift_name("updateKarateProgress(newProgress:)")));
- (void)updateRocketSyncUserRocketProgress:(int32_t)userRocketProgress userRocketDailyLessons:(int32_t)userRocketDailyLessons userRocketDailyDayId:(NSString *)userRocketDailyDayId __attribute__((swift_name("updateRocketSync(userRocketProgress:userRocketDailyLessons:userRocketDailyDayId:)")));
- (void)updateTornadoProgressNewProgress:(int32_t)newProgress __attribute__((swift_name("updateTornadoProgress(newProgress:)")));
- (void)updateVolcanoProgressNewProgress:(int32_t)newProgress __attribute__((swift_name("updateVolcanoProgress(newProgress:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeProgressWindow")))
@interface SharedBadgeProgressWindow : SharedBase
- (instancetype)initWithCurrent:(int32_t)current target:(int32_t)target __attribute__((swift_name("init(current:target:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeProgressWindow *)doCopyCurrent:(int32_t)current target:(int32_t)target __attribute__((swift_name("doCopy(current:target:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t current __attribute__((swift_name("current")));
@property (readonly) int32_t target __attribute__((swift_name("target")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeState")))
@interface SharedBadgeState : SharedBase
- (instancetype)initWithKind:(SharedBadgeKind *)kind unlocked:(BOOL)unlocked value:(SharedInt * _Nullable)value showValue:(BOOL)showValue levelTone:(SharedBadgeLevelTone * _Nullable)levelTone __attribute__((swift_name("init(kind:unlocked:value:showValue:levelTone:)"))) __attribute__((objc_designated_initializer));
- (SharedBadgeState *)doCopyKind:(SharedBadgeKind *)kind unlocked:(BOOL)unlocked value:(SharedInt * _Nullable)value showValue:(BOOL)showValue levelTone:(SharedBadgeLevelTone * _Nullable)levelTone __attribute__((swift_name("doCopy(kind:unlocked:value:showValue:levelTone:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedBadgeKind *kind __attribute__((swift_name("kind")));
@property (readonly) SharedBadgeLevelTone * _Nullable levelTone __attribute__((swift_name("levelTone")));
@property (readonly) BOOL showValue __attribute__((swift_name("showValue")));
@property (readonly) BOOL unlocked __attribute__((swift_name("unlocked")));
@property (readonly) SharedInt * _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupBadgeSyncResult")))
@interface SharedCupBadgeSyncResult : SharedBase
- (instancetype)initWithBefore:(int32_t)before after:(int32_t)after updateMap:(NSDictionary<NSString *, id> * _Nullable)updateMap __attribute__((swift_name("init(before:after:updateMap:)"))) __attribute__((objc_designated_initializer));
- (SharedCupBadgeSyncResult *)doCopyBefore:(int32_t)before after:(int32_t)after updateMap:(NSDictionary<NSString *, id> * _Nullable)updateMap __attribute__((swift_name("doCopy(before:after:updateMap:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t after __attribute__((swift_name("after")));
@property (readonly) int32_t before __attribute__((swift_name("before")));
@property (readonly) NSDictionary<NSString *, id> * _Nullable updateMap __attribute__((swift_name("updateMap")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupPieceRow")))
@interface SharedCupPieceRow : SharedBase
- (instancetype)initWithTitleUnit:(NSString *)titleUnit rank:(int32_t)rank season:(int32_t)season __attribute__((swift_name("init(titleUnit:rank:season:)"))) __attribute__((objc_designated_initializer));
- (SharedCupPieceRow *)doCopyTitleUnit:(NSString *)titleUnit rank:(int32_t)rank season:(int32_t)season __attribute__((swift_name("doCopy(titleUnit:rank:season:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t rank __attribute__((swift_name("rank")));
@property (readonly) int32_t season __attribute__((swift_name("season")));
@property (readonly) NSString *titleUnit __attribute__((swift_name("titleUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FishingStreakPeriod")))
@interface SharedFishingStreakPeriod : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fishingStreakPeriod __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFishingStreakPeriod *shared __attribute__((swift_name("shared")));
- (NSString *)currentPeriodKeyNowMs:(int64_t)nowMs __attribute__((swift_name("currentPeriodKey(nowMs:)")));
- (BOOL)isConsecutivePeriodAfterLastStreakPeriodKey:(NSString *)lastStreakPeriodKey currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("isConsecutivePeriodAfter(lastStreakPeriodKey:currentPeriodKey:)")));
- (BOOL)isFishingStreakBrokenLastStreakPeriodKey:(NSString *)lastStreakPeriodKey currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("isFishingStreakBroken(lastStreakPeriodKey:currentPeriodKey:)")));
- (BOOL)isSamePeriodA:(NSString *)a b:(NSString *)b __attribute__((swift_name("isSamePeriod(a:b:)")));
- (SharedLong * _Nullable)periodBucketFromKeyKey:(NSString *)key __attribute__((swift_name("periodBucketFromKey(key:)")));
- (NSString *)periodKeyForBucketBucket:(int64_t)bucket __attribute__((swift_name("periodKeyForBucket(bucket:)")));
@property (readonly) int64_t PERIOD_MS __attribute__((swift_name("PERIOD_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MedalPieceRow")))
@interface SharedMedalPieceRow : SharedBase
- (instancetype)initWithTitleUnit:(NSString *)titleUnit season:(int32_t)season __attribute__((swift_name("init(titleUnit:season:)"))) __attribute__((objc_designated_initializer));
- (SharedMedalPieceRow *)doCopyTitleUnit:(NSString *)titleUnit season:(int32_t)season __attribute__((swift_name("doCopy(titleUnit:season:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t season __attribute__((swift_name("season")));
@property (readonly) NSString *titleUnit __attribute__((swift_name("titleUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserBadgeProgress")))
@interface SharedUserBadgeProgress : SharedBase
- (instancetype)initWithUserDartProgress:(int32_t)userDartProgress userKarateProgress:(int32_t)userKarateProgress userGolfProgress:(int32_t)userGolfProgress userBowlingProgress:(int32_t)userBowlingProgress userFishingProgress:(int32_t)userFishingProgress userFishingStreak:(int32_t)userFishingStreak userFishingStreakPeriodKey:(NSString *)userFishingStreakPeriodKey userRocketProgress:(int32_t)userRocketProgress userRocketDailyLessons:(int32_t)userRocketDailyLessons userRocketDailyDayId:(NSString *)userRocketDailyDayId userTornadoProgress:(int32_t)userTornadoProgress userVolcanoProgress:(int32_t)userVolcanoProgress userDinoProgress:(int32_t)userDinoProgress userCrocodileProgress:(int32_t)userCrocodileProgress userGoatProgress:(int32_t)userGoatProgress userEagleProgress:(int32_t)userEagleProgress userFlyProgress:(int32_t)userFlyProgress userTurtleProgress:(int32_t)userTurtleProgress goldMedalPiece:(NSArray<SharedMedalPieceRow *> *)goldMedalPiece silverMedalPiece:(NSArray<SharedMedalPieceRow *> *)silverMedalPiece bronzeMedalPiece:(NSArray<SharedMedalPieceRow *> *)bronzeMedalPiece cupPiece:(NSArray<SharedCupPieceRow *> *)cupPiece pendingLeaderboardRewardSeason:(SharedInt * _Nullable)pendingLeaderboardRewardSeason __attribute__((swift_name("init(userDartProgress:userKarateProgress:userGolfProgress:userBowlingProgress:userFishingProgress:userFishingStreak:userFishingStreakPeriodKey:userRocketProgress:userRocketDailyLessons:userRocketDailyDayId:userTornadoProgress:userVolcanoProgress:userDinoProgress:userCrocodileProgress:userGoatProgress:userEagleProgress:userFlyProgress:userTurtleProgress:goldMedalPiece:silverMedalPiece:bronzeMedalPiece:cupPiece:pendingLeaderboardRewardSeason:)"))) __attribute__((objc_designated_initializer));
- (SharedUserBadgeProgress *)doCopyUserDartProgress:(int32_t)userDartProgress userKarateProgress:(int32_t)userKarateProgress userGolfProgress:(int32_t)userGolfProgress userBowlingProgress:(int32_t)userBowlingProgress userFishingProgress:(int32_t)userFishingProgress userFishingStreak:(int32_t)userFishingStreak userFishingStreakPeriodKey:(NSString *)userFishingStreakPeriodKey userRocketProgress:(int32_t)userRocketProgress userRocketDailyLessons:(int32_t)userRocketDailyLessons userRocketDailyDayId:(NSString *)userRocketDailyDayId userTornadoProgress:(int32_t)userTornadoProgress userVolcanoProgress:(int32_t)userVolcanoProgress userDinoProgress:(int32_t)userDinoProgress userCrocodileProgress:(int32_t)userCrocodileProgress userGoatProgress:(int32_t)userGoatProgress userEagleProgress:(int32_t)userEagleProgress userFlyProgress:(int32_t)userFlyProgress userTurtleProgress:(int32_t)userTurtleProgress goldMedalPiece:(NSArray<SharedMedalPieceRow *> *)goldMedalPiece silverMedalPiece:(NSArray<SharedMedalPieceRow *> *)silverMedalPiece bronzeMedalPiece:(NSArray<SharedMedalPieceRow *> *)bronzeMedalPiece cupPiece:(NSArray<SharedCupPieceRow *> *)cupPiece pendingLeaderboardRewardSeason:(SharedInt * _Nullable)pendingLeaderboardRewardSeason __attribute__((swift_name("doCopy(userDartProgress:userKarateProgress:userGolfProgress:userBowlingProgress:userFishingProgress:userFishingStreak:userFishingStreakPeriodKey:userRocketProgress:userRocketDailyLessons:userRocketDailyDayId:userTornadoProgress:userVolcanoProgress:userDinoProgress:userCrocodileProgress:userGoatProgress:userEagleProgress:userFlyProgress:userTurtleProgress:goldMedalPiece:silverMedalPiece:bronzeMedalPiece:cupPiece:pendingLeaderboardRewardSeason:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedMedalPieceRow *> *bronzeMedalPiece __attribute__((swift_name("bronzeMedalPiece")));
@property (readonly) NSArray<SharedCupPieceRow *> *cupPiece __attribute__((swift_name("cupPiece")));
@property (readonly) NSArray<SharedMedalPieceRow *> *goldMedalPiece __attribute__((swift_name("goldMedalPiece")));
@property (readonly) SharedInt * _Nullable pendingLeaderboardRewardSeason __attribute__((swift_name("pendingLeaderboardRewardSeason")));
@property (readonly) NSArray<SharedMedalPieceRow *> *silverMedalPiece __attribute__((swift_name("silverMedalPiece")));
@property (readonly) int32_t userBowlingProgress __attribute__((swift_name("userBowlingProgress")));
@property (readonly) int32_t userCrocodileProgress __attribute__((swift_name("userCrocodileProgress")));
@property (readonly) int32_t userDartProgress __attribute__((swift_name("userDartProgress")));
@property (readonly) int32_t userDinoProgress __attribute__((swift_name("userDinoProgress")));
@property (readonly) int32_t userEagleProgress __attribute__((swift_name("userEagleProgress")));
@property (readonly) int32_t userFishingProgress __attribute__((swift_name("userFishingProgress")));
@property (readonly) int32_t userFishingStreak __attribute__((swift_name("userFishingStreak")));
@property (readonly) NSString *userFishingStreakPeriodKey __attribute__((swift_name("userFishingStreakPeriodKey")));
@property (readonly) int32_t userFlyProgress __attribute__((swift_name("userFlyProgress")));
@property (readonly) int32_t userGoatProgress __attribute__((swift_name("userGoatProgress")));
@property (readonly) int32_t userGolfProgress __attribute__((swift_name("userGolfProgress")));
@property (readonly) int32_t userKarateProgress __attribute__((swift_name("userKarateProgress")));
@property (readonly) NSString *userRocketDailyDayId __attribute__((swift_name("userRocketDailyDayId")));
@property (readonly) int32_t userRocketDailyLessons __attribute__((swift_name("userRocketDailyLessons")));
@property (readonly) int32_t userRocketProgress __attribute__((swift_name("userRocketProgress")));
@property (readonly) int32_t userTornadoProgress __attribute__((swift_name("userTornadoProgress")));
@property (readonly) int32_t userTurtleProgress __attribute__((swift_name("userTurtleProgress")));
@property (readonly) int32_t userVolcanoProgress __attribute__((swift_name("userVolcanoProgress")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonLeaderboardRepository")))
@interface SharedLessonLeaderboardRepository : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)lessonLeaderboardRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLessonLeaderboardRepository *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> * _Nullable)buildSubmitDataHasUser:(BOOL)hasUser partId:(int32_t)partId lessonKey:(NSString *)lessonKey recordScore:(int32_t)recordScore titleUnit:(NSString * _Nullable)titleUnit __attribute__((swift_name("buildSubmitData(hasUser:partId:lessonKey:recordScore:titleUnit:)")));
- (NSArray<SharedInt *> *)competitionRanksForOrderedDocsRecordScoreValues:(NSArray<id> *)recordScoreValues __attribute__((swift_name("competitionRanksForOrderedDocs(recordScoreValues:)")));
- (NSArray<SharedInt *> *)competitionRanksForOrderedScoresScores:(NSArray<SharedInt *> *)scores __attribute__((swift_name("competitionRanksForOrderedScores(scores:)")));
- (NSString *)entriesCollectionPathPartId:(int32_t)partId lessonKey:(NSString *)lessonKey season:(int32_t)season __attribute__((swift_name("entriesCollectionPath(partId:lessonKey:season:)")));
- (SharedInt * _Nullable)firstChestLessonIndexForPartLessonItemTypes:(NSArray<SharedInt *> *)lessonItemTypes __attribute__((swift_name("firstChestLessonIndexForPart(lessonItemTypes:)")));
- (NSString *)leaderboardDocumentIdPartId:(int32_t)partId lessonKey:(NSString *)lessonKey season:(int32_t)season __attribute__((swift_name("leaderboardDocumentId(partId:lessonKey:season:)")));
- (NSArray<SharedLessonLeaderboardRepositoryLeaderboardEntry *> *)mapLeaderboardEntriesDocuments:(NSArray<SharedKotlinPair<NSString *, NSDictionary<NSString *, id> *> *> *)documents avatarCache:(NSDictionary<NSString *, NSString *> *)avatarCache ownUid:(NSString * _Nullable)ownUid ownAvatarConfig:(NSString * _Nullable)ownAvatarConfig __attribute__((swift_name("mapLeaderboardEntries(documents:avatarCache:ownUid:ownAvatarConfig:)")));
- (NSArray<NSArray<NSString *> *> *)missingAvatarChunksUids:(NSArray<NSString *> *)uids avatarCache:(NSDictionary<NSString *, NSString *> *)avatarCache ownUid:(NSString * _Nullable)ownUid ownAvatarConfig:(NSString * _Nullable)ownAvatarConfig __attribute__((swift_name("missingAvatarChunks(uids:avatarCache:ownUid:ownAvatarConfig:)")));
- (int32_t)rankingScoreRecordScoreValue:(id _Nullable)recordScoreValue __attribute__((swift_name("rankingScore(recordScoreValue:)")));
@property (readonly) int32_t AVATAR_QUERY_CHUNK __attribute__((swift_name("AVATAR_QUERY_CHUNK")));
@property (readonly) NSString *CF_SUBMIT_LEADERBOARD_SCORE __attribute__((swift_name("CF_SUBMIT_LEADERBOARD_SCORE")));
@property (readonly) NSString *COLLECTION __attribute__((swift_name("COLLECTION")));
@property (readonly) NSString *ENTRIES __attribute__((swift_name("ENTRIES")));
@property (readonly) NSString *F_LABEL __attribute__((swift_name("F_LABEL")));
@property (readonly) NSString *F_NAME __attribute__((swift_name("F_NAME")));
@property (readonly) NSString *F_SCORE __attribute__((swift_name("F_SCORE")));
@property (readonly) NSString *F_TITLE_UNIT __attribute__((swift_name("F_TITLE_UNIT")));
@property (readonly) NSString *F_UPDATED __attribute__((swift_name("F_UPDATED")));
@property (readonly) int64_t LEADERBOARD_LIST_QUERY_LIMIT __attribute__((swift_name("LEADERBOARD_LIST_QUERY_LIMIT")));
@property (readonly) int32_t LESSON_ITEM_TYPE_CHEST __attribute__((swift_name("LESSON_ITEM_TYPE_CHEST")));
@property (readonly) NSString *META_LESSON_KEY __attribute__((swift_name("META_LESSON_KEY")));
@property (readonly) NSString *META_PART_ID __attribute__((swift_name("META_PART_ID")));
@property (readonly) NSString *META_SEASON __attribute__((swift_name("META_SEASON")));
@property (readonly) NSString *PUBLIC_PROFILES_COLLECTION __attribute__((swift_name("PUBLIC_PROFILES_COLLECTION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LessonLeaderboardRepository.LeaderboardEntry")))
@interface SharedLessonLeaderboardRepositoryLeaderboardEntry : SharedBase
- (instancetype)initWithUserId:(NSString *)userId displayRank:(int32_t)displayRank rewardRank:(int32_t)rewardRank displayName:(NSString *)displayName recordLabel:(NSString *)recordLabel avatarConfig:(NSString * _Nullable)avatarConfig __attribute__((swift_name("init(userId:displayRank:rewardRank:displayName:recordLabel:avatarConfig:)"))) __attribute__((objc_designated_initializer));
- (SharedLessonLeaderboardRepositoryLeaderboardEntry *)doCopyUserId:(NSString *)userId displayRank:(int32_t)displayRank rewardRank:(int32_t)rewardRank displayName:(NSString *)displayName recordLabel:(NSString *)recordLabel avatarConfig:(NSString * _Nullable)avatarConfig __attribute__((swift_name("doCopy(userId:displayRank:rewardRank:displayName:recordLabel:avatarConfig:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable avatarConfig __attribute__((swift_name("avatarConfig")));
@property (readonly) NSString *displayName __attribute__((swift_name("displayName")));
@property (readonly) int32_t displayRank __attribute__((swift_name("displayRank")));
@property (readonly) NSString *recordLabel __attribute__((swift_name("recordLabel")));
@property (readonly) int32_t rewardRank __attribute__((swift_name("rewardRank")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordLogic")))
@interface SharedRecordLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)recordLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRecordLogic *shared __attribute__((swift_name("shared")));
- (NSString *)badgeDrawableForRankRank:(int32_t)rank __attribute__((swift_name("badgeDrawableForRank(rank:)")));
- (NSString *)displayNameOrFallbackDisplayName:(NSString *)displayName fallback:(NSString *)fallback __attribute__((swift_name("displayNameOrFallback(displayName:fallback:)")));
- (SharedRecordLogicSeasonCountdown *)formatSeasonCountdownRemainingMs:(int64_t)remainingMs __attribute__((swift_name("formatSeasonCountdown(remainingMs:)")));
- (BOOL)hasAvatarAvatarConfig:(NSString * _Nullable)avatarConfig __attribute__((swift_name("hasAvatar(avatarConfig:)")));
- (BOOL)isCurrentUserEntryUserId:(NSString *)entryUserId currentUid:(NSString * _Nullable)currentUid __attribute__((swift_name("isCurrentUser(entryUserId:currentUid:)")));
- (NSString *)leaderboardErrorMessageLocalizedMessage:(NSString * _Nullable)localizedMessage isPermissionDenied:(BOOL)isPermissionDenied loadErrorText:(NSString *)loadErrorText __attribute__((swift_name("leaderboardErrorMessage(localizedMessage:isPermissionDenied:loadErrorText:)")));
- (SharedRecordLogicRowColors *)rowColorsIsCurrentUser:(BOOL)isCurrentUser __attribute__((swift_name("rowColors(isCurrentUser:)")));
- (NSString *)seasonIndexLabelBoundLeaderboardSeason:(int32_t)boundLeaderboardSeason nowUtcMs:(int64_t)nowUtcMs __attribute__((swift_name("seasonIndexLabel(boundLeaderboardSeason:nowUtcMs:)")));
- (BOOL)shouldRebindLeaderboardNewSeason:(int32_t)newSeason boundLeaderboardSeason:(int32_t)boundLeaderboardSeason __attribute__((swift_name("shouldRebindLeaderboard(newSeason:boundLeaderboardSeason:)")));
- (SharedRecordLogicLeaderboardSections *)splitLeaderboardEntries:(NSArray<SharedLessonLeaderboardRepositoryLeaderboardEntry *> *)entries __attribute__((swift_name("splitLeaderboard(entries:)")));
@property (readonly) int64_t COUNTDOWN_TICK_MS __attribute__((swift_name("COUNTDOWN_TICK_MS")));
@property (readonly) NSString *STRING_RECORD_LEADERBOARD_LOAD_ERROR __attribute__((swift_name("STRING_RECORD_LEADERBOARD_LOAD_ERROR")));
@property (readonly) NSString *STRING_RECORD_LEADERBOARD_TITLE __attribute__((swift_name("STRING_RECORD_LEADERBOARD_TITLE")));
@property (readonly) NSString *STRING_RECORD_SEASON_COUNTDOWN_DAYS_HOURS __attribute__((swift_name("STRING_RECORD_SEASON_COUNTDOWN_DAYS_HOURS")));
@property (readonly) NSString *STRING_RECORD_SEASON_COUNTDOWN_HOURS_MINUTES __attribute__((swift_name("STRING_RECORD_SEASON_COUNTDOWN_HOURS_MINUTES")));
@property (readonly) NSString *TEXT_RECORD_LEADERBOARD_LOAD_ERROR __attribute__((swift_name("TEXT_RECORD_LEADERBOARD_LOAD_ERROR")));
@property (readonly) NSString *TEXT_RECORD_LEADERBOARD_TITLE __attribute__((swift_name("TEXT_RECORD_LEADERBOARD_TITLE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordLogic.LeaderboardSections")))
@interface SharedRecordLogicLeaderboardSections : SharedBase
- (instancetype)initWithIsEmpty:(BOOL)isEmpty first:(SharedLessonLeaderboardRepositoryLeaderboardEntry * _Nullable)first rest:(NSArray<SharedLessonLeaderboardRepositoryLeaderboardEntry *> *)rest __attribute__((swift_name("init(isEmpty:first:rest:)"))) __attribute__((objc_designated_initializer));
- (SharedRecordLogicLeaderboardSections *)doCopyIsEmpty:(BOOL)isEmpty first:(SharedLessonLeaderboardRepositoryLeaderboardEntry * _Nullable)first rest:(NSArray<SharedLessonLeaderboardRepositoryLeaderboardEntry *> *)rest __attribute__((swift_name("doCopy(isEmpty:first:rest:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLessonLeaderboardRepositoryLeaderboardEntry * _Nullable first __attribute__((swift_name("first")));
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));
@property (readonly) NSArray<SharedLessonLeaderboardRepositoryLeaderboardEntry *> *rest __attribute__((swift_name("rest")));
@property (readonly) BOOL showRestList __attribute__((swift_name("showRestList")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordLogic.RowColors")))
@interface SharedRecordLogicRowColors : SharedBase
- (instancetype)initWithCardColor:(NSString *)cardColor nameColor:(NSString *)nameColor timeColor:(NSString *)timeColor __attribute__((swift_name("init(cardColor:nameColor:timeColor:)"))) __attribute__((objc_designated_initializer));
- (SharedRecordLogicRowColors *)doCopyCardColor:(NSString *)cardColor nameColor:(NSString *)nameColor timeColor:(NSString *)timeColor __attribute__((swift_name("doCopy(cardColor:nameColor:timeColor:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *cardColor __attribute__((swift_name("cardColor")));
@property (readonly) NSString *nameColor __attribute__((swift_name("nameColor")));
@property (readonly) NSString *timeColor __attribute__((swift_name("timeColor")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordLogic.SeasonCountdown")))
@interface SharedRecordLogicSeasonCountdown : SharedBase
- (instancetype)initWithStringKey:(NSString *)stringKey arg1:(int32_t)arg1 arg2:(int32_t)arg2 text:(NSString *)text __attribute__((swift_name("init(stringKey:arg1:arg2:text:)"))) __attribute__((objc_designated_initializer));
- (SharedRecordLogicSeasonCountdown *)doCopyStringKey:(NSString *)stringKey arg1:(int32_t)arg1 arg2:(int32_t)arg2 text:(NSString *)text __attribute__((swift_name("doCopy(stringKey:arg1:arg2:text:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t arg1 __attribute__((swift_name("arg1")));
@property (readonly) int32_t arg2 __attribute__((swift_name("arg2")));
@property (readonly) NSString *stringKey __attribute__((swift_name("stringKey")));
@property (readonly) NSString *text __attribute__((swift_name("text")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SeasonClock")))
@interface SharedSeasonClock : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)seasonClock __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSeasonClock *shared __attribute__((swift_name("shared")));
- (void)addSeasonChangeListenerListener:(void (^)(SharedInt *, SharedInt *))listener __attribute__((swift_name("addSeasonChangeListener(listener:)")));
- (int32_t)currentSeasonNowUtcMs:(int64_t)nowUtcMs __attribute__((swift_name("currentSeason(nowUtcMs:)")));
- (int64_t)millisUntilCurrentSeasonEndsNowUtcMs:(int64_t)nowUtcMs __attribute__((swift_name("millisUntilCurrentSeasonEnds(nowUtcMs:)")));
- (int64_t)nowUtcMs __attribute__((swift_name("nowUtcMs()")));
- (BOOL)onServerTimeServerNowUtcMs:(int64_t)serverNowUtcMs deviceNowMs:(int64_t)deviceNowMs __attribute__((swift_name("onServerTime(serverNowUtcMs:deviceNowMs:)")));
- (SharedLong * _Nullable)parseServerNowResultData:(id _Nullable)resultData __attribute__((swift_name("parseServerNow(resultData:)")));
- (BOOL)pollSeasonChangeNow:(int64_t)now __attribute__((swift_name("pollSeasonChange(now:)")));
- (void)removeSeasonChangeListenerListener:(void (^)(SharedInt *, SharedInt *))listener __attribute__((swift_name("removeSeasonChangeListener(listener:)")));
- (void)restoreOffsetSavedOffsetMs:(int64_t)savedOffsetMs __attribute__((swift_name("restoreOffset(savedOffsetMs:)")));
- (BOOL)shouldRefreshFromServerForce:(BOOL)force syncInFlight:(BOOL)syncInFlight lastSyncElapsedMs:(int64_t)lastSyncElapsedMs elapsedNowMs:(int64_t)elapsedNowMs __attribute__((swift_name("shouldRefreshFromServer(force:syncInFlight:lastSyncElapsedMs:elapsedNowMs:)")));
@property (readonly) NSString *CF_GET_SEASON_INFO __attribute__((swift_name("CF_GET_SEASON_INFO")));
@property (readonly) NSString *FIELD_SERVER_NOW_MS __attribute__((swift_name("FIELD_SERVER_NOW_MS")));
@property (readonly) NSString *KEY_OFFSET_MS __attribute__((swift_name("KEY_OFFSET_MS")));
@property (readonly) int64_t PLAUSIBLE_MAX_UTC_MS __attribute__((swift_name("PLAUSIBLE_MAX_UTC_MS")));
@property (readonly) int64_t PLAUSIBLE_MIN_UTC_MS __attribute__((swift_name("PLAUSIBLE_MIN_UTC_MS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) int64_t SEASON_ANCHOR_UTC_MS __attribute__((swift_name("SEASON_ANCHOR_UTC_MS")));
@property (readonly) int64_t SEASON_DURATION_MS __attribute__((swift_name("SEASON_DURATION_MS")));
@property (readonly) int64_t SEASON_POLL_INTERVAL_MS __attribute__((swift_name("SEASON_POLL_INTERVAL_MS")));
@property (readonly) int64_t SYNC_MIN_INTERVAL_MS __attribute__((swift_name("SYNC_MIN_INTERVAL_MS")));
@property (readonly) int64_t serverOffsetMs __attribute__((swift_name("serverOffsetMs")));
@property void (^ _Nullable serverTimeObserver)(SharedLong *) __attribute__((swift_name("serverTimeObserver")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SeasonLeaderboardRewardPayloads")))
@interface SharedSeasonLeaderboardRewardPayloads : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)seasonLeaderboardRewardPayloads __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSeasonLeaderboardRewardPayloads *shared __attribute__((swift_name("shared")));
- (NSArray<SharedBadgeLevelUpPayload *> *)buildForSeasonProgress:(SharedUserBadgeProgress *)progress season:(int32_t)season __attribute__((swift_name("buildForSeason(progress:season:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadAnimation")))
@interface SharedBeadAnimation : SharedBase
- (instancetype)initWithBeadId:(NSString *)beadId animationType:(int32_t)animationType __attribute__((swift_name("init(beadId:animationType:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedBeadAnimationCompanion *companion __attribute__((swift_name("companion")));
- (SharedBeadAnimationPlan * _Nullable)animateDistances:(SharedBeadMoveDistances *)distances __attribute__((swift_name("animate(distances:)")));
- (int32_t)getAnimationType __attribute__((swift_name("getAnimationType()")));
- (NSString *)getBeadId __attribute__((swift_name("getBeadId()")));
- (BOOL)isAnimating __attribute__((swift_name("isAnimating()")));
- (void)onAnimationEnd __attribute__((swift_name("onAnimationEnd()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadAnimation.Companion")))
@interface SharedBeadAnimationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBeadAnimationCompanion *shared __attribute__((swift_name("shared")));
- (SharedBeadAnimationPlan * _Nullable)planForAnimationType:(int32_t)animationType distances:(SharedBeadMoveDistances *)distances __attribute__((swift_name("planFor(animationType:distances:)")));
@property (readonly) int64_t ANIMATION_DURATION_MS __attribute__((swift_name("ANIMATION_DURATION_MS")));
@property (readonly) NSString *DRAWABLE_BEAD __attribute__((swift_name("DRAWABLE_BEAD")));
@property (readonly) NSString *DRAWABLE_BEAD_SELECTED __attribute__((swift_name("DRAWABLE_BEAD_SELECTED")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadAnimationPlan")))
@interface SharedBeadAnimationPlan : SharedBase
- (instancetype)initWithDrawableKey:(NSString *)drawableKey translationY:(float)translationY bottomMarginDelta:(int32_t)bottomMarginDelta resetTranslationAtEnd:(BOOL)resetTranslationAtEnd durationMs:(int64_t)durationMs __attribute__((swift_name("init(drawableKey:translationY:bottomMarginDelta:resetTranslationAtEnd:durationMs:)"))) __attribute__((objc_designated_initializer));
- (SharedBeadAnimationPlan *)doCopyDrawableKey:(NSString *)drawableKey translationY:(float)translationY bottomMarginDelta:(int32_t)bottomMarginDelta resetTranslationAtEnd:(BOOL)resetTranslationAtEnd durationMs:(int64_t)durationMs __attribute__((swift_name("doCopy(drawableKey:translationY:bottomMarginDelta:resetTranslationAtEnd:durationMs:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t bottomMarginDelta __attribute__((swift_name("bottomMarginDelta")));
@property (readonly) NSString *drawableKey __attribute__((swift_name("drawableKey")));
@property (readonly) int64_t durationMs __attribute__((swift_name("durationMs")));
@property (readonly) BOOL resetTranslationAtEnd __attribute__((swift_name("resetTranslationAtEnd")));
@property (readonly) float translationY __attribute__((swift_name("translationY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadMoveDistances")))
@interface SharedBeadMoveDistances : SharedBase
- (instancetype)initWithBottomPx:(float)bottomPx topPx:(float)topPx __attribute__((swift_name("init(bottomPx:topPx:)"))) __attribute__((objc_designated_initializer));
- (SharedBeadMoveDistances *)doCopyBottomPx:(float)bottomPx topPx:(float)topPx __attribute__((swift_name("doCopy(bottomPx:topPx:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float bottomPx __attribute__((swift_name("bottomPx")));
@property (readonly) float topPx __attribute__((swift_name("topPx")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BeadsBehavior")))
@interface SharedBeadsBehavior : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)beadsBehavior __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBeadsBehavior *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GlobalValues")))
@interface SharedGlobalValues : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)globalValues __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedGlobalValues *shared __attribute__((swift_name("shared")));
- (int32_t)generateRandomNumberDigitCount:(int32_t)digitCount __attribute__((swift_name("generateRandomNumber(digitCount:)")));
- (NSString *)randomNumberChangeToStringDigitCount:(int32_t)digitCount __attribute__((swift_name("randomNumberChangeToString(digitCount:)")));
- (NSArray<NSString *> *)randomUniqueNumberStringsDigitCount:(int32_t)digitCount count:(int32_t)count __attribute__((swift_name("randomUniqueNumberStrings(digitCount:count:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("JavaRandom")))
@interface SharedJavaRandom : SharedBase
- (instancetype)initWithSeed:(int64_t)seed __attribute__((swift_name("init(seed:)"))) __attribute__((objc_designated_initializer));
- (BOOL)nextBoolean __attribute__((swift_name("nextBoolean()")));
- (double)nextDouble __attribute__((swift_name("nextDouble()")));
- (float)nextFloat __attribute__((swift_name("nextFloat()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
- (int32_t)nextIntBound:(int32_t)bound __attribute__((swift_name("nextInt(bound:)")));
- (int64_t)nextLong __attribute__((swift_name("nextLong()")));
- (void)setSeedSeed:(int64_t)seed __attribute__((swift_name("setSeed(seed:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MathOperation_")))
@interface SharedMathOperation_ : SharedBase
- (instancetype)initWithFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("init(firstNumber:operator:secondNumber:)"))) __attribute__((objc_designated_initializer));
- (SharedMathOperation_ *)doCopyFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("doCopy(firstNumber:operator:secondNumber:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable firstNumber __attribute__((swift_name("firstNumber")));
@property (readonly, getter=operator) NSString * _Nullable operator_ __attribute__((swift_name("operator_")));
@property (readonly) SharedInt * _Nullable secondNumber __attribute__((swift_name("secondNumber")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MathOperationGenerator")))
@interface SharedMathOperationGenerator : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mathOperationGenerator __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMathOperationGenerator *shared __attribute__((swift_name("shared")));
- (SharedMathOperation_ *)extractionBeadRules __attribute__((swift_name("extractionBeadRules()")));
- (SharedMathOperation_ *)extractionBeadRulesFourThree __attribute__((swift_name("extractionBeadRulesFourThree()")));
- (NSArray<SharedMathOperation_ *> *)extractionBeadRulesListCount:(int32_t)count __attribute__((swift_name("extractionBeadRulesList(count:)")));
- (SharedMathOperation_ *)extractionBeadRulesThreeTwo __attribute__((swift_name("extractionBeadRulesThreeTwo()")));
- (SharedMathOperation_ *)extractionGenerateMathOperation __attribute__((swift_name("extractionGenerateMathOperation()")));
- (NSArray<SharedMathOperation_ *> *)extractionGenerateMathOperationListCount:(int32_t)count __attribute__((swift_name("extractionGenerateMathOperationList(count:)")));
- (SharedMathOperation_ *)extractionGenerateMathOperationTen __attribute__((swift_name("extractionGenerateMathOperationTen()")));
- (SharedMathOperation_ *)extractionGenerateMathOperationTenWithOtherRules __attribute__((swift_name("extractionGenerateMathOperationTenWithOtherRules()")));
- (SharedMathOperation_ *)extractionGenerateMathOperationTenWithOtherRulesExtreme __attribute__((swift_name("extractionGenerateMathOperationTenWithOtherRulesExtreme()")));
- (SharedMathOperation_ *)extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton __attribute__((swift_name("extractionGenerateMathOperationTenWithOtherRulesExtremeForMaraton()")));
- (NSArray<SharedInt *> *)generalCollectionFourDigitsCount:(int32_t)count __attribute__((swift_name("generalCollectionFourDigits(count:)")));
- (NSArray<SharedInt *> *)generalCollectionOneDigitsCount:(int32_t)count __attribute__((swift_name("generalCollectionOneDigits(count:)")));
- (NSArray<SharedInt *> *)generalCollectionThreeDigitsCount:(int32_t)count __attribute__((swift_name("generalCollectionThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generalCollectionTwoDigitsCount:(int32_t)count __attribute__((swift_name("generalCollectionTwoDigits(count:)")));
- (SharedMathOperation_ *)generateMathOperation __attribute__((swift_name("generateMathOperation()")));
- (SharedMathOperation_ *)generateMathOperation2 __attribute__((swift_name("generateMathOperation2()")));
- (SharedMathOperation_ *)generateMathOperation3 __attribute__((swift_name("generateMathOperation3()")));
- (SharedMathOperation_ *)generateMathOperation4 __attribute__((swift_name("generateMathOperation4()")));
- (SharedMathOperation_ *)generateMathOperationBeadRule __attribute__((swift_name("generateMathOperationBeadRule()")));
- (NSArray<SharedMathOperation_ *> *)generateMathOperationListCount:(int32_t)count __attribute__((swift_name("generateMathOperationList(count:)")));
- (NSArray<SharedMathOperation_ *> *)generateMathOperationList3Count:(int32_t)count __attribute__((swift_name("generateMathOperationList3(count:)")));
- (NSArray<SharedMathOperation_ *> *)generateMathOperationList4Count:(int32_t)count __attribute__((swift_name("generateMathOperationList4(count:)")));
- (SharedMathOperation_ *)generateMathOperationWithDigitsFirstDigitCount:(int32_t)firstDigitCount secondDigitCount:(int32_t)secondDigitCount __attribute__((swift_name("generateMathOperationWithDigits(firstDigitCount:secondDigitCount:)")));
- (SharedMathOperation_ *)generateMathOperationWithDigits2FirstNumberDigits:(int32_t)firstNumberDigits secondNumberDigits:(int32_t)secondNumberDigits __attribute__((swift_name("generateMathOperationWithDigits2(firstNumberDigits:secondNumberDigits:)")));
- (SharedMathOperation_ *)generateMathOperationWithDigits3FirstNumberDigits:(int32_t)firstNumberDigits secondNumberDigits:(int32_t)secondNumberDigits __attribute__((swift_name("generateMathOperationWithDigits3(firstNumberDigits:secondNumberDigits:)")));
- (SharedMathOperation_ *)generateMathOperationWithDigitsBeadRuleFirstNumberDigits:(int32_t)firstNumberDigits secondNumberDigits:(int32_t)secondNumberDigits __attribute__((swift_name("generateMathOperationWithDigitsBeadRule(firstNumberDigits:secondNumberDigits:)")));
- (SharedMathOperation_ *)generateRandomMathOperation1 __attribute__((swift_name("generateRandomMathOperation1()")));
- (SharedMathOperation_ *)generateRelatedNumbersFirstDigitCount:(int32_t)firstDigitCount secondDigitCount:(int32_t)secondDigitCount __attribute__((swift_name("generateRelatedNumbers(firstDigitCount:secondDigitCount:)")));
- (SharedMathOperation_ *)generateRelatedNumbers0FirstDigitCount:(int32_t)firstDigitCount secondDigitCount:(int32_t)secondDigitCount __attribute__((swift_name("generateRelatedNumbers0(firstDigitCount:secondDigitCount:)")));
- (SharedMathOperation_ *)generateRelatedNumbers2FirstDigitCount:(int32_t)firstDigitCount secondDigitCount:(int32_t)secondDigitCount __attribute__((swift_name("generateRelatedNumbers2(firstDigitCount:secondDigitCount:)")));
- (NSArray<SharedInt *> *)generateRelatedNumbers2BlindingCount:(int32_t)count digitsOne:(int32_t)digitsOne __attribute__((swift_name("generateRelatedNumbers2Blinding(count:digitsOne:)")));
- (NSArray<SharedMathOperation_ *> *)generateRelatedNumbersListCount:(int32_t)count firstDigitCount:(int32_t)firstDigitCount secondDigitCount:(int32_t)secondDigitCount __attribute__((swift_name("generateRelatedNumbersList(count:firstDigitCount:secondDigitCount:)")));
- (NSArray<SharedInt *> *)generateSequence1Count:(int32_t)count __attribute__((swift_name("generateSequence1(count:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyNewCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyNew(count:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyOldCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyOld(count:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyOldOneCountCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyOldOneCount(count:)")));
- (NSArray<SharedInt *> *)generateSequence10RulesEasyOneCountCount:(int32_t)count __attribute__((swift_name("generateSequence10RulesEasyOneCount(count:)")));
- (NSArray<SharedInt *> *)generateSequence1DigitsCount:(int32_t)count digitsOne:(int32_t)digitsOne __attribute__((swift_name("generateSequence1Digits(count:digitsOne:)")));
- (NSArray<SharedInt *> *)generateSequence5RulesNewCount:(int32_t)count __attribute__((swift_name("generateSequence5RulesNew(count:)")));
- (NSArray<SharedInt *> *)generateSequenceBeadRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceBeadRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionCount:(int32_t)count __attribute__((swift_name("generateSequenceExtraction(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionBeadRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionBeadRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionBeadRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionBeadRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionFiveRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionFiveRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionFiveRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionFiveRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRace(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceFourDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRaceFourDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceHardCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRaceHard(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRaceThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceThreeDigitsHardCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRaceThreeDigitsHard(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceTwoDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRaceTwoDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionRaceTwoDigitsHardCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionRaceTwoDigitsHard(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionTenRulesCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionTenRules(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionTenRulesThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionTenRulesThreeDigits(count:)")));
- (NSArray<SharedInt *> *)generateSequenceExtractionThreeDigitsCount:(int32_t)count __attribute__((swift_name("generateSequenceExtractionThreeDigits(count:)")));
- (SharedMathOperation_ *)irregularExtractionFirstNumberDigits:(int32_t)firstNumberDigits secondNumberDigits:(int32_t)secondNumberDigits __attribute__((swift_name("irregularExtraction(firstNumberDigits:secondNumberDigits:)")));
- (SharedMathOperation_ *)irregularExtractionFiveRulesFirstNumberDigits:(int32_t)firstNumberDigits secondNumberDigits:(int32_t)secondNumberDigits __attribute__((swift_name("irregularExtractionFiveRules(firstNumberDigits:secondNumberDigits:)")));
- (SharedMathOperation_ *)irregularExtractionFiveRulesMixFirstNumberDigits:(int32_t)firstNumberDigits secondNumberDigits:(int32_t)secondNumberDigits __attribute__((swift_name("irregularExtractionFiveRulesMix(firstNumberDigits:secondNumberDigits:)")));
- (SharedMathOperation_ *)multiplicationFull __attribute__((swift_name("multiplicationFull()")));
- (SharedMathOperation_ *)multiplicationLessFive __attribute__((swift_name("multiplicationLessFive()")));
- (SharedMathOperation_ *)multiplicationLessFiveFull __attribute__((swift_name("multiplicationLessFiveFull()")));
- (SharedMathOperation_ *)multiplicationThreeFull __attribute__((swift_name("multiplicationThreeFull()")));
- (SharedMathOperation_ *)multiplicationThreeTwoFive __attribute__((swift_name("multiplicationThreeTwoFive()")));
- (SharedMathOperation_ *)multiplicationThreeTwoFull __attribute__((swift_name("multiplicationThreeTwoFull()")));
- (SharedMathOperation_ *)multiplicationTwo __attribute__((swift_name("multiplicationTwo()")));
- (SharedMathOperation_ *)multiplicationTwoFull __attribute__((swift_name("multiplicationTwoFull()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RastgeleKaynagi")))
@interface SharedRastgeleKaynagi : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)rastgeleKaynagi __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRastgeleKaynagi *shared __attribute__((swift_name("shared")));
- (void)varsayilanaDon __attribute__((swift_name("varsayilanaDon()")));
- (SharedJavaRandom *)yeniJavaRandom __attribute__((swift_name("yeniJavaRandom()")));
@property SharedKotlinRandom *rastgele __attribute__((swift_name("rastgele")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecordTimeUtils")))
@interface SharedRecordTimeUtils : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)recordTimeUtils __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRecordTimeUtils *shared __attribute__((swift_name("shared")));
- (NSString *)formatScoreScore:(SharedInt * _Nullable)score __attribute__((swift_name("formatScore(score:)")));
- (SharedInt * _Nullable)parseRecordToScoreRecord:(NSString *)record __attribute__((swift_name("parseRecordToScore(record:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccountDeletionRules")))
@interface SharedAccountDeletionRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)accountDeletionRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccountDeletionRules *shared __attribute__((swift_name("shared")));
- (NSString *)followerRefPathFollowingUid:(NSString *)followingUid myUid:(NSString *)myUid __attribute__((swift_name("followerRefPath(followingUid:myUid:)")));
- (NSString *)followingRefPathFollowerUid:(NSString *)followerUid myUid:(NSString *)myUid __attribute__((swift_name("followingRefPath(followerUid:myUid:)")));
- (BOOL)requiresRecentLoginLastSignInTimestampMs:(SharedLong * _Nullable)lastSignInTimestampMs nowMs:(int64_t)nowMs __attribute__((swift_name("requiresRecentLogin(lastSignInTimestampMs:nowMs:)")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) int32_t DELETE_BATCH_SIZE __attribute__((swift_name("DELETE_BATCH_SIZE")));
@property (readonly) NSArray<NSString *> *LEAF_SUBCOLLECTIONS __attribute__((swift_name("LEAF_SUBCOLLECTIONS")));
@property (readonly) NSString *MESSAGE_USER_NOT_FOUND __attribute__((swift_name("MESSAGE_USER_NOT_FOUND")));
@property (readonly) int64_t RECENT_LOGIN_WINDOW_MS __attribute__((swift_name("RECENT_LOGIN_WINDOW_MS")));
@property (readonly) NSArray<SharedKotlinPair<NSString *, NSString *> *> *SINGLE_DOC_SUBCOLLECTIONS __attribute__((swift_name("SINGLE_DOC_SUBCOLLECTIONS")));
@property (readonly) NSArray<NSString *> *STEP_ORDER __attribute__((swift_name("STEP_ORDER")));
@property (readonly) NSString *SUBCOLLECTION_FOLLOWERS __attribute__((swift_name("SUBCOLLECTION_FOLLOWERS")));
@property (readonly) NSString *SUBCOLLECTION_FOLLOWING __attribute__((swift_name("SUBCOLLECTION_FOLLOWING")));
@property (readonly) NSString *SUBCOLLECTION_LESSON_PROGRESS __attribute__((swift_name("SUBCOLLECTION_LESSON_PROGRESS")));
@property (readonly) NSString *SUBCOLLECTION_LESSON_PROGRESS_ITEMS __attribute__((swift_name("SUBCOLLECTION_LESSON_PROGRESS_ITEMS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthRules")))
@interface SharedAuthRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)authRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAuthRules *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, NSString *> *)cacheBasicUserEmail:(NSString *)email role:(NSString *)role name:(NSString *)name userId:(NSString *)userId __attribute__((swift_name("cacheBasicUser(email:role:name:userId:)")));
- (NSDictionary<NSString *, id> *)checkEmailRegisteredPayloadEmail:(NSString *)email __attribute__((swift_name("checkEmailRegisteredPayload(email:)")));
- (NSDictionary<NSString *, id> *)confirmParentalConsentPayloadCode:(NSString *)code firstTutorialShown:(BOOL)firstTutorialShown __attribute__((swift_name("confirmParentalConsentPayload(code:firstTutorialShown:)")));
- (NSDictionary<NSString *, id> *)emailStudentUserDataUid:(NSString *)uid userId:(NSString *)userId email:(NSString *)email name:(NSString *)name firstTutorialShown:(BOOL)firstTutorialShown nowMs:(int64_t)nowMs walletFields:(NSDictionary<NSString *, id> *)walletFields __attribute__((swift_name("emailStudentUserData(uid:userId:email:name:firstTutorialShown:nowMs:walletFields:)")));
- (NSDictionary<NSString *, id> * _Nullable)findTeacherEmailByUserIdPayloadUserId:(NSString *)userId __attribute__((swift_name("findTeacherEmailByUserIdPayload(userId:)")));
- (NSString *)generateFallbackUserIdNowMs:(int64_t)nowMs random:(SharedKotlinRandom *)random __attribute__((swift_name("generateFallbackUserId(nowMs:random:)")));
- (NSString *)generateUserIdCandidateRandom:(SharedKotlinRandom *)random __attribute__((swift_name("generateUserIdCandidate(random:)")));
- (NSString *)googleApiExceptionMessageStatusCode:(int32_t)statusCode detail:(NSString * _Nullable)detail __attribute__((swift_name("googleApiExceptionMessage(statusCode:detail:)")));
- (NSString *)googleFirebaseAuthErrorMessageErrorCode:(NSString * _Nullable)errorCode errorMessageFull:(NSString *)errorMessageFull __attribute__((swift_name("googleFirebaseAuthErrorMessage(errorCode:errorMessageFull:)")));
- (SharedAuthRulesGoogleLoginFailureAction *)googleLoginFailureActionError:(NSString * _Nullable)error __attribute__((swift_name("googleLoginFailureAction(error:)")));
- (NSDictionary<NSString *, id> *)googleStudentUserDataUid:(NSString *)uid userId:(NSString *)userId email:(NSString * _Nullable)email displayName:(NSString * _Nullable)displayName firstTutorialShown:(BOOL)firstTutorialShown nowMs:(int64_t)nowMs birthYear:(SharedInt * _Nullable)birthYear acquisitionSource:(NSString * _Nullable)acquisitionSource walletFields:(NSDictionary<NSString *, id> *)walletFields __attribute__((swift_name("googleStudentUserData(uid:userId:email:displayName:firstTutorialShown:nowMs:birthYear:acquisitionSource:walletFields:)")));
- (BOOL)isGoogleLoginAllowedRoleField:(NSString * _Nullable)roleField __attribute__((swift_name("isGoogleLoginAllowed(roleField:)")));
- (BOOL)isOtpLoginAllowedRoleField:(NSString * _Nullable)roleField __attribute__((swift_name("isOtpLoginAllowed(roleField:)")));
- (BOOL)isTeacherInviteValidExists:(BOOL)exists used:(SharedBoolean * _Nullable)used expiresAtMs:(SharedLong * _Nullable)expiresAtMs nowMs:(int64_t)nowMs __attribute__((swift_name("isTeacherInviteValid(exists:used:expiresAtMs:nowMs:)")));
- (BOOL)isValidEmailEmail:(NSString *)email __attribute__((swift_name("isValidEmail(email:)")));
- (BOOL)isValidPendingTeacherRegistrationEmail:(NSString *)email name:(NSString *)name password:(NSString *)password __attribute__((swift_name("isValidPendingTeacherRegistration(email:name:password:)")));
- (NSString *)normalizeEmailEmail:(NSString *)email __attribute__((swift_name("normalizeEmail(email:)")));
- (NSString *)otpLoginRoleRoleField:(NSString * _Nullable)roleField __attribute__((swift_name("otpLoginRole(roleField:)")));
- (SharedKotlinTriple<SharedBoolean *, NSString *, NSString *> *)parseEmailRegisteredResultData:(NSDictionary<id, id> * _Nullable)data __attribute__((swift_name("parseEmailRegisteredResult(data:)")));
- (NSString * _Nullable)parseTokenData:(NSDictionary<id, id> * _Nullable)data __attribute__((swift_name("parseToken(data:)")));
- (NSString *)registerStudentErrorMessageExceptionMessage:(NSString * _Nullable)exceptionMessage localizedMessage:(NSString * _Nullable)localizedMessage __attribute__((swift_name("registerStudentErrorMessage(exceptionMessage:localizedMessage:)")));
- (NSDictionary<NSString *, id> *)requestGoogleParentalConsentPayloadChildEmail:(NSString *)childEmail parentEmail:(NSString *)parentEmail birthYear:(int32_t)birthYear acquisitionSource:(NSString * _Nullable)acquisitionSource lang:(NSString *)lang __attribute__((swift_name("requestGoogleParentalConsentPayload(childEmail:parentEmail:birthYear:acquisitionSource:lang:)")));
- (BOOL)requiresParentalConsentBirthYear:(SharedInt * _Nullable)birthYear utcYear:(int32_t)utcYear __attribute__((swift_name("requiresParentalConsent(birthYear:utcYear:)")));
- (NSString *)resendPurposeIsRegistration:(BOOL)isRegistration __attribute__((swift_name("resendPurpose(isRegistration:)")));
- (NSDictionary<NSString *, id> *)resetTeacherPasswordPayloadEmail:(NSString *)email code:(NSString *)code newPassword:(NSString *)newPassword __attribute__((swift_name("resetTeacherPasswordPayload(email:code:newPassword:)")));
- (NSDictionary<NSString *, id> *)sendParentalRegistrationCodePayloadChildEmail:(NSString *)childEmail parentEmail:(NSString *)parentEmail birthYear:(int32_t)birthYear acquisitionSource:(NSString * _Nullable)acquisitionSource lang:(NSString *)lang __attribute__((swift_name("sendParentalRegistrationCodePayload(childEmail:parentEmail:birthYear:acquisitionSource:lang:)")));
- (NSDictionary<NSString *, id> *)sendVerificationCodePayloadEmail:(NSString *)email purpose:(NSString *)purpose __attribute__((swift_name("sendVerificationCodePayload(email:purpose:)")));
- (BOOL)skipStepTutorialShownExists:(BOOL)exists data:(NSDictionary<NSString *, id> * _Nullable)data __attribute__((swift_name("skipStepTutorialShown(exists:data:)")));
- (SharedAuthRulesStudentEmailFlow *)studentEmailFlowAlreadyRegistered:(BOOL)alreadyRegistered uid:(NSString * _Nullable)uid role:(NSString * _Nullable)role __attribute__((swift_name("studentEmailFlow(alreadyRegistered:uid:role:)")));
- (NSString *)teacherEmailAlreadyRegisteredMessageRole:(NSString * _Nullable)role __attribute__((swift_name("teacherEmailAlreadyRegisteredMessage(role:)")));
- (NSDictionary<NSString *, id> *)teacherInviteUsedUpdateUid:(NSString *)uid nowMs:(int64_t)nowMs __attribute__((swift_name("teacherInviteUsedUpdate(uid:nowMs:)")));
- (NSDictionary<NSString *, id> *)teacherUserDataUid:(NSString *)uid userId:(NSString *)userId email:(NSString *)email name:(NSString *)name firstTutorialShown:(BOOL)firstTutorialShown nowMs:(int64_t)nowMs walletFields:(NSDictionary<NSString *, id> *)walletFields __attribute__((swift_name("teacherUserData(uid:userId:email:name:firstTutorialShown:nowMs:walletFields:)")));
- (NSString * _Nullable)validateLoginEmailRawEmail:(NSString * _Nullable)rawEmail __attribute__((swift_name("validateLoginEmail(rawEmail:)")));
- (NSString * _Nullable)validateNameName:(NSString *)name __attribute__((swift_name("validateName(name:)")));
- (NSString * _Nullable)validateNewPasswordNewPassword:(NSString *)newPassword __attribute__((swift_name("validateNewPassword(newPassword:)")));
- (NSString * _Nullable)validateRegisterEmailRawEmail:(NSString * _Nullable)rawEmail __attribute__((swift_name("validateRegisterEmail(rawEmail:)")));
- (NSString * _Nullable)validateTeacherRegistrationRawName:(NSString *)rawName rawEmail:(NSString *)rawEmail password:(NSString *)password passwordConfirm:(NSString *)passwordConfirm __attribute__((swift_name("validateTeacherRegistration(rawName:rawEmail:password:passwordConfirm:)")));
- (NSDictionary<NSString *, id> *)verifyChildEmailForParentalConsentPayloadEmail:(NSString *)email code:(NSString *)code birthYear:(int32_t)birthYear __attribute__((swift_name("verifyChildEmailForParentalConsentPayload(email:code:birthYear:)")));
- (NSDictionary<NSString *, id> *)verifyLoginCodePayloadEmail:(NSString *)email code:(NSString *)code __attribute__((swift_name("verifyLoginCodePayload(email:code:)")));
- (NSDictionary<NSString *, id> *)verifyRegistrationCodePayloadEmail:(NSString *)email code:(NSString *)code pending:(SharedAuthRulesPendingRegistration * _Nullable)pending birthYear:(SharedInt * _Nullable)birthYear acquisitionSource:(NSString * _Nullable)acquisitionSource __attribute__((swift_name("verifyRegistrationCodePayload(email:code:pending:birthYear:acquisitionSource:)")));
- (NSDictionary<NSString *, id> *)verifyTeacherPasswordResetCodePayloadEmail:(NSString *)email code:(NSString *)code __attribute__((swift_name("verifyTeacherPasswordResetCodePayload(email:code:)")));
@property (readonly) NSString *APP_PREFS_NAME __attribute__((swift_name("APP_PREFS_NAME")));
@property (readonly) NSString *COLLECTION_TEACHER_INVITES __attribute__((swift_name("COLLECTION_TEACHER_INVITES")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *ERROR_ACCOUNT_NOT_REGISTERED __attribute__((swift_name("ERROR_ACCOUNT_NOT_REGISTERED")));
@property (readonly) NSString *ERROR_CHILD_EMAIL_NOT_VERIFIED __attribute__((swift_name("ERROR_CHILD_EMAIL_NOT_VERIFIED")));
@property (readonly) NSString *ERROR_PARENTAL_CONSENT_REQUIRED __attribute__((swift_name("ERROR_PARENTAL_CONSENT_REQUIRED")));
@property (readonly) NSString *FIELD_ACQUISITION_SOURCE __attribute__((swift_name("FIELD_ACQUISITION_SOURCE")));
@property (readonly) NSString *FIELD_BIRTH_YEAR __attribute__((swift_name("FIELD_BIRTH_YEAR")));
@property (readonly) NSString *FIELD_CREATED_AT __attribute__((swift_name("FIELD_CREATED_AT")));
@property (readonly) NSString *FIELD_EMAIL __attribute__((swift_name("FIELD_EMAIL")));
@property (readonly) NSString *FIELD_FIRST_TUTORIAL_SHOWN __attribute__((swift_name("FIELD_FIRST_TUTORIAL_SHOWN")));
@property (readonly) NSString *FIELD_NAME __attribute__((swift_name("FIELD_NAME")));
@property (readonly) NSString *FIELD_ROLE __attribute__((swift_name("FIELD_ROLE")));
@property (readonly) NSString *FIELD_SKIP_STEP_TUTORIAL_SHOWN __attribute__((swift_name("FIELD_SKIP_STEP_TUTORIAL_SHOWN")));
@property (readonly) NSString *FIELD_TEACHER_APPROVED __attribute__((swift_name("FIELD_TEACHER_APPROVED")));
@property (readonly) NSString *FIELD_UID __attribute__((swift_name("FIELD_UID")));
@property (readonly) NSString *FIELD_USER_ID __attribute__((swift_name("FIELD_USER_ID")));
@property (readonly) NSString *FIELD_VERIFIED __attribute__((swift_name("FIELD_VERIFIED")));
@property (readonly) NSString *FN_CHECK_EMAIL_REGISTERED __attribute__((swift_name("FN_CHECK_EMAIL_REGISTERED")));
@property (readonly) NSString *FN_CONFIRM_PARENTAL_CONSENT __attribute__((swift_name("FN_CONFIRM_PARENTAL_CONSENT")));
@property (readonly) NSString *FN_CREATE_TEACHER_INVITE __attribute__((swift_name("FN_CREATE_TEACHER_INVITE")));
@property (readonly) NSString *FN_FIND_TEACHER_EMAIL_BY_USER_ID __attribute__((swift_name("FN_FIND_TEACHER_EMAIL_BY_USER_ID")));
@property (readonly) NSString *FN_RESET_TEACHER_PASSWORD __attribute__((swift_name("FN_RESET_TEACHER_PASSWORD")));
@property (readonly) NSString *FN_SEND_STUDENT_VERIFICATION_CODE __attribute__((swift_name("FN_SEND_STUDENT_VERIFICATION_CODE")));
@property (readonly) NSString *FN_VERIFY_LOGIN_CODE __attribute__((swift_name("FN_VERIFY_LOGIN_CODE")));
@property (readonly) NSString *FN_VERIFY_REGISTRATION_CODE __attribute__((swift_name("FN_VERIFY_REGISTRATION_CODE")));
@property (readonly) NSString *FN_VERIFY_TEACHER_PASSWORD_RESET_CODE __attribute__((swift_name("FN_VERIFY_TEACHER_PASSWORD_RESET_CODE")));
@property (readonly) int32_t GOOGLE_STATUS_CANCELLED __attribute__((swift_name("GOOGLE_STATUS_CANCELLED")));
@property (readonly) NSString *INVITE_FIELD_EXPIRES_AT __attribute__((swift_name("INVITE_FIELD_EXPIRES_AT")));
@property (readonly) NSString *INVITE_FIELD_USED __attribute__((swift_name("INVITE_FIELD_USED")));
@property (readonly) NSString *INVITE_FIELD_USED_AT __attribute__((swift_name("INVITE_FIELD_USED_AT")));
@property (readonly) NSString *INVITE_FIELD_USED_BY_UID __attribute__((swift_name("INVITE_FIELD_USED_BY_UID")));
@property (readonly) int32_t MAX_AGE_DIFF_NEEDING_CONSENT __attribute__((swift_name("MAX_AGE_DIFF_NEEDING_CONSENT")));
@property (readonly) int32_t MAX_NAME_LENGTH __attribute__((swift_name("MAX_NAME_LENGTH")));
@property (readonly) NSString *MESSAGE_CODE_NOT_SENT __attribute__((swift_name("MESSAGE_CODE_NOT_SENT")));
@property (readonly) NSString *MESSAGE_CODE_NOT_VERIFIED __attribute__((swift_name("MESSAGE_CODE_NOT_VERIFIED")));
@property (readonly) NSString *MESSAGE_CREDENTIALS_NOT_VERIFIED __attribute__((swift_name("MESSAGE_CREDENTIALS_NOT_VERIFIED")));
@property (readonly) NSString *MESSAGE_EMAIL_VERIFICATION_REQUIRED __attribute__((swift_name("MESSAGE_EMAIL_VERIFICATION_REQUIRED")));
@property (readonly) NSString *MESSAGE_ENTER_EMAIL __attribute__((swift_name("MESSAGE_ENTER_EMAIL")));
@property (readonly) NSString *MESSAGE_GOOGLE_CANCELLED __attribute__((swift_name("MESSAGE_GOOGLE_CANCELLED")));
@property (readonly) NSString *MESSAGE_GOOGLE_NOT_SELECTED __attribute__((swift_name("MESSAGE_GOOGLE_NOT_SELECTED")));
@property (readonly) NSString *MESSAGE_GOOGLE_NO_TOKEN __attribute__((swift_name("MESSAGE_GOOGLE_NO_TOKEN")));
@property (readonly) NSString *MESSAGE_INVALID_INFO __attribute__((swift_name("MESSAGE_INVALID_INFO")));
@property (readonly) NSString *MESSAGE_INVALID_INVITE __attribute__((swift_name("MESSAGE_INVALID_INVITE")));
@property (readonly) NSString *MESSAGE_NOT_STUDENT __attribute__((swift_name("MESSAGE_NOT_STUDENT")));
@property (readonly) NSString *MESSAGE_NOT_TEACHER __attribute__((swift_name("MESSAGE_NOT_TEACHER")));
@property (readonly) NSString *MESSAGE_OPERATION_FAILED __attribute__((swift_name("MESSAGE_OPERATION_FAILED")));
@property (readonly) NSString *MESSAGE_PASSWORD_NOT_UPDATED __attribute__((swift_name("MESSAGE_PASSWORD_NOT_UPDATED")));
@property (readonly) NSString *MESSAGE_SESSION_NOT_OPENED __attribute__((swift_name("MESSAGE_SESSION_NOT_OPENED")));
@property (readonly) NSString *MESSAGE_TEACHER_EMAIL_FOR_STUDENT __attribute__((swift_name("MESSAGE_TEACHER_EMAIL_FOR_STUDENT")));
@property (readonly) NSString *MESSAGE_TEACHER_ON_STUDENT_LOGIN __attribute__((swift_name("MESSAGE_TEACHER_ON_STUDENT_LOGIN")));
@property (readonly) NSString *MESSAGE_TOKEN_MISSING __attribute__((swift_name("MESSAGE_TOKEN_MISSING")));
@property (readonly) NSString *MESSAGE_USER_INFO_UNAVAILABLE __attribute__((swift_name("MESSAGE_USER_INFO_UNAVAILABLE")));
@property (readonly) NSString *MESSAGE_USER_NOT_CREATED __attribute__((swift_name("MESSAGE_USER_NOT_CREATED")));
@property (readonly) NSString *MESSAGE_USER_NOT_FOUND __attribute__((swift_name("MESSAGE_USER_NOT_FOUND")));
@property (readonly) NSString *MESSAGE_WRONG_PASSWORD_OR_EMAIL __attribute__((swift_name("MESSAGE_WRONG_PASSWORD_OR_EMAIL")));
@property (readonly) int32_t MIN_PASSWORD_LENGTH __attribute__((swift_name("MIN_PASSWORD_LENGTH")));
@property (readonly) NSString *OTP_PURPOSE_LOGIN __attribute__((swift_name("OTP_PURPOSE_LOGIN")));
@property (readonly) NSString *OTP_PURPOSE_REGISTER __attribute__((swift_name("OTP_PURPOSE_REGISTER")));
@property (readonly) NSString *OTP_PURPOSE_REGISTER_PARENTAL __attribute__((swift_name("OTP_PURPOSE_REGISTER_PARENTAL")));
@property (readonly) NSString *OTP_PURPOSE_TEACHER_RESET __attribute__((swift_name("OTP_PURPOSE_TEACHER_RESET")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) NSString *PREF_FIRST_TUTORIAL_SHOWN __attribute__((swift_name("PREF_FIRST_TUTORIAL_SHOWN")));
@property (readonly) NSString *PREF_USER_EMAIL __attribute__((swift_name("PREF_USER_EMAIL")));
@property (readonly) NSString *PREF_USER_ID __attribute__((swift_name("PREF_USER_ID")));
@property (readonly) NSString *PREF_USER_NAME __attribute__((swift_name("PREF_USER_NAME")));
@property (readonly) NSString *PREF_USER_TYPE __attribute__((swift_name("PREF_USER_TYPE")));
@property (readonly) NSString *PROVIDER_GOOGLE __attribute__((swift_name("PROVIDER_GOOGLE")));
@property (readonly) NSString *PROVIDER_PASSWORD __attribute__((swift_name("PROVIDER_PASSWORD")));
@property (readonly) NSString *ROLE_STUDENT __attribute__((swift_name("ROLE_STUDENT")));
@property (readonly) NSString *ROLE_TEACHER __attribute__((swift_name("ROLE_TEACHER")));
@property (readonly) NSString *SERVER_REASON_CHILD_EMAIL_UNVERIFIED __attribute__((swift_name("SERVER_REASON_CHILD_EMAIL_UNVERIFIED")));
@property (readonly) NSString *USER_ID_LETTERS __attribute__((swift_name("USER_ID_LETTERS")));
@property (readonly) int32_t USER_ID_MAX_ATTEMPTS __attribute__((swift_name("USER_ID_MAX_ATTEMPTS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthRules.GoogleLoginFailureAction")))
@interface SharedAuthRulesGoogleLoginFailureAction : SharedKotlinEnum<SharedAuthRulesGoogleLoginFailureAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAuthRulesGoogleLoginFailureAction *showUserInfo __attribute__((swift_name("showUserInfo")));
@property (class, readonly) SharedAuthRulesGoogleLoginFailureAction *silent __attribute__((swift_name("silent")));
@property (class, readonly) SharedAuthRulesGoogleLoginFailureAction *showError __attribute__((swift_name("showError")));
+ (SharedKotlinArray<SharedAuthRulesGoogleLoginFailureAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAuthRulesGoogleLoginFailureAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthRules.PendingRegistration")))
@interface SharedAuthRulesPendingRegistration : SharedBase
- (instancetype)initWithName:(NSString *)name password:(NSString *)password role:(NSString *)role __attribute__((swift_name("init(name:password:role:)"))) __attribute__((objc_designated_initializer));
- (SharedAuthRulesPendingRegistration *)doCopyName:(NSString *)name password:(NSString *)password role:(NSString *)role __attribute__((swift_name("doCopy(name:password:role:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *password __attribute__((swift_name("password")));
@property (readonly) NSString *role __attribute__((swift_name("role")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthRules.PendingRegistrationStore")))
@interface SharedAuthRulesPendingRegistrationStore : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedAuthRulesPendingRegistration * _Nullable)consumePendingRegistrationEmail:(NSString *)email __attribute__((swift_name("consumePendingRegistration(email:)")));
- (NSString * _Nullable)createPendingRegistrationForOTPEmail:(NSString *)email __attribute__((swift_name("createPendingRegistrationForOTP(email:)")));
- (NSString * _Nullable)createPendingTeacherRegistrationForOTPEmail:(NSString *)email name:(NSString *)name password:(NSString *)password __attribute__((swift_name("createPendingTeacherRegistrationForOTP(email:name:password:)")));
- (void)rememberPendingRegistrationEmail:(NSString *)email name:(NSString *)name password:(NSString *)password role:(NSString *)role __attribute__((swift_name("rememberPendingRegistration(email:name:password:role:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthRules.StudentEmailFlow")))
@interface SharedAuthRulesStudentEmailFlow : SharedKotlinEnum<SharedAuthRulesStudentEmailFlow *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAuthRulesStudentEmailFlow *rejectTeacher __attribute__((swift_name("rejectTeacher")));
@property (class, readonly) SharedAuthRulesStudentEmailFlow *sendLoginCode __attribute__((swift_name("sendLoginCode")));
@property (class, readonly) SharedAuthRulesStudentEmailFlow *errorCodeNotSent __attribute__((swift_name("errorCodeNotSent")));
@property (class, readonly) SharedAuthRulesStudentEmailFlow *theNewRegistration __attribute__((swift_name("theNewRegistration")));
+ (SharedKotlinArray<SharedAuthRulesStudentEmailFlow *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAuthRulesStudentEmailFlow *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FcmRules")))
@interface SharedFcmRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)fcmRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFcmRules *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)clearTokenUpdateExistingDevices:(NSArray<NSDictionary<id, id> *> *)existingDevices deviceId:(NSString *)deviceId nowMs:(int64_t)nowMs __attribute__((swift_name("clearTokenUpdate(existingDevices:deviceId:nowMs:)")));
- (NSArray<NSDictionary<id, id> *> *)devicesFromRaw:(id _Nullable)raw __attribute__((swift_name("devicesFrom(raw:)")));
- (BOOL)isProfileCompleteForTokenUserData:(NSDictionary<NSString *, id> * _Nullable)userData __attribute__((swift_name("isProfileCompleteForToken(userData:)")));
- (SharedFcmRulesIncoming * _Nullable)parseIncomingData:(NSDictionary<NSString *, NSString *> *)data appName:(NSString *)appName __attribute__((swift_name("parseIncoming(data:appName:)")));
- (NSDictionary<NSString *, id> *)saveTokenUpdateExistingDevices:(NSArray<NSDictionary<id, id> *> *)existingDevices deviceId:(NSString *)deviceId token:(NSString *)token nowMs:(int64_t)nowMs __attribute__((swift_name("saveTokenUpdate(existingDevices:deviceId:token:nowMs:)")));
- (BOOL)shouldShowNotificationCurrentUid:(NSString * _Nullable)currentUid recipientUid:(NSString *)recipientUid notificationsEnabled:(BOOL)notificationsEnabled __attribute__((swift_name("shouldShowNotification(currentUid:recipientUid:notificationsEnabled:)")));
@property (readonly) NSString *CHANNEL_ID_MESSAGES __attribute__((swift_name("CHANNEL_ID_MESSAGES")));
@property (readonly) NSString *DATA_BODY __attribute__((swift_name("DATA_BODY")));
@property (readonly) NSString *DATA_MESSAGE_ID __attribute__((swift_name("DATA_MESSAGE_ID")));
@property (readonly) NSString *DATA_MESSAGE_PREVIEW __attribute__((swift_name("DATA_MESSAGE_PREVIEW")));
@property (readonly) NSString *DATA_QUESTION_ID __attribute__((swift_name("DATA_QUESTION_ID")));
@property (readonly) NSString *DATA_RECIPIENT_UID __attribute__((swift_name("DATA_RECIPIENT_UID")));
@property (readonly) NSString *DATA_SENDER_NAME __attribute__((swift_name("DATA_SENDER_NAME")));
@property (readonly) NSString *DATA_TITLE __attribute__((swift_name("DATA_TITLE")));
@property (readonly) NSString *DEVICE_FIELD_DEVICE_ID __attribute__((swift_name("DEVICE_FIELD_DEVICE_ID")));
@property (readonly) NSString *DEVICE_FIELD_TOKEN __attribute__((swift_name("DEVICE_FIELD_TOKEN")));
@property (readonly) NSString *DEVICE_FIELD_UPDATED_AT __attribute__((swift_name("DEVICE_FIELD_UPDATED_AT")));
@property (readonly) NSString *FIELD_FCM_DEVICES __attribute__((swift_name("FIELD_FCM_DEVICES")));
@property (readonly) NSString *FIELD_FCM_TOKEN __attribute__((swift_name("FIELD_FCM_TOKEN")));
@property (readonly) NSString *FIELD_FCM_TOKEN_UPDATED_AT __attribute__((swift_name("FIELD_FCM_TOKEN_UPDATED_AT")));
@property (readonly) int32_t MAX_DEVICES __attribute__((swift_name("MAX_DEVICES")));
@property (readonly) int32_t MAX_INBOX_LINES __attribute__((swift_name("MAX_INBOX_LINES")));
@property (readonly) NSString *PREFS_NAME_THREADS __attribute__((swift_name("PREFS_NAME_THREADS")));
@property (readonly) NSString *PREF_NOTIFICATIONS_ENABLED __attribute__((swift_name("PREF_NOTIFICATIONS_ENABLED")));
@property (readonly) NSString *UNKNOWN_DEVICE __attribute__((swift_name("UNKNOWN_DEVICE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FcmRules.Incoming")))
@interface SharedFcmRulesIncoming : SharedBase
- (instancetype)initWithTitle:(NSString *)title body:(NSString *)body questionId:(NSString *)questionId messageId:(NSString * _Nullable)messageId recipientUid:(NSString *)recipientUid __attribute__((swift_name("init(title:body:questionId:messageId:recipientUid:)"))) __attribute__((objc_designated_initializer));
- (SharedFcmRulesIncoming *)doCopyTitle:(NSString *)title body:(NSString *)body questionId:(NSString *)questionId messageId:(NSString * _Nullable)messageId recipientUid:(NSString *)recipientUid __attribute__((swift_name("doCopy(title:body:questionId:messageId:recipientUid:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *body __attribute__((swift_name("body")));
@property (readonly) NSString * _Nullable messageId __attribute__((swift_name("messageId")));
@property (readonly) NSString *questionId __attribute__((swift_name("questionId")));
@property (readonly) NSString *recipientUid __attribute__((swift_name("recipientUid")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FollowRules")))
@interface SharedFollowRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)followRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFollowRules *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)followBackWritesMyUid:(NSString *)myUid myUserId:(NSString *)myUserId myName:(NSString *)myName targetUid:(NSString *)targetUid targetUserId:(NSString *)targetUserId targetName:(NSString *)targetName nowMs:(int64_t)nowMs __attribute__((swift_name("followBackWrites(myUid:myUserId:myName:targetUid:targetUserId:targetName:nowMs:)")));
- (NSDictionary<NSString *, id> *)followDocUserId:(NSString *)userId name:(NSString *)name nowMs:(int64_t)nowMs __attribute__((swift_name("followDoc(userId:name:nowMs:)")));
- (NSDictionary<NSString *, NSDictionary<NSString *, id> *> *)followWritesMyUid:(NSString *)myUid myUserId:(NSString *)myUserId myName:(NSString *)myName targetUid:(NSString *)targetUid targetUserId:(NSString *)targetUserId targetName:(NSString *)targetName nowMs:(int64_t)nowMs __attribute__((swift_name("followWrites(myUid:myUserId:myName:targetUid:targetUserId:targetName:nowMs:)")));
- (NSString *)followerDocPathTargetUid:(NSString *)targetUid myUid:(NSString *)myUid __attribute__((swift_name("followerDocPath(targetUid:myUid:)")));
- (NSString *)followingDocPathMyUid:(NSString *)myUid targetUid:(NSString *)targetUid __attribute__((swift_name("followingDocPath(myUid:targetUid:)")));
- (SharedFollowRulesFriendSearchResult * _Nullable)parseSearchResultDocId:(NSString *)docId data:(NSDictionary<NSString *, id> *)data currentUserId:(NSString *)currentUserId __attribute__((swift_name("parseSearchResult(docId:data:currentUserId:)")));
@property (readonly) NSString *COLLECTION_PUBLIC_PROFILES __attribute__((swift_name("COLLECTION_PUBLIC_PROFILES")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *FIELD_FOLLOWED_AT __attribute__((swift_name("FIELD_FOLLOWED_AT")));
@property (readonly) NSString *FIELD_NAME __attribute__((swift_name("FIELD_NAME")));
@property (readonly) NSString *FIELD_SELECTED_AVATAR __attribute__((swift_name("FIELD_SELECTED_AVATAR")));
@property (readonly) NSString *FIELD_USER_ID __attribute__((swift_name("FIELD_USER_ID")));
@property (readonly) NSString *SUBCOLLECTION_FOLLOWERS __attribute__((swift_name("SUBCOLLECTION_FOLLOWERS")));
@property (readonly) NSString *SUBCOLLECTION_FOLLOWING __attribute__((swift_name("SUBCOLLECTION_FOLLOWING")));
@property (readonly) int32_t TAB_FOLLOWERS __attribute__((swift_name("TAB_FOLLOWERS")));
@property (readonly) int32_t TAB_FOLLOWING __attribute__((swift_name("TAB_FOLLOWING")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FollowRules.FriendSearchResult")))
@interface SharedFollowRulesFriendSearchResult : SharedBase
- (instancetype)initWithFirebaseUid:(NSString *)firebaseUid name:(NSString *)name userId:(NSString *)userId selectedAvatar:(int32_t)selectedAvatar __attribute__((swift_name("init(firebaseUid:name:userId:selectedAvatar:)"))) __attribute__((objc_designated_initializer));
- (SharedFollowRulesFriendSearchResult *)doCopyFirebaseUid:(NSString *)firebaseUid name:(NSString *)name userId:(NSString *)userId selectedAvatar:(int32_t)selectedAvatar __attribute__((swift_name("doCopy(firebaseUid:name:userId:selectedAvatar:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *firebaseUid __attribute__((swift_name("firebaseUid")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t selectedAvatar __attribute__((swift_name("selectedAvatar")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LoginFlowTexts")))
@interface SharedLoginFlowTexts : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loginFlowTexts __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLoginFlowTexts *shared __attribute__((swift_name("shared")));
- (NSString *)otpTitleForPasswordReset:(BOOL)forPasswordReset isRegistration:(BOOL)isRegistration __attribute__((swift_name("otpTitle(forPasswordReset:isRegistration:)")));
- (NSString *)policyPageLangCode:(NSString *)langCode __attribute__((swift_name("policyPage(langCode:)")));
- (NSString *)privacyUrlLangCode:(NSString *)langCode __attribute__((swift_name("privacyUrl(langCode:)")));
- (NSString *)termsUrlLangCode:(NSString *)langCode __attribute__((swift_name("termsUrl(langCode:)")));
@property (readonly) NSString *BUTTON_CONTINUE __attribute__((swift_name("BUTTON_CONTINUE")));
@property (readonly) NSString *BUTTON_SENDING __attribute__((swift_name("BUTTON_SENDING")));
@property (readonly) NSString *BUTTON_UPDATE_PASSWORD __attribute__((swift_name("BUTTON_UPDATE_PASSWORD")));
@property (readonly) NSString *BUTTON_UPDATING __attribute__((swift_name("BUTTON_UPDATING")));
@property (readonly) NSString *BUTTON_VERIFY __attribute__((swift_name("BUTTON_VERIFY")));
@property (readonly) NSString *BUTTON_VERIFYING __attribute__((swift_name("BUTTON_VERIFYING")));
@property (readonly) NSString *MESSAGE_CODE_RESENT __attribute__((swift_name("MESSAGE_CODE_RESENT")));
@property (readonly) NSString *MESSAGE_GOOGLE_LOGIN_FAILED __attribute__((swift_name("MESSAGE_GOOGLE_LOGIN_FAILED")));
@property (readonly) NSString *MESSAGE_GOOGLE_REGISTER_FAILED __attribute__((swift_name("MESSAGE_GOOGLE_REGISTER_FAILED")));
@property (readonly) int64_t NEW_ACCOUNT_WINDOW_MS __attribute__((swift_name("NEW_ACCOUNT_WINDOW_MS")));
@property (readonly) NSString *PRIVACY_LINK_TEXT __attribute__((swift_name("PRIVACY_LINK_TEXT")));
@property (readonly) NSString *PRIVACY_URL __attribute__((swift_name("PRIVACY_URL")));
@property (readonly) NSString *TERMS_FULL_TEXT __attribute__((swift_name("TERMS_FULL_TEXT")));
@property (readonly) NSString *TERMS_LINK_TEXT __attribute__((swift_name("TERMS_LINK_TEXT")));
@property (readonly) NSString *TERMS_URL __attribute__((swift_name("TERMS_URL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MessageReport")))
@interface SharedMessageReport : SharedBase
- (instancetype)initWithId:(NSString *)id questionId:(NSString *)questionId messageId:(NSString *)messageId reportedByUid:(NSString *)reportedByUid reportedUserUid:(NSString *)reportedUserUid messagePreview:(NSString *)messagePreview reason:(NSString *)reason type:(NSString *)type mediaUrl:(NSString * _Nullable)mediaUrl thumbnailUrl:(NSString * _Nullable)thumbnailUrl reportedAt:(SharedLong * _Nullable)reportedAt status:(NSString *)status handledByUid:(NSString * _Nullable)handledByUid handledAt:(SharedLong * _Nullable)handledAt __attribute__((swift_name("init(id:questionId:messageId:reportedByUid:reportedUserUid:messagePreview:reason:type:mediaUrl:thumbnailUrl:reportedAt:status:handledByUid:handledAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMessageReportCompanion *companion __attribute__((swift_name("companion")));
- (SharedMessageReport *)doCopyId:(NSString *)id questionId:(NSString *)questionId messageId:(NSString *)messageId reportedByUid:(NSString *)reportedByUid reportedUserUid:(NSString *)reportedUserUid messagePreview:(NSString *)messagePreview reason:(NSString *)reason type:(NSString *)type mediaUrl:(NSString * _Nullable)mediaUrl thumbnailUrl:(NSString * _Nullable)thumbnailUrl reportedAt:(SharedLong * _Nullable)reportedAt status:(NSString *)status handledByUid:(NSString * _Nullable)handledByUid handledAt:(SharedLong * _Nullable)handledAt __attribute__((swift_name("doCopy(id:questionId:messageId:reportedByUid:reportedUserUid:messagePreview:reason:type:mediaUrl:thumbnailUrl:reportedAt:status:handledByUid:handledAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSDictionary<NSString *, id> *)toMap __attribute__((swift_name("toMap()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable handledAt __attribute__((swift_name("handledAt")));
@property (readonly) NSString * _Nullable handledByUid __attribute__((swift_name("handledByUid")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable mediaUrl __attribute__((swift_name("mediaUrl")));
@property (readonly) NSString *messageId __attribute__((swift_name("messageId")));
@property (readonly) NSString *messagePreview __attribute__((swift_name("messagePreview")));
@property (readonly) NSString *questionId __attribute__((swift_name("questionId")));
@property (readonly) NSString *reason __attribute__((swift_name("reason")));
@property (readonly) SharedLong * _Nullable reportedAt __attribute__((swift_name("reportedAt")));
@property (readonly) NSString *reportedByUid __attribute__((swift_name("reportedByUid")));
@property (readonly) NSString *reportedUserUid __attribute__((swift_name("reportedUserUid")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString * _Nullable thumbnailUrl __attribute__((swift_name("thumbnailUrl")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MessageReport.Companion")))
@interface SharedMessageReportCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMessageReportCompanion *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)actionTakenUpdateMyUid:(NSString *)myUid nowMs:(int64_t)nowMs __attribute__((swift_name("actionTakenUpdate(myUid:nowMs:)")));
- (NSDictionary<NSString *, id> *)dismissUpdateMyUid:(NSString *)myUid nowMs:(int64_t)nowMs __attribute__((swift_name("dismissUpdate(myUid:nowMs:)")));
- (SharedMessageReport *)fromMapId:(NSString *)id data:(NSDictionary<NSString *, id> *)data __attribute__((swift_name("fromMap(id:data:)")));
- (NSString *)reasonLabelReason:(NSString *)reason __attribute__((swift_name("reasonLabel(reason:)")));
@property (readonly) NSString *COLLECTION __attribute__((swift_name("COLLECTION")));
@property (readonly) NSString *FIELD_HANDLED_AT __attribute__((swift_name("FIELD_HANDLED_AT")));
@property (readonly) NSString *FIELD_HANDLED_BY_UID __attribute__((swift_name("FIELD_HANDLED_BY_UID")));
@property (readonly) NSString *FIELD_MEDIA_URL __attribute__((swift_name("FIELD_MEDIA_URL")));
@property (readonly) NSString *FIELD_MESSAGE_ID __attribute__((swift_name("FIELD_MESSAGE_ID")));
@property (readonly) NSString *FIELD_MESSAGE_PREVIEW __attribute__((swift_name("FIELD_MESSAGE_PREVIEW")));
@property (readonly) NSString *FIELD_QUESTION_ID __attribute__((swift_name("FIELD_QUESTION_ID")));
@property (readonly) NSString *FIELD_REASON __attribute__((swift_name("FIELD_REASON")));
@property (readonly) NSString *FIELD_REPORTED_AT __attribute__((swift_name("FIELD_REPORTED_AT")));
@property (readonly) NSString *FIELD_REPORTED_BY_UID __attribute__((swift_name("FIELD_REPORTED_BY_UID")));
@property (readonly) NSString *FIELD_REPORTED_USER_UID __attribute__((swift_name("FIELD_REPORTED_USER_UID")));
@property (readonly) NSString *FIELD_STATUS __attribute__((swift_name("FIELD_STATUS")));
@property (readonly) NSString *FIELD_THUMBNAIL_URL __attribute__((swift_name("FIELD_THUMBNAIL_URL")));
@property (readonly) NSString *FIELD_TYPE __attribute__((swift_name("FIELD_TYPE")));
@property (readonly) NSString *REASON_HARASSMENT __attribute__((swift_name("REASON_HARASSMENT")));
@property (readonly) NSString *REASON_INAPPROPRIATE __attribute__((swift_name("REASON_INAPPROPRIATE")));
@property (readonly) NSString *REASON_OTHER __attribute__((swift_name("REASON_OTHER")));
@property (readonly) NSString *REASON_SPAM __attribute__((swift_name("REASON_SPAM")));
@property (readonly) NSString *STATUS_ACTION_TAKEN __attribute__((swift_name("STATUS_ACTION_TAKEN")));
@property (readonly) NSString *STATUS_DISMISSED __attribute__((swift_name("STATUS_DISMISSED")));
@property (readonly) NSString *STATUS_PENDING __attribute__((swift_name("STATUS_PENDING")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OtpRules")))
@interface SharedOtpRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)otpRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedOtpRules *shared __attribute__((swift_name("shared")));
- (SharedInt * _Nullable)getActiveWrongAttemptCooldownEndMs:(int64_t)endMs savedEmail:(NSString * _Nullable)savedEmail forEmail:(NSString *)forEmail nowMs:(int64_t)nowMs __attribute__((swift_name("getActiveWrongAttemptCooldown(endMs:savedEmail:forEmail:nowMs:)")));
- (int64_t)getResendCooldownRemainingMsEndMs:(int64_t)endMs savedEmail:(NSString * _Nullable)savedEmail forEmail:(NSString *)forEmail nowMs:(int64_t)nowMs __attribute__((swift_name("getResendCooldownRemainingMs(endMs:savedEmail:forEmail:nowMs:)")));
- (BOOL)isValidCodeLengthRawCode:(NSString * _Nullable)rawCode __attribute__((swift_name("isValidCodeLength(rawCode:)")));
- (SharedOtpRulesWrongAttemptResult *)onVerifyFailedCurrentWrongAttempts:(int32_t)currentWrongAttempts error:(NSString * _Nullable)error __attribute__((swift_name("onVerifyFailed(currentWrongAttempts:error:)")));
- (NSString *)resendCooldownTryAgainLabelSec:(int32_t)sec __attribute__((swift_name("resendCooldownTryAgainLabel(sec:)")));
- (SharedKotlinPair<SharedLong *, NSString *> *)startResendCooldownEmail:(NSString *)email nowMs:(int64_t)nowMs __attribute__((swift_name("startResendCooldown(email:nowMs:)")));
- (int64_t)wrongAttemptCooldownEndMsNowMs:(int64_t)nowMs __attribute__((swift_name("wrongAttemptCooldownEndMs(nowMs:)")));
- (NSString *)wrongCooldownTryLaterMessageRemainingMin:(int32_t)remainingMin __attribute__((swift_name("wrongCooldownTryLaterMessage(remainingMin:)")));
@property (readonly) int32_t CODE_LENGTH __attribute__((swift_name("CODE_LENGTH")));
@property (readonly) int32_t MAX_WRONG_ATTEMPTS __attribute__((swift_name("MAX_WRONG_ATTEMPTS")));
@property (readonly) NSString *MESSAGE_CODE_LENGTH __attribute__((swift_name("MESSAGE_CODE_LENGTH")));
@property (readonly) NSString *MESSAGE_TOO_MANY_WRONG __attribute__((swift_name("MESSAGE_TOO_MANY_WRONG")));
@property (readonly) NSString *OTP_COOLDOWN_EMAIL __attribute__((swift_name("OTP_COOLDOWN_EMAIL")));
@property (readonly) NSString *OTP_COOLDOWN_END_MS __attribute__((swift_name("OTP_COOLDOWN_END_MS")));
@property (readonly) NSString *OTP_COOLDOWN_PREFS __attribute__((swift_name("OTP_COOLDOWN_PREFS")));
@property (readonly) NSString *OTP_RESEND_COOLDOWN_EMAIL __attribute__((swift_name("OTP_RESEND_COOLDOWN_EMAIL")));
@property (readonly) NSString *OTP_RESEND_COOLDOWN_END_MS __attribute__((swift_name("OTP_RESEND_COOLDOWN_END_MS")));
@property (readonly) int64_t RESEND_COOLDOWN_MS __attribute__((swift_name("RESEND_COOLDOWN_MS")));
@property (readonly) int64_t WRONG_ATTEMPT_COOLDOWN_MS __attribute__((swift_name("WRONG_ATTEMPT_COOLDOWN_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OtpRules.WrongAttemptResult")))
@interface SharedOtpRulesWrongAttemptResult : SharedBase
- (instancetype)initWithWrongAttempts:(int32_t)wrongAttempts verifyEnabled:(BOOL)verifyEnabled startCooldown:(BOOL)startCooldown __attribute__((swift_name("init(wrongAttempts:verifyEnabled:startCooldown:)"))) __attribute__((objc_designated_initializer));
- (SharedOtpRulesWrongAttemptResult *)doCopyWrongAttempts:(int32_t)wrongAttempts verifyEnabled:(BOOL)verifyEnabled startCooldown:(BOOL)startCooldown __attribute__((swift_name("doCopy(wrongAttempts:verifyEnabled:startCooldown:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL startCooldown __attribute__((swift_name("startCooldown")));
@property (readonly) BOOL verifyEnabled __attribute__((swift_name("verifyEnabled")));
@property (readonly) int32_t wrongAttempts __attribute__((swift_name("wrongAttempts")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PendingUploadMeta")))
@interface SharedPendingUploadMeta : SharedBase
- (instancetype)initWithQuestionId:(NSString *)questionId type:(NSString *)type filePath:(NSString * _Nullable)filePath textContent:(NSString * _Nullable)textContent caption:(NSString * _Nullable)caption __attribute__((swift_name("init(questionId:type:filePath:textContent:caption:)"))) __attribute__((objc_designated_initializer));
- (SharedPendingUploadMeta *)doCopyQuestionId:(NSString *)questionId type:(NSString *)type filePath:(NSString * _Nullable)filePath textContent:(NSString * _Nullable)textContent caption:(NSString * _Nullable)caption __attribute__((swift_name("doCopy(questionId:type:filePath:textContent:caption:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable caption __attribute__((swift_name("caption")));
@property (readonly) NSString * _Nullable filePath __attribute__((swift_name("filePath")));
@property (readonly) NSString *questionId __attribute__((swift_name("questionId")));
@property (readonly) NSString * _Nullable textContent __attribute__((swift_name("textContent")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("QuestionMessage")))
@interface SharedQuestionMessage : SharedBase
- (instancetype)initWithId:(NSString *)id senderUid:(NSString *)senderUid senderRole:(NSString *)senderRole type:(NSString *)type textContent:(NSString * _Nullable)textContent mediaStoragePath:(NSString * _Nullable)mediaStoragePath mediaUrl:(NSString * _Nullable)mediaUrl thumbnailUrl:(NSString * _Nullable)thumbnailUrl mediaSizeBytes:(SharedLong * _Nullable)mediaSizeBytes clientId:(NSString * _Nullable)clientId createdAt:(SharedLong * _Nullable)createdAt deliveredAt:(SharedLong * _Nullable)deliveredAt readAt:(SharedLong * _Nullable)readAt deletedForUids:(NSArray<NSString *> * _Nullable)deletedForUids __attribute__((swift_name("init(id:senderUid:senderRole:type:textContent:mediaStoragePath:mediaUrl:thumbnailUrl:mediaSizeBytes:clientId:createdAt:deliveredAt:readAt:deletedForUids:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedQuestionMessageCompanion *companion __attribute__((swift_name("companion")));
- (SharedQuestionMessage *)doCopyId:(NSString *)id senderUid:(NSString *)senderUid senderRole:(NSString *)senderRole type:(NSString *)type textContent:(NSString * _Nullable)textContent mediaStoragePath:(NSString * _Nullable)mediaStoragePath mediaUrl:(NSString * _Nullable)mediaUrl thumbnailUrl:(NSString * _Nullable)thumbnailUrl mediaSizeBytes:(SharedLong * _Nullable)mediaSizeBytes clientId:(NSString * _Nullable)clientId createdAt:(SharedLong * _Nullable)createdAt deliveredAt:(SharedLong * _Nullable)deliveredAt readAt:(SharedLong * _Nullable)readAt deletedForUids:(NSArray<NSString *> * _Nullable)deletedForUids __attribute__((swift_name("doCopy(id:senderUid:senderRole:type:textContent:mediaStoragePath:mediaUrl:thumbnailUrl:mediaSizeBytes:clientId:createdAt:deliveredAt:readAt:deletedForUids:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)isPending __attribute__((swift_name("isPending()")));
- (BOOL)isUnreadForUid:(NSString *)uid __attribute__((swift_name("isUnreadFor(uid:)")));
- (NSDictionary<NSString *, id> *)toMap __attribute__((swift_name("toMap()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable clientId __attribute__((swift_name("clientId")));
@property (readonly) SharedLong * _Nullable createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSArray<NSString *> * _Nullable deletedForUids __attribute__((swift_name("deletedForUids")));
@property (readonly) SharedLong * _Nullable deliveredAt __attribute__((swift_name("deliveredAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) SharedLong * _Nullable mediaSizeBytes __attribute__((swift_name("mediaSizeBytes")));
@property (readonly) NSString * _Nullable mediaStoragePath __attribute__((swift_name("mediaStoragePath")));
@property (readonly) NSString * _Nullable mediaUrl __attribute__((swift_name("mediaUrl")));
@property (readonly) SharedLong * _Nullable readAt __attribute__((swift_name("readAt")));
@property (readonly) NSString *senderRole __attribute__((swift_name("senderRole")));
@property (readonly) NSString *senderUid __attribute__((swift_name("senderUid")));
@property (readonly) NSString * _Nullable textContent __attribute__((swift_name("textContent")));
@property (readonly) NSString * _Nullable thumbnailUrl __attribute__((swift_name("thumbnailUrl")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("QuestionMessage.Companion")))
@interface SharedQuestionMessageCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedQuestionMessageCompanion *shared __attribute__((swift_name("shared")));
- (SharedQuestionMessage *)fromMapData:(NSDictionary<NSString *, id> *)data __attribute__((swift_name("fromMap(data:)")));
@property (readonly) NSString *FIELD_CLIENT_ID __attribute__((swift_name("FIELD_CLIENT_ID")));
@property (readonly) NSString *FIELD_CREATED_AT __attribute__((swift_name("FIELD_CREATED_AT")));
@property (readonly) NSString *FIELD_DELETED_FOR_UIDS __attribute__((swift_name("FIELD_DELETED_FOR_UIDS")));
@property (readonly) NSString *FIELD_DELIVERED_AT __attribute__((swift_name("FIELD_DELIVERED_AT")));
@property (readonly) NSString *FIELD_ID __attribute__((swift_name("FIELD_ID")));
@property (readonly) NSString *FIELD_MEDIA_SIZE_BYTES __attribute__((swift_name("FIELD_MEDIA_SIZE_BYTES")));
@property (readonly) NSString *FIELD_MEDIA_STORAGE_PATH __attribute__((swift_name("FIELD_MEDIA_STORAGE_PATH")));
@property (readonly) NSString *FIELD_MEDIA_URL __attribute__((swift_name("FIELD_MEDIA_URL")));
@property (readonly) NSString *FIELD_READ_AT __attribute__((swift_name("FIELD_READ_AT")));
@property (readonly) NSString *FIELD_SENDER_ROLE __attribute__((swift_name("FIELD_SENDER_ROLE")));
@property (readonly) NSString *FIELD_SENDER_UID __attribute__((swift_name("FIELD_SENDER_UID")));
@property (readonly) NSString *FIELD_TEXT_CONTENT __attribute__((swift_name("FIELD_TEXT_CONTENT")));
@property (readonly) NSString *FIELD_THUMBNAIL_URL __attribute__((swift_name("FIELD_THUMBNAIL_URL")));
@property (readonly) NSString *FIELD_TYPE __attribute__((swift_name("FIELD_TYPE")));
@property (readonly) NSString *PENDING_ID_PREFIX __attribute__((swift_name("PENDING_ID_PREFIX")));
@property (readonly) NSString *TYPE_AUDIO __attribute__((swift_name("TYPE_AUDIO")));
@property (readonly) NSString *TYPE_IMAGE __attribute__((swift_name("TYPE_IMAGE")));
@property (readonly) NSString *TYPE_TEXT __attribute__((swift_name("TYPE_TEXT")));
@property (readonly) NSString *TYPE_VIDEO __attribute__((swift_name("TYPE_VIDEO")));
@property (readonly) int64_t UNREAD_SCAN_LIMIT __attribute__((swift_name("UNREAD_SCAN_LIMIT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RestrictedUser")))
@interface SharedRestrictedUser : SharedBase
- (instancetype)initWithUid:(NSString *)uid name:(NSString *)name banned:(BOOL)banned restrictedUntil:(SharedLong * _Nullable)restrictedUntil restrictionReason:(NSString * _Nullable)restrictionReason restrictionMessagePreview:(NSString * _Nullable)restrictionMessagePreview restrictionMessageType:(NSString * _Nullable)restrictionMessageType restrictionMediaUrl:(NSString * _Nullable)restrictionMediaUrl restrictionThumbnailUrl:(NSString * _Nullable)restrictionThumbnailUrl restrictedAt:(SharedLong * _Nullable)restrictedAt restrictedByUid:(NSString * _Nullable)restrictedByUid __attribute__((swift_name("init(uid:name:banned:restrictedUntil:restrictionReason:restrictionMessagePreview:restrictionMessageType:restrictionMediaUrl:restrictionThumbnailUrl:restrictedAt:restrictedByUid:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedRestrictedUserCompanion *companion __attribute__((swift_name("companion")));
- (SharedRestrictedUser *)doCopyUid:(NSString *)uid name:(NSString *)name banned:(BOOL)banned restrictedUntil:(SharedLong * _Nullable)restrictedUntil restrictionReason:(NSString * _Nullable)restrictionReason restrictionMessagePreview:(NSString * _Nullable)restrictionMessagePreview restrictionMessageType:(NSString * _Nullable)restrictionMessageType restrictionMediaUrl:(NSString * _Nullable)restrictionMediaUrl restrictionThumbnailUrl:(NSString * _Nullable)restrictionThumbnailUrl restrictedAt:(SharedLong * _Nullable)restrictedAt restrictedByUid:(NSString * _Nullable)restrictedByUid __attribute__((swift_name("doCopy(uid:name:banned:restrictedUntil:restrictionReason:restrictionMessagePreview:restrictionMessageType:restrictionMediaUrl:restrictionThumbnailUrl:restrictedAt:restrictedByUid:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL banned __attribute__((swift_name("banned")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) SharedLong * _Nullable restrictedAt __attribute__((swift_name("restrictedAt")));
@property (readonly) NSString * _Nullable restrictedByUid __attribute__((swift_name("restrictedByUid")));
@property (readonly) SharedLong * _Nullable restrictedUntil __attribute__((swift_name("restrictedUntil")));
@property (readonly) NSString * _Nullable restrictionMediaUrl __attribute__((swift_name("restrictionMediaUrl")));
@property (readonly) NSString * _Nullable restrictionMessagePreview __attribute__((swift_name("restrictionMessagePreview")));
@property (readonly) NSString * _Nullable restrictionMessageType __attribute__((swift_name("restrictionMessageType")));
@property (readonly) NSString * _Nullable restrictionReason __attribute__((swift_name("restrictionReason")));
@property (readonly) NSString * _Nullable restrictionThumbnailUrl __attribute__((swift_name("restrictionThumbnailUrl")));
@property (readonly) NSString *uid __attribute__((swift_name("uid")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RestrictedUser.Companion")))
@interface SharedRestrictedUserCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRestrictedUserCompanion *shared __attribute__((swift_name("shared")));
- (SharedRestrictedUser *)fromMapUid:(NSString *)uid data:(NSDictionary<NSString *, id> *)data __attribute__((swift_name("fromMap(uid:data:)")));
- (int64_t)restrictedUntilMsNowMs:(int64_t)nowMs days:(int32_t)days __attribute__((swift_name("restrictedUntilMs(nowMs:days:)")));
- (SharedRestrictedUserRestrictionUpdate *)restrictionUpdateReport:(SharedMessageReport *)report myUid:(NSString *)myUid nowMs:(int64_t)nowMs days:(SharedInt * _Nullable)days __attribute__((swift_name("restrictionUpdate(report:myUid:nowMs:days:)")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *FIELD_BANNED __attribute__((swift_name("FIELD_BANNED")));
@property (readonly) NSString *FIELD_NAME __attribute__((swift_name("FIELD_NAME")));
@property (readonly) NSString *FIELD_RESTRICTED_AT __attribute__((swift_name("FIELD_RESTRICTED_AT")));
@property (readonly) NSString *FIELD_RESTRICTED_BY_UID __attribute__((swift_name("FIELD_RESTRICTED_BY_UID")));
@property (readonly) NSString *FIELD_RESTRICTED_UNTIL __attribute__((swift_name("FIELD_RESTRICTED_UNTIL")));
@property (readonly) NSString *FIELD_RESTRICTION_MEDIA_URL __attribute__((swift_name("FIELD_RESTRICTION_MEDIA_URL")));
@property (readonly) NSString *FIELD_RESTRICTION_MESSAGE_PREVIEW __attribute__((swift_name("FIELD_RESTRICTION_MESSAGE_PREVIEW")));
@property (readonly) NSString *FIELD_RESTRICTION_MESSAGE_TYPE __attribute__((swift_name("FIELD_RESTRICTION_MESSAGE_TYPE")));
@property (readonly) NSString *FIELD_RESTRICTION_REASON __attribute__((swift_name("FIELD_RESTRICTION_REASON")));
@property (readonly) NSString *FIELD_RESTRICTION_THUMBNAIL_URL __attribute__((swift_name("FIELD_RESTRICTION_THUMBNAIL_URL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RestrictedUser.RestrictionUpdate")))
@interface SharedRestrictedUserRestrictionUpdate : SharedBase
- (instancetype)initWithAlanlar:(NSDictionary<NSString *, id> *)alanlar silinecekAlanlar:(NSArray<NSString *> *)silinecekAlanlar __attribute__((swift_name("init(alanlar:silinecekAlanlar:)"))) __attribute__((objc_designated_initializer));
- (SharedRestrictedUserRestrictionUpdate *)doCopyAlanlar:(NSDictionary<NSString *, id> *)alanlar silinecekAlanlar:(NSArray<NSString *> *)silinecekAlanlar __attribute__((swift_name("doCopy(alanlar:silinecekAlanlar:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, id> *alanlar __attribute__((swift_name("alanlar")));
@property (readonly) NSArray<NSString *> *silinecekAlanlar __attribute__((swift_name("silinecekAlanlar")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionDeviceRules")))
@interface SharedSessionDeviceRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)sessionDeviceRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSessionDeviceRules *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)claimUpdateDeviceId:(NSString *)deviceId nowMs:(int64_t)nowMs __attribute__((swift_name("claimUpdate(deviceId:nowMs:)")));
- (SharedSessionDeviceRulesDecision *)decideIsLoggedIn:(BOOL)isLoggedIn userDocExists:(BOOL)userDocExists activeDeviceId:(NSString * _Nullable)activeDeviceId lastUpdatedMs:(SharedLong * _Nullable)lastUpdatedMs deviceId:(NSString *)deviceId nowMs:(int64_t)nowMs __attribute__((swift_name("decide(isLoggedIn:userDocExists:activeDeviceId:lastUpdatedMs:deviceId:nowMs:)")));
- (BOOL)needsNewDeviceIdExisting:(NSString * _Nullable)existing __attribute__((swift_name("needsNewDeviceId(existing:)")));
- (NSDictionary<NSString *, id> *)releaseUpdateNowMs:(int64_t)nowMs __attribute__((swift_name("releaseUpdate(nowMs:)")));
- (BOOL)shouldForceSignOutActiveDeviceId:(NSString * _Nullable)activeDeviceId deviceId:(NSString *)deviceId __attribute__((swift_name("shouldForceSignOut(activeDeviceId:deviceId:)")));
- (BOOL)shouldReleaseActiveDeviceId:(NSString * _Nullable)activeDeviceId deviceId:(NSString *)deviceId __attribute__((swift_name("shouldRelease(activeDeviceId:deviceId:)")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *FIELD_ACTIVE_DEVICE_ID __attribute__((swift_name("FIELD_ACTIVE_DEVICE_ID")));
@property (readonly) NSString *FIELD_ACTIVE_SESSION_UPDATED_AT __attribute__((swift_name("FIELD_ACTIVE_SESSION_UPDATED_AT")));
@property (readonly) int64_t HEARTBEAT_INTERVAL_MS __attribute__((swift_name("HEARTBEAT_INTERVAL_MS")));
@property (readonly) NSString *KEY_DEVICE_ID __attribute__((swift_name("KEY_DEVICE_ID")));
@property (readonly) NSString *MESSAGE_TRY_AGAIN_LATER __attribute__((swift_name("MESSAGE_TRY_AGAIN_LATER")));
@property (readonly) NSString *PREFS_DEVICE __attribute__((swift_name("PREFS_DEVICE")));
@property (readonly) int64_t SESSION_STALE_MS __attribute__((swift_name("SESSION_STALE_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionDeviceRules.Decision")))
@interface SharedSessionDeviceRulesDecision : SharedKotlinEnum<SharedSessionDeviceRulesDecision *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSessionDeviceRulesDecision *redirectToLogin __attribute__((swift_name("redirectToLogin")));
@property (class, readonly) SharedSessionDeviceRulesDecision *signOutAndRedirect __attribute__((swift_name("signOutAndRedirect")));
@property (class, readonly) SharedSessionDeviceRulesDecision *claimAndAllow __attribute__((swift_name("claimAndAllow")));
@property (class, readonly) SharedSessionDeviceRulesDecision *allow __attribute__((swift_name("allow")));
@property (class, readonly) SharedSessionDeviceRulesDecision *rejectOtherDevice __attribute__((swift_name("rejectOtherDevice")));
+ (SharedKotlinArray<SharedSessionDeviceRulesDecision *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSessionDeviceRulesDecision *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SignupFlowRules")))
@interface SharedSignupFlowRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)signupFlowRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSignupFlowRules *shared __attribute__((swift_name("shared")));
- (NSArray<SharedSignupFlowRulesStep *> *)flowStepsStreakStepsEnabled:(BOOL)streakStepsEnabled __attribute__((swift_name("flowSteps(streakStepsEnabled:)")));
- (int32_t)progressForStep:(SharedSignupFlowRulesStep *)step streakStepsEnabled:(BOOL)streakStepsEnabled __attribute__((swift_name("progressFor(step:streakStepsEnabled:)")));
- (int32_t)questionCountStreakStepsEnabled:(BOOL)streakStepsEnabled __attribute__((swift_name("questionCount(streakStepsEnabled:)")));
- (BOOL)streakStepsEnabledForceTeacher:(BOOL)forceTeacher __attribute__((swift_name("streakStepsEnabled(forceTeacher:)")));
@property (readonly) NSArray<SharedInt *> *CHALLENGE_OPTIONS __attribute__((swift_name("CHALLENGE_OPTIONS")));
@property (readonly) int32_t DEFAULT_REMINDER_HOUR __attribute__((swift_name("DEFAULT_REMINDER_HOUR")));
@property (readonly) NSArray<SharedInt *> *GOAL_OPTIONS __attribute__((swift_name("GOAL_OPTIONS")));
@property (readonly) NSArray<NSString *> *SOROBAN_LEVELS __attribute__((swift_name("SOROBAN_LEVELS")));
@property (readonly) NSString *SOROBAN_LEVEL_FIELD __attribute__((swift_name("SOROBAN_LEVEL_FIELD")));
@property (readonly) int64_t SOROBAN_PENDING_MAX_AGE_MS __attribute__((swift_name("SOROBAN_PENDING_MAX_AGE_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SignupFlowRules.Step")))
@interface SharedSignupFlowRulesStep : SharedKotlinEnum<SharedSignupFlowRulesStep *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSignupFlowRulesStep *meet __attribute__((swift_name("meet")));
@property (class, readonly) SharedSignupFlowRulesStep *brief __attribute__((swift_name("brief")));
@property (class, readonly) SharedSignupFlowRulesStep *age __attribute__((swift_name("age")));
@property (class, readonly) SharedSignupFlowRulesStep *source __attribute__((swift_name("source")));
@property (class, readonly) SharedSignupFlowRulesStep *soroban __attribute__((swift_name("soroban")));
@property (class, readonly) SharedSignupFlowRulesStep *goal __attribute__((swift_name("goal")));
@property (class, readonly) SharedSignupFlowRulesStep *roadmap __attribute__((swift_name("roadmap")));
@property (class, readonly) SharedSignupFlowRulesStep *challengeIntro __attribute__((swift_name("challengeIntro")));
@property (class, readonly) SharedSignupFlowRulesStep *challenge __attribute__((swift_name("challenge")));
@property (class, readonly) SharedSignupFlowRulesStep *reminder __attribute__((swift_name("reminder")));
+ (SharedKotlinArray<SharedSignupFlowRulesStep *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSignupFlowRulesStep *> *entries __attribute__((swift_name("entries")));
@property (readonly) BOOL asks __attribute__((swift_name("asks")));
@property (readonly) BOOL isIntro __attribute__((swift_name("isIntro")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SplashRules")))
@interface SharedSplashRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)splashRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSplashRules *shared __attribute__((swift_name("shared")));
- (BOOL)hasEverHadAccountAccountEverCreated:(BOOL)accountEverCreated signedIn:(BOOL)signedIn legacyFirstTutorialShown:(BOOL)legacyFirstTutorialShown __attribute__((swift_name("hasEverHadAccount(accountEverCreated:signedIn:legacyFirstTutorialShown:)")));
- (SharedSplashRulesRoute *)routeIsOnline:(BOOL)isOnline loginStartEverShown:(BOOL)loginStartEverShown hasExistingLogin:(BOOL)hasExistingLogin __attribute__((swift_name("route(isOnline:loginStartEverShown:hasExistingLogin:)")));
- (BOOL)shouldPersistAccountFlagAccountEverCreated:(BOOL)accountEverCreated signedIn:(BOOL)signedIn legacyFirstTutorialShown:(BOOL)legacyFirstTutorialShown __attribute__((swift_name("shouldPersistAccountFlag(accountEverCreated:signedIn:legacyFirstTutorialShown:)")));
- (BOOL)showsFirstTutorialHadAccount:(BOOL)hadAccount __attribute__((swift_name("showsFirstTutorial(hadAccount:)")));
@property (readonly) int64_t BRAND_REVEAL_MS __attribute__((swift_name("BRAND_REVEAL_MS")));
@property (readonly) int64_t FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS __attribute__((swift_name("FORCE_UPDATE_MAX_WAIT_AFTER_SPLASH_MS")));
@property (readonly) int64_t FORCE_UPDATE_MAX_WAIT_DEEP_LINK_MS __attribute__((swift_name("FORCE_UPDATE_MAX_WAIT_DEEP_LINK_MS")));
@property (readonly) NSString *PREF_ACCOUNT_EVER_CREATED __attribute__((swift_name("PREF_ACCOUNT_EVER_CREATED")));
@property (readonly) NSString *PREF_LEGACY_FIRST_TUTORIAL_SHOWN __attribute__((swift_name("PREF_LEGACY_FIRST_TUTORIAL_SHOWN")));
@property (readonly) NSString *PREF_LOGIN_START_EVER_SHOWN __attribute__((swift_name("PREF_LOGIN_START_EVER_SHOWN")));
@property (readonly) int64_t SPLASH_ANIMATION_MS __attribute__((swift_name("SPLASH_ANIMATION_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SplashRules.Route")))
@interface SharedSplashRulesRoute : SharedKotlinEnum<SharedSplashRulesRoute *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSplashRulesRoute *offlineMain __attribute__((swift_name("offlineMain")));
@property (class, readonly) SharedSplashRulesRoute *loginStart __attribute__((swift_name("loginStart")));
@property (class, readonly) SharedSplashRulesRoute *checkSingleDevice __attribute__((swift_name("checkSingleDevice")));
@property (class, readonly) SharedSplashRulesRoute *guestMain __attribute__((swift_name("guestMain")));
+ (SharedKotlinArray<SharedSplashRulesRoute *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSplashRulesRoute *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StudentQuestion")))
@interface SharedStudentQuestion : SharedBase
- (instancetype)initWithId:(NSString *)id studentUid:(NSString *)studentUid studentEmail:(NSString * _Nullable)studentEmail screenshotStoragePath:(NSString *)screenshotStoragePath screenshotUrl:(NSString *)screenshotUrl mediaType:(NSString *)mediaType videoStoragePath:(NSString * _Nullable)videoStoragePath videoUrl:(NSString * _Nullable)videoUrl videoDurationSec:(SharedInt * _Nullable)videoDurationSec message:(NSString *)message previewText:(NSString *)previewText lastMessageAt:(SharedLong * _Nullable)lastMessageAt createdAt:(SharedLong * _Nullable)createdAt status:(NSString *)status claimedByTeacherUid:(NSString * _Nullable)claimedByTeacherUid claimedAt:(SharedLong * _Nullable)claimedAt resolvedAt:(SharedLong * _Nullable)resolvedAt deletedForUids:(NSArray<NSString *> * _Nullable)deletedForUids __attribute__((swift_name("init(id:studentUid:studentEmail:screenshotStoragePath:screenshotUrl:mediaType:videoStoragePath:videoUrl:videoDurationSec:message:previewText:lastMessageAt:createdAt:status:claimedByTeacherUid:claimedAt:resolvedAt:deletedForUids:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedStudentQuestionCompanion *companion __attribute__((swift_name("companion")));
- (SharedStudentQuestion *)doCopyId:(NSString *)id studentUid:(NSString *)studentUid studentEmail:(NSString * _Nullable)studentEmail screenshotStoragePath:(NSString *)screenshotStoragePath screenshotUrl:(NSString *)screenshotUrl mediaType:(NSString *)mediaType videoStoragePath:(NSString * _Nullable)videoStoragePath videoUrl:(NSString * _Nullable)videoUrl videoDurationSec:(SharedInt * _Nullable)videoDurationSec message:(NSString *)message previewText:(NSString *)previewText lastMessageAt:(SharedLong * _Nullable)lastMessageAt createdAt:(SharedLong * _Nullable)createdAt status:(NSString *)status claimedByTeacherUid:(NSString * _Nullable)claimedByTeacherUid claimedAt:(SharedLong * _Nullable)claimedAt resolvedAt:(SharedLong * _Nullable)resolvedAt deletedForUids:(NSArray<NSString *> * _Nullable)deletedForUids __attribute__((swift_name("doCopy(id:studentUid:studentEmail:screenshotStoragePath:screenshotUrl:mediaType:videoStoragePath:videoUrl:videoDurationSec:message:previewText:lastMessageAt:createdAt:status:claimedByTeacherUid:claimedAt:resolvedAt:deletedForUids:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSDictionary<NSString *, id> *)toMap __attribute__((swift_name("toMap()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable claimedAt __attribute__((swift_name("claimedAt")));
@property (readonly) NSString * _Nullable claimedByTeacherUid __attribute__((swift_name("claimedByTeacherUid")));
@property (readonly) SharedLong * _Nullable createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSArray<NSString *> * _Nullable deletedForUids __attribute__((swift_name("deletedForUids")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) SharedLong * _Nullable lastMessageAt __attribute__((swift_name("lastMessageAt")));
@property (readonly) NSString *mediaType __attribute__((swift_name("mediaType")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@property (readonly) NSString *previewText __attribute__((swift_name("previewText")));
@property (readonly) SharedLong * _Nullable resolvedAt __attribute__((swift_name("resolvedAt")));
@property (readonly) NSString *screenshotStoragePath __attribute__((swift_name("screenshotStoragePath")));
@property (readonly) NSString *screenshotUrl __attribute__((swift_name("screenshotUrl")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString * _Nullable studentEmail __attribute__((swift_name("studentEmail")));
@property (readonly) NSString *studentUid __attribute__((swift_name("studentUid")));
@property (readonly) SharedInt * _Nullable videoDurationSec __attribute__((swift_name("videoDurationSec")));
@property (readonly) NSString * _Nullable videoStoragePath __attribute__((swift_name("videoStoragePath")));
@property (readonly) NSString * _Nullable videoUrl __attribute__((swift_name("videoUrl")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("StudentQuestion.Companion")))
@interface SharedStudentQuestionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedStudentQuestionCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)docPathQuestionId:(NSString *)questionId __attribute__((swift_name("docPath(questionId:)")));
- (SharedStudentQuestion *)fromMapId:(NSString *)id data:(NSDictionary<NSString *, id> *)data __attribute__((swift_name("fromMap(id:data:)")));
- (NSString *)messagesPathQuestionId:(NSString *)questionId __attribute__((swift_name("messagesPath(questionId:)")));
@property (readonly) NSString *COLLECTION __attribute__((swift_name("COLLECTION")));
@property (readonly) NSString *FIELD_CLAIMED_AT __attribute__((swift_name("FIELD_CLAIMED_AT")));
@property (readonly) NSString *FIELD_CLAIMED_BY_TEACHER_UID __attribute__((swift_name("FIELD_CLAIMED_BY_TEACHER_UID")));
@property (readonly) NSString *FIELD_CREATED_AT __attribute__((swift_name("FIELD_CREATED_AT")));
@property (readonly) NSString *FIELD_DELETED_FOR_UIDS __attribute__((swift_name("FIELD_DELETED_FOR_UIDS")));
@property (readonly) NSString *FIELD_LAST_MESSAGE_AT __attribute__((swift_name("FIELD_LAST_MESSAGE_AT")));
@property (readonly) NSString *FIELD_MEDIA_TYPE __attribute__((swift_name("FIELD_MEDIA_TYPE")));
@property (readonly) NSString *FIELD_MESSAGE __attribute__((swift_name("FIELD_MESSAGE")));
@property (readonly) NSString *FIELD_PREVIEW_TEXT __attribute__((swift_name("FIELD_PREVIEW_TEXT")));
@property (readonly) NSString *FIELD_RESOLVED_AT __attribute__((swift_name("FIELD_RESOLVED_AT")));
@property (readonly) NSString *FIELD_SCREENSHOT_STORAGE_PATH __attribute__((swift_name("FIELD_SCREENSHOT_STORAGE_PATH")));
@property (readonly) NSString *FIELD_SCREENSHOT_URL __attribute__((swift_name("FIELD_SCREENSHOT_URL")));
@property (readonly) NSString *FIELD_STATUS __attribute__((swift_name("FIELD_STATUS")));
@property (readonly) NSString *FIELD_STUDENT_EMAIL __attribute__((swift_name("FIELD_STUDENT_EMAIL")));
@property (readonly) NSString *FIELD_STUDENT_UID __attribute__((swift_name("FIELD_STUDENT_UID")));
@property (readonly) NSString *FIELD_VIDEO_DURATION_SEC __attribute__((swift_name("FIELD_VIDEO_DURATION_SEC")));
@property (readonly) NSString *FIELD_VIDEO_STORAGE_PATH __attribute__((swift_name("FIELD_VIDEO_STORAGE_PATH")));
@property (readonly) NSString *FIELD_VIDEO_URL __attribute__((swift_name("FIELD_VIDEO_URL")));
@property (readonly) NSString *MEDIA_TYPE_IMAGE __attribute__((swift_name("MEDIA_TYPE_IMAGE")));
@property (readonly) NSString *MEDIA_TYPE_VIDEO __attribute__((swift_name("MEDIA_TYPE_VIDEO")));
@property (readonly) NSString *STATUS_CLAIMED __attribute__((swift_name("STATUS_CLAIMED")));
@property (readonly) NSString *STATUS_EXPIRED __attribute__((swift_name("STATUS_EXPIRED")));
@property (readonly) NSString *STATUS_PENDING __attribute__((swift_name("STATUS_PENDING")));
@property (readonly) NSString *STATUS_RESOLVED __attribute__((swift_name("STATUS_RESOLVED")));
@property (readonly) NSString *SUBCOLLECTION_MESSAGES __attribute__((swift_name("SUBCOLLECTION_MESSAGES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TeacherLoginRules")))
@interface SharedTeacherLoginRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)teacherLoginRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTeacherLoginRules *shared __attribute__((swift_name("shared")));
- (NSString * _Nullable)validateForgotPasswordEmailRawEmail:(NSString * _Nullable)rawEmail __attribute__((swift_name("validateForgotPasswordEmail(rawEmail:)")));
- (NSString * _Nullable)validateLoginRawEmail:(NSString *)rawEmail password:(NSString *)password __attribute__((swift_name("validateLogin(rawEmail:password:)")));
- (NSString * _Nullable)validateNewPasswordNewPassword:(NSString *)newPassword confirm:(NSString *)confirm __attribute__((swift_name("validateNewPassword(newPassword:confirm:)")));
@property (readonly) NSString *MESSAGE_PASSWORD_UPDATED_LOGIN_MANUALLY __attribute__((swift_name("MESSAGE_PASSWORD_UPDATED_LOGIN_MANUALLY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserAskQuestionRestriction")))
@interface SharedUserAskQuestionRestriction : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)userAskQuestionRestriction __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserAskQuestionRestriction *shared __attribute__((swift_name("shared")));
- (BOOL)isRestrictedUserData:(NSDictionary<NSString *, id> *)userData nowMs:(int64_t)nowMs __attribute__((swift_name("isRestricted(userData:nowMs:)")));
- (BOOL)isRestrictedBanned:(SharedBoolean * _Nullable)banned restrictedUntilMs:(SharedLong * _Nullable)restrictedUntilMs nowMs:(int64_t)nowMs __attribute__((swift_name("isRestricted(banned:restrictedUntilMs:nowMs:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserIdRules")))
@interface SharedUserIdRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)userIdRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserIdRules *shared __attribute__((swift_name("shared")));
- (NSString *)generateCandidate __attribute__((swift_name("generateCandidate()")));
- (NSString *)generateFallbackNowMs:(int64_t)nowMs __attribute__((swift_name("generateFallback(nowMs:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserInfoRules")))
@interface SharedUserInfoRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)userInfoRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserInfoRules *shared __attribute__((swift_name("shared")));
- (int32_t)birthYearFromAgeAge:(int32_t)age currentYear:(int32_t)currentYear __attribute__((swift_name("birthYearFromAge(age:currentYear:)")));
- (int32_t)birthYearFromRawAgeRawAge:(NSString * _Nullable)rawAge currentYear:(int32_t)currentYear __attribute__((swift_name("birthYearFromRawAge(rawAge:currentYear:)")));
- (BOOL)isValidAgeRawAge:(NSString * _Nullable)rawAge __attribute__((swift_name("isValidAge(rawAge:)")));
- (SharedInt * _Nullable)parseAgeRawAge:(NSString * _Nullable)rawAge __attribute__((swift_name("parseAge(rawAge:)")));
@property (readonly) int32_t AGE_MAX_LENGTH __attribute__((swift_name("AGE_MAX_LENGTH")));
@property (readonly) int32_t MAX_AGE __attribute__((swift_name("MAX_AGE")));
@property (readonly) NSString *MESSAGE_EMAIL_NOT_REGISTERED_REDIRECT __attribute__((swift_name("MESSAGE_EMAIL_NOT_REGISTERED_REDIRECT")));
@property (readonly) NSString *MESSAGE_INVALID_AGE __attribute__((swift_name("MESSAGE_INVALID_AGE")));
@property (readonly) int32_t MIN_AGE __attribute__((swift_name("MIN_AGE")));
@property (readonly) NSArray<NSString *> *SOURCE_DISPLAY_NAMES_TR __attribute__((swift_name("SOURCE_DISPLAY_NAMES_TR")));
@property (readonly) NSArray<NSString *> *SOURCE_NAMES __attribute__((swift_name("SOURCE_NAMES")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AdRules")))
@interface SharedAdRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)adRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAdRules *shared __attribute__((swift_name("shared")));
- (BOOL)canShowRewardedAdUid:(NSString * _Nullable)uid __attribute__((swift_name("canShowRewardedAd(uid:)")));
- (NSString *)interstitialAdUnitIdIsDebug:(BOOL)isDebug __attribute__((swift_name("interstitialAdUnitId(isDebug:)")));
- (BOOL)isRetryableErrorCode:(int32_t)errorCode __attribute__((swift_name("isRetryable(errorCode:)")));
- (SharedLong * _Nullable)nextRetryDelayMsErrorCode:(int32_t)errorCode attempt:(int32_t)attempt __attribute__((swift_name("nextRetryDelayMs(errorCode:attempt:)")));
- (BOOL)shouldLogLoadFailureAttempt:(int32_t)attempt __attribute__((swift_name("shouldLogLoadFailure(attempt:)")));
- (BOOL)shouldShowAdSkipAfterRewardedShowAdSkipAfter:(BOOL)showAdSkipAfter hostSupportsDialogs:(BOOL)hostSupportsDialogs __attribute__((swift_name("shouldShowAdSkipAfterRewarded(showAdSkipAfter:hostSupportsDialogs:)")));
- (NSString *)ssvCustomDataUid:(NSString *)uid nonce:(NSString *)nonce __attribute__((swift_name("ssvCustomData(uid:nonce:)")));
@property (readonly) NSString *ANDROID_INTERSTITIAL_AD_UNIT_ID __attribute__((swift_name("ANDROID_INTERSTITIAL_AD_UNIT_ID")));
@property (readonly) NSString *ANDROID_REWARDED_AD_UNIT_ID __attribute__((swift_name("ANDROID_REWARDED_AD_UNIT_ID")));
@property (readonly) int32_t ERROR_CODE_INTERNAL_ERROR __attribute__((swift_name("ERROR_CODE_INTERNAL_ERROR")));
@property (readonly) int32_t ERROR_CODE_INVALID_REQUEST __attribute__((swift_name("ERROR_CODE_INVALID_REQUEST")));
@property (readonly) int32_t ERROR_CODE_MEDIATION_NO_FILL __attribute__((swift_name("ERROR_CODE_MEDIATION_NO_FILL")));
@property (readonly) int32_t ERROR_CODE_NETWORK_ERROR __attribute__((swift_name("ERROR_CODE_NETWORK_ERROR")));
@property (readonly) int32_t ERROR_CODE_NO_FILL __attribute__((swift_name("ERROR_CODE_NO_FILL")));
@property (readonly) NSArray<SharedLong *> *RETRY_DELAYS_MS __attribute__((swift_name("RETRY_DELAYS_MS")));
@property (readonly) NSString *TAG_AD_SKIP __attribute__((swift_name("TAG_AD_SKIP")));
@property (readonly) NSString *TEST_INTERSTITIAL_AD_UNIT_ID __attribute__((swift_name("TEST_INTERSTITIAL_AD_UNIT_ID")));
@property (readonly) NSString *TOAST_AD_NOT_READY __attribute__((swift_name("TOAST_AD_NOT_READY")));
@property (readonly) NSString *TOAST_LOGIN_REQUIRED __attribute__((swift_name("TOAST_LOGIN_REQUIRED")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AdRules.AdSkipPolicy")))
@interface SharedAdRulesAdSkipPolicy : SharedBase
- (instancetype)initWithStore:(id<SharedOyunKeyValueStore>)store nowMs:(SharedLong *(^)(void))nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone debugEveryAd:(BOOL)debugEveryAd __attribute__((swift_name("init(store:nowMs:timeZone:debugEveryAd:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAdRulesAdSkipPolicyCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)onAdClosedEligible:(BOOL)eligible __attribute__((swift_name("onAdClosed(eligible:)")));
- (void)resetForDebug __attribute__((swift_name("resetForDebug()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AdRules.AdSkipPolicyCompanion")))
@interface SharedAdRulesAdSkipPolicyCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAdRulesAdSkipPolicyCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t ADS_PER_PANEL __attribute__((swift_name("ADS_PER_PANEL")));
@property (readonly) NSString *KEY_ADS_SINCE_PANEL __attribute__((swift_name("KEY_ADS_SINCE_PANEL")));
@property (readonly) NSString *KEY_DAY_COUNT __attribute__((swift_name("KEY_DAY_COUNT")));
@property (readonly) NSString *KEY_DAY_ID __attribute__((swift_name("KEY_DAY_ID")));
@property (readonly) NSString *KEY_LAST_PANEL_MS __attribute__((swift_name("KEY_LAST_PANEL_MS")));
@property (readonly) int32_t MAX_PANELS_PER_DAY __attribute__((swift_name("MAX_PANELS_PER_DAY")));
@property (readonly) int64_t MIN_GAP_MS __attribute__((swift_name("MIN_GAP_MS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AdRules.InterstitialCounter")))
@interface SharedAdRulesInterstitialCounter : SharedBase
- (instancetype)initWithInitialCount:(int32_t)initialCount __attribute__((swift_name("init(initialCount:)"))) __attribute__((objc_designated_initializer));
- (BOOL)onInterstitialDismissedShowAdSkipAfter:(BOOL)showAdSkipAfter hostSupportsDialogs:(BOOL)hostSupportsDialogs __attribute__((swift_name("onInterstitialDismissed(showAdSkipAfter:hostSupportsDialogs:)")));
@property (readonly) int32_t interstitialAdShownCount __attribute__((swift_name("interstitialAdShownCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AgeConsentManager")))
@interface SharedAgeConsentManager : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)ageConsentManager __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAgeConsentManager *shared __attribute__((swift_name("shared")));
- (SharedAgeConsentManagerAdRequestTags *)adRequestTagsTreatment:(SharedAgeConsentManagerAgeTreatment *)treatment __attribute__((swift_name("adRequestTags(treatment:)")));
- (BOOL)analyticsAdConsentGrantedTreatment:(SharedAgeConsentManagerAgeTreatment *)treatment __attribute__((swift_name("analyticsAdConsentGranted(treatment:)")));
- (int32_t)birthYearFromDocDocExists:(BOOL)docExists containsBirthYear:(BOOL)containsBirthYear birthYearValue:(SharedLong * _Nullable)birthYearValue __attribute__((swift_name("birthYearFromDoc(docExists:containsBirthYear:birthYearValue:)")));
- (BOOL)consentUnderAgeTreatment:(SharedAgeConsentManagerAgeTreatment *)treatment __attribute__((swift_name("consentUnderAge(treatment:)")));
- (int32_t)getDigitalConsentAgeForCountryCountryCode:(NSString *)countryCode __attribute__((swift_name("getDigitalConsentAgeForCountry(countryCode:)")));
- (NSString *)resolveCountryCodeSimCountryIso:(NSString * _Nullable)simCountryIso networkCountryIso:(NSString * _Nullable)networkCountryIso localeCountry:(NSString * _Nullable)localeCountry __attribute__((swift_name("resolveCountryCode(simCountryIso:networkCountryIso:localeCountry:)")));
- (BOOL)shouldRevokeAnalyticsAdConsentConsentUid:(NSString * _Nullable)consentUid currentUid:(NSString * _Nullable)currentUid __attribute__((swift_name("shouldRevokeAnalyticsAdConsent(consentUid:currentUid:)")));
- (SharedAgeConsentManagerAgeTreatment *)treatmentForIsLoggedIn:(BOOL)isLoggedIn birthYear:(int32_t)birthYear currentYear:(int32_t)currentYear countryCode:(NSString *)countryCode __attribute__((swift_name("treatmentFor(isLoggedIn:birthYear:currentYear:countryCode:)")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *DEBUG_TEST_DEVICE_ID __attribute__((swift_name("DEBUG_TEST_DEVICE_ID")));
@property (readonly) NSString *FALLBACK_COUNTRY __attribute__((swift_name("FALLBACK_COUNTRY")));
@property (readonly) NSString *FIELD_BIRTH_YEAR __attribute__((swift_name("FIELD_BIRTH_YEAR")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AgeConsentManager.AdRequestTags")))
@interface SharedAgeConsentManagerAdRequestTags : SharedBase
- (instancetype)initWithChildDirectedTreatment:(BOOL)childDirectedTreatment underAgeOfConsent:(BOOL)underAgeOfConsent __attribute__((swift_name("init(childDirectedTreatment:underAgeOfConsent:)"))) __attribute__((objc_designated_initializer));
- (SharedAgeConsentManagerAdRequestTags *)doCopyChildDirectedTreatment:(BOOL)childDirectedTreatment underAgeOfConsent:(BOOL)underAgeOfConsent __attribute__((swift_name("doCopy(childDirectedTreatment:underAgeOfConsent:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL childDirectedTreatment __attribute__((swift_name("childDirectedTreatment")));
@property (readonly) BOOL underAgeOfConsent __attribute__((swift_name("underAgeOfConsent")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AgeConsentManager.AgeTreatment")))
@interface SharedAgeConsentManagerAgeTreatment : SharedKotlinEnum<SharedAgeConsentManagerAgeTreatment *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAgeConsentManagerAgeTreatment *child __attribute__((swift_name("child")));
@property (class, readonly) SharedAgeConsentManagerAgeTreatment *teen __attribute__((swift_name("teen")));
@property (class, readonly) SharedAgeConsentManagerAgeTreatment *unspecified __attribute__((swift_name("unspecified")));
+ (SharedKotlinArray<SharedAgeConsentManagerAgeTreatment *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAgeConsentManagerAgeTreatment *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AnimatedQuest")))
@interface SharedAnimatedQuest : SharedBase
- (instancetype)initWithMissionId:(NSString *)missionId window:(SharedMissionWindow *)window isClaimed:(BOOL)isClaimed titleResId:(NSString *)titleResId iconRes:(NSString *)iconRes target:(int32_t)target fromCount:(int32_t)fromCount toCount:(int32_t)toCount staggerIndex:(int32_t)staggerIndex __attribute__((swift_name("init(missionId:window:isClaimed:titleResId:iconRes:target:fromCount:toCount:staggerIndex:)"))) __attribute__((objc_designated_initializer));
- (SharedAnimatedQuest *)doCopyMissionId:(NSString *)missionId window:(SharedMissionWindow *)window isClaimed:(BOOL)isClaimed titleResId:(NSString *)titleResId iconRes:(NSString *)iconRes target:(int32_t)target fromCount:(int32_t)fromCount toCount:(int32_t)toCount staggerIndex:(int32_t)staggerIndex __attribute__((swift_name("doCopy(missionId:window:isClaimed:titleResId:iconRes:target:fromCount:toCount:staggerIndex:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (int32_t)estimatedCountAtPct:(float)pct __attribute__((swift_name("estimatedCountAt(pct:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedMissionQuestState *)stateAtPct:(float)pct __attribute__((swift_name("stateAt(pct:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL canClaim __attribute__((swift_name("canClaim")));
@property (readonly) float endPct __attribute__((swift_name("endPct")));
@property (readonly) int32_t fromCount __attribute__((swift_name("fromCount")));
@property (readonly) NSString *iconRes __attribute__((swift_name("iconRes")));
@property (readonly) BOOL isClaimed __attribute__((swift_name("isClaimed")));
@property (readonly) NSString *missionId __attribute__((swift_name("missionId")));
@property (readonly) BOOL shouldAnimate __attribute__((swift_name("shouldAnimate")));
@property (readonly) int32_t staggerIndex __attribute__((swift_name("staggerIndex")));
@property (readonly) int64_t startDelayMs __attribute__((swift_name("startDelayMs")));
@property (readonly) float startPct __attribute__((swift_name("startPct")));
@property (readonly) int32_t target __attribute__((swift_name("target")));
@property (readonly) NSString *titleResId __attribute__((swift_name("titleResId")));
@property (readonly) int32_t toCount __attribute__((swift_name("toCount")));
@property (readonly) SharedMissionWindow *window __attribute__((swift_name("window")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppRatingManager")))
@interface SharedAppRatingManager : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs uid:(NSString * _Nullable (^)(void))uid nowMs:(SharedLong *(^)(void))nowMs __attribute__((swift_name("init(prefs:uid:nowMs:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAppRatingManagerCompanion *companion __attribute__((swift_name("companion")));
- (SharedRatingPromptDecision *)checkAndShowRatingPromptCompletedLessonCount:(int32_t)completedLessonCount adSkipShowing:(BOOL)adSkipShowing cupPathRevealPending:(BOOL)cupPathRevealPending ratingDialogShowing:(BOOL)ratingDialogShowing __attribute__((swift_name("checkAndShowRatingPrompt(completedLessonCount:adSkipShowing:cupPathRevealPending:ratingDialogShowing:)")));
- (void)markAsRated __attribute__((swift_name("markAsRated()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppRatingManager.Companion")))
@interface SharedAppRatingManagerCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppRatingManagerCompanion *shared __attribute__((swift_name("shared")));
- (BOOL)canSubmitFeedbackFeedbackText:(NSString *)feedbackText __attribute__((swift_name("canSubmitFeedback(feedbackText:)")));
- (NSDictionary<NSString *, id> *)feedbackDataUid:(NSString * _Nullable)uid stars:(int32_t)stars feedback:(NSString *)feedback __attribute__((swift_name("feedbackData(uid:stars:feedback:)")));
- (NSString *)feedbackDocIdUid:(NSString * _Nullable)uid __attribute__((swift_name("feedbackDocId(uid:)")));
- (NSString *)hasRatedKeyUid:(NSString * _Nullable)uid __attribute__((swift_name("hasRatedKey(uid:)")));
- (BOOL)isStarOnIndex:(int32_t)index rating:(int32_t)rating __attribute__((swift_name("isStarOn(index:rating:)")));
- (BOOL)shouldLaunchStoreReviewSelectedRating:(int32_t)selectedRating __attribute__((swift_name("shouldLaunchStoreReview(selectedRating:)")));
- (NSString *)starDrawableIndex:(int32_t)index rating:(int32_t)rating __attribute__((swift_name("starDrawable(index:rating:)")));
@property (readonly) NSString *ANONYMOUS_UID __attribute__((swift_name("ANONYMOUS_UID")));
@property (readonly) NSString *COLLECTION_RATING_FEEDBACK __attribute__((swift_name("COLLECTION_RATING_FEEDBACK")));
@property (readonly) NSString *FIELD_FEEDBACK __attribute__((swift_name("FIELD_FEEDBACK")));
@property (readonly) NSString *FIELD_STARS __attribute__((swift_name("FIELD_STARS")));
@property (readonly) NSString *FIELD_TIMESTAMP __attribute__((swift_name("FIELD_TIMESTAMP")));
@property (readonly) NSString *FIELD_UID __attribute__((swift_name("FIELD_UID")));
@property (readonly) int32_t MIN_COMPLETED_LESSONS __attribute__((swift_name("MIN_COMPLETED_LESSONS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) int64_t PROMPT_DEBOUNCE_MS __attribute__((swift_name("PROMPT_DEBOUNCE_MS")));
@property (readonly) int64_t STAR_SELECT_DELAY_MS __attribute__((swift_name("STAR_SELECT_DELAY_MS")));
@property (readonly) int32_t STORE_REVIEW_MIN_STARS __attribute__((swift_name("STORE_REVIEW_MIN_STARS")));
@property (readonly) NSString *SUBTITLE_LOW_RATING __attribute__((swift_name("SUBTITLE_LOW_RATING")));
@property (readonly) NSString *TAG_AD_SKIP __attribute__((swift_name("TAG_AD_SKIP")));
@property (readonly) NSString *TAG_RATING_DIALOG __attribute__((swift_name("TAG_RATING_DIALOG")));
@property (readonly) NSString *TITLE_LOW_RATING __attribute__((swift_name("TITLE_LOW_RATING")));
@property (readonly) NSString *TOAST_FEEDBACK_EMPTY __attribute__((swift_name("TOAST_FEEDBACK_EMPTY")));
@property (readonly) NSString *TOAST_FEEDBACK_THANKS __attribute__((swift_name("TOAST_FEEDBACK_THANKS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BillingCatalog")))
@interface SharedBillingCatalog : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)billingCatalog __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBillingCatalog *shared __attribute__((swift_name("shared")));
- (NSString *)subscriptionForPlanNamePlanName:(NSString *)planName __attribute__((swift_name("subscriptionForPlanName(planName:)")));
- (int32_t)subscriptionRankProductId:(NSString *)productId __attribute__((swift_name("subscriptionRank(productId:)")));
@property (readonly) NSString *CREDITS_LARGE __attribute__((swift_name("CREDITS_LARGE")));
@property (readonly) NSString *CREDITS_MEDIUM __attribute__((swift_name("CREDITS_MEDIUM")));
@property (readonly) NSString *CREDITS_SMALL __attribute__((swift_name("CREDITS_SMALL")));
@property (readonly) NSString *GOLD_LARGE __attribute__((swift_name("GOLD_LARGE")));
@property (readonly) NSString *GOLD_MEDIUM __attribute__((swift_name("GOLD_MEDIUM")));
@property (readonly) NSString *GOLD_SMALL __attribute__((swift_name("GOLD_SMALL")));
@property (readonly) NSString *KEYS_LARGE __attribute__((swift_name("KEYS_LARGE")));
@property (readonly) NSString *KEYS_MEDIUM __attribute__((swift_name("KEYS_MEDIUM")));
@property (readonly) NSString *KEYS_SMALL __attribute__((swift_name("KEYS_SMALL")));
@property (readonly) NSString *SOBI_CREDITS_120 __attribute__((swift_name("SOBI_CREDITS_120")));
@property (readonly) NSString *SOBI_CREDITS_20 __attribute__((swift_name("SOBI_CREDITS_20")));
@property (readonly) NSString *SOBI_CREDITS_50 __attribute__((swift_name("SOBI_CREDITS_50")));
@property (readonly) NSString *SUB_LITE __attribute__((swift_name("SUB_LITE")));
@property (readonly) NSString *SUB_PRO __attribute__((swift_name("SUB_PRO")));
@property (readonly) NSArray<NSString *> *consumables __attribute__((swift_name("consumables")));
@property (readonly) NSArray<NSString *> *subscriptions __attribute__((swift_name("subscriptions")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestCrystalPolicy")))
@interface SharedChestCrystalPolicy : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestCrystalPolicy __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestCrystalPolicy *shared __attribute__((swift_name("shared")));
- (SharedChestRewardOutcome *)outcomeFromServerRewardType:(NSString *)rewardType amount:(int32_t)amount __attribute__((swift_name("outcomeFromServer(rewardType:amount:)")));
@property (readonly) NSString *FALLBACK_VIDEO __attribute__((swift_name("FALLBACK_VIDEO")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestRarity")))
@interface SharedChestRarity : SharedKotlinEnum<SharedChestRarity *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedChestRarityCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedChestRarity *common __attribute__((swift_name("common")));
@property (class, readonly) SharedChestRarity *rare __attribute__((swift_name("rare")));
@property (class, readonly) SharedChestRarity *epic __attribute__((swift_name("epic")));
@property (class, readonly) SharedChestRarity *legendary __attribute__((swift_name("legendary")));
+ (SharedKotlinArray<SharedChestRarity *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedChestRarity *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *drawableRes __attribute__((swift_name("drawableRes")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t labelColor __attribute__((swift_name("labelColor")));
@property (readonly) NSString *openDrawableRes __attribute__((swift_name("openDrawableRes")));
@property (readonly) SharedRarityTheme *theme __attribute__((swift_name("theme")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestRarity.Companion")))
@interface SharedChestRarityCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestRarityCompanion *shared __attribute__((swift_name("shared")));
- (SharedChestRarity * _Nullable)fromNameOrNullName:(NSString * _Nullable)name __attribute__((swift_name("fromNameOrNull(name:)")));
- (SharedChestRarity *)startRarityFromArgumentName:(NSString * _Nullable)name __attribute__((swift_name("startRarityFromArgument(name:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestResultLogic_")))
@interface SharedChestResultLogic_ : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestResultLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestResultLogic_ *shared __attribute__((swift_name("shared")));
- (SharedCupIconChange * _Nullable)changeCupIconLessonItem:(id<SharedOyunLessonItem>)lessonItem globalPartId:(int32_t)globalPartId __attribute__((swift_name("changeCupIcon(lessonItem:globalPartId:)")));
- (NSString *)formatSecondsSeconds:(int32_t)seconds __attribute__((swift_name("formatSeconds(seconds:)")));
- (NSString *)lessonProgressKeyType:(int32_t)type id:(SharedInt * _Nullable)id partId:(SharedInt * _Nullable)partId title:(NSString *)title fallbackIndex:(int32_t)fallbackIndex __attribute__((swift_name("lessonProgressKey(type:id:partId:title:fallbackIndex:)")));
- (SharedInt * _Nullable)parseTimeToSecondsValue:(NSString *)value __attribute__((swift_name("parseTimeToSeconds(value:)")));
- (int32_t)resolveCupPoint2ThresholdCurrentCupPoint2:(SharedInt * _Nullable)currentCupPoint2 templateCupPoint2:(SharedInt * _Nullable)templateCupPoint2 scoreCap:(int32_t)scoreCap __attribute__((swift_name("resolveCupPoint2Threshold(currentCupPoint2:templateCupPoint2:scoreCap:)")));
- (int32_t)resolveScoreCapCurrentCupPoint1:(SharedInt * _Nullable)currentCupPoint1 templateCupPoint1:(SharedInt * _Nullable)templateCupPoint1 __attribute__((swift_name("resolveScoreCap(currentCupPoint1:templateCupPoint1:)")));
- (SharedScoreBarColorBlend *)scoreBarColorBlendAnimatedScore:(int32_t)animatedScore previousScore:(int32_t)previousScore scoreCap:(int32_t)scoreCap __attribute__((swift_name("scoreBarColorBlend(animatedScore:previousScore:scoreCap:)")));
- (float)scoreProgressAnimatedScore:(int32_t)animatedScore scoreCap:(int32_t)scoreCap __attribute__((swift_name("scoreProgress(animatedScore:scoreCap:)")));
- (BOOL)shouldIncrementChestRecordBreakMissionItemType:(int32_t)itemType record:(SharedInt * _Nullable)record toplamPuan:(int32_t)toplamPuan __attribute__((swift_name("shouldIncrementChestRecordBreakMission(itemType:record:toplamPuan:)")));
- (float)starMarkerRatioThreshold:(int32_t)threshold scoreCap:(int32_t)scoreCap __attribute__((swift_name("starMarkerRatio(threshold:scoreCap:)")));
- (float)starSoundRateTier:(int32_t)tier __attribute__((swift_name("starSoundRate(tier:)")));
- (NSArray<SharedInt *> *)starThresholdsCupPoint2Threshold:(int32_t)cupPoint2Threshold scoreCap:(int32_t)scoreCap __attribute__((swift_name("starThresholds(cupPoint2Threshold:scoreCap:)")));
@property (readonly) NSString *ARG_PENDING_CHEST_RECORD_BREAK_MISSION __attribute__((swift_name("ARG_PENDING_CHEST_RECORD_BREAK_MISSION")));
@property (readonly) NSString *COLOR_DARK_RED __attribute__((swift_name("COLOR_DARK_RED")));
@property (readonly) NSString *COLOR_GREEN __attribute__((swift_name("COLOR_GREEN")));
@property (readonly) NSString *COLOR_YELLOW __attribute__((swift_name("COLOR_YELLOW")));
@property (readonly) NSArray<NSString *> *animations __attribute__((swift_name("animations")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestResultStarTracker")))
@interface SharedChestResultStarTracker : SharedBase
- (instancetype)initWithCupPoint2Threshold:(int32_t)cupPoint2Threshold scoreCap:(int32_t)scoreCap __attribute__((swift_name("init(cupPoint2Threshold:scoreCap:)"))) __attribute__((objc_designated_initializer));
- (NSArray<SharedInt *> *)onScoreChangedPreviousScore:(int32_t)previousScore currentScore:(int32_t)currentScore __attribute__((swift_name("onScoreChanged(previousScore:currentScore:)")));
- (void)reset __attribute__((swift_name("reset()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestRewardDisplay")))
@interface SharedChestRewardDisplay : SharedBase
- (instancetype)initWithEarnedGold:(int32_t)earnedGold earnedKey:(int32_t)earnedKey textValue:(NSString *)textValue textDrawable:(NSString *)textDrawable coinDrawable:(NSString *)coinDrawable __attribute__((swift_name("init(earnedGold:earnedKey:textValue:textDrawable:coinDrawable:)"))) __attribute__((objc_designated_initializer));
- (SharedChestRewardDisplay *)doCopyEarnedGold:(int32_t)earnedGold earnedKey:(int32_t)earnedKey textValue:(NSString *)textValue textDrawable:(NSString *)textDrawable coinDrawable:(NSString *)coinDrawable __attribute__((swift_name("doCopy(earnedGold:earnedKey:textValue:textDrawable:coinDrawable:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *coinDrawable __attribute__((swift_name("coinDrawable")));
@property (readonly) int32_t earnedGold __attribute__((swift_name("earnedGold")));
@property (readonly) int32_t earnedKey __attribute__((swift_name("earnedKey")));
@property (readonly) NSString *textDrawable __attribute__((swift_name("textDrawable")));
@property (readonly) NSString *textValue __attribute__((swift_name("textValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestRewardOutcome")))
@interface SharedChestRewardOutcome : SharedBase
- (instancetype)initWithType:(SharedChestRewardType *)type amount:(int32_t)amount iconRes:(NSString *)iconRes label:(NSString *)label __attribute__((swift_name("init(type:amount:iconRes:label:)"))) __attribute__((objc_designated_initializer));
- (SharedChestRewardOutcome *)doCopyType:(SharedChestRewardType *)type amount:(int32_t)amount iconRes:(NSString *)iconRes label:(NSString *)label __attribute__((swift_name("doCopy(type:amount:iconRes:label:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t amount __attribute__((swift_name("amount")));
@property (readonly) NSString *iconRes __attribute__((swift_name("iconRes")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) SharedChestRewardType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestRewardType")))
@interface SharedChestRewardType : SharedKotlinEnum<SharedChestRewardType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedChestRewardType *gold __attribute__((swift_name("gold")));
@property (class, readonly) SharedChestRewardType *key __attribute__((swift_name("key")));
+ (SharedKotlinArray<SharedChestRewardType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedChestRewardType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestStepIndicator")))
@interface SharedChestStepIndicator : SharedKotlinEnum<SharedChestStepIndicator *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedChestStepIndicator *done __attribute__((swift_name("done")));
@property (class, readonly) SharedChestStepIndicator *active __attribute__((swift_name("active")));
@property (class, readonly) SharedChestStepIndicator *future __attribute__((swift_name("future")));
+ (SharedKotlinArray<SharedChestStepIndicator *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedChestStepIndicator *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestTapAction")))
@interface SharedChestTapAction : SharedKotlinEnum<SharedChestTapAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedChestTapAction *ignored __attribute__((swift_name("ignored")));
@property (class, readonly) SharedChestTapAction *step __attribute__((swift_name("step")));
@property (class, readonly) SharedChestTapAction *openChest __attribute__((swift_name("openChest")));
@property (class, readonly) SharedChestTapAction *close __attribute__((swift_name("close")));
+ (SharedKotlinArray<SharedChestTapAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedChestTapAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestTapResult")))
@interface SharedChestTapResult : SharedBase
- (instancetype)initWithAction:(SharedChestTapAction *)action rarityChanged:(BOOL)rarityChanged showOpenHint:(BOOL)showOpenHint soundRate:(float)soundRate __attribute__((swift_name("init(action:rarityChanged:showOpenHint:soundRate:)"))) __attribute__((objc_designated_initializer));
- (SharedChestTapResult *)doCopyAction:(SharedChestTapAction *)action rarityChanged:(BOOL)rarityChanged showOpenHint:(BOOL)showOpenHint soundRate:(float)soundRate __attribute__((swift_name("doCopy(action:rarityChanged:showOpenHint:soundRate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedChestTapAction *action __attribute__((swift_name("action")));
@property (readonly) BOOL rarityChanged __attribute__((swift_name("rarityChanged")));
@property (readonly) BOOL showOpenHint __attribute__((swift_name("showOpenHint")));
@property (readonly) float soundRate __attribute__((swift_name("soundRate")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ChestTypeProgressHelper_")))
@interface SharedChestTypeProgressHelper_ : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)chestTypeProgressHelper __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedChestTypeProgressHelper_ *shared __attribute__((swift_name("shared")));
- (NSString *)resolvedChestIconItem:(id<SharedOyunLessonItem>)item recordValue:(int32_t)recordValue globalPartId:(int32_t)globalPartId __attribute__((swift_name("resolvedChestIcon(item:recordValue:globalPartId:)")));
- (BOOL)shouldIncrementKarateForFirstThreeStarsItem:(id<SharedOyunLessonItem>)item recordScoreThisRun:(int32_t)recordScoreThisRun globalPartId:(int32_t)globalPartId __attribute__((swift_name("shouldIncrementKarateForFirstThreeStars(item:recordScoreThisRun:globalPartId:)")));
- (int32_t)starCountForChestIconIconRes:(NSString *)iconRes __attribute__((swift_name("starCountForChestIcon(iconRes:)")));
@property (readonly) int32_t BLINDING_STAR_PART_ID __attribute__((swift_name("BLINDING_STAR_PART_ID")));
@property (readonly) NSString *ICON_STAR_ON __attribute__((swift_name("ICON_STAR_ON")));
@property (readonly) NSString *ICON_TIER0 __attribute__((swift_name("ICON_TIER0")));
@property (readonly) NSString *ICON_TIER1 __attribute__((swift_name("ICON_TIER1")));
@property (readonly) NSString *ICON_TIER2 __attribute__((swift_name("ICON_TIER2")));
@property (readonly) NSString *ICON_TIER3 __attribute__((swift_name("ICON_TIER3")));
@property (readonly) int32_t ONE_STAR_THRESHOLD __attribute__((swift_name("ONE_STAR_THRESHOLD")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalBreakLogic")))
@interface SharedCrystalBreakLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)crystalBreakLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCrystalBreakLogic *shared __attribute__((swift_name("shared")));
- (SharedCrystalRarityBehavior *)behaviorForVideoName:(NSString *)videoName __attribute__((swift_name("behaviorFor(videoName:)")));
- (SharedCrystalRarityTier *)nextRarityTierForCheckpointCheckpointNumber:(int32_t)checkpointNumber currentTier:(SharedCrystalRarityTier *)currentTier behavior:(SharedCrystalRarityBehavior *)behavior random:(SharedKotlinRandom *)random __attribute__((swift_name("nextRarityTierForCheckpoint(checkpointNumber:currentTier:behavior:random:)")));
- (int32_t)rarityWaveStarCountTier:(SharedCrystalRarityTier *)tier __attribute__((swift_name("rarityWaveStarCount(tier:)")));
- (NSString *)resolveVideoNameRequestedVideoName:(NSString * _Nullable)requestedVideoName __attribute__((swift_name("resolveVideoName(requestedVideoName:)")));
@property (readonly) NSString *CLICK_SOUND __attribute__((swift_name("CLICK_SOUND")));
@property (readonly) float CLICK_SOUND_VOLUME __attribute__((swift_name("CLICK_SOUND_VOLUME")));
@property (readonly) int64_t RARITY_APPLY_DELAY_MS __attribute__((swift_name("RARITY_APPLY_DELAY_MS")));
@property (readonly) int64_t TICK_INTERVAL_MS __attribute__((swift_name("TICK_INTERVAL_MS")));
@property (readonly) float VIDEO_VOLUME __attribute__((swift_name("VIDEO_VOLUME")));
@property (readonly) NSArray<SharedLong *> *checkpointsMs __attribute__((swift_name("checkpointsMs")));
@property (readonly) NSArray<NSString *> *videoNames __attribute__((swift_name("videoNames")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalBreakState")))
@interface SharedCrystalBreakState : SharedBase
- (instancetype)initWithRequestedVideoName:(NSString * _Nullable)requestedVideoName __attribute__((swift_name("init(requestedVideoName:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithRequestedVideoName:(NSString * _Nullable)requestedVideoName random:(SharedKotlinRandom *)random __attribute__((swift_name("init(requestedVideoName:random:)"))) __attribute__((objc_designated_initializer));
- (BOOL)maybeUpdateRarityLabelPositionMs:(int64_t)positionMs __attribute__((swift_name("maybeUpdateRarityLabel(positionMs:)")));
- (void)onPlaybackStarted __attribute__((swift_name("onPlaybackStarted()")));
- (BOOL)onResumeWaveDelayElapsedContinueTapCount:(int32_t)continueTapCount __attribute__((swift_name("onResumeWaveDelayElapsed(continueTapCount:)")));
- (SharedCrystalTapResult *)onTapIsPlaying:(BOOL)isPlaying __attribute__((swift_name("onTap(isPlaying:)")));
- (SharedCrystalTickResult *)onTickPositionMs:(int64_t)positionMs isPlaying:(BOOL)isPlaying __attribute__((swift_name("onTick(positionMs:isPlaying:)")));
- (void)onVideoEnded __attribute__((swift_name("onVideoEnded()")));
@property (readonly) int32_t continueTapCount __attribute__((swift_name("continueTapCount")));
@property (readonly) SharedCrystalRarityTier *currentRarityTier __attribute__((swift_name("currentRarityTier")));
@property (readonly) BOOL hasPlaybackStarted __attribute__((swift_name("hasPlaybackStarted")));
@property (readonly) int32_t nextCheckpointIndex __attribute__((swift_name("nextCheckpointIndex")));
@property (readonly) int32_t pendingAutoContinues __attribute__((swift_name("pendingAutoContinues")));
@property (readonly) int32_t playPhaseTapCount __attribute__((swift_name("playPhaseTapCount")));
@property (readonly) int32_t rarityCheckpointAppliedCount __attribute__((swift_name("rarityCheckpointAppliedCount")));
@property (readonly) BOOL rarityLabelVisible __attribute__((swift_name("rarityLabelVisible")));
@property (readonly) BOOL shouldIdlePulse __attribute__((swift_name("shouldIdlePulse")));
@property (readonly) BOOL shouldTick __attribute__((swift_name("shouldTick")));
@property (readonly) int32_t tapWaveColor __attribute__((swift_name("tapWaveColor")));
@property (readonly) BOOL videoEnded __attribute__((swift_name("videoEnded")));
@property (readonly) NSString *videoName __attribute__((swift_name("videoName")));
@property (readonly) BOOL waitingTapToContinue __attribute__((swift_name("waitingTapToContinue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalRarityBehavior")))
@interface SharedCrystalRarityBehavior : SharedKotlinEnum<SharedCrystalRarityBehavior *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCrystalRarityBehavior *staticCommon __attribute__((swift_name("staticCommon")));
@property (class, readonly) SharedCrystalRarityBehavior *forceEpicOnSecondCheckpoint __attribute__((swift_name("forceEpicOnSecondCheckpoint")));
@property (class, readonly) SharedCrystalRarityBehavior *forceEpicFromFirstCheckpoint __attribute__((swift_name("forceEpicFromFirstCheckpoint")));
@property (class, readonly) SharedCrystalRarityBehavior *forceEpicThenDestansi __attribute__((swift_name("forceEpicThenDestansi")));
@property (class, readonly) SharedCrystalRarityBehavior *forceEpicThenLegendary __attribute__((swift_name("forceEpicThenLegendary")));
@property (class, readonly) SharedCrystalRarityBehavior *forceDestansiOnSecondCheckpoint __attribute__((swift_name("forceDestansiOnSecondCheckpoint")));
@property (class, readonly) SharedCrystalRarityBehavior *forceDestansiFromFirstCheckpoint __attribute__((swift_name("forceDestansiFromFirstCheckpoint")));
@property (class, readonly) SharedCrystalRarityBehavior *forceDestansiThenLegendary __attribute__((swift_name("forceDestansiThenLegendary")));
@property (class, readonly) SharedCrystalRarityBehavior *checkpointRandomProgress __attribute__((swift_name("checkpointRandomProgress")));
+ (SharedKotlinArray<SharedCrystalRarityBehavior *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCrystalRarityBehavior *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalRarityTier")))
@interface SharedCrystalRarityTier : SharedKotlinEnum<SharedCrystalRarityTier *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCrystalRarityTier *common __attribute__((swift_name("common")));
@property (class, readonly) SharedCrystalRarityTier *epic __attribute__((swift_name("epic")));
@property (class, readonly) SharedCrystalRarityTier *destansi __attribute__((swift_name("destansi")));
@property (class, readonly) SharedCrystalRarityTier *legendary __attribute__((swift_name("legendary")));
+ (SharedKotlinArray<SharedCrystalRarityTier *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCrystalRarityTier *> *entries __attribute__((swift_name("entries")));
@property (readonly) int32_t color __attribute__((swift_name("color")));
@property (readonly) NSString *label __attribute__((swift_name("label")));
@property (readonly) int32_t rank __attribute__((swift_name("rank")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalTapAction")))
@interface SharedCrystalTapAction : SharedKotlinEnum<SharedCrystalTapAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedCrystalTapAction *ignored __attribute__((swift_name("ignored")));
@property (class, readonly) SharedCrystalTapAction *tapWave __attribute__((swift_name("tapWave")));
@property (class, readonly) SharedCrystalTapAction *resume __attribute__((swift_name("resume")));
@property (class, readonly) SharedCrystalTapAction *dismiss __attribute__((swift_name("dismiss")));
+ (SharedKotlinArray<SharedCrystalTapAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedCrystalTapAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalTapResult")))
@interface SharedCrystalTapResult : SharedBase
- (instancetype)initWithAction:(SharedCrystalTapAction *)action resumeWaveForContinueTap:(int32_t)resumeWaveForContinueTap __attribute__((swift_name("init(action:resumeWaveForContinueTap:)"))) __attribute__((objc_designated_initializer));
- (SharedCrystalTapResult *)doCopyAction:(SharedCrystalTapAction *)action resumeWaveForContinueTap:(int32_t)resumeWaveForContinueTap __attribute__((swift_name("doCopy(action:resumeWaveForContinueTap:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedCrystalTapAction *action __attribute__((swift_name("action")));
@property (readonly) int32_t resumeWaveForContinueTap __attribute__((swift_name("resumeWaveForContinueTap")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CrystalTickResult")))
@interface SharedCrystalTickResult : SharedBase
- (instancetype)initWithKeepTicking:(BOOL)keepTicking pausePlayback:(BOOL)pausePlayback rarityChanged:(BOOL)rarityChanged resumeWaveForContinueTap:(int32_t)resumeWaveForContinueTap __attribute__((swift_name("init(keepTicking:pausePlayback:rarityChanged:resumeWaveForContinueTap:)"))) __attribute__((objc_designated_initializer));
- (SharedCrystalTickResult *)doCopyKeepTicking:(BOOL)keepTicking pausePlayback:(BOOL)pausePlayback rarityChanged:(BOOL)rarityChanged resumeWaveForContinueTap:(int32_t)resumeWaveForContinueTap __attribute__((swift_name("doCopy(keepTicking:pausePlayback:rarityChanged:resumeWaveForContinueTap:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL keepTicking __attribute__((swift_name("keepTicking")));
@property (readonly) BOOL pausePlayback __attribute__((swift_name("pausePlayback")));
@property (readonly) BOOL rarityChanged __attribute__((swift_name("rarityChanged")));
@property (readonly) int32_t resumeWaveForContinueTap __attribute__((swift_name("resumeWaveForContinueTap")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CupIconChange")))
@interface SharedCupIconChange : SharedBase
- (instancetype)initWithIcon:(NSString *)icon pendingChestStarGainAmount:(int32_t)pendingChestStarGainAmount __attribute__((swift_name("init(icon:pendingChestStarGainAmount:)"))) __attribute__((objc_designated_initializer));
- (SharedCupIconChange *)doCopyIcon:(NSString *)icon pendingChestStarGainAmount:(int32_t)pendingChestStarGainAmount __attribute__((swift_name("doCopy(icon:pendingChestStarGainAmount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *icon __attribute__((swift_name("icon")));
@property (readonly) int32_t pendingChestStarGainAmount __attribute__((swift_name("pendingChestStarGainAmount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionBrokenHeartAction")))
@interface SharedDailyQuestionBrokenHeartAction : SharedKotlinEnum<SharedDailyQuestionBrokenHeartAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedDailyQuestionBrokenHeartAction *playHeal __attribute__((swift_name("playHeal")));
@property (class, readonly) SharedDailyQuestionBrokenHeartAction *playBreakToHold116 __attribute__((swift_name("playBreakToHold116")));
@property (class, readonly) SharedDailyQuestionBrokenHeartAction *showHold116 __attribute__((swift_name("showHold116")));
@property (class, readonly) SharedDailyQuestionBrokenHeartAction *showFirstFrame __attribute__((swift_name("showFirstFrame")));
+ (SharedKotlinArray<SharedDailyQuestionBrokenHeartAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedDailyQuestionBrokenHeartAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionBrokenHeartStore")))
@interface SharedDailyQuestionBrokenHeartStore : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs uid:(NSString * _Nullable (^)(void))uid __attribute__((swift_name("init(prefs:uid:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedDailyQuestionBrokenHeartStoreCompanion *companion __attribute__((swift_name("companion")));
- (void)clearBrokenHold116PeriodKey:(NSString *)periodKey __attribute__((swift_name("clearBrokenHold116(periodKey:)")));
- (BOOL)consumeHealPlayPeriodKey:(NSString *)periodKey __attribute__((swift_name("consumeHealPlay(periodKey:)")));
- (BOOL)consumePlayRequestPeriodKey:(NSString *)periodKey __attribute__((swift_name("consumePlayRequest(periodKey:)")));
- (BOOL)isBrokenHold116PeriodKey:(NSString *)periodKey __attribute__((swift_name("isBrokenHold116(periodKey:)")));
- (void)requestHealPlayPeriodKey:(NSString *)periodKey __attribute__((swift_name("requestHealPlay(periodKey:)")));
- (void)requestPlayOnNextBindPeriodKey:(NSString *)periodKey __attribute__((swift_name("requestPlayOnNextBind(periodKey:)")));
- (void)resetForPeriodPeriodKey:(NSString *)periodKey __attribute__((swift_name("resetForPeriod(periodKey:)")));
- (void)setBrokenHold116PeriodKey:(NSString *)periodKey hold:(BOOL)hold __attribute__((swift_name("setBrokenHold116(periodKey:hold:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionBrokenHeartStore.Companion")))
@interface SharedDailyQuestionBrokenHeartStoreCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyQuestionBrokenHeartStoreCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)healPlayKeyUid:(NSString *)uid periodKey:(NSString *)periodKey __attribute__((swift_name("healPlayKey(uid:periodKey:)")));
- (NSString *)hold116KeyUid:(NSString *)uid periodKey:(NSString *)periodKey __attribute__((swift_name("hold116Key(uid:periodKey:)")));
- (NSString *)playKeyUid:(NSString *)uid periodKey:(NSString *)periodKey __attribute__((swift_name("playKey(uid:periodKey:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionCardClickAction")))
@interface SharedDailyQuestionCardClickAction : SharedKotlinEnum<SharedDailyQuestionCardClickAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedDailyQuestionCardClickAction *showChallengeCompleteToast __attribute__((swift_name("showChallengeCompleteToast")));
@property (class, readonly) SharedDailyQuestionCardClickAction *ignore __attribute__((swift_name("ignore")));
@property (class, readonly) SharedDailyQuestionCardClickAction *loadChallenge __attribute__((swift_name("loadChallenge")));
+ (SharedKotlinArray<SharedDailyQuestionCardClickAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedDailyQuestionCardClickAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionCardLogic")))
@interface SharedDailyQuestionCardLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dailyQuestionCardLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyQuestionCardLogic *shared __attribute__((swift_name("shared")));
- (SharedDailyQuestionBrokenHeartAction *)brokenHeartActionState:(SharedDailyQuestionCardUiState *)state store:(SharedDailyQuestionBrokenHeartStore *)store __attribute__((swift_name("brokenHeartAction(state:store:)")));
- (BOOL)canTapProgressUi:(SharedDailyQuestionCardUiState * _Nullable)ui __attribute__((swift_name("canTapProgress(ui:)")));
- (SharedDailyQuestionCardClickAction *)cardClickActionState:(SharedDailyQuestionCardUiState *)state __attribute__((swift_name("cardClickAction(state:)")));
- (SharedDailyQuestionChallengeClickAction *)challengeClickActionChallenge:(SharedDailyQuestionChallenge *)challenge __attribute__((swift_name("challengeClickAction(challenge:)")));
- (int64_t)countdownTickDelayMsRemainingMs:(int64_t)remainingMs __attribute__((swift_name("countdownTickDelayMs(remainingMs:)")));
- (NSString * _Nullable)focusLabelArgumentState:(SharedDailyQuestionCardUiState *)state __attribute__((swift_name("focusLabelArgument(state:)")));
- (BOOL)isProgressClaimTapReadyUi:(SharedDailyQuestionCardUiState * _Nullable)ui lastVisualPercent:(float)lastVisualPercent animatorRunning:(BOOL)animatorRunning __attribute__((swift_name("isProgressClaimTapReady(ui:lastVisualPercent:animatorRunning:)")));
- (SharedDailyQuestionProgressPlan *)progressPlanState:(SharedDailyQuestionCardUiState *)state lastShown:(SharedInt * _Nullable)lastShown __attribute__((swift_name("progressPlan(state:lastShown:)")));
- (BOOL)shouldRefreshOnPeriodRolloverCardPeriodKey:(NSString *)cardPeriodKey currentPeriodKey:(NSString *)currentPeriodKey nowMs:(int64_t)nowMs lastRefreshMs:(int64_t)lastRefreshMs __attribute__((swift_name("shouldRefreshOnPeriodRollover(cardPeriodKey:currentPeriodKey:nowMs:lastRefreshMs:)")));
- (SharedInt * _Nullable)solvedToPersistUi:(SharedDailyQuestionCardUiState *)ui __attribute__((swift_name("solvedToPersist(ui:)")));
@property (readonly) float CLAIM_READY_VISUAL_PERCENT __attribute__((swift_name("CLAIM_READY_VISUAL_PERCENT")));
@property (readonly) int64_t DAILY_PROGRESS_ANIM_DURATION_MS __attribute__((swift_name("DAILY_PROGRESS_ANIM_DURATION_MS")));
@property (readonly) int64_t PERIOD_ROLLOVER_REFRESH_MIN_INTERVAL_MS __attribute__((swift_name("PERIOD_ROLLOVER_REFRESH_MIN_INTERVAL_MS")));
@property (readonly) SharedChestRarity *REWARD_CHEST_START_RARITY __attribute__((swift_name("REWARD_CHEST_START_RARITY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionCardProgressAnimStore")))
@interface SharedDailyQuestionCardProgressAnimStore : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs uid:(NSString * _Nullable (^)(void))uid __attribute__((swift_name("init(prefs:uid:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedDailyQuestionCardProgressAnimStoreCompanion *companion __attribute__((swift_name("companion")));
- (SharedInt * _Nullable)getLastShownPeriodKey:(NSString *)periodKey __attribute__((swift_name("getLastShown(periodKey:)")));
- (void)setLastShownPeriodKey:(NSString *)periodKey solved:(int32_t)solved __attribute__((swift_name("setLastShown(periodKey:solved:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionCardProgressAnimStore.Companion")))
@interface SharedDailyQuestionCardProgressAnimStoreCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyQuestionCardProgressAnimStoreCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)prefsKeyUid:(NSString *)uid periodKey:(NSString *)periodKey __attribute__((swift_name("prefsKey(uid:periodKey:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionCardUiState")))
@interface SharedDailyQuestionCardUiState : SharedBase
- (instancetype)initWithPeriodKey:(NSString *)periodKey solvedCount:(int32_t)solvedCount totalCount:(int32_t)totalCount titleUnit:(NSString *)titleUnit difficulty:(NSString *)difficulty isComplete:(BOOL)isComplete rewardClaimed:(BOOL)rewardClaimed poolAvailable:(BOOL)poolAvailable pendingContinueSlotIndex:(SharedInt * _Nullable)pendingContinueSlotIndex globalSuccessRate:(SharedInt * _Nullable)globalSuccessRate isLoaded:(BOOL)isLoaded __attribute__((swift_name("init(periodKey:solvedCount:totalCount:titleUnit:difficulty:isComplete:rewardClaimed:poolAvailable:pendingContinueSlotIndex:globalSuccessRate:isLoaded:)"))) __attribute__((objc_designated_initializer));
- (SharedDailyQuestionCardUiState *)doCopyPeriodKey:(NSString *)periodKey solvedCount:(int32_t)solvedCount totalCount:(int32_t)totalCount titleUnit:(NSString *)titleUnit difficulty:(NSString *)difficulty isComplete:(BOOL)isComplete rewardClaimed:(BOOL)rewardClaimed poolAvailable:(BOOL)poolAvailable pendingContinueSlotIndex:(SharedInt * _Nullable)pendingContinueSlotIndex globalSuccessRate:(SharedInt * _Nullable)globalSuccessRate isLoaded:(BOOL)isLoaded __attribute__((swift_name("doCopy(periodKey:solvedCount:totalCount:titleUnit:difficulty:isComplete:rewardClaimed:poolAvailable:pendingContinueSlotIndex:globalSuccessRate:isLoaded:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (BOOL)shouldShowChallengeCompleteToast __attribute__((swift_name("shouldShowChallengeCompleteToast()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL canClaimReward __attribute__((swift_name("canClaimReward")));
@property (readonly) NSString *difficulty __attribute__((swift_name("difficulty")));
@property (readonly) SharedInt * _Nullable globalSuccessRate __attribute__((swift_name("globalSuccessRate")));
@property (readonly) BOOL isComplete __attribute__((swift_name("isComplete")));
@property (readonly) BOOL isLoaded __attribute__((swift_name("isLoaded")));
@property (readonly) BOOL needsDiamondContinue __attribute__((swift_name("needsDiamondContinue")));
@property (readonly) SharedInt * _Nullable pendingContinueSlotIndex __attribute__((swift_name("pendingContinueSlotIndex")));
@property (readonly) NSString *periodKey __attribute__((swift_name("periodKey")));
@property (readonly) BOOL poolAvailable __attribute__((swift_name("poolAvailable")));
@property (readonly) BOOL rewardClaimed __attribute__((swift_name("rewardClaimed")));
@property (readonly) int32_t solvedCount __attribute__((swift_name("solvedCount")));
@property (readonly) NSString *titleUnit __attribute__((swift_name("titleUnit")));
@property (readonly) int32_t totalCount __attribute__((swift_name("totalCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionChallenge")))
@interface SharedDailyQuestionChallenge : SharedBase
- (instancetype)initWithPeriodKey:(NSString *)periodKey solvedCount:(int32_t)solvedCount questions:(NSArray<SharedDailyQuestionSlot *> *)questions rewardClaimed:(BOOL)rewardClaimed pendingContinueSlotIndex:(SharedInt * _Nullable)pendingContinueSlotIndex __attribute__((swift_name("init(periodKey:solvedCount:questions:rewardClaimed:pendingContinueSlotIndex:)"))) __attribute__((objc_designated_initializer));
- (NSString *)cardDifficultyLabel __attribute__((swift_name("cardDifficultyLabel()")));
- (NSString *)cardTitleUnit __attribute__((swift_name("cardTitleUnit()")));
- (SharedDailyQuestionChallenge *)doCopyPeriodKey:(NSString *)periodKey solvedCount:(int32_t)solvedCount questions:(NSArray<SharedDailyQuestionSlot *> *)questions rewardClaimed:(BOOL)rewardClaimed pendingContinueSlotIndex:(SharedInt * _Nullable)pendingContinueSlotIndex __attribute__((swift_name("doCopy(periodKey:solvedCount:questions:rewardClaimed:pendingContinueSlotIndex:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (int32_t)playSlotIndex __attribute__((swift_name("playSlotIndex()")));
- (SharedDailyQuestionSlot * _Nullable)slotForPlay __attribute__((swift_name("slotForPlay()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t currentSlotIndex __attribute__((swift_name("currentSlotIndex")));
@property (readonly) BOOL isComplete __attribute__((swift_name("isComplete")));
@property (readonly) BOOL needsDiamondContinue __attribute__((swift_name("needsDiamondContinue")));
@property (readonly) SharedInt * _Nullable pendingContinueSlotIndex __attribute__((swift_name("pendingContinueSlotIndex")));
@property (readonly) NSString *periodKey __attribute__((swift_name("periodKey")));
@property (readonly) NSArray<SharedDailyQuestionSlot *> *questions __attribute__((swift_name("questions")));
@property (readonly) BOOL rewardClaimed __attribute__((swift_name("rewardClaimed")));
@property (readonly) int32_t solvedCount __attribute__((swift_name("solvedCount")));
@end

__attribute__((swift_name("DailyQuestionChallengeCache")))
@protocol SharedDailyQuestionChallengeCache
@required
- (SharedDailyQuestionChallenge * _Nullable)readCacheKey:(NSString *)cacheKey __attribute__((swift_name("read(cacheKey:)")));
- (void)writeCacheKey:(NSString *)cacheKey challenge:(SharedDailyQuestionChallenge *)challenge __attribute__((swift_name("write(cacheKey:challenge:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionChallengeClickAction")))
@interface SharedDailyQuestionChallengeClickAction : SharedKotlinEnum<SharedDailyQuestionChallengeClickAction *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedDailyQuestionChallengeClickAction *ignore __attribute__((swift_name("ignore")));
@property (class, readonly) SharedDailyQuestionChallengeClickAction *showChallengeCompleteToast __attribute__((swift_name("showChallengeCompleteToast")));
@property (class, readonly) SharedDailyQuestionChallengeClickAction *showContinuePanel __attribute__((swift_name("showContinuePanel")));
@property (class, readonly) SharedDailyQuestionChallengeClickAction *launchLesson __attribute__((swift_name("launchLesson")));
+ (SharedKotlinArray<SharedDailyQuestionChallengeClickAction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedDailyQuestionChallengeClickAction *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionPeriod")))
@interface SharedDailyQuestionPeriod : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dailyQuestionPeriod __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyQuestionPeriod *shared __attribute__((swift_name("shared")));
- (NSString *)currentPeriodKeyNowMs:(int64_t)nowMs __attribute__((swift_name("currentPeriodKey(nowMs:)")));
- (NSString *)formatCountdownRemainingMs:(int64_t)remainingMs __attribute__((swift_name("formatCountdown(remainingMs:)")));
- (BOOL)isConsecutivePeriodAfterLastStreakPeriodKey:(NSString *)lastStreakPeriodKey currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("isConsecutivePeriodAfter(lastStreakPeriodKey:currentPeriodKey:)")));
- (BOOL)isFishingStreakBrokenLastStreakPeriodKey:(NSString *)lastStreakPeriodKey currentPeriodKey:(NSString *)currentPeriodKey __attribute__((swift_name("isFishingStreakBroken(lastStreakPeriodKey:currentPeriodKey:)")));
- (BOOL)isSamePeriodA:(NSString *)a b:(NSString *)b __attribute__((swift_name("isSamePeriod(a:b:)")));
- (int64_t)millisUntilCurrentPeriodEndsNowMs:(int64_t)nowMs __attribute__((swift_name("millisUntilCurrentPeriodEnds(nowMs:)")));
- (SharedLong * _Nullable)periodBucketFromKeyKey:(NSString *)key __attribute__((swift_name("periodBucketFromKey(key:)")));
- (NSString *)periodKeyForBucketBucket:(int64_t)bucket __attribute__((swift_name("periodKeyForBucket(bucket:)")));
@property (readonly) int32_t BROKEN_HEART_HOLD_FRAME __attribute__((swift_name("BROKEN_HEART_HOLD_FRAME")));
@property (readonly) NSString *FIRESTORE_COLLECTION __attribute__((swift_name("FIRESTORE_COLLECTION")));
@property (readonly) int32_t KEY_CONTINUE_COST __attribute__((swift_name("KEY_CONTINUE_COST")));
@property (readonly) int64_t PERIOD_MS __attribute__((swift_name("PERIOD_MS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) int32_t QUESTIONS_PER_PERIOD __attribute__((swift_name("QUESTIONS_PER_PERIOD")));
@property (readonly) BOOL USE_STABLE_SEQUENCE __attribute__((swift_name("USE_STABLE_SEQUENCE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionPoolBuilder")))
@interface SharedDailyQuestionPoolBuilder : SharedBase
- (instancetype)initWithGenerators:(id<SharedDailyQuestionGenerators>)generators random:(SharedKotlinRandom *)random __attribute__((swift_name("init(generators:random:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedDailyQuestionPoolBuilderCompanion *companion __attribute__((swift_name("companion")));
- (SharedDailyQuestionChallenge * _Nullable)buildChallengePeriodKey:(NSString *)periodKey uidKey:(NSString *)uidKey sources:(NSArray<SharedDailyQuestionSource *> *)sources __attribute__((swift_name("buildChallenge(periodKey:uidKey:sources:)")));
- (NSArray<SharedDailyQuestionSource *> *)buildSourcesForPartItems:(NSArray<id<SharedOyunLessonItem>> *)items partId:(int32_t)partId __attribute__((swift_name("buildSourcesForPart(items:partId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionPoolBuilder.Companion")))
@interface SharedDailyQuestionPoolBuilderCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyQuestionPoolBuilderCompanion *shared __attribute__((swift_name("shared")));
- (NSString *)difficultyLabelItem:(id<SharedOyunLessonItem>)item __attribute__((swift_name("difficultyLabel(item:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionProgressPlan")))
@interface SharedDailyQuestionProgressPlan : SharedBase
- (instancetype)initWithSolved:(int32_t)solved fromSolved:(int32_t)fromSolved startPct:(float)startPct endPct:(float)endPct shouldAnimate:(BOOL)shouldAnimate __attribute__((swift_name("init(solved:fromSolved:startPct:endPct:shouldAnimate:)"))) __attribute__((objc_designated_initializer));
- (SharedDailyQuestionProgressPlan *)doCopySolved:(int32_t)solved fromSolved:(int32_t)fromSolved startPct:(float)startPct endPct:(float)endPct shouldAnimate:(BOOL)shouldAnimate __attribute__((swift_name("doCopy(solved:fromSolved:startPct:endPct:shouldAnimate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float endPct __attribute__((swift_name("endPct")));
@property (readonly) int32_t fromSolved __attribute__((swift_name("fromSolved")));
@property (readonly) BOOL shouldAnimate __attribute__((swift_name("shouldAnimate")));
@property (readonly) int32_t solved __attribute__((swift_name("solved")));
@property (readonly) float startPct __attribute__((swift_name("startPct")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionRepository")))
@interface SharedDailyQuestionRepository : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)dailyQuestionRepository __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyQuestionRepository *shared __attribute__((swift_name("shared")));
- (void)cacheRewardClaimedCache:(id<SharedDailyQuestionChallengeCache>)cache cacheKey:(NSString *)cacheKey periodKey:(NSString *)periodKey __attribute__((swift_name("cacheRewardClaimed(cache:cacheKey:periodKey:)")));
- (void)cacheSolvedCountCache:(id<SharedDailyQuestionChallengeCache>)cache cacheKey:(NSString *)cacheKey periodKey:(NSString *)periodKey newCount:(int32_t)newCount __attribute__((swift_name("cacheSolvedCount(cache:cacheKey:periodKey:newCount:)")));
- (SharedDailyQuestionCardUiState *)cardUiStateWithoutChallengePeriodKey:(NSString *)periodKey poolAvailable:(BOOL)poolAvailable __attribute__((swift_name("cardUiStateWithoutChallenge(periodKey:poolAvailable:)")));
- (NSString *)challengeCacheKeyUid:(NSString *)uid __attribute__((swift_name("challengeCacheKey(uid:)")));
- (NSString *)challengeDocPathUid:(NSString *)uid __attribute__((swift_name("challengeDocPath(uid:)")));
- (NSDictionary<NSString *, id> *)challengeToFirestoreMapChallenge:(SharedDailyQuestionChallenge *)challenge __attribute__((swift_name("challengeToFirestoreMap(challenge:)")));
- (NSString * _Nullable)difficultyKeyDifficulty:(NSString *)difficulty __attribute__((swift_name("difficultyKey(difficulty:)")));
- (int32_t)nextSolvedCountCurrentSolvedCount:(SharedLong * _Nullable)currentSolvedCount __attribute__((swift_name("nextSolvedCount(currentSolvedCount:)")));
- (SharedDailyQuestionChallenge * _Nullable)parseChallengeDocumentData:(NSDictionary<NSString *, id> * _Nullable)data periodKey:(NSString *)periodKey __attribute__((swift_name("parseChallengeDocument(data:periodKey:)")));
- (SharedDailyQuestionSlot * _Nullable)parseSlotMap:(NSDictionary<id, id> *)map __attribute__((swift_name("parseSlot(map:)")));
- (NSString *)partCollectionNamePartId:(int32_t)partId __attribute__((swift_name("partCollectionName(partId:)")));
- (NSString *)rateFieldDifficultyKey:(NSString *)difficultyKey __attribute__((swift_name("rateField(difficultyKey:)")));
- (SharedDailyQuestionChallenge * _Nullable)readChallengeFromPrefsCache:(id<SharedDailyQuestionChallengeCache>)cache cacheKey:(NSString *)cacheKey periodKey:(NSString *)periodKey __attribute__((swift_name("readChallengeFromPrefs(cache:cacheKey:periodKey:)")));
- (SharedDailyQuestionChallenge * _Nullable)resolveLoadedChallengeDocExists:(BOOL)docExists data:(NSDictionary<NSString *, id> * _Nullable)data periodKey:(NSString *)periodKey __attribute__((swift_name("resolveLoadedChallenge(docExists:data:periodKey:)")));
- (int32_t)safePendingContinueSlotSlotIndex:(int32_t)slotIndex __attribute__((swift_name("safePendingContinueSlot(slotIndex:)")));
- (NSString *)successFieldDifficultyKey:(NSString *)difficultyKey __attribute__((swift_name("successField(difficultyKey:)")));
- (NSString *)successRateDocIdTitleUnit:(NSString *)titleUnit __attribute__((swift_name("successRateDocId(titleUnit:)")));
- (NSString *)successRateDocPathSlot:(SharedDailyQuestionSlot *)slot __attribute__((swift_name("successRateDocPath(slot:)")));
- (NSDictionary<NSString *, id> *)successRateUpdatesDifficultyKey:(NSString *)difficultyKey currentTotal:(SharedLong * _Nullable)currentTotal currentSuccess:(SharedLong * _Nullable)currentSuccess isSuccess:(BOOL)isSuccess __attribute__((swift_name("successRateUpdates(difficultyKey:currentTotal:currentSuccess:isSuccess:)")));
- (SharedDailyQuestionCardUiState *)toCardUiStateChallenge:(SharedDailyQuestionChallenge *)challenge poolAvailable:(BOOL)poolAvailable __attribute__((swift_name("toCardUiState(challenge:poolAvailable:)")));
- (NSString *)totalFieldDifficultyKey:(NSString *)difficultyKey __attribute__((swift_name("totalField(difficultyKey:)")));
- (void)updateCachedPendingContinueCache:(id<SharedDailyQuestionChallengeCache>)cache cacheKey:(NSString *)cacheKey periodKey:(NSString *)periodKey slotIndex:(SharedInt * _Nullable)slotIndex __attribute__((swift_name("updateCachedPendingContinue(cache:cacheKey:periodKey:slotIndex:)")));
@property (readonly) NSString *COLLECTION_SUCCESS_RATE __attribute__((swift_name("COLLECTION_SUCCESS_RATE")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *DOC_CURRENT __attribute__((swift_name("DOC_CURRENT")));
@property (readonly) NSString *DOC_DAILY_QUESTION_SUCCESS_RATE __attribute__((swift_name("DOC_DAILY_QUESTION_SUCCESS_RATE")));
@property (readonly) NSString *FIELD_DIFFICULTY __attribute__((swift_name("FIELD_DIFFICULTY")));
@property (readonly) NSString *FIELD_DISPLAY_INTERVAL __attribute__((swift_name("FIELD_DISPLAY_INTERVAL")));
@property (readonly) NSString *FIELD_ITEM_INDEX __attribute__((swift_name("FIELD_ITEM_INDEX")));
@property (readonly) NSString *FIELD_MATH_FIRST __attribute__((swift_name("FIELD_MATH_FIRST")));
@property (readonly) NSString *FIELD_MATH_OPERATOR __attribute__((swift_name("FIELD_MATH_OPERATOR")));
@property (readonly) NSString *FIELD_MATH_SECOND __attribute__((swift_name("FIELD_MATH_SECOND")));
@property (readonly) NSString *FIELD_PART_ID __attribute__((swift_name("FIELD_PART_ID")));
@property (readonly) NSString *FIELD_PENDING_CONTINUE_SLOT __attribute__((swift_name("FIELD_PENDING_CONTINUE_SLOT")));
@property (readonly) NSString *FIELD_PERIOD_KEY __attribute__((swift_name("FIELD_PERIOD_KEY")));
@property (readonly) NSString *FIELD_QUESTIONS __attribute__((swift_name("FIELD_QUESTIONS")));
@property (readonly) NSString *FIELD_REWARD_CLAIMED __attribute__((swift_name("FIELD_REWARD_CLAIMED")));
@property (readonly) NSString *FIELD_SEQUENCE __attribute__((swift_name("FIELD_SEQUENCE")));
@property (readonly) NSString *FIELD_SOLVED_COUNT __attribute__((swift_name("FIELD_SOLVED_COUNT")));
@property (readonly) NSString *FIELD_TITLE_UNIT __attribute__((swift_name("FIELD_TITLE_UNIT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionSlot")))
@interface SharedDailyQuestionSlot : SharedBase
- (instancetype)initWithSequence:(NSArray<SharedInt *> *)sequence partId:(int32_t)partId itemIndex:(int32_t)itemIndex titleUnit:(NSString *)titleUnit difficulty:(NSString *)difficulty displayIntervalMs:(SharedLong * _Nullable)displayIntervalMs mathOperation:(SharedMathOperation * _Nullable)mathOperation __attribute__((swift_name("init(sequence:partId:itemIndex:titleUnit:difficulty:displayIntervalMs:mathOperation:)"))) __attribute__((objc_designated_initializer));
- (SharedDailyQuestionSlot *)doCopySequence:(NSArray<SharedInt *> *)sequence partId:(int32_t)partId itemIndex:(int32_t)itemIndex titleUnit:(NSString *)titleUnit difficulty:(NSString *)difficulty displayIntervalMs:(SharedLong * _Nullable)displayIntervalMs mathOperation:(SharedMathOperation * _Nullable)mathOperation __attribute__((swift_name("doCopy(sequence:partId:itemIndex:titleUnit:difficulty:displayIntervalMs:mathOperation:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *difficulty __attribute__((swift_name("difficulty")));
@property (readonly) SharedLong * _Nullable displayIntervalMs __attribute__((swift_name("displayIntervalMs")));
@property (readonly) int32_t itemIndex __attribute__((swift_name("itemIndex")));
@property (readonly) SharedMathOperation * _Nullable mathOperation __attribute__((swift_name("mathOperation")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@property (readonly) NSArray<SharedInt *> *sequence __attribute__((swift_name("sequence")));
@property (readonly) NSString *titleUnit __attribute__((swift_name("titleUnit")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyQuestionSource")))
@interface SharedDailyQuestionSource : SharedBase
- (instancetype)initWithListIndex:(int32_t)listIndex item:(id<SharedOyunLessonItem>)item finishedChestIndex:(int32_t)finishedChestIndex partId:(int32_t)partId __attribute__((swift_name("init(listIndex:item:finishedChestIndex:partId:)"))) __attribute__((objc_designated_initializer));
- (SharedDailyQuestionSource *)doCopyListIndex:(int32_t)listIndex item:(id<SharedOyunLessonItem>)item finishedChestIndex:(int32_t)finishedChestIndex partId:(int32_t)partId __attribute__((swift_name("doCopy(listIndex:item:finishedChestIndex:partId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t finishedChestIndex __attribute__((swift_name("finishedChestIndex")));
@property (readonly) id<SharedOyunLessonItem> item __attribute__((swift_name("item")));
@property (readonly) int32_t listIndex __attribute__((swift_name("listIndex")));
@property (readonly) int32_t partId __attribute__((swift_name("partId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnergyManager")))
@interface SharedEnergyManager : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs platform:(id<SharedEnergyPlatform>)platform nowMs:(SharedLong *(^)(void))nowMs __attribute__((swift_name("init(prefs:platform:nowMs:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEnergyManagerCompanion *companion __attribute__((swift_name("companion")));
- (void)adoptServerFullTimeFullTime:(int64_t)fullTime __attribute__((swift_name("adoptServerFullTime(fullTime:)")));
- (void)destroy __attribute__((swift_name("destroy()")));
- (int32_t)getCurrentEnergy __attribute__((swift_name("getCurrentEnergy()")));
- (int64_t)getEnergyRefreshMillis __attribute__((swift_name("getEnergyRefreshMillis()")));
- (int32_t)getEnergyRefreshMinutes __attribute__((swift_name("getEnergyRefreshMinutes()")));
- (int32_t)getMaxEnergy __attribute__((swift_name("getMaxEnergy()")));
- (int64_t)getTimeUntilNextEnergy __attribute__((swift_name("getTimeUntilNextEnergy()")));
- (NSString *)getUserPlan __attribute__((swift_name("getUserPlan()")));
- (NSString *)getUserRole __attribute__((swift_name("getUserRole()")));
- (BOOL)hasEnoughEnergyAmount:(int32_t)amount __attribute__((swift_name("hasEnoughEnergy(amount:)")));
- (BOOL)isEnergyBlocked __attribute__((swift_name("isEnergyBlocked()")));
- (BOOL)isInfiniteEnergy __attribute__((swift_name("isInfiniteEnergy()")));
- (BOOL)isTeacherApproved __attribute__((swift_name("isTeacherApproved()")));
- (void)onSpendEnergyFailed __attribute__((swift_name("onSpendEnergyFailed()")));
- (void)onSpendEnergyResultData:(id _Nullable)data __attribute__((swift_name("onSpendEnergyResult(data:)")));
- (void)onTimerTick __attribute__((swift_name("onTimerTick()")));
- (void)onUserDocLoadFailed __attribute__((swift_name("onUserDocLoadFailed()")));
- (void)onUserDocLoadedExists:(BOOL)exists serverFullTime:(SharedLong * _Nullable)serverFullTime legacyEnergy:(SharedInt * _Nullable)legacyEnergy legacyLastUpdate:(SharedLong * _Nullable)legacyLastUpdate __attribute__((swift_name("onUserDocLoaded(exists:serverFullTime:legacyEnergy:legacyLastUpdate:)")));
- (void)setEnergyUpdateCallbackCallback:(void (^)(SharedInt *))callback __attribute__((swift_name("setEnergyUpdateCallback(callback:)")));
- (void)setUserPlanPlan:(NSString *)plan __attribute__((swift_name("setUserPlan(plan:)")));
- (void)setUserRoleApprovalRole:(NSString *)role teacherApproved:(BOOL)teacherApproved __attribute__((swift_name("setUserRoleApproval(role:teacherApproved:)")));
- (BOOL)useEnergyAmount:(int32_t)amount __attribute__((swift_name("useEnergy(amount:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EnergyManager.Companion")))
@interface SharedEnergyManagerCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEnergyManagerCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *FIELD_ENERGY_FULL_TIME __attribute__((swift_name("FIELD_ENERGY_FULL_TIME")));
@property (readonly) NSString *KEY_ENERGY_FULL_TIME __attribute__((swift_name("KEY_ENERGY_FULL_TIME")));
@property (readonly) NSString *KEY_TEACHER_APPROVED __attribute__((swift_name("KEY_TEACHER_APPROVED")));
@property (readonly) NSString *KEY_USER_PLAN __attribute__((swift_name("KEY_USER_PLAN")));
@property (readonly) NSString *KEY_USER_ROLE __attribute__((swift_name("KEY_USER_ROLE")));
@property (readonly) NSString *LEGACY_FIELD_ENERGY __attribute__((swift_name("LEGACY_FIELD_ENERGY")));
@property (readonly) NSString *LEGACY_FIELD_LAST_UPDATE __attribute__((swift_name("LEGACY_FIELD_LAST_UPDATE")));
@property (readonly) NSString *LEGACY_KEY_CURRENT_ENERGY __attribute__((swift_name("LEGACY_KEY_CURRENT_ENERGY")));
@property (readonly) NSString *LEGACY_KEY_LAST_ENERGY_UPDATE __attribute__((swift_name("LEGACY_KEY_LAST_ENERGY_UPDATE")));
@property (readonly) NSString *PLAN_FREE __attribute__((swift_name("PLAN_FREE")));
@property (readonly) NSString *PLAN_LITE __attribute__((swift_name("PLAN_LITE")));
@property (readonly) NSString *PLAN_PREMIUM __attribute__((swift_name("PLAN_PREMIUM")));
@property (readonly) NSString *PLAN_PRO __attribute__((swift_name("PLAN_PRO")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) NSString *ROLE_TEACHER __attribute__((swift_name("ROLE_TEACHER")));
@end

__attribute__((swift_name("EnergyPlatform")))
@protocol SharedEnergyPlatform
@required
- (void)callSpendEnergyPayload:(NSDictionary<NSString *, id> *)payload __attribute__((swift_name("callSpendEnergy(payload:)")));
- (void)cancelTick __attribute__((swift_name("cancelTick()")));
- (BOOL)isSignedIn __attribute__((swift_name("isSignedIn()")));
- (void)loadUserDoc __attribute__((swift_name("loadUserDoc()")));
- (void)scheduleTickDelayMs:(int64_t)delayMs __attribute__((swift_name("scheduleTick(delayMs:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FirstTutorialShownStore")))
@interface SharedFirstTutorialShownStore : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs __attribute__((swift_name("init(prefs:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedFirstTutorialShownStoreCompanion *companion __attribute__((swift_name("companion")));
- (void)markShownUid:(NSString * _Nullable)uid writeCloud:(void (^)(NSString *))writeCloud __attribute__((swift_name("markShown(uid:writeCloud:)")));
- (BOOL)readLocal __attribute__((swift_name("readLocal()")));
- (void)repairFirestoreIfLocalShownUid:(NSString * _Nullable)uid writeCloud:(void (^)(NSString *))writeCloud __attribute__((swift_name("repairFirestoreIfLocalShown(uid:writeCloud:)")));
- (SharedFirstTutorialShownStoreResolution *)resolveShownFirestoreValue:(SharedBoolean * _Nullable)firestoreValue __attribute__((swift_name("resolveShown(firestoreValue:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FirstTutorialShownStore.Companion")))
@interface SharedFirstTutorialShownStoreCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFirstTutorialShownStoreCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSString *KEY __attribute__((swift_name("KEY")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FirstTutorialShownStore.Resolution")))
@interface SharedFirstTutorialShownStoreResolution : SharedBase
- (instancetype)initWithShown:(BOOL)shown cloudNeedsRepair:(BOOL)cloudNeedsRepair __attribute__((swift_name("init(shown:cloudNeedsRepair:)"))) __attribute__((objc_designated_initializer));
- (SharedFirstTutorialShownStoreResolution *)doCopyShown:(BOOL)shown cloudNeedsRepair:(BOOL)cloudNeedsRepair __attribute__((swift_name("doCopy(shown:cloudNeedsRepair:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL cloudNeedsRepair __attribute__((swift_name("cloudNeedsRepair")));
@property (readonly) BOOL shown __attribute__((swift_name("shown")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InMemoryDailyQuestionChallengeCache")))
@interface SharedInMemoryDailyQuestionChallengeCache : SharedBase <SharedDailyQuestionChallengeCache>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedDailyQuestionChallenge * _Nullable)readCacheKey:(NSString *)cacheKey __attribute__((swift_name("read(cacheKey:)")));
- (void)writeCacheKey:(NSString *)cacheKey challenge:(SharedDailyQuestionChallenge *)challenge __attribute__((swift_name("write(cacheKey:challenge:)")));
@end

__attribute__((swift_name("OyunKeyValueStore")))
@protocol SharedOyunKeyValueStore
@required
- (void)clear __attribute__((swift_name("clear()")));
- (BOOL)containsKey:(NSString *)key __attribute__((swift_name("contains(key:)")));
- (BOOL)getBooleanKey:(NSString *)key defaultValue:(BOOL)defaultValue __attribute__((swift_name("getBoolean(key:defaultValue:)")));
- (int32_t)getIntKey:(NSString *)key defaultValue:(int32_t)defaultValue __attribute__((swift_name("getInt(key:defaultValue:)")));
- (int64_t)getLongKey:(NSString *)key defaultValue:(int64_t)defaultValue __attribute__((swift_name("getLong(key:defaultValue:)")));
- (NSString * _Nullable)getStringKey:(NSString *)key __attribute__((swift_name("getString(key:)")));
- (NSSet<NSString *> *)keys __attribute__((swift_name("keys()")));
- (void)putBooleanKey:(NSString *)key value:(BOOL)value __attribute__((swift_name("putBoolean(key:value:)")));
- (void)putIntKey:(NSString *)key value:(int32_t)value __attribute__((swift_name("putInt(key:value:)")));
- (void)putLongKey:(NSString *)key value:(int64_t)value __attribute__((swift_name("putLong(key:value:)")));
- (void)putStringKey:(NSString *)key value:(NSString *)value __attribute__((swift_name("putString(key:value:)")));
- (void)removeKey:(NSString *)key __attribute__((swift_name("remove(key:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InMemoryKeyValueStore")))
@interface SharedInMemoryKeyValueStore : SharedBase <SharedOyunKeyValueStore>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)clear __attribute__((swift_name("clear()")));
- (BOOL)containsKey:(NSString *)key __attribute__((swift_name("contains(key:)")));
- (BOOL)getBooleanKey:(NSString *)key defaultValue:(BOOL)defaultValue __attribute__((swift_name("getBoolean(key:defaultValue:)")));
- (int32_t)getIntKey:(NSString *)key defaultValue:(int32_t)defaultValue __attribute__((swift_name("getInt(key:defaultValue:)")));
- (int64_t)getLongKey:(NSString *)key defaultValue:(int64_t)defaultValue __attribute__((swift_name("getLong(key:defaultValue:)")));
- (NSString * _Nullable)getStringKey:(NSString *)key __attribute__((swift_name("getString(key:)")));
- (NSSet<NSString *> *)keys __attribute__((swift_name("keys()")));
- (void)putBooleanKey:(NSString *)key value:(BOOL)value __attribute__((swift_name("putBoolean(key:value:)")));
- (void)putIntKey:(NSString *)key value:(int32_t)value __attribute__((swift_name("putInt(key:value:)")));
- (void)putLongKey:(NSString *)key value:(int64_t)value __attribute__((swift_name("putLong(key:value:)")));
- (void)putStringKey:(NSString *)key value:(NSString *)value __attribute__((swift_name("putString(key:value:)")));
- (void)removeKey:(NSString *)key __attribute__((swift_name("remove(key:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarathonGuideStore")))
@interface SharedMarathonGuideStore : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs firstChestLessonIndexForGuidePart:(SharedInt * _Nullable (^)(void))firstChestLessonIndexForGuidePart __attribute__((swift_name("init(prefs:firstChestLessonIndexForGuidePart:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMarathonGuideStoreCompanion *companion __attribute__((swift_name("companion")));
- (void)clearPending __attribute__((swift_name("clearPending()")));
- (NSString * _Nullable)eligibilityRejectReasonPartId:(int32_t)partId lessonIndex:(int32_t)lessonIndex item:(id<SharedOyunLessonItem>)item __attribute__((swift_name("eligibilityRejectReason(partId:lessonIndex:item:)")));
- (int32_t)firstMarathonLessonIndex __attribute__((swift_name("firstMarathonLessonIndex()")));
- (BOOL)isPending __attribute__((swift_name("isPending()")));
- (BOOL)isShown __attribute__((swift_name("isShown()")));
- (void)markShown __attribute__((swift_name("markShown()")));
- (BOOL)scheduleIfEligiblePartId:(int32_t)partId lessonIndex:(int32_t)lessonIndex item:(id<SharedOyunLessonItem> _Nullable)item __attribute__((swift_name("scheduleIfEligible(partId:lessonIndex:item:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MarathonGuideStore.Companion")))
@interface SharedMarathonGuideStoreCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMarathonGuideStoreCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t DEFAULT_FIRST_MARATHON_INDEX __attribute__((swift_name("DEFAULT_FIRST_MARATHON_INDEX")));
@property (readonly) int32_t GUIDE_PART_ID __attribute__((swift_name("GUIDE_PART_ID")));
@property (readonly) NSString *KEY_PENDING __attribute__((swift_name("KEY_PENDING")));
@property (readonly) NSString *KEY_SHOWN __attribute__((swift_name("KEY_SHOWN")));
@property (readonly) NSString *LEGACY_SHOWN __attribute__((swift_name("LEGACY_SHOWN")));
@property (readonly) NSString *LOG_TAG __attribute__((swift_name("LOG_TAG")));
@property (readonly) NSString *PREFS __attribute__((swift_name("PREFS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MathOperation")))
@interface SharedMathOperation : SharedBase
- (instancetype)initWithFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("init(firstNumber:operator:secondNumber:)"))) __attribute__((objc_designated_initializer));
- (SharedMathOperation *)doCopyFirstNumber:(SharedInt * _Nullable)firstNumber operator:(NSString * _Nullable)operator_ secondNumber:(SharedInt * _Nullable)secondNumber __attribute__((swift_name("doCopy(firstNumber:operator:secondNumber:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable firstNumber __attribute__((swift_name("firstNumber")));
@property (readonly, getter=operator) NSString * _Nullable operator_ __attribute__((swift_name("operator_")));
@property (readonly) SharedInt * _Nullable secondNumber __attribute__((swift_name("secondNumber")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionCountdownLabel")))
@interface SharedMissionCountdownLabel : SharedBase
- (instancetype)initWithResKey:(NSString *)resKey value:(int32_t)value __attribute__((swift_name("init(resKey:value:)"))) __attribute__((objc_designated_initializer));
- (SharedMissionCountdownLabel *)doCopyResKey:(NSString *)resKey value:(int32_t)value __attribute__((swift_name("doCopy(resKey:value:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *resKey __attribute__((swift_name("resKey")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionCounterType")))
@interface SharedMissionCounterType : SharedKotlinEnum<SharedMissionCounterType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedMissionCounterType *stepFinish __attribute__((swift_name("stepFinish")));
@property (class, readonly) SharedMissionCounterType *stepIncrement __attribute__((swift_name("stepIncrement")));
@property (class, readonly) SharedMissionCounterType *perfectStepIncrement __attribute__((swift_name("perfectStepIncrement")));
@property (class, readonly) SharedMissionCounterType *chestRecordBreak __attribute__((swift_name("chestRecordBreak")));
@property (class, readonly) SharedMissionCounterType *chestStarGain __attribute__((swift_name("chestStarGain")));
@property (class, readonly) SharedMissionCounterType *learnMinutes __attribute__((swift_name("learnMinutes")));
+ (SharedKotlinArray<SharedMissionCounterType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedMissionCounterType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionProgressOverlay")))
@interface SharedMissionProgressOverlay : SharedBase
- (instancetype)initWithFillWidth:(int32_t)fillWidth fillVisible:(BOOL)fillVisible shineWidth:(int32_t)shineWidth shineVisible:(BOOL)shineVisible fillDrawable:(NSString *)fillDrawable shineDrawable:(NSString *)shineDrawable __attribute__((swift_name("init(fillWidth:fillVisible:shineWidth:shineVisible:fillDrawable:shineDrawable:)"))) __attribute__((objc_designated_initializer));
- (SharedMissionProgressOverlay *)doCopyFillWidth:(int32_t)fillWidth fillVisible:(BOOL)fillVisible shineWidth:(int32_t)shineWidth shineVisible:(BOOL)shineVisible fillDrawable:(NSString *)fillDrawable shineDrawable:(NSString *)shineDrawable __attribute__((swift_name("doCopy(fillWidth:fillVisible:shineWidth:shineVisible:fillDrawable:shineDrawable:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *fillDrawable __attribute__((swift_name("fillDrawable")));
@property (readonly) BOOL fillVisible __attribute__((swift_name("fillVisible")));
@property (readonly) int32_t fillWidth __attribute__((swift_name("fillWidth")));
@property (readonly) NSString *shineDrawable __attribute__((swift_name("shineDrawable")));
@property (readonly) BOOL shineVisible __attribute__((swift_name("shineVisible")));
@property (readonly) int32_t shineWidth __attribute__((swift_name("shineWidth")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionQuestCatalog")))
@interface SharedMissionQuestCatalog : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)missionQuestCatalog __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMissionQuestCatalog *shared __attribute__((swift_name("shared")));
@property (readonly) NSArray<SharedMissionQuestDefinition *> *all __attribute__((swift_name("all")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionQuestData")))
@interface SharedMissionQuestData : SharedBase
- (instancetype)initWithMissionId:(NSString *)missionId titleResId:(NSString *)titleResId progress:(int32_t)progress target:(int32_t)target iconRes:(NSString *)iconRes window:(SharedMissionWindow *)window isClaimed:(BOOL)isClaimed __attribute__((swift_name("init(missionId:titleResId:progress:target:iconRes:window:isClaimed:)"))) __attribute__((objc_designated_initializer));
- (SharedMissionQuestData *)doCopyMissionId:(NSString *)missionId titleResId:(NSString *)titleResId progress:(int32_t)progress target:(int32_t)target iconRes:(NSString *)iconRes window:(SharedMissionWindow *)window isClaimed:(BOOL)isClaimed __attribute__((swift_name("doCopy(missionId:titleResId:progress:target:iconRes:window:isClaimed:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL canClaim __attribute__((swift_name("canClaim")));
@property (readonly) BOOL done __attribute__((swift_name("done")));
@property (readonly) NSString *iconRes __attribute__((swift_name("iconRes")));
@property (readonly) BOOL isClaimed __attribute__((swift_name("isClaimed")));
@property (readonly) NSString *missionId __attribute__((swift_name("missionId")));
@property (readonly) int32_t percent __attribute__((swift_name("percent")));
@property (readonly) int32_t progress __attribute__((swift_name("progress")));
@property (readonly) SharedKotlinPair<SharedInt *, SharedInt *> *progressLabelArgs __attribute__((swift_name("progressLabelArgs")));
@property (readonly) SharedMissionQuestState *state __attribute__((swift_name("state")));
@property (readonly) int32_t target __attribute__((swift_name("target")));
@property (readonly) NSString *titleResId __attribute__((swift_name("titleResId")));
@property (readonly) SharedMissionWindow *window __attribute__((swift_name("window")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionQuestDefinition")))
@interface SharedMissionQuestDefinition : SharedBase
- (instancetype)initWithId:(NSString *)id titleResId:(NSString *)titleResId target:(int32_t)target counterType:(SharedMissionCounterType *)counterType availableWindows:(NSSet<SharedMissionWindow *> *)availableWindows __attribute__((swift_name("init(id:titleResId:target:counterType:availableWindows:)"))) __attribute__((objc_designated_initializer));
- (SharedMissionQuestDefinition *)doCopyId:(NSString *)id titleResId:(NSString *)titleResId target:(int32_t)target counterType:(SharedMissionCounterType *)counterType availableWindows:(NSSet<SharedMissionWindow *> *)availableWindows __attribute__((swift_name("doCopy(id:titleResId:target:counterType:availableWindows:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSSet<SharedMissionWindow *> *availableWindows __attribute__((swift_name("availableWindows")));
@property (readonly) SharedMissionCounterType *counterType __attribute__((swift_name("counterType")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) int32_t target __attribute__((swift_name("target")));
@property (readonly) NSString *titleResId __attribute__((swift_name("titleResId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionQuestState")))
@interface SharedMissionQuestState : SharedKotlinEnum<SharedMissionQuestState *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedMissionQuestState *claimed __attribute__((swift_name("claimed")));
@property (class, readonly) SharedMissionQuestState *done __attribute__((swift_name("done")));
@property (class, readonly) SharedMissionQuestState *pending __attribute__((swift_name("pending")));
+ (SharedKotlinArray<SharedMissionQuestState *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedMissionQuestState *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionUiLogic")))
@interface SharedMissionUiLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)missionUiLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMissionUiLogic *shared __attribute__((swift_name("shared")));
- (SharedKotlinPair<NSArray<SharedAnimatedQuest *> *, NSArray<SharedAnimatedQuest *> *> *)animatedQuestsWeeklyMissions:(NSArray<SharedMissionQuestDefinition *> *)weeklyMissions dailyMissions:(NSArray<SharedMissionQuestDefinition *> *)dailyMissions before:(SharedMissionsProgressStoreSnapshot *)before after:(SharedMissionsProgressStoreSnapshot *)after isClaimed:(SharedBoolean *(^)(SharedMissionWindow *, NSString *))isClaimed __attribute__((swift_name("animatedQuests(weeklyMissions:dailyMissions:before:after:isClaimed:)")));
- (SharedChestRarity *)chestStartRarityForWindowWindow:(SharedMissionWindow *)window __attribute__((swift_name("chestStartRarityForWindow(window:)")));
- (int32_t)countCompletedMissionsDailyMissions:(NSArray<SharedMissionQuestDefinition *> *)dailyMissions weeklyMissions:(NSArray<SharedMissionQuestDefinition *> *)weeklyMissions before:(SharedMissionsProgressStoreSnapshot *)before after:(SharedMissionsProgressStoreSnapshot *)after __attribute__((swift_name("countCompletedMissions(dailyMissions:weeklyMissions:before:after:)")));
- (SharedMissionCountdownLabel *)dailyCountdownLabelHoursUntilDailyReset:(int32_t)hoursUntilDailyReset __attribute__((swift_name("dailyCountdownLabel(hoursUntilDailyReset:)")));
- (SharedMissionProgressOverlay * _Nullable)dailyQuestionProgressOverlayTrackWidth:(int32_t)trackWidth percent:(float)percent complete:(BOOL)complete startInset:(int32_t)startInset endGap:(int32_t)endGap __attribute__((swift_name("dailyQuestionProgressOverlay(trackWidth:percent:complete:startInset:endGap:)")));
- (SharedMissionCountdownLabel *)formatWeeklyCountdownMs:(int64_t)ms __attribute__((swift_name("formatWeeklyCountdown(ms:)")));
- (NSString *)iconForWindowWindow:(SharedMissionWindow *)window __attribute__((swift_name("iconForWindow(window:)")));
- (BOOL)isPerfectLessonLessonSuccessRate:(float)lessonSuccessRate __attribute__((swift_name("isPerfectLesson(lessonSuccessRate:)")));
- (BOOL)justFinishedChestForRatingLessonBeforeClaim:(id<SharedOyunLessonItem> _Nullable)lessonBeforeClaim __attribute__((swift_name("justFinishedChestForRating(lessonBeforeClaim:)")));
- (BOOL)missionCounterDeltaBefore:(SharedMissionsProgressStoreSnapshot *)before after:(SharedMissionsProgressStoreSnapshot *)after __attribute__((swift_name("missionCounterDelta(before:after:)")));
- (SharedMissionProgressOverlay * _Nullable)missionProgressOverlayTrackWidth:(int32_t)trackWidth percent:(float)percent done:(BOOL)done claimed:(BOOL)claimed startInset:(int32_t)startInset endGap:(int32_t)endGap __attribute__((swift_name("missionProgressOverlay(trackWidth:percent:done:claimed:startInset:endGap:)")));
- (SharedMissionQuestData *)questDataMission:(SharedMissionQuestDefinition *)mission window:(SharedMissionWindow *)window snapshot:(SharedMissionsProgressStoreSnapshot *)snapshot isClaimed:(BOOL)isClaimed __attribute__((swift_name("questData(mission:window:snapshot:isClaimed:)")));
- (BOOL)shouldIncrementDartProgressLessonBeforeClaim:(id<SharedOyunLessonItem> _Nullable)lessonBeforeClaim lessonSuccessRate:(float)lessonSuccessRate __attribute__((swift_name("shouldIncrementDartProgress(lessonBeforeClaim:lessonSuccessRate:)")));
@property (readonly) NSString *ICON_DAILY __attribute__((swift_name("ICON_DAILY")));
@property (readonly) NSString *ICON_WEEKLY __attribute__((swift_name("ICON_WEEKLY")));
@property (readonly) int64_t PROGRESS_ANIM_DURATION_MS __attribute__((swift_name("PROGRESS_ANIM_DURATION_MS")));
@property (readonly) NSString *RESULT_CHEST_CLOSED __attribute__((swift_name("RESULT_CHEST_CLOSED")));
@property (readonly) NSString *RES_COMPLETED_LABEL __attribute__((swift_name("RES_COMPLETED_LABEL")));
@property (readonly) NSString *RES_DAYS_SHORT __attribute__((swift_name("RES_DAYS_SHORT")));
@property (readonly) NSString *RES_HOURS_SHORT __attribute__((swift_name("RES_HOURS_SHORT")));
@property (readonly) NSString *RES_PROGRESS_FORMAT __attribute__((swift_name("RES_PROGRESS_FORMAT")));
@property (readonly) NSString *RES_REWARD_CLAIMED_LABEL __attribute__((swift_name("RES_REWARD_CLAIMED_LABEL")));
@property (readonly) int64_t STAGGER_DELAY_MS __attribute__((swift_name("STAGGER_DELAY_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionWindow")))
@interface SharedMissionWindow : SharedKotlinEnum<SharedMissionWindow *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedMissionWindow *daily __attribute__((swift_name("daily")));
@property (class, readonly) SharedMissionWindow *weekly __attribute__((swift_name("weekly")));
+ (SharedKotlinArray<SharedMissionWindow *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedMissionWindow *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionsProgressStore")))
@interface SharedMissionsProgressStore : SharedBase
- (instancetype)initWithPrefsProvider:(id<SharedOyunKeyValueStore> (^)(NSString *))prefsProvider cloud:(id<SharedMissionCloudGateway>)cloud elapsedRealtimeMs:(SharedLong *(^)(void))elapsedRealtimeMs wallClockMs:(SharedLong *(^)(void))wallClockMs timeZone:(SharedKotlinx_datetimeTimeZone *(^)(void))timeZone __attribute__((swift_name("init(prefsProvider:cloud:elapsedRealtimeMs:wallClockMs:timeZone:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMissionsProgressStoreCompanion *companion __attribute__((swift_name("companion")));
- (void)applyPendingLearningMinutes __attribute__((swift_name("applyPendingLearningMinutes()")));
- (NSString *)calendarDayId __attribute__((swift_name("calendarDayId()")));
- (void)clearLocalCache __attribute__((swift_name("clearLocalCache()")));
- (void)ensureResets __attribute__((swift_name("ensureResets()")));
- (void)forceReselectMissions __attribute__((swift_name("forceReselectMissions()")));
- (void)forceUploadStateToCloud __attribute__((swift_name("forceUploadStateToCloud()")));
- (SharedMissionsProgressStoreSnapshot *)getSnapshotApplyCloudHydrate:(BOOL)applyCloudHydrate __attribute__((swift_name("getSnapshot(applyCloudHydrate:)")));
- (BOOL)hasVisibleMissionProgressBefore:(SharedMissionsProgressStoreSnapshot *)before after:(SharedMissionsProgressStoreSnapshot *)after __attribute__((swift_name("hasVisibleMissionProgress(before:after:)")));
- (int32_t)hoursUntilDailyReset __attribute__((swift_name("hoursUntilDailyReset()")));
- (BOOL)isMissionRewardClaimedWindow:(SharedMissionWindow *)window missionId:(NSString *)missionId __attribute__((swift_name("isMissionRewardClaimed(window:missionId:)")));
- (void)markMissionRewardClaimedWindow:(SharedMissionWindow *)window missionId:(NSString *)missionId __attribute__((swift_name("markMissionRewardClaimed(window:missionId:)")));
- (int64_t)millisUntilWeeklyReset __attribute__((swift_name("millisUntilWeeklyReset()")));
- (int32_t)missionProgressSnapshot:(SharedMissionsProgressStoreSnapshot *)snapshot window:(SharedMissionWindow *)window mission:(SharedMissionQuestDefinition *)mission __attribute__((swift_name("missionProgress(snapshot:window:mission:)")));
- (void)recordChestRecordBreakProgress __attribute__((swift_name("recordChestRecordBreakProgress()")));
- (void)recordChestStarGainProgressAmount:(int32_t)amount __attribute__((swift_name("recordChestStarGainProgress(amount:)")));
- (void)recordLearningDurationMsElapsedMs:(int64_t)elapsedMs __attribute__((swift_name("recordLearningDurationMs(elapsedMs:)")));
- (void)recordPerfectStepIncrementProgress __attribute__((swift_name("recordPerfectStepIncrementProgress()")));
- (void)recordStepFinishProgress __attribute__((swift_name("recordStepFinishProgress()")));
- (void)recordStepIncrementProgress __attribute__((swift_name("recordStepIncrementProgress()")));
- (void)resetAllProgress __attribute__((swift_name("resetAllProgress()")));
- (NSArray<SharedMissionQuestDefinition *> *)selectedMissionsForDaily __attribute__((swift_name("selectedMissionsForDaily()")));
- (NSArray<SharedMissionQuestDefinition *> *)selectedMissionsForWeekly __attribute__((swift_name("selectedMissionsForWeekly()")));
- (NSArray<SharedMissionQuestDefinition *> *)selectedMissionsForWindowWindow:(SharedMissionWindow *)window __attribute__((swift_name("selectedMissionsForWindow(window:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionsProgressStore.Companion")))
@interface SharedMissionsProgressStoreCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMissionsProgressStoreCompanion *shared __attribute__((swift_name("shared")));
- (int64_t)computeTrustedNowMsNowWall:(int64_t)nowWall nowElapsed:(int64_t)nowElapsed lastTrusted:(int64_t)lastTrusted lastElapsed:(int64_t)lastElapsed __attribute__((swift_name("computeTrustedNowMs(nowWall:nowElapsed:lastTrusted:lastElapsed:)")));
- (NSString *)dayIdEpochMs:(int64_t)epochMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("dayId(epochMs:timeZone:)")));
- (NSArray<NSString *> *)generateMissionIdsWindow:(SharedMissionWindow *)window seedKey:(NSString *)seedKey __attribute__((swift_name("generateMissionIds(window:seedKey:)")));
- (int32_t)hoursUntilDailyResetNowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("hoursUntilDailyReset(nowMs:timeZone:)")));
- (int32_t)javaStringHashCodeS:(NSString *)s __attribute__((swift_name("javaStringHashCode(s:)")));
- (int64_t)millisUntilWeeklyResetNowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("millisUntilWeeklyReset(nowMs:timeZone:)")));
- (NSString *)missionIdsCsvIds:(NSArray<NSString *> *)ids __attribute__((swift_name("missionIdsCsv(ids:)")));
- (int32_t)missionProgressSnapshot:(SharedMissionsProgressStoreSnapshot *)snapshot window:(SharedMissionWindow *)window mission:(SharedMissionQuestDefinition *)mission __attribute__((swift_name("missionProgress(snapshot:window:mission:)")));
- (NSArray<NSString *> *)parseMissionIdsCsvCsv:(NSString * _Nullable)csv __attribute__((swift_name("parseMissionIdsCsv(csv:)")));
- (int32_t)readCounterSnapshot:(SharedMissionsProgressStoreSnapshot *)snapshot window:(SharedMissionWindow *)window counterType:(SharedMissionCounterType *)counterType __attribute__((swift_name("readCounter(snapshot:window:counterType:)")));
- (NSString *)rewardClaimKeyForWindow:(SharedMissionWindow *)window periodId:(NSString *)periodId missionId:(NSString *)missionId __attribute__((swift_name("rewardClaimKeyFor(window:periodId:missionId:)")));
- (NSString *)stateDocPathUid:(NSString *)uid __attribute__((swift_name("stateDocPath(uid:)")));
- (NSString *)weekIdEpochMs:(int64_t)epochMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("weekId(epochMs:timeZone:)")));
@property (readonly) int64_t CLOCK_TOLERANCE_MS __attribute__((swift_name("CLOCK_TOLERANCE_MS")));
@property (readonly) NSString *FIELD_REWARD_CLAIMED_KEYS __attribute__((swift_name("FIELD_REWARD_CLAIMED_KEYS")));
@property (readonly) NSString *FIRESTORE_COLLECTION __attribute__((swift_name("FIRESTORE_COLLECTION")));
@property (readonly) NSString *FIRESTORE_DOC_STATE __attribute__((swift_name("FIRESTORE_DOC_STATE")));
@property (readonly) NSString *KEY_DAILY_CHEST_RECORD_BREAK_COUNT __attribute__((swift_name("KEY_DAILY_CHEST_RECORD_BREAK_COUNT")));
@property (readonly) NSString *KEY_DAILY_CHEST_STAR_GAIN_COUNT __attribute__((swift_name("KEY_DAILY_CHEST_STAR_GAIN_COUNT")));
@property (readonly) NSString *KEY_DAILY_LEARN_MINUTES_COUNT __attribute__((swift_name("KEY_DAILY_LEARN_MINUTES_COUNT")));
@property (readonly) NSString *KEY_DAILY_LEARN_REMAINDER_MS __attribute__((swift_name("KEY_DAILY_LEARN_REMAINDER_MS")));
@property (readonly) NSString *KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT __attribute__((swift_name("KEY_DAILY_PERFECT_STEP_INCREMENT_COUNT")));
@property (readonly) NSString *KEY_DAILY_SELECTED_MISSION_IDS __attribute__((swift_name("KEY_DAILY_SELECTED_MISSION_IDS")));
@property (readonly) NSString *KEY_DAILY_SELECTION_NONCE __attribute__((swift_name("KEY_DAILY_SELECTION_NONCE")));
@property (readonly) NSString *KEY_DAILY_STEP_FINISH_COUNT __attribute__((swift_name("KEY_DAILY_STEP_FINISH_COUNT")));
@property (readonly) NSString *KEY_DAILY_STEP_INCREMENT_COUNT __attribute__((swift_name("KEY_DAILY_STEP_INCREMENT_COUNT")));
@property (readonly) NSString *KEY_DAY_ID __attribute__((swift_name("KEY_DAY_ID")));
@property (readonly) NSString *KEY_TRUSTED_ANCHOR_ELAPSED_MS __attribute__((swift_name("KEY_TRUSTED_ANCHOR_ELAPSED_MS")));
@property (readonly) NSString *KEY_TRUSTED_NOW_MS __attribute__((swift_name("KEY_TRUSTED_NOW_MS")));
@property (readonly) NSString *KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT __attribute__((swift_name("KEY_WEEKLY_CHEST_RECORD_BREAK_COUNT")));
@property (readonly) NSString *KEY_WEEKLY_CHEST_STAR_GAIN_COUNT __attribute__((swift_name("KEY_WEEKLY_CHEST_STAR_GAIN_COUNT")));
@property (readonly) NSString *KEY_WEEKLY_LEARN_MINUTES_COUNT __attribute__((swift_name("KEY_WEEKLY_LEARN_MINUTES_COUNT")));
@property (readonly) NSString *KEY_WEEKLY_LEARN_REMAINDER_MS __attribute__((swift_name("KEY_WEEKLY_LEARN_REMAINDER_MS")));
@property (readonly) NSString *KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT __attribute__((swift_name("KEY_WEEKLY_PERFECT_STEP_INCREMENT_COUNT")));
@property (readonly) NSString *KEY_WEEKLY_SELECTED_MISSION_IDS __attribute__((swift_name("KEY_WEEKLY_SELECTED_MISSION_IDS")));
@property (readonly) NSString *KEY_WEEKLY_SELECTION_NONCE __attribute__((swift_name("KEY_WEEKLY_SELECTION_NONCE")));
@property (readonly) NSString *KEY_WEEKLY_STEP_FINISH_COUNT __attribute__((swift_name("KEY_WEEKLY_STEP_FINISH_COUNT")));
@property (readonly) NSString *KEY_WEEKLY_STEP_INCREMENT_COUNT __attribute__((swift_name("KEY_WEEKLY_STEP_INCREMENT_COUNT")));
@property (readonly) NSString *KEY_WEEK_ID __attribute__((swift_name("KEY_WEEK_ID")));
@property (readonly) NSString *PREFS __attribute__((swift_name("PREFS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MissionsProgressStore.Snapshot")))
@interface SharedMissionsProgressStoreSnapshot : SharedBase
- (instancetype)initWithDailyStepFinishCount:(int32_t)dailyStepFinishCount weeklyStepFinishCount:(int32_t)weeklyStepFinishCount dailyStepIncrementCount:(int32_t)dailyStepIncrementCount weeklyStepIncrementCount:(int32_t)weeklyStepIncrementCount dailyPerfectStepIncrementCount:(int32_t)dailyPerfectStepIncrementCount weeklyPerfectStepIncrementCount:(int32_t)weeklyPerfectStepIncrementCount dailyChestRecordBreakCount:(int32_t)dailyChestRecordBreakCount weeklyChestRecordBreakCount:(int32_t)weeklyChestRecordBreakCount dailyChestStarGainCount:(int32_t)dailyChestStarGainCount weeklyChestStarGainCount:(int32_t)weeklyChestStarGainCount dailyLearnMinutesCount:(int32_t)dailyLearnMinutesCount weeklyLearnMinutesCount:(int32_t)weeklyLearnMinutesCount __attribute__((swift_name("init(dailyStepFinishCount:weeklyStepFinishCount:dailyStepIncrementCount:weeklyStepIncrementCount:dailyPerfectStepIncrementCount:weeklyPerfectStepIncrementCount:dailyChestRecordBreakCount:weeklyChestRecordBreakCount:dailyChestStarGainCount:weeklyChestStarGainCount:dailyLearnMinutesCount:weeklyLearnMinutesCount:)"))) __attribute__((objc_designated_initializer));
- (SharedMissionsProgressStoreSnapshot *)doCopyDailyStepFinishCount:(int32_t)dailyStepFinishCount weeklyStepFinishCount:(int32_t)weeklyStepFinishCount dailyStepIncrementCount:(int32_t)dailyStepIncrementCount weeklyStepIncrementCount:(int32_t)weeklyStepIncrementCount dailyPerfectStepIncrementCount:(int32_t)dailyPerfectStepIncrementCount weeklyPerfectStepIncrementCount:(int32_t)weeklyPerfectStepIncrementCount dailyChestRecordBreakCount:(int32_t)dailyChestRecordBreakCount weeklyChestRecordBreakCount:(int32_t)weeklyChestRecordBreakCount dailyChestStarGainCount:(int32_t)dailyChestStarGainCount weeklyChestStarGainCount:(int32_t)weeklyChestStarGainCount dailyLearnMinutesCount:(int32_t)dailyLearnMinutesCount weeklyLearnMinutesCount:(int32_t)weeklyLearnMinutesCount __attribute__((swift_name("doCopy(dailyStepFinishCount:weeklyStepFinishCount:dailyStepIncrementCount:weeklyStepIncrementCount:dailyPerfectStepIncrementCount:weeklyPerfectStepIncrementCount:dailyChestRecordBreakCount:weeklyChestRecordBreakCount:dailyChestStarGainCount:weeklyChestStarGainCount:dailyLearnMinutesCount:weeklyLearnMinutesCount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t dailyChestRecordBreakCount __attribute__((swift_name("dailyChestRecordBreakCount")));
@property (readonly) int32_t dailyChestStarGainCount __attribute__((swift_name("dailyChestStarGainCount")));
@property (readonly) int32_t dailyLearnMinutesCount __attribute__((swift_name("dailyLearnMinutesCount")));
@property (readonly) int32_t dailyPerfectStepIncrementCount __attribute__((swift_name("dailyPerfectStepIncrementCount")));
@property (readonly) int32_t dailyStepFinishCount __attribute__((swift_name("dailyStepFinishCount")));
@property (readonly) int32_t dailyStepIncrementCount __attribute__((swift_name("dailyStepIncrementCount")));
@property (readonly) int32_t weeklyChestRecordBreakCount __attribute__((swift_name("weeklyChestRecordBreakCount")));
@property (readonly) int32_t weeklyChestStarGainCount __attribute__((swift_name("weeklyChestStarGainCount")));
@property (readonly) int32_t weeklyLearnMinutesCount __attribute__((swift_name("weeklyLearnMinutesCount")));
@property (readonly) int32_t weeklyPerfectStepIncrementCount __attribute__((swift_name("weeklyPerfectStepIncrementCount")));
@property (readonly) int32_t weeklyStepFinishCount __attribute__((swift_name("weeklyStepFinishCount")));
@property (readonly) int32_t weeklyStepIncrementCount __attribute__((swift_name("weeklyStepIncrementCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NewChestLogic")))
@interface SharedNewChestLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)getNewChestLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedNewChestLogic *shared __attribute__((swift_name("shared")));
- (SharedNewChestLogicCloseRoute *)closeRouteTopBackStackName:(NSString * _Nullable)topBackStackName __attribute__((swift_name("closeRoute(topBackStackName:)")));
- (NSArray<SharedChestRarity *> *)rarityPathFromServerNames:(NSArray<NSString *> *)names __attribute__((swift_name("rarityPathFromServer(names:)")));
- (SharedChestRewardDisplay *)rewardDisplayServerRewardType:(NSString *)serverRewardType serverRewardAmount:(int32_t)serverRewardAmount currentRarity:(SharedChestRarity *)currentRarity __attribute__((swift_name("rewardDisplay(serverRewardType:serverRewardAmount:currentRarity:)")));
- (float)soundRateForRarityRarityOrdinal:(int32_t)rarityOrdinal __attribute__((swift_name("soundRateForRarity(rarityOrdinal:)")));
- (SharedChestStepIndicator *)stepIndicatorIndex:(int32_t)index tapCount:(int32_t)tapCount __attribute__((swift_name("stepIndicator(index:tapCount:)")));
@property (readonly) NSString *ARG_AD_NONCE __attribute__((swift_name("ARG_AD_NONCE")));
@property (readonly) NSString *ARG_START_RARITY __attribute__((swift_name("ARG_START_RARITY")));
@property (readonly) int64_t HINT_DELAY_MS __attribute__((swift_name("HINT_DELAY_MS")));
@property (readonly) NSString *HINT_OPEN_CHEST __attribute__((swift_name("HINT_OPEN_CHEST")));
@property (readonly) NSString *RESULT_CHEST_CLOSED __attribute__((swift_name("RESULT_CHEST_CLOSED")));
@property (readonly) NSString *RESULT_EARNED_GOLD __attribute__((swift_name("RESULT_EARNED_GOLD")));
@property (readonly) NSString *RESULT_EARNED_KEY __attribute__((swift_name("RESULT_EARNED_KEY")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NewChestLogic.CloseRoute")))
@interface SharedNewChestLogicCloseRoute : SharedKotlinEnum<SharedNewChestLogicCloseRoute *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedNewChestLogicCloseRoute *mapChestSlide __attribute__((swift_name("mapChestSlide")));
@property (class, readonly) SharedNewChestLogicCloseRoute *popNamed __attribute__((swift_name("popNamed")));
@property (class, readonly) SharedNewChestLogicCloseRoute *finishOverlay __attribute__((swift_name("finishOverlay")));
+ (SharedKotlinArray<SharedNewChestLogicCloseRoute *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedNewChestLogicCloseRoute *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NewChestState")))
@interface SharedNewChestState : SharedBase
- (instancetype)initWithStartRarity:(SharedChestRarity *)startRarity __attribute__((swift_name("init(startRarity:)"))) __attribute__((objc_designated_initializer));
- (void)applyServerOutcomeOutcome:(SharedServerRewardsChestOutcome *)outcome __attribute__((swift_name("applyServerOutcome(outcome:)")));
- (NSDictionary<NSString *, SharedInt *> *)closeResult __attribute__((swift_name("closeResult()")));
- (void)onRewardAnimationEnd __attribute__((swift_name("onRewardAnimationEnd()")));
- (SharedChestTapResult *)onScreenTapped __attribute__((swift_name("onScreenTapped()")));
- (SharedChestRewardDisplay *)showReward __attribute__((swift_name("showReward()")));
@property (readonly) BOOL canClose __attribute__((swift_name("canClose")));
@property (readonly) SharedChestRarity *currentRarity __attribute__((swift_name("currentRarity")));
@property (readonly) int32_t earnedGoldAmount __attribute__((swift_name("earnedGoldAmount")));
@property (readonly) int32_t earnedKeyAmount __attribute__((swift_name("earnedKeyAmount")));
@property (readonly) BOOL outcomeReady __attribute__((swift_name("outcomeReady")));
@property (readonly) NSArray<SharedChestRarity *> *serverRarityPath __attribute__((swift_name("serverRarityPath")));
@property (readonly) int32_t serverRewardAmount __attribute__((swift_name("serverRewardAmount")));
@property (readonly) NSString *serverRewardType __attribute__((swift_name("serverRewardType")));
@property (readonly) int32_t tapCount __attribute__((swift_name("tapCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OyunLessonItemCompanion")))
@interface SharedOyunLessonItemCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedOyunLessonItemCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) int32_t TYPE_CHEST __attribute__((swift_name("TYPE_CHEST")));
@property (readonly) int32_t TYPE_HEADER __attribute__((swift_name("TYPE_HEADER")));
@property (readonly) int32_t TYPE_LESSON __attribute__((swift_name("TYPE_LESSON")));
@property (readonly) int32_t TYPE_RACE __attribute__((swift_name("TYPE_RACE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OyunLessonItemData")))
@interface SharedOyunLessonItemData : SharedBase <SharedOyunLessonItem>
- (instancetype)initWithType:(int32_t)type title:(NSString *)title titleUnit:(NSString * _Nullable)titleUnit stepIsFinish:(BOOL)stepIsFinish record:(SharedInt * _Nullable)record cupPoint1:(SharedInt * _Nullable)cupPoint1 cupPoint2:(SharedInt * _Nullable)cupPoint2 isBlinding:(SharedBoolean * _Nullable)isBlinding stepCupIcon:(NSString *)stepCupIcon mapFragmentIndex:(SharedInt * _Nullable)mapFragmentIndex __attribute__((swift_name("init(type:title:titleUnit:stepIsFinish:record:cupPoint1:cupPoint2:isBlinding:stepCupIcon:mapFragmentIndex:)"))) __attribute__((objc_designated_initializer));
- (SharedOyunLessonItemData *)doCopyType:(int32_t)type title:(NSString *)title titleUnit:(NSString * _Nullable)titleUnit stepIsFinish:(BOOL)stepIsFinish record:(SharedInt * _Nullable)record cupPoint1:(SharedInt * _Nullable)cupPoint1 cupPoint2:(SharedInt * _Nullable)cupPoint2 isBlinding:(SharedBoolean * _Nullable)isBlinding stepCupIcon:(NSString *)stepCupIcon mapFragmentIndex:(SharedInt * _Nullable)mapFragmentIndex __attribute__((swift_name("doCopy(type:title:titleUnit:stepIsFinish:record:cupPoint1:cupPoint2:isBlinding:stepCupIcon:mapFragmentIndex:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedInt * _Nullable cupPoint1 __attribute__((swift_name("cupPoint1")));
@property (readonly) SharedInt * _Nullable cupPoint2 __attribute__((swift_name("cupPoint2")));
@property (readonly) SharedBoolean * _Nullable isBlinding __attribute__((swift_name("isBlinding")));
@property (readonly) SharedInt * _Nullable mapFragmentIndex __attribute__((swift_name("mapFragmentIndex")));
@property (readonly) SharedInt * _Nullable record __attribute__((swift_name("record")));
@property (readonly) NSString *stepCupIcon __attribute__((swift_name("stepCupIcon")));
@property (readonly) BOOL stepIsFinish __attribute__((swift_name("stepIsFinish")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString * _Nullable titleUnit __attribute__((swift_name("titleUnit")));
@property (readonly) int32_t type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PlanStatus")))
@interface SharedPlanStatus : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)planStatus __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPlanStatus *shared __attribute__((swift_name("shared")));
- (NSString *)effectivePlanStoredPlan:(NSString * _Nullable)storedPlan planExpiresAt:(SharedLong * _Nullable)planExpiresAt giftProUntil:(SharedLong * _Nullable)giftProUntil nowMs:(int64_t)nowMs __attribute__((swift_name("effectivePlan(storedPlan:planExpiresAt:giftProUntil:nowMs:)")));
- (SharedLong * _Nullable)giftProUntilGiftProUntil:(SharedLong * _Nullable)giftProUntil nowMs:(int64_t)nowMs __attribute__((swift_name("giftProUntil(giftProUntil:nowMs:)")));
- (BOOL)isProPlanPlan:(NSString *)plan __attribute__((swift_name("isProPlan(plan:)")));
@property (readonly) NSString *FIELD_GIFT_PRO_UNTIL __attribute__((swift_name("FIELD_GIFT_PRO_UNTIL")));
@property (readonly) NSString *FIELD_PLAN __attribute__((swift_name("FIELD_PLAN")));
@property (readonly) NSString *FIELD_PLAN_EXPIRES_AT __attribute__((swift_name("FIELD_PLAN_EXPIRES_AT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RarityTheme")))
@interface SharedRarityTheme : SharedBase
- (instancetype)initWithBgColor:(int32_t)bgColor platformColor:(int32_t)platformColor starColor:(int32_t)starColor stepActiveColor:(int32_t)stepActiveColor stepFutureColor:(int32_t)stepFutureColor __attribute__((swift_name("init(bgColor:platformColor:starColor:stepActiveColor:stepFutureColor:)"))) __attribute__((objc_designated_initializer));
- (SharedRarityTheme *)doCopyBgColor:(int32_t)bgColor platformColor:(int32_t)platformColor starColor:(int32_t)starColor stepActiveColor:(int32_t)stepActiveColor stepFutureColor:(int32_t)stepFutureColor __attribute__((swift_name("doCopy(bgColor:platformColor:starColor:stepActiveColor:stepFutureColor:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t bgColor __attribute__((swift_name("bgColor")));
@property (readonly) int32_t platformColor __attribute__((swift_name("platformColor")));
@property (readonly) int32_t starColor __attribute__((swift_name("starColor")));
@property (readonly) int32_t stepActiveColor __attribute__((swift_name("stepActiveColor")));
@property (readonly) int32_t stepFutureColor __attribute__((swift_name("stepFutureColor")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RatingPromptDecision")))
@interface SharedRatingPromptDecision : SharedBase
- (instancetype)initWithResult:(BOOL)result showDialog:(BOOL)showDialog __attribute__((swift_name("init(result:showDialog:)"))) __attribute__((objc_designated_initializer));
- (SharedRatingPromptDecision *)doCopyResult:(BOOL)result showDialog:(BOOL)showDialog __attribute__((swift_name("doCopy(result:showDialog:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL result __attribute__((swift_name("result")));
@property (readonly) BOOL showDialog __attribute__((swift_name("showDialog")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScoreBarColorBlend")))
@interface SharedScoreBarColorBlend : SharedBase
- (instancetype)initWithFromColor:(NSString *)fromColor toColor:(NSString *)toColor fraction:(float)fraction __attribute__((swift_name("init(fromColor:toColor:fraction:)"))) __attribute__((objc_designated_initializer));
- (SharedScoreBarColorBlend *)doCopyFromColor:(NSString *)fromColor toColor:(NSString *)toColor fraction:(float)fraction __attribute__((swift_name("doCopy(fromColor:toColor:fraction:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) float fraction __attribute__((swift_name("fraction")));
@property (readonly) NSString *fromColor __attribute__((swift_name("fromColor")));
@property (readonly) NSString *toColor __attribute__((swift_name("toColor")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServerEnergy")))
@interface SharedServerEnergy : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)serverEnergy __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedServerEnergy *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)buyWithKeysPayload __attribute__((swift_name("buyWithKeysPayload()")));
- (NSDictionary<NSString *, id> *)claimAdEnergyPayloadAdNonce:(NSString *)adNonce __attribute__((swift_name("claimAdEnergyPayload(adNonce:)")));
- (SharedServerEnergyBuyWithKeysResult * _Nullable)parseBuyWithKeysResultData:(id _Nullable)data __attribute__((swift_name("parseBuyWithKeysResult(data:)")));
- (SharedLong * _Nullable)parseClaimAdEnergyResultData:(id _Nullable)data __attribute__((swift_name("parseClaimAdEnergyResult(data:)")));
- (SharedLong * _Nullable)readFullTimeData:(id _Nullable)data __attribute__((swift_name("readFullTime(data:)")));
- (BOOL)shouldRetryClaimAdEnergyIsUnavailable:(BOOL)isUnavailable attempt:(int32_t)attempt __attribute__((swift_name("shouldRetryClaimAdEnergy(isUnavailable:attempt:)")));
- (NSDictionary<NSString *, id> *)spendEnergyPayloadAmount:(int32_t)amount __attribute__((swift_name("spendEnergyPayload(amount:)")));
@property (readonly) int32_t AD_VERIFY_MAX_ATTEMPTS __attribute__((swift_name("AD_VERIFY_MAX_ATTEMPTS")));
@property (readonly) int64_t AD_VERIFY_RETRY_DELAY_MS __attribute__((swift_name("AD_VERIFY_RETRY_DELAY_MS")));
@property (readonly) NSString *ERROR_BUY_ENERGY_INVALID __attribute__((swift_name("ERROR_BUY_ENERGY_INVALID")));
@property (readonly) NSString *ERROR_CLAIM_AD_ENERGY_INVALID __attribute__((swift_name("ERROR_CLAIM_AD_ENERGY_INVALID")));
@property (readonly) NSString *FUNCTION_BUY_ENERGY_WITH_KEYS __attribute__((swift_name("FUNCTION_BUY_ENERGY_WITH_KEYS")));
@property (readonly) NSString *FUNCTION_CLAIM_AD_ENERGY __attribute__((swift_name("FUNCTION_CLAIM_AD_ENERGY")));
@property (readonly) NSString *FUNCTION_SPEND_ENERGY __attribute__((swift_name("FUNCTION_SPEND_ENERGY")));
@property (readonly) NSString *PARAM_AD_NONCE __attribute__((swift_name("PARAM_AD_NONCE")));
@property (readonly) NSString *PARAM_AMOUNT __attribute__((swift_name("PARAM_AMOUNT")));
@property (readonly) NSString *RESULT_ENERGY_FULL_TIME __attribute__((swift_name("RESULT_ENERGY_FULL_TIME")));
@property (readonly) NSString *RESULT_KEYS __attribute__((swift_name("RESULT_KEYS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServerEnergy.BuyWithKeysResult")))
@interface SharedServerEnergyBuyWithKeysResult : SharedBase
- (instancetype)initWithFullTime:(int64_t)fullTime keys:(int32_t)keys __attribute__((swift_name("init(fullTime:keys:)"))) __attribute__((objc_designated_initializer));
- (SharedServerEnergyBuyWithKeysResult *)doCopyFullTime:(int64_t)fullTime keys:(int32_t)keys __attribute__((swift_name("doCopy(fullTime:keys:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t fullTime __attribute__((swift_name("fullTime")));
@property (readonly) int32_t keys __attribute__((swift_name("keys")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServerRewards")))
@interface SharedServerRewards : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)serverRewards __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedServerRewards *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> *)claimCupPathChestPayloadCupField:(NSString *)cupField milestone:(int32_t)milestone __attribute__((swift_name("claimCupPathChestPayload(cupField:milestone:)")));
- (NSDictionary<NSString *, id> *)openChestPayloadStartRarity:(NSString *)startRarity adNonce:(NSString * _Nullable)adNonce __attribute__((swift_name("openChestPayload(startRarity:adNonce:)")));
- (NSDictionary<NSString *, id> *)openCrystalPayload __attribute__((swift_name("openCrystalPayload()")));
- (SharedServerRewardsChestOutcome * _Nullable)parseChestOutcomeData:(id _Nullable)data startRarity:(NSString *)startRarity __attribute__((swift_name("parseChestOutcome(data:startRarity:)")));
- (SharedServerRewardsCrystalOutcome * _Nullable)parseCrystalOutcomeData:(id _Nullable)data __attribute__((swift_name("parseCrystalOutcome(data:)")));
- (SharedServerRewardsChestOutcome * _Nullable)parseCupPathChestOutcomeData:(id _Nullable)data __attribute__((swift_name("parseCupPathChestOutcome(data:)")));
- (BOOL)shouldRetryOpenChestIsUnavailable:(BOOL)isUnavailable attempt:(int32_t)attempt __attribute__((swift_name("shouldRetryOpenChest(isUnavailable:attempt:)")));
@property (readonly) int32_t AD_VERIFY_MAX_ATTEMPTS __attribute__((swift_name("AD_VERIFY_MAX_ATTEMPTS")));
@property (readonly) int64_t AD_VERIFY_RETRY_DELAY_MS __attribute__((swift_name("AD_VERIFY_RETRY_DELAY_MS")));
@property (readonly) NSString *ERROR_CLAIM_CUP_PATH_CHEST_EMPTY __attribute__((swift_name("ERROR_CLAIM_CUP_PATH_CHEST_EMPTY")));
@property (readonly) NSString *ERROR_OPEN_CHEST_EMPTY __attribute__((swift_name("ERROR_OPEN_CHEST_EMPTY")));
@property (readonly) NSString *ERROR_OPEN_CRYSTAL_EMPTY __attribute__((swift_name("ERROR_OPEN_CRYSTAL_EMPTY")));
@property (readonly) NSString *FUNCTION_CLAIM_CUP_PATH_CHEST __attribute__((swift_name("FUNCTION_CLAIM_CUP_PATH_CHEST")));
@property (readonly) NSString *FUNCTION_OPEN_CHEST __attribute__((swift_name("FUNCTION_OPEN_CHEST")));
@property (readonly) NSString *FUNCTION_OPEN_CRYSTAL_REWARD __attribute__((swift_name("FUNCTION_OPEN_CRYSTAL_REWARD")));
@property (readonly) NSString *PARAM_AD_NONCE __attribute__((swift_name("PARAM_AD_NONCE")));
@property (readonly) NSString *PARAM_CUP_FIELD __attribute__((swift_name("PARAM_CUP_FIELD")));
@property (readonly) NSString *PARAM_MILESTONE __attribute__((swift_name("PARAM_MILESTONE")));
@property (readonly) NSString *PARAM_START_RARITY __attribute__((swift_name("PARAM_START_RARITY")));
@property (readonly) NSString *RESULT_FINAL_RARITY __attribute__((swift_name("RESULT_FINAL_RARITY")));
@property (readonly) NSString *RESULT_RARITY_PATH __attribute__((swift_name("RESULT_RARITY_PATH")));
@property (readonly) NSString *RESULT_REWARD_AMOUNT __attribute__((swift_name("RESULT_REWARD_AMOUNT")));
@property (readonly) NSString *RESULT_REWARD_TYPE __attribute__((swift_name("RESULT_REWARD_TYPE")));
@property (readonly) NSString *RESULT_VIDEO_NAME __attribute__((swift_name("RESULT_VIDEO_NAME")));
@property (readonly) NSString *REWARD_TYPE_GOLD __attribute__((swift_name("REWARD_TYPE_GOLD")));
@property (readonly) NSString *REWARD_TYPE_KEY __attribute__((swift_name("REWARD_TYPE_KEY")));
@end

__attribute__((swift_name("ServerRewardsChestCaller")))
@protocol SharedServerRewardsChestCaller
@required
- (void)openChestAttemptStartRarity:(NSString *)startRarity adNonce:(NSString * _Nullable)adNonce onResult:(void (^)(SharedServerRewardsChestOutcome *))onResult onFailure:(void (^)(SharedKotlinException *))onFailure __attribute__((swift_name("openChestAttempt(startRarity:adNonce:onResult:onFailure:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServerRewards.ChestOutcome")))
@interface SharedServerRewardsChestOutcome : SharedBase
- (instancetype)initWithRarityPath:(NSArray<NSString *> *)rarityPath finalRarity:(NSString *)finalRarity rewardType:(NSString *)rewardType rewardAmount:(int32_t)rewardAmount __attribute__((swift_name("init(rarityPath:finalRarity:rewardType:rewardAmount:)"))) __attribute__((objc_designated_initializer));
- (SharedServerRewardsChestOutcome *)doCopyRarityPath:(NSArray<NSString *> *)rarityPath finalRarity:(NSString *)finalRarity rewardType:(NSString *)rewardType rewardAmount:(int32_t)rewardAmount __attribute__((swift_name("doCopy(rarityPath:finalRarity:rewardType:rewardAmount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *finalRarity __attribute__((swift_name("finalRarity")));
@property (readonly) NSArray<NSString *> *rarityPath __attribute__((swift_name("rarityPath")));
@property (readonly) int32_t rewardAmount __attribute__((swift_name("rewardAmount")));
@property (readonly) NSString *rewardType __attribute__((swift_name("rewardType")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServerRewards.ChestPrefetcher")))
@interface SharedServerRewardsChestPrefetcher : SharedBase
- (instancetype)initWithCaller:(id<SharedServerRewardsChestCaller>)caller __attribute__((swift_name("init(caller:)"))) __attribute__((objc_designated_initializer));
- (void)openChestStartRarity:(NSString *)startRarity adNonce:(NSString * _Nullable)adNonce onResult:(void (^)(SharedServerRewardsChestOutcome *))onResult onFailure:(void (^)(SharedKotlinException *))onFailure __attribute__((swift_name("openChest(startRarity:adNonce:onResult:onFailure:)")));
- (void)prefetchChestStartRarity:(NSString *)startRarity adNonce:(NSString * _Nullable)adNonce __attribute__((swift_name("prefetchChest(startRarity:adNonce:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ServerRewards.CrystalOutcome")))
@interface SharedServerRewardsCrystalOutcome : SharedBase
- (instancetype)initWithRewardType:(NSString *)rewardType rewardAmount:(int32_t)rewardAmount __attribute__((swift_name("init(rewardType:rewardAmount:)"))) __attribute__((objc_designated_initializer));
- (SharedServerRewardsCrystalOutcome *)doCopyRewardType:(NSString *)rewardType rewardAmount:(int32_t)rewardAmount __attribute__((swift_name("doCopy(rewardType:rewardAmount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t rewardAmount __attribute__((swift_name("rewardAmount")));
@property (readonly) NSString *rewardType __attribute__((swift_name("rewardType")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ShopFragmentLogic")))
@interface SharedShopFragmentLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)shopFragmentLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedShopFragmentLogic *shared __attribute__((swift_name("shared")));
- (NSArray<NSString *> *)allProductIds __attribute__((swift_name("allProductIds()")));
- (BOOL)canBuyLifeWithKeysCurrentKeys:(int32_t)currentKeys __attribute__((swift_name("canBuyLifeWithKeys(currentKeys:)")));
- (SharedShopFragmentLogicEnergySectionState *)energySectionStateIsInfinite:(BOOL)isInfinite currentEnergy:(int32_t)currentEnergy maxEnergy:(int32_t)maxEnergy timeUntilNextEnergyMs:(int64_t)timeUntilNextEnergyMs __attribute__((swift_name("energySectionState(isInfinite:currentEnergy:maxEnergy:timeUntilNextEnergyMs:)")));
- (NSString *)formatTimerTimeToNextMs:(int64_t)timeToNextMs __attribute__((swift_name("formatTimer(timeToNextMs:)")));
- (int32_t)optimisticKeysAfterBuyLifeCurrentKeys:(int32_t)currentKeys __attribute__((swift_name("optimisticKeysAfterBuyLife(currentKeys:)")));
- (NSString *)priceLabelCard:(SharedShopFragmentLogicPackCard *)card storePrice:(NSString * _Nullable)storePrice __attribute__((swift_name("priceLabel(card:storePrice:)")));
- (NSString *)proPriceLabelStorePrice:(NSString * _Nullable)storePrice __attribute__((swift_name("proPriceLabel(storePrice:)")));
- (SharedShopFragmentLogicProductKind * _Nullable)productKindProductId:(NSString *)productId __attribute__((swift_name("productKind(productId:)")));
- (NSString * _Nullable)purchaseAnimationIconResNameProductId:(NSString *)productId __attribute__((swift_name("purchaseAnimationIconResName(productId:)")));
- (SharedInt * _Nullable)purchaseAnimationItemCountProductId:(NSString *)productId __attribute__((swift_name("purchaseAnimationItemCount(productId:)")));
@property (readonly) int32_t COLOR_BUY_BUTTON_DISABLED __attribute__((swift_name("COLOR_BUY_BUTTON_DISABLED")));
@property (readonly) int32_t COLOR_BUY_BUTTON_ENABLED __attribute__((swift_name("COLOR_BUY_BUTTON_ENABLED")));
@property (readonly) int32_t COLOR_BUY_TEXT_DISABLED __attribute__((swift_name("COLOR_BUY_TEXT_DISABLED")));
@property (readonly) int32_t COLOR_BUY_TEXT_ENABLED __attribute__((swift_name("COLOR_BUY_TEXT_ENABLED")));
@property (readonly) int32_t COLOR_TIMER_COUNTING __attribute__((swift_name("COLOR_TIMER_COUNTING")));
@property (readonly) int32_t COLOR_TIMER_FULL __attribute__((swift_name("COLOR_TIMER_FULL")));
@property (readonly) int64_t ENERGY_TICK_MS __attribute__((swift_name("ENERGY_TICK_MS")));
@property (readonly) NSArray<SharedShopFragmentLogicPackCard *> *GOLD_CARDS __attribute__((swift_name("GOLD_CARDS")));
@property (readonly) NSArray<SharedShopFragmentLogicPackCard *> *KEY_CARDS __attribute__((swift_name("KEY_CARDS")));
@property (readonly) int32_t LIFE_KEY_COST __attribute__((swift_name("LIFE_KEY_COST")));
@property (readonly) NSString *MESSAGE_AD_LIFE_FAILED __attribute__((swift_name("MESSAGE_AD_LIFE_FAILED")));
@property (readonly) NSString *MESSAGE_BUY_LIFE_FAILED __attribute__((swift_name("MESSAGE_BUY_LIFE_FAILED")));
@property (readonly) NSString *MESSAGE_NOT_ENOUGH_KEYS __attribute__((swift_name("MESSAGE_NOT_ENOUGH_KEYS")));
@property (readonly) NSString *MESSAGE_PRODUCT_UNAVAILABLE __attribute__((swift_name("MESSAGE_PRODUCT_UNAVAILABLE")));
@property (readonly) NSString *MESSAGE_PURCHASE_FAILED __attribute__((swift_name("MESSAGE_PURCHASE_FAILED")));
@property (readonly) NSString *MESSAGE_PURCHASE_NOT_STARTED __attribute__((swift_name("MESSAGE_PURCHASE_NOT_STARTED")));
@property (readonly) NSString *MESSAGE_PURCHASE_NOT_VERIFIED __attribute__((swift_name("MESSAGE_PURCHASE_NOT_VERIFIED")));
@property (readonly) NSString *MESSAGE_SUBSCRIPTION_OFFER_MISSING __attribute__((swift_name("MESSAGE_SUBSCRIPTION_OFFER_MISSING")));
@property (readonly) NSString *PRO_FALLBACK_PRICE_LABEL __attribute__((swift_name("PRO_FALLBACK_PRICE_LABEL")));
@property (readonly) NSString *PRO_PRODUCT_ID __attribute__((swift_name("PRO_PRODUCT_ID")));
@property (readonly) int32_t PURCHASE_ANIMATION_ITEM_COUNT __attribute__((swift_name("PURCHASE_ANIMATION_ITEM_COUNT")));
@property (readonly) int64_t PURCHASE_ANIMATION_SPREAD_MS __attribute__((swift_name("PURCHASE_ANIMATION_SPREAD_MS")));
@property (readonly) NSString *TEXT_ENERGY_FULL __attribute__((swift_name("TEXT_ENERGY_FULL")));
@property (readonly) NSString *TEXT_INFINITE __attribute__((swift_name("TEXT_INFINITE")));
@property (readonly) NSString *TITLE_GOLD __attribute__((swift_name("TITLE_GOLD")));
@property (readonly) NSString *TITLE_KEYS __attribute__((swift_name("TITLE_KEYS")));
@property (readonly) NSString *TITLE_LIFE __attribute__((swift_name("TITLE_LIFE")));
@property (readonly) NSString *TITLE_SPECIAL_OFFERS __attribute__((swift_name("TITLE_SPECIAL_OFFERS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ShopFragmentLogic.EnergySectionState")))
@interface SharedShopFragmentLogicEnergySectionState : SharedBase
- (instancetype)initWithHeaderEnergyText:(NSString *)headerEnergyText timerText:(NSString *)timerText timerColor:(int32_t)timerColor canGetLife:(BOOL)canGetLife buyButtonColor:(int32_t)buyButtonColor buyTextColor:(int32_t)buyTextColor buyIconAlpha:(float)buyIconAlpha watchAdAlpha:(float)watchAdAlpha buyCostText:(NSString *)buyCostText __attribute__((swift_name("init(headerEnergyText:timerText:timerColor:canGetLife:buyButtonColor:buyTextColor:buyIconAlpha:watchAdAlpha:buyCostText:)"))) __attribute__((objc_designated_initializer));
- (SharedShopFragmentLogicEnergySectionState *)doCopyHeaderEnergyText:(NSString *)headerEnergyText timerText:(NSString *)timerText timerColor:(int32_t)timerColor canGetLife:(BOOL)canGetLife buyButtonColor:(int32_t)buyButtonColor buyTextColor:(int32_t)buyTextColor buyIconAlpha:(float)buyIconAlpha watchAdAlpha:(float)watchAdAlpha buyCostText:(NSString *)buyCostText __attribute__((swift_name("doCopy(headerEnergyText:timerText:timerColor:canGetLife:buyButtonColor:buyTextColor:buyIconAlpha:watchAdAlpha:buyCostText:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t buyButtonColor __attribute__((swift_name("buyButtonColor")));
@property (readonly) NSString *buyCostText __attribute__((swift_name("buyCostText")));
@property (readonly) float buyIconAlpha __attribute__((swift_name("buyIconAlpha")));
@property (readonly) int32_t buyTextColor __attribute__((swift_name("buyTextColor")));
@property (readonly) BOOL canGetLife __attribute__((swift_name("canGetLife")));
@property (readonly) NSString *headerEnergyText __attribute__((swift_name("headerEnergyText")));
@property (readonly) int32_t timerColor __attribute__((swift_name("timerColor")));
@property (readonly) NSString *timerText __attribute__((swift_name("timerText")));
@property (readonly) float watchAdAlpha __attribute__((swift_name("watchAdAlpha")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ShopFragmentLogic.PackCard")))
@interface SharedShopFragmentLogicPackCard : SharedBase
- (instancetype)initWithProductId:(NSString *)productId imageResName:(NSString *)imageResName currencyIconResName:(NSString *)currencyIconResName amountLabel:(NSString *)amountLabel fallbackPrice:(NSString *)fallbackPrice __attribute__((swift_name("init(productId:imageResName:currencyIconResName:amountLabel:fallbackPrice:)"))) __attribute__((objc_designated_initializer));
- (SharedShopFragmentLogicPackCard *)doCopyProductId:(NSString *)productId imageResName:(NSString *)imageResName currencyIconResName:(NSString *)currencyIconResName amountLabel:(NSString *)amountLabel fallbackPrice:(NSString *)fallbackPrice __attribute__((swift_name("doCopy(productId:imageResName:currencyIconResName:amountLabel:fallbackPrice:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *amountLabel __attribute__((swift_name("amountLabel")));
@property (readonly) NSString *currencyIconResName __attribute__((swift_name("currencyIconResName")));
@property (readonly) NSString *fallbackPrice __attribute__((swift_name("fallbackPrice")));
@property (readonly) NSString *imageResName __attribute__((swift_name("imageResName")));
@property (readonly) NSString *productId __attribute__((swift_name("productId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ShopFragmentLogic.ProductKind")))
@interface SharedShopFragmentLogicProductKind : SharedKotlinEnum<SharedShopFragmentLogicProductKind *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedShopFragmentLogicProductKind *consumable __attribute__((swift_name("consumable")));
@property (class, readonly) SharedShopFragmentLogicProductKind *subscription __attribute__((swift_name("subscription")));
+ (SharedKotlinArray<SharedShopFragmentLogicProductKind *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedShopFragmentLogicProductKind *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeTracker")))
@interface SharedTimeTracker : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs platform:(id<SharedTimeTrackerPlatform>)platform nowMs:(SharedLong *(^)(void))nowMs __attribute__((swift_name("init(prefs:platform:nowMs:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTimeTrackerCompanion *companion __attribute__((swift_name("companion")));
- (void)initialize __attribute__((swift_name("initialize()")));
- (void)onPeriodicTick __attribute__((swift_name("onPeriodicTick()")));
- (void)reset __attribute__((swift_name("reset()")));
- (void)startTracking __attribute__((swift_name("startTracking()")));
- (void)stopTracking __attribute__((swift_name("stopTracking()")));
@property void (^ _Nullable onUnsyncedTimeChanged)(SharedLong *) __attribute__((swift_name("onUnsyncedTimeChanged")));
@property (readonly) int64_t unsyncedTime __attribute__((swift_name("unsyncedTime")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeTracker.Companion")))
@interface SharedTimeTrackerCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTimeTrackerCompanion *shared __attribute__((swift_name("shared")));
- (SharedTimeTrackerCompanionSyncWrite *)buildSyncWriteUnsyncedSeconds:(int64_t)unsyncedSeconds nowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("buildSyncWrite(unsyncedSeconds:nowMs:timeZone:)")));
- (NSString *)dayKeyOffsetDays:(int32_t)offsetDays nowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("dayKey(offsetDays:nowMs:timeZone:)")));
@property (readonly) NSString *DAILY_FIELD __attribute__((swift_name("DAILY_FIELD")));
@property (readonly) NSString *FIELD_TOTAL_TIME_SPENT __attribute__((swift_name("FIELD_TOTAL_TIME_SPENT")));
@property (readonly) NSString *KEY_LAST_START_TIME __attribute__((swift_name("KEY_LAST_START_TIME")));
@property (readonly) NSString *KEY_UNSYNCED_TIME __attribute__((swift_name("KEY_UNSYNCED_TIME")));
@property (readonly) int64_t MAX_RECOVERABLE_SECONDS __attribute__((swift_name("MAX_RECOVERABLE_SECONDS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) int32_t RETENTION_DAYS __attribute__((swift_name("RETENTION_DAYS")));
@property (readonly) int64_t TICK_INTERVAL_SECONDS __attribute__((swift_name("TICK_INTERVAL_SECONDS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeTracker.CompanionSyncWrite")))
@interface SharedTimeTrackerCompanionSyncWrite : SharedBase
- (instancetype)initWithTotalIncrement:(int64_t)totalIncrement dailyFieldPath:(NSArray<NSString *> *)dailyFieldPath dailyIncrement:(int64_t)dailyIncrement expiredFieldPath:(NSArray<NSString *> *)expiredFieldPath __attribute__((swift_name("init(totalIncrement:dailyFieldPath:dailyIncrement:expiredFieldPath:)"))) __attribute__((objc_designated_initializer));
- (SharedTimeTrackerCompanionSyncWrite *)doCopyTotalIncrement:(int64_t)totalIncrement dailyFieldPath:(NSArray<NSString *> *)dailyFieldPath dailyIncrement:(int64_t)dailyIncrement expiredFieldPath:(NSArray<NSString *> *)expiredFieldPath __attribute__((swift_name("doCopy(totalIncrement:dailyFieldPath:dailyIncrement:expiredFieldPath:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *dailyFieldPath __attribute__((swift_name("dailyFieldPath")));
@property (readonly) int64_t dailyIncrement __attribute__((swift_name("dailyIncrement")));
@property (readonly) NSArray<NSString *> *expiredFieldPath __attribute__((swift_name("expiredFieldPath")));
@property (readonly) int64_t totalIncrement __attribute__((swift_name("totalIncrement")));
@end

__attribute__((swift_name("TimeTrackerPlatform")))
@protocol SharedTimeTrackerPlatform
@required
- (NSString * _Nullable)currentUid __attribute__((swift_name("currentUid()")));
- (void)incrementTotalTimeSpentUid:(NSString *)uid seconds:(int64_t)seconds __attribute__((swift_name("incrementTotalTimeSpent(uid:seconds:)")));
- (void)setTotalTimeSpentUid:(NSString *)uid value:(int64_t)value __attribute__((swift_name("setTotalTimeSpent(uid:value:)")));
- (void)startPeriodicTickIntervalSeconds:(int64_t)intervalSeconds __attribute__((swift_name("startPeriodicTick(intervalSeconds:)")));
- (void)stopPeriodicTick __attribute__((swift_name("stopPeriodicTick()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TrustedClock")))
@interface SharedTrustedClock : SharedBase
- (instancetype)initWithPrefs:(id<SharedOyunKeyValueStore>)prefs elapsedRealtimeMs:(SharedLong *(^)(void))elapsedRealtimeMs bootCount:(int32_t)bootCount wallNowMs:(SharedLong *(^)(void))wallNowMs __attribute__((swift_name("init(prefs:elapsedRealtimeMs:bootCount:wallNowMs:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTrustedClockCompanion *companion __attribute__((swift_name("companion")));
- (void)addAnchorListenerListener:(void (^)(void))listener __attribute__((swift_name("addAnchorListener(listener:)")));
- (int64_t)nowMs __attribute__((swift_name("nowMs()")));
- (int64_t)onServerTimeServerNowMs:(int64_t)serverNowMs __attribute__((swift_name("onServerTime(serverNowMs:)")));
- (void)removeAnchorListenerListener:(void (^)(void))listener __attribute__((swift_name("removeAnchorListener(listener:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TrustedClock.Companion")))
@interface SharedTrustedClockCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTrustedClockCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *KEY_BOOT_COUNT __attribute__((swift_name("KEY_BOOT_COUNT")));
@property (readonly) NSString *KEY_ELAPSED_MS __attribute__((swift_name("KEY_ELAPSED_MS")));
@property (readonly) NSString *KEY_SERVER_NOW_MS __attribute__((swift_name("KEY_SERVER_NOW_MS")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) int32_t UNKNOWN_BOOT __attribute__((swift_name("UNKNOWN_BOOT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TrustedClockRules")))
@interface SharedTrustedClockRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)trustedClockRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTrustedClockRules *shared __attribute__((swift_name("shared")));
- (int64_t)trustedNowAnchor:(SharedTrustedClockRulesAnchor * _Nullable)anchor wallNowMs:(int64_t)wallNowMs elapsedNowMs:(int64_t)elapsedNowMs bootCount:(int32_t)bootCount __attribute__((swift_name("trustedNow(anchor:wallNowMs:elapsedNowMs:bootCount:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TrustedClockRules.Anchor")))
@interface SharedTrustedClockRulesAnchor : SharedBase
- (instancetype)initWithServerNowMs:(int64_t)serverNowMs elapsedMs:(int64_t)elapsedMs bootCount:(int32_t)bootCount __attribute__((swift_name("init(serverNowMs:elapsedMs:bootCount:)"))) __attribute__((objc_designated_initializer));
- (SharedTrustedClockRulesAnchor *)doCopyServerNowMs:(int64_t)serverNowMs elapsedMs:(int64_t)elapsedMs bootCount:(int32_t)bootCount __attribute__((swift_name("doCopy(serverNowMs:elapsedMs:bootCount:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t bootCount __attribute__((swift_name("bootCount")));
@property (readonly) int64_t elapsedMs __attribute__((swift_name("elapsedMs")));
@property (readonly) int64_t serverNowMs __attribute__((swift_name("serverNowMs")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserWallet")))
@interface SharedUserWallet : SharedBase
- (instancetype)initWithKeys:(int32_t)keys currency:(int32_t)currency rollbackToken:(NSString * _Nullable)rollbackToken questionCredits:(int32_t)questionCredits sobiCredits:(int32_t)sobiCredits plan:(NSString *)plan __attribute__((swift_name("init(keys:currency:rollbackToken:questionCredits:sobiCredits:plan:)"))) __attribute__((objc_designated_initializer));
- (SharedUserWallet *)doCopyKeys:(int32_t)keys currency:(int32_t)currency rollbackToken:(NSString * _Nullable)rollbackToken questionCredits:(int32_t)questionCredits sobiCredits:(int32_t)sobiCredits plan:(NSString *)plan __attribute__((swift_name("doCopy(keys:currency:rollbackToken:questionCredits:sobiCredits:plan:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t currency __attribute__((swift_name("currency")));
@property (readonly) int32_t keys __attribute__((swift_name("keys")));
@property (readonly) NSString *plan __attribute__((swift_name("plan")));
@property (readonly) int32_t questionCredits __attribute__((swift_name("questionCredits")));
@property (readonly) NSString * _Nullable rollbackToken __attribute__((swift_name("rollbackToken")));
@property (readonly) int32_t sobiCredits __attribute__((swift_name("sobiCredits")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserWalletFirestore")))
@interface SharedUserWalletFirestore : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)userWalletFirestore __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserWalletFirestore *shared __attribute__((swift_name("shared")));
- (NSDictionary<NSString *, id> * _Nullable)applyCurrencyDeltaPayloadDelta:(int32_t)delta reason:(NSString *)reason rollbackToken:(NSString * _Nullable)rollbackToken __attribute__((swift_name("applyCurrencyDeltaPayload(delta:reason:rollbackToken:)")));
- (NSDictionary<NSString *, id> * _Nullable)applyDeltaPayloadKeyDelta:(int32_t)keyDelta currencyDelta:(int32_t)currencyDelta reason:(NSString *)reason rollbackToken:(NSString * _Nullable)rollbackToken __attribute__((swift_name("applyDeltaPayload(keyDelta:currencyDelta:reason:rollbackToken:)")));
- (NSDictionary<NSString *, id> * _Nullable)applyKeyDeltaPayloadDelta:(int32_t)delta reason:(NSString *)reason rollbackToken:(NSString * _Nullable)rollbackToken __attribute__((swift_name("applyKeyDeltaPayload(delta:reason:rollbackToken:)")));
- (void)cacheCurrencyPrefs:(id<SharedOyunKeyValueStore>)prefs value:(int32_t)value __attribute__((swift_name("cacheCurrency(prefs:value:)")));
- (void)cacheKeysPrefs:(id<SharedOyunKeyValueStore>)prefs value:(int32_t)value __attribute__((swift_name("cacheKeys(prefs:value:)")));
- (NSString *)displayTextPrefs:(id<SharedOyunKeyValueStore>)prefs value:(int32_t)value __attribute__((swift_name("displayText(prefs:value:)")));
- (int32_t)getCachedCurrencyPrefs:(id<SharedOyunKeyValueStore>)prefs __attribute__((swift_name("getCachedCurrency(prefs:)")));
- (int32_t)getCachedKeysPrefs:(id<SharedOyunKeyValueStore>)prefs __attribute__((swift_name("getCachedKeys(prefs:)")));
- (BOOL)isUnlimitedPrefs:(id<SharedOyunKeyValueStore>)prefs __attribute__((swift_name("isUnlimited(prefs:)")));
- (int32_t)modeOfRole:(NSString * _Nullable)role teacherApproved:(SharedBoolean * _Nullable)teacherApproved __attribute__((swift_name("modeOf(role:teacherApproved:)")));
- (NSDictionary<NSString *, id> *)registrationWalletFields __attribute__((swift_name("registrationWalletFields()")));
- (void)rememberModePrefs:(id<SharedOyunKeyValueStore>)prefs role:(NSString * _Nullable)role teacherApproved:(SharedBoolean * _Nullable)teacherApproved __attribute__((swift_name("rememberMode(prefs:role:teacherApproved:)")));
- (SharedWalletLoadResult *)resolveLoadedWalletPrefs:(id<SharedOyunKeyValueStore>)prefs docKeys:(SharedInt * _Nullable)docKeys docCurrency:(SharedInt * _Nullable)docCurrency __attribute__((swift_name("resolveLoadedWallet(prefs:docKeys:docCurrency:)")));
- (int32_t)visiblePrefs:(id<SharedOyunKeyValueStore>)prefs raw:(int32_t)raw __attribute__((swift_name("visible(prefs:raw:)")));
- (SharedUserWallet *)walletFromCachePrefs:(id<SharedOyunKeyValueStore>)prefs __attribute__((swift_name("walletFromCache(prefs:)")));
- (SharedUserWallet *)walletFromSnapshotPrefs:(id<SharedOyunKeyValueStore>)prefs docKeys:(SharedInt * _Nullable)docKeys docCurrency:(SharedInt * _Nullable)docCurrency questionCredits:(int32_t)questionCredits sobiCredits:(int32_t)sobiCredits plan:(NSString *)plan __attribute__((swift_name("walletFromSnapshot(prefs:docKeys:docCurrency:questionCredits:sobiCredits:plan:)")));
- (SharedUserWallet *)walletFromUpdateResultPrefs:(id<SharedOyunKeyValueStore>)prefs resultData:(id _Nullable)resultData __attribute__((swift_name("walletFromUpdateResult(prefs:resultData:)")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) int32_t DEFAULT_CURRENCY __attribute__((swift_name("DEFAULT_CURRENCY")));
@property (readonly) int32_t DEFAULT_KEYS __attribute__((swift_name("DEFAULT_KEYS")));
@property (readonly) NSString *FIELD_CURRENCY __attribute__((swift_name("FIELD_CURRENCY")));
@property (readonly) NSString *FIELD_KEYS __attribute__((swift_name("FIELD_KEYS")));
@property (readonly) NSString *FIELD_QUESTION_CREDITS __attribute__((swift_name("FIELD_QUESTION_CREDITS")));
@property (readonly) NSString *FIELD_SOBI_CREDITS __attribute__((swift_name("FIELD_SOBI_CREDITS")));
@property (readonly) NSString *FUNCTION_UPDATE_USER_WALLET __attribute__((swift_name("FUNCTION_UPDATE_USER_WALLET")));
@property (readonly) int32_t MODE_LOCKED __attribute__((swift_name("MODE_LOCKED")));
@property (readonly) int32_t MODE_NORMAL __attribute__((swift_name("MODE_NORMAL")));
@property (readonly) int32_t MODE_UNLIMITED __attribute__((swift_name("MODE_UNLIMITED")));
@property (readonly) NSString *PARAM_CURRENCY __attribute__((swift_name("PARAM_CURRENCY")));
@property (readonly) NSString *PARAM_KEYS __attribute__((swift_name("PARAM_KEYS")));
@property (readonly) NSString *PARAM_REASON __attribute__((swift_name("PARAM_REASON")));
@property (readonly) NSString *PARAM_ROLLBACK_TOKEN __attribute__((swift_name("PARAM_ROLLBACK_TOKEN")));
@property (readonly) NSString *PREFS_APP __attribute__((swift_name("PREFS_APP")));
@property (readonly) NSString *PREF_CURRENCY __attribute__((swift_name("PREF_CURRENCY")));
@property (readonly) NSString *PREF_CURRENCY_MIGRATED __attribute__((swift_name("PREF_CURRENCY_MIGRATED")));
@property (readonly) NSString *PREF_WALLET_MODE __attribute__((swift_name("PREF_WALLET_MODE")));
@property (readonly) int32_t UNLIMITED_BALANCE __attribute__((swift_name("UNLIMITED_BALANCE")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WalletLoadResult")))
@interface SharedWalletLoadResult : SharedBase
- (instancetype)initWithWallet:(SharedUserWallet *)wallet patch:(NSDictionary<NSString *, id> *)patch __attribute__((swift_name("init(wallet:patch:)"))) __attribute__((objc_designated_initializer));
- (SharedWalletLoadResult *)doCopyWallet:(SharedUserWallet *)wallet patch:(NSDictionary<NSString *, id> *)patch __attribute__((swift_name("doCopy(wallet:patch:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<NSString *, id> *patch __attribute__((swift_name("patch")));
@property (readonly) SharedUserWallet *wallet __attribute__((swift_name("wallet")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WalletReason")))
@interface SharedWalletReason : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)walletReason __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWalletReason *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *PURCHASE_ROLLBACK __attribute__((swift_name("PURCHASE_ROLLBACK")));
@property (readonly) NSString *SPEND __attribute__((swift_name("SPEND")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccountSettingsRules")))
@interface SharedAccountSettingsRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)accountSettingsRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccountSettingsRules *shared __attribute__((swift_name("shared")));
- (BOOL)showPaymentsPlan:(NSString * _Nullable)plan __attribute__((swift_name("showPayments(plan:)")));
@property (readonly) NSString *PLAY_SUBSCRIPTIONS_URL __attribute__((swift_name("PLAY_SUBSCRIPTIONS_URL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EditProfileRules")))
@interface SharedEditProfileRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)editProfileRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEditProfileRules *shared __attribute__((swift_name("shared")));
- (BOOL)providerAllowsPasswordChangeProviderIds:(NSArray<NSString *> *)providerIds __attribute__((swift_name("providerAllowsPasswordChange(providerIds:)")));
- (BOOL)shouldUpdatePasswordNewPassword:(NSString *)newPassword passwordFieldVisible:(BOOL)passwordFieldVisible __attribute__((swift_name("shouldUpdatePassword(newPassword:passwordFieldVisible:)")));
- (BOOL)showPasswordFieldProviderIds:(NSArray<NSString *> *)providerIds role:(NSString * _Nullable)role userDocLoaded:(BOOL)userDocLoaded __attribute__((swift_name("showPasswordField(providerIds:role:userDocLoaded:)")));
- (NSString *)usernameFieldTextUserId:(NSString *)userId __attribute__((swift_name("usernameFieldText(userId:)")));
@property (readonly) NSString *DELETE_DIALOG_MESSAGE __attribute__((swift_name("DELETE_DIALOG_MESSAGE")));
@property (readonly) NSString *DELETE_DIALOG_TITLE __attribute__((swift_name("DELETE_DIALOG_TITLE")));
@property (readonly) NSString *DELETE_PROGRESS_MESSAGE __attribute__((swift_name("DELETE_PROGRESS_MESSAGE")));
@property (readonly) NSString *PROVIDER_GOOGLE __attribute__((swift_name("PROVIDER_GOOGLE")));
@property (readonly) NSString *PROVIDER_PASSWORD __attribute__((swift_name("PROVIDER_PASSWORD")));
@property (readonly) NSString *ROLE_STUDENT __attribute__((swift_name("ROLE_STUDENT")));
@property (readonly) NSString *TEXT_SAVE __attribute__((swift_name("TEXT_SAVE")));
@property (readonly) NSString *TEXT_SAVING __attribute__((swift_name("TEXT_SAVING")));
@property (readonly) NSString *TOAST_AUTH_FAILED __attribute__((swift_name("TOAST_AUTH_FAILED")));
@property (readonly) NSString *TOAST_DELETED __attribute__((swift_name("TOAST_DELETED")));
@property (readonly) NSString *TOAST_DELETE_RECENT_LOGIN __attribute__((swift_name("TOAST_DELETE_RECENT_LOGIN")));
@property (readonly) NSString *TOAST_FIRESTORE_FAILED __attribute__((swift_name("TOAST_FIRESTORE_FAILED")));
@property (readonly) NSString *TOAST_PASSWORD_RECENT_LOGIN __attribute__((swift_name("TOAST_PASSWORD_RECENT_LOGIN")));
@property (readonly) NSString *TOAST_PASSWORD_UPDATED __attribute__((swift_name("TOAST_PASSWORD_UPDATED")));
@property (readonly) NSString *TOAST_SAVED __attribute__((swift_name("TOAST_SAVED")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FeedbackGate")))
@interface SharedFeedbackGate : SharedBase
- (instancetype)initWithAllowed:(BOOL)allowed prunedTimestamps:(NSString *)prunedTimestamps __attribute__((swift_name("init(allowed:prunedTimestamps:)"))) __attribute__((objc_designated_initializer));
- (SharedFeedbackGate *)doCopyAllowed:(BOOL)allowed prunedTimestamps:(NSString *)prunedTimestamps __attribute__((swift_name("doCopy(allowed:prunedTimestamps:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL allowed __attribute__((swift_name("allowed")));
@property (readonly) NSString *prunedTimestamps __attribute__((swift_name("prunedTimestamps")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FeedbackRules")))
@interface SharedFeedbackRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)feedbackRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFeedbackRules *shared __attribute__((swift_name("shared")));
- (SharedFeedbackGate *)canSendFeedbackSavedTimestamps:(NSString *)savedTimestamps nowMs:(int64_t)nowMs __attribute__((swift_name("canSendFeedback(savedTimestamps:nowMs:)")));
- (BOOL)canSubmitText:(NSString *)text __attribute__((swift_name("canSubmit(text:)")));
- (NSDictionary<NSString *, id> *)feedbackDocUid:(NSString * _Nullable)uid email:(NSString * _Nullable)email message:(NSString *)message appVersion:(NSString * _Nullable)appVersion osVersion:(NSString *)osVersion deviceModel:(NSString *)deviceModel __attribute__((swift_name("feedbackDoc(uid:email:message:appVersion:osVersion:deviceModel:)")));
- (NSString *)recordFeedbackTimestampSavedTimestamps:(NSString *)savedTimestamps nowMs:(int64_t)nowMs __attribute__((swift_name("recordFeedbackTimestamp(savedTimestamps:nowMs:)")));
- (NSString *)sendFailedToastMessage:(NSString * _Nullable)message __attribute__((swift_name("sendFailedToast(message:)")));
@property (readonly) NSString *ANONYMOUS_USER __attribute__((swift_name("ANONYMOUS_USER")));
@property (readonly) NSString *COLLECTION_FEEDBACK __attribute__((swift_name("COLLECTION_FEEDBACK")));
@property (readonly) NSString *FIELD_APP_VERSION __attribute__((swift_name("FIELD_APP_VERSION")));
@property (readonly) NSString *FIELD_CREATED_AT __attribute__((swift_name("FIELD_CREATED_AT")));
@property (readonly) NSString *FIELD_DEVICE_MODEL __attribute__((swift_name("FIELD_DEVICE_MODEL")));
@property (readonly) NSString *FIELD_EMAIL __attribute__((swift_name("FIELD_EMAIL")));
@property (readonly) NSString *FIELD_MESSAGE __attribute__((swift_name("FIELD_MESSAGE")));
@property (readonly) NSString *FIELD_OS_VERSION __attribute__((swift_name("FIELD_OS_VERSION")));
@property (readonly) NSString *FIELD_USER_ID __attribute__((swift_name("FIELD_USER_ID")));
@property (readonly) int32_t MAX_PER_WINDOW __attribute__((swift_name("MAX_PER_WINDOW")));
@property (readonly) NSString *NO_EMAIL __attribute__((swift_name("NO_EMAIL")));
@property (readonly) NSString *PREFS_NAME __attribute__((swift_name("PREFS_NAME")));
@property (readonly) NSString *PREF_TIMESTAMPS __attribute__((swift_name("PREF_TIMESTAMPS")));
@property (readonly) NSString *TOAST_RATE_LIMITED __attribute__((swift_name("TOAST_RATE_LIMITED")));
@property (readonly) NSString *TOAST_SENT __attribute__((swift_name("TOAST_SENT")));
@property (readonly) NSString *UNKNOWN_VERSION __attribute__((swift_name("UNKNOWN_VERSION")));
@property (readonly) int64_t WINDOW_MS __attribute__((swift_name("WINDOW_MS")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("FriendSearchRules")))
@interface SharedFriendSearchRules : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)friendSearchRules __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedFriendSearchRules *shared __attribute__((swift_name("shared")));
- (NSString *)avatarLetterName:(NSString *)name __attribute__((swift_name("avatarLetter(name:)")));
- (NSArray<NSString *> *)dedupeUidsUids:(NSArray<NSString *> *)uids __attribute__((swift_name("dedupeUids(uids:)")));
- (NSString *)emptyListTextTab:(int32_t)tab __attribute__((swift_name("emptyListText(tab:)")));
- (NSString *)followAvatarColorPosition:(int32_t)position __attribute__((swift_name("followAvatarColor(position:)")));
- (NSString *)followedBackToastName:(NSString *)name __attribute__((swift_name("followedBackToast(name:)")));
- (NSString *)followedToastName:(NSString *)name __attribute__((swift_name("followedToast(name:)")));
- (BOOL)hasMoreResultsBatchResultCount:(int32_t)batchResultCount __attribute__((swift_name("hasMoreResults(batchResultCount:)")));
- (int64_t)pageSizeIsLoadMore:(BOOL)isLoadMore __attribute__((swift_name("pageSize(isLoadMore:)")));
- (NSString *)queryEndQuery:(NSString *)query __attribute__((swift_name("queryEnd(query:)")));
- (NSString *)resultCountTextCount:(int32_t)count __attribute__((swift_name("resultCountText(count:)")));
- (NSString *)searchAvatarColorPosition:(int32_t)position __attribute__((swift_name("searchAvatarColor(position:)")));
- (BOOL)shouldClearResultsQuery:(NSString *)query __attribute__((swift_name("shouldClearResults(query:)")));
- (BOOL)shouldSearchQuery:(NSString *)query __attribute__((swift_name("shouldSearch(query:)")));
- (BOOL)showFollowBackButtonTab:(int32_t)tab alreadyFollowing:(BOOL)alreadyFollowing __attribute__((swift_name("showFollowBackButton(tab:alreadyFollowing:)")));
@property (readonly) int64_t FIRST_PAGE_SIZE __attribute__((swift_name("FIRST_PAGE_SIZE")));
@property (readonly) NSArray<NSString *> *FOLLOW_AVATAR_COLORS __attribute__((swift_name("FOLLOW_AVATAR_COLORS")));
@property (readonly) int64_t LOAD_MORE_PAGE_SIZE __attribute__((swift_name("LOAD_MORE_PAGE_SIZE")));
@property (readonly) int32_t MIN_QUERY_LENGTH_TO_KEEP __attribute__((swift_name("MIN_QUERY_LENGTH_TO_KEEP")));
@property (readonly) int32_t MIN_QUERY_LENGTH_TO_SEARCH __attribute__((swift_name("MIN_QUERY_LENGTH_TO_SEARCH")));
@property (readonly) NSArray<NSString *> *SEARCH_AVATAR_COLORS __attribute__((swift_name("SEARCH_AVATAR_COLORS")));
@property (readonly) int64_t SEARCH_DELAY_MS __attribute__((swift_name("SEARCH_DELAY_MS")));
@property (readonly) NSString *TEXT_EMPTY_FOLLOWERS __attribute__((swift_name("TEXT_EMPTY_FOLLOWERS")));
@property (readonly) NSString *TEXT_EMPTY_FOLLOWING __attribute__((swift_name("TEXT_EMPTY_FOLLOWING")));
@property (readonly) NSString *TEXT_LOADING_MORE __attribute__((swift_name("TEXT_LOADING_MORE")));
@property (readonly) NSString *TEXT_LOAD_MORE __attribute__((swift_name("TEXT_LOAD_MORE")));
@property (readonly) NSString *TEXT_NOT_FOUND __attribute__((swift_name("TEXT_NOT_FOUND")));
@property (readonly) NSString *TOAST_ALREADY_FOLLOWING __attribute__((swift_name("TOAST_ALREADY_FOLLOWING")));
@property (readonly) NSString *TOAST_NO_SESSION __attribute__((swift_name("TOAST_NO_SESSION")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ProfileBadgeRenderConfig")))
@interface SharedProfileBadgeRenderConfig : SharedBase
- (instancetype)initWithBaseAsset:(NSString * _Nullable)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topBackgroundRes:(NSString * _Nullable)topBackgroundRes __attribute__((swift_name("init(baseAsset:topAsset:topHoldFrame:topScale:topBackgroundRes:)"))) __attribute__((objc_designated_initializer));
- (SharedProfileBadgeRenderConfig *)doCopyBaseAsset:(NSString * _Nullable)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale topBackgroundRes:(NSString * _Nullable)topBackgroundRes __attribute__((swift_name("doCopy(baseAsset:topAsset:topHoldFrame:topScale:topBackgroundRes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable baseAsset __attribute__((swift_name("baseAsset")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) NSString * _Nullable topBackgroundRes __attribute__((swift_name("topBackgroundRes")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ProfileBadgeSlotUi")))
@interface SharedProfileBadgeSlotUi : SharedBase
- (instancetype)initWithKindName:(NSString *)kindName unlocked:(BOOL)unlocked showValue:(BOOL)showValue valueText:(NSString *)valueText baseAsset:(NSString *)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale hasCircleFrame:(BOOL)hasCircleFrame toneName:(NSString *)toneName __attribute__((swift_name("init(kindName:unlocked:showValue:valueText:baseAsset:topAsset:topHoldFrame:topScale:hasCircleFrame:toneName:)"))) __attribute__((objc_designated_initializer));
- (SharedProfileBadgeSlotUi *)doCopyKindName:(NSString *)kindName unlocked:(BOOL)unlocked showValue:(BOOL)showValue valueText:(NSString *)valueText baseAsset:(NSString *)baseAsset topAsset:(NSString *)topAsset topHoldFrame:(float)topHoldFrame topScale:(float)topScale hasCircleFrame:(BOOL)hasCircleFrame toneName:(NSString *)toneName __attribute__((swift_name("doCopy(kindName:unlocked:showValue:valueText:baseAsset:topAsset:topHoldFrame:topScale:hasCircleFrame:toneName:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *baseAsset __attribute__((swift_name("baseAsset")));
@property (readonly) BOOL hasCircleFrame __attribute__((swift_name("hasCircleFrame")));
@property (readonly) NSString *kindName __attribute__((swift_name("kindName")));
@property (readonly) BOOL showValue __attribute__((swift_name("showValue")));
@property (readonly) NSString *toneName __attribute__((swift_name("toneName")));
@property (readonly) NSString *topAsset __attribute__((swift_name("topAsset")));
@property (readonly) float topHoldFrame __attribute__((swift_name("topHoldFrame")));
@property (readonly) float topScale __attribute__((swift_name("topScale")));
@property (readonly) BOOL unlocked __attribute__((swift_name("unlocked")));
@property (readonly) NSString *valueText __attribute__((swift_name("valueText")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ProfileCupCard")))
@interface SharedProfileCupCard : SharedBase
- (instancetype)initWithTitle:(NSString *)title animFile:(NSString *)animFile showForbiddenIcon:(BOOL)showForbiddenIcon field:(NSString *)field __attribute__((swift_name("init(title:animFile:showForbiddenIcon:field:)"))) __attribute__((objc_designated_initializer));
- (SharedProfileCupCard *)doCopyTitle:(NSString *)title animFile:(NSString *)animFile showForbiddenIcon:(BOOL)showForbiddenIcon field:(NSString *)field __attribute__((swift_name("doCopy(title:animFile:showForbiddenIcon:field:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *animFile __attribute__((swift_name("animFile")));
@property (readonly) NSString *field __attribute__((swift_name("field")));
@property (readonly) BOOL showForbiddenIcon __attribute__((swift_name("showForbiddenIcon")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ProfileFragmentLogic")))
@interface SharedProfileFragmentLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)profileFragmentLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedProfileFragmentLogic *shared __attribute__((swift_name("shared")));
- (NSString *)avatarDrawableNameSelectedAvatar:(int32_t)selectedAvatar __attribute__((swift_name("avatarDrawableName(selectedAvatar:)")));
- (int32_t)badgeLevelKind:(SharedBadgeKind *)kind progress:(SharedUserBadgeProgress *)progress __attribute__((swift_name("badgeLevel(kind:progress:)")));
- (double)badgeSortingWeightKind:(SharedBadgeKind *)kind progress:(SharedUserBadgeProgress *)progress __attribute__((swift_name("badgeSortingWeight(kind:progress:)")));
- (NSString *)completionTextPercentage:(int32_t)percentage __attribute__((swift_name("completionText(percentage:)")));
- (int32_t)cupScoreRawFieldValue:(id _Nullable)rawFieldValue __attribute__((swift_name("cupScore(rawFieldValue:)")));
- (NSString *)displayNameDocName:(NSString * _Nullable)docName authDisplayName:(NSString * _Nullable)authDisplayName isOtherUser:(BOOL)isOtherUser __attribute__((swift_name("displayName(docName:authDisplayName:isOtherUser:)")));
- (BOOL)followButtonTapUnfollowsFollowing:(BOOL)following __attribute__((swift_name("followButtonTapUnfollows(following:)")));
- (NSString *)followTargetUserIdUsernameAndDateText:(NSString *)usernameAndDateText __attribute__((swift_name("followTargetUserId(usernameAndDateText:)")));
- (float)frameToProgressFrame:(float)frame startFrame:(float)startFrame endFrame:(float)endFrame __attribute__((swift_name("frameToProgress(frame:startFrame:endFrame:)")));
- (BOOL)isValidAvatarSelectedAvatar:(int32_t)selectedAvatar __attribute__((swift_name("isValidAvatar(selectedAvatar:)")));
- (SharedKotlinPair<NSArray<SharedInt *> *, SharedInt *> * _Nullable)levelSpecKind:(SharedBadgeKind *)kind __attribute__((swift_name("levelSpec(kind:)")));
- (int64_t)longestStreakRawFieldValue:(id _Nullable)rawFieldValue __attribute__((swift_name("longestStreak(rawFieldValue:)")));
- (NSDictionary<NSString *, id> *)missingBadgeDefaultsExistingFields:(NSSet<NSString *> *)existingFields __attribute__((swift_name("missingBadgeDefaults(existingFields:)")));
- (int32_t)nudgedFollowersCountCurrent:(int32_t)current delta:(int32_t)delta __attribute__((swift_name("nudgedFollowersCount(current:delta:)")));
- (NSArray<NSString *> *)ownedPreviewBeadTypesAllTypeNames:(NSArray<NSString *> *)allTypeNames ownedCounts:(NSDictionary<NSString *, SharedInt *> *)ownedCounts initialType:(NSString *)initialType __attribute__((swift_name("ownedPreviewBeadTypes(allTypeNames:ownedCounts:initialType:)")));
- (NSArray<SharedBadgeState *> *)pickProfileBadgesStates:(NSArray<SharedBadgeState *> *)states progress:(SharedUserBadgeProgress *)progress slotCount:(int32_t)slotCount __attribute__((swift_name("pickProfileBadges(states:progress:slotCount:)")));
- (int32_t)previewBeadStepIndex:(int32_t)index size:(int32_t)size forward:(BOOL)forward __attribute__((swift_name("previewBeadStep(index:size:forward:)")));
- (SharedProfileBadgeRenderConfig *)profileBadgeRenderConfigType:(SharedBadgeKind *)type __attribute__((swift_name("profileBadgeRenderConfig(type:)")));
- (NSArray<SharedProfileBadgeSlotUi *> *)profileBadgeSlotsProgress:(SharedUserBadgeProgress *)progress __attribute__((swift_name("profileBadgeSlots(progress:)")));
- (NSArray<SharedProfileBadgeSlotUi *> *)profileBadgeSlotsFromStateDocDoc:(NSDictionary<NSString *, id> * _Nullable)doc __attribute__((swift_name("profileBadgeSlotsFromStateDoc(doc:)")));
- (NSString *)profileCollectionIsOtherUser:(BOOL)isOtherUser __attribute__((swift_name("profileCollection(isOtherUser:)")));
- (NSString *)subscriptionTextPlan:(NSString * _Nullable)plan __attribute__((swift_name("subscriptionText(plan:)")));
- (NSString *)totalTimeTextTotalSeconds:(int64_t)totalSeconds __attribute__((swift_name("totalTimeText(totalSeconds:)")));
- (NSArray<NSString *> *)unfollowDeletePathsMyUid:(NSString *)myUid targetUid:(NSString *)targetUid __attribute__((swift_name("unfollowDeletePaths(myUid:targetUid:)")));
- (NSString *)usernameAndDateTextUserId:(NSString *)userId year:(int32_t)year __attribute__((swift_name("usernameAndDateText(userId:year:)")));
@property (readonly) NSArray<NSString *> *ALL_BEAD_TYPE_NAMES __attribute__((swift_name("ALL_BEAD_TYPE_NAMES")));
@property (readonly) NSString *ALWAYS_OWNED_BEAD __attribute__((swift_name("ALWAYS_OWNED_BEAD")));
@property (readonly) int32_t AVATAR_COUNT __attribute__((swift_name("AVATAR_COUNT")));
@property (readonly) NSDictionary<NSString *, id> *BADGE_PROGRESS_DEFAULTS __attribute__((swift_name("BADGE_PROGRESS_DEFAULTS")));
@property (readonly) NSString *BADGE_PROGRESS_DOCUMENT __attribute__((swift_name("BADGE_PROGRESS_DOCUMENT")));
@property (readonly) NSString *BADGE_PROGRESS_SUBCOLLECTION __attribute__((swift_name("BADGE_PROGRESS_SUBCOLLECTION")));
@property (readonly) NSString *COLLECTION_PUBLIC_PROFILES __attribute__((swift_name("COLLECTION_PUBLIC_PROFILES")));
@property (readonly) NSString *COLLECTION_USERS __attribute__((swift_name("COLLECTION_USERS")));
@property (readonly) NSArray<SharedProfileCupCard *> *CUP_CARDS __attribute__((swift_name("CUP_CARDS")));
@property (readonly) NSString *CUP_PROGRESS_DOCUMENT __attribute__((swift_name("CUP_PROGRESS_DOCUMENT")));
@property (readonly) NSString *CUP_PROGRESS_SUBCOLLECTION __attribute__((swift_name("CUP_PROGRESS_SUBCOLLECTION")));
@property (readonly) NSString *DEFAULT_AVATAR_DRAWABLE __attribute__((swift_name("DEFAULT_AVATAR_DRAWABLE")));
@property (readonly) int32_t DEFAULT_CUP_SCORE __attribute__((swift_name("DEFAULT_CUP_SCORE")));
@property (readonly) NSString *DEFAULT_DISPLAY_NAME __attribute__((swift_name("DEFAULT_DISPLAY_NAME")));
@property (readonly) NSString *FIELD_CREATED_AT __attribute__((swift_name("FIELD_CREATED_AT")));
@property (readonly) NSString *FIELD_FOLLOWERS_COUNT __attribute__((swift_name("FIELD_FOLLOWERS_COUNT")));
@property (readonly) NSString *FIELD_FOLLOWING_COUNT __attribute__((swift_name("FIELD_FOLLOWING_COUNT")));
@property (readonly) NSString *FIELD_LONGEST_STREAK __attribute__((swift_name("FIELD_LONGEST_STREAK")));
@property (readonly) NSString *FIELD_NAME __attribute__((swift_name("FIELD_NAME")));
@property (readonly) NSString *FIELD_PLAN __attribute__((swift_name("FIELD_PLAN")));
@property (readonly) NSString *FIELD_SELECTED_AVATAR __attribute__((swift_name("FIELD_SELECTED_AVATAR")));
@property (readonly) NSString *FIELD_TOTAL_TIME_SPENT __attribute__((swift_name("FIELD_TOTAL_TIME_SPENT")));
@property (readonly) NSString *FIELD_USER_ID __attribute__((swift_name("FIELD_USER_ID")));
@property (readonly) NSArray<SharedBadgeKind *> *FIRST_GROUP_ORDER __attribute__((swift_name("FIRST_GROUP_ORDER")));
@property (readonly) NSArray<SharedBadgeKind *> *LOCKED_PRIORITY_ORDER __attribute__((swift_name("LOCKED_PRIORITY_ORDER")));
@property (readonly) int64_t PREVIEW_BEAD_SYNC_DELAY_MS __attribute__((swift_name("PREVIEW_BEAD_SYNC_DELAY_MS")));
@property (readonly) int32_t PROFILE_BADGE_SLOT_COUNT __attribute__((swift_name("PROFILE_BADGE_SLOT_COUNT")));
@property (readonly) NSArray<SharedBadgeKind *> *SECOND_GROUP_DEFAULT_ORDER __attribute__((swift_name("SECOND_GROUP_DEFAULT_ORDER")));
@property (readonly) NSString *TEXT_COMPLETION_LOADING __attribute__((swift_name("TEXT_COMPLETION_LOADING")));
@property (readonly) NSString *TEXT_FOLLOW __attribute__((swift_name("TEXT_FOLLOW")));
@property (readonly) NSString *TEXT_FOLLOWING __attribute__((swift_name("TEXT_FOLLOWING")));
@property (readonly) NSString *TOAST_FOLLOWED __attribute__((swift_name("TOAST_FOLLOWED")));
@property (readonly) NSString *TOAST_SINGLE_BEAD __attribute__((swift_name("TOAST_SINGLE_BEAD")));
@end

@interface SharedLessonItem (Extensions)
- (id<SharedOyunLessonItem>)asOyunLessonItem __attribute__((swift_name("asOyunLessonItem()")));
@end

@interface SharedCupMathOperation (Extensions)
- (SharedMathOperation_ *)toMathOperation __attribute__((swift_name("toMathOperation()")));
@end

@interface SharedCupRuleEngineCupLessonItemOverride (Extensions)
- (SharedLessonItem *)applyToPlaceholderPlaceholder:(SharedLessonItem *)placeholder __attribute__((swift_name("applyToPlaceholder(placeholder:)")));
@end

@interface SharedMathOperation_ (Extensions)
- (SharedMathOperation *)toOyun __attribute__((swift_name("toOyun()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BadgeProgressFirestoreKt")))
@interface SharedBadgeProgressFirestoreKt : SharedBase
+ (SharedInt * _Nullable)firestoreIntOrNull:(id _Nullable)receiver __attribute__((swift_name("firestoreIntOrNull(_:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GorevGunlukSoruKopruleriKt")))
@interface SharedGorevGunlukSoruKopruleriKt : SharedBase
+ (SharedMissionsProgressStore *)bellekIciGorevDeposuKurElapsedRealtimeMs:(SharedLong *(^)(void))elapsedRealtimeMs ornekSayaclar:(NSDictionary<NSString *, SharedInt *> *)ornekSayaclar __attribute__((swift_name("bellekIciGorevDeposuKur(elapsedRealtimeMs:ornekSayaclar:)")));
+ (SharedBellekIciGunlukSoruDeposu *)bellekIciGunlukSoruDeposuKurUid:(NSString * _Nullable)uid builder:(SharedDailyQuestionPoolBuilder *)builder ornekBasariOrani:(SharedInt * _Nullable)ornekBasariOrani __attribute__((swift_name("bellekIciGunlukSoruDeposuKur(uid:builder:ornekBasariOrani:)")));
+ (SharedDailyQuestionBrokenHeartStore *)bellekIciKalpDeposuKurUid:(NSString * _Nullable)uid __attribute__((swift_name("bellekIciKalpDeposuKur(uid:)")));
+ (SharedDailyQuestionCardProgressAnimStore *)bellekIciKartAnimDeposuKurUid:(NSString * _Nullable)uid __attribute__((swift_name("bellekIciKartAnimDeposuKur(uid:)")));
+ (SharedMissionsProgressStore *)gorevDeposuKurPrefsProvider:(id<SharedOyunKeyValueStore> (^)(NSString *))prefsProvider cloud:(id<SharedMissionCloudGateway>)cloud elapsedRealtimeMs:(SharedLong *(^)(void))elapsedRealtimeMs __attribute__((swift_name("gorevDeposuKur(prefsProvider:cloud:elapsedRealtimeMs:)")));
+ (SharedGorevEkraniDurumu *)gorevEkraniDurumuStore:(SharedMissionsProgressStore *)store __attribute__((swift_name("gorevEkraniDurumu(store:)")));
+ (SharedGunlukSoruBaslatma * _Nullable)gunlukSoruBaslatmaChallenge:(SharedDailyQuestionChallenge *)challenge __attribute__((swift_name("gunlukSoruBaslatma(challenge:)")));
+ (float)gunlukSoruCubukAnimasyonYuzdesiStartPct:(float)startPct endPct:(float)endPct t:(float)t __attribute__((swift_name("gunlukSoruCubukAnimasyonYuzdesi(startPct:endPct:t:)")));
+ (SharedGunlukSoruCubukGorunumu *)gunlukSoruCubukGorunumuUi:(SharedDailyQuestionCardUiState *)ui pct:(float)pct trackWidth:(int32_t)trackWidth startInset:(int32_t)startInset endGap:(int32_t)endGap __attribute__((swift_name("gunlukSoruCubukGorunumu(ui:pct:trackWidth:startInset:endGap:)")));
+ (SharedGunlukSoruBaslatma * _Nullable)gunlukSoruDevamBaslatmaChallenge:(SharedDailyQuestionChallenge *)challenge __attribute__((swift_name("gunlukSoruDevamBaslatma(challenge:)")));
+ (SharedDailyQuestionCardUiState *)gunlukSoruDevamSonrasiKartChallenge:(SharedDailyQuestionChallenge *)challenge poolAvailable:(BOOL)poolAvailable __attribute__((swift_name("gunlukSoruDevamSonrasiKart(challenge:poolAvailable:)")));
+ (SharedDailyQuestionPoolBuilder *)gunlukSoruHavuzuVarsayilan __attribute__((swift_name("gunlukSoruHavuzuVarsayilan()")));
+ (NSArray<SharedDailyQuestionSource *> *)gunlukSoruKaynaklariBuilder:(SharedDailyQuestionPoolBuilder *)builder part1Items:(NSArray<SharedLessonItem *> *)part1Items part2Items:(NSArray<SharedLessonItem *> *)part2Items part3Items:(NSArray<SharedLessonItem *> *)part3Items __attribute__((swift_name("gunlukSoruKaynaklari(builder:part1Items:part2Items:part3Items:)")));
+ (SharedDailyQuestionCardUiState *)gunlukSoruOdulAlindiKartState:(SharedDailyQuestionCardUiState *)state __attribute__((swift_name("gunlukSoruOdulAlindiKart(state:)")));
+ (BOOL)gunlukSoruSonucunuIsleDepo:(SharedBellekIciGunlukSoruDeposu *)depo kalpDeposu:(SharedDailyQuestionBrokenHeartStore *)kalpDeposu periodKey:(NSString *)periodKey slotIndex:(int32_t)slotIndex isSuccess:(BOOL)isSuccess nowMs:(int64_t)nowMs __attribute__((swift_name("gunlukSoruSonucunuIsle(depo:kalpDeposu:periodKey:slotIndex:isSuccess:nowMs:)")));
+ (SharedDailyQuestionCardUiState *)gunlukSoruVarsayilanKartPoolAvailable:(BOOL)poolAvailable nowMs:(int64_t)nowMs __attribute__((swift_name("gunlukSoruVarsayilanKart(poolAvailable:nowMs:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GuidePanelDataKt")))
@interface SharedGuidePanelDataKt : SharedBase
+ (NSArray<SharedGuidePanelData *> *)marathonGuidePanelData __attribute__((swift_name("marathonGuidePanelData()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KorDersKoprusuKt")))
@interface SharedKorDersKoprusuKt : SharedBase
+ (NSArray<SharedBlindingOperation *> *)korDersIslemlerineCevirOperations:(NSArray<id> *)operations __attribute__((swift_name("korDersIslemlerineCevir(operations:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KupaZamanKt")))
@interface SharedKupaZamanKt : SharedBase
+ (NSString *)kupaCalendarDayIdNowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("kupaCalendarDayId(nowMs:timeZone:)")));
+ (NSString *)kupaDateIdDaysAgo:(int32_t)daysAgo nowMs:(int64_t)nowMs timeZone:(SharedKotlinx_datetimeTimeZone *)timeZone __attribute__((swift_name("kupaDateId(daysAgo:nowMs:timeZone:)")));
+ (int64_t)kupaNowMs __attribute__((swift_name("kupaNowMs()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OyunSaatKt")))
@interface SharedOyunSaatKt : SharedBase
+ (int64_t)oyunSimdiMs __attribute__((swift_name("oyunSimdiMs()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PaketKopruleriKt")))
@interface SharedPaketKopruleriKt : SharedBase
+ (NSArray<id<SharedOyunLessonItem>> *)asOyunLessonItems:(NSArray<SharedLessonItem *> *)receiver __attribute__((swift_name("asOyunLessonItems(_:)")));
+ (SharedDailyQuestionPoolBuilder *)gunlukSoruHavuzuKurRandom:(SharedKotlinRandom *)random __attribute__((swift_name("gunlukSoruHavuzuKur(random:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SharedKotlinArray<T> : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinFloatArray")))
@interface SharedKotlinFloatArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedFloat *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (float)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinFloatIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(float)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinIntArray")))
@interface SharedKotlinIntArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedInt *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int32_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinIntIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int32_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SharedKotlinEnumCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinPair")))
@interface SharedKotlinPair<__covariant A, __covariant B> : SharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("init(first:second:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinPair<A, B> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("doCopy(first:second:)")));
- (BOOL)equalsOther:(id _Nullable)other __attribute__((swift_name("equals(other:)")));
- (int32_t)hashCode __attribute__((swift_name("hashCode()")));
- (NSString *)toString __attribute__((swift_name("toString()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinRandom")))
@interface SharedKotlinRandom : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) SharedKotlinRandomDefault *companion __attribute__((swift_name("companion")));
- (int32_t)nextBitsBitCount:(int32_t)bitCount __attribute__((swift_name("nextBits(bitCount:)")));
- (BOOL)nextBoolean __attribute__((swift_name("nextBoolean()")));
- (SharedKotlinByteArray *)nextBytesArray:(SharedKotlinByteArray *)array __attribute__((swift_name("nextBytes(array:)")));
- (SharedKotlinByteArray *)nextBytesSize:(int32_t)size __attribute__((swift_name("nextBytes(size:)")));
- (SharedKotlinByteArray *)nextBytesArray:(SharedKotlinByteArray *)array fromIndex:(int32_t)fromIndex toIndex:(int32_t)toIndex __attribute__((swift_name("nextBytes(array:fromIndex:toIndex:)")));
- (double)nextDouble __attribute__((swift_name("nextDouble()")));
- (double)nextDoubleUntil:(double)until __attribute__((swift_name("nextDouble(until:)")));
- (double)nextDoubleFrom:(double)from until:(double)until __attribute__((swift_name("nextDouble(from:until:)")));
- (float)nextFloat __attribute__((swift_name("nextFloat()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
- (int32_t)nextIntUntil:(int32_t)until __attribute__((swift_name("nextInt(until:)")));
- (int32_t)nextIntFrom:(int32_t)from until:(int32_t)until __attribute__((swift_name("nextInt(from:until:)")));
- (int64_t)nextLong __attribute__((swift_name("nextLong()")));
- (int64_t)nextLongUntil:(int64_t)until __attribute__((swift_name("nextLong(until:)")));
- (int64_t)nextLongFrom:(int64_t)from until:(int64_t)until __attribute__((swift_name("nextLong(from:until:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUnit")))
@interface SharedKotlinUnit : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/TimeZoneSerializer))
*/
__attribute__((swift_name("Kotlinx_datetimeTimeZone")))
@interface SharedKotlinx_datetimeTimeZone : SharedBase
@property (class, readonly, getter=companion) SharedKotlinx_datetimeTimeZoneCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedKotlinx_datetimeInstant *)toInstant:(SharedKotlinx_datetimeLocalDateTime *)receiver __attribute__((swift_name("toInstant(_:)")));
- (SharedKotlinx_datetimeLocalDateTime *)toLocalDateTime:(SharedKotlinx_datetimeInstant *)receiver __attribute__((swift_name("toLocalDateTime(_:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinTriple")))
@interface SharedKotlinTriple<__covariant A, __covariant B, __covariant C> : SharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second third:(C _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinTriple<A, B, C> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second third:(C _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
- (BOOL)equalsOther:(id _Nullable)other __attribute__((swift_name("equals(other:)")));
- (int32_t)hashCode __attribute__((swift_name("hashCode()")));
- (NSString *)toString __attribute__((swift_name("toString()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@property (readonly) C _Nullable third __attribute__((swift_name("third")));
@end

__attribute__((swift_name("KotlinThrowable")))
@interface SharedKotlinThrowable : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (SharedKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface SharedKotlinException : SharedKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SharedKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

__attribute__((swift_name("KotlinFloatIterator")))
@interface SharedKotlinFloatIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedFloat *)next __attribute__((swift_name("next()")));
- (float)nextFloat __attribute__((swift_name("nextFloat()")));
@end

__attribute__((swift_name("KotlinIntIterator")))
@interface SharedKotlinIntIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedInt *)next __attribute__((swift_name("next()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinRandom.Default")))
@interface SharedKotlinRandomDefault : SharedKotlinRandom
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (instancetype)default_ __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinRandomDefault *shared __attribute__((swift_name("shared")));
- (int32_t)nextBitsBitCount:(int32_t)bitCount __attribute__((swift_name("nextBits(bitCount:)")));
- (BOOL)nextBoolean __attribute__((swift_name("nextBoolean()")));
- (SharedKotlinByteArray *)nextBytesArray:(SharedKotlinByteArray *)array __attribute__((swift_name("nextBytes(array:)")));
- (SharedKotlinByteArray *)nextBytesSize:(int32_t)size __attribute__((swift_name("nextBytes(size:)")));
- (SharedKotlinByteArray *)nextBytesArray:(SharedKotlinByteArray *)array fromIndex:(int32_t)fromIndex toIndex:(int32_t)toIndex __attribute__((swift_name("nextBytes(array:fromIndex:toIndex:)")));
- (double)nextDouble __attribute__((swift_name("nextDouble()")));
- (double)nextDoubleUntil:(double)until __attribute__((swift_name("nextDouble(until:)")));
- (double)nextDoubleFrom:(double)from until:(double)until __attribute__((swift_name("nextDouble(from:until:)")));
- (float)nextFloat __attribute__((swift_name("nextFloat()")));
- (int32_t)nextInt __attribute__((swift_name("nextInt()")));
- (int32_t)nextIntUntil:(int32_t)until __attribute__((swift_name("nextInt(until:)")));
- (int32_t)nextIntFrom:(int32_t)from until:(int32_t)until __attribute__((swift_name("nextInt(from:until:)")));
- (int64_t)nextLong __attribute__((swift_name("nextLong()")));
- (int64_t)nextLongUntil:(int64_t)until __attribute__((swift_name("nextLong(until:)")));
- (int64_t)nextLongFrom:(int64_t)from until:(int64_t)until __attribute__((swift_name("nextLong(from:until:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinByteArray")))
@interface SharedKotlinByteArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedByte *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int8_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinByteIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int8_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeTimeZone.Companion")))
@interface SharedKotlinx_datetimeTimeZoneCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeTimeZoneCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinx_datetimeTimeZone *)currentSystemDefault __attribute__((swift_name("currentSystemDefault()")));
- (SharedKotlinx_datetimeTimeZone *)ofZoneId:(NSString *)zoneId __attribute__((swift_name("of(zoneId:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) SharedKotlinx_datetimeFixedOffsetTimeZone *UTC __attribute__((swift_name("UTC")));
@property (readonly) NSSet<NSString *> *availableZoneIds __attribute__((swift_name("availableZoneIds")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/InstantIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeInstant")))
@interface SharedKotlinx_datetimeInstant : SharedBase <SharedKotlinComparable>
@property (class, readonly, getter=companion) SharedKotlinx_datetimeInstantCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeInstant *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (SharedKotlinx_datetimeInstant *)minusDuration:(int64_t)duration __attribute__((swift_name("minus(duration:)")));
- (int64_t)minusOther:(SharedKotlinx_datetimeInstant *)other __attribute__((swift_name("minus(other:)")));
- (SharedKotlinx_datetimeInstant *)plusDuration:(int64_t)duration __attribute__((swift_name("plus(duration:)")));
- (int64_t)toEpochMilliseconds __attribute__((swift_name("toEpochMilliseconds()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t epochSeconds __attribute__((swift_name("epochSeconds")));
@property (readonly) int32_t nanosecondsOfSecond __attribute__((swift_name("nanosecondsOfSecond")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalDateTimeIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateTime")))
@interface SharedKotlinx_datetimeLocalDateTime : SharedBase <SharedKotlinComparable>
- (instancetype)initWithDate:(SharedKotlinx_datetimeLocalDate *)date time:(SharedKotlinx_datetimeLocalTime *)time __attribute__((swift_name("init(date:time:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year monthNumber:(int32_t)monthNumber dayOfMonth:(int32_t)dayOfMonth hour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(year:monthNumber:dayOfMonth:hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(SharedKotlinx_datetimeMonth *)month dayOfMonth:(int32_t)dayOfMonth hour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(year:month:dayOfMonth:hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalDateTimeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeLocalDateTime *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinx_datetimeLocalDate *date __attribute__((swift_name("date")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth")));
@property (readonly) SharedKotlinx_datetimeDayOfWeek *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) int32_t hour __attribute__((swift_name("hour")));
@property (readonly) int32_t minute __attribute__((swift_name("minute")));
@property (readonly) SharedKotlinx_datetimeMonth *month __attribute__((swift_name("month")));
@property (readonly) int32_t monthNumber __attribute__((swift_name("monthNumber")));
@property (readonly) int32_t nanosecond __attribute__((swift_name("nanosecond")));
@property (readonly) int32_t second __attribute__((swift_name("second")));
@property (readonly) SharedKotlinx_datetimeLocalTime *time __attribute__((swift_name("time")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end

__attribute__((swift_name("KotlinByteIterator")))
@interface SharedKotlinByteIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedByte *)next __attribute__((swift_name("next()")));
- (int8_t)nextByte __attribute__((swift_name("nextByte()")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializationStrategy")))
@protocol SharedKotlinx_serialization_coreSerializationStrategy
@required
- (void)serializeEncoder:(id<SharedKotlinx_serialization_coreEncoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDeserializationStrategy")))
@protocol SharedKotlinx_serialization_coreDeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<SharedKotlinx_serialization_coreDecoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<SharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreKSerializer")))
@protocol SharedKotlinx_serialization_coreKSerializer <SharedKotlinx_serialization_coreSerializationStrategy, SharedKotlinx_serialization_coreDeserializationStrategy>
@required
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/FixedOffsetTimeZoneSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeFixedOffsetTimeZone")))
@interface SharedKotlinx_datetimeFixedOffsetTimeZone : SharedKotlinx_datetimeTimeZone
- (instancetype)initWithOffset:(SharedKotlinx_datetimeUtcOffset *)offset __attribute__((swift_name("init(offset:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion *companion __attribute__((swift_name("companion")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) SharedKotlinx_datetimeUtcOffset *offset __attribute__((swift_name("offset")));
@property (readonly) int32_t totalSeconds __attribute__((swift_name("totalSeconds"))) __attribute__((deprecated("Use offset.totalSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeInstant.Companion")))
@interface SharedKotlinx_datetimeInstantCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeInstantCompanion *shared __attribute__((swift_name("shared")));
- (SharedKotlinx_datetimeInstant *)fromEpochMillisecondsEpochMilliseconds:(int64_t)epochMilliseconds __attribute__((swift_name("fromEpochMilliseconds(epochMilliseconds:)")));
- (SharedKotlinx_datetimeInstant *)fromEpochSecondsEpochSeconds:(int64_t)epochSeconds nanosecondAdjustment:(int32_t)nanosecondAdjustment __attribute__((swift_name("fromEpochSeconds(epochSeconds:nanosecondAdjustment:)")));
- (SharedKotlinx_datetimeInstant *)fromEpochSecondsEpochSeconds:(int64_t)epochSeconds nanosecondAdjustment_:(int64_t)nanosecondAdjustment __attribute__((swift_name("fromEpochSeconds(epochSeconds:nanosecondAdjustment_:)")));
- (SharedKotlinx_datetimeInstant *)now __attribute__((swift_name("now()"))) __attribute__((unavailable("Use Clock.System.now() instead")));
- (SharedKotlinx_datetimeInstant *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) SharedKotlinx_datetimeInstant *DISTANT_FUTURE __attribute__((swift_name("DISTANT_FUTURE")));
@property (readonly) SharedKotlinx_datetimeInstant *DISTANT_PAST __attribute__((swift_name("DISTANT_PAST")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalDateIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDate")))
@interface SharedKotlinx_datetimeLocalDate : SharedBase <SharedKotlinComparable>
- (instancetype)initWithYear:(int32_t)year monthNumber:(int32_t)monthNumber dayOfMonth:(int32_t)dayOfMonth __attribute__((swift_name("init(year:monthNumber:dayOfMonth:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithYear:(int32_t)year month:(SharedKotlinx_datetimeMonth *)month dayOfMonth:(int32_t)dayOfMonth __attribute__((swift_name("init(year:month:dayOfMonth:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalDateCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeLocalDate *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (int32_t)toEpochDays __attribute__((swift_name("toEpochDays()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth")));
@property (readonly) SharedKotlinx_datetimeDayOfWeek *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) SharedKotlinx_datetimeMonth *month __attribute__((swift_name("month")));
@property (readonly) int32_t monthNumber __attribute__((swift_name("monthNumber")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/LocalTimeIso8601Serializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalTime")))
@interface SharedKotlinx_datetimeLocalTime : SharedBase <SharedKotlinComparable>
- (instancetype)initWithHour:(int32_t)hour minute:(int32_t)minute second:(int32_t)second nanosecond:(int32_t)nanosecond __attribute__((swift_name("init(hour:minute:second:nanosecond:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeLocalTimeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKotlinx_datetimeLocalTime *)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (int32_t)toMillisecondOfDay __attribute__((swift_name("toMillisecondOfDay()")));
- (int64_t)toNanosecondOfDay __attribute__((swift_name("toNanosecondOfDay()")));
- (int32_t)toSecondOfDay __attribute__((swift_name("toSecondOfDay()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t hour __attribute__((swift_name("hour")));
@property (readonly) int32_t minute __attribute__((swift_name("minute")));
@property (readonly) int32_t nanosecond __attribute__((swift_name("nanosecond")));
@property (readonly) int32_t second __attribute__((swift_name("second")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeMonth")))
@interface SharedKotlinx_datetimeMonth : SharedKotlinEnum<SharedKotlinx_datetimeMonth *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinx_datetimeMonth *january __attribute__((swift_name("january")));
@property (class, readonly) SharedKotlinx_datetimeMonth *february __attribute__((swift_name("february")));
@property (class, readonly) SharedKotlinx_datetimeMonth *march __attribute__((swift_name("march")));
@property (class, readonly) SharedKotlinx_datetimeMonth *april __attribute__((swift_name("april")));
@property (class, readonly) SharedKotlinx_datetimeMonth *may __attribute__((swift_name("may")));
@property (class, readonly) SharedKotlinx_datetimeMonth *june __attribute__((swift_name("june")));
@property (class, readonly) SharedKotlinx_datetimeMonth *july __attribute__((swift_name("july")));
@property (class, readonly) SharedKotlinx_datetimeMonth *august __attribute__((swift_name("august")));
@property (class, readonly) SharedKotlinx_datetimeMonth *september __attribute__((swift_name("september")));
@property (class, readonly) SharedKotlinx_datetimeMonth *october __attribute__((swift_name("october")));
@property (class, readonly) SharedKotlinx_datetimeMonth *november __attribute__((swift_name("november")));
@property (class, readonly) SharedKotlinx_datetimeMonth *december __attribute__((swift_name("december")));
+ (SharedKotlinArray<SharedKotlinx_datetimeMonth *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinx_datetimeMonth *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDateTime.Companion")))
@interface SharedKotlinx_datetimeLocalDateTimeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalDateTimeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBuilder:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithDateTime>))builder __attribute__((swift_name("Format(builder:)")));
- (SharedKotlinx_datetimeLocalDateTime *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeDayOfWeek")))
@interface SharedKotlinx_datetimeDayOfWeek : SharedKotlinEnum<SharedKotlinx_datetimeDayOfWeek *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *monday __attribute__((swift_name("monday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *tuesday __attribute__((swift_name("tuesday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *wednesday __attribute__((swift_name("wednesday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *thursday __attribute__((swift_name("thursday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *friday __attribute__((swift_name("friday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *saturday __attribute__((swift_name("saturday")));
@property (class, readonly) SharedKotlinx_datetimeDayOfWeek *sunday __attribute__((swift_name("sunday")));
+ (SharedKotlinArray<SharedKotlinx_datetimeDayOfWeek *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinx_datetimeDayOfWeek *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreEncoder")))
@protocol SharedKotlinx_serialization_coreEncoder
@required
- (id<SharedKotlinx_serialization_coreCompositeEncoder>)beginCollectionDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<SharedKotlinx_serialization_coreCompositeEncoder>)beginStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<SharedKotlinx_serialization_coreEncoder>)encodeInlineDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerialDescriptor")))
@protocol SharedKotlinx_serialization_coreSerialDescriptor
@required

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSArray<id<SharedKotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreSerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSArray<id<SharedKotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) SharedKotlinx_serialization_coreSerialKind *kind __attribute__((swift_name("kind")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDecoder")))
@protocol SharedKotlinx_serialization_coreDecoder
@required
- (id<SharedKotlinx_serialization_coreCompositeDecoder>)beginStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<SharedKotlinx_serialization_coreDecoder>)decodeInlineDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (SharedKotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=kotlinx/datetime/serializers/UtcOffsetSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeUtcOffset")))
@interface SharedKotlinx_datetimeUtcOffset : SharedBase
@property (class, readonly, getter=companion) SharedKotlinx_datetimeUtcOffsetCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t totalSeconds __attribute__((swift_name("totalSeconds")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeFixedOffsetTimeZone.Companion")))
@interface SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeFixedOffsetTimeZoneCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormat")))
@protocol SharedKotlinx_datetimeDateTimeFormat
@required
- (NSString *)formatValue:(id _Nullable)value __attribute__((swift_name("format(value:)")));
- (id<SharedKotlinAppendable>)formatToAppendable:(id<SharedKotlinAppendable>)appendable value:(id _Nullable)value __attribute__((swift_name("formatTo(appendable:value:)")));
- (id _Nullable)parseInput:(id)input __attribute__((swift_name("parse(input:)")));
- (id _Nullable)parseOrNullInput:(id)input __attribute__((swift_name("parseOrNull(input:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalDate.Companion")))
@interface SharedKotlinx_datetimeLocalDateCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalDateCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBlock:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithDate>))block __attribute__((swift_name("Format(block:)")));
- (SharedKotlinx_datetimeLocalDate *)fromEpochDaysEpochDays:(int32_t)epochDays __attribute__((swift_name("fromEpochDays(epochDays:)")));
- (SharedKotlinx_datetimeLocalDate *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeLocalTime.Companion")))
@interface SharedKotlinx_datetimeLocalTimeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeLocalTimeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBuilder:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithTime>))builder __attribute__((swift_name("Format(builder:)")));
- (SharedKotlinx_datetimeLocalTime *)fromMillisecondOfDayMillisecondOfDay:(int32_t)millisecondOfDay __attribute__((swift_name("fromMillisecondOfDay(millisecondOfDay:)")));
- (SharedKotlinx_datetimeLocalTime *)fromNanosecondOfDayNanosecondOfDay:(int64_t)nanosecondOfDay __attribute__((swift_name("fromNanosecondOfDay(nanosecondOfDay:)")));
- (SharedKotlinx_datetimeLocalTime *)fromSecondOfDaySecondOfDay:(int32_t)secondOfDay __attribute__((swift_name("fromSecondOfDay(secondOfDay:)")));
- (SharedKotlinx_datetimeLocalTime *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilder")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilder
@required
- (void)charsValue:(NSString *)value __attribute__((swift_name("chars(value:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithDate")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithDate <SharedKotlinx_datetimeDateTimeFormatBuilder>
@required
- (void)dateFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("date(format:)")));
- (void)dayOfMonthPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("dayOfMonth(padding:)")));
- (void)dayOfWeekNames:(SharedKotlinx_datetimeDayOfWeekNames *)names __attribute__((swift_name("dayOfWeek(names:)")));
- (void)monthNameNames:(SharedKotlinx_datetimeMonthNames *)names __attribute__((swift_name("monthName(names:)")));
- (void)monthNumberPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("monthNumber(padding:)")));
- (void)yearPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("year(padding:)")));
- (void)yearTwoDigitsBaseYear:(int32_t)baseYear __attribute__((swift_name("yearTwoDigits(baseYear:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithTime")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithTime <SharedKotlinx_datetimeDateTimeFormatBuilder>
@required
- (void)amPmHourPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("amPmHour(padding:)")));
- (void)amPmMarkerAm:(NSString *)am pm:(NSString *)pm __attribute__((swift_name("amPmMarker(am:pm:)")));
- (void)hourPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("hour(padding:)")));
- (void)minutePadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("minute(padding:)")));
- (void)secondPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("second(padding:)")));
- (void)secondFractionFixedLength:(int32_t)fixedLength __attribute__((swift_name("secondFraction(fixedLength:)")));
- (void)secondFractionMinLength:(int32_t)minLength maxLength:(int32_t)maxLength __attribute__((swift_name("secondFraction(minLength:maxLength:)")));
- (void)timeFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("time(format:)")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithDateTime")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithDateTime <SharedKotlinx_datetimeDateTimeFormatBuilderWithDate, SharedKotlinx_datetimeDateTimeFormatBuilderWithTime>
@required
- (void)dateTimeFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("dateTime(format:)")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeEncoder")))
@protocol SharedKotlinx_serialization_coreCompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<SharedKotlinx_serialization_coreEncoder>)encodeInlineElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializersModule")))
@interface SharedKotlinx_serialization_coreSerializersModule : SharedBase

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<SharedKotlinx_serialization_coreSerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreKSerializer> _Nullable)getContextualKClass:(id<SharedKotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreSerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<SharedKotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<SharedKotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end

__attribute__((swift_name("KotlinAnnotation")))
@protocol SharedKotlinAnnotation
@required
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerialKind")))
@interface SharedKotlinx_serialization_coreSerialKind : SharedBase
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeDecoder")))
@protocol SharedKotlinx_serialization_coreCompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<SharedKotlinx_serialization_coreDecoder>)decodeInlineElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinNothing")))
@interface SharedKotlinNothing : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeUtcOffset.Companion")))
@interface SharedKotlinx_datetimeUtcOffsetCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeUtcOffsetCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_datetimeDateTimeFormat>)FormatBlock:(void (^)(id<SharedKotlinx_datetimeDateTimeFormatBuilderWithUtcOffset>))block __attribute__((swift_name("Format(block:)")));
- (SharedKotlinx_datetimeUtcOffset *)parseInput:(id)input format:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("parse(input:format:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@property (readonly) SharedKotlinx_datetimeUtcOffset *ZERO __attribute__((swift_name("ZERO")));
@end

__attribute__((swift_name("KotlinAppendable")))
@protocol SharedKotlinAppendable
@required
- (id<SharedKotlinAppendable>)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));
- (id<SharedKotlinAppendable>)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));
- (id<SharedKotlinAppendable>)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimePadding")))
@interface SharedKotlinx_datetimePadding : SharedKotlinEnum<SharedKotlinx_datetimePadding *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinx_datetimePadding *none __attribute__((swift_name("none")));
@property (class, readonly) SharedKotlinx_datetimePadding *zero __attribute__((swift_name("zero")));
@property (class, readonly) SharedKotlinx_datetimePadding *space __attribute__((swift_name("space")));
+ (SharedKotlinArray<SharedKotlinx_datetimePadding *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinx_datetimePadding *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeDayOfWeekNames")))
@interface SharedKotlinx_datetimeDayOfWeekNames : SharedBase
- (instancetype)initWithNames:(NSArray<NSString *> *)names __attribute__((swift_name("init(names:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMonday:(NSString *)monday tuesday:(NSString *)tuesday wednesday:(NSString *)wednesday thursday:(NSString *)thursday friday:(NSString *)friday saturday:(NSString *)saturday sunday:(NSString *)sunday __attribute__((swift_name("init(monday:tuesday:wednesday:thursday:friday:saturday:sunday:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeDayOfWeekNamesCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *names __attribute__((swift_name("names")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeMonthNames")))
@interface SharedKotlinx_datetimeMonthNames : SharedBase
- (instancetype)initWithNames:(NSArray<NSString *> *)names __attribute__((swift_name("init(names:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithJanuary:(NSString *)january february:(NSString *)february march:(NSString *)march april:(NSString *)april may:(NSString *)may june:(NSString *)june july:(NSString *)july august:(NSString *)august september:(NSString *)september october:(NSString *)october november:(NSString *)november december:(NSString *)december __attribute__((swift_name("init(january:february:march:april:may:june:july:august:september:october:november:december:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinx_datetimeMonthNamesCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *names __attribute__((swift_name("names")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerializersModuleCollector")))
@protocol SharedKotlinx_serialization_coreSerializersModuleCollector
@required
- (void)contextualKClass:(id<SharedKotlinKClass>)kClass provider:(id<SharedKotlinx_serialization_coreKSerializer> (^)(NSArray<id<SharedKotlinx_serialization_coreKSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<SharedKotlinKClass>)kClass serializer:(id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<SharedKotlinKClass>)baseClass actualClass:(id<SharedKotlinKClass>)actualClass actualSerializer:(id<SharedKotlinx_serialization_coreKSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<SharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<SharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<SharedKotlinKClass>)baseClass defaultSerializerProvider:(id<SharedKotlinx_serialization_coreSerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end

__attribute__((swift_name("KotlinKDeclarationContainer")))
@protocol SharedKotlinKDeclarationContainer
@required
@end

__attribute__((swift_name("KotlinKAnnotatedElement")))
@protocol SharedKotlinKAnnotatedElement
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((swift_name("KotlinKClassifier")))
@protocol SharedKotlinKClassifier
@required
@end

__attribute__((swift_name("KotlinKClass")))
@protocol SharedKotlinKClass <SharedKotlinKDeclarationContainer, SharedKotlinKAnnotatedElement, SharedKotlinKClassifier>
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end

__attribute__((swift_name("Kotlinx_datetimeDateTimeFormatBuilderWithUtcOffset")))
@protocol SharedKotlinx_datetimeDateTimeFormatBuilderWithUtcOffset <SharedKotlinx_datetimeDateTimeFormatBuilder>
@required
- (void)offsetFormat:(id<SharedKotlinx_datetimeDateTimeFormat>)format __attribute__((swift_name("offset(format:)")));
- (void)offsetHoursPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("offsetHours(padding:)")));
- (void)offsetMinutesOfHourPadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("offsetMinutesOfHour(padding:)")));
- (void)offsetSecondsOfMinutePadding:(SharedKotlinx_datetimePadding *)padding __attribute__((swift_name("offsetSecondsOfMinute(padding:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeDayOfWeekNames.Companion")))
@interface SharedKotlinx_datetimeDayOfWeekNamesCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeDayOfWeekNamesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKotlinx_datetimeDayOfWeekNames *ENGLISH_ABBREVIATED __attribute__((swift_name("ENGLISH_ABBREVIATED")));
@property (readonly) SharedKotlinx_datetimeDayOfWeekNames *ENGLISH_FULL __attribute__((swift_name("ENGLISH_FULL")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_datetimeMonthNames.Companion")))
@interface SharedKotlinx_datetimeMonthNamesCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_datetimeMonthNamesCompanion *shared __attribute__((swift_name("shared")));
@property (readonly) SharedKotlinx_datetimeMonthNames *ENGLISH_ABBREVIATED __attribute__((swift_name("ENGLISH_ABBREVIATED")));
@property (readonly) SharedKotlinx_datetimeMonthNames *ENGLISH_FULL __attribute__((swift_name("ENGLISH_FULL")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
