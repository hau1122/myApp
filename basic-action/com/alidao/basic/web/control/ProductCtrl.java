package com.alidao.basic.web.control;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Product;
import com.alidao.basic.entity.ProductProject;
import com.alidao.basic.entity.ProductType;
import com.alidao.basic.service.IntegralTypeService;
import com.alidao.basic.service.ProductProjectService;
import com.alidao.basic.service.ProductService;
import com.alidao.basic.service.ProductTypeService;
import com.alidao.common.Constants;
import com.alidao.jse.util.DateUtil;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.Page;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.model.ParamException;
import com.alidao.users.dao4mybatis.UserBindDao;
import com.alidao.users.entity.UserBind;
import com.alidao.users.entity.UserInvest;
import com.alidao.users.service.UserInvestService;
import com.alidao.wxapi.bean.TokenForWxapis;
import com.alidao.wxapi.util.WxapiUtil;

@Controller
@RequestMapping("product")
public class ProductCtrl extends WebCtrl {

	@Autowired
	private ProductService productService;
	
	@Autowired
	private ProductTypeService productTypeService;
	
	@Autowired
	private ProductProjectService productProjectService;
	
	@Autowired
	private UserBindDao userBindDao;
	
	@Autowired
	private UserInvestService userInvestService;

	@Autowired
	private IntegralTypeService integralTypeService;
	
	@RequestMapping("init")
	public void init(Model model, String typeId,Integer type) {
		model.addAttribute("typeId", typeId);
		model.addAttribute("type", type);
	}
/**
 * 特殊基金列表页面
 * @param model
 * @param typeId
 */
	@RequestMapping("specialInit")
	public void specialInit(Model model,Integer type) {
		model.addAttribute("type", type);
	}
	
	@RequestMapping("specialEdit")
	public void specialEdit(Model model, String id) {
		if (StringUtil.isNotBlank(id)) {
			model.addAttribute("object", productService.find(id));
		}
		List<ProductType> list = productTypeService.list(null);
		model.addAttribute("productTypeList", list);
	}
	
	@RequestMapping("list")
	public void list(PageParam pageParam, Product object,
			HttpServletResponse response) throws IOException {
		productService.page(pageParam, object).jsonOut(response);
	}

	@RequestMapping("edit")
	public void edit(Model model, String id) {
		if (StringUtil.isNotBlank(id)) {
			model.addAttribute("object", productService.find(id));
		}
		model.addAttribute("integralTypes", integralTypeService.list(null));
		List<ProductType> list = productTypeService.list(null);
		model.addAttribute("productTypeList", list);
	}
	
	@RequestMapping("save")
	public void save(Product object, String[] zdyname, String[] zdynote, HttpServletResponse response)
			throws IOException, ParamException {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(productService.save(object, zdyname, zdynote)).jsonOut(response);
		} else {
			getResponse(productService.mdfy(object, zdyname, zdynote)).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		getResponse(productService.lose(id)).jsonOut(response);
	}
	
	
	/**
	 * 列举产品下的自定义项目数据
	 * @param productId
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("listProject")
	public void listProject(String productId, HttpServletResponse response) throws IOException{
		ProductProject productProject = new ProductProject();
		productProject.setProductId(productId);
		getQueryResponse(productProjectService.list(productProject)).jsonOut(response);
	}
	
	
	/**
	 * 产品推送模版消息
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping("send/{type}")
	public void send(@PathVariable("type") Integer type, String id, HttpServletResponse response) throws Exception {
		if (type == 1) {
			//产品上线推送
			String json_data = "";
			Product product = productService.find(id);
			if (product != null) {
				//组装json_data数据
				json_data = "{\"first\": {\"value\":\"高和畅上线新产品！\",\"color\":\"#173177\"}," +
							"\"keyword1\":{\"value\":\"" + product.getName() + "\",\"color\":\"#173177\"}," +
							"\"keyword2\": {\"value\":\"" + product.getEndTime() + "\",\"color\":\"#173177\"}," +
							"\"keyword3\": {\"value\":\"" + product.getIncome() + "%\",\"color\":\"#173177\"}," +
							"\"keyword4\": {\"value\":\"1万元起投\",\"color\":\"#173177\"}," +
							"\"remark\": {\"value\":\"敬请关注！\",\"color\":\"#173177\"}}";
			}
			String productTemplateId = Constants.get("product.templateId");
			TokenForWxapis tokenForWxapis = WxapiUtil.getWxapisToken(Constants.get("wxapi.appid"), Constants.get("wxapi.appsecret"));
			Long pageNo = 0L, pageSize = 20L;
			PageParam pageParam = new PageParam();
			pageParam.setPageNo(pageNo);
			pageParam.setPageSize(pageSize);
			boolean hasnext = true;
			Page<UserBind> page = null;
			do {
				pageParam.setPageNo(++pageNo);
				page = userBindDao.queryForPage(pageParam, new UserBind());
				if (page.getResult() == Page.SUCC) {
					List<UserBind> list = page.getTableList();
					for (int i = 0; list != null && i < list.size(); i++) {
						//推送产品上线消息模版
						WxapiUtil.sendTM2WxUser(tokenForWxapis.getAccess_token(), list.get(i).getAccount(), productTemplateId, "", json_data);
					}
				}
				hasnext = page.getPageParam().getHasNext();
			} while (hasnext);
			getResponse(1).jsonOut(response);
		} else {
			//收益到账推送
			String json_data = "";
			Product product = productService.find(id);
			String incomeendTemplateId = Constants.get("incomeend.templateId");
			TokenForWxapis tokenForWxapis = WxapiUtil.getWxapisToken(Constants.get("wxapi.appid"), Constants.get("wxapi.appsecret"));
			Long pageNo = 0L, pageSize = 20L;
			PageParam pageParam = new PageParam();
			pageParam.setPageNo(pageNo);
			pageParam.setPageSize(pageSize);
			boolean hasnext = true;
			Page<UserBind> page = null;
			do {
				pageParam.setPageNo(++pageNo);
				page = userBindDao.queryForPage(pageParam, new UserBind());
				if (page.getResult() == Page.SUCC) {
					List<UserBind> list = page.getTableList();
					for (int i = 0; list != null && i < list.size(); i++) {
						UserInvest userInvest = new UserInvest();
						userInvest.setUserId(list.get(i).getUserId());
						userInvest.setProductId(id);
						userInvest.setStatus(UserInvest.STATUS_HAS_OVER);
						userInvest = userInvestService.find(userInvest);
						//可能会重复判断，老子也不管了，老子做好玩猫去了。
						if (product != null && userInvest != null) {
							//组装json_data数据
							json_data = "{\"first\": {\"value\":\"你认购的‘" + product.getName() + "’产品收益已到账\",\"color\":\"#173177\"}," +
										"\"income_amount\":{\"value\":\"" + userInvest.getIncomeMoney() + "\",\"color\":\"#173177\"}," +
										"\"income_time\": {\"value\":\"" + DateUtil.formatDate(new Date(), "yyyy年MM月dd日") + "\",\"color\":\"#173177\"}," +
										"\"remark\": {\"value\":\"你好，你的" + product.getName() + "产品收益已到账，请注意查看确认。客服热线400-6196-805。\",\"color\":\"#173177\"}}";
							//推送收益到账消息模版
							WxapiUtil.sendTM2WxUser(tokenForWxapis.getAccess_token(), list.get(i).getAccount(), incomeendTemplateId, "", json_data);
						}
					}
				}
				hasnext = page.getPageParam().getHasNext();
			} while (hasnext);
			getResponse(1).jsonOut(response);
		}
	}
	
	
	
	/**
	 * 特殊产品收益到账推送
	 * @param type
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("specialSend")
	public void specialSend(Integer type, HttpServletResponse response) throws Exception {
		if (type == 1) {
			// 一期产品
			//收益到账推送
			String json_data = "";
			Product product = productService.find("22111208609fb74d07b8ef6a124f80c0");
			String incomeendTemplateId = Constants.get("incomeend.templateId");
			TokenForWxapis tokenForWxapis = WxapiUtil.getWxapisToken(Constants.get("wxapi.appid"), Constants.get("wxapi.appsecret"));
			Long pageNo = 0L, pageSize = 20L;
			PageParam pageParam = new PageParam();
			pageParam.setPageNo(pageNo);
			pageParam.setPageSize(pageSize);
			boolean hasnext = true;
			Page<UserBind> page = null;
			do {
				pageParam.setPageNo(++pageNo);
				page = userBindDao.queryForPage(pageParam, new UserBind());
				if (page.getResult() == Page.SUCC) {
					List<UserBind> list = page.getTableList();
					for (int i = 0; list != null && i < list.size(); i++) {
						UserInvest userInvest = new UserInvest();
						userInvest.setUserId(list.get(i).getUserId());
						userInvest.setProductId("22111208609fb74d07b8ef6a124f80c0");
						userInvest.setStatus(UserInvest.STATUS_HOLD_ING);
						userInvest = userInvestService.find(userInvest);
						//可能会重复判断，老子也不管了，老子做好玩猫去了。
						if (product != null && userInvest != null) {
							//组装json_data数据
							json_data = "{\"first\": {\"value\":\"你认购的‘" + product.getName() + "’第二次收益返款已完成发放。\",\"color\":\"#173177\"}," +
										"\"income_amount\":{\"value\":\"" + userInvest.getInvestMoney() * 112.623 + "\",\"color\":\"#173177\"}," +
										"\"income_time\": {\"value\":\"" + DateUtil.formatDate(new Date(), "yyyy年MM月dd日 HH:mm") + "\",\"color\":\"#173177\"}," +
										"\"remark\": {\"value\":\"你好，你的" + product.getName() + "第二次收益返款已完成发放。，请注意查看确认。客服热线400-6196-805。\",\"color\":\"#173177\"}}";
							//推送收益到账消息模版
							WxapiUtil.sendTM2WxUser(tokenForWxapis.getAccess_token(), list.get(i).getAccount(), incomeendTemplateId, "", json_data);
						}
					}
				}
				hasnext = page.getPageParam().getHasNext();
			} while (hasnext);
		} else {
			// 二期产品
			//收益到账推送
			String json_data = "";
			Product product = productService.find("7121338b20e809d212ba6c17e6b94a56");
			String incomeendTemplateId = Constants.get("incomeend.templateId");
			TokenForWxapis tokenForWxapis = WxapiUtil.getWxapisToken(Constants.get("wxapi.appid"), Constants.get("wxapi.appsecret"));
			Long pageNo = 0L, pageSize = 20L;
			PageParam pageParam = new PageParam();
			pageParam.setPageNo(pageNo);
			pageParam.setPageSize(pageSize);
			boolean hasnext = true;
			Page<UserBind> page = null;
			do {
				pageParam.setPageNo(++pageNo);
				page = userBindDao.queryForPage(pageParam, new UserBind());
				if (page.getResult() == Page.SUCC) {
					List<UserBind> list = page.getTableList();
					for (int i = 0; list != null && i < list.size(); i++) {
						UserInvest userInvest = new UserInvest();
						userInvest.setUserId(list.get(i).getUserId());
						userInvest.setProductId("7121338b20e809d212ba6c17e6b94a56");
						userInvest.setStatus(UserInvest.STATUS_HOLD_ING);
						userInvest = userInvestService.find(userInvest);
						//可能会重复判断，老子也不管了，老子做好玩猫去了。
						if (product != null && userInvest != null) {
							//组装json_data数据
							json_data = "{\"first\": {\"value\":\"你认购的‘" + product.getName() + "’第二次收益返款已完成发放。\",\"color\":\"#173177\"}," +
										"\"income_amount\":{\"value\":\"" + userInvest.getInvestMoney() * 113.043 + "\",\"color\":\"#173177\"}," +
										"\"income_time\": {\"value\":\"" + DateUtil.formatDate(new Date(), "yyyy年MM月dd日 HH:mm") + "\",\"color\":\"#173177\"}," +
										"\"remark\": {\"value\":\"你好，你的" + product.getName() + "第二次收益返款已完成发放。，请注意查看确认。客服热线400-6196-805。\",\"color\":\"#173177\"}}";
							//推送收益到账消息模版
							WxapiUtil.sendTM2WxUser(tokenForWxapis.getAccess_token(), list.get(i).getAccount(), incomeendTemplateId, "", json_data);
						}
					}
				}
				hasnext = page.getPageParam().getHasNext();
			} while (hasnext);
		}
	}
}
