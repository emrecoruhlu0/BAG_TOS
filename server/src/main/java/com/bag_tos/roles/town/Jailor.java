package com.bag_tos.roles.town;

import com.bag_tos.common.model.RoleType;
import com.bag_tos.roles.Role;

/**
 * Gardiyan rolü
 */
public class Jailor extends Role {
    @Override
    public String getName() {
        return "Gardiyan";
    }

    @Override
    public RoleType getRoleType() {
        return RoleType.JAILOR;
    }
}