package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.service.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(builder = @org.mapstruct.Builder(disableBuilder = true), config = DefaultConfigMapper.class)
public interface UserMapper extends EntityMapper<UserDto, User>{
}
