package com.hank.service;

import javax.servlet.ServletContext;

import com.hank.dao.LoginDao;
import com.hank.domain.User;

public class LoginService {

	public boolean login(String username, String password) {
		LoginDao dao = new LoginDao();
		User user = dao.login(username, password);
		// 3、根据返回的结果给用户不同显示信息
		if (user != null) {
			return true;
		} else {
			return false;
		}
	}

}
