create table t_student_demo(
                               id int primary key auto_increment,
                               name varchar(100),
                               finish int,
                               delete_flag int not null default 0
);