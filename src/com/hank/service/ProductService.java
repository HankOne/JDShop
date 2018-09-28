package com.hank.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.hank.dao.ProductDao;
import com.hank.domain.Category;
import com.hank.domain.Order;
import com.hank.domain.OrderItem;
import com.hank.domain.PageBean;
import com.hank.domain.Product;
import com.hank.utils.DataSourceUtils;
import com.hank.vo.Condition;

public class ProductService {

	// 获得热门商品列表
	public List<Product> findHotProductList() {
		ProductDao dao = new ProductDao();
		List<Product> findHotProductList = null;
		try {
			findHotProductList = dao.findHotProductList();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return findHotProductList;
	}

	// 获得最新商品列表
	public List<Product> findNewProductList() {
		ProductDao dao = new ProductDao();
		List<Product> findNewProductList = null;
		try {
			findNewProductList = dao.findNewProductList();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return findNewProductList;
	}

	public List<Category> findCategoryList() {
		ProductDao dao = new ProductDao();
		List<Category> categoryList = null;
		try {
			categoryList = dao.findCategoryList();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return categoryList;
	}

	public PageBean findProductListByCid(String cid, int currentPage, int currentCount) {
		ProductDao dao = new ProductDao();

		// 封装一个pageBean返回web层
		PageBean<Product> pageBean = new PageBean<Product>();

		// 1.封装当前页
		pageBean.setCurrentPage(currentPage);

		// 2.封装每页显示的条数
		pageBean.setCurrentCount(currentCount);

		// 3.封装总条数
		int totalCount = 0;
		try {
			totalCount = dao.getCount(cid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		pageBean.setTotalCount(totalCount);

		// 4.封装总页数
		int totalPage = (int) Math.ceil(totalCount * 1.0 / currentCount);
		pageBean.setTotalPage(totalPage);

		// 5.封装当前显示的数据
		// 当前页与起始索引index的关系
		int index = (currentPage - 1) * currentCount;
		List<Product> list = null;
		try {
			list = dao.findProductListByCid(cid, index, currentCount);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		pageBean.setList(list);
		return pageBean;
	}

	public Product findProductByPid(String pid) {
		ProductDao dao = new ProductDao();
		Product product = null;
		try {
			product = dao.findProductByPid(pid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return product;
	}

	// 根据关键字查询商品
	public List<Object> findProductByWord(String word) throws SQLException {
		ProductDao dao = new ProductDao();
		return dao.findProductByWord(word);
	}

	// 提交订单 将订单的数据和订单项的数据存储到数据库中
	public void submitOrder(Order order) {

		ProductDao dao = new ProductDao();

		try {
			// 1、开启事务
			DataSourceUtils.startTransaction();
			// 2、调用dao存储order表数据的方法
			dao.addOrders(order);
			// 3、调用dao存储orderitem表数据的方法
			dao.addOrderItem(order);

		} catch (SQLException e) {
			try {
				DataSourceUtils.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				DataSourceUtils.commitAndRelease();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateOrderAdrr(Order order) {
		ProductDao dao = new ProductDao();
		try {
			dao.updateOrderAdrr(order);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 获得所有的类别
	public List<Category> findAllCategory() throws SQLException {
		ProductDao dao = new ProductDao();
		return dao.findAllCategory();
	}

	// 查询所有的商品
	public List<Product> findAllProduct() throws SQLException {
		// 因为没有复杂业务 直接传递请求到dao层
		ProductDao dao = new ProductDao();
		return dao.findAllProduct();
	}

	// 添加数据
	public void addProduct(Product product) throws SQLException {
		ProductDao dao = new ProductDao();
		dao.addProduct(product);
	}

	// 根据pid删除商品
	public void delProductByPid(String pid) throws SQLException {
		ProductDao dao = new ProductDao();
		dao.delProductByPid(pid);
	}

	// 更新商品
	public void updateProduct(Product product) throws SQLException {
		ProductDao dao = new ProductDao();
		dao.updateProduct(product);
	}

	// 根据条件查询商品列表
	public List<Product> findProductListByCondition(Condition condition) throws SQLException {
		ProductDao dao = new ProductDao();
		return dao.findProductListByCondition(condition);
	}

	// 获得指定用户的订单集合
	public List<Order> findAllOrders(String uid) throws SQLException {
		ProductDao dao = new ProductDao();
		List<Order> orderList = dao.findAllOrders(uid);
		return orderList;
	}

	public List<Map<String, Object>> findAllOrderItemByOid(String oid) {
		ProductDao dao = new ProductDao();
		List<Map<String, Object>> mapList = null;
		try {
			mapList = dao.findAllOrderItemByOid(oid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return mapList;
	}
}
