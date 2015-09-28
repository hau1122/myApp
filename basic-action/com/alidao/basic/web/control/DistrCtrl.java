package com.alidao.basic.web.control;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.dao4mybatis.HolidayDao;
import com.alidao.basic.entity.Holiday;
import com.alidao.basic.entity.Order;
import com.alidao.basic.entity.Product;
import com.alidao.basic.entity.ProductType;
import com.alidao.basic.service.ProductService;
import com.alidao.basic.service.ProductTypeService;
import com.alidao.common.Constants;
import com.alidao.jse.util.DateUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.Page;
import com.alidao.jxe.model.PageParam;
import com.alidao.users.dao4mybatis.UserBindDao;
import com.alidao.users.dao4mybatis.UserInvestDao;
import com.alidao.users.entity.UserBind;
import com.alidao.users.entity.UserInvest;
import com.alidao.users.service.UserInvestService;
import com.alidao.utils.XlsUtil;
import com.alidao.wxapi.bean.TokenForWxapis;
import com.alidao.wxapi.util.WxapiUtil;

/**
 * 分配管理
 * @author huangyl
 *
 */
@Controller
@RequestMapping("distr")
public class DistrCtrl extends WebCtrl{
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private UserInvestDao userInvestDao;
	
	@Autowired
	private UserInvestService userInvestService;
	
	@Autowired
	private ProductTypeService productTypeService;
	
	@Autowired
	private UserBindDao userBindDao;
	

	@Autowired
	private HolidayDao holidayDao;
	
	public Date convert(Date endTime) {//2016-01-12
		for (int i = 0; i < 3; i++) {
			endTime = DateUtil.addOneDay(endTime);
			if(isRestDay(endTime)) {
				i--;//如果是休息日则跳过这一天，时间往后加
			}
		}
		return endTime;
	}
	
	public boolean isRestDay(Date date) {
		boolean flag = false;
		//先判断该日期是否是周六、周天
		int week = getWeekOfDate(date);
		if(week==0||week==6) {//周六、周天
			flag = true;
			return flag;
		}
		Holiday holiday = new Holiday();
		holiday.setYear(Integer.parseInt(DateUtil.formatDate(date, "yyyy-MM-dd").substring(0,4)));
		List<Holiday> holidays = holidayDao.queryForList(holiday);
		if(holidays != null && holidays.size() > 0) {
			for (Holiday holiday2 : holidays) {
				if(date.getTime() >= holiday2.getStartTime().getTime() && 
						date.getTime() <= holiday2.getEndTime().getTime()) {
					flag = true;
					break;
				}
			}
		}
		return flag;
	}
	
   public static int getWeekOfDate(Date dt) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return w;
    }
   
 
   
	@RequestMapping("init")
	public String init(Model model,String productId, String userId,Integer productType) {
		model.addAttribute("productTypes",productTypeService.list(new ProductType()));
		List<UserInvest> list = userInvestDao.queryForList(new UserInvest());
		if(list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				if(list.get(i).getRepayTime()==null &&
						list.get(i).getIncomeEndTime() != null) {
					UserInvest userInvest = new UserInvest();
					userInvest.setId(list.get(i).getId());
					userInvest.setRepayTime(convert(list.get(i).getIncomeEndTime()));
					userInvestDao.update(userInvest);
				}
			}
		}
//		String s = "2015-09-17";
//		System.out.println(DateUtil.formatDate(convert(DateUtil.formatStrToDate(s,"yyyy-MM-dd")),"yyyy-MM-dd"));
		return "order/distrInit";
	}

	@RequestMapping("list")
	public void list(PageParam pageParam, UserInvest object,Date startTime,Date endTime,
			HttpServletResponse response) throws IOException {
		object.setStatus(UserInvest.STATUS_HOLD_ING);
		if(object.getProductId()=="") {
			object.setProductId(null);
		}
		if(startTime != null && endTime != null) {
			object.addCondition("and repay_time between '"+startTime+"' and '"+endTime+"' ");
		}
		Page<UserInvest> page = userInvestService.page(pageParam, object);
		List<UserInvest> list = page.getTableList();
		for (int i = 0; list != null && i < list.size(); i++) {
			list.get(i).setProductType(
					productTypeService.find(productService.find
							(list.get(i).getProductId()).getTypeId()).getName());//产品分类名称
			list.get(i).getOrder().setOpenBankId(getBankName(list.get(i).getOrder().getOpenBankId()));
		}
		page.jsonOut(response);
	}
	
	//计算支付金额
	private Integer calcuActualMoney(Order order) {
		if(order.getProductType()==1) {
			return order.getInvestMoney() 
			- order.getUseIntegral()
			- order.getCashMoney();
		}else {
			if (order.getActualMoney() <= 0) {
				return 0;
			}
		}
		return null;
	}

	@RequestMapping("distrInput")
	public String edit(Model model, Long id,Integer type) {
		if (id != null) {
			UserInvest object = userInvestService.find(id);
			object.getOrder().setOpenBankId(getBankName(object.getOrder().getOpenBankId()));//银行名称
			object.setProductType(
					productTypeService.find(productService.find
							(object.getProductId()).getTypeId()).getName());//产品分类
			object.getOrder().setActualMoney(calcuActualMoney(object.getOrder()));//支付金额
			model.addAttribute("object", object);
		}
		model.addAttribute("type", type);
		return "order/distrInput";
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
	public void export(HttpServletRequest request, PageParam pageParam,
			UserInvest object,Date startTime,Date endTime, HttpServletResponse response) throws Exception{
		OutputStream os = response.getOutputStream();
		object.setStatus(UserInvest.STATUS_HOLD_ING);
		if(object.getProductId()=="") {
			object.setProductId(null);
		}
		if(startTime != null && endTime != null) {
			object.addCondition("and repay_time between '"+startTime+"' and '"+endTime+"' ");
		}
		pageParam.setPageSize(60000L);//统计的最大数量
		Page<UserInvest> page = userInvestService.page(pageParam, object);
		List<UserInvest> list = page.getTableList();
		Long totalMoney = 0L;
		String[] statOfLine = new String[1];// 统计结果行
		statOfLine[0] = "订单总计：" + page.getPageParam().getTotalCount() + "个 订单总支付金额：" + totalMoney + "元 ";
		String[] srr = null;
		String[] heads = new String[14];
		List res = new ArrayList();
		int count = 0;
		for (int i = 0; list != null && i < list.size(); i++) {
			list.get(i).setProductType(
					productTypeService.find(productService.find
							(list.get(i).getProductId()).getTypeId()).getName());//产品分类名称
			list.get(i).getOrder().setOpenBankId(getBankName(list.get(i).getOrder().getOpenBankId()));
			srr = new String[14];// 显示14个订单的字段
			setData(srr, list.get(i));// 数据赋值
			if (count == 0) {
				// 第一行中文备注只需赋值一次即可
				setHead(heads);
			}
			res.add(srr);
			count ++;
		}
		//res.add(statOfLine);// 加入统计结果
		XlsUtil.setHead("导出数据.xls", response);
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
	
	public void setData(String[] srr, UserInvest userInvest) {
		srr[0] = userInvest.getProductType();// 产品类型
		srr[1] = userInvest.getProductName();// 产品名称
		srr[2] = userInvest.getOrder().getUserLinkman();// 姓名
		srr[3] = userInvest.getOrder().getOpenBankId();//开户行
		srr[4] = userInvest.getOrder().getCardNo();//银行卡号
		srr[5] = userInvest.getInvestMoney()+ "元";// 投资金额
		userInvest.getOrder().setActualMoney(calcuActualMoney(userInvest.getOrder()));//计算支付金额
		srr[6] = userInvest.getOrder().getActualMoney() + "元";//支付金额
		srr[7] = userInvest.getIncomeMoney() + "元";// 收益金额
		srr[8] = userInvest.getOrder().getActualMoney() + userInvest.getIncomeMoney() + "元";// 还款金额
		srr[9] = userInvest.getOrder().getUseIntegral()+"";
		srr[10] = userInvest.getOrder().getCashMoney()+"元"; 
		srr[11] =  DateUtil.formatDate(userInvest.getIncomeStartTime(), "yyyy-MM-dd");
		srr[12] =  DateUtil.formatDate(userInvest.getIncomeEndTime(), "yyyy-MM-dd");
		if(userInvest.getRepayTime() != null) {
			srr[13] = DateUtil.formatDate(userInvest.getRepayTime(), "yyyy-MM-dd");// 还款日
		} else {
			srr[13] = "";
		}
	}
	
	public void setHead(String[] srr) {
		srr[0] = "产品类型";
		srr[1] = "产品名称";
		srr[2] = "姓名";
		srr[3] = "开户行";
		srr[4] = "银行卡号";
		srr[5] = "投资金额";
		srr[6] = "支付金额";
		srr[7] = "收益金额";
		srr[8] = "还款金额";
		srr[9] = "金币";
		srr[10] = "代金券";
		srr[11] = "起息日";
		srr[12] = "到息日";
		srr[13] = "返款日";
	}
	
	
	@RequestMapping("getProduct")
	public void getProduct(Product object,
			HttpServletResponse response) throws Exception {
		getQueryResponse(productService.list(object)).jsonOut(response);
	}
	
	
	@RequestMapping("send")
	public void send(Long id,
			HttpServletResponse response) throws Exception {
		UserInvest userInvest = userInvestService.find(id);
		UserBind userBind = new UserBind();
		userBind.setUserId(userInvest.getUserId());
		userBind = userBindDao.select(userBind);
		Product product = productService.find(userInvest.getProductId());
		String json_data = "";
		TokenForWxapis tokenForWxapis = WxapiUtil.getWxapisToken(Constants.get("wxapi.appid"), Constants.get("wxapi.appsecret"));
		String incomeendTemplateId = Constants.get("incomeend.templateId");
		if (product != null && userInvest != null) {
			//组装json_data数据
			json_data = "{\"first\": {\"value\":\"你认购的‘" + product.getName() + "’产品收益已到账\",\"color\":\"#173177\"}," +
						"\"income_amount\":{\"value\":\"" + userInvest.getIncomeMoney() + "\",\"color\":\"#173177\"}," +
						"\"income_time\": {\"value\":\"" + DateUtil.formatDate(new Date(), "yyyy年MM月dd日") + "\",\"color\":\"#173177\"}," +
						"\"remark\": {\"value\":\"你好，你的" + product.getName() + "产品收益已到账，请注意查看确认。客服热线400-6196-805。\",\"color\":\"#173177\"}}";
			//推送收益到账消息模版
			WxapiUtil.sendTM2WxUser(tokenForWxapis.getAccess_token(),userBind.getAccount(), incomeendTemplateId, "", json_data);
		}
		getResponse(1).jsonOut(response);
	}
	
	
	@RequestMapping("sendAll")
	public void sendAll(HttpServletRequest request, PageParam pageParam,
			UserInvest object,Date startTime,Date endTime, HttpServletResponse response) throws Exception {
		if(object.getProductId()=="") {
			object.setProductId(null);
		}
		if(startTime != null && endTime != null) {
			object.addCondition("and repay_time between '"+startTime+"' and '"+endTime+"' ");
		}
		TokenForWxapis tokenForWxapis = WxapiUtil.getWxapisToken(Constants.get("wxapi.appid"), Constants.get("wxapi.appsecret"));
		String incomeendTemplateId = Constants.get("incomeend.templateId");
		pageParam.setPageSize(60000L);// 统计的最大数量
		Page<UserInvest> page = userInvestService.page(pageParam, object);
		List<UserInvest> list = page.getTableList();
		String json_data = "";
		if(list != null && list.size() > 0) {
			for (UserInvest userInvest : list) {
				UserBind userBind = new UserBind();
				userBind.setUserId(userInvest.getUserId());
				userBind = userBindDao.select(userBind);
				Product product = productService.find(userInvest.getProductId());
				if (product != null && userInvest != null) {
					//组装json_data数据
					json_data = "{\"first\": {\"value\":\"你认购的‘" + product.getName() + "’产品收益已到账\",\"color\":\"#173177\"}," +
								"\"income_amount\":{\"value\":\"" + userInvest.getIncomeMoney() + "\",\"color\":\"#173177\"}," +
								"\"income_time\": {\"value\":\"" + DateUtil.formatDate(new Date(), "yyyy年MM月dd日") + "\",\"color\":\"#173177\"}," +
								"\"remark\": {\"value\":\"你好，你的" + product.getName() + "产品收益已到账，请注意查看确认。客服热线400-6196-805。\",\"color\":\"#173177\"}}";
					//推送收益到账消息模版
					WxapiUtil.sendTM2WxUser(tokenForWxapis.getAccess_token(),userBind.getAccount(), incomeendTemplateId, "", json_data);
				}
			}
		}
		getResponse(1).jsonOut(response);
	}
	
	
	public String getBankName(String code) {
		String s = "";
		if ("0308".equals(code)) {
			s = "招商银行";
		} else if ("0105".equals(code)) {
			s = "中国建设银行";
		} else if ("0302".equals(code)) {
			s = "中信银行";
		} else if ("0303".equals(code)) {
			s = "中国光大银行";
		} else if ("0306".equals(code)) {
			s = "广东发展银行";
		} else if ("0305".equals(code)) {
			s = "中国民生银行";
		} else if ("0410".equals(code)) {
			s = "中国平安银行";
		} else if ("0100".equals(code)) {
			s = "邮储银行";
		} else if ("0102".equals(code)) {
			s = "中国工商银行";
		} else if ("0103".equals(code)) {
			s = "中国农业银行";
		} else if ("0104".equals(code)) {
			s = "中国银行";
		} else if ("0301".equals(code)) {
			s = "交通银行";
		} else if ("0307".equals(code)) {
			s = "深发展银行";
		} else if ("0309".equals(code)) {
			s = "兴业银行";
		}
		return s;
	}

}
