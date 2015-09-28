package com.alidao.basic.web.control;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.entity.CashCoupon;
import com.alidao.basic.service.CashCouponService;
import com.alidao.jse.util.DateUtil;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.Condition;
import com.alidao.jxe.model.PageParam;
import com.alidao.users.entity.User;
import com.alidao.users.service.UserService;

@Controller
@RequestMapping("cashCoupon")
public class CashCouponCtrl extends WebCtrl {

	@Autowired
	private CashCouponService cashCouponService;
	
	@Autowired
	private UserService userService;

	@RequestMapping("init")
	public String init(Model model, 
			String userId) {
		if(StringUtil.isEmpty(userId)){
			return "cashCoupon/list";
		}
		model.addAttribute("user", userService.find(userId));
		return "cashCoupon/init";
	}

	@RequestMapping("page")
	public void page(
			PageParam pageParam, CashCoupon object,HttpServletResponse response,Date startDate,Date endDate) 
					throws Exception {
		SimpleDateFormat simple=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(startDate != null){
			object.addCondition("vaild_start_time <= '" + simple.format(startDate) + "'", Condition.SEP_AND);
		}
		if(endDate != null){
			object.addCondition("vaild_end_time <= '" + simple.format(endDate) + "'", Condition.SEP_AND);
		}
		if(object.getStatus()!=null&&object.getStatus().equals(CashCoupon.STATUS_EXPIRED)){
			object.setStatus(null);
			object.addCondition("vaild_end_time<'" + DateUtil.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss") + "'", CashCoupon.SEP_AND);
		}
		cashCouponService.page(
			pageParam, object
		).jsonOut(response);
	}

	@RequestMapping("list")
	public void list(
			CashCoupon object, 
			HttpServletResponse response)
			throws Exception {
		getQueryResponse(
			cashCouponService.list(object)
		).jsonOut(response);
	}

	@RequestMapping("edit")
	public void edit(
			String id, String userId, 
			Model model) {
		if (id != null) {
			CashCoupon object = cashCouponService.find(id);
			model.addAttribute("object", object);
		}
		model.addAttribute("userId", userId);
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			CashCoupon object)
			throws Exception {
		if (object.getId() == null) {
			object.setName("");
			object.setVaildStartTime(new Date());
			object.setReaded(CashCoupon.READ_NO);
			object.setStatus(CashCoupon.STATUS_GET_YES);
			getResponse(
				cashCouponService.save(object)
			).jsonOut(response);
		} else {
			getResponse(
				cashCouponService.mdfy(object)
			).jsonOut(response);
		}
	}

	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			cashCouponService.lose(id)
		).jsonOut(response);
	}
	
	/**
	 * 给老用户分发代金券
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("initCashCoupon")
	public void initCashCoupon(HttpServletResponse response) throws Exception {
		List<User> list = userService.list(null);
		log.info("---------- send start,please wait... ----------");
		for (User user : list) {
			//代金券逻辑处理
			CashCoupon cashCoupon = new CashCoupon();
			cashCoupon.setUserId(user.getId());
			cashCoupon.setVaildStartTime(user.getCreateTime());
			cashCoupon.setVaildEndTime(DateUtil.monthAddMonth(user.getCreateTime(), 3));
			cashCoupon.setStatus(CashCoupon.STATUS_GET_YES);
			cashCoupon.setReaded(CashCoupon.READ_NO);
			cashCoupon.setMoney(200);//第一张代金券金额200元
			cashCoupon.setUseCondition(5);//5W才可使用
			cashCouponService.save(cashCoupon);
			cashCoupon.setId(null);
			cashCoupon.setMoney(250);//第二张代金券金额250元
			cashCoupon.setUseCondition(10);//10W才可使用
			cashCouponService.save(cashCoupon);
			cashCoupon.setId(null);
			cashCoupon.setMoney(550);//第三张代金券金额550元
			cashCoupon.setUseCondition(20);//20W才可使用
			cashCouponService.save(cashCoupon);
		}
		log.info("---------- send over... ----------");
		getResponse(1).jsonOut(response);
	}

}
