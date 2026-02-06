package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;



    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        //将setmealDTO中的属性值拷贝到setmeal对象中
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();


        if(setmealDishes!=null&&setmealDishes.size()>0){
            setmealDishes.forEach(dish->{
                dish.setSetmealId(setmealId);

            });
        }

        //保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);


    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);  //这个SetmealVO里的变量好像跟需求文档里的不太一致，多了很多变量,好像前端没什么问题？

        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        //先一个个检查ids对应的套餐是否是起售种

        ids.forEach(setmealId -> {
            Setmeal setmeal = setmealMapper.getById(setmealId);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        //删除套餐表中的数据
        setmealMapper.deleteByIds(ids);
        //同时删除套餐和菜品的关联数据
        setmealDishMapper.deleteBySetmealIds(ids);

    }

    public Setmeal getSetmealById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        return setmeal;
    }

    @Override
    public SetmealVO getSetmealByIdWithDish(Long id) {
        SetmealVO setmealVO = new SetmealVO();
        Setmeal setmeal = setmealMapper.getById(id);
        if(setmeal!=null){
            BeanUtils.copyProperties(setmeal, setmealVO);
            List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
            setmealVO.setSetmealDishes(setmealDishes);
            return setmealVO;
        }
        return null;
    }



    @Transactional
    public void updateWithDishes(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();

        //更新套餐表中的数据
        BeanUtils.copyProperties(setmealDTO,setmeal);
        List<Long> setmeal_ids = new ArrayList<>();
        setmeal_ids.add(setmealDTO.getId());
        setmealMapper.update(setmeal);

        //删除套餐和菜品的关联数据
        setmealDishMapper.deleteBySetmealId(setmeal.getId());

        //重新插入套餐和菜品的关联数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes!=null&&setmealDishes.size()>0){
            setmealDishes.forEach(setmealDish->{
                setmealDish.setSetmealId(setmeal.getId());//正是因为前端不会把 setmealId 填到 setmealDishes 里的每个对象，所以后端必须自己补上

            });
        }

        setmealDishMapper.insertBatch(setmealDishes);

    }

    public void startOrStop(Integer status, Long id) {
        if(id==null){
            throw new IllegalArgumentException("套餐ID不能为空");
        }

        if(status == StatusConstant.ENABLE){
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList!=null&&dishList.size()>0){
                for(Dish dish:dishList){
                    if(dish.getStatus() == StatusConstant.DISABLE){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }

        Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
        setmealMapper.update(setmeal);
    }
}
