package com.alidao.basic.web.control;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Order;
import com.alidao.basic.entity.Product;
import com.alidao.basic.service.OrderService;
import com.alidao.basic.service.ProductService;
import com.alidao.jse.util.DateUtil;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.Condition;
import com.alidao.jxe.model.Page;
import com.alidao.jxe.model.PageParam;
import com.alidao.users.entity.User;
import com.alidao.users.service.UserService;
import com.alidao.utils.XlsUtil;
import com.google.gson.GsonBuilder;

@Controller
@RequestMapping("order")
public class OrderCtrl extends WebCtrl {

	@Autowired
	private OrderService orderService;
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private UserService userService;

	@RequestMapping("init")
	public void init(Model model, String productId, String userId,Integer productType) {
		model.addAttribute("userId", userId);
		model.addAttribute("productType", productType);
		model.addAttribute("productId", productId);
		if (StringUtil.isEmpty(productId)) {
			Product product =new Product();
			product.addOrderBy("create_time",true);
			product.setType(productType);
			List<Product> products = productService.list(product);
			model.addAttribute("products", products);
		}
	}

	@RequestMapping("list")
	public void list(PageParam pageParam, Order object,
			HttpServletResponse response,Date startDate,Date endDate) throws IOException {
		if (StringUtil.isEmpty(object.getProductId())) {
			object.setProductId(null);
		}
		SimpleDateFormat simple=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(startDate != null){
			object.addCondition("create_time >= '" + simple.format(startDate) + "'", Condition.SEP_AND);
		}
		if(endDate != null){
			object.addCondition("create_time <= '" + simple.format(endDate) + "'", Condition.SEP_AND);
		}
		if (object.getStatus() != null && object.getStatus() == 3) {
			// 等待支付+支付成功
			object.setStatus(null);
			object.addCondition("status in (-1,1)", Condition.SEP_AND);
		}
		Page<Order> page = orderService.page(pageParam, object);
		if (StringUtil.isNotBlank(object.getProductId())) {
			List<Order> list = page.getTableList();
			for (int i = 0; list != null && i < list.size(); i++) {
				Order cell = list.get(i);
				String userId = cell.getUserId();
				Order cdt = new Order();
				cdt.setUserId(userId);
				cdt.setStatus(Order.PAYED);
				String date = DateUtil.formatDate(cell.getCreateTime(), "yyyy-MM-dd HH:mm:ss");
				cdt.addCondition("create_time", Order.CDT_LT, "'" + date + "'", Order.SEP_AND);
				if (orderService.find(cdt) == null) {
					String realname = cell.getUser().getRealname();
					realname += "　<font color='red'>(新)</font>";
					cell.getUser().setRealname(realname);
				}
			}
		}
		GsonBuilder builder = new GsonBuilder();
		builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
		response.getWriter().write(builder.create().toJson(page));
	}

	@RequestMapping("edit")
	public void edit(Model model, String id) {
		if (StringUtil.isNotBlank(id)) {
			model.addAttribute("object", orderService.find(id));
		}
	}

	@RequestMapping("save")
	public void save(Order object, HttpServletResponse response)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(orderService.save(object)).jsonOut(response);
		} else {
			getResponse(orderService.mdfy(object)).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		getResponse(orderService.lose(id)).jsonOut(response);
	}
	
	/**
	 * 订单关闭
	 * @param id
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("close/{id}")
	public void close(@PathVariable("id") String id, 
			HttpServletResponse response) throws Exception {
		Order object = new Order();
		object.setId(id);
		object.setStatus(2);//已关闭
		getResponse(orderService.mdfy(object)).jsonOut(response);
	}
	
	
	/**
	 * 查看订单页面
	 * @param model
	 * @param id
	 */
	@RequestMapping("show")
	public void show(Model model, String id){
		model.addAttribute("object", orderService.find(id));
	}
	
	
	/**
	 * 导出
	 * @param request
	 * @param pageParam
	 * @param response
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping("export")
	public void export(HttpServletRequest request, PageParam pageParam, Order object, HttpServletResponse response,Date startDate,Date endDate) throws Exception{
		SimpleDateFormat simple=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(startDate != null){
			object.addCondition("create_time >= '" + simple.format(startDate) + "'", Condition.SEP_AND);
		}
		if(endDate != null){
			object.addCondition("create_time <= '" + simple.format(endDate) + "'", Condition.SEP_AND);
		}
		OutputStream os = response.getOutputStream();
		
		if (StringUtil.isEmpty(object.getProductId())) {
			object.setProductId(null);
		}
		pageParam.setPageSize(60000L);// 统计的最大数量
		Page<Order> page = orderService.page(pageParam, object);
		Long totalMoney = 0L;
		if (page.getTableList() != null) {
			for (Order order : page.getTableList()) {
				if (order.getStatus() == Order.PAYED) {
					// 支付成功
					totalMoney += (order.getInvestMoney() - order.getCashMoney() - order.getUseIntegral());
				}
			}
		}
		String[] statOfLine = new String[1];// 统计结果行
		statOfLine[0] = "订单总计：" + page.getPageParam().getTotalCount() + "个 订单总支付金额：" + totalMoney + "元 ";
		
		String[] srr = null;
		String[] heads = new String[14];
		List res = new ArrayList();
		int count = 0;
		if (page.getTableList() != null) {
			for (Order order : page.getTableList()) {
				srr = new String[14];// 显示14个订单的字段
				setData(srr, order);// 数据赋值
				if (count == 0) {
					// 第一行中文备注只需赋值一次即可
					setHead(heads);
				}
				res.add(srr);
				count ++;
			}
		}
		res.add(statOfLine);// 加入统计结果
		XlsUtil.setHead("订单导出结果.xls", response);
		try {
			XlsUtil.downXlsData(heads,res,os);
		} finally {
			if (null != os) {
				os.flush();
				os.close();
				os = null;
			}
		}
	}
	
	public void setData(String[] srr, Order order) {
		srr[0] = order.getId();// 订单号
		srr[1] = order.getUserLinkman();// 用户姓名
		srr[2] = order.getUserContact();// 用户联系电话
		User user = userService.find(order.getUserId());
		if (user != null) {
			// 证件类型
			if ("01".equals(user.getCredentialsType())) {
				srr[3] = "身份证";
			} else if ("02".equals(user.getCredentialsType())) {
				srr[3] = "军官证";
			} else if ("03".equals(user.getCredentialsType())) {
				srr[3] = "护照";
			} else if ("04".equals(user.getCredentialsType())) {
				srr[3] = "户口簿";
			} else if ("05".equals(user.getCredentialsType())) {
				srr[3] = "回乡证";
			} else {
				srr[3] = "其他";
			}
			srr[4] = user.getCredentialsCode();// 证件号码
		}
		if (order.getProduct() != null) {
			srr[5] = order.getProduct().getName();// 产品名称
		}
		srr[6] = order.getInvestMoney() + "元";// 投资金额
		srr[7] = (order.getInvestMoney() - order.getCashMoney() - order.getUseIntegral()) + "元";// 支付金额
		// 支付类型
		if (order.getPayType() != null) {
			if (order.getPayType() == 1) {
				srr[8] = "支付宝WAP支付";
			} else if (order.getPayType() == 2) {
				srr[8] = "支付宝快捷支付";
			} else if (order.getPayType() == 3) {
				srr[8] = "微信支付";
			} else if (order.getPayType() == 4) {
				srr[8] = "银联支付";
			} else if (order.getPayType() == 5) {
				srr[8] = "积分支付";
			} else {
				srr[8] = "线下支付";
			}
		} else {
			srr[8] = "";
		}
		srr[9] = order.getSerialNo();// 流水号
		// 订单状态
		if (order.getStatus() == -1) {
			srr[10] = "等待支付";
		} else if (order.getStatus() == 0) {
			srr[10] = "支付定金";
		} else if (order.getStatus() == 1) {
			srr[10] = "支付成功";
		} else {
			srr[10] = "已关闭";
		}
		srr[11] = order.getPayTime() == null ? "" : DateUtil.formatDate(order.getPayTime(), "yyyy-MM-dd hh:mm:ss");// 付款时间
		// 银行
		if ("0308".equals(order.getOpenBankId())) {
			srr[12] = "招商银行";
		} else if ("0105".equals(order.getOpenBankId())) {
			srr[12] = "中国建设银行";
		} else if ("0302".equals(order.getOpenBankId())) {
			srr[12] = "中信银行";
		} else if ("0303".equals(order.getOpenBankId())) {
			srr[12] = "中国光大银行";
		} else if ("0306".equals(order.getOpenBankId())) {
			srr[12] = "广东发展银行";
		} else if ("0305".equals(order.getOpenBankId())) {
			srr[12] = "中国民生银行";
		} else if ("0410".equals(order.getOpenBankId())) {
			srr[12] = "中国平安银行";
		} else if ("0100".equals(order.getOpenBankId())) {
			srr[12] = "邮储银行";
		} else if ("0102".equals(order.getOpenBankId())) {
			srr[12] = "中国工商银行";
		} else if ("0103".equals(order.getOpenBankId())) {
			srr[12] = "中国农业银行";
		} else if ("0104".equals(order.getOpenBankId())) {
			srr[12] = "中国银行";
		} else if ("0301".equals(order.getOpenBankId())) {
			srr[12] = "交通银行";
		} else if ("0307".equals(order.getOpenBankId())) {
			srr[12] = "深发展银行";
		} else if ("0309".equals(order.getOpenBankId())) {
			srr[12] = "兴业银行";
		} else {
			srr[12] = "";
		}
		srr[13] = order.getCardNo();// 卡号
	}
	
	public void setHead(String[] srr) {
		srr[0] = "订单号";
		srr[1] = "用户姓名";
		srr[2] = "用户联系电话";
		srr[3] = "证件类型";
		srr[4] = "证件号码";
		srr[5] = "产品名称";
		srr[6] = "投资金额";
		srr[7] = "支付金额";
		srr[8] = "支付类型";
		srr[9] = "流水号";
		srr[10] = "状态";
		srr[11] = "付款时间";
		srr[12] = "银行";
		srr[13] = "卡号";
	}
	
}
