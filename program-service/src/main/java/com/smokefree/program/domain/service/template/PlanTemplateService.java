package com.smokefree.program.domain.service.template;


import com.smokefree.program.web.dto.plan.PlanRecommendationRes;
import com.smokefree.program.web.dto.plan.PlanTemplateDetailRes;
import com.smokefree.program.web.dto.plan.PlanTemplateSummaryRes;

import java.util.List;
import java.util.UUID;

public interface PlanTemplateService {
    List<PlanTemplateSummaryRes> listAll();
    PlanTemplateDetailRes getDetail(UUID id);
    PlanRecommendationRes recommendBySeverity(String severity); // string: LOW|MODERATE|SEVERE|...
}

