package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    @AutoFill(value = com.sky.enumeration.OperationType.INSERT)
    void insert(Setmeal setmeal);


    Integer countByCategoryId(Long id);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void deleteByIds(List<Long> ids);

    @Select("select * from setmeal where id = #{setmealId}")
    Setmeal getById(Long setmealId);

    @AutoFill(value = com.sky.enumeration.OperationType.UPDATE)
    void update(Setmeal setmeal);
}
