package com.mrlu.tx.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mrlu.tx.entity.Person;
import org.apache.ibatis.annotations.Mapper;

/**
 * (StudentDemo)表数据库访问层
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Mapper
public interface PersonMapper extends BaseMapper<Person> {

}
