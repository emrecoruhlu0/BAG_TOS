package com.bag_tos.roles.town;

import com.bag_tos.common.model.RoleType;
import com.bag_tos.roles.Role;

/**
 * Doktor rolü
 */
public class Doktor extends Role {
    @Override
    public String getName() {
        return "Doktor";
    }

    @Override
    public RoleType getRoleType() {
        return RoleType.DOKTOR;
    }
}