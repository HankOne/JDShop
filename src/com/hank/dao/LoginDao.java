package com.hank.dao;

import java.sql.SQLException;

import javax.servlet.ServletContext;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;

import com.hank.domain.User;
import com.hank.utils.DataSourceUtils;

public class LoginDao {

	public User login(String username, String password) {
		// 从数据库中验证该用户名和密码是否正确
		QueryRunner runner = new QueryRunner(DataSourceUtils.getDataSource());
		String sql = "select * from user where username=? and password=?";
		User user = null;
		try {
			// runner.query(sql, rsh, params)
			user = runner.query(sql, new BeanHandler<User>(User.class), username, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return user;

	}

}
