package com.smokefree.program.domain.service.template.impl;

import com.smokefree.program.domain.model.PlanStep;
import com.smokefree.program.domain.model.SeverityLevel;
import com.smokefree.program.domain.repo.PlanStepRepo;
import com.smokefree.program.domain.repo.PlanTemplateRepo;
import com.smokefree.program.domain.service.ContentModuleService;
import com.smokefree.program.domain.service.quiz.SeverityRuleService;
import com.smokefree.program.domain.service.template.PlanTemplateService;
import com.smokefree.program.web.dto.plan.*;
import com.smokefree.program.web.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanTemplateServiceImpl implements PlanTemplateService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ContentModuleService moduleService;
    private final PlanTemplateRepo templateRepo;
    private final PlanStepRepo stepRepo;
    private final SeverityRuleService severityRules;

    @Override
    @Transactional(readOnly = true)
    public List<PlanTemplateSummaryRes> listAll() {
        // sort theo level rồi code cho an toàn
        var templates = templateRepo.findAll(
                Sort.by(Sort.Order.asc("level"), Sort.Order.asc("code"))
        );

        return templates.stream()
                .map(t -> new PlanTemplateSummaryRes(
                        t.getId(),
                        t.getCode(),
                        t.getName(),
                        null,                 // hiện chưa có cột description
                        t.getTotalDays()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlanTemplateDetailRes getDetail(UUID id) {
        var t = templateRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + id));

        var steps = stepRepo.findByTemplateIdOrderByDayNoAscSlotAsc(id);

        // stepNo: đánh số tuần tự theo toàn bộ template
        AtomicInteger seq = new AtomicInteger(0);
        var stepDtos = steps.stream()
                .map(s -> new PlanTemplateDetailRes.StepRes(
                        seq.incrementAndGet(),                  // stepNo tăng dần
                        s.getTitle(),
                        s.getDetails(),
                        (s.getDayNo() == null ? null : Math.max(0, s.getDayNo() - 1)), // dayOffset 0-based
                        null                                    // type: chưa lưu trong DB
                ))
                .toList();

        return new PlanTemplateDetailRes(
                t.getId(),
                t.getCode(),
                t.getName(),
                null,                         // description
                t.getTotalDays(),
                stepDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PlanRecommendationRes recommendBySeverity(String severity) {
        // severity ở đây nhận "LOW"/"MODERATE"/"HIGH" hoặc số score "27"
        SeverityLevel lvl = severityRules.fromRaw(severity);

        String code = severityRules.recommendPlanCode(lvl);

        var t = templateRepo.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Plan template not found by code: " + code));

        String reason = switch (lvl) {
            case HIGH     -> "Điểm/triệu chứng cao → cần lộ trình 60 ngày.";
            case MODERATE -> "Mức trung bình → lộ trình 45 ngày cân bằng cường độ.";
            case LOW      -> "Mức nhẹ → 30 ngày là đủ để hình thành thói quen mới.";
        };

        return new PlanRecommendationRes(
                lvl.name(), t.getId(), t.getCode(), t.getName(), t.getTotalDays(), reason
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PlanDaysRes getDays(UUID templateId, boolean expandModule, String lang) {
        var t = templateRepo.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));

        var steps = stepRepo.findByTemplateIdOrderByDayNoAscSlotAsc(t.getId());

        var grouped = steps.stream().collect(Collectors.groupingBy(
                PlanStep::getDayNo,
                TreeMap::new,
                Collectors.toList()
        ));

        var dayDtos = grouped.entrySet().stream()
                .map(e -> {
                    var stepDtos = e.getValue().stream().map(s -> {
                        String moduleCode = s.getModuleCode();
                        String moduleType = null;
                        PlanStepRes.ModuleBrief moduleBrief = null;

                        boolean hasModule = moduleCode != null && !moduleCode.isBlank();

                        if (hasModule) {
                            try {
                                var m = moduleService.getLatestByCode(moduleCode, lang);
                                moduleType = m.type();
                                if (expandModule) {
                                    moduleBrief = new PlanStepRes.ModuleBrief(
                                            m.version(),
                                            m.type(),
                                            m.payload(),
                                            m.etag()
                                    );
                                }
                            } catch (NotFoundException ex) {
                                // Có thể log cảnh báo nếu cần, hoặc để trống moduleType/moduleBrief
                                 log.warn("Module not found for code={} lang={}", moduleCode, lang, ex);
                            }
                        }


                        return new PlanStepRes(
                                s.getSlot().format(TIME_FMT),
                                s.getTitle(),
                                s.getMaxMinutes(),
                                moduleType,
                                moduleCode,
                                moduleBrief
                        );
                    }).toList();

                    return new PlanDayRes(e.getKey(), stepDtos);
                })
                .toList();

        return new PlanDaysRes(
                t.getId(),
                t.getCode(),
                t.getName(),
                t.getTotalDays(),
                dayDtos
        );
    }



    @Override
    @Transactional(readOnly = true)
    public PlanDaysRes getDaysByCode(String code, boolean expandModule, String lang) {
        var t = templateRepo.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Template code not found: " + code));
        return getDays(t.getId(), expandModule, lang);
    }
}
