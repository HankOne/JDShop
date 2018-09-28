package com.hank.web.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

import com.hank.domain.User;
import com.hank.service.UserService;
import com.hank.utils.CommonsUtils;
import com.hank.utils.MailUtils;

public class RegisterServlet extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		// 获得表单数据
		Map<String, String[]> properties = request.getParameterMap();
		User user = new User();
		try {
			// 自己指定一个类型转换器（将String转成Date）
			ConvertUtils.register(new Converter() {
				@Override
				public Object convert(Class clazz, Object value) {
					// 将string转成date
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					Date parse = null;
					try {
						parse = format.parse(value.toString());
					} catch (ParseException e) {
						e.printStackTrace();
					}
					return parse;
				}
			}, Date.class);

			// 映射封装
			BeanUtils.populate(user, properties);
		} catch (IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// private String uid;
		user.setUid(CommonsUtils.getUUID());
		// private String telephone;
		user.setTelephone(null);
		// private int state;//是否激活
		user.setState(0);
		// private String code;//激活码
		String activeCode = CommonsUtils.getUUID();
		user.setCode(activeCode);

		// 将user传递给service层

		UserService service = new UserService();
		boolean isRegisterSuccess = service.regist(user);

		// 是否注册成功
		if (isRegisterSuccess) {
			// 发送激活页面
			String emailMsg = "恭喜你注册成功，点击下面页面进行激活账户" + "<a href='http://localhost:8080/Shop/active?activeCode="
					+ activeCode + "'>" + "http://localhost:8080/Shop/active?activeCode=" + activeCode + "</a>";
			System.out.println(emailMsg);
			System.out.println(user.getEmail());
			/*try {
				MailUtils.sendMail(user.getEmail(), emailMsg);
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				System.out.println("没发送就抛异常了");
				e.printStackTrace();
			}*/

			// 跳转到注册成功页面
			response.sendRedirect(request.getContextPath() + "/registerSuccess.jsp");
		} else {
			response.sendRedirect(request.getContextPath() + "/registerFail.jsp");
		}

	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}