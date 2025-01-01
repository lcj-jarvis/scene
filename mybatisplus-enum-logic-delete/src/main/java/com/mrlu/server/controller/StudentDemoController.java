package com.mrlu.server.controller;


import com.mrlu.response.CommonResults;
import com.mrlu.server.entity.StudentDemo;
import com.mrlu.server.service.StudentDemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * (StudentDemo)表控制层
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@RestController
@RequestMapping("/student")
public class StudentDemoController  {
    /**
     * 服务对象
     */
    @Autowired
    private StudentDemoService studentDemoService;

    /**
     * 通过id查询(未逻辑删除的)详情
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("/{id}")
    public CommonResults<StudentDemo> getById(@PathVariable("id")Integer id) {
        return CommonResults.ok(this.studentDemoService.getById(id));
    }

    /**
     * 获取(未逻辑删除)列表数据
     * http://localhost:8080/mp/student/list
     * @return
     */
    @GetMapping("/list")
    public CommonResults<List<StudentDemo>> list() {
        return CommonResults.ok(this.studentDemoService.list());
    }

    /**
     * 新增
     *
     * 配合枚举的两种反解析方式进行测试
     * 方式一：
     * {
     *             "name": "desFirst",
     *              "finish": {
     *                 "value"" 0,
     *                 "desc": "未结束"
     *              }
     *  }
     *
     * 方式二：
     *{
     *             "name": "desSecond",
     *              "finish": 1
     *  }
     *
     *
     * @param student
     * @return
     */
    @PostMapping("/add")
    public CommonResults<Boolean> add(@RequestBody StudentDemo student) {
        student.setDeleteFlag(0);
        return CommonResults.ok(this.studentDemoService.save(student));
    }

    /**
     * 逻辑删除
     * http://localhost:8080/mp/student/delete/6
     */
    @GetMapping("/delete/{id}")
    public CommonResults<Boolean> logicDelete(@PathVariable("id")Integer id) {
        return CommonResults.ok(this.studentDemoService.removeById(id));
    }

    /**
     * 批量逻辑删除
     * http://localhost:8080/student/batch-delete?ids=4,5,7
     */
    @GetMapping("/batch-delete")
    public CommonResults<Boolean> batchLogicDelete(@RequestParam List<Integer> ids) {
        return CommonResults.ok(this.studentDemoService.removeBatchByIds(ids));
    }

}

