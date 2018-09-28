package com.hank.web.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;

import com.google.gson.Gson;
import com.hank.domain.Cart;
import com.hank.domain.CartItem;
import com.hank.domain.Category;
import com.hank.domain.Order;
import com.hank.domain.OrderItem;
import com.hank.domain.PageBean;
import com.hank.domain.Product;
import com.hank.domain.User;
import com.hank.service.ProductService;
import com.hank.utils.CommonsUtils;
import com.hank.utils.JedisPoolUtils;
import com.hank.vo.Condition;

import redis.clients.jedis.Jedis;

public class ProductServlet extends BaseServlet {

	/*
	 * public void doGet(HttpServletRequest request, HttpServletResponse response)
	 * throws ServletException, IOException { //获取请求的那个方法的method String
	 * methodName=request.getParameter("method");
	 * if("productList".equals(methodName)) { productList(request, response); }else
	 * if("categoryList".equals(methodName)) { categoryList(request, response);
	 * }else if("index".equals(methodName)) { index(request, response); }else
	 * if("productInfo".equals(methodName)) { productInfo(request, response); } }
	 * 
	 * public void doPost(HttpServletRequest request, HttpServletResponse response)
	 * throws ServletException, IOException { doGet(request, response); }
	 */

	// 获得我的订单
	public void myOrders(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, ServletException {
		HttpSession session = request.getSession();

		// 判断用户是否已经登录 未登录下面代码不执行
		User user = (User) session.getAttribute("user");
		if (user == null) {
			// 没有登录
			response.sendRedirect(request.getContextPath() + "/login.jsp");
			return;
		}

		ProductService service = new ProductService();
		// 查询该用户的所有订单信息(单表查询oredres)
		// 集合中的每一个Oredr对象是不完整的，缺少List<OrderItem> orderItems数据
		List<Order> orderList = service.findAllOrders(user.getUid());
		// 循环所有的订单 为每个订单填充订单信息
		if (orderList != null) {
			for (Order order : orderList) {
				// 获得每一个订单的oid
				String oid = order.getOid();
				// 查询该订单的所有订单项--mapList封装的是多个订单项和该订单项中的商品的信息
				List<Map<String, Object>> mapList = service.findAllOrderItemByOid(oid);

				// 将mapList转换成List<OrderItem> orderItems
				for (Map<String, Object> map : mapList) {

					try {
						// 从map中取出count subtotal封装到OrderItem中
						OrderItem item = new OrderItem();
						BeanUtils.populate(item, map);

						// 从map中取出pimage,pname,shop_price封装到product中
						Product product = new Product();
						BeanUtils.populate(product, map);

						// 将Product封装到OrdreItem中
						item.setProduct(product);

						// 将OredrItem封装到order中的orderItemList中
						order.getOrderItems().add(item);
					} catch (IllegalAccessException | InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}
		
		
//		orderList封装完整了
		request.setAttribute("orderList", orderList);
		request.getRequestDispatcher("/order_list.jsp").forward(request, response);

	}

	// 确认订单---更新收获人信息+在线支付
	public void confirmOrder(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession();

		Order attribute = (Order) session.getAttribute("order");
		double payMoney = attribute.getTotal();
		// 1、更新收货人信息
		Map<String, String[]> properties = request.getParameterMap();
		Order order = new Order();
		try {
			BeanUtils.populate(order, properties);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		ProductService service = new ProductService();
		service.updateOrderAdrr(order);

		session.setAttribute("payMoney", payMoney);
		response.sendRedirect(request.getContextPath() + "/pay.jsp");
	}

	// 提交订单
	public void submitOrder(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession();

		// 判断用户是否已经登录 未登录下面代码不执行
		User user = (User) session.getAttribute("user");
		if (user == null) {
			// 没有登录
			response.sendRedirect(request.getContextPath() + "/login.jsp");
			return;
		}

		// 目的：封装好一个Order对象 传递给service层
		Order order = new Order();

		// 1、private String oid;//该订单的订单号
		String oid = CommonsUtils.getUUID();
		order.setOid(oid);

		// 2、private Date ordertime;//下单时间
		order.setOrdertime(new Date());

		// 3、private double total;//该订单的总金额
		// 获得session中的购物车
		Cart cart = (Cart) session.getAttribute("cart");
		double total = cart.getTotal();
		order.setTotal(total);

		// 4、private int state;//订单支付状态 1代表已付款 0代表未付款
		order.setState(0);

		// 5、private String address;//收货地址
		order.setAddress(null);

		// 6、private String name;//收货人
		order.setName(null);

		// 7、private String telephone;//收货人电话
		order.setTelephone(null);

		// 8、private User user;//该订单属于哪个用户
		order.setUser(user);

		// 9、该订单中有多少订单项List<OrderItem> orderItems = new ArrayList<OrderItem>();
		// 获得购物车中的购物项的集合map
		Map<String, CartItem> cartItems = cart.getCartItems();
		for (Map.Entry<String, CartItem> entry : cartItems.entrySet()) {
			// 取出每一个购物项
			CartItem cartItem = entry.getValue();
			// 创建新的订单项
			OrderItem orderItem = new OrderItem();
			// 1)private String itemid;//订单项的id
			orderItem.setItemid(CommonsUtils.getUUID());
			// 2)private int count;//订单项内商品的购买数量
			orderItem.setCount(cartItem.getBuyNum());
			// 3)private double subtotal;//订单项小计
			orderItem.setSubtotal(cartItem.getSubtotal());
			// 4)private Product product;//订单项内部的商品
			orderItem.setProduct(cartItem.getProduct());
			// 5)private Order order;//该订单项属于哪个订单
			orderItem.setOrder(order);

			// 将该订单项添加到订单的订单项集合中
			order.getOrderItems().add(orderItem);
		}

		// order对象封装完毕
		// 传递数据到service层
		ProductService service = new ProductService();
		service.submitOrder(order);

		session.setAttribute("order", order);

		// 页面跳转
		response.sendRedirect(request.getContextPath() + "/order_info.jsp");

	}

	// 清空购物车
	public void clearCart(HttpServletRequest request, HttpServletResponse response) throws IOException {

		HttpSession session = request.getSession();
		session.removeAttribute("cart");

		// 跳转回购物车
		response.sendRedirect(request.getContextPath() + "/cart.jsp");
	}

	// 删除单一商品
	public void delProFromCart(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 获得要删除吧的pid
		String pid = request.getParameter("pid");
		// 删除session中的购物车中购物项集合中的item
		HttpSession session = request.getSession();
		Cart cart = (Cart) session.getAttribute("cart");
		if (cart != null) {
			Map<String, CartItem> cartItems = cart.getCartItems();

			// 需要修改总价
			cart.setTotal(cart.getTotal() - cartItems.get(pid).getSubtotal());

			cartItems.remove(pid);
			cart.setCartItems(cartItems);
		}

		session.setAttribute("cart", cart);

		// 跳转回购物车
		response.sendRedirect(request.getContextPath() + "/cart.jsp");
	}

	// 将商品添加到购物车
	public void addProductToCart(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession();

		ProductService service = new ProductService();

		// 获得购物车商品的pid
		String pid = request.getParameter("pid");
		// 获得该商品的购买数量
		int buyNum = Integer.parseInt(request.getParameter("buyNum"));

		// 获得product对象
		Product product = service.findProductByPid(pid);
		// 计算小计
		double subtotal = product.getShop_price() * buyNum;

		// 封装CartItem
		CartItem item = new CartItem();
		item.setProduct(product);
		item.setBuyNum(buyNum);
		item.setSubtotal(subtotal);

		// 获得购物车--判断session中是否已经存在购物车
		Cart cart = (Cart) session.getAttribute("cart");
		if (cart == null) {
			cart = new Cart();
		}

		// 将购物项放到购物车中--key是pid
		// 先判断购物车中是否包含此购物项了--判断key是否存在
		Map<String, CartItem> cartItems = cart.getCartItems();
		double newsubtotal = 0.0;
		if (cartItems.containsKey(pid)) {
			// 购物车已经包含此商品---将现在买的商品与原有的数量进行相加
			// 取出原有商品数量
			CartItem cartItem = cartItems.get(pid);
			int oldBuyNum = cartItem.getBuyNum();
			// 修改数量
			cartItem.setBuyNum(oldBuyNum + buyNum);
			// 修改小计
			newsubtotal = buyNum * product.getShop_price();
			double oldsubtotal = cart.getCartItems().get(pid).getSubtotal();
			cartItem.setSubtotal(newsubtotal + oldsubtotal);

			// cart.setCartItems(cartItems);
		} else {
			// 购物车中没有该商品
			cart.getCartItems().put(pid, item);
			newsubtotal = subtotal;
		}
		// 计算总计
		double total = cart.getTotal() + newsubtotal;
		cart.setTotal(total);

		// 将购物车再次放到session中
		session.setAttribute("cart", cart);

		// 直接跳转到购物车页面
		response.sendRedirect(request.getContextPath() + "/cart.jsp");

	}

	// 显示商品类别的功能
	public void categoryList(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ProductService service = new ProductService();

		// 先从缓存中查询category,如果有直接使用，没有再在数据库查询 存到缓存中
		// 获得jedis对象 连接jedis数据库
		Jedis jedis = JedisPoolUtils.getJedis();
		String categoryListJson = jedis.get("categoryListJson");

		// 判断categoryListJson是否为空
		if (categoryListJson == null) {
			System.out.println("缓存没有数据 正在数据库查询");
			// 准备分类数据
			List<Category> categoryList = service.findCategoryList();
			Gson gson = new Gson();
			categoryListJson = gson.toJson(categoryList);
			jedis.set("categoryListJson", categoryListJson);

		}

		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write(categoryListJson);

	}

	// 显示首页的功能
	public void index(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		ProductService service = new ProductService();
		// 准备热门商品List<Product>
		List<Product> hotProductList = service.findHotProductList();

		// 准备最新商品
		List<Product> newProductList = service.findNewProductList();

		// 准备分类数据
		// List<Category> categoryList = service.findCategoryList();
		// request.setAttribute("categoryList", categoryList);

		request.setAttribute("hotProductList", hotProductList);
		request.setAttribute("newProductList", newProductList);

		request.getRequestDispatcher("/index.jsp").forward(request, response);

	}

	// 显示商品详细信息的功能
	public void productInfo(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 获得当前页
		String currentPage = request.getParameter("currentPage");
		// 获得商品类别
		String cid = request.getParameter("cid");

		// 获得要查询的id
		String pid = request.getParameter("pid");

		ProductService service = new ProductService();
		Product product = service.findProductByPid(pid);

		request.setAttribute("product", product);
		request.setAttribute("currentPage", currentPage);
		request.setAttribute("cid", cid);

		// 获得客户端cookie,名字是pids的cookie、
		String pids = pid;
		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("pids".equals(cookie.getName())) {
					pids = cookie.getValue();// 1-3-2
					// 将pids拆成一个数组
					String[] split = pids.split("-");// {1-3-2}
					List<String> asList = Arrays.asList(split);// 将数组转换成集合 [1,3,2]
					LinkedList<String> list = new LinkedList<String>(asList);// [1,3,2]
					// 判断集合是否存在当前pid
					if (list.contains(pid)) {
						// 包含当前查看商品的pid
						list.remove(pid);
						list.addFirst(pid);
					} else {
						// 不包含当前查看商品的pid，直接放在集合头部
						list.addFirst(pid);
					}
					// 将[1,3,2]转成1-3-2;
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < list.size() && i < 7; i++) {
						sb.append(list.get(i));
						sb.append("-");// 1-3-2-
					}
					// 去掉最后一个-
					pids = sb.substring(0, sb.length() - 1);
				}
			}
		}

		Cookie cookie_pids = new Cookie("pids", pids);
		response.addCookie(cookie_pids);

		// 在转发之前创建cookie存储pid

		request.getRequestDispatcher("/product_info.jsp").forward(request, response);

	}

	// 根据商品类别获得商品列表的功能
	public void productList(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 获得cid
		String cid = request.getParameter("cid");

		String currentStr = request.getParameter("currentPage");
		if (currentStr == null)
			currentStr = "1";
		int currentPage = Integer.parseInt(currentStr);
		int currentCount = 12;

		ProductService service = new ProductService();
		PageBean pageBean = service.findProductListByCid(cid, currentPage, currentCount);

		request.setAttribute("pageBean", pageBean);
		request.setAttribute("cid", cid);

		// 定义一个记录历史商品的集合
		List<Product> historyProductList = new ArrayList<Product>();

		// 获得客户端携带pids的cookie
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("pids".equals(cookie.getName())) {
					String pids = cookie.getValue();
					String[] split = pids.split("-");
					for (String pid : split) {
						Product pro = service.findProductByPid(pid);
						historyProductList.add(pro);
					}
				}
			}
		}
		// 将历史浏览商品放到域中
		request.setAttribute("historyProductList", historyProductList);

		request.getRequestDispatcher("/product_list.jsp").forward(request, response);

	}

	/**
	 * 后台管理页面
	 * 
	 * @throws IOException
	 * @throws ServletException
	 */
	public void adminProductList(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 传递请求到service层
		ProductService service = new ProductService();

		// 获得所有的商品的类别数据
		List<Category> categoryList = null;
		try {
			categoryList = service.findAllCategory();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		request.setAttribute("categoryList", categoryList);

		List<Product> productList = null;
		try {
			productList = service.findAllProduct();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 将productList放到request域
		request.setAttribute("productList", productList);

		request.getRequestDispatcher("/admin/product/list.jsp").forward(request, response);

	}

	public void adminAddProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {

		request.setCharacterEncoding("UTF-8");

		// 1、获取数据
		Map<String, String[]> properties = request.getParameterMap();
		// 2、封装数据
		Product product = new Product();
		try {
			BeanUtils.populate(product, properties);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		// 此位置Product已经封装完毕----将表单的数据封装完毕
		// 手动设置表单中没有数据
		// 1）、private String pid;
		product.setPid(UUID.randomUUID().toString());
		// 2）、private String pimage;
		product.setPimage("products/1/c_0033.jpg");
		// 3）、private String pdate;//上架日期
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String pdate = format.format(new Date());
		product.setPdate(pdate);
		// 4）、private int pflag;//商品是否下载 0代表未下架
		product.setPflag(0);

		// 3、传递数据给service层
		ProductService service = new ProductService();
		try {
			service.addProduct(product);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 跳转到列表页面
		response.sendRedirect(request.getContextPath() + "/product?method=adminProductList");
	}

	public void adminAddProductUI(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// 获得所有的商品的类别数据
		ProductService service = new ProductService();
		List<Category> categoryList = null;
		try {
			categoryList = service.findAllCategory();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		request.setAttribute("categoryList", categoryList);

		request.getRequestDispatcher("/admin/product/add.jsp").forward(request, response);

	}

	public void adminDelProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 获取要删除的pid
		String pid = request.getParameter("pid");

		// 传递pid到service层
		ProductService service = new ProductService();
		try {
			service.delProductByPid(pid);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		response.sendRedirect(request.getContextPath() + "/product?method=adminProductList");
	}

	public void adminUpdateProductUI(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// 获得要查询Product的pid
		String pid = request.getParameter("pid");
		// 传递pid查询商品信息
		ProductService service = new ProductService();
		Product product = null;
		product = service.findProductByPid(pid);

		// 获得所有的商品的类别数据
		List<Category> categoryList = null;
		try {
			categoryList = service.findAllCategory();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		request.setAttribute("categoryList", categoryList);
		request.setAttribute("product", product);

		request.getRequestDispatcher("/admin/product/edit.jsp").forward(request, response);
	}

	public void adminUpdateProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {

		request.setCharacterEncoding("UTF-8");

		// 1、获取数据
		Map<String, String[]> properties = request.getParameterMap();
		// 2、封装数据
		Product product = new Product();
		try {
			BeanUtils.populate(product, properties);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		// 此位置Product已经封装完毕----将表单的数据封装完毕
		// 手动设置表单中没有数据
		// 2）、private String pimage;
		product.setPimage("products/1/c_0033.jpg");
		// 3）、private String pdate;//上架日期
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String pdate = format.format(new Date());
		product.setPdate(pdate);
		// 4）、private int pflag;//商品是否下载 0代表未下架
		product.setPflag(0);

		// 3、传递数据给service层
		ProductService service = new ProductService();
		try {
			service.updateProduct(product);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 跳转到列表页面
		response.sendRedirect(request.getContextPath() + "/product?method=adminProductList");
	}

	public void adminSearchProductList(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		request.setCharacterEncoding("UTF-8");

		// 1、收集表单数据
		Map<String, String[]> properties = request.getParameterMap();
		// 2、将散装的查询数据封装到一个VO实体中
		Condition condition = new Condition();
		try {
			BeanUtils.populate(condition, properties);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		// 3、将实体传递给service层
		ProductService service = new ProductService();
		List<Product> productList = null;
		try {
			productList = service.findProductListByCondition(condition);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 准备商品类别
		// 获得所有的商品的类别数据
		List<Category> categoryList = null;
		try {
			categoryList = service.findAllCategory();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		request.setAttribute("condition", condition);

		request.setAttribute("categoryList", categoryList);

		request.setAttribute("productList", productList);

		request.getRequestDispatcher("/admin/product/list.jsp").forward(request, response);
	}
}
