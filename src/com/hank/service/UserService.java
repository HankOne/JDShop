package com.hank.service;

import java.sql.SQLException;

import com.hank.dao.UserDao;
import com.hank.domain.User;

public class UserService {

	public User login(String username, String password) throws SQLException {
		UserDao dao = new UserDao();
		return dao.login(username, password);
	}

	public boolean regist(User user) {
		UserDao dao = new UserDao();
		int row = 0;
		try {
			row = dao.regist(user);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return row > 0 ? true : false;
	}

	// 校验用户名是否存在
	public boolean checkUsername(String username) {
		UserDao dao = new UserDao();
		long isExist = 0;
		try {
			isExist = dao.checkUsername(username);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return isExist > 0 ? true : false;
	}

}
