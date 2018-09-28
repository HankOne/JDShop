package com.hank.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hank.service.UserService;

public class CheckUsernameServlet extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		//获得用户名
		String username=request.getParameter("username");
		
		UserService service=new UserService();
		boolean isExist=service.checkUsername(username);
		String json="{\"isExist\":"+isExist+"}";
		
		response.getWriter().write(json);

	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}