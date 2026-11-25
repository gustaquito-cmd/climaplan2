package com.universidad.climaplan.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de invitación a un plan")
public enum PlanMemberStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
