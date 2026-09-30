package ir.jetvam.modules.origination.service;

import ir.jetvam.modules.origination.model.ApplicationActionActor;
import ir.jetvam.modules.origination.model.ApplicationJourneyStage;
import ir.jetvam.modules.origination.model.ApplicationJourneyStageStatus;
import ir.jetvam.modules.origination.model.ApplicationStatus;

import java.util.Arrays;
import java.util.List;

/**
 * Projects technical application statuses into an explicit journey and actionable next-step description.
 * This keeps customer and operations screens consistent without duplicating workflow decisions in UI code.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public final class ApplicationJourneyProjector {

    private ApplicationJourneyProjector() {
    }

    public static List<OriginationModels.JourneyStageView> stages(ApplicationStatus status) {
        ApplicationJourneyStage current = stage(status);
        int currentIndex = current.ordinal();
        return Arrays.stream(ApplicationJourneyStage.values())
                .map(stage -> new OriginationModels.JourneyStageView(
                        stage, title(stage), description(stage), stageStatus(status, stage, currentIndex)
                )).toList();
    }

    public static OriginationModels.CurrentActionView currentAction(ApplicationStatus status) {
        return switch (status) {
            case WAITING_CONTROL_CONFIRMATION -> action(ApplicationActionActor.CUSTOMER, true,
                    "START_CONTROLS", "شروع بررسی شرایط", "شرایط و هزینه‌های احتمالی را بررسی و اجرای کنترل‌ها را تأیید کنید.");
            case WAITING_CONTROL_FEE -> action(ApplicationActionActor.CUSTOMER, true,
                    "PAY_CONTROL_FEE", "پرداخت کارمزد استعلام", "برای ارسال استعلام جاری، صورتحساب فعال را پرداخت کنید.");
            case WAITING_CONTROLS -> action(ApplicationActionActor.SYSTEM, false,
                    "WAIT_FOR_CONTROLS", "در حال بررسی سیستمی", "درخواست استعلام ارسال شده است؛ نتیجه پس از callback به‌روز می‌شود.");
            case WAITING_PERSONAL_INFORMATION -> action(ApplicationActionActor.CUSTOMER, true,
                    "COMPLETE_PERSONAL_INFORMATION", "تکمیل اطلاعات فردی", "اطلاعات فردی و بانکی پرونده را تکمیل کنید.");
            case WAITING_EMPLOYMENT_INFORMATION, WAITING_EMPLOYMENT_DOCUMENTS -> action(
                    ApplicationActionActor.CUSTOMER, true, "COMPLETE_EMPLOYMENT",
                    "تکمیل اطلاعات شغلی و مدارک", "اطلاعات تحصیلی، شغلی و مدارک خواسته‌شده را ثبت کنید.");
            case WAITING_GUARANTEE -> action(ApplicationActionActor.CUSTOMER, true,
                    "COMPLETE_GUARANTEES", "تکمیل ضامن و وثیقه", "ضامنین را معرفی و اطلاعات وثیقه‌های مشخص‌شده را تکمیل کنید.");
            case WAITING_APPLICATION_FEE -> action(ApplicationActionActor.CUSTOMER, true,
                    "PAY_APPLICATION_FEES", "پرداخت بدهی درخواست", "صورتحساب‌های فعال این درخواست را پرداخت کنید.");
            case WAITING_ORIGINAL_COLLATERAL -> action(ApplicationActionActor.OPERATIONS, true,
                    "DELIVER_ORIGINAL_COLLATERAL", "تحویل اصل وثیقه", "اصل وثیقه را تحویل دهید؛ سپس کارشناس ستاد دریافت آن را تأیید می‌کند.");
            case WAITING_SIGNATURE -> action(ApplicationActionActor.CUSTOMER, true,
                    "SIGN_CONTRACT", "امضای قرارداد", "قرارداد آماده امضای دیجیتال است.");
            case WAITING_CREDIT_ALLOCATION -> action(ApplicationActionActor.OPERATIONS, false,
                    "WAIT_FOR_CREDIT_ALLOCATION", "منتظر تخصیص اعتبار", "اقدامی از سمت مشتری لازم نیست؛ پرونده در صف تخصیص اعتبار است.");
            case MANUAL_REVIEW -> action(ApplicationActionActor.OPERATIONS, false,
                    "MANUAL_REVIEW", "بررسی کارشناسی", "پرونده نیازمند بررسی کارشناس عملیات است.");
            case REJECTED -> action(ApplicationActionActor.NONE, false,
                    "REJECTED", "درخواست رد شده است", "علت رد در جزئیات پرونده نمایش داده شده است.");
            case COMPLETED -> action(ApplicationActionActor.NONE, false,
                    "COMPLETED", "فرآیند تکمیل شده است", "تسهیلات با موفقیت تخصیص یافته است.");
            case CANCELLED -> action(ApplicationActionActor.NONE, false,
                    "CANCELLED", "درخواست لغو شده است", "این پرونده دیگر اقدام فعالی ندارد.");
        };
    }

    private static OriginationModels.CurrentActionView action(
            ApplicationActionActor actor, boolean customerActionRequired, String code, String title, String description
    ) {
        return new OriginationModels.CurrentActionView(code, title, description, actor, customerActionRequired);
    }

    private static ApplicationJourneyStage stage(ApplicationStatus status) {
        return switch (status) {
            case WAITING_CONTROL_CONFIRMATION, WAITING_CONTROL_FEE, WAITING_CONTROLS, REJECTED, MANUAL_REVIEW ->
                    ApplicationJourneyStage.ELIGIBILITY_CONTROLS;
            case WAITING_PERSONAL_INFORMATION -> ApplicationJourneyStage.PERSONAL_INFORMATION;
            case WAITING_EMPLOYMENT_INFORMATION, WAITING_EMPLOYMENT_DOCUMENTS ->
                    ApplicationJourneyStage.EMPLOYMENT_INFORMATION;
            case WAITING_GUARANTEE -> ApplicationJourneyStage.GUARANTEE_AND_COLLATERAL;
            case WAITING_APPLICATION_FEE -> ApplicationJourneyStage.APPLICATION_FEES;
            case WAITING_ORIGINAL_COLLATERAL -> ApplicationJourneyStage.ORIGINAL_COLLATERAL_DELIVERY;
            case WAITING_SIGNATURE -> ApplicationJourneyStage.CONTRACT_SIGNATURE;
            case WAITING_CREDIT_ALLOCATION, COMPLETED, CANCELLED -> ApplicationJourneyStage.CREDIT_ALLOCATION;
        };
    }

    private static ApplicationJourneyStageStatus stageStatus(
            ApplicationStatus applicationStatus, ApplicationJourneyStage stage, int currentIndex
    ) {
        if (applicationStatus == ApplicationStatus.REJECTED && stage.ordinal() == currentIndex) {
            return ApplicationJourneyStageStatus.FAILED;
        }
        if (applicationStatus == ApplicationStatus.CANCELLED && stage.ordinal() == currentIndex) {
            return ApplicationJourneyStageStatus.CANCELLED;
        }
        if (applicationStatus == ApplicationStatus.COMPLETED || stage.ordinal() < currentIndex) {
            return ApplicationJourneyStageStatus.COMPLETED;
        }
        return stage.ordinal() == currentIndex
                ? ApplicationJourneyStageStatus.CURRENT : ApplicationJourneyStageStatus.UPCOMING;
    }

    private static String title(ApplicationJourneyStage stage) {
        return switch (stage) {
            case PLAN_SELECTION -> "انتخاب طرح";
            case ELIGIBILITY_CONTROLS -> "کنترل شرایط و استعلام‌ها";
            case PERSONAL_INFORMATION -> "اطلاعات فردی و بانکی";
            case EMPLOYMENT_INFORMATION -> "اطلاعات شغلی و مدارک";
            case GUARANTEE_AND_COLLATERAL -> "ضامن و وثیقه";
            case APPLICATION_FEES -> "پرداخت هزینه‌ها";
            case ORIGINAL_COLLATERAL_DELIVERY -> "تحویل اصل وثیقه";
            case CONTRACT_SIGNATURE -> "امضای قرارداد";
            case CREDIT_ALLOCATION -> "تخصیص اعتبار";
        };
    }

    private static String description(ApplicationJourneyStage stage) {
        return switch (stage) {
            case PLAN_SELECTION -> "انتخاب محصول، مبلغ و مدت بازپرداخت";
            case ELIGIBILITY_CONTROLS -> "کنترل‌های محلی و استعلام‌های بانکی موردنیاز طرح";
            case PERSONAL_INFORMATION -> "ثبت اطلاعات پایه موردنیاز پرونده";
            case EMPLOYMENT_INFORMATION -> "ثبت وضعیت شغلی، درآمد و مدارک متناظر";
            case GUARANTEE_AND_COLLATERAL -> "معرفی ضامن و تکمیل وثیقه‌های متقاضی یا ضامن";
            case APPLICATION_FEES -> "تسویه بدهی‌های فعال درخواست";
            case ORIGINAL_COLLATERAL_DELIVERY -> "تحویل و تأیید نسخه فیزیکی وثیقه‌های لازم";
            case CONTRACT_SIGNATURE -> "ساخت و امضای دیجیتال قرارداد";
            case CREDIT_ALLOCATION -> "بررسی نهایی و تخصیص اعتبار";
        };
    }
}
