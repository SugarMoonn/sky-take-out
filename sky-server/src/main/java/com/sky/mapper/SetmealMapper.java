package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SetmealMapper {

    @AutoFill(value = com.sky.enumeration.OperationType.INSERT)
    void insert(Setmeal setmeal);


    Integer countByCategoryId(Long id);
}
