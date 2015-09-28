package com.alidao.basic.wap.control;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.CashCoupon;
import com.alidao.basic.entity.Fcode;
import com.alidao.basic.entity.Order;
import com.alidao.basic.entity.Product;
import com.alidao.basic.service.CashCouponService;
import com.alidao.basic.service.FcodeService;
import com.alidao.basic.service.OrderService;
import com.alidao.basic.service.ProductService;
import com.alidao.cnpay.entity.CardBind;
import com.alidao.cnpay.service.CardBindService;
import com.alidao.exception.MessageException;
import com.alidao.jse.util.DateUtil;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.help.PowerHelper;
import com.alidao.jxe.model.Condition;
import com.alidao.jxe.model.Page;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.model.ResponseForAjax;
import com.alidao.jxe.util.HttpUtil;
import com.alidao.users.authorizing.UseridTracker;
import com.alidao.users.entity.User;
import com.alidao.users.entity.UserInvest;
import com.alidao.users.service.UserIntegralService;
import com.alidao.users.service.UserInvestService;
import com.alidao.users.service.UserService;
import com.alidao.wxapi.util.OpenidTracker;
import com.alidao.wxapi.util.WxapiUtil;

@Controller
@RequestMapping("wap/product")
public class ProductWapCtrl extends WebCtrl {

	@Autowired
	private ProductService productService;

	@Autowired
	private FcodeService fcodeService;

	@Autowired
	private UserService userService;

	@Autowired
	private OrderService orderService;
	
	@Autowired
	private UserIntegralService userIntegralService;
	
	@Autowired
	private CashCouponService cashCouponService;
	
	@Autowired
	private UserInvestService userInvestService;
	
	@Autowired
	private CardBindService cardBindService;
	
	@RequestMapping("donate")
	public void donate() throws Exception {
		Order object = new Order();
		object.setStatus(Order.PAYED);
		List<Order> list = orderService.list(object);
		for (Order cell : list) {
			String name = cell.getUserName();
			if (StringUtil.isNotBlank(name)) {
				if (name.indexOf('\\') > -1) {
					CardBind card = new CardBind();
					card.setCardNo(cell.getCardNo());
					card.setStatus(CardBind.BIND_ED);
					card = cardBindService.find(card);
					if (card != null) {
						cell.setUserName(card.getUserName());
						cell.setStatus(null);
						orderService.mdfy(cell);
					}
				}
			}
		}
	}

	/**
	 * 产品列表页面
	 */
	@RequestMapping("list")
	public String list(Model model, String type) {
		// 检查产品是否只有一个,如果只有一个,需求要直接跳到该产品的详情页面,否则正常走列表页面
		Product cdt = new Product();
		if (StringUtil.isNotBlank(type)) {
			cdt.setTypeId(type);
		}
		cdt.addOrderBy("type", true);
		List<Product> list = productService.list(cdt);
		Collections.sort(list);
		model.addAttribute("list", list);
		return "wap/product/list";
		/*if (list != null && list.size() == 1) {	// 转跳到详情页面
			return "redirect:".concat("detail?id=" + list.get(0).getId());
		} else {	// 转跳到列表页面
			Collections.sort(list);
			model.addAttribute("list", list);
			return "wap/product/list";
		}*/
	}

	/**
	 * 分页
	 * @param response
	 * @param pageParam
	 * @param object
	 * @throws IOException
	 */
	@RequestMapping("page")
	public void page(HttpServletResponse response, PageParam pageParam,
			Product object) throws IOException {
		Page<Product> page = productService.page(pageParam, object);
		List<Product> list = page.getTableList() == null ? 
				new ArrayList<Product>() : page.getTableList();
		page.setTableList(list);
		page.jsonOut(response);
	}
	
	/**
	 * 详情
	 * @param model
	 * @param id
	 */
	@RequestMapping("detail")
	public String detail(Model model, String id, String pid, 
			Boolean flag, HttpServletRequest request) {
		Product product = productService.find(id);
		model.addAttribute("object", product);
		String userId = PowerHelper.get();
		if (WxapiUtil.fromMicBrowser(request)) {
			String openid = OpenidTracker.get();
			User user = userService.find(openid, User.BINDED_WECHAT);
			if (user != null) {
				model.addAttribute("user", user);
				userId = user.getId();
				if (StringUtil.isNotBlank(user.getSessionKey())) {
					model.addAttribute("type", 1);//正常显示
				} else {
					model.addAttribute("type", 2);//没有登录,显示登录
				}
			} else {
				model.addAttribute("type", 0);//没有绑定过,显示注册
			}
		}
		int investmentMoney = 0;
		if (userId != null) {
			Order object = new Order();
			object.setUserId(userId);
			object.setProductId(id);
			object.addCondition("status not in(2)", Condition.SEP_AND);// 排除订单状态为已关闭
			List<Order> list = orderService.list(object);
			for (int i = 0; list != null && i < list.size(); i++) {
				Integer investmoney = list.get(i).getInvestMoney();
				if (product.getSmallProduct() == 0) {
					investmoney = investmoney / 10000;
				}
				investmentMoney += investmoney;
			}
			model.addAttribute("investmentMoney", investmentMoney);
			model.addAttribute("userId", userId);
		} else {
			model.addAttribute("investmentMoney", investmentMoney);
		}
		String lctx = HttpUtil.getWebAppUrl(request);
		model.addAttribute("lctx", lctx);
		//邀请人判断
		if (StringUtil.isNotBlank(pid)) {
			model.addAttribute("puser", userService.find(pid));
		}
		if (flag != null) {
			model.addAttribute("flag", flag);
		}
		if(product.getType() == Product.TYPE_SPECIAL){
			return "wap/product/specialDetail";
		}else{
			return "wap/product/detail";
		}
	}

	/**
	 * 查询F码
	 * @return
	 */
	@RequestMapping("fcode")
	public void fcode(HttpServletResponse response, String fcode)
			throws IOException {
		ResponseForAjax responseForAjax = new ResponseForAjax();
		Fcode object = new Fcode();
		object.setFcode(fcode);
		object = fcodeService.find(object);
		if (object != null) {
			if (object.getStatus() == Fcode.STATUS_BE_USED) {
				responseForAjax.setResult(2);
				responseForAjax.setMessage("F码已使用");
			} else {
				if (new Date().getTime() > object.getEndTime().getTime()) {
					responseForAjax.setResult(3);
					responseForAjax.setMessage("F码已过期");
				} else {
					responseForAjax.setResult(1);
				}
			}
		} else {
			responseForAjax.setResult(0);
			responseForAjax.setMessage("F码不存在");
		}
		responseForAjax.jsonOut(response);
	}

	/**
	 * productName产品名称
	 * incomeStartTime收益开始时间
	 * incomeEndTime收益结束时间
	 * totalMoney产品金额
	 * flingMoney起投金额
	 * increaseMoney递增金额
	 * maxMoney最大可投金额
	 * contractTime签约时间
	 * expectIncome年化收益率
	 * allotType收益分配方式
	 * startTime起息日
	 * dateLimit期限
	 * bankName开户行
	 * accountName开户名称
	 * account开户账号
	 * orderNo订单号
	 * createTime下单时间
	 * username高和畅账号
	 * mobile用户联系方式
	 * realName投资人姓名
	 * certType投资人证件类型
	 * certId投资人证件号
	 * investMoney投资金额(转让份额)
	 * investShare投资份额(转让金额)
	 * incomeMoney债权回收款金额
	 * payMoney支付金额
	 * payTime支付时间
	 * payUserName支付开户人姓名
	 * payCardNo支付银行卡号
	 * payBankName支付银行名称
	 * realIncomeTime真实回款日期
	**/
	/**
	 * 确认合同页面
	 * @param model
	 * @param id
	 * @param investMoney
	 */
	@RequestMapping("contract")
	public void contract(HttpServletRequest request, Model model, 
			String productId, String orderId, Integer investMoney, String fcode) {
		Order order = orderService.find(orderId);
		model.addAttribute("fcode", fcode);
		model.addAttribute("order", order);
		model.addAttribute("investMoney", investMoney);
		Product product = productService.find(productId);
		model.addAttribute("product", product);
		String html = product.getContractNote();
		if (StringUtil.isNotBlank(html)) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
			html = html.replaceAll("#productName#", product.getName())
				.replaceAll("#totalMoney#", (product.getSmallProduct() == 0 ? product.getTotalMoney() * 10000 : product.getTotalMoney()) + "元")
				.replaceAll("#flingMoney#", (product.getSmallProduct() == 0 ? product.getFlingMoney() * 10000 : product.getFlingMoney()) + "元")
				.replaceAll("#increaseMoney#", (product.getSmallProduct() == 0 ? product.getIncreaseMoney() * 10000 : product.getIncreaseMoney()) + "元")
				.replaceAll("#maxMoney#", (product.getSmallProduct() == 0 ? product.getMaxMoney() * 10000 : product.getMaxMoney()) + "元")
				.replaceAll("#contractTime#", product.getContractTime() == null ? "" : sdf.format(product.getContractTime()))
				.replaceAll("#expectIncome#", NULL(product.getExpectIncome()))
				.replaceAll("#allotType#", NULL(product.getAllotType()))
				.replaceAll("#startTime#", NULL(product.getStartTime()))
				.replaceAll("#dateLimit#", NULL(product.getEndTime()))
				.replaceAll("#bankName#", NULL(product.getBankName()))
				.replaceAll("#accountName#", NULL(product.getAccountName()))
				.replaceAll("#account#", NULL(product.getAccount()));
			String sIncomeStartTime = "";
			String sIncomeEndTime = "";
			String sRealIncomeTime = "";
			String sOrderNo = "";
			String sCreateTime = "";
			String sUsername = "";
			String sMobile = "";
			String sRealName = "";
			String sCertType = "";
			String sCertId = "";
			String sInvestMoney = "";
			String sInvestShare = "";
			String sIncomeMoney = "";
			String sPayMoney = "";
			String sPayTime = "";
			String sPayUserName = "";
			String sPayCardNo = "";
			String sPayBankName = "";
			if (order != null) {
				sOrderNo = order.getId();
				sCreateTime = sdf.format(order.getCreateTime());
				sUsername = order.getUser().getUsername();
				sMobile = order.getUser().getMobile();
				sRealName = order.getUser().getRealname();
				sCertType = CERT(order.getUser().getCredentialsType());
				sCertId = order.getUser().getCredentialsCode();
				sInvestMoney = order.getInvestMoney() + "元";
				sInvestShare = order.getInvestMoney() + "份";
				UserInvest invest = new UserInvest();
				invest.setOrderId(order.getId());
				invest = userInvestService.find(invest);
				if (invest != null) {
					sIncomeMoney = (invest.getInvestMoney()
							+ invest._calcAllIncomeMoney()) + "";
					int start = sIncomeMoney.indexOf(".");
					if (start > -1) {
						int end = sIncomeMoney.length();
						if ((end - start - 1) > 2) {
							sIncomeMoney = sIncomeMoney.substring(0, start + 3);
						} else {
							if ((end - start - 1) > 1) {
								sIncomeMoney += "0";
							}
						}
					} else {
						sIncomeMoney += ".00";
					}
					sIncomeMoney += "元";
				}
				if (order.getStatus() == Order.PAYED) {
					sPayMoney = order.getInvestMoney() - 
							order.getUseIntegral() - order.getCashMoney() + "元";
					sPayTime = sdf.format(order.getPayTime());
					sPayUserName = order.getUserName();
					sPayCardNo = order.getCardNo();
					sPayBankName = BANK(order.getOpenBankId());
					sIncomeStartTime = sdf.format(invest.getIncomeStartTime());
					sIncomeEndTime = sdf.format(invest.getIncomeEndTime());
					sRealIncomeTime = getRealIncomeTime(invest.getIncomeEndTime());
				} else {
					if (product.getIncomeType() == Product.INCOME_TYPE_FIXED) {
						sIncomeStartTime = sdf.format(product.getIncomeStartTime());
						sIncomeEndTime = sdf.format(product.getIncomeEndTime());
						sRealIncomeTime = getRealIncomeTime(invest.getIncomeEndTime());
					} else {
						sIncomeStartTime = "自缴款日次日起开始计息";
						sIncomeEndTime = "自缴款日次日起第" + product.getIncomeDays() + "日";
						sRealIncomeTime = "收益到期后3个工作日";
					}
				}
			} else {
				if (product.getIncomeType() == Product.INCOME_TYPE_FIXED) {
					sIncomeStartTime = sdf.format(product.getIncomeStartTime());
					sIncomeEndTime = sdf.format(product.getIncomeEndTime());
					sRealIncomeTime = getRealIncomeTime(product.getIncomeEndTime());
				} else {
					sIncomeStartTime = "自缴款日次日起开始计息";
					sIncomeEndTime = "自缴款日次日起第" + product.getIncomeDays() + "日";
					sRealIncomeTime = "收益到期后3个工作日";
				}
			}
			html = html.replaceAll("#orderNo#", sOrderNo)
				.replaceAll("#incomeStartTime#", sIncomeStartTime)
				.replaceAll("#incomeEndTime#", sIncomeEndTime)
				.replaceAll("#realIncomeTime#", sRealIncomeTime)
				.replaceAll("#createTime#", sCreateTime)
				.replaceAll("#username#", sUsername)
				.replaceAll("#mobile#", sMobile)
				.replaceAll("#realName#", sRealName)
				.replaceAll("#certType#", sCertType)
				.replaceAll("#certId#", sCertId)
				.replaceAll("#investMoney#", sInvestMoney)
				.replaceAll("#investShare#", sInvestShare)
				.replaceAll("#incomeMoney#", sIncomeMoney)
				.replaceAll("#payMoney#", sPayMoney)
				.replaceAll("#payTime#", sPayTime)
				.replaceAll("#payUserName#", sPayUserName)
				.replaceAll("#payCardNo#", sPayCardNo)
				.replaceAll("#payBankName#", sPayBankName);
		} else {
			html = "";
		}
		model.addAttribute("html", html);
	}
	
	private String NULL(String string) {
		if (string == null) {
			return "";
		}
		return string;
	}
	
	private String CERT(String string) {
		if (string == null) {
			return "";
		}
		if ("01".equals(string)) {
			return "身份证";
		} else if ("02".equals(string)) {
			return "军官证";
		} else if ("03".equals(string)) {
			return "护照";
		} else if ("04".equals(string)) {
			return "户口簿";
		} else if ("05".equals(string)) {
			return "回乡证";
		} else if ("06".equals(string)) {
			return "其他";
		} else if ("90".equals(string)) {
			return "港澳通行证";
		} else {
			return string;
		}
	}
	
	private String BANK(String string) {
		if (string == null) {
			return "";
		}
		if ("0100".equals(string)) {
			return "邮储银行";
		} else if ("0102".equals(string)) {
			return "中国工商银行";
		} else if ("0103".equals(string)) {
			return "中国农业银行";
		} else if ("0104".equals(string)) {
			return "中国银行";
		} else if ("0105".equals(string)) {
			return "中国建设银行";
		} else if ("0301".equals(string)) {
			return "交通银行";
		} else if ("0302".equals(string)) {
			return "中信银行";
		} else if ("0303".equals(string)) {
			return "中国光大银行";
		} else if ("0305".equals(string)) {
			return "中国民生银行";
		} else if ("0306".equals(string)) {
			return "广东发展银行";
		} else if ("0307".equals(string)) {
			return "深发展银行";
		} else if ("0308".equals(string)) {
			return "招商银行";
		} else if ("0309".equals(string)) {
			return "兴业银行";
		} else if ("0410".equals(string)) {
			return "中国平安银行";
		} else if ("9000".equals(string)) {
			return "浦发银行";
		} else if ("9001".equals(string)) {
			return "北京银行";
		} else if ("9002".equals(string)) {
			return "杭州银行";
		} else if ("9003".equals(string)) {
			return "华夏银行";
		} else if ("9004".equals(string)) {
			return "上海银行";
		} else if ("9005".equals(string)) {
			return "城市商业银行";
		} else {
			return string;
		}
	}
	
	private static String getRealIncomeTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		Integer weekday = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		if (weekday == 7 || weekday == 1 || weekday == 2) {
			calendar.add(Calendar.DAY_OF_YEAR, 3);
		}
		if (weekday == 3 || weekday == 4 || weekday == 5) {
			calendar.add(Calendar.DAY_OF_YEAR, 5);
		}
		if (weekday == 6) {
			calendar.add(Calendar.DAY_OF_YEAR, 4);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
		return sdf.format(calendar.getTime());
	}
	
	@RequestMapping("special")
	public void special(String ftype, String orderId, Model model) {
		model.addAttribute("ftype", ftype);
		model.addAttribute("order", orderService.find(orderId));
	}

	/**
	 * 定购成功
	 * @param model
	 * @param productId
	 * @param money
	 * @throws Exception 
	 */
	@RequestMapping("buy")
	public void buy(HttpServletRequest request, HttpServletResponse response, 
			String productId, Integer money, String fcode) throws Exception {
		ResponseForAjax resp = new ResponseForAjax();
		Product product = productService.find(productId);
		if (product != null) {
			Integer surplusMoney = product.getSurplusMoney();
			int investmentMoney = 0;
			String uid = UseridTracker.get();
			Order object = new Order();
			object.setProductId(productId);
			object.setUserId(uid);
			object.addCondition("status not in(2)", Condition.SEP_AND);	// 排除订单状态为已关闭
			List<Order> list = orderService.list(object);
			for (int i = 0; list != null && i < list.size(); i++) {
				Integer investmoney = list.get(i).getInvestMoney();
				if (product.getSmallProduct() == 0) {
					investmoney = investmoney / 10000;
				}
				investmentMoney += investmoney;
			}
			Date now = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			now = sdf.parse(sdf.format(now));
			if (now.getTime() >= product.getSubscribeStartTime().getTime() 
					&& now.getTime() <= product.getSubscribeEndTime().getTime()) {
				if (money > surplusMoney) {
					// 用户当前已经投资金额+本次投资金额如果大于产品每人最大投资金额
					// 用户本次投资金额大于剩余可投金额,则提示投资失败
					resp.setResult(-3);
					resp.setMessage("剩余可投资金额不足");
				} else {
					// 正常处理逻辑
					if ((investmentMoney + money) > product.getMaxMoney()) {
						resp.setResult(-3);
						resp.setMessage("已超出个人可投资金额");
					} else {
						Order order = new Order();
						String serial = StringUtil.genMsecSerial();
						order.setId(serial.substring(2) + new Random().nextInt(10));
						order.setProductId(productId);
						if (product.getSmallProduct() == 0) {
							money *= 10000;
						}
						order.setInvestMoney(money);
						User user = userService.find(uid);
						if (user != null) {
							// 保存用户信息
							order.setUserId(user.getId());
							order.setUserUsername(user.getUsername());
							order.setUserLinkman(user.getRealname());
							order.setUserContact(user.getMobile());
						}
						order.setStatus(-1);	// 等待支付
						order.setOnlinePay(product.getPayType());//是否仅线上支付
						order.setProductType(Order.PRODUCT_TYPE_COMMON);
						order.setUseIntegral(0);	//初始化使用积分
						order.setCashMoney(0);	//初始化使用代金券金额
						request.getSession().setAttribute("order", order);
						request.getSession().setAttribute("fcode", fcode);
						//orderService.wapOrder(order, fcode);
						
						resp.setData(order.getId());
						resp.setResult(1);
					}
				}
			} else {
				if (now.getTime() <= product.getSubscribeStartTime().getTime()) {
					resp.setResult(-2);
					resp.setMessage("未到认购时间");
				} else {
					resp.setResult(-2);
					resp.setMessage("已过认购时间");
				}
			}
		}
		resp.jsonOut(response);
	}
	
	/**
	 * 特权本金
	 * @param model
	 * @param productId
	 * @param money
	 * @throws Exception 
	 */
	@RequestMapping("specialBuy")
	public void specialBuy(HttpServletRequest request, HttpServletResponse response, 
			String productId, Integer money) throws Exception {
		ResponseForAjax resp = new ResponseForAjax();
		Product product = productService.find(productId);
		if (product != null) {
			String uid = UseridTracker.get();
			Order object = new Order();
			object.setProductId(productId);
			object.setUserId(uid);
			object.addCondition("status not in(2)", Condition.SEP_AND);	// 排除订单状态为已关闭
			List<Order> list = orderService.list(object);
			if(product.getType() == Product.TYPE_SPECIAL&&list!=null&&list.size()>0){
				resp.setResult(-3);
				resp.setMessage("您已认购该产品");
			}else{
				Date now = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				now = sdf.parse(sdf.format(now));
				if (now.getTime() >= product.getSubscribeStartTime().getTime() 
						&& now.getTime() <= product.getSubscribeEndTime().getTime()) {
							Order order = new Order();
							String serial = StringUtil.genMsecSerial();
							order.setId(serial.substring(2) + new Random().nextInt(10));
							order.setProductId(productId);
							order.setInvestMoney(money);
							User user = userService.find(uid);
							if (user != null) {
								// 保存用户信息
								order.setUserId(user.getId());
								order.setUserUsername(user.getUsername());
								order.setUserLinkman(user.getRealname());
								order.setUserContact(user.getMobile());
							}
							order.setStatus(-1);	// 等待支付
							order.setOnlinePay(product.getPayType());//是否仅线上支付
							order.setProductType(Order.PRODUCT_TYPE_SPECIAL);
							order.setUseIntegral(0);	//初始化使用积分
							order.setCashMoney(0);	//初始化使用代金券金额
							request.getSession().setAttribute("order", order);
							resp.setData(order.getId());
							resp.setResult(1);
				} else {
					if (now.getTime() <= product.getSubscribeStartTime().getTime()) {
						resp.setResult(-2);
						resp.setMessage("未到认购时间");
					} else {
						resp.setResult(-2);
						resp.setMessage("已过认购时间");
					}
				}
			}
		}
		resp.jsonOut(response);
	}
	
	/**
	 * 特殊基金确认订单页面
	 * @param request
	 * @param productId
	 * @param money
	 * @param orderId
	 * @param model
	 * @return
	 */
	@RequestMapping("specialPay")
	public void specialPay(HttpServletRequest request, 
			String productId, Integer money, String orderId, Model model) {
		model.addAttribute("productId", productId);
		Product product = productService.find(productId);
		model.addAttribute("product", product);
		model.addAttribute("orderId", orderId);
		model.addAttribute("money", money);
		//查询用户可用积分数
		String uid = UseridTracker.get();
		model.addAttribute("integral", 
			userIntegralService.getMyVaildIntegral(uid, null, product.getUseIntegralType()));
	}
	
	/**
	 * 转跳到特权本金定购成功页面
	 * @param request
	 * @param model
	 * @param productId
	 * @param investMoney
	 * @throws ParseException 
	 */
	@RequestMapping("specialSuccess")
	public String specialSuccess(HttpServletRequest request, Integer useIntegral, 
			String productId, Integer money, String orderId, Integer type, 
			 Model model) throws ParseException {
		synchronized (this.getClass()) {
			try {
				Product product = productService.find(productId);
				if (product != null) {
					String uid = UseridTracker.get();
					Order object = new Order();
					object.setProductId(productId);
					object.setUserId(uid);
					object.addCondition("status not in(2)", Condition.SEP_AND);	// 排除订单状态为已关闭
					List<Order> list = orderService.list(object);
					if(product.getType() == Product.TYPE_SPECIAL&&list!=null&&list.size()>0){
						throw new MessageException("你已认购该产品");
					}
					Date now = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					now = sdf.parse(sdf.format(now));
					if (now.getTime() >= product.getSubscribeStartTime().getTime() 
							&& now.getTime() <= product.getSubscribeEndTime().getTime()) {
					} else {
						if (now.getTime() <= product.getSubscribeStartTime().getTime()) {
							throw new MessageException("未到认购时间");
						} else {
							throw new MessageException("已过认购时间");
						}
					}
				}
				Order order = (Order) request.getSession().getAttribute("order");
				if (order != null) {
					if(type==0){
						order.setActualMoney(product.getActualPayMoney());
					}else{
						Integer myIntegral=userIntegralService.getMyVaildIntegral(order.getUserId(), null);
						if(myIntegral.intValue()<useIntegral.intValue()){
							throw new MessageException("金币不足");
						}
						order.setActualMoney(0);
					}
					order.setType(type);
					orderService.specialOrder(order, useIntegral);
					request.getSession().removeAttribute("order");
				}
				if(type==1){//金币
					//使用金币支付是不是应该跳转支付成功页面
					Order object=orderService.find(order.getId());
					model.addAttribute("object", object);
					return "wap/product/succ";
				}
				model.addAttribute("product", productService.find(productId));
				model.addAttribute("order", orderService.find(orderId));
				model.addAttribute("money", money);
				model.addAttribute("endTime", DateUtil.addDay(new Date(), 1));
				String lctx = HttpUtil.getWebAppUrl(request);
				model.addAttribute("lctx", lctx);
				model.addAttribute("pid", UseridTracker.get());
				return "wap/product/success";
			} catch (Exception e) {
				e.printStackTrace();
				model.addAttribute("productId", productId);
				model.addAttribute("error", e.getMessage());
				return "wap/product/error";
			}
		}
	}
	
	/**
	 * 金币抵扣页面
	 * @param request
	 * @param productId
	 * @param money
	 * @param orderId
	 * @param model
	 * @return
	 */
	@RequestMapping("contpay")
	public String contpay(HttpServletRequest request, 
			String productId, Integer money, String orderId, Model model) {
		model.addAttribute("productId", productId);
		Product product = productService.find(productId);
		model.addAttribute("product", product);
		Integer integrallimit = product.getIntegralLimit();
		if (integrallimit == null) { integrallimit = 100; }
		model.addAttribute("integrallimit", integrallimit);
		if (product.getSmallProduct() == 0) {
			integrallimit = money * integrallimit;
		}
		model.addAttribute("maxintegral", integrallimit);
		model.addAttribute("orderId", orderId);
		model.addAttribute("money", money);
		//查询用户可用积分数
		String uid = UseridTracker.get();
		model.addAttribute("integral", 
			userIntegralService.getMyVaildIntegral(uid, null, product.getUseIntegralType()));
		//查询用户可用的代金券列表
		CashCoupon cashCoupon = new CashCoupon();
		cashCoupon.setUserId(uid);
		cashCoupon.setStatus(CashCoupon.STATUS_GET_YES);
		if (product.getSmallProduct() == 0) {
			// 和原来逻辑一样
			cashCoupon.addCondition("use_condition <= " + money, Condition.SEP_AND);
		} else {
			// 小产品,万元为单位的都是元为单位
			cashCoupon.addCondition("use_condition <= " + money / 10000.0, Condition.SEP_AND);
		}
		cashCoupon.addCondition("vaild_end_time >= now()", Condition.SEP_AND);
		model.addAttribute("cashCouponList", cashCouponService.list(cashCoupon));
		return "wap/product/contpay";
	}
	
	/**
	 * 转跳到定购成功页面
	 * @param request
	 * @param model
	 * @param productId
	 * @param investMoney
	 * @throws ParseException 
	 */
	@RequestMapping("success")
	public String success(HttpServletRequest request, Integer useIntegral, 
			String productId, Integer money, String orderId, Integer type, 
			String cashId, Integer cashMoney, Model model) throws ParseException {
		synchronized (this.getClass()) {
			try {
				Product product = productService.find(productId);
				if (product != null) {
					Integer surplusMoney = product.getSurplusMoney();
					int investmentMoney = 0;
					String uid = UseridTracker.get();
					Order object = new Order();
					object.setProductId(productId);
					object.setUserId(uid);
					object.addCondition("status not in(2)", Condition.SEP_AND);	// 排除订单状态为已关闭
					List<Order> list = orderService.list(object);
					for (int i = 0; list != null && i < list.size(); i++) {
						Integer investmoney = list.get(i).getInvestMoney();
						if (product.getSmallProduct() == 0) {
							investmoney = investmoney / 10000;
						}
						investmentMoney += investmoney;
					}
					Date now = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					now = sdf.parse(sdf.format(now));
					if (now.getTime() >= product.getSubscribeStartTime().getTime() 
							&& now.getTime() <= product.getSubscribeEndTime().getTime()) {
						if (money > surplusMoney) {
							// 用户当前已经投资金额+本次投资金额如果大于产品每人最大投资金额
							// 用户本次投资金额大于剩余可投金额,则提示投资失败
							throw new MessageException("剩余可投资金额不足");
						} else {
							// 正常处理逻辑
							if ((investmentMoney + money) > product.getMaxMoney()) {
								throw new MessageException("已超出个人可投资金额");
							}
						}
					} else {
						if (now.getTime() <= product.getSubscribeStartTime().getTime()) {
							throw new MessageException("未到认购时间");
						} else {
							throw new MessageException("已过认购时间");
						}
					}
				}
				Order order = (Order) request.getSession().getAttribute("order");
				String fcode = (String) request.getSession().getAttribute("fcode");
				if (order != null) {
					order.setType(type);
					orderService.wapOrder(order, fcode, useIntegral, cashId, cashMoney);
					request.getSession().removeAttribute("order");
					request.getSession().removeAttribute("fcode");
				}
				model.addAttribute("product", productService.find(productId));
				Order orDer = orderService.find(orderId);
				
				model.addAttribute("order", orDer);
				Date date = orDer.getCreateTime();
				if (date != null) {
					long millis = new Date().getTime() - date.getTime();
					millis = 40 * 60 * 1000 - millis;
					long min = 0;
					long sec = 0;
					if (millis > 0) {
						min = millis / 1000 / 60;
						sec = millis / 1000 % 60;
					}
					model.addAttribute("min", min);
					model.addAttribute("sec", sec);
					System.out.println("=======================" + min);
					System.out.println("=======================" + sec);
				}
				model.addAttribute("money", money);
				model.addAttribute("endTime", DateUtil.addDay(new Date(), 1));
				String lctx = HttpUtil.getWebAppUrl(request);
				model.addAttribute("lctx", lctx);
				model.addAttribute("pid", UseridTracker.get());
				return "wap/product/success";
			} catch (Exception e) {
				e.printStackTrace();
				model.addAttribute("productId", productId);
				model.addAttribute("error", e.getMessage());
				return "wap/product/error";
			}
		}
	}
	
	/**
	 * 转跳到定购成功页面
	 * @param request
	 * @param model
	 * @param productId
	 * @param investMoney
	 */
	@RequestMapping("pay")
	public String pay(HttpServletRequest request, 
			String productId, Integer money, String orderId, Model model) {
		model.addAttribute("product", productService.find(productId));
		model.addAttribute("order", orderService.find(orderId));
		model.addAttribute("money", money);
		model.addAttribute("endTime", DateUtil.addDay(new Date(), 1));
		String lctx = HttpUtil.getWebAppUrl(request);
		model.addAttribute("lctx", lctx);
		model.addAttribute("user", userService.find(UseridTracker.get()));
		return "wap/product/pay";
	}
	
}
