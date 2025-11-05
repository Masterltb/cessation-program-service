package com.smokefree.program.web.dto.coach;

import java.util.UUID;

public record MyCustomerRes(UUID customerId, String displayName, String currentPlanCode) {}
