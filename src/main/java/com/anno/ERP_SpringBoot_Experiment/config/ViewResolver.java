package com.anno.ERP_SpringBoot_Experiment.config;

import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ViewResolver {

    private final SecurityUtil securityUtil;

    /**
     * Xác định view class dựa trên role của user hiện tại.
     * <ul>
     *   <li>ADMIN / SUPER_ADMIN → {@link Views.Admin} (full data)</li>
     *   <li>User anonymous hoặc USER → {@link Views.User} (public data)</li>
     * </ul>
     */
    public Class<?> resolveViewClass() {
        if (securityUtil.hasRole("ADMIN") || securityUtil.hasRole("SUPER_ADMIN")) {
            return Views.Admin.class;
        }
        return Views.User.class;
    }
}
